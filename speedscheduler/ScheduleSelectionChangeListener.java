package speedscheduler;

import java.util.Vector;

/**
 * Implement this interface and register with the SpeedSchedulerThread if you want 
 * to receive notification when the thread chooses a new schedule.
 */
public interface ScheduleSelectionChangeListener
{
	/**
	 * Called to indicate that the SpeedSchedulerThread has selected a new
	 * schedule.  
	 * @param selectedSchedules A vector of Schedules that the SpeedSchedulerThread just selected.  
	 *          Empty to indicate that no schedule is currently active.
	 * @see Schedule
	 */
	public void scheduleSelectionChanged( Vector selectedSchedules );
}
