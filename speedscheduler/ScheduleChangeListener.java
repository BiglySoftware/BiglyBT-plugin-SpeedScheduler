package speedscheduler;

import java.util.Vector;

/**
 * Implement this interface and register with the SchedulePersistencyManager 
 * to receive notification when the user-configured schedules have changed on disk.
 */
public interface ScheduleChangeListener
{
	/**
	 * Called to indicate that the user-configured schedules 
	 * have changed on disk.
	 * @param schedules The vector of schedules that are now selected
	 * @param defaultMaxUploadRate The new default max upload rate
	 * @param defaultMaxDownloadRate The new default max download rate
	 */
	public void schedulesChanged( Vector schedules, int defaultMaxUploadRate, int defaultMaxDownloadRate );
}
