/*
 * Created on Jul 24, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package speedscheduler.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import speedscheduler.Log;
import speedscheduler.Schedule;
import speedscheduler.SpeedSchedulerPlugin;
import speedscheduler.Time;

/**
 * An implementation of ScheduleIO that persists Schedules to and loads
 * Schedules from an XML
 * file.
 */
public class XmlScheduleIO implements ScheduleIO
{

	protected boolean schedulesLoaded = false;
	
	private static final String[] SAX_PARSER_CLASSES = {
				"com.sun.org.apache.xerces.internal.parsers.SAXParser",
				"org.apache.crimson.parser.XMLReaderImpl",
				"com.bluecast.xml.Piccolo" };
	
	private static final String INDENT = "   "; 
	private int defaultMaxUploadRate, defaultMaxDownloadRate;
	private Vector schedules;
	
	/** 
	 * @see speedscheduler.io.ScheduleIO#saveSchedules(java.util.Vector, int, int)
	 */
	@Override
	public void saveSchedules(Vector schedulesToSave, int defaultMaxUploadRate, int defaultMaxDownloadRate) throws IOException
	{
		if( null == schedulesToSave )
			throw new IllegalArgumentException( "Error: schedulesToSave parameter cannot be null!" );
		this.defaultMaxUploadRate = defaultMaxUploadRate;
		this.defaultMaxDownloadRate = defaultMaxDownloadRate;
		this.schedules = schedulesToSave;
		
		StringBuffer s = new StringBuffer();
		s.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" )
			.append( "<SpeedScheduler>\n" )
			.append( INDENT )
			.append( "<defaultRates upload=\"" )
			.append( defaultMaxUploadRate )
			.append( "\" download=\"" )
			.append( defaultMaxDownloadRate )
			.append( "\"/>\n")
			.append( INDENT )
			.append( "<schedules>\n" );
		Iterator i = schedulesToSave.iterator();
		while( i.hasNext() ) {
			Object next = i.next();
			if( ! ( next instanceof Schedule ) ) {
				Log.println( "Warning: Bad vector contents in call to XmlScheduleIO.saveSchedules() [not instanceof Schedule]", Log.ERROR );
				continue;
			}
			Schedule schedule = (Schedule) next;
			s.append( scheduleToXml( schedule ) );
		}
		s.append( INDENT )
			.append( "</schedules>\n" )
			.append( "</SpeedScheduler>\n" );
		Log.println( "XmlScheduleIO.saveSchedules() -- Writing XML:\n" + s.toString(), Log.DEBUG );
		BufferedWriter writer = new BufferedWriter( new FileWriter( new File( getSaveFileName() ) ) );
		writer.write( s.toString() );
		writer.close();
	}

	/** 
	 * @see speedscheduler.io.ScheduleIO#saveDefaultSpeeds(int, int)
	 */
	@Override
	public void saveDefaultSpeeds(int defaultMaxUploadSpeed, int defaultMaxDownloadSpeed ) throws IOException
	{
		this.saveSchedules( this.schedules, defaultMaxUploadSpeed, defaultMaxDownloadSpeed );
	}
	
	/**
	 * Converts a schedul to an XML representation.
	 * @param schedule The Schedule to convert to XML.
	 * @return
	 */
	public String scheduleToXml( Schedule schedule ) 
	{
		if( null == schedule )
			throw new IllegalArgumentException( "Argument cannot be null" );
		StringBuffer s = new StringBuffer();
		s.append( INDENT )
			.append( INDENT )
			.append( "<schedule enabled=\"" )
			.append( schedule.isEnabled() )
			.append( "\">\n")
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<startTime hour=\"" )
			.append( schedule.getStartTime().getHour() )
			.append( "\" minute=\"" )
			.append( schedule.getStartTime().getMinute() )
			.append( "\"/>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<endTime hour=\"" )
			.append( schedule.getEndTime().getHour() )
			.append( "\" minute=\"" )
			.append( schedule.getEndTime().getMinute() )
			.append( "\"/>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<rates upload=\"" )
			.append( schedule.getMaxUploadRate() )
			.append( "\" download=\"" )
			.append( schedule.getMaxDownloadRate() )
			.append( "\" pauseDownloads=\"" )
			.append( schedule.areDownloadsPaused() )
			.append( "\" pauseSeeds=\"" )
			.append( schedule.areSeedsPaused() )
			.append( "\"/>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<days>\n" );
		for( int i=0; i<schedule.getSelectedDays().length; i++ )
			s.append( INDENT )
				.append( INDENT )
				.append( INDENT )
				.append( INDENT )
				.append( "<day index=\"" )
				.append( i )
				.append( "\" selected=\"" )
				.append( schedule.getSelectedDays()[i] )
				.append( "\"/>\n" );
		s.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "</days>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<category selected=\"" )
			.append( schedule.getCategory() )
			.append( "\"/>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( INDENT )
			.append( "<catSelection not_in=\"" )
			.append( schedule.getCatSelection()[0] )
			.append( "\" in=\"" )
			.append( schedule.getCatSelection()[1] )
			.append( "\"/>\n" )
			.append( INDENT )
			.append( INDENT )
			.append( "</schedule>\n" );
		return s.toString();
	}
	
	/**
	 * For unit testing only.
	 */
	public static void main( String args[] )
	{
		Schedule s = new Schedule();
		s.setMaxUploadRate( 15 );
		s.setMaxDownloadRate( 40 );
		s.setSelectedDays( new boolean[7] );
		s.setStartTime( new Time( 8, 15 ) );
		s.setEndTime( new Time( 17, 30 ) );
		System.out.println( new XmlScheduleIO().scheduleToXml( s ) );
	}

	/** 
	 * @see speedscheduler.io.ScheduleIO#loadSchedules()
	 */
	@Override
	public void loadSchedules() throws IOException
	{
		if( schedulesLoaded )
			return;
		ScheduleHandler scheduleXmlHandler = new ScheduleHandler();
		XMLReader parser = null;
		try {
			parser = getSaxParser();
		} catch( SAXException e ) {
			Log.println( e.getMessage(), Log.ERROR );
		}
		parser.setContentHandler( scheduleXmlHandler );
		try {
			String 	file_name = getSaveFileName();
			
			Log.println( "Loading scheduled from '" + file_name + "'", Log.FORCE );
			
			//parser.parse( file_name );
			FileInputStream	fis = new FileInputStream( file_name );
			
			try{
				parser.parse( new InputSource( fis ) );
				
			}finally{
				
				fis.close();
			}
		} catch( FileNotFoundException e ) {
			// There is no file yet. Create an empty one.
			File f = new File( getSaveFileName() );
			f.createNewFile();
		} catch( IOException e ) {
			Log.printStackTrace( e, Log.ERROR );
			throw e;
		} catch( SAXException e ) {
			Log.printStackTrace( e, Log.ERROR );
		}
		Log.println( "Done parsing. Schedules: " + getSchedules() + ", upload: " + getDefaultMaxUploadSpeed() + ", download: " + getDefaultMaxDownloadSpeed(), Log.DEBUG );
		
		defaultMaxDownloadRate = scheduleXmlHandler.getDefaultMaxDownloadRate();
		defaultMaxUploadRate = scheduleXmlHandler.getDefaultMaxUploadRate();
		schedules = scheduleXmlHandler.getSchedules();
		schedulesLoaded = true;
	}
	
	/**
	 * Finds an appropriate SAX driver class name
	 * @return
	 */
	private XMLReader getSaxParser() throws SAXException
	{
		XMLReader parser;
		
		// Get the user-configured parser if there is one:
		String parserClassName = SpeedSchedulerPlugin.getInstance().getConfigParameter( "sax.parser", "" );
		if( null != parserClassName && parserClassName.length() != 0 && isValidSaxDriver( parserClassName ) ) {
			System.setProperty( "org.xml.sax.driver", parserClassName );
			return XMLReaderFactory.createXMLReader();
		}
			
		// Try out the hard-coded SAX parsers
		for( int i=0; i<SAX_PARSER_CLASSES.length; i++ ) {
			parserClassName = SAX_PARSER_CLASSES[i];
			if( isValidSaxDriver( parserClassName ) ) {
				System.setProperty( "org.xml.sax.driver", parserClassName );
				try {
					return XMLReaderFactory.createXMLReader();
				} catch (SAXException e) {
					 // This one didn't work. Keep trying.
				}
			}
		}
		
		throw new SAXException( "No suitable parser was found on this system." );
	}
	
	/**
	 * Checks a class name. If it is a usable SAX parser, returns true.
	 * @param parserClassName
	 * @return true if the supplied driver class name is a usable SAX parser
	 */
	private boolean isValidSaxDriver( String parserClassName ) 
	{
		try {
			System.setProperty( "org.xml.sax.driver", parserClassName );
			XMLReader parser = XMLReaderFactory.createXMLReader();
		} catch( Exception e ) {
			// The class didn't work.
			return false;
		}
		// The class loaded successfully!
		return true;
	}

	/**
	 * @see speedscheduler.io.ScheduleIO#getSchedules()
	 */
	@Override
	public Vector getSchedules()
	{
		return schedules;
	}

	/**
	 * @see speedscheduler.ScheduleIO#getDefaultMaxUploadSpeed()
	 */
	@Override
	public int getDefaultMaxUploadSpeed()
	{
		return defaultMaxUploadRate;
	}

	/**
	 * @see speedscheduler.ScheduleIO#getDefaultMaxDownloadSpeed()
	 */
	@Override
	public int getDefaultMaxDownloadSpeed()
	{
		return defaultMaxDownloadRate;
	}
	
    /**
     * Helper function that tells us where to save the Schedules.
     */
    private String getSaveFileName()
    {
        return SpeedSchedulerPlugin.getInstance().getPluginDirectoryName() + File.separator + "SavedSchedules.xml";
    }
    
    private class ScheduleHandler extends DefaultHandler
	{
    	protected Vector schedules = new Vector( 3 );
    	protected int defaultMaxUploadRate;
    	protected int defaultMaxDownloadRate;
    	protected Schedule currentSchedule = null;
    	
		/** Initialize.
		 * @see org.xml.sax.ContentHandler#startDocument()
		 */
		@Override
		public void startDocument() throws SAXException
		{
			schedules = new Vector( 3 );
		}
		/**
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes ) throws SAXException
		{
			Log.println( "ScheduleHandler.startElement( " + localName + " )", Log.DEBUG );
			Log.println( "   attributes: ", Log.DEBUG );
			for( int i=0; i<attributes.getLength(); i++ ) {
				Log.println( "    " + i + ". " + attributes.getLocalName( i ) + ": " + attributes.getValue( i ), Log.DEBUG );
			}
			
			if( "defaultRates".equalsIgnoreCase( localName ) ) {
				defaultMaxUploadRate = Integer.parseInt( attributes.getValue( "upload" ) );
				defaultMaxDownloadRate = Integer.parseInt( attributes.getValue( "download" ) );
			} else if( "schedule".equalsIgnoreCase( localName ) ) {
				currentSchedule = new Schedule();
				boolean enabled = ! "false".equalsIgnoreCase( attributes.getValue( "enabled" ) );
				currentSchedule.setEnabled( enabled );
			} else if( "startTime".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				int hour = Integer.parseInt( attributes.getValue( "hour" ) );
				int minute = Integer.parseInt( attributes.getValue( "minute" ) );
				currentSchedule.setStartTime( new Time( hour, minute ) );
			} else if( "endTime".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				int hour = Integer.parseInt( attributes.getValue( "hour" ) );
				int minute = Integer.parseInt( attributes.getValue( "minute" ) );
				currentSchedule.setEndTime( new Time( hour, minute ) );
			} else if( "rates".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				int upload = Integer.parseInt( attributes.getValue( "upload" ) );
				int download = Integer.parseInt( attributes.getValue( "download" ) );
				boolean downloadsPaused = "true".equalsIgnoreCase( attributes.getValue( "pauseDownloads" ) );
				boolean seedsPaused = "true".equalsIgnoreCase( attributes.getValue( "pauseSeeds" ) );
				// For legacy support of older versions of the XML.
				if( "true".equalsIgnoreCase( attributes.getValue( "pauseTransfers" ) ) )
					seedsPaused = downloadsPaused = true;
				currentSchedule.setMaxUploadRate( upload );
				currentSchedule.setMaxDownloadRate( download );
				currentSchedule.setDownloadsPaused( downloadsPaused );
				currentSchedule.setSeedsPaused( seedsPaused );
			} else if( "day".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				int i = Integer.parseInt( attributes.getValue( "index" ) );
				boolean selected = "true".equalsIgnoreCase( attributes.getValue( "selected" ) );
				currentSchedule.getSelectedDays()[i] = selected;
			} else if( "category".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				String selected = attributes.getValue( "selected" );
				currentSchedule.setCategory(selected);
			} else if( "catSelection".equalsIgnoreCase( localName ) ) {
				if( null == currentSchedule )
					return;
				boolean catSelection[] = {false,false};
				catSelection[0] = "true".equalsIgnoreCase( attributes.getValue( "not_in" ) );
				catSelection[1] = "true".equalsIgnoreCase( attributes.getValue( "in" ) );
				currentSchedule.setCatSelection(catSelection);
			}
		}

		/**
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName ) throws SAXException
		{
			Log.println( "ScheduleHandler.endElement()", Log.DEBUG );
			Log.println( "   localName: " + localName, Log.DEBUG );
			
			if( "schedule".equalsIgnoreCase( localName ) ) {
				if( null != currentSchedule ) 
					schedules.add( currentSchedule );
				else
					Log.println( "Warning: Found an empty schedule in the XML file.", Log.ERROR );
				currentSchedule = null;
			}
		}
		
		public int getDefaultMaxUploadRate()
		{
			return defaultMaxUploadRate;
		}
		
		public int getDefaultMaxDownloadRate()
		{
			return defaultMaxDownloadRate;
		}
		
		public Vector getSchedules()
		{
			Log.println( "ScheduleHandler.getSchedules()", Log.DEBUG );
			Log.println( "   Returning vector of size " + schedules.size(), Log.DEBUG );
			return schedules;
		}
	}

}