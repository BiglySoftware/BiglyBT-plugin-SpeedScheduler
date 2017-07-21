package speedscheduler;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

/**
 * Dialog which allows the user to select a set of days of the week, an hour span, and 
 * corresponding transfer configuration. To use the dialog, do this:
 * 
 * <code> 
 * ScheduleSelectionDialog dialog = new ScheduleSelectionDialog( shell );
 * Schedule userSelectedSchedule = dialog.open(); 
 * System.out.println( "User selected: " + userSelectedSchedule.toString() ); 
 * </code> 
 *
 * Note that the returned Schedule will be null if the user selects cancel or closes the
 * dialog without clicking Ok. 
 * @author David Smith 
 * @see Schedule 
 */
public class ScheduleEditDialog extends Dialog
{
    private ScheduleEditComposite scheduleEditComposite;
    private Schedule returnSchedule;
    private Color white;
    private static final int MARGIN_WIDTH = 15;
    private static final int MARGIN_HEIGHT = 15;
    private Image clockImage;
    private Shell shell;
    private Display display;
    private Shell parent;

    /**
     * Creates a new ScheduleSelectionDialog window.
     * Use open() to show the window and get the user-inputted schedule.
     */ 
    public ScheduleEditDialog( Shell parent, int style, Schedule editSchedule )
    {
        super( parent, style );
        white = parent.getDisplay().getSystemColor( SWT.COLOR_WHITE ); 
        returnSchedule = editSchedule;
        Log.println( "ScheduleEditDialog.construct( " + editSchedule + " )", Log.DEBUG );
    }

    /**
     * Creates a new ScheduleSelectionDialog window.
     * Use open() to show the window and get the user-inputted schedule.
     */ 
    public ScheduleEditDialog( Shell parent )
    {
        this( parent, 0, null );
    }

    public ScheduleEditDialog( Shell parent, Schedule editSchedule )
    {
        this( parent, 0, editSchedule );
    }

    /**
     * Displays the dialog and returns after the user has selected their schedule.
     * The schedule is returned.
     * @return The user-selected schedule.
     */
    public Schedule open()
    {
    	Log.println( "ScheduleEditDialog.open() - " + returnSchedule, Log.DEBUG );
        parent = getParent(); 
        display = parent.getDisplay();
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL); 
        shell.setText( getText() ); 
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = MARGIN_WIDTH;
        formLayout.marginHeight = MARGIN_HEIGHT;
        shell.setLayout( formLayout );

        Composite descPanel = new Composite( shell, 0 );
        descPanel.setBackground( white );
        FormData descPanelLayout = new FormData();
        descPanelLayout.top = new FormAttachment( 0, -MARGIN_HEIGHT );
        descPanelLayout.left = new FormAttachment( 0, -MARGIN_WIDTH );
        descPanelLayout.right = new FormAttachment( 100, MARGIN_WIDTH );
        descPanelLayout.bottom = new FormAttachment( 0, 60 );
        descPanel.setLayoutData( descPanelLayout );
        final Image clockImage = ImageUtils.getImage( display, this.getClass(), "schedule.png" );
        descPanel.addPaintListener( new PaintListener() {
            @Override
            public void paintControl(PaintEvent event ) {
                event.gc.drawImage( clockImage, MARGIN_HEIGHT, MARGIN_WIDTH );
            }
        });

        Log.println( "  Drawing labels...", Log.DEBUG );
        
        int labelX = 0;
        if( null != clockImage )
        	labelX = clockImage.getBounds().width + MARGIN_WIDTH + MARGIN_WIDTH;
        else
        	labelX = MARGIN_WIDTH;
        int labelWidth = 400;
        Label titleLabel = new Label( descPanel, 0 );
        if( null == returnSchedule )
            titleLabel.setText( "New Speed Schedule" );
        else
            titleLabel.setText( "Edit Speed Schedule" );
        titleLabel.setBackground( descPanel.getBackground() );
        titleLabel.setBounds( labelX, MARGIN_HEIGHT, labelWidth, 24 );
        titleLabel.setFont( new Font( display, "Some Font", 16, SWT.BOLD ) );
        Label subTitleLabel = new Label( descPanel, 0 );
        subTitleLabel.setText( "Here you may specify transfer speeds to use on a schedule." );
        subTitleLabel.setBounds( labelX, titleLabel.getLocation().y + titleLabel.getSize().y + 5, labelWidth, 20 );
        subTitleLabel.setBackground( descPanel.getBackground() );
        subTitleLabel.setForeground( new Color( display, 150, 150, 150 ) );
        subTitleLabel.setFont( new Font( display, "Some Other Font", 10, SWT.NORMAL ) );

        Log.println( "  Drawing the schedule edit composite...", Log.DEBUG );
        
        // Set the ScheduleSelectionWidget's location to 0,0 and use FormLayout to 
        // attach all other widgets to it
        scheduleEditComposite = new ScheduleEditComposite( shell, 0, returnSchedule );
        FormData scheduleEditLayout = new FormData();
        // Set the top of the selection widget 90 pixels from the top of the form
        scheduleEditLayout.top = new FormAttachment( descPanel, MARGIN_WIDTH, 0 );
        // Stretch the selection widget all the way to the right side (100% horizontal)
        scheduleEditLayout.right = new FormAttachment( 100, 0 );
        // Attach the left side of the widget to the left side of the form (0% horizontal)
        scheduleEditLayout.left = new FormAttachment( 0, 0 );
        scheduleEditComposite.setLayoutData( scheduleEditLayout );

        Log.println( "  Drawing OK/Cancel buttons...", Log.DEBUG );
        
        // OK Button
        Button okButton = new Button( shell, 0 );
        okButton.setText( "       Ok      " );
        FormData okButtonLayout = new FormData();
        okButtonLayout.right = new FormAttachment( 100, 0 );
        okButtonLayout.top = new FormAttachment( scheduleEditComposite, 10 );
        okButton.setLayoutData( okButtonLayout );
        okButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                // Sanity checks
                if( ! scheduleEditComposite.checkHours() ) {
                    errorMessageBox( "Can't save schedule because your\nhour-of-day selection has problems." );
                    return;
                }
                if( scheduleEditComposite.noDaysSelected() ) {
                    errorMessageBox( "Can't save schedule because you haven't\nselected any days of the week." );
                    return;
                }

                // Store the Schedule selected by the user so it is returned when this 
                // dialog disposes.
                returnSchedule = scheduleEditComposite.getSchedule();
                shell.dispose();
            }
        });
        okButton.setFocus();

        // Cancel Button
        Button cancelButton = new Button( shell, 0 );
        cancelButton.setText( "   Cancel   " );
        FormData cancelButtonLayout = new FormData();
        cancelButtonLayout.right = new FormAttachment( okButton, -5, 0 );
        cancelButtonLayout.top   = new FormAttachment( scheduleEditComposite, 10 );
        cancelButton.setLayoutData( cancelButtonLayout );
        cancelButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                // Remove any user selected Schedule so null is returned when this 
                // dialog disposes.
                returnSchedule = null;
                shell.dispose();
            }
        });

        Log.println( "  Packing and Opening...", Log.DEBUG );
        shell.pack();
        shell.open(); 
        Log.println( "  Entering event loop...", Log.DEBUG );
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) 
                display.sleep();
        }
        
        Log.println( "ScheduleEditComposite.open() returning: " + returnSchedule, Log.DEBUG );
        return returnSchedule;
    }

    private void errorMessageBox( String msg )
    {
        MessageBox messageBox = new MessageBox( shell, SWT.ICON_ERROR );
        messageBox.setMessage( msg );
        messageBox.setText( "Error" );
        messageBox.open();
    }
}
