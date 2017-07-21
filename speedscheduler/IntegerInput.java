package speedscheduler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * A convenient widget input box for inputting integers. Users cannot input
 * anything but 0-9 into this baby.
 */
public class IntegerInput
{
	private Text input;
	private IntegerVerifyListener listener;

	/**
	 * Creates a new IntegerInput widget, embedded in the specified parent
	 * Composite, with the specified style.
	 * 
	 * @param parent The composite to put this widget in.
	 * @param style The style with which to draw this widget
	 * @see SWT
	 */
	public IntegerInput(Composite parent, int style)
	{    		
		Log.println( "IntegerInput.constructor()", Log.DEBUG );
		input = new Text( parent, style | SWT.SINGLE );
		input.setText( "0" );
		listener = new IntegerVerifyListener();
		input.addVerifyListener( listener );
		input.addListener( SWT.FocusIn, new Listener() {
			@Override
			public void handleEvent(Event arg0 ) {
				// Highlight the whole string on focus
				input.setSelection( 0, input.getText().length() );
			} 
		});
		input.addListener( SWT.FocusOut, new Listener() {
			@Override
			public void handleEvent(Event arg0 ) {
				if( input.getText().trim().equals( "" ) )
					input.setText( "0" );
			} 
		});
	}
	
	/**
	 * Gets the value entered by the user
	 * @return The integer value entered by the user.
	 */
	public synchronized int getValue()
	{
		Log.println( "IntegerInput.getValue()", Log.DEBUG );
		if( input.getText().trim().length() == 0 ) {
			return 0;
		}
		
		try { 			
			return Integer.parseInt( input.getText() );
		} catch( NumberFormatException e ) {
			Log.println( "Bad number format on IntgerInput: " + e.getMessage(), Log.ERROR );
			// This should never happen.
			input.setText( "0" );
			input.setSelection( 0, input.getText().length() );
			return 0;
		}
	}
	
	/**
	 * Sets the displayed value for the widget.
	 * @param newValue The value to display.
	 */
	public void setValue( int newValue )
	{
		//Log.println( "IntegerInput.setValue()", Log.DEBUG );
		//try{ throw ( new Exception( "Let's see who's calling me." ) ); }
		//catch( Exception e ) { Log.printStackTrace(e, Log.DEBUG ); }
		input.setText( ""+newValue );
	}
	
	/**
	 * Adds a ModifyListener to this inptu box, such that every time a new
	 * character is inputted, this event will fire.
	 * @param listener The ModifyListener to notify when the event happens.
	 * @see org.eclipse.swt.events.ModifyListener
	 */
    public void addModifyListener( ModifyListener listener )
    {
    	input.addModifyListener( listener );
    }    
    
    /**
     * Sets the layoutData for this widget.
     * @see org.eclipse.swt.widgets.Control.setLayoutData
     * @param layoutData The layoutData to set.
     */
    public void setLayoutData( Object layoutData )
    {
    	input.setLayoutData( layoutData );
    }
    
    /**
     * Enables/disables this widget.
     * @param enabled Specify true to enable this widget (allow input), and false to
     *   		disalbe this widget (disallow input, and turn it gray).
     */
    public void setEnabled( boolean enabled )
    {
    	input.setEnabled( enabled );
    }
    
    /**
     * Sets the width and height of the widget (only if your layout manager respects this).
     * @param width The new width.
     * @param height The new height.
     */
    public void setSize( int width, int height )
    {
    	input.setSize( width, height );
    }
    
    public void addListener( int type, Listener listener )
    {
    	input.addListener( type, listener );
    }
    
    private class IntegerVerifyListener implements VerifyListener
	{
		@Override
		public void verifyText(VerifyEvent e )
		{
			Log.println( "", Log.DEBUG );
			Log.println( "IntegerVerifyListener.verifyText()", Log.DEBUG );
			Log.println( "  Time:" + e.time, Log.DEBUG );
			Log.println( "  this.input:" + input.hashCode(), Log.DEBUG );
			Log.println( "  Widget: " + e.widget, Log.DEBUG );
			Log.println( "  Text: \"" + e.text + "\"", Log.DEBUG );
			Log.println( "  Start:" + e.start, Log.DEBUG );
			Log.println( "  End:" + e.end, Log.DEBUG );
			Log.println( "  Mask:" + e.stateMask, Log.DEBUG ); 
			Log.println( "  Character:" + e.character, Log.DEBUG );
			Log.println( "  Keycode:" + e.keyCode, Log.DEBUG );
			System.err.flush();
			
			String text = e.text;
			char[] chars = new char[text.length()];
			text.getChars(0, chars.length, chars, 0);
			for (int i = 0; i < chars.length; i++) {
				if ( ! ( '0' <= chars[i] && chars[i] <= '9' ) ) {
					e.doit = false;
					return;
				}
			}
		}
	}
}