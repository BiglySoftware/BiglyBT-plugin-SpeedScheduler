package speedscheduler;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.swt.SWTException;
import com.biglybt.pif.logging.Logger;
import com.biglybt.pif.logging.LoggerChannel;

/**
 * A simple logger for this plugin.
 */
public class Log {
	private static PrintStream out = System.out;
	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;
	public static final int GRAPHICAL = 4;
	public static final int FORCE = 5;
	public static final int NONE = Integer.MAX_VALUE;
	private static int logLevel = DEBUG;
	private static boolean TAKE_OVER_STDOUT = false;
	private static boolean TAKE_OVER_STDERR = false;
	private static boolean APPEND_LOG = true;
	private static LoggerChannel loggerChannel;
	public static final String DEFAULT_LOG_FILE = SpeedSchedulerPlugin.getInstance().getPluginInterface().getPluginconfig().getPluginUserFile("SpeedScheduler.log").getPath();
	
	static {
		setFile( SpeedSchedulerPlugin.getInstance().getConfigParameter( "log.file", DEFAULT_LOG_FILE ) );
	}
	
	public static void setFile( String file )
	{
		try {
			SpeedSchedulerPlugin plugin = SpeedSchedulerPlugin.getInstance();
	        //out = new PrintStream( new FileOutputStream( SpeedSchedulerPlugin.getInstance().getPluginDirectoryName() + "/SpeedScheduler.log", APPEND_LOG ) );
			out = new PrintStream( new FileOutputStream( file, APPEND_LOG ) );
	        out.println( "\n\n   - - -   SpeedScheduler Logger initialized.   - - - \n\n" );
	        out.flush();
	        if( TAKE_OVER_STDOUT )
	        	System.setOut( out );
	        if( TAKE_OVER_STDERR )
	        	System.setErr( out );
        } catch( Exception e ) {
        	System.err.println( e.getMessage() );
        }
	}
	
	/**
	 * No instantiations!
	 */
	private Log() 
	{	
	}
	
	public static void setLevel( int newLevel )
	{
		logLevel = newLevel;
	}
	
	public static void println( String msg, int level )
	{
		if( logLevel == NONE )
			return;
		if( level >= logLevel ) {
			printLineHeader();
			out.println( msg );
			out.flush();
		}
		
		if( level  == ERROR )
			guiDisplay( ERROR, msg );
		if( level == GRAPHICAL )
			guiDisplay( INFO, msg );
	}
	
	private static void printLineHeader()
	{
		out.print( getDateTimeString() );
		out.print( "|" );
		out.print( Thread.currentThread().getName() );
		out.print( "|" );
	}
	
	public static void printStackTrace( Exception e, int level )
	{
		if( logLevel == NONE )
			return;
		if( level < logLevel )
			return;
		if( null == e )
			return;
		printLineHeader();
		e.printStackTrace( out );
		out.flush();
		
		if( level == ERROR )
			guiDisplay( e );
			
	}
	
	public static void printStackTrace( Exception e )
	{
		printStackTrace( e, ERROR );
	}
	
	private static String getDateTimeString()
	{
		Date d = new Date();
		return DateFormat.getDateInstance().format( d ) + " " +
			DateFormat.getTimeInstance().format( d );
	}
	
	private static void guiDisplay( int level, String msg )
	{
		int azureusLogLevel = LoggerChannel.LT_INFORMATION;
		switch( level ) {
			case ERROR:
				azureusLogLevel = LoggerChannel.LT_ERROR;
				break;
			case WARN:
				azureusLogLevel = LoggerChannel.LT_WARNING;
				break;
			case INFO:
			case DEBUG:
				azureusLogLevel = LoggerChannel.LT_INFORMATION;
				break;
		}
		
		initGuiLogger();
		try { 
			loggerChannel.logAlert( azureusLogLevel, msg );
		} catch( SWTException e ) { }
	}
	
	private static void guiDisplay( Throwable t )
	{
		initGuiLogger();
		loggerChannel.log( t.getMessage(), t );
	}
	
	private static void initGuiLogger()
	{
		if( null == loggerChannel ) {
			SpeedSchedulerPlugin ssp = SpeedSchedulerPlugin.getInstance(); 
			Logger logger = ssp.getAzureusPluginInterface().getLogger();
			loggerChannel = logger.getChannel( "SpeedScheduler" );
		}
	}
}
