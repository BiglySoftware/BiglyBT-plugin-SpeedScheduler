package speedscheduler.io;

import java.io.IOException;
import java.util.Vector;

/**
* The SchedulePersistencyManager utilizes implementations of ScheduleIO to 
* persist and unpersist (save and load) user-defined schedules. Implement
* this interface if you want the SchedulePersistencyManager to use your
* implementation for persisting schedules.
*/
public interface ScheduleIO
{
	public void saveSchedules(Vector schedulesToSave, int defaultMaxUploadSpeed, int defaultMaxDownloadSpeed, boolean enforceDefaults ) throws IOException;
	public void loadSchedules() throws IOException;
	public Vector getSchedules();
	public int getDefaultMaxUploadSpeed();
	public int getDefaultMaxDownloadSpeed();
	public boolean getDefaultsEnforce();
}

/**
 * Thrown by implementors of ScheduleIO if attempts are made to retrieve 
 * persisted schedules without first calling loadSchedules().
 */
class SchedulesNotLoadedException extends RuntimeException
{
	/**
	 * Construct a SchedulesNotLoadedException with a default message. 
	 */
	public SchedulesNotLoadedException()
	{
		super( "You cannot perform this action without first calling loadSchedules()." );
	}
}