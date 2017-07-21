package speedscheduler;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
//import org.eclipse.swt.events.ModifyEvent;
//import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.*;

/**
 * Draws a UI for editing transfer rates: max upload speed, max download
 * speed, and whether transfers should be paused.
 */
class TransferConfigurationComposite extends Composite
{
    private IntegerInput maxUploadRateText;
    private IntegerInput maxDownloadRateText;
    private Button pauseDownloadsCheckbox, pauseSeedsCheckbox;
    
    private String maxUploadRateCaption = "Maximum upload speed: ";
    private String maxDownloadRateCaption = "Maximum download speed: ";
    private String pauseDownloadsCaption = "Pause downloads";
    private String pauseSeedsCaption = "Pause seeds (completed downloads)";
    
    private int inputWidth = 30;
    private boolean categorySelection[] = {false,false};
    
    /**
     * Used to draw the SpeedConfigurationChooser in the plugin view. 
     */
    public TransferConfigurationComposite( Composite parent )
    {
    	this( parent, 0, 0, false, false);
    }

    /**
     * Create a new composite and populate it with initial default values.
     * @param parent
     * @param maxUploadRate
     * @param maxDownloadRate
     * @param pauseDownloads
     * @param pauseSeeds
     * @param categorySelection
     */
    public TransferConfigurationComposite( Composite parent, int maxUploadRate, int maxDownloadRate, boolean pauseDownloads, boolean pauseSeeds)
    {
        super( parent, parent.getStyle() );
        Log.println( "TransferConfigurationComposite.construct() " + maxUploadRate +", " + maxDownloadRate, Log.DEBUG );
        
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        setLayout( layout );
        
        Log.println( "  Drawing the checkbox and input boxen.", Log.DEBUG );
        
        pauseDownloadsCheckbox = new Button( this, SWT.CHECK );
        pauseDownloadsCheckbox.setText( pauseDownloadsCaption );
        pauseDownloadsCheckbox.setSelection( pauseDownloads );
        new Label( this, this.getStyle() ).setText("");
        new Label( this, this.getStyle() ).setText("");
       
        pauseSeedsCheckbox = new Button( this, SWT.CHECK );
        pauseSeedsCheckbox.setText( pauseSeedsCaption );
        pauseSeedsCheckbox.setSelection( pauseSeeds );
        new Label( this, this.getStyle() ).setText("");
        new Label( this, this.getStyle() ).setText("");
        
        Listener checkBoxListener = new Listener() {
    		@Override
		    public void handleEvent(Event e ) {
    			if( pauseDownloadsCheckbox.getSelection() && pauseSeedsCheckbox.getSelection()) {
    				maxUploadRateText.setEnabled( false );
    				maxDownloadRateText.setEnabled( false );
    			} else if( pauseDownloadsCheckbox.getSelection()) {
    				maxDownloadRateText.setEnabled( false );
    				maxUploadRateText.setEnabled( true );
    			} else {
    				maxUploadRateText.setEnabled( true );
    				maxDownloadRateText.setEnabled( true );
    			}
			}
		};
        pauseDownloadsCheckbox.addListener( SWT.Selection, checkBoxListener );
        pauseSeedsCheckbox.addListener( SWT.Selection, checkBoxListener );

        new Label( this, this.getStyle() ).setText( "    " + maxUploadRateCaption );
        maxUploadRateText = new IntegerInput( this, SWT.BORDER );
        maxUploadRateText.setValue( maxUploadRate );

        GridData gridData = new GridData();
        gridData.widthHint = inputWidth;
        maxUploadRateText.setLayoutData( gridData );
        new Label( this, this.getStyle() ).setText( "kbytes/sec (for unlimited, use 0)" );
        
        new Label( this, this.getStyle() ).setText( "    " + maxDownloadRateCaption );
        maxDownloadRateText = new IntegerInput( this, SWT.BORDER );
        maxDownloadRateText.setValue( maxDownloadRate );
        Listener downloadRateLimitListener = new Listener() {
			@Override
			public void handleEvent(Event e ) {
				limitDownloadRateIfNeeded();
			} 
        };
        maxDownloadRateText.addListener( SWT.FocusOut, downloadRateLimitListener );
        maxUploadRateText.addListener( SWT.FocusOut, downloadRateLimitListener );
        
        maxUploadRateText.setEnabled( true );
        maxDownloadRateText.setEnabled( true );
        if( pauseDownloads )
        	maxDownloadRateText.setEnabled( false );
        if( pauseSeeds && pauseDownloads ) {
        	maxUploadRateText.setEnabled( false );
        	maxDownloadRateText.setEnabled( false );
        }
        
        GridData gridData2 = new GridData();
        gridData2.widthHint = inputWidth;
        maxDownloadRateText.setLayoutData( gridData2 );
        new Label( this, this.getStyle() ).setText( "kbytes/sec (for unlimited, use 0)" );
        if( pauseDownloads )
        	maxDownloadRateText.setEnabled( false );
        
        Log.println( "   Done with TransferConfigurationComposite.", Log.DEBUG );
    }     
    
    /**
     * Checks the upload rate and sees if it violates the &lt;5 rule. If so,
     * limit the downloads to 2*upload.   
     *
     */
    private void limitDownloadRateIfNeeded()
    {
    	int upRate =  maxUploadRateText.getValue();
    	int downRate = maxDownloadRateText.getValue();
    	if( upRate < 5 && upRate != 0 && 
				( downRate > upRate * 2 || 
			      downRate == 0 ) )
			maxDownloadRateText.setValue( upRate * 2 );
    }
    
    /**
     * Gets the upload rate entered by the user.
     * @return The upload rate
     */
    public int getMaxUploadRate()
    {
    	return maxUploadRateText.getValue();        
    }
    
    /**
     * Gets the download rate entered by the user.
     * @return The download rate
     */
    public int getMaxDownloadRate()
    {
    	return maxDownloadRateText.getValue();        
    }
    
    /**
     * Gets whether the user has indicate that downloading torrents should be paused.
     * @return True if downloads should be paused, false if not.
     */
    public boolean areDownloadsPaused()
    {
    	return pauseDownloadsCheckbox.getSelection();    	
    }
    /**
     * Gets whether the user has indicate that seeding torrents should be paused.
     * @return True if seeds should be paused, false if not.
     */
    public boolean areSeedsPaused() 
    {
    	return pauseSeedsCheckbox.getSelection();
    }
}
