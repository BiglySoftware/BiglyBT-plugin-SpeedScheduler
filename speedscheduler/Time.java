package speedscheduler;

/** 
 * Simple time object to store hour and minute and compare to other Times.
 */
public class Time implements java.io.Serializable
{
    private int hour;
    private int minute;

    /**
     * Creates a new Time with the specified hour and minute.
     * @param hour
     * @param minute
     */
    public Time( int hour, int minute ) 
    { 
        this.setHour( hour );
        this.setMinute( minute );
    }

    /**
     * Gets the hour value of this time (0-23).
     * @return
     */
    public int getHour()
    { 
        return hour;
    } 

    /**
     * Gets the minute value of this time (0-59).
     * @return
     */
    public int getMinute() 
    {
        return minute;
    } 

    /**
     * Sets the hour for this time, must be between 0 and 23.
     * @param hour The new hour.
     * @throws IllegalArgumentException if the hour is out of the range from 0-23.
     */
    public void setHour( int hour )
    { 
        if( hour < 0 || hour >= 24 )
            throw new IllegalArgumentException( "Hour must be in range 0-23" );
        this.hour = hour;
    }

    /**
     * Sets the minute for this time, must be between 0 and 59.
     * @param hour The new minute.
     * @throws IllegalArgumentException if the minute is out of the range from 0-59.
     */
    public void setMinute( int minute )
    {
        if( minute < 0 || minute >= 60 )
            throw new IllegalArgumentException( "Minute must be in range 0-59" );
        this.minute = minute;
    }
    
    /**
     * Figures out if this time is between the two specified times, t1 and t2, inclusive.
     * @param t1
     * @param t2
     * @return True if this time is between the two specified times, false otherwise.
     */
    public boolean between( Time t1, Time t2 )
    {
        if( null == t1 || null == t2 )
            return false;
        if( ( this.earlierThan( t2 ) || this.equals( t2 ) ) &&
        		( t1.earlierThan( this ) || this.equals( t1 ) ) ) 
            return true;
        else if( ( this.earlierThan( t1 ) || this.equals( t1 ) ) && 
        		( t2.earlierThan( this ) || this.equals( t2 ) ) )
            return true;
        else
            return false;
    }
    
    /**
     * Figures out if this time is before the specified time t. 
     * @param t 
     * @return True if this time occurs earlier during the day than Time t.
     */
    public boolean earlierThan( Time t )
    {
        if( null == t )
            return false;
        else if( t.getHour() < this.hour )
            return false;
        else if( t.getHour() > this.hour )
            return true;
        else 
            return this.minute < t.getMinute();
    }

    /**
     * Figures out if this time is equal to the specified time.
     * @param t
     * @return True if this time is equal, false otherwise.
     */
    public boolean equals( Object o )
    {
    	//Log.println( "Time.equals()", Log.DEBUG );
    	if( ! ( o instanceof Time ) )
    		return false;
    	Time t = (Time) o;
        if( null == t )
            return false;
        if( t.getHour() == this.hour && t.getMinute() == this.minute )
            return true;
        else
            return false;
    }
    
    /**
     * Gets this object's hash code
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
    	return hour + minute;
    }
    
    /**
     * Gets a human readable representation of this time, with am/pm and nice 
     * formatting. Use this to display the Time for people to read.
     * @return The human readable time.
     */
    public String toString()
    {
    	SpeedSchedulerPlugin plugin = SpeedSchedulerPlugin.getInstance();
    	// Display in 24 hour time?
    	if( plugin.getConfigParameter( "time.display", 12 ) == 24 ) {
    		StringBuffer s = new StringBuffer();
    		if( hour < 10 )
    			s.append( "0" );
    		s.append( hour );
    		s.append( ":" );
    		if( minute < 10 )
    			s.append( "0" );
    		s.append( minute );
    		return s.toString();
    	} else {
    		// Display in "normal" time (with AM/PM)
	        StringBuffer s = new StringBuffer();
	        if( hour == 0 && minute == 0 )
	        	return "Midnight";
	        if( hour == 0 )
	            s.append( 12 );
	        else if( hour < 12 )
	            s.append( hour );
	        else if( hour == 12 )
	            s.append( 12 );
	        else
	            s.append( hour - 12 );
	        s.append( ":" );
	        if( minute < 10 )
	            s.append( 0 );
	        s.append( minute );
	        if( hour < 12 )
	            s.append( " am" );
	        else
	            s.append( " pm" );
	        return s.toString();
    	}
    }
    
    /** For unit test only */
    public static void main( String args[] )
    {
    	Time startTime = new Time( 0, 0 );
    	Time endTime = new Time( 8, 0 );
    	Time testTime = new Time( 0, 0 );
    	System.out.println( testTime.between( startTime, endTime ) );
    }
}

