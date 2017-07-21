package speedscheduler;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;

/**
 * A widget that draws checkboxes for each day of the week and 
 * provides accessor methods for fetching user input.
 */
class DayOfWeekComposite extends Composite
{
    /** Stores which days have been selected with the widget. 
     * Index 0 is Sunday, 1 is Monday, ...,  and 6 is Saturday. */
    private boolean[] selectedDays = new boolean[TimeUtils.WEEK_LEN];

    /**
     * Creates a new DayOfWeekChooser using the style of the specified parent.
     */
    public DayOfWeekComposite( Composite parent )
    {
        this( parent, null );
    }

    /**
     * Creates a new DayOfWeekChooser given the specified parent Composite
     * and style.
     */ 
    public DayOfWeekComposite( Composite parent, boolean[] defaultSelectedDays )
    {
        super( parent, SWT.NONE );
        Log.println( "DayOfWeekComposite.construct()", Log.DEBUG );
        initializeSelectedDays( defaultSelectedDays );
        Log.println( "  Setting up layouts.", Log.DEBUG );
        RowLayout layout = new RowLayout();
        layout.wrap = true;
        setLayout( layout );
        Log.println( "  Creating \"Days: \" label", Log.DEBUG );
        new Label( this, this.getStyle() ).setText( "Days: " );
        Log.println( "  Setting up checkboxes.", Log.DEBUG );
        for( int day=0; day<TimeUtils.WEEK_LEN; day++ )
            createDayOfWeekCheckbox( day );
    }

    /**
     * Factory to crank out a checkbox for a day of the week, complete
     * with text and a listener that calls handleDayOfWeekClick() on clicks.
     *
     * @param dayOfWeek The day of the week index from 0 to TimeUtils.WEEK_LEN-1.
     * @return A Button ready for action. 
     */
    private Button createDayOfWeekCheckbox( final int dayOfWeek )
    {
    	Log.println( "DayOfWeekComposite.createDayOfWeekCheckbox( " + dayOfWeek + " )" , Log.DEBUG );
        if( dayOfWeek < 0 || dayOfWeek >= TimeUtils.WEEK_LEN )
            return null;
        final Button b = new Button( this, SWT.CHECK );
        b.setText( TimeUtils.getShortDayName( dayOfWeek ) );
        b.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                handleDayOfWeekClick( b, dayOfWeek );
            }});
        if( isDaySelected( dayOfWeek ) )
            b.setSelection( true );
        return b;
    }

    /**
     * When a day-of-week checkbox is clicked.
     */
    private void handleDayOfWeekClick( Button buttonClicked, int dayOfWeek )
    {
        boolean selected = buttonClicked.getSelection();
        this.setDaySelected( dayOfWeek, selected );
        //dumpSelectedDays();
    }

    /**
     * Wrapper access to selectedDays array.
     */
    private void setDaySelected( int dayOfWeek, boolean selected )
    {
        if( dayOfWeek < 0 || dayOfWeek >= TimeUtils.WEEK_LEN )
            return;
        selectedDays[ dayOfWeek ] = selected;
    }
    
    /**
     * Initializes all days to false or to the specified defaults.
     */
    private void initializeSelectedDays( boolean[] defaultSelectedDays )
    {
    	Log.println( "DayOfWeekComposite.initializeSelectedDays()", Log.DEBUG );
        if( null != defaultSelectedDays && defaultSelectedDays.length == TimeUtils.WEEK_LEN ) {
            selectedDays = defaultSelectedDays;
        } else {
        	selectedDays = new boolean[ TimeUtils.WEEK_LEN ];
            for( int i=0; i<selectedDays.length; i++ )
                selectedDays[i] = false;
        }
        Log.println( "  Done initializing selected days.", Log.DEBUG );
    }
    
    /**
     * Gets whether a day is selected by day index.
     *
     * @param dayOfWeek Valid range is from 0 to TimeUtils.WEEK_LEN-1, where 0 represents
     *        the first day of the week (typically Sunday) and TimeUtils.WEEK_LEN-1 (typically
     *        6, ie Saturday) represents the last day of the week.
     * @return True if the specified dayOfWeek is selected, or false if not. 
     */
    protected boolean isDaySelected( int dayOfWeek )
    {
        if( dayOfWeek < 0 || dayOfWeek >= TimeUtils.WEEK_LEN )
            return false;
        return selectedDays[ dayOfWeek ];
    }

    /**
     * Get the selected days array. This returns an array of booleans of length TimeUtils.WEEK_LEN (typically 7) where
     * each entry in the array indicates whether the day is selected (true) or not (false).
     *
     * @return The array of boolean selected days.
     */
    protected boolean[] getSelectedDays()
    {
        return selectedDays;
    }

    /**
     * Prints to stderr the status for each day (whether selected or not)
     */
    private void dumpSelectedDays()
    {
        System.err.println( "\nSelected days:" );
        for( int day=0; day<selectedDays.length; day++ )
            System.err.println( "  " + TimeUtils.getShortDayName(day) + ": " + isDaySelected(day) );
    }
}

