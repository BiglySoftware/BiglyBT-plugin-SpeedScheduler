package speedscheduler;

import java.util.Vector;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.torrent.TorrentAttribute;

import com.biglybt.core.tag.Tag;
import com.biglybt.core.tag.TagManagerFactory;
import com.biglybt.core.tag.TagType;

/**
 * A widget for choosing a category based schedule. 
 */
class CatComposite extends Composite
{   
	private boolean selection[];
	private String category;
	private static final String DEFAULT_CAT = "Uncategorized";
	private static final String DEFAULT_TAG = "";
	protected TorrentAttribute torrent_categories;
	protected Vector torrent_cat_names;

	private Button disable;
	private Combo catSelectorCombo, operationSelectorCombo;

	/**
	 * Create a new CatComposite and place it in the specified parent Composite.
	 * @param parent The parent composite to embed this widget in.
	 */
	/*
	public CatComposite( Composite parent)
	{
		this( parent, null, null);
	}
	*/
	
	/**
	 * Creates a new CatComposite given a parent and default 
	 * selection (In/Not In) and the Category it applies to.
	 * @param parent The parent Composite in which to place this widget.
	 * @param selection[] The current selection (In/Not In/Disabled), Note Disabled represented by {fasle,false}. 
	 * @param defCategory The category chosen to represent the schedule.
	 */
	public CatComposite( Composite parent, boolean use_tags, boolean selection[], String defCategory)
	{		
		super( parent, SWT.NONE );
		
		Log.println( "CatComposite.construct()", Log.DEBUG );

		GridLayout Gridlayout = new GridLayout();
		Gridlayout.numColumns = 5;
		setLayout( Gridlayout );

		if( null == selection )
			selection = new boolean[] {false,false};

		if(defCategory == null)
			defCategory = use_tags?DEFAULT_TAG:DEFAULT_CAT;

		this.selection = selection;
		this.category = defCategory;

		//Add User Interface to the screen

		disable = new Button( this, SWT.CHECK );
		disable.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event ) {
				handleCatClick();
			}});
		disable.setSelection( this.selection[0] || this.selection[1] );
		new Label( this, this.getStyle() ).setText("Apply this schedule by " + (use_tags?"tag":"category" ));
		new Label( this, this.getStyle() ).setText("");
		new Label( this, this.getStyle() ).setText("");
		new Label( this, this.getStyle() ).setText("");
		new Label( this, this.getStyle() ).setText("");

		new Label( this, this.getStyle() ).setText( "Only apply this schedule to torrents" );
		operationSelectorCombo = new Combo( this, SWT.READ_ONLY | SWT.DROP_DOWN);

		operationSelectorCombo.add( "not in" );
		operationSelectorCombo.add( "in" );

		if( this.selection[1] )
			operationSelectorCombo.select(1);
		else
			operationSelectorCombo.select(0);

		new Label( this, this.getStyle() ).setText( "this " + (use_tags?"tag":"category" ) + ":" );
		catSelectorCombo = new Combo( this, SWT.READ_ONLY | SWT.DROP_DOWN );

		if(selection[0] || selection[1])
		{
			catSelectorCombo.setEnabled(true);
			operationSelectorCombo.setEnabled(true);
		}
		else
		{
			catSelectorCombo.setEnabled(false);
			operationSelectorCombo.setEnabled(false);
		}
		
		PluginInterface pluginInterface = SpeedSchedulerPlugin.getInstance().getAzureusPluginInterface();
		TorrentAttribute[] tas = pluginInterface.getTorrentManager().getDefinedAttributes();

		for (int i = 0; i < tas.length; i++) {

			if (tas[i].getName() == TorrentAttribute.TA_CATEGORY) {
				torrent_categories = tas[i];
				break;
			}
		}

		String[] x;
		
		if ( use_tags ){
			
			java.util.List<Tag> tags = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL ).getTags();
			
			x = new String[ tags.size()];
			
			for ( int i=0;i<x.length;i++){
				
				x[i] = tags.get(i).getTagName( true );
			}
		}else{
			
			x = torrent_categories.getDefinedValues();
		}
		
		torrent_cat_names = new Vector();

		int SetIndex;;
		
		if ( use_tags ){
			
			SetIndex = -1;

		}else{
			catSelectorCombo.add( DEFAULT_CAT );
			torrent_cat_names.add( DEFAULT_CAT );
			
			//Add Default Category and set this as the index.

			SetIndex = 0;
		}
		
		//Add rest of Azureus Categorys
		for (int i =0;i<x.length; i++)
		{
			Log.println( "CatComposite.construct()-TorrentCat: "+x[i], Log.DEBUG );
			catSelectorCombo.add( x[i] );
			torrent_cat_names.add( x[i] );

			//If we find a matching category, change the index
			if(category.compareTo(x[i]) == 0)
				SetIndex = catSelectorCombo.getItemCount()-1;
		}

		//Get the Combo box to have the correct item selected
		
		if ( SetIndex >= 0 ){
		
			catSelectorCombo.select(SetIndex);
		}
		
		// Change the variables when selection change happens.
		SelectionAdapter catComboSelectionAdapter = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event )
			{
				int index = ((Combo)event.widget).getSelectionIndex();
				category = (String)torrent_cat_names.elementAt( index );
			}
		};
		catSelectorCombo.addSelectionListener( catComboSelectionAdapter );

		SelectionAdapter operationComboSelectionAdapter = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event )
			{
				handleCatClick();
			}
		};
		operationSelectorCombo.addSelectionListener( operationComboSelectionAdapter );
	}

	/**
	 * Updates the variables ready to be returned.
	 */
	private void handleCatClick()
	{
		if( disable.getSelection() && operationSelectorCombo.getSelectionIndex() == 0 )
		{
			selection[0] = true;
			selection[1] = false;
		}
		else if ( disable.getSelection() && operationSelectorCombo.getSelectionIndex() == 1 )
		{
			selection[0] = false;
			selection[1] = true;
		}
		else
		{
			selection[0] = false;
			selection[1] = false;
		}

		if(selection[0] || selection[1])
		{
			catSelectorCombo.setEnabled(true);
			operationSelectorCombo.setEnabled(true);
		}
		else
		{
			catSelectorCombo.setEnabled(false);
			operationSelectorCombo.setEnabled(false);
		}
	}

	/**
	 * Gets the selected boxes chosen by the user.
	 * @return The selected boxes chosen by the user.
	 */
	public boolean[] getSelection()
	{
		return selection;
	}

	/**
	 * Gets the category chosen by the user.
	 * @return The category chosen by the user.
	 */
	public String getCategory()
	{
		return category;
	}
}
