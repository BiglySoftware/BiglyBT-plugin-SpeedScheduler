package speedscheduler;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;

/**
 * Displays a UI for editing a Schedule. Embed this Composite in your UI to edit
 * a Schedule.
 */
public class ScheduleEditComposite extends Composite
{
    private TransferConfigurationComposite      transferConfigComposite;
    private DayOfWeekComposite dayOfWeekComposite;
    private HourSpanComposite  hourSpanComposite;
    private CatComposite catComposite;

    public ScheduleEditComposite( Composite parent, int style, Schedule editSchedule )
    {
        super( parent, style );
        Log.println( "ScheduleEditComposite.construct()", Log.DEBUG );

        int defaultUploadRate = 0;
        int defaultDownloadRate = 0;
        boolean[] defaultSelectedDays = new boolean[TimeUtils.WEEK_LEN];
        Time defaultStartTime = new Time( 8, 0 );
        Time defaultEndTime = new Time( 17, 0 );
        boolean[] catSelection = {false,false};
        String category = "Uncategorized";
        if( null == editSchedule ) {
        	editSchedule = new Schedule( defaultSelectedDays, defaultStartTime, defaultEndTime, defaultUploadRate, defaultDownloadRate, false, false, catSelection, category );
        } else {
        	defaultUploadRate = editSchedule.getMaxUploadRate();
        	defaultSelectedDays = editSchedule.getSelectedDays();
        	defaultStartTime = editSchedule.getStartTime();
        	defaultEndTime = editSchedule.getEndTime();
        	catSelection = editSchedule.getCatSelection();
        	category = editSchedule.getCategory();
        }

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 5;
        formLayout.marginHeight = 5;
        this.setLayout( formLayout );

        RowLayout rowLayout = new RowLayout();
        rowLayout.marginTop = 15;
        rowLayout.marginLeft = 15;
        rowLayout.marginRight = 15;
        rowLayout.marginBottom = 15;
        
        Log.println( "  Drawing the TransferConfigurationComposite.", Log.DEBUG );
        Group transferGroup = new Group( this, 0 );
        transferGroup.setLayout( rowLayout );
        transferGroup.setText( "Speeds" );
        transferConfigComposite = new TransferConfigurationComposite( transferGroup, editSchedule.getMaxUploadRate(), editSchedule.getMaxDownloadRate(), editSchedule.areDownloadsPaused(), editSchedule.areSeedsPaused());
        FormData rateGroupLayout = new FormData();
        rateGroupLayout.left = new FormAttachment( 0, 0 );
        rateGroupLayout.right = new FormAttachment( 100, 0 );
        transferGroup.setLayoutData( rateGroupLayout );

        Log.println( "  Drawing the DayOfWeekComposite.", Log.DEBUG );
        Group dayGroup = new Group( this, 0 );
        dayGroup.setLayout( rowLayout );
        dayGroup.setText( "Days of the Week" );
        dayOfWeekComposite = new DayOfWeekComposite( dayGroup, defaultSelectedDays );
        FormData dayGroupLayout = new FormData();
        dayGroupLayout.top = new FormAttachment( transferGroup, 15, 0 );
        dayGroupLayout.left = new FormAttachment( 0, 0 );
        dayGroupLayout.right = new FormAttachment( 100, 0 );
        dayGroup.setLayoutData( dayGroupLayout );

        Log.println( "  Drawing the HourSpanComposite.", Log.DEBUG );
        Group hourGroup = new Group( this, 0 );
        hourGroup.setLayout( rowLayout );
        hourGroup.setText( "Hours of the Day" );
        hourSpanComposite =  new HourSpanComposite( hourGroup, defaultStartTime, defaultEndTime );
        FormData hourGroupLayout = new FormData();
        hourGroupLayout.top = new FormAttachment( dayGroup, 15, 0 );
        hourGroupLayout.left = new FormAttachment( 0, 0 );
        hourGroupLayout.right = new FormAttachment( 100, 0 );
        hourGroup.setLayoutData( hourGroupLayout );
        
		boolean use_tags = SpeedSchedulerPlugin.getInstance().getUseTagsNotCategories();

        Log.println( "  Drawing the Category.", Log.DEBUG );
        Group catGroup = new Group( this, 0 );
        catGroup.setLayout( rowLayout );
        catGroup.setText( use_tags?"Tag":"Category" );
        catComposite =  new CatComposite( catGroup, use_tags, catSelection, category);
        FormData catLayout = new FormData();
        catLayout.top = new FormAttachment( hourGroup, 15, 0 );
        catLayout.left = new FormAttachment( 0, 0 );
        catLayout.right = new FormAttachment( 100, 0 );
        catGroup.setLayoutData( catLayout );
        
        Log.println( "  Done constructing the ScheduleEditComposite.", Log.DEBUG );
    }

    /**
     * Gets the Schedule as specified by the user.
     * @return
     */
    public Schedule getSchedule()
    {
        return new Schedule( dayOfWeekComposite.getSelectedDays(), 
	                hourSpanComposite.getStartTime(), 
	                hourSpanComposite.getEndTime(),
					transferConfigComposite.getMaxUploadRate(), 
					transferConfigComposite.getMaxDownloadRate(),
					transferConfigComposite.areDownloadsPaused(),
					transferConfigComposite.areSeedsPaused(),
					catComposite.getSelection(),
					catComposite.getCategory());
    }

    /**
     * Shows a Schedule with default values populated initially.
     * @param parent
     */
    public ScheduleEditComposite( Composite parent )
    {
        this( parent, parent.getStyle(), null );
    }

    /**
     * Checks that the hours selected are sane (ie, the start hour is
     * earlier than the end hour, and there are more than 0 hours selected).
     * @return True if hours chosen by the user are sane, false otherwise.
     */
    public boolean checkHours()
    {
        return hourSpanComposite.checkTimes();
    }

    /**
     * Checks the day selection for sanity. If there are no days selected, 
     * returns true, false otherwise.
     * @return True if the user hasn't selected any days, false if they
     * 		have (false is good).
     */
    public boolean noDaysSelected()
    {
        for( int day=0; day<TimeUtils.WEEK_LEN; day++ )
            if( dayOfWeekComposite.isDaySelected( day ) )
                return false;
        return true;
    }
}
