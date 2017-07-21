package speedscheduler.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import speedscheduler.Log;
import speedscheduler.Schedule;
import speedscheduler.SpeedSchedulerPlugin;

/**
 * An implementation of ScheduleIO that persists Schedules as binary
 * serialized Java objects.
 * 
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BinaryScheduleIO implements ScheduleIO
{
	protected Vector schedules = new Vector( 3 );
	protected int defaultMaxUploadRate;
	protected int defaultMaxDownloadRate;
	protected boolean schedulesLoaded = false;
	
	/**
	 * Fetches the loaded schedules.
	 */
	@Override
	public Vector getSchedules()
	{
		if( ! schedulesLoaded )
			throw new SchedulesNotLoadedException();
		return schedules;
	}
	
    /**
     * Save these Schedules and default up/down rates to the persistent file
     * using a vector of binary Java objects and the ObjectOutputStream.
     * @param schedulesToSave
     * @param defaultMaxUploadRate
     * @param defaultMaxDownloadRate
     * @throws IOException
     */
    @Override
    public void saveSchedules(Vector schedulesToSave, int defaultMaxUploadRate, int defaultMaxDownloadRate ) throws IOException
    {
    	Log.println( "SchedulePersistencyManager.saveSchedules()", Log.DEBUG );
        if( null == schedulesToSave )
            throw new IllegalArgumentException( "SchedulePersistencyManager.saveSchedules: Cannot save schedules if null" );
        schedules = schedulesToSave;
        this.defaultMaxUploadRate = defaultMaxUploadRate;
        this.defaultMaxDownloadRate = defaultMaxDownloadRate;

        // Write the schedules to disk using an ObjectOutputStream
        int numSchedules = schedules.size();
        FileOutputStream fos = new FileOutputStream( getSaveFileName() );
        ObjectOutputStream oos = new ObjectOutputStream( fos );
        oos.writeObject( new Integer( defaultMaxUploadRate ) );
        oos.writeObject( new Integer( defaultMaxDownloadRate ) );
        //oos.writeObject( new Integer( numSchedules ) );
        for( int i=0; i<numSchedules; i++ ) {
            Schedule schedule = (Schedule)schedules.get(i);
            Log.println( "Saving schedule: " + schedules, Log.DEBUG );
            oos.writeObject( schedules );
            oos.flush();
        }
        oos.close();
        Log.println( "  Done saving schedules.", Log.DEBUG );
    }

    /**
     * Loads the Schedules and default up/down rates from the saved file of
     * binary Java Objects using an ObjectInputStream.
     * @return
     * @throws IOException
     */
    @Override
    public void loadSchedules() throws IOException
    {
        Log.println( "SchedulePersistencyManager.loadSchedules()", Log.DEBUG );
        Object objectRead = null;
        schedules = new Vector();
        try {
            FileInputStream fis = new FileInputStream( getSaveFileName() );
            ObjectInputStream ois = new ObjectInputStream( fis );
            defaultMaxUploadRate = ((Integer)ois.readObject()).intValue();
            defaultMaxDownloadRate = ((Integer)ois.readObject()).intValue();
            //int numSchedules = ((Integer)ois.readObject()).intValue();
            Log.println( "SchedulePersistencyManager: Loading schedules from file \"" + getSaveFileName() + "\"...", Log.DEBUG );
            schedules = (Vector) ois.readObject();
        } catch( ClassCastException e  ) {
            // TODO Graphical notification of corrupt schedule file
            Log.println( "Problem with saved schedules file: " + e.getMessage(), Log.ERROR );
            Log.println( "Loading empty list of schedules.", Log.ERROR );
            schedules = new Vector();
        } catch( FileNotFoundException e ) {
            // Schedule file is empty. That's okay. Give them an empty vector and
        	// default speeds.
            schedules = new Vector();
            defaultMaxUploadRate = SpeedSchedulerPlugin.getInstance().getAzureusGlobalUploadSpeed(); 
            defaultMaxDownloadRate = SpeedSchedulerPlugin.getInstance().getAzureusGlobalDownloadSpeed();
            Log.println( "Grabbed defaultMaxUploadRate from config: " + defaultMaxUploadRate, Log.DEBUG );
            Log.println( "Grabbed defaultMaxDownloadRate from config: " + defaultMaxUploadRate, Log.DEBUG );
        } catch( ClassNotFoundException e ) {
            // TODO Graphical notification of corrupt schedule file
            Log.println( "Error: Corrupt data found in saved schedules file, " + getSaveFileName() + ".\nLoading empty list of schedules.", Log.ERROR );
            schedules = new Vector();
        }
        schedulesLoaded = true;
        Log.println( "  Done loading schedules.", Log.DEBUG );
    }
    
    /**
     * Helper function that tells us where to save the Schedules.
     */
    private String getSaveFileName()
    {
        return SpeedSchedulerPlugin.getInstance().getPluginDirectoryName() + "/SavedSchedules.conf";
    }
    
	/**
	 * @see speedscheduler.io.ScheduleIO#getDefaultMaxUploadSpeed()
	 */
	@Override
	public int getDefaultMaxUploadSpeed()
	{		
		if( ! schedulesLoaded )
			throw new SchedulesNotLoadedException();
		return defaultMaxUploadRate;
	}
	
	/**
	 * @see speedscheduler.io.ScheduleIO#getDefaultMaxDownloadSpeed()
	 */
	@Override
	public int getDefaultMaxDownloadSpeed()
	{
		if( ! schedulesLoaded )
			throw new SchedulesNotLoadedException();
		return defaultMaxDownloadRate;
	}

	/* (non-Javadoc)
	 * @see speedscheduler.io.ScheduleIO#saveDefaultSpeeds(int, int)
	 */
	@Override
	public void saveDefaultSpeeds(int defaultMaxUploadSpeed, int defaultMaxDownloadSpeed ) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
}