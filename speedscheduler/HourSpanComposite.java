package speedscheduler;

import java.util.Vector;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

/**
 * A widget for choosing a span of time during a single day. 
 */
class HourSpanComposite extends Composite
{
    private Time startTime = new Time( 8, 0 );
    private Time endTime = new Time( 17, 0 );
    /** Minutes in this interval are selectable. */
    
    private static final String WARNING_ZERO_MINUTE_SPAN = "Zero minutes selected.";
    private static final String WARNING_START_AFTER_END = "Start time is after end time.";
    private static final Time DEFAULT_START_TIME = new Time( 8, 0 );
    private static final Time DEFAULT_END_TIME = new Time( 17, 0 );

    private Label messageLabel;
    private Color red;

    /**
     * Create a new HourSpanComposite and place it in the specified parent Composite.
     * @param parent The parent composite to embed this widget in.
     */
    public HourSpanComposite( Composite parent )
    {
        this( parent, null, null );
    }

    /**
     * Creates a new HourSpanComposite given a parent and default selected 
     * start and end times.
     * @param parent The parent Composite in which to place this widget.
     * @param defaultStartTime The start time to be selected when the widget is drawn. 
     * @param defaultEndTime The end time to be selected when the widget is drawn.
     */
    public HourSpanComposite( Composite parent, Time defaultStartTime, Time defaultEndTime )
    {
        super( parent, SWT.NONE );
        Log.println( "DayOfWeekComposite.construct()", Log.DEBUG );
        
        if( null == defaultStartTime )
            defaultStartTime = DEFAULT_START_TIME;
        if( null == defaultEndTime )
            defaultEndTime = DEFAULT_END_TIME;
        
        final int MINUTE_GRANULARITY = SpeedSchedulerPlugin.getInstance().getConfigParameter( "minutes.granularity", 15 );
        final int TOTAL_SELECTABLE_TIME_UNITS = (int)( 24.0f / ((float)MINUTE_GRANULARITY / 60.0f) );
        //final Time[] selectableTimes = new Time[ TOTAL_SELECTABLE_TIME_UNITS+1 ];
        final Vector selectableTimes = new Vector();
        
        RowLayout layout = new RowLayout();
        layout.type = SWT.HORIZONTAL;
        layout.wrap = true;
        setLayout( layout );
        new Label( this, this.getStyle() ).setText( "From:" );
        Combo startTimeCombo = new Combo( this, SWT.READ_ONLY | SWT.DROP_DOWN );
        new Label( this, this.getStyle() ).setText( "To:" );
        Combo endTimeCombo = new Combo( this, SWT.READ_ONLY | SWT.DROP_DOWN );
        messageLabel = new Label( this, 0 );
        messageLabel.setText( WARNING_START_AFTER_END );
        messageLabel.setVisible( false );
        red = parent.getDisplay().getSystemColor( SWT.COLOR_RED );
        messageLabel.setForeground( red );
        int startSelectionIndex = 0;
        int endSelectionIndex = 0;
        int counter = 0;
        for( int hour=0; hour<24; hour++ ) {
            for( int minute=0; minute<60; minute+=MINUTE_GRANULARITY ) {
                Time time = new Time( hour, minute );
                if( time.equals( defaultStartTime ) )
                    startSelectionIndex = counter;
                if( time.equals( defaultEndTime ) )
                    endSelectionIndex = counter;
                //selectableTimes.add[counter] = time;
                selectableTimes.add( time );
                startTimeCombo.add( time.toString() );
                endTimeCombo.add( time.toString() );
                counter++;
            }
        }
        
        // Add 11:59pm to the list of times, and see if it is selected
        Time lastTime = new Time( 23, 59 );
        startTimeCombo.add( lastTime.toString() );
        endTimeCombo.add( lastTime.toString() );
        if( defaultStartTime.equals( lastTime ))
        	startSelectionIndex = counter;
        if( defaultEndTime.equals( lastTime ))
        	endSelectionIndex = counter;
		
        //selectableTimes[counter] = lastTime;
        selectableTimes.add( lastTime );
        SelectionAdapter startComboSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event )  {
                int index = ((Combo)event.widget).getSelectionIndex();
                //startTime = selectableTimes[ index ];
                startTime = (Time)selectableTimes.elementAt( index );
                checkTimes();
            }};
        SelectionAdapter endComboSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event )  {
                int index = ((Combo)event.widget).getSelectionIndex();
                //endTime = selectableTimes[ index ];
                endTime = (Time)selectableTimes.elementAt( index );
                checkTimes();
            }};
        startTimeCombo.addSelectionListener( startComboSelectionAdapter );
        endTimeCombo.addSelectionListener( endComboSelectionAdapter );
        startTimeCombo.select( startSelectionIndex );
        //startTime = selectableTimes[ startSelectionIndex ];
        startTime = (Time)selectableTimes.elementAt( startSelectionIndex );
        endTimeCombo.select( endSelectionIndex );
        //endTime = selectableTimes[ endSelectionIndex ];
        endTime = (Time)selectableTimes.elementAt( endSelectionIndex );
    }

    /**
     * A message area is displayed on the widget for informing the user about
     * bone-headed actions they may take.
     * @param msg The message to post to the form.
     */
    private void setMessageText( String msg )
    {
        if( null == msg || "".equals( msg.trim() ) ) {
            messageLabel.setVisible( false );
        } else {
            messageLabel.setVisible( true );
        }
        if( ! messageLabel.getText().equals( msg ) )
            messageLabel.setText( msg );
    }

    /**
     * Returns true if times are sane, and posts messages to the widget if not.
     * @return True if times chosen are sane, false if not.
     */
    public boolean checkTimes()
    {
        if( startTime == null || endTime == null )
            return false;
        if( endTime.earlierThan( startTime ) ) {
            setMessageText( WARNING_START_AFTER_END );
            return false;
        } else if( endTime.equals( startTime ) ) {
            setMessageText( WARNING_ZERO_MINUTE_SPAN );
            return false;
        } else {
            setMessageText( "" );
            return true;
        }
    }

    /**
     * Gets the start time chosen by the user.
     * @return The start time chosen by the user.
     */
    public Time getStartTime()
    {
        return startTime;
    }
    
    /**
     * Gets the end time chosen by the user.
     * @return The end time chosen by the user.
     */
    public Time getEndTime()
    {
        return endTime;
    }
}
