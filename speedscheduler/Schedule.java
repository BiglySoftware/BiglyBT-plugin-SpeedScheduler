package speedscheduler;

import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * The Schedule is fundamental to SpeedScheduler. It stores two Time objects, a
 * set of days of the week, and rate information (max up rate, max down rate, and 
 * whether transfers should be paused during this Schedule). SpeedScheduler passes
 * instances of Schedule all over the place to figure out when to do what.
 */
public class Schedule implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Properties and defaults:
	private Time startTime = new Time( 8, 0 );
    private Time endTime = new Time( 17, 0 );
    private boolean[] selectedDays = { false, false, false, false, false, false, false };
    private int maxUploadRate = 0;
    private int maxDownloadRate = 0;
    private boolean seedsPaused = false;
    private boolean downloadsPaused = false;
    private boolean enabled = true;
    private boolean catSelection[] = {false,false};
    private String category = "Uncategorized";

    /**
     * Create a new Schedule with a specified set of days, start/end times, and
     * rate configuration.
     * 
     * @param selectedDays An array of 7 booleans (see TimeUtils.WEEK_LEN) representing
     * 			which days of the week this Schedule applies to. True entries in thearray
     * 			indicate that this Schedule <b>does</b> apply, and False entries indicate
     * 			the opposite. Sunday is index 0 and Saturday is index 6.
     * @param startTime The start time of this Schedule.
     * @param endTime The end time of this Schedule.
     * @param maxUploadRate The maxium upload rate for this Schedule.
     * @param maxDownloadRate The maxium download rate for this Schedule.
     * @param downloadsPaused True if all torrents should be paused during this Schedule.
     * @param seedsPaused True if all torrents should be paused during this Schedule.
     * @param catSelection[] Determins what the category section does if anything.
     * @param Category The category to apply the schedule to if activated
     */
    public Schedule( boolean[] selectedDays, Time startTime, Time endTime, int maxUploadRate, int maxDownloadRate, boolean downloadsPaused, boolean seedsPaused, boolean catSelection[], String category )
    {
    	//Log.println( "Schedule.construct()", Log.DEBUG );
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxUploadRate = maxUploadRate;
        this.selectedDays = selectedDays;
        this.maxDownloadRate = maxDownloadRate;
        this.downloadsPaused = downloadsPaused;
        this.seedsPaused = seedsPaused;
        this.catSelection = catSelection;
        this.category = category;
    }

    /**
	 * Creates a new schedule with default values.
	 */
	public Schedule()
	{
		//this( new boolean[TimeUtils.WEEK_LEN], new Time( 8, 0 ), new Time( 17, 0 ), 0, 0, false );
	}

	/**
     * Gets a human-readable string to show which days of the week have been selected.
     * Examples: "Monday through Friday", "Monday, Wednesday, Friday", "Saturday, Sunday".
     * @return The string representing the days selected.
     */
    public String getDaySelectionString()
    {
    	//Log.println( "Schedule.getDaySelectionString()", Log.DEBUG );
    	// first check if every day is selected
    	for( int day=0; day<TimeUtils.WEEK_LEN; day++ ) {
    		if( ! isDaySelected( day ) )
    			break;
    		if( day == TimeUtils.WEEK_LEN-1 )
    			return "Everyday";
    	}
        boolean firstDayPrinted = false;
        StringBuffer s = new StringBuffer();
        for( int day=0; day<TimeUtils.WEEK_LEN; day++ ) {
            if( ! isDaySelected( day ) )
                continue;
            int i;
            // Check if the next few days are also selected, so we can say "Mon through Sat"
            for( i=1; day+i<TimeUtils.WEEK_LEN; i++ ) {
                if( ! isDaySelected( day+i ) ) {
                    i--;
                    break;
                }
            }
            // Did we run off the end of the week? Back up the index if so.
            if( day+i == TimeUtils.WEEK_LEN )
                i--;
            if( i > 1 ) {
                if( firstDayPrinted )
                    s.append( ", " );
                s.append( TimeUtils.getDayName( day ) )
                	.append( " through " );
                day += i;
        		s.append( TimeUtils.getDayName( day ) );
            } else {
                if( firstDayPrinted )
                    s.append( ", " ); //$NON-NLS-1$
                s.append( TimeUtils.getDayName( day ) );
            }
            firstDayPrinted = true;
        }
        return s.toString();
    }

    /**
     * Gets the end time for this Schedule, represented as a Time object.
     * @return The end time.
     */
    public Time getEndTime()
    {
        return endTime;
    }

    /**
     * Gets a nicely formatted string representing the hours selected by this 
     * Schedule. Examples "8:00am - 10:00am", "All day", "Midnight - 3:00am".
     * @return The string.
     */
    public String getHourSelectionString()
    {
    	if( startTime.equals( new Time( 0, 0 ) ) && endTime.equals( new Time( 23, 59 ))) {
    		return "All day";
    	} else {
    		StringBuffer s = new StringBuffer( startTime.toString() )
				.append( " - " ) //$NON-NLS-1$
				.append( endTime.toString() );
			return s.toString();
    	}
    	/*
        StringBuffer s = new StringBuffer();
        s.append( startTime.toString() )
            .append( " - " )
            .append( endTime.toString() );
        return s.toString();
        */
    }

    /**
     * Gets a human-readable string representing the max upload speed for this
     * Schedule. Examples: "8 kbytes/sec", "Unlimited".
     * @return The string.
     */
    public String getUploadRateString()
    {
    	StringBuffer str = new StringBuffer( );
    	if( 0 == maxUploadRate) {
    		str.append( "Unlimited" );
    	} else {
        	str.append( maxUploadRate ).append( " kbytes/sec" );
    	}
    	if( seedsPaused && downloadsPaused)
    		str = new StringBuffer( "" );
    	else if( seedsPaused )
    		str.append( " (seeds paused)" );
    	return str.toString();
    }
    
    /**
     * Gets a human-readable string representing the max download speed for this
     * Schedule. Examples: "8 kbytes/sec", "Unlimited".
     * @return
     */
    public String getDownloadRateString()
    {
    	if( downloadsPaused && seedsPaused )
    		return "All transfers paused";
    	else if( downloadsPaused )
    		return "Downloads paused";
    	else if( 0 == maxDownloadRate) {
    		return "Unlimited";
    	} else {
    		StringBuffer s = new StringBuffer();
    		s.append( maxDownloadRate ).append( " kbytes/sec" );
    		return s.toString();
    	}
    }

    /**
     * Gets the raw boolean array (of size TimeUtils.WEEK_LEN) to show which days
     * have been selected for this Schedule. Sunday is index 0, and Saturday is index 6.
     * @return The array of selected days.
     */
    public boolean[] getSelectedDays()
    {
        return selectedDays;
    }

    /**
     * Gets the start time for this Schedule.
     * @return The start time.
     */
    public Time getStartTime()
    {
        return startTime;
    }

    /**
     * Returns true if the specified Time object falls withing the span covered
     * by this Schedule.
     * @param time The time to check.
     * @return True if the specified Time is within this Schedule, false otherwise.
     */
    public boolean inSpan( Time time )
    {
        if( null == startTime || null == endTime || null == time )
            return false;
        if( time.between( startTime, endTime ) )
            return true;
        else
            return false;
    }

    /**
     * Given a day index, gets whether that day is selected by this Schedule.
     * @param dayIndex
     * @return
     */
    public boolean isDaySelected( int dayIndex )
    {
        return ( null != selectedDays && 
                   dayIndex < selectedDays.length &&
                     selectedDays[dayIndex] );
    }

    /**
     * Sets the end time.
     * @param endTime The end time.
     */
    public void setEndTime( Time endTime )
    {
        this.endTime = endTime;
    }

    /**
     * Sets the selected days for this Schedule.
     * @param selectedDays The selected days array.
     */
    public void setSelectedDays( boolean[] selectedDays )
    {
        this.selectedDays = selectedDays;
    }

    /**
     * Sets the start time for this Schedule.
     * @param startTime The start time.
     */
    public void setStartTime( Time startTime )
    {
        this.startTime = startTime;
    }

    /**
     * Gets a human readable representation of this Schedule, mostly for debugging
     * purposes.
     * @return the string.
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        return s.append( "Up: " )
			.append( getUploadRateString() )
			.append( ", Down: " )
			.append( getDownloadRateString() )
            .append( ", Days: " )
            .append( getDaySelectionString() )
            .append( ", Times: " )
            .append( getHourSelectionString() )
			.append( ", Enabled: " )
			.append( isEnabled () )
            .toString();
    }

    /**
     * Gets the max download rate for this schedule.
     * @return The max download rate.
     */
	public int getMaxDownloadRate()
	{
		return maxDownloadRate;
	}
    /**
     * Sets the max download rate for this schedule.
     */	
	public void setMaxDownloadRate(int maxDownloadRate)
	{
		this.maxDownloadRate = maxDownloadRate;
	}

    /**
     * Gets the max upload rate for this schedule.
     * @return The max upload rate.
     */
	public int getMaxUploadRate()
	{
		return maxUploadRate;
	}
    /**
     * Sets the max upload rate for this schedule.
     */
	public void setMaxUploadRate(int maxUploadRate)
	{
		this.maxUploadRate = maxUploadRate;
	}

	/**
	 * Gets whether transfers are supposed to be paused during this Schedule.
	 * @return True if so, false otherwise.
	 */
	public boolean areDownloadsPaused()
	{
		return downloadsPaused;
	}

	/**
	 * Sets whether downloading torrents are supposed to be paused during this Schedule.
	 */
	public void setDownloadsPaused(boolean paused)
	{
		this.downloadsPaused = paused;
	}
	
	/**
	 * Sets whether seeding torrents are supposed to be paused during this Schedule.
	 */
	public void setSeedsPaused(boolean paused )
	{
		this.seedsPaused = paused;
	}
	
	/**
	 * Compares the specified Schedule (t) with this one.
	 * @param t The Schedule to compare to this one
	 * @return True if the Schedules are equivelant or the same Schedule instance,
	 *         and false otherwise.
	 */
	public boolean equals( Object o )
	{
		Log.println( "Schedule.equals()", Log.DEBUG ); //$NON-NLS-1$
		if( ! ( o instanceof Schedule ) ) {
			Log.println( "  returning false (not a Schedule object).", Log.DEBUG ); //$NON-NLS-1$
			return false;
		}
		Schedule t = (Schedule) o;
		//Log.println( "Schedule.equals( " + t + " )", Log.DEBUG );
		if( null == t ) {
			Log.println( "  returning false (null Schedule).", Log.DEBUG ); //$NON-NLS-1$
			return false;
		}
		if( this == t ) {
			Log.println( "  returning true (same ref).", Log.DEBUG ); //$NON-NLS-1$
			return true;
		}
		if( startTime.equals( t.getStartTime() ) &&
				endTime.equals( t.getEndTime() ) &&
				maxUploadRate == t.getMaxUploadRate() &&
				maxDownloadRate == t.getMaxDownloadRate() &&
				downloadsPaused == t.areDownloadsPaused() && 
				Arrays.equals( t.getSelectedDays(), this.selectedDays ) ) {
			/*
			for( int i=0; i<selectedDays.length; i++ )
				if( selectedDays[i] != t.getSelectedDays()[i] ) {
					Log.println( "  returning false (mismatched day selection).", Log.DEBUG ); //$NON-NLS-1$
					return false;
				}
				*/
			Log.println( "  returning true (everything checks out).", Log.DEBUG ); //$NON-NLS-1$
			return true;
		} else {
			Log.println( "  returning false (mismatched time or rate).", Log.DEBUG ); //$NON-NLS-1$
			return false;
		}
	}
	
	/**
     * Gets this object's hash code
     * @see java.lang.Object#hashCode()
     */
	public int hashCode()
	{
		CRC32 crc = new CRC32();
		crc.update( startTime.hashCode() );
		crc.update( endTime.hashCode() );
		crc.update( maxDownloadRate );
		crc.update( maxUploadRate );
		for( int i=0; i<selectedDays.length; i++ )
			crc.update( selectedDays[i] ? 0 : 1 );
		return (int) crc.getValue();
	}
	
	/**
	 * Gets whether this Schedule is enabled by the user.
	 * @return True if this Schedule is enabled by the user, false otherwise.
	 */
	public boolean isEnabled()
	{
		return enabled;
	}
	
	/**
	 * Enable or disable this schedule
	 * @param enabled The enabled to set.
	 */
	public void setEnabled( boolean enabled )
	{
		this.enabled = enabled;
	}
	
	/**
	 * Toggles the schedule from enabled to disabled and vice versa.
	 * 
	 * @return The enabled state AFTER the toggle.
	 */
	public boolean toggleEnabledState()
	{
		enabled = ! enabled;
		return enabled;
	}

	/**
	 * @return Whether seeds are paused during this schedule.
	 */
	public boolean areSeedsPaused()
	{
		return seedsPaused;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public boolean[] getCatSelection() {
		return catSelection;
	}

	public void setCatSelection(boolean[] catSelection) {
		this.catSelection = catSelection;
	}
	
}
