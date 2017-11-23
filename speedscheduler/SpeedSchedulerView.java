package speedscheduler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import com.biglybt.pif.PluginInterface;
import com.biglybt.ui.swt.pif.*;

;/**
 * This is the main graphical portion of the SpeedScheduler, itself an Azureus view.
 * Access this by clicking View -- Plugins -- Speed Scheduler.
 */ 
public class SpeedSchedulerView implements ScheduleSelectionChangeListener, UISWTViewEventListener
{
    /** To contain all the configured schedules. */
    private Vector schedules = new Vector();
    private static final int BUTTON_PADDING = 5;
    private static final int MARGIN_WIDTH = 15;
    private static final int MARGIN_HEIGHT = 15;
    private static final String HELP_URL = "http://thesmithfam.org/blog/azureus-speed-scheduler-faq/";
    private Table scheduleTable;
    private Image watchImage, watchImageDisabled, checkImage;
    private Image defaultImage;
    private Composite mainComposite;
    private Menu contextMenu;
    private MenuItem newItem, editItem, deleteItem, enableItem;
    private Shell shell;
    private Display display;
    private SchedulePersistencyManager schedulePersistencyManager;
    private int defaultMaxUploadRate = 4;
    private int defaultMaxDownloadRate = 4;
    private boolean defaultsEnforce = true;
    
    private Vector allWidgets = new Vector(12);
    private IntegerInput defaultMaxUploadRateInput, defaultMaxDownloadRateInput;
    private Vector activeSchedules = new Vector();
    private Button editScheduleButton, newScheduleButton, deleteScheduleButton, enableScheduleButton;
	private static Color gray = null;
	private static Color blue = null;
	private static Cursor handCursor = null;
	private boolean viewIsDisposed = true;
	Composite defaultSpeedsImageComposite;
	private PluginInterface pluginInterface;

    /**
     * Called by the Azureus plugin system when my plugin is loaded.
     */
    private void initialize( Composite parent )
    {
    	Log.println( "SpeedSchedulerView.initialize()", Log.DEBUG );
    	initView( parent );
    }
    
    /**
     * Called exactly once when the user clicks the X on my view.
     */
    private void delete()
    {
    	Log.println( "SpeedSchedulerView.delete()", Log.DEBUG );
    	viewIsDisposed = true;
    	if( null != watchImage )
    		watchImage.dispose();
    	if( null != watchImageDisabled )
    		watchImageDisabled.dispose();
    	if( null != checkImage )
    		checkImage.dispose();
    }
    
    /**
     * Called periodically while the user is looking at my view (like more than once a second).
     */
    private void refresh()
    {
    	Log.println( "SpeedSchedulerView.refresh()", Log.DEBUG );
    	
    	// as long as we are refreshed, we are not yet disposed
    	viewIsDisposed = false;
    }
    
    /** 
     * Constructor used by Azureus to instantiate me and give me access 
     * to the PluginInterface.
     */
    public SpeedSchedulerView(PluginInterface pluginInterface)
    {
        this.pluginInterface = pluginInterface;
    	schedulePersistencyManager = SchedulePersistencyManager.getInstance();
        // Inform the SpeedSchedulerThread that we would like to receive notification
        // whenever a new schedule is chosen.
        SpeedSchedulerThread.getInstance().addScheduleSelectionListener( this );
    }

    /**
     * Initializes all the graphical controls and loads the saved schedules to display the view.
     */
    public void initView( final Composite parent ) 
    {
    	Log.println( "SpeedSchedulerView.initView()", Log.DEBUG );
    	
    	// clean up the allWidgets vector so we don't have any left-overs
    	// from previous viewings.
    	allWidgets.removeAllElements();
    	
        display = parent.getDisplay();
        shell = parent.getShell();
        // Only initialize gray if it hasn't already been initialized
        if( gray == null )
        	gray = display.getSystemColor(SWT.COLOR_GRAY );
        if( blue == null )
        	blue = display.getSystemColor( SWT.COLOR_BLUE );
        if( handCursor == null )
        	handCursor = new Cursor( display, SWT.CURSOR_HAND );
        mainComposite = new Composite( parent, SWT.NONE );
        FormLayout layout = new FormLayout();
        layout.marginWidth = MARGIN_WIDTH;
        layout.marginHeight = MARGIN_HEIGHT;
        mainComposite.setLayout(layout);

        final Composite titlePanel = new Composite( mainComposite, 0 );
        titlePanel.setBackground( display.getSystemColor( SWT.COLOR_WHITE ) );
        FormData titlePanelLayout = new FormData();
        titlePanelLayout.top = new FormAttachment( 0, -MARGIN_HEIGHT );
        titlePanelLayout.left = new FormAttachment( 0, -MARGIN_WIDTH );
        titlePanelLayout.right = new FormAttachment( 100, MARGIN_WIDTH );
        titlePanelLayout.bottom = new FormAttachment( 0, 60 );
        titlePanel.setLayoutData( titlePanelLayout );
        final Image clockImage = ImageUtils.getImage( display, this.getClass(), "clock.png" );
        final Composite descPanelFinal = titlePanel;
        titlePanel.addPaintListener( new PaintListener() {
            @Override
            public void paintControl(PaintEvent event ) {
                event.gc.drawImage( clockImage, MARGIN_WIDTH, MARGIN_HEIGHT );
            }
        });
        int labelX = 100;
        if( clockImage != null ) 
        	labelX = clockImage.getBounds().width + MARGIN_WIDTH + MARGIN_WIDTH;
        int labelWidth = 400;
        Label titleLabel = new Label( titlePanel, 0 );
        titleLabel.setText( "Speed Scheduler" );
        titleLabel.setBackground( titlePanel.getBackground() );
        titleLabel.setBounds( labelX, MARGIN_HEIGHT, labelWidth, 24 );
        titleLabel.setFont( new Font( display, "Some Font", 16, SWT.BOLD ) );
        Label subTitleLabel = new Label( titlePanel, 0 );
        subTitleLabel.setText( "Configure transfer speeds on a schedule.   (version " + Version.getVersion() + ")" );
        subTitleLabel.setBounds( labelX, titleLabel.getLocation().y + titleLabel.getSize().y + 5, labelWidth, 20 );
        subTitleLabel.setBackground( titlePanel.getBackground() );
        subTitleLabel.setForeground( new Color( display, 150, 150, 150 ) );
        subTitleLabel.setFont( new Font( display, "Some Other Font", 10, SWT.NORMAL ) );
        
        final Composite buttonPanel = new Composite( mainComposite, 0 );
        //final Composite defaultRateComposite = new Composite( mainComposite, 0 );
        final Group defaultSpeedComposite = new Group( mainComposite, 0 );
        defaultSpeedComposite.setText( "Default Speeds  (used when no schedule applies)" );
        //defaultRateComposite.setFont( new Font( parent.getDisplay(), "wordup", 10, SWT.BOLD ) );
        
        
        
        
        
        
        final Button enableCheckbox = new Button( mainComposite, SWT.CHECK );
        enableCheckbox.setSelection( SpeedSchedulerPlugin.getInstance().isEnabled() );
        FormData enableCheckboxFormat = new FormData();
        enableCheckboxFormat.left = new FormAttachment( 0, 0 );
        enableCheckboxFormat.top = new FormAttachment( titlePanel, MARGIN_HEIGHT );
        enableCheckbox.setLayoutData( enableCheckboxFormat );
        enableCheckbox.setText( "Enable SpeedScheduler" );
        enableCheckbox.addListener( SWT.Selection, new Listener() {
    		@Override
		    public void handleEvent(Event e ) {
    			Log.println( "enableCheckbox.handleEvent()" + e, Log.DEBUG );
				if( enableCheckbox.getSelection() ) {
					SpeedSchedulerPlugin.getInstance().setEnabled( true );
					setAllWidgetsEnabled( true );
				} else {
					SpeedSchedulerPlugin.getInstance().setEnabled( false );
					setAllWidgetsEnabled( false );
				}
			}});        

        scheduleTable = new Table( mainComposite, SWT.BORDER | SWT.FULL_SELECTION );
        watchImage = ImageUtils.getImage( display, this.getClass(), "schedule-small.gif" );
        defaultImage = watchImage;
        watchImageDisabled = ImageUtils.getImage( display, this.getClass(), "schedule-small-disabled.gif" );
        checkImage = ImageUtils.getImage( display, this.getClass(), "schedule-small-active.gif" );
        scheduleTable.setLinesVisible( false );
        scheduleTable.setHeaderVisible( true );
        FormData scheduleTableFormat = new FormData();
        scheduleTableFormat.left = new FormAttachment( 0, 0 );
        scheduleTableFormat.right = new FormAttachment( buttonPanel, -10 );
        scheduleTableFormat.top = new FormAttachment( enableCheckbox, MARGIN_HEIGHT );
        scheduleTableFormat.bottom = new FormAttachment( defaultSpeedComposite, -10 );
        scheduleTable.setLayoutData( scheduleTableFormat );
        // De-select all rows if clicks are made  out of bounds
        scheduleTable.addListener( SWT.MouseDown, new Listener() {
        	@Override
	        public void handleEvent(Event event ) {
        		Log.println( "scheduleTable.handleEvent( MouseDown )" , Log.DEBUG );
        		Log.println( "  event: " + event, Log.DEBUG );
        		TableItem selectedItem = scheduleTable.getItem( new Point( event.x, event.y ) );
        		Log.println( "  selectedItem: " + selectedItem, Log.DEBUG );
        		if( null == selectedItem )
        			scheduleTable.deselectAll();
        		setEditScheduleButtonsEnabledState();
        	}
        });
        // If the user uses the keypad to navigate schedules, update the edit/delete 
        // button status accordingly.
        scheduleTable.addKeyListener( new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0 ) {}
			@Override
			public void keyReleased(KeyEvent arg0 ) {
				setEditScheduleButtonsEnabledState();
			}
		});
        // Edit events on double click
        scheduleTable.addListener( SWT.MouseDoubleClick, new Listener() { 
            @Override
            public void handleEvent(Event event ) {
                editScheduleEvent();
            }
        });

        allWidgets.add( scheduleTable );

        // Columns
        TableColumn downRateCol = new TableColumn( scheduleTable, SWT.LEFT );
        downRateCol.setText( "Max Download Speed" );
        downRateCol.setWidth( 150 );
        TableColumn upRateCol = new TableColumn( scheduleTable, SWT.LEFT );
        upRateCol.setText( "Max Upload Speed" );
        upRateCol.setWidth( 140 );        
        TableColumn daysCol = new TableColumn( scheduleTable, SWT.LEFT );
        daysCol.setText( "Days of Week" );
        daysCol.setWidth( 180 );
        TableColumn hoursCol = new TableColumn( scheduleTable, SWT.LEFT );
        hoursCol.setText( "Hours of Day" );
        hoursCol.setWidth( 120 );
        TableColumn catCol = new TableColumn( scheduleTable, SWT.LEFT );
        
		boolean use_tags = SpeedSchedulerPlugin.getInstance().getUseTagsNotCategories();

        catCol.setText( use_tags?"Tag":"Category" );
        catCol.setWidth( 120 );
        
        // Make each column header clickable to allow sorting of schedules.
        // FIXME: Set a member variable indicating which column is sorted (and whether 
        //			it is ascending or descending), and then call this.refeshScheduleTable().
        // 			Add logic to refreshScheduleTable() to read that member variable
        //			and draw accordingly.
        // TODO: Add an event listener to know when a column is resized and store that in
        //			the Azureus configuration.
        /*
        final TableColumn[] columns = scheduleTable.getColumns();
        for( int i=0; i<columns.length; i++ ) {
        	final int columnIndex = i;
        	TableColumn column = columns[i];
        	column.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    TableItem[] items = scheduleTable.getItems();
                    Collator collator = Collator.getInstance(Locale.getDefault());
                    for (int i=1; i<items.length; i++) {
                        String value1 = items[i].getText(columnIndex);
                        for (int j=0; j<=i; j++){
                            String value2 = items[j].getText(columnIndex);
                            if (collator.compare(value1, value2) < 0) {
                                //String[] values = {items[i].getText(0), items[i].getText(1)};
                                String[] values = new String[ columns.length ];
                                for( int k=0; k<values.length; k++ )
                                	values[k] = items[i].getText(k);
                                items[i].dispose();
                                TableItem item = new TableItem(scheduleTable, SWT.NONE, j);
                                item.setText(values);
                                break;
                            }
                        }
                    }
                }
        	});
        }
        */
         	
        
        final Label timeLabel = new Label( mainComposite, 0 );
        //timeLabel.setForeground( gray );
        timeLabel.setEnabled( false );
        timeLabel.setText( "                            " + getTimeLabelString() );
        timeLabel.setAlignment( SWT.RIGHT );
        FormData timeLabelLayout = new FormData();
        timeLabelLayout.top = new FormAttachment( titlePanel, MARGIN_HEIGHT );
        //timeLabelLayout.left = new FormAttachment( enableCheckbox, MARGIN_WIDTH );
        //timeLabelLayout.right = new FormAttachment( 100, 0 );
        timeLabelLayout.right = new FormAttachment( scheduleTable, -2, SWT.RIGHT );
        //timeLabelLayout.bottom = new FormAttachment( scheduleTable, MARGIN_HEIGHT );
        timeLabel.setLayoutData( timeLabelLayout );
        Thread t = new Thread( new Runnable() {
        	@Override
	        public void run() {
        		while( true ) {
        			//Log.println( "\n\n\nTop of infinite thread loop\n\n\n", Log.DEBUG );
        			try{ Thread.sleep( 1000 ); }
        			catch( InterruptedException e ) {}
        			if( ! viewIsDisposed )
	        			display.syncExec( new Runnable() {
	        				@Override
					        public void run() {
	        					if( null == timeLabel || timeLabel.isDisposed() )
	        						return;	
	        					String timeString = getTimeLabelString();
	        					//Log.println( "\n\n\nInside syncExec(), updating label to this time: " + TimeUtils.getCurrentTime() + "\n\n\n", Log.DEBUG );
	        					if( ! timeLabel.getText().equalsIgnoreCase( timeString ) )
	        						timeLabel.setText( timeString );
	        				}
	        			});
        		}
        	}
        });
        t.setDaemon( true );
        t.start();
        
        // Default rates at the bottom
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 5;
        gridLayout.marginHeight = 10;
        gridLayout.marginWidth = 10;
        //defaultRateComposite.setLayout( new RowLayout() );
        defaultSpeedComposite.setLayout( gridLayout );
        
        defaultSpeedsImageComposite = new Composite( defaultSpeedComposite, 0 );
        
        //defaultSpeedsImageComposite.setText( "TEST" );
        defaultSpeedsImageComposite.addPaintListener( new PaintListener() {
			@Override
			public void paintControl(PaintEvent event ) {
				if( activeSchedules.size() == 0 )
					event.gc.drawImage( checkImage, 0, 0 );
				else
					event.gc.drawImage( watchImage, 0, 0 );
			}
		});
        GridData imageGridData = new GridData();
        imageGridData.widthHint = 27;
        imageGridData.heightHint = 29;
        imageGridData.verticalSpan = 2;
        defaultSpeedsImageComposite.setLayoutData( imageGridData );
        
        defaultMaxUploadRate	= schedulePersistencyManager.getDefaultMaxUploadSpeed();
        defaultMaxDownloadRate	= schedulePersistencyManager.getDefaultMaxDownloadSpeed();
        defaultsEnforce			= schedulePersistencyManager.getDefaultsEnforce();
        
        new Label( defaultSpeedComposite, 0 ).setText( "Max upload: " );
        defaultMaxUploadRateInput = new IntegerInput( defaultSpeedComposite, SWT.BORDER );
       
        defaultMaxUploadRateInput.setValue( defaultMaxUploadRate );
        GridData uploadGridData = new GridData();
        uploadGridData.widthHint = 35;
        defaultMaxUploadRateInput.setLayoutData( uploadGridData );
       
        defaultMaxUploadRateInput.addListener( SWT.FocusOut, new Listener() {
        	@Override
	        public void handleEvent(Event e ) {
        		limitDownloadRateIfNeeded();
        		try {
                	Log.println( "defaultMaxUploadRateInput.modifyText()!", Log.DEBUG );
                	Log.println( "  Getting current value...", Log.DEBUG );
                    defaultMaxUploadRate = defaultMaxUploadRateInput.getValue();
                    Log.println( "  Value is " + defaultMaxUploadRate, Log.DEBUG );
                    Log.println( "  Saving schedules and new default upload speed.", Log.DEBUG );
                    schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
                    //limitDownloadRateIfNeeded();
                } catch( IOException ex ) {
                    errorMessageBox( "Could not save settings: " + ex.getMessage() );
                }
        	}
        });
        new Label( defaultSpeedComposite, 0 ).setText( "kbytes/sec (for unlimited, use 0)   " );
        allWidgets.add( defaultMaxUploadRateInput );
        
        
        new Label( defaultSpeedComposite, SWT.NULL );
        
        new Label( defaultSpeedComposite, 0 ).setText( "Max download: " );
        defaultMaxDownloadRateInput = new IntegerInput( defaultSpeedComposite, SWT.BORDER );
      
        defaultMaxDownloadRateInput.setValue( defaultMaxDownloadRate );
        GridData downloadGridData = new GridData();
        downloadGridData.widthHint = 35;
        defaultMaxDownloadRateInput.setLayoutData( downloadGridData );
        defaultMaxDownloadRateInput.addListener( SWT.FocusOut, new Listener() {
        	@Override
	        public void handleEvent(Event e ) {
        		limitDownloadRateIfNeeded();
        		try {
                    defaultMaxDownloadRate = defaultMaxDownloadRateInput.getValue();
                    schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
                    //limitDownloadRateIfNeeded();
                } catch( IOException ex ) {
                    errorMessageBox( "Could not save settings: " + ex.getMessage() );
                }
        	}
        });
        new Label( defaultSpeedComposite, 0 ).setText( "kbytes/sec (for unlimited, use 0)   " );
        allWidgets.add( defaultMaxDownloadRateInput );
        
        Button defaultEnforceInput = new Button( defaultSpeedComposite, SWT.CHECK );
        defaultEnforceInput.setText( "Enforce default speed limits once applied" );
        allWidgets.add( defaultEnforceInput );
        
        defaultEnforceInput.setSelection( defaultsEnforce );
        defaultEnforceInput.addSelectionListener(
        	new SelectionAdapter(){
				
				@Override
				public void widgetSelected(SelectionEvent arg0){
					defaultsEnforce 	= defaultEnforceInput.getSelection();
					try{
						schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
					 } catch( IOException ex ) {
		                    errorMessageBox( "Could not save settings: " + ex.getMessage() );
		             }
				}
			});
        
        FormData defaultRateLayout = new FormData();
        defaultRateLayout.bottom = new FormAttachment( 100, 0 );        
        defaultRateLayout.right = new FormAttachment( scheduleTable, 0, SWT.RIGHT );
        defaultRateLayout.left = new FormAttachment( 0, 0 );
        defaultSpeedComposite.setLayoutData( defaultRateLayout );

        // Button panel on the right side
        FormData buttonPanelLayout = new FormData();
        buttonPanelLayout.right = new FormAttachment( 100, 0 );
        buttonPanelLayout.top = new FormAttachment( enableCheckbox, MARGIN_HEIGHT );
        buttonPanelLayout.bottom = new FormAttachment( 100, 0 );
        //buttonPanelLayout.bottom = new FormAttachment( scheduleTable, 0, SWT.BOTTOM  );
        buttonPanel.setLayoutData( buttonPanelLayout );
        FormLayout formLayout = new FormLayout();
        buttonPanel.setLayout( formLayout );

        // New action
        SelectionAdapter newScheduleAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                newScheduleEvent();
            }
        };

        // Edit action
        SelectionAdapter editScheduleAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                editScheduleEvent();
            }
        };

        // Delete action
        SelectionAdapter deleteScheduleAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                deleteScheduleEvent();
            }
        };
        
        // Enable/Disable action
        SelectionAdapter enableScheduleAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event ) {
                enableScheduleEvent();
            }
        };

        // New schedule button
        newScheduleButton = new Button( buttonPanel, 0 );
        FormData newScheduleButtonLayout = new FormData();
        newScheduleButtonLayout.top = new FormAttachment( 0, 0 );
        newScheduleButtonLayout.left = new FormAttachment( 0, 0 );
        newScheduleButtonLayout.right = new FormAttachment( 100, 0 );
        newScheduleButton.setLayoutData( newScheduleButtonLayout );
        newScheduleButton.setText( "    New Schedule   " );
        newScheduleButton.setToolTipText( "Create a new schedule." );
        newScheduleButton.addSelectionListener( newScheduleAdapter );
        allWidgets.add( newScheduleButton );

        // Edit button
        editScheduleButton = new Button( buttonPanel, 0 );
        FormData editScheduleButtonLayout = new FormData();
        editScheduleButtonLayout.top = new FormAttachment( newScheduleButton, BUTTON_PADDING * 3, 0 );
        editScheduleButtonLayout.left = new FormAttachment( 0, 0 );
        editScheduleButtonLayout.right = new FormAttachment( 100, 0 );
        editScheduleButton.setLayoutData( editScheduleButtonLayout );
        editScheduleButton.setText( "    Edit Schedule   " );
        editScheduleButton.setToolTipText( "Edit the schedule selected in the table." );
        editScheduleButton.addSelectionListener( editScheduleAdapter );
        allWidgets.add( editScheduleButton );

        // Delete button
        deleteScheduleButton = new Button( buttonPanel, 0 );
        FormData deleteScheduleButtonLayout = new FormData();
        deleteScheduleButtonLayout.top = new FormAttachment( editScheduleButton, BUTTON_PADDING, 0 );
        deleteScheduleButtonLayout.left = new FormAttachment( 0, 0 );
        deleteScheduleButtonLayout.right = new FormAttachment( 100, 0 );
        deleteScheduleButton.setLayoutData( deleteScheduleButtonLayout );
        deleteScheduleButton.setText( "   Delete Schedule  " ); 
        deleteScheduleButton.setToolTipText( "Delete the schedule selected in the table." );
        deleteScheduleButton.addSelectionListener( deleteScheduleAdapter );
        allWidgets.add( deleteScheduleButton );
        
        // Enable button
        enableScheduleButton = new Button( buttonPanel, 0 );
        FormData enableScheduleButtonLayout = new FormData();
        enableScheduleButtonLayout.top = new FormAttachment( deleteScheduleButton, BUTTON_PADDING * 3, 0 );
        enableScheduleButtonLayout.left = new FormAttachment( 0, 0 );
        enableScheduleButtonLayout.right = new FormAttachment( 100, 0 );
        enableScheduleButton.setLayoutData( enableScheduleButtonLayout );
        enableScheduleButton.setText( "Enable Schedule" );
        enableScheduleButton.setToolTipText( "Temporarily turn on/off this schedule. You can turn it back on later.");
        enableScheduleButton.addSelectionListener( enableScheduleAdapter );
        allWidgets.add( enableScheduleButton );

        // Help button
        final Label linkLabel = new Label( buttonPanel, SWT.NULL);
        FormData linkLabelLayout = new FormData();
        linkLabelLayout.top = new FormAttachment( enableScheduleButton, BUTTON_PADDING * 6, 0 );
        linkLabelLayout.left = new FormAttachment( 0, 0 );
        linkLabelLayout.right = new FormAttachment( 100, 0 );
        linkLabel.setLayoutData( linkLabelLayout );
        linkLabel.setAlignment( SWT.CENTER );
        linkLabel.setText( "Get help with\nSpeedScheduler" );
        linkLabel.setCursor( handCursor );
        linkLabel.setForeground( blue );
        linkLabel.addMouseListener( new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent arg0 ) {
				Program.launch( HELP_URL );
			}
			@Override
			public void mouseDown(MouseEvent arg0 ) {
				Program.launch( HELP_URL );
			}
			@Override
			public void mouseUp(MouseEvent arg0 ){}
        });
        
        contextMenu = new Menu( shell, SWT.POP_UP );
        scheduleTable.setMenu( contextMenu );

        newItem = new MenuItem( contextMenu, SWT.PUSH );
        newItem.addSelectionListener( newScheduleAdapter );
        newItem.setText( "New Schedule" );

        new MenuItem( contextMenu, SWT.SEPARATOR );

        editItem = new MenuItem( contextMenu, SWT.PUSH );
        editItem.addSelectionListener( editScheduleAdapter );
        editItem.setText( "Edit Schedule" );
        
        deleteItem = new MenuItem( contextMenu, SWT.PUSH );
        deleteItem.addSelectionListener( deleteScheduleAdapter );
        deleteItem.setText( "Delete Schedule" );
        
        new MenuItem( contextMenu, SWT.SEPARATOR );
        
        enableItem = new MenuItem( contextMenu, SWT.PUSH );
        enableItem.addSelectionListener( enableScheduleAdapter );
        enableItem.setText( "Enable Schedule" );

        try {
            schedulePersistencyManager.loadSchedules();
            schedules = schedulePersistencyManager.getSchedules();
            Log.println( "SpeedSchedulerView fetched " + schedules.size() + " from persistency manager.", Log.DEBUG );
            defaultMaxUploadRate = schedulePersistencyManager.getDefaultMaxUploadSpeed();
            defaultMaxDownloadRate = schedulePersistencyManager.getDefaultMaxDownloadSpeed();
        } catch( IOException e ) {
            // TODO graphical user notification
            Log.println( "Error loading configured schedules: " + e.getMessage(), Log.ERROR );
        }
        
        // Now, disable/enable all controls based on our enabled state:
		setAllWidgetsEnabled( SpeedSchedulerPlugin.getInstance().isEnabled() );
		setEditScheduleButtonsEnabledState();
		
		// Let anyone know that we are not disposed
		viewIsDisposed = false;
		
		// Refresh the content of the Schedule table
        refeshScheduleTable();
    }
    
    private String getTimeLabelString()
    {
		String dayName = TimeUtils.getDayName( TimeUtils.getCurrentDayIndex() );
		String time = TimeUtils.getCurrentTime().toString();
		String timeString = dayName + ", " + time; 
		return timeString;
    }
    
    /**
     * Sets the edit/delete buttons enabled if there is a schedule selected in the table, sets
     * them disabled if there is no schedule currently selected.
     */
    private void setEditScheduleButtonsEnabledState()
    {
    	int i = scheduleTable.getSelectionIndex(); 
    	boolean enableWidgets = ( i != -1 );
    	
		editScheduleButton.setEnabled( enableWidgets );
		deleteScheduleButton.setEnabled( enableWidgets );
		enableScheduleButton.setEnabled( enableWidgets );
		
		editItem.setEnabled( enableWidgets );
		deleteItem.setEnabled( enableWidgets );
		enableItem.setEnabled( enableWidgets );
		
		if( i >= 0 && i < schedules.size() ) {
			Schedule s = (Schedule) schedules.get( i );
			if( s.isEnabled() ) {
				enableItem.setText( "Disable Schedule" );
				enableScheduleButton.setText( "Disable Schedule" );
			} else {
				enableItem.setText( "Enable Schedule" );
				enableScheduleButton.setText( "Enable Schedule" );
			}
		}
    }
    
    /**
     * Checks the upload rate and sees if it violates the &lt;5 rule. If so,
     * limit the downloads to 2*upload.   
     *
     */
    private void limitDownloadRateIfNeeded()
    {
    	/* No more limiting:
    	Log.println( "SpeedSchdedulerView.limitDownloadRateIfNeeded()", Log.DEBUG );
    	int upRate =  defaultMaxUploadRateInput.getValue();
    	int downRate = defaultMaxDownloadRateInput.getValue();
    	if( upRate < 5 && upRate != 0 && 
				( downRate > upRate * 2 || 
			      downRate == 0 ) )
			defaultMaxDownloadRateInput.setValue( upRate * 2 );
		*/
    }
    
    /**
     * Enables or disables all widgets that have been added to the 
     * allWidets Vector. This turns off or on the SpeedScheduler display.
     * 
     * @param enabled True to enable all widgets, and false to disable.
     */
    private void setAllWidgetsEnabled( boolean enabled )
    {
    	Log.println( "SpeedSchedulerView.setAllWidgetsEnabled( " + enabled + " )", Log.DEBUG );
		if( null == allWidgets || 0 == allWidgets.size() ) 
			return;
		//Iterator i = allWidgets.iterator();
		for( Iterator i = allWidgets.iterator() ; i.hasNext() ; ) {
			Object o = i.next();
			if( o instanceof Control )
				((Control) o).setEnabled( enabled );
			else if( o instanceof IntegerInput )
				((IntegerInput) o).setEnabled( enabled );
		}
		
		// If we are enabling all the widgets, then re-draw the enabled states for the edit/delete buttons
		if( enabled )
			setEditScheduleButtonsEnabledState();
	}

	/**
     * Pops up an edit Schedule dialog for editing the selected Schedule
     * (if any) from the table.
     */
    private void editScheduleEvent()
    {
    	Log.println( "SpeedSchedulerView.editScheduleEvent()", Log.DEBUG );
        int i = scheduleTable.getSelectionIndex();
        if( i >= schedules.size() || i < 0 )
            return;
        Schedule selectedSchedule = (Schedule) schedules.get(i);
        ScheduleEditDialog dialog = new ScheduleEditDialog( shell, selectedSchedule );
        Schedule newSchedule = dialog.open();
        if( null == newSchedule )
            return;
        schedules.set( i, newSchedule );
        try {
            schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
        } catch( IOException e ) {
        	Log.println( "Error saving schedule: " + e.getMessage(), Log.ERROR );
        }
        refeshScheduleTable( );
        scheduleTable.setSelection( i );
    }

    /**
     * Pops up a new Schedule dialog for creating a new Schedule.
     */
    private void newScheduleEvent()
    {
    	Log.println( "SpeedSchedulerView.newScheduleEvent()", Log.DEBUG );
        ScheduleEditDialog dialog = new ScheduleEditDialog( shell );
        Schedule newSchedule = dialog.open();
        if( null != newSchedule ) {
        	Log.println( "Adding new Schedule to list and saving...", Log.DEBUG );
            schedules.add( newSchedule );
            try {
                schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
            } catch( IOException e ) {
                Log.println( "Error saving new schedule: " + e.getMessage(), Log.ERROR );
            }
            refeshScheduleTable();
            scheduleTable.setSelection( scheduleTable.getItemCount() - 1 );
        }
    }

    /**
     * Deletes the selected Schedule from the table (if any). Saves changes to disk immediately.
     */
    private void deleteScheduleEvent()
    {
    	Log.println( "SpeedSchedulerView.deleteScheduleEvent()", Log.DEBUG );
        int i = scheduleTable.getSelectionIndex();
        if( i < 0 || i >= schedules.size() )
            return;
        schedules.remove( i );
        try {
            schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
        } catch( IOException e ) {
            // TODO Graphical user notification
        	Log.println( "Error saving schedule: " + e.getMessage(), Log.ERROR );
        }
        refeshScheduleTable();
    }
    
    /**
     * toggles the selected Schedule from the table between enabled and disabled.
     */
    private void enableScheduleEvent()
    {
    	Log.println( "SpeedSchedulerView.enableScheduleEvent()", Log.DEBUG );
        int i = scheduleTable.getSelectionIndex();
        if( i < 0 || i >= schedules.size() )
            return;
        Schedule s = (Schedule) schedules.get( i );
        s.toggleEnabledState();
        try {
            schedulePersistencyManager.saveSchedules( schedules, defaultMaxUploadRate, defaultMaxDownloadRate, defaultsEnforce );
        } catch( IOException e ) {
            // TODO Graphical user notification
            Log.println( "Error saving schedule: " + e.getMessage(), Log.ERROR );
        }
        refeshScheduleTable( );
        scheduleTable.setSelection( i );
    }
    
    /**
     * Refreshes the Schedule table based on the contents of the schedules vector.
     * The currently selected item will remain selected after the table is refreshed.
     */
    protected synchronized void refeshScheduleTable()
    {
    	Log.println( "SpeedSchedulerView.refreshScheduleTable()", Log.DEBUG );
    	if( viewIsDisposed )
    		return;
    	if( null == scheduleTable )
    		return;
    	int selectedIndex = scheduleTable.getSelectionIndex();
        scheduleTable.removeAll();
        Iterator i = schedules.iterator();
        boolean alternatingRowBackground = true;
        while( i.hasNext() ) {
        	alternatingRowBackground = ! alternatingRowBackground;
            Schedule newSchedule = (Schedule)i.next();
            Log.println( "  Drawing Schedule: " + newSchedule, Log.DEBUG );
            String displayCategory="";
            
            if(newSchedule.getCatSelection()[0])
            	displayCategory = "Not in "+newSchedule.getCategory();
            else if(newSchedule.getCatSelection()[1])
            	displayCategory = "In     "+newSchedule.getCategory();
            
            TableItem newRow = new TableItem( scheduleTable, SWT.NONE );
            newRow.setText( new String[] { newSchedule.getDownloadRateString(),
            								newSchedule.getUploadRateString(),
                                             newSchedule.getDaySelectionString(), 
                                               newSchedule.getHourSelectionString(),
                                                displayCategory} );
            
            // What icon to draw next to this new row
            if (!newSchedule.isEnabled()) {
				Log.println( "   Setting image: watchImageDisabled.", Log.DEBUG );
				newRow.setImage( watchImageDisabled );
				newRow.setForeground( gray );
			} else if( isActive( newSchedule ) ) {
				Log.println( "   Setting image: checkImage.", Log.DEBUG );
				newRow.setImage( checkImage );
				newRow.setFont( new Font( display, "some bold font", newRow.getFont().getFontData()[0].getHeight(), SWT.BOLD ) );
			} else { // schedule is enabled, but not active
				Log.println( "   Setting image: watchImage.", Log.DEBUG );
				newRow.setImage( watchImage );
			}
        }
    	scheduleTable.setSelection( selectedIndex );
        setEditScheduleButtonsEnabledState();
        
        // Updates the default composite (at the bottom) if need be.
        defaultSpeedsImageComposite.redraw();
    }
    
    private boolean isActive( Schedule t )
    {
    	//Log.println( "SpeedSchedulerView.isActive( " + t + " )", Log.DEBUG );
    	//Log.println( "  currently active schedules:" + activeSchedules, Log.DEBUG );
    	if( null == t ) {
    		//Log.println( "  t is null, returning false.", Log.DEBUG );
    		return false;
    	}
    	synchronized( activeSchedules ) {
    		if( null == activeSchedules ) 
        		activeSchedules = new Vector(0);
    		Iterator i = activeSchedules.iterator();
    		while( i.hasNext() ) {
    			//Log.println( "Comparing schedules..." );
    			Schedule activeSchedule = (Schedule) i.next();
    			if( t.equals( activeSchedule ) ) {
    				//Log.println( "   Yes active.", Log.DEBUG );
    				return true;
    			}
    		}
    	}
    	//Log.println( "   Not active.", Log.DEBUG );
    	return false;
    }

    /**
     * Draws a (not so) friendly error message dialog.
     * @param msg The message to display in the dialog.
     */
    private void errorMessageBox( String msg )
    {
    	Log.println( "SpeedSchedulerView.errorMessageBox()", Log.DEBUG );
        MessageBox messageBox = new MessageBox( shell, SWT.ICON_ERROR );
        messageBox.setMessage( msg );
        messageBox.setText( "Error" );
        messageBox.open();
    }

	/**
	 * Called by the SpeedSchedulerThread to indicate that it has selected a new schedule. Note
	 * that this is called from a non-SWT thread, so we take precautions to ensure proper thread
	 * access.
	 * 
	 * @see speedscheduler.ScheduleSelectionChangeListener#scheduleSelectionChanged(speedscheduler.Schedule)
	 */
	@Override
	public void scheduleSelectionChanged(Vector newActiveSchedules )
	{
		Log.println( "SpeedSchedulerView.scheduleSelectionChanged( " + newActiveSchedules + " )", Log.INFO );
		//Iterator i = schedules.iterator();
		//while( i.hasNext() )
			//Log.println( "  New schedule selected: " + ((Schedule) i.next() ), Log.DEBUG );
		synchronized( activeSchedules ) {
			Log.println( "  Old activate schedules: " + activeSchedules, Log.DEBUG );
			activeSchedules = newActiveSchedules;
			Log.println( "  Newly activated schedules: " + activeSchedules, Log.DEBUG );
		}
		
		if( null == display ) {
			Log.println( "  Display is null, not redrawing schedules.", Log.DEBUG );
		} else {
			Log.println( "  Redrawing schedules...", Log.DEBUG );
			display.asyncExec( new Runnable() { @Override
			public void run() {
				refeshScheduleTable( );
			}});
		}
	}
	
	  boolean isCreated = false;
	  @Override
	  public boolean eventOccurred(UISWTViewEvent event) {
	    switch (event.getType()) {
	      case UISWTViewEvent.TYPE_CREATE:
	        if (isCreated) // Comment this out if you want to allow multiple views!
	          return false;

	        isCreated = true;
	        break;

	      case UISWTViewEvent.TYPE_DESTROY:
	        delete(); // Remove if not defined
	        isCreated = false;
	        break;

	      case UISWTViewEvent.TYPE_INITIALIZE:
	        initialize((Composite)event.getData());
	        break;

	      case UISWTViewEvent.TYPE_REFRESH:
	        refresh(); // Remove if not defined
	        break;
	    }

	    return true;
	  }
	
}
