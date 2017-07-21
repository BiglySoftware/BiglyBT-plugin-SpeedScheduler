package speedscheduler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;

import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.download.DownloadException;
import com.biglybt.pif.torrent.Torrent;
import com.biglybt.pif.torrent.TorrentAttribute;
import com.biglybt.pifimpl.local.PluginCoreUtils;

import com.biglybt.core.tag.Tag;
import com.biglybt.core.tag.TagManager;
import com.biglybt.core.tag.TagManagerFactory;
import com.biglybt.core.tag.TagType;

/**
 * This thread is responsible for monitoring the user-configured schedules
 * and adjusting the Azureus max upload speed based thereupon.
 */
public class SpeedSchedulerThread extends Thread implements ScheduleChangeListener
{
	/**
	 * When no schedule applies, the default rate is used.
	 */
	private int defaultMaxUploadRate = -1;
	private int defaultMaxDownloadRate = -1;
	private static final int DEFAULT_SLEEP_TIME = 60*1000;
	private static final int MIN_SLEEP_TIME = 1000;
	/**
	 * The list of the user configured schedules.
	 */
	private Vector schedules = new Vector();
	/**
	 * The list of schedules that are currently active based on day of week and time
	 * of day (the run() method chooses these).
	 */
	private Vector activeSchedules = new Vector();
	/**
	 * Interrace to Azureus.
	 */
	PluginInterface pluginInterface;
	/**
	 * Interrace to Azureus configuration.
	 */
	PluginConfig pluginConfig;
	/**
	 * To store the name of the configuration parameter we are interested in.
	 */
	String maxUploadSpeedParam, maxDownloadSpeedParam;
	/**
	 * A reference to the running thread.
	 */
	private SpeedSchedulerThread runningThread;
	private static SpeedSchedulerThread instance;

	/**
	 * Flag that tells the thread's loop whether to continue or pause.
	 */
	private boolean runFlag = SpeedSchedulerPlugin.getInstance().isEnabled();

	/** Maintains a list of the registered listeners. */
	private Vector scheduleSelectionListeners = new Vector(2);

	/**
	 * Creates a new SpeedSchedulerThread, which registers a listener with the
	 * SchedulePersistencyManager.
	 */
	public SpeedSchedulerThread()
	{
		Log.println( "SpeedSchedulerThread.construct()", Log.DEBUG );
		this.setName( "SpeedScheduler" );
		this.setDaemon( true );
		this.pluginInterface = SpeedSchedulerPlugin.getInstance().getAzureusPluginInterface();
		this.pluginConfig = pluginInterface.getPluginconfig();
		this.runningThread = this;
		instance = this;

		// Register with the persistency manager so he notifies us whenever the time
		// spans are changed by the user.
		SchedulePersistencyManager schedulePersistencyManager = SchedulePersistencyManager.getInstance();
		schedulePersistencyManager.addScheduleChangeListener( this );
		schedules = schedulePersistencyManager.getSchedules();
		defaultMaxUploadRate = schedulePersistencyManager.getDefaultMaxUploadSpeed();
		defaultMaxDownloadRate = schedulePersistencyManager.getDefaultMaxDownloadSpeed();
	}

	public static SpeedSchedulerThread getInstance()
	{
		return instance;
	}

	/**
	 * Start your engines and never come back.
	 */
	@Override
	public void run()
	{
		// Flag: True the first time through the loop, false thereafter.
		boolean firstLoopFlag = true;
		int loopCounter = 0; 
		// Flag: Tracks whether we have warned the user that auto speed is installed and enabled.
		boolean autoSpeedHasBeenWarned = false;
		while( true ) {

			Log.println( "SpeedSchedulerThread looping...", Log.DEBUG );
			//if( ++loopCounter > 100 )
			//Log.println( "SpeedSchedulerThread looping (" + loopCounter + ")...", Log.GRAPHICAL );
			try {
				try {
					// Don't sleep very long for the first loop.
					if( firstLoopFlag ) {
						sleep( 1000 );
					} else {
						int sleepTime = SpeedSchedulerPlugin.getInstance().getConfigParameter( "thread.sleep.time", DEFAULT_SLEEP_TIME );
						if( sleepTime < MIN_SLEEP_TIME )
							sleepTime = MIN_SLEEP_TIME;
						sleep( sleepTime );
					}
				} catch (InterruptedException e) {
					Log.println( "SpeedSchedulerThread interrupted.", Log.DEBUG );
				}

				firstLoopFlag = false;

				// Are we turned off? If so, just loop over again. Be sure to notify all the 
				// schedule change listeners that there are currently no selected schedules.
				runFlag = SpeedSchedulerPlugin.getInstance().isEnabled();
				if( ! runFlag ) {
					Log.println( "SpeedScheduler has been disabled by user. Not doing anything.", Log.DEBUG );
					activeSchedules = new Vector( 0 );
					notifyScheduleSelectionListeners( activeSchedules );
					continue;
				}

				// Warn the user about AutoSpeed behavior, if they haven't already been warned
				if( SpeedSchedulerPlugin.getInstance().isAutoSpeedEnabled() && ! SpeedSchedulerPlugin.getInstance().hasUserBeenWarnedAboutAutoSpeed() ) {
					Log.println( "Since you are using both the SpeedScheduler and AutoSpeed, SpeedScheduler will change AutoSpeed's max upload speed to prevent toe stomping.", Log.GRAPHICAL );

					SpeedSchedulerPlugin.getInstance().setUserHasBeenWarnedAboutAutoSpeed( true );
				}

				Log.println( "SpeedSchedulerThread looping (we are enabled)...", Log.DEBUG );

				Time currentTime = TimeUtils.getCurrentTime();
				int currentDay = TimeUtils.getCurrentDayIndex();
				if( null == schedules )  {
					// Should never happen, but if it does, just carry on
					continue;
				}

				Log.println( "\nCurrent time: " + currentTime.toString(), Log.DEBUG );
				Log.println( "Current day:  " + currentDay, Log.DEBUG );

				int maxUploadRateChosen = -1;
				int maxDownloadRateChosen = -1;
				boolean downloadsPaused = false;
				boolean seedsPaused = false;

				Vector categoryExclude = new Vector();
				Vector categoryOnly = new Vector();
				Vector schedulesChosen = new Vector(2);

				// Prevent the schedulesChanged() method from writing the vector 
				// while we are reading from it (the Iterator will barf if that
				// happens).                
				synchronized( schedules ) {
					Iterator i = schedules.iterator();
					while( i.hasNext() ) {
						Schedule s = (Schedule) i.next();
						Log.println( "  Thread analyzing schedule: " + s.toString(), Log.DEBUG );
						if( ! s.isEnabled() ) {
							Log.println( "  schedule is not enabled. skipping.", Log.DEBUG );
							continue;
						}
						if( s.inSpan( currentTime ) && s.isDaySelected( currentDay ) ) {
							Log.println( "   Current day/time IS in this schedule!", Log.DEBUG );
							if( s.getMaxUploadRate() > maxUploadRateChosen ) {
								maxUploadRateChosen = s.getMaxUploadRate();
								if( ! schedulesChosen.contains( s ) )
									schedulesChosen.add( s );
							}
							if( s.getCatSelection()[0] || s.getCatSelection()[1] )
							{

								if( s.getCatSelection()[0] )
								{
									Log.println( "Not in added", Log.DEBUG );
									categoryExclude.add( s.getCategory() );

									if( s.areDownloadsPaused() ) {
										downloadsPaused = true;
									}
									if( s.areSeedsPaused() ) {
										seedsPaused = true;
									}
								}
								else
								{
									Log.println( "In For added", Log.DEBUG );
									String[] data = { s.getCategory(), s.areDownloadsPaused()+"", s.areSeedsPaused()+""};
									categoryOnly.add( data );
								}
							}
							else
							{
								if( s.areDownloadsPaused() ) {
									downloadsPaused = true;
								}
								if( s.areSeedsPaused() ) {
									seedsPaused = true;
								}
							}
							if( s.getMaxDownloadRate() > maxDownloadRateChosen ) {
								maxDownloadRateChosen = s.getMaxDownloadRate();
								if( ! schedulesChosen.contains( s ) )
									schedulesChosen.add( s );
							}
						} else {
							Log.println( "   Current day/time is NOT in this schedule.", Log.DEBUG );
						}
					}

					// Loop over all Schedules again for the purpose of checking if
					// transfers are currently disabled.
//					i = schedules.iterator();
//					while( i.hasNext() ) {
//					Schedule s = (Schedule) i.next();
//					Log.println( "  Thread analyzing schedule: " + s.toString(), Log.DEBUG );
//					if( ! s.isEnabled() ) {
//					Log.println( "  schedule is not enabled. skipping.", Log.DEBUG );
//					continue;
//					}
//					if( s.inSpan( currentTime ) && s.isDaySelected( currentDay ) ) {
//					Log.println( "   Current day/time IS in this schedule!", Log.DEBUG );
//					if( ! schedulesChosen.contains( s ) )
//					schedulesChosen.add( s );
//					if( s.areDownloadsPaused() ) {
//					downloadsPaused = true;
//					}
//					if( s.areSeedsPaused() ) {
//					seedsPaused = true;
//					}
//					} else {
//					Log.println( "   Current day/time is NOT in this schedule.", Log.DEBUG );
//					}
//					}
				}

				// Did the active schedules actually change?
				if( ! schedulesChosen.equals( activeSchedules ) ) {
					activeSchedules = schedulesChosen;
					notifyScheduleSelectionListeners( activeSchedules );
				} else {
					Log.println( "Not notifying schedule selection listeners, since the schedules chosen did not change.", Log.DEBUG );
				}

				boolean use_tags = SpeedSchedulerPlugin.getInstance().getUseTagsNotCategories();

				TorrentAttribute[] tas = pluginInterface.getTorrentManager().getDefinedAttributes();
				TorrentAttribute torrent_categories = null;
				for (int i = 0; i < tas.length; i++) {

					if (tas[i].getName() == TorrentAttribute.TA_CATEGORY) {

						torrent_categories = tas[i];

						break;
					}
				}
	
				TagManager tag_manager = TagManagerFactory.getTagManager();
				
				// First check to see if downloads are paused now. If so, turn off all downloads
				if( downloadsPaused ) {                	
					Download[] torrents = pluginInterface.getDownloadManager().getDownloads();
					for( int i=0; i<torrents.length; i++ ) {
						Download d = torrents[i];

						//Do we have a 'Not in'?
						boolean not_in = false;
						String torrent_cat = getDownloadCatOrTag( use_tags, torrent_categories, tag_manager, d );
						
						Iterator ex = categoryExclude.iterator();
						
						while(ex.hasNext())
						{
							if(torrent_cat.compareTo((String) ex.next())==0)
							{
								not_in = true;
							}
						}
						// If it's not complete, it's a download. Pause it it's not already stopped or stopping
						if( ! d.isComplete() && d.getState() != Download.ST_STOPPED && d.getState() != Download.ST_STOPPING && ! d.isForceStart() && !not_in )
						{
							pauseTorrent( d );
						}
//						Is this a paused download and did we actually pause it?
						else if( ! d.isComplete() && d.getState() != Download.ST_DOWNLOADING  && ! d.isForceStart() && not_in && wePausedTorrent( d ) )
						{
							unPauseTorrent( d );
						}
					}
				} else {
					// Unpause all paused downloads that we paused originally.
					Download[] torrents = pluginInterface.getDownloadManager().getDownloads();
					for( int i=0; i<torrents.length; i++ ) {
						Download d = torrents[i];

						//Do we have a in?
						boolean in = false;
						String torrent_cat = getDownloadCatOrTag( use_tags, torrent_categories, tag_manager, d );
						
						Iterator on = categoryOnly.iterator();
						while(on.hasNext())
						{
							String[] compare = (String[])on.next();
							//If category matches and downloadPause Ticked then pause
							if(torrent_cat.compareTo(compare[0])==0 && "true".equalsIgnoreCase(compare[1]))
							{
								in = true;
							}
						}
						// Is this a paused download and did we actually pause it?
						if( ! d.isComplete() && d.getState() != Download.ST_DOWNLOADING && ! d.isForceStart() && !in && wePausedTorrent( d ) )
						{
							unPauseTorrent( d );
						}
						//If it's not complete, it's a download. Pause it it's not already stopped or stopping
						else if( ! d.isComplete() && d.getState() != Download.ST_STOPPED && d.getState() != Download.ST_STOPPING && ! d.isForceStart() && in )
						{
							pauseTorrent( d );
						}
					}                	
				}

				// First check to see if downloads are paused now. If so, turn off all downloads
				if( seedsPaused ) {                	
					Download[] torrents = pluginInterface.getDownloadManager().getDownloads();
					for( int i=0; i<torrents.length; i++ ) {
						Download d = torrents[i];
//						Do we have a not in?
						boolean not_in = false;
						String torrent_cat = getDownloadCatOrTag( use_tags, torrent_categories, tag_manager, d );
						
						Iterator ex = categoryExclude.iterator();
						while(ex.hasNext())
						{
							if(torrent_cat.compareTo((String) ex.next())==0)
							{
								not_in = true;
							}
						}
						// If it's complete, it's a seed. Pause it it's not already stopped or stopping
						if( d.isComplete() && d.getState() != Download.ST_STOPPED && d.getState() != Download.ST_STOPPING && ! d.isForceStart() && !not_in)
						{
							pauseTorrent( d );
						}
//						Is this torrent currently seeding and did we actually pause it?
						// Note, we don't pause force started torrents.
						else if( d.isComplete() && d.getState() != Download.ST_SEEDING && ! d.isForceStart() && not_in && wePausedTorrent( d ))
						{
							unPauseTorrent( d );
						}

					}
				} else {
					// Unpause all seeds that we paused.
					Download[] torrents = pluginInterface.getDownloadManager().getDownloads();

					for( int i=0; i<torrents.length; i++ ) {
						Download d = torrents[i];

//						Do we have a Only?
						boolean only = false;
						String torrent_cat = getDownloadCatOrTag( use_tags, torrent_categories, tag_manager, d );
						
						Iterator on = categoryOnly.iterator();
						while(on.hasNext())
						{
							String[] compare = (String[])on.next();
							//If category matches and downloadPause Ticked then pause
							if(torrent_cat.compareTo(compare[0])==0 && "true".equalsIgnoreCase(compare[2]))
							{
								only = true;
							}
						}

						// Is this torrent currently seeding and did we actually pause it?
						// Note, we don't pause force started torrents.
						if( d.isComplete() && d.getState() != Download.ST_SEEDING && ! d.isForceStart() && !only && wePausedTorrent( d ))
						{
							unPauseTorrent( d );
						}
//						If it's complete, it's a seed. Pause it it's not already stopped or stopping
						else if( d.isComplete() && d.getState() != Download.ST_STOPPED && d.getState() != Download.ST_STOPPING && ! d.isForceStart() && only)
						{
							pauseTorrent( d );
						}
					}                	
				}

				SpeedSchedulerPlugin speedSchedulerPlugin = SpeedSchedulerPlugin.getInstance();

				// Choose a new max upload rate if necessary
				if( maxUploadRateChosen > -1 ) {
					Log.println( "Current upload rate: " + speedSchedulerPlugin.getEffectiveMaxUploadSpeed(), Log.DEBUG );


					if(  speedSchedulerPlugin.getEffectiveMaxUploadSpeed() != maxUploadRateChosen ) {

						Log.println( "SpeedScheduler: Changing upload rate to " + maxUploadRateChosen, Log.DEBUG );

						speedSchedulerPlugin.setEffectiveMaxUploadSpeed( maxUploadRateChosen );

					} else {
                        Log.println( "SpeedSchedule: Schedule matches but upload rate already set.", Log.DEBUG );

                    }
				} else if( speedSchedulerPlugin.getEffectiveMaxUploadSpeed() != defaultMaxUploadRate ) {

					speedSchedulerPlugin.setEffectiveMaxUploadSpeed( defaultMaxUploadRate );

				} else {
					Log.println( "SpeedScheduler: No change to upload rate this time.", Log.DEBUG );
				}

				// Choose a new max download rate if necessary
				if( maxDownloadRateChosen > -1 ) {
					Log.println( "Current download rate: " + speedSchedulerPlugin.getAzureusGlobalDownloadSpeed(), Log.DEBUG );
					if( speedSchedulerPlugin.getAzureusGlobalDownloadSpeed() != maxDownloadRateChosen ) {
						Log.println( "SpeedScheduler: Changing download rate to " + maxDownloadRateChosen, Log.DEBUG );
						//pluginConfig.setIntParameter( maxDownloadSpeedParam, maxDownloadRateChosen );
						speedSchedulerPlugin.setAzureusGlobalDownloadSpeed( maxDownloadRateChosen );
					} else {
						Log.println( "SpeedSchedule: Schedule matches but download rate already set.", Log.DEBUG );
					}
				} else if( speedSchedulerPlugin.getAzureusGlobalDownloadSpeed() != defaultMaxDownloadRate ) {
					Log.println( "SpeedScheduler: Changing download rate to default: " + defaultMaxDownloadRate, Log.DEBUG );
					//pluginConfig.setIntParameter( maxDownloadSpeedParam, defaultMaxDownloadRate );
					speedSchedulerPlugin.setAzureusGlobalDownloadSpeed( defaultMaxDownloadRate );
				} else {
					Log.println( "SpeedScheduler: No change to download rate this time.", Log.DEBUG );
				}		
			} catch( Exception e ) {
				// Catch and ignore all exceptions to prevent this thread from ever dying
				Log.println( "SpeedSchedulerThread encountered an error, but I will keep on trucking:", Log.ERROR );
				Log.println( e.getMessage(), Log.ERROR );
				Log.printStackTrace( e, Log.ERROR );
			}
		}

	}

	private String
	getDownloadCatOrTag(
		boolean				use_tags,
		TorrentAttribute	torrent_categories,
		TagManager			tag_manager,
		Download			d )
	{
		if ( use_tags ){
			
			List<Tag> tags = tag_manager.getTagsForTaggable( TagType.TT_DOWNLOAD_MANUAL, PluginCoreUtils.unwrap( d ));
			
			if ( tags.size() == 0 ){
				
				return( "" );
				
			}else if ( tags.size() == 1 ){
				
				return( tags.get(0).getTagName( true ));
				
			}else{
				
				Collections.sort(
					tags,
					new Comparator<Tag>()
					{
						@Override
						public int compare(Tag o1, Tag o2) {
							return( o1.getTagName( true ).compareTo( o2.getTagName( true )));
						}
					});
				
				return( tags.get(0).getTagName( true ));
			}
		}else{
			String torrent_cat = d.getAttribute(torrent_categories);
			
			if (torrent_cat==null) torrent_cat = "Uncategorized";
			
			return( torrent_cat );
		}
	}
	

	// ----------------------------------------------------------
	// All methods below are used for pausing/unpausing Torrents.
	// SpeedScheduler has to remember which Torrents it paused, to
	// know which ones it can unpause (resume) when the time comes.
	// ----------------------------------------------------------


	/**
	 * Pauses the specified download and adds it to the list of downloads
	 * that we have paused, that we can thereby recall later which 
	 * downloads we have paused when it is time to un-pause them. 
	 * 
	 * @param download The download to pause.
	 * @see com.biglybt.pif.download.Download
	 */
	private void pauseTorrent( Download download )
	{
		Log.println( "pauseDownload( " + download + " )", Log.DEBUG );
		// Sanity checks
		if( null == download )
			return;
		// Only pause this download if it is currently running
		if( download.getState() == Download.ST_STOPPED ||
				download.getState() == Download.ST_STOPPING )
			return;
		try {
			download.stop();
			addPausedDownload( download );
		} catch( DownloadException e ) {
			Log.println( "Error: Could not pause download: " + e.getMessage(), Log.ERROR );
			Log.printStackTrace( e, Log.ERROR );
			// Do nothing, download is not persisted.
		}    	
	}

	/**
	 * Records that we paused the specified download in a persistent
	 * such that we will un-pause this download again when schedules
	 * indicate such.
	 * 
	 * @param download The download to pause and record as such.
	 * @see com.biglybt.pif.download.Download
	 */
	private void addPausedDownload( Download download )
	{
		Log.println( "addPausedDownload( " + download + " )", Log.DEBUG );
		// Sanity checks
		if( null == download )
			return;
		if( weAlreadyPausedDownload( download ) )
			return;
		Torrent torrent = download.getTorrent();
		if( null == torrent )
			return;
		String hash = pluginInterface.getUtilities().getFormatters().formatByteArray( torrent.getHash(), false );
		if( null == hash || 0 == hash.trim().length() )
			return;
		File file = new File( getPausedDownloadsFile() );
		BufferedWriter bw = null;
		try {
			// Append to the pausedDownloadsFile
			bw = new BufferedWriter( new FileWriter( file, true ) );
			bw.write( hash );
			bw.newLine();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO notify user that there's a problem (including better
			// handling of IOException's children like FileNotFoundException).
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Returns whether we paused the specified download based on its
	 * hash and the contents of the persistent paused downloads file.
	 * 
	 * @param download The download to check.
	 * @return True if we have already paused this download, or false
	 * otherwise.
	 * @see com.biglybt.pif.download.Download
	 */
	private boolean wePausedTorrent( Download download )
	{
		Log.println( "wePausedDownload( " + download + " )", Log.DEBUG );
		// Sanity checks
		if( null == download )
			throw new IllegalArgumentException( "Download paramter cannot be null to wePausedTorrent()" );
		Torrent torrent = download.getTorrent();
		if( null == torrent ) {
			Log.println( "Warning: Download has null Torrent\n  " + download.toString(), Log.WARN );
			return false;
		}

		String hash = pluginInterface.getUtilities().getFormatters().formatByteArray( torrent.getHash(), false );
//		Log.println( "  hash: " + hash, Log.DEBUG);
		if( null == hash || hash.trim().length() == 0 ) {
			Log.println( "Warning: empty hash for download.", Log.WARN );
			return false;
		}

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader( new File( getPausedDownloadsFile() ) ) );
		} catch (FileNotFoundException e) {
			// If the file is not found, we obviously couldn't have saved 
			// info about this download.
			Log.println( "Error: Paused torrents file not found, returning false.", Log.DEBUG );
			return false;
		}

		try {
			String line = null;
			try {
				// Look at each line of the file. If one of the hashes matches,
				// return true.
				Log.println( "Comparing two hashes:", Log.DEBUG);
				while( null != ( line = br.readLine() ) ) {
	//				Log.println( "File: \"" + line + "\"", Log.DEBUG );
	//				Log.println( "Parm: \"" + hash + "\"", Log.DEBUG );
					if( line.trim().equals( hash.trim() ) ) {
						Log.println( "  A match!", Log.DEBUG );
						Log.println( "Returning true!", Log.DEBUG );
						Log.println("", Log.DEBUG);
						return true;
					} else {
	//					Log.println( "  Not a match!", Log.DEBUG );
					}
				}
			} catch (IOException e) {
				Log.println( "Warning: Encountered an error reading the saved downloads file:\n  " + e.getMessage(), Log.DEBUG );
				Log.println( "Returning false!", Log.DEBUG );
				return false;
			}
	
			Log.println( "Returning false!", Log.DEBUG );
			return false;
		} finally {
			try {br.close();}
			catch (IOException ioe) {}
		}
	}

	/**
	 * Alias for wePausedDownload()
	 * @see wePausedDownload
	 * @see com.biglybt.pif.download.Download
	 */
	private boolean weAlreadyPausedDownload( Download download )
	{
		Log.println( "weAlreadyPauseDownload( " + download + " )", Log.DEBUG );
		return wePausedTorrent( download );
	}

	/**
	 * Gets the name of the file where paused download hashes are
	 * stored.
	 * 
	 * @return The name of the file. Example: <code>"C:/Documents and Settings/Dave/Application Data/Azureus/Plugins/SpeedScheduler/PausedTorrents.conf</code>"
	 * @see com.biglybt.pif.download.Download
	 */
	private String getPausedDownloadsFile()
	{
		Log.println( "getPausedDownloadsFile( )", Log.DEBUG );
		return SpeedSchedulerPlugin.getInstance().getPluginDirectoryName() + "/PausedTorrents.conf";
	}

	/**
	 * Gets the download with the specified hash (if any). Contacts the 
	 * DownloadManager and searchces for the Torrent that has the specified
	 * hash. If no such torrent is found, returns null. 
	 * 
	 * @param hash The hash of the Torrent, whose Download object to fetch.
	 * @return The Download if found or null if not found.
	 */
	private Download getDownloadFromHash( String hash )
	{
		Log.println( "getDownloadFromHash( " + hash + " )", Log.DEBUG );
		if( null == hash || hash.trim().length() == 0 )
			return null;
		hash = hash.trim();
		Download[] downloads = pluginInterface.getDownloadManager().getDownloads();
		if( null == downloads || downloads.length == 0 )
			return null;
		for( int i=0; i<downloads.length; i++ )
			if( hash.equals( new String( downloads[i].getTorrent().getHash() ) ) )
				return downloads[i];
		return null;
	}

	/**
	 * Unpauses the specified download (ie, restarts it), and removes it
	 * from the persistent list of paused downloads.
	 *   
	 * @param download The download to unpause.
	 */
	private void unPauseTorrent( Download download )
	{
		Log.println( "unPauseDownload( " + download + " )", Log.DEBUG );
		if( null == download )
			return;
		// Is this download currently running?
		if( download.getState() != Download.ST_DOWNLOADING &&
				download.getState() != Download.ST_PREPARING &&
				download.getState() != Download.ST_QUEUED &&
				download.getState() != Download.ST_WAITING &&
				download.getState() != Download.ST_READY &&
				download.getState() != Download.ST_SEEDING ) {
			try {
				download.restart();
				removePausedDownload( download );
			} catch( DownloadException e ) {
				Log.println( "Warning: Could not unpause download:\n  " + e.getMessage(), Log.WARN );
				return;
			}
		} else {
			Log.println( "Warning: Attempt to unpause a download not in a paused state.", Log.WARN );
		}


	}

	/**
	 * Removes a download's hash from the persistent file of downloads
	 * that we have paused.
	 * 
	 * @param download The Download to remove from the file.
	 */
	private void removePausedDownload( Download download )
	{
		Log.println( "removePausedDownload( " + download + " ) ", Log.DEBUG );
		if( null == download )
			throw new IllegalArgumentException( "Download paramter cannot be null to removePausedDownload()" );
		Torrent torrent = download.getTorrent();
		if( null == torrent )
			return;
		String hash = pluginInterface.getUtilities().getFormatters().formatByteArray( torrent.getHash(), false );
		if( null == hash || 0 == hash.trim().length() )
			return;
		hash = hash.trim();
		Vector hashes = new Vector();
		BufferedReader br = null;
		try {
			br = new BufferedReader( new FileReader( new File( getPausedDownloadsFile() ) ) );
		} catch (FileNotFoundException e) {
			Log.println( "Warning: Attempting to remove a download from the saved downloads file, but the file does not exist.", Log.WARN );
			return;
		}

		String line = null;
		try {
			while( null != ( line = br.readLine() ) )
				hashes.add( line.trim() );
		} catch( IOException e ) {
			Log.println( "Warning: Encountered an error while removing a download from the saved downloads file:\n   " + e.getMessage(), Log.WARN );
			return;
		}
		try {
			br.close();
		} catch( IOException e ) {
			Log.println( "Warning: Could not close saved downloads files:\n  " + e.getMessage(), Log.WARN );
		}

		int i = hashes.indexOf( hash );
		if( -1 == i ) {
			Log.println( " Warning: hash (" + hash + ") not found in paused download file!", Log.WARN );
			return;
		}
		hashes.remove( i );
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter( new FileWriter( new File( getPausedDownloadsFile() ) ) );
		} catch (IOException e) {
			Log.println( "Warning: Encountered an error while re-writing the saved downloads file:\n   " + e.getMessage(), Log.WARN );
			return;
		}
		Iterator iterator = hashes.iterator();
		while( iterator.hasNext() ) {
			try {
				String hashLine = (String) iterator.next();
				bw.write( hashLine );
				bw.newLine();
			} catch( IOException e ) {
				try {bw.close();}
				catch (IOException ioe) {}
				Log.println( "Warning: Encountered an error while removing a download from the saved downloads file:\n   " + e.getMessage(), Log.WARN );
				return;
			}
		}
		try {
			bw.close();
		} catch( IOException e ) {
			Log.println( "Error closing saved downloads file:\n  " + e.getMessage(), Log.ERROR );
		}
	}

	/**
	 * Called by the SchedulePersistencyManager whenever the user changes the schedules.
	 */
	@Override
	public void schedulesChanged(Vector newSchedules, int newDefaultMaxUploadRate, int newDefaultMaxDownloadRate )
	{
		Log.println( "SpeedSchedulerThread.schedulesChanged()", Log.DEBUG );
		Log.println( "   new default up rate:   " + newDefaultMaxUploadRate, Log.DEBUG );
		Log.println( "   new default down rate: " + newDefaultMaxDownloadRate, Log.DEBUG );
		synchronized( schedules ) {
			schedules = newSchedules;
			this.defaultMaxUploadRate = newDefaultMaxUploadRate;
			this.defaultMaxDownloadRate = newDefaultMaxDownloadRate;
		}
		runningThread.interrupt();
	}

	/**
	 * Call this method to sign-up to receive notification when the SpeedSchedulerThread
	 * selects a new schedule to use when limiting download/upload speeds.
	 * @see ScheduleSelectionChangeListener
	 * @param listener The listener that implements the ScheduleSelectionLisetener interface
	 *        who will be notified.
	 */
	public void addScheduleSelectionListener( ScheduleSelectionChangeListener listener ) 
	{
		if( null == listener )
			throw new IllegalArgumentException( "ScheduleChangeListener argument cannot be null!" );
		if( null == scheduleSelectionListeners )
			scheduleSelectionListeners = new Vector();
		scheduleSelectionListeners.add( listener );
	}

	/**
	 * Helper function that notifies all ScheduleSelectionListeners that
	 * the SpeedSchedulerThread has chosen a new schedule.
	 * @param schedules Vector of Schedules that we just chose.
	 * @see ScheduleSelectionChangeListener
	 */
	private void notifyScheduleSelectionListeners( Vector schedules )
	{
		int notificationCount = 0;
		Log.println( "SpeedSchedulerThread.notifyScheduleSelectionListeners( " + schedules + " )", Log.DEBUG );
		if( null == this.scheduleSelectionListeners )
			this.scheduleSelectionListeners = new Vector();
		Iterator i = this.scheduleSelectionListeners.iterator();
		while( i.hasNext() ) {
			Object next = i.next();
			if( ! ( next instanceof ScheduleSelectionChangeListener ) ) {
				Log.println( "Warning: Bad ScheduleSelectionListener in SpeedSchedulerThread's list", Log.ERROR );
				continue;
			}
			ScheduleSelectionChangeListener listener = (ScheduleSelectionChangeListener) next;
			try {
				Log.println( "  Notifying listener.", Log.DEBUG );
				notificationCount++;
				listener.scheduleSelectionChanged( schedules );
			} catch( Exception e ) {
				Log.printStackTrace( e, Log.ERROR );
			}
		}
		Log.println( "  Notified " + notificationCount + " listeners.", Log.DEBUG );
	}
}
