package speedscheduler;

import java.util.Iterator;
import java.util.Vector;
import java.io.*;

import speedscheduler.io.BinaryScheduleIO;
import speedscheduler.io.ScheduleIO;
import speedscheduler.io.XmlScheduleIO;

/**
 * Responsible for loading/saving schedules to/from disk. Listeners
 * can register with this class to be notified when the user changes
 * the saved schedules.
 */
public class SchedulePersistencyManager
{
    /** Maintains a list of the registered listeners. */
    private Vector scheduleChangeListeners;
    /** Make it easy for people to get an instance of me. */
    private static SchedulePersistencyManager instance = new SchedulePersistencyManager();
    /** Where to save the schedules (TODO: hard-coded for now, find plugin dir and save there) */
    private ScheduleIO scheduleIO = new XmlScheduleIO();
    private ScheduleIO scheduleIO2 = new BinaryScheduleIO();

    /**
     * Gets the persisted default max upload rate (used when no schedules apply).
     * @return
     */
    public int getDefaultMaxUploadSpeed()
    {
        return scheduleIO.getDefaultMaxUploadSpeed();
    }
    
    /**
     * Gets the persisted default max download rate (used when no schedules apply).
     * @return
     */
    public int getDefaultMaxDownloadSpeed()
    {
        return scheduleIO.getDefaultMaxDownloadSpeed();    	
    }

    
    /**
     * Create a new SchedulePersistencyManager.
     */
    private SchedulePersistencyManager()
    {
    	Log.println( "SchedulePersistencyManager.construct()", Log.DEBUG );
        this.scheduleChangeListeners = new Vector();
        try {
            scheduleIO.loadSchedules();
        } catch( IOException e ) {
            // TODO Graphical notification or throw the exception to caller
            Log.println( "Unable to load schedules from file: " + e.getMessage(), Log.ERROR );
        }
    }

    /**
     * Gets the singleton instance of this class. We need a PluginInterface in
     * case we have to re-intantiate ourselves.
     * @param pluginInterface
     * @return
     */
    public static SchedulePersistencyManager getInstance()
    {
    	Log.println( "SchedulePersistencyManager.getInstance()", Log.DEBUG );
        return instance;
    }

    /**
     * If you want to know when the Schedules have changed,
     * register here. Don't call us, we'll call you. 
     * 
     * @param listener The ScheduleChangeListener you want to be notified
     * when the user changes a Schedule.
     */
    public void addScheduleChangeListener( ScheduleChangeListener listener )
    {
    	Log.println( "SchedulePersistencyManager.addScheduleChangeListener()", Log.DEBUG );
        if( null == listener )
            throw new IllegalArgumentException( "ScheduleChangeListener cannot be null!" );
        if( null == scheduleChangeListeners )
            scheduleChangeListeners = new Vector();
        scheduleChangeListeners.add( listener );
    }

    /**
     * Gest the currently persisted Schedules.
     * @return
     */
    public Vector getSchedules()
    {
    	/*
    	if( null == schedules )
    		schedules = new Vector();
    	Log.println( "SchedulePersistencyManager.getSchedules()", Log.DEBUG );
    	Log.println( "   Returning vector of size " + schedules.size(), Log.DEBUG );
    	return schedules;
    	*/
    	return scheduleIO.getSchedules();
    }
    
    /**
     * Save these Schedules and default up/down rates to the persistent file.
     * @param schedulesToSave
     * @param defaultMaxUploadSpeed
     * @param defaultMaxDownloadSpeed
     * @throws IOException
     */
    public void saveSchedules( Vector schedulesToSave, int defaultMaxUploadSpeed, int defaultMaxDownloadSpeed ) throws IOException
    {
    	scheduleIO.saveSchedules( schedulesToSave, defaultMaxUploadSpeed, defaultMaxDownloadSpeed );
    	
    	// Notify schedule listeners that schedules have been changed by the user.
        if( null == scheduleChangeListeners )
            scheduleChangeListeners = new Vector();
        Iterator i = scheduleChangeListeners.iterator();
        while( i.hasNext() ) {
        	Object next = i.next();
        	if( ! ( next instanceof ScheduleChangeListener ) )
        		continue;
        	ScheduleChangeListener listener = (ScheduleChangeListener) next;
        	try {
        		listener.schedulesChanged( scheduleIO.getSchedules(),
        								   scheduleIO.getDefaultMaxUploadSpeed(),
										   scheduleIO.getDefaultMaxDownloadSpeed() );
        	} catch( Exception e ) {
        		Log.printStackTrace( e, Log.ERROR );
        	}
        }
    }
    
	public void saveDefaultSpeeds( int defaultMaxUploadSpeed, int defaultMaxDownloadSpeed ) throws IOException
	{
		scheduleIO.saveDefaultSpeeds( defaultMaxUploadSpeed, defaultMaxDownloadSpeed );
	}
    
    public void loadSchedules() throws IOException
	{
    	scheduleIO.loadSchedules();
	}
}
