package speedscheduler;
import java.util.Calendar;

/**
 * Some handy constances and helper functions for working with times and such.
 */
public class TimeUtils
{
    /** Number of days in a week */
    public static final int WEEK_LEN = 7;
    
    /**
     * Gets the name of the day by week index.
     * @param dayOfWeekIndex the week index, 0: Sunday, 6: Saturday.
     * @return
     */
    public static String getShortDayName( int dayOfWeekIndex )
    {
        switch( dayOfWeekIndex ) {
            case 0:
                return "Sun";
            case 1:
                return "Mon";
            case 2:
                return "Tues";
            case 3:
                return "Wed";
            case 4:
                return "Thur";
            case 5:
                return "Fri";
            case 6:
                return "Sat";
            default:
                return null;
        }

    }
    
    /**
     * Utility to translate the dayOfWeek index to a human-readable string.
     * @return A string representing the day of the week, ie, "Sunday", "Wednesday", etc.
     */
    public static String getDayName( int dayOfWeekIndex )
    {
        switch( dayOfWeekIndex ) {
            case 0:
                return "Sunday";
            case 1:
                return "Monday";
            case 2:
                return "Tuesday";
            case 3:
                return "Wednesday";
            case 4:
                return "Thursday";
            case 5:
                return "Friday";
            case 6:
                return "Saturday";
            default:
                return null;
        }
    }

    /** 
     * Returns the current time according to the computer as a Time object.
     * This will only be as accurate as the computer's clock. 
     */
    public static Time getCurrentTime()
    {
    	//Log.println( "Time.getCurrentTime()", Log.DEBUG );
    	Calendar calendar = getCurrentCalendar();
        int hour = calendar.get( Calendar.HOUR_OF_DAY );
        int minute = calendar.get( Calendar.MINUTE );
        return new Time( hour, minute );
    }

    /**
     * Returns the day of the week indexed at 0.
     */
    public static int getCurrentDayIndex()
    {
    	//Log.println( "Time.getCurrentDayIndex()", Log.DEBUG );
        return getCurrentCalendar().get(Calendar.DAY_OF_WEEK) - 1; // return 0-indexed day of week
    }
    
    private static Calendar getCurrentCalendar()
    {
    	//Calendar calendar = new GregorianCalendar( TimeZone.getDefault() );
        //calendar.setTime( new Date() );
    	
    	// Here's an easier way to do it:
        return Calendar.getInstance();
    }
}
