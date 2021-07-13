package speedscheduler;

import com.biglybt.pif.Plugin;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginException;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.ui.config.Parameter;
import com.biglybt.pif.ui.config.ParameterListener;

import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;

import com.biglybt.ui.swt.pif.*;

/**
 * The main plugin class for this plugin. Azureus instantiates and 
 * initializes this class, so this class must do all the plugin
 * bootstrapping, including loading the saved user schedules
 * and starting the SpeedSchedulerThread. The file plugin.properties
 * references this class.
 */
public class SpeedSchedulerPlugin implements Plugin
{
    /**
     * Our access to Azureus.
     */
	/** A reference to the Azureus PluginConfig implementation */
    private PluginInterface pluginInterface;
    /** A reference to the Azureus PluginConfig implementation */
    private PluginConfig pluginConfig;
    /** A reference to this singleton */
    private static SpeedSchedulerPlugin speedSchedulerPlugin;
    /** The name of the parameter in the Azureus config that tells us if we are enabled or not. */
    private static final String ENABLED_PARAM = "speedscheduler.enabled";
    /** A reference to the thread that powers this whole shindig */
    private SpeedSchedulerThread speedSchedulerThread;
    
    private static final String MAX_UPLOAD_SPEED_PARAM = PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC;
    private static final String MAX_DOWNLOAD_SPEED_PARAM = PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC;
    private static final String AUTO_SPEED_ENABLED_CONFIG_VAR = "enable";
    private static final String AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR = "maximumUp";
    private static final String AUTO_SPEED_PLUGIN_NAME = "Auto Speed";
    private static final String AUTO_SPEED_WARNED_PARAM = "autospeed.hasbeenwarned";
    private static final String BUILT_IN_AUTO_SPEED_ENABLED_PARAM = PluginConfig.CORE_PARAM_BOOLEAN_AUTO_SPEED_ON;
    private static final String BUILT_IN_AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR = "AutoSpeed Max Upload KBs";

    private static final boolean DEFAULT_USE_TAGS_NOT_CATS	= false;
    private static final boolean DEFAULT_OVERRIDE_FORCED	= false;
    
    

    /**
     * This method is called when the plugin is loaded / initialized
     * In our case, it'll simply store the pluginInterface reference
     * and register our PluginView.
     */
	@Override
	public void initialize(final PluginInterface pluginInterface )
	{
    	speedSchedulerPlugin = this;
        this.pluginInterface = pluginInterface;
        this.pluginConfig = this.pluginInterface.getPluginconfig();

        // Setup the SpeedScheduler config page:
		BasicPluginConfigModel configModel = pluginInterface.getUIManager().createBasicPluginConfigModel("speedscheduler.config.title");

		int i = 0;
		Parameter parameters[] = new Parameter[12];

		// Schedules, sleeping, and stuff:
		parameters[++i] = configModel.addIntParameter2( "thread.sleep.time",
				"speedscheduler.thread.sleep.time", 10000 );
		parameters[i].addListener( new ParameterListener() {
			@Override
			public void parameterChanged(Parameter p ) {
				if( speedSchedulerThread != null )
					speedSchedulerThread.interrupt();
			}
		});
		parameters[++i] = configModel.addIntListParameter2( "time.display",
				"speedscheduler.time.display", new int[] { 12, 24 },
				new String[] { "12-hour (5:00pm)", "24-hour (17:00)" }, 12 );
		parameters[++i] = configModel.addIntParameter2( "minutes.granularity",
				"speedscheduler.minutes.granularity", 15 );
		
		parameters[++i] = configModel.addBooleanParameter2( "use.tags.not.cats",
				"speedscheduler.use.tags.not.cats", DEFAULT_USE_TAGS_NOT_CATS );
		
		parameters[++i] = configModel.addBooleanParameter2( "override.forced",
				"speedscheduler.override.forced", DEFAULT_OVERRIDE_FORCED );

		// Logging:
		parameters[++i] = configModel.addFileParameter2( "log.file",
				"speedscheduler.log.file", "" );
				//getPluginDirectoryName() + File.separator + "SpeedScheduler.log" );
		parameters[i].addListener( new ParameterListener() {
			@Override
			public void parameterChanged(Parameter p ) {
				Log.setFile( getConfigParameter( "log.file", Log.DEFAULT_LOG_FILE ) );
			}
		});
		parameters[++i] = configModel.addIntListParameter2("log.level",
				"speedscheduler.log.level",
				new int[] { Log.DEBUG, Log.INFO, Log.ERROR, Log.NONE },
				new String[] { "Debug", "Information", "Errors", "Nothing" }, Log.ERROR );
		parameters[i].addListener( new ParameterListener( ) {
			@Override
			public void parameterChanged(Parameter p ) {
				Log.setLevel( getConfigParameter( "log.level", Log.ERROR ) );
			}
		});
    	
		// XML Parsing:
		parameters[++i] = configModel.addStringParameter2( "sax.parser",
				"speedscheduler.sax.parser", "" );

//
//		ERROR -> DEBUG
//		
//		
		// Setup the loglevel
    	Log.setLevel( this.getConfigParameter( "log.level", Log.ERROR ) );
    	
    	
		// Launch the polling schedule thread
        speedSchedulerThread = new SpeedSchedulerThread();
        speedSchedulerThread.start();
        
        // Do this AFTER starting the thread!
           pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
               @Override
               public void UIAttached(UIInstance instance) {
	            if (instance instanceof UISWTInstance) {
	                swtInstance = (UISWTInstance)instance;
	                swtInstance.addView(UISWTInstance.VIEW_MAIN, "Speed Scheduler", new SpeedSchedulerView(pluginInterface));
	              }
               }

            @Override
            public void UIDetached(UIInstance instance) {
              if (instance instanceof UISWTInstance) {
                swtInstance = null;
	              ((UISWTInstance) instance).removeViews(UISWTInstance.VIEW_MAIN, "Speed Scheduler");
              }
            }});
    }

	private UISWTInstance swtInstance = null;

	/**
     * Checks if SWT is available on the host system.
     * @return True is SWT is available, false otherwise.
     */
    public boolean isSwtAvailable()
    {
        try {
			Class.forName( "org.eclipse.swt.SWT" );
		} catch (ClassNotFoundException e1) {
			return false;
		}
		return true;
    }

    /**
     * Gets a reference to the Azureus plugin interface.
     * @return the plugin interface
     * 
     */
    public PluginInterface getAzureusPluginInterface()
    {
    	return pluginInterface;
    }
    
    /**
     * If the user has checked the "Enable SpeedSCheduler" in the view,
     * this will return true. Returns false if not. This setting is persistent
     * in the Azureus PluginConfig.
     * @return True if the SpeedScheduler is enabled, false otherwise.
     */
    public boolean isEnabled()
    {
    	return pluginConfig.getPluginBooleanParameter( ENABLED_PARAM );
    }
    
    /**
     * Turns on/off the SpeedScheduler. The setting is persisted in the Azureus
     * PluginConfig.
     * @param enabled True to turn it on, false to turn it off.
     * @see this.isEnabled
     */
    public void setEnabled( boolean enabled )
    {
    	pluginConfig.setPluginParameter( ENABLED_PARAM, enabled );
    	
    	if( false == enabled ) {
    		// Revert to default up/down speeds
    		SchedulePersistencyManager persistencyManager = SchedulePersistencyManager.getInstance();
    		setAzureusGlobalDownloadSpeed( persistencyManager.getDefaultMaxDownloadSpeed() );
    		setAzureusGlobalUploadSpeed( persistencyManager.getDefaultMaxUploadSpeed() );
    	}
    	
    	try {
			pluginConfig.save();
		} catch (PluginException e) {
			e.printStackTrace();
		}
		
		// Wake up the SpeedSchedulerThread so it will re-examine the
		// enabled setting and take action if necessary. 
    	speedSchedulerThread.interrupt();
    }
    
    /**
     * Get the singleton instance of the SpeedSchedulerPlugin.
     * @return The singleton instance of the SpeedSchedulerPlugin object.
     */
    public static SpeedSchedulerPlugin getInstance()
    {
    	return speedSchedulerPlugin; 
    }
    
    /**
    * Gets Azureus' built-in autospeed on/off
    * @see PluginInterface
    * @see PluginConfig
	*/
    public boolean getAzureusBuiltInAutoSpeedEnabled()
    {
    	return pluginConfig.getCoreBooleanParameter( BUILT_IN_AUTO_SPEED_ENABLED_PARAM  );
    }

    /**
     * Sets Azureus' maximum global upload speed.
     * @param newSpeed The new max upload speed.
     * @see PluginInterface
     * @see PluginConfig
     */
    public void setAzureusGlobalUploadSpeed( int newSpeed )
    {
    	pluginConfig.setCoreIntParameter( MAX_UPLOAD_SPEED_PARAM, newSpeed );
    }

    /**
     * Sets Azureus' maximum global download speed.
     * @param newSpeed The new max download speed.
     * @see PluginInterface
     * @see PluginConfig
     */
    public void setAzureusGlobalDownloadSpeed( int newSpeed )
    {
    	pluginConfig.setCoreIntParameter( MAX_DOWNLOAD_SPEED_PARAM, newSpeed );
    }

    /**
     * Sets Azureus' maximum global download speed.
     * @see PluginInterface
     * @see PluginConfig
     */
    public int getAzureusGlobalDownloadSpeed()
    {
    	return pluginConfig.getCoreIntParameter( MAX_DOWNLOAD_SPEED_PARAM );
    }

    /**
     * Gets Azureus' maximum global upload speed.
     * @see PluginInterface
     * @see PluginConfig
     */
    public int getAzureusGlobalUploadSpeed()
    {
    	return pluginConfig.getCoreIntParameter( MAX_UPLOAD_SPEED_PARAM );
    }

    /**
     * @param name The config entry name whose value to fetch.
     * @param defaultValue The value to return if the property does not exist.
     * @return The value of the key.
     */
    public int getConfigParameter( String name, int defaultValue )
    {
    	return pluginConfig.getPluginIntParameter( name, defaultValue );
    }

    public String getConfigParameter( String name, String defaultValue )
    {
    	return pluginConfig.getPluginStringParameter( name, defaultValue );
    }

    public PluginInterface getPluginInterface() {
    	return pluginInterface;
    }

    public boolean
    getUseTagsNotCategories()
    {
    	return( pluginConfig.getPluginBooleanParameter( "use.tags.not.cats", DEFAULT_USE_TAGS_NOT_CATS ));
    }
    
    public boolean
    getOverrideForced()
    {
    	return( pluginConfig.getPluginBooleanParameter( "override.forced", DEFAULT_OVERRIDE_FORCED ));
    }

	/**
	 * Gets the directory where we are installed.
	 * @return The name of the directory.
	 */
	public String getPluginDirectoryName()
	{
		return pluginInterface.getPluginDirectoryName();
	}

	/**
	 * Figures out if the Auto Speed plugin is installed and enabled.
	 * @return true if it is both installed *and* enabled.
	 */
	public boolean isAutoSpeedEnabled()
	{
		/* Plug-in */
    	PluginInterface autoSpeedInterface = getAutoSpeedPluginInterface();
    	if( autoSpeedInterface != null ) {
			PluginConfig autoSpeedConfig = autoSpeedInterface.getPluginconfig();
			Log.println( "Autospeed plug-in enabled: " + autoSpeedConfig.getPluginBooleanParameter( AUTO_SPEED_ENABLED_CONFIG_VAR ), Log.DEBUG );
			if( autoSpeedConfig.getPluginBooleanParameter( AUTO_SPEED_ENABLED_CONFIG_VAR, false ) )
				return true;
		}

		/* Built-in */
		if (getAzureusBuiltInAutoSpeedEnabled()) {
			Log.println( "Built-in auto-speed enabled: ", Log.DEBUG );
			return true;
		}
		else {
			return false;
		}
	}


	/**
	 * Fetches the AutoSpeed plugin interface.
	 * @return A reference to the auto speed plugin interface object.
	 */
	private PluginInterface getAutoSpeedPluginInterface()
	{
		PluginInterface[] interfaces = pluginInterface.getPluginManager().getPluginInterfaces();
		for( int i=0; i<interfaces.length; i++ )
    		if( interfaces[i].getPluginName().equalsIgnoreCase( AUTO_SPEED_PLUGIN_NAME ) )
    			return interfaces[i];
    	return null;
	}

	/**
	 * Sets auto speed's maximum upload setting.
	 * @param speed The speed in Kb/sec.
	 */
	public void setAutoSpeedMaxUploadSpeed( int speed )
	{
		PluginInterface autoSpeedInterface = getAutoSpeedPluginInterface();
		if( autoSpeedInterface != null )
			autoSpeedInterface.getPluginconfig().setPluginParameter( AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR, speed );

		if( getAzureusBuiltInAutoSpeedEnabled()) {
			pluginConfig.setUnsafeIntParameter( BUILT_IN_AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR, speed);
		}
	}

	/**
	 * Gets auto speed's maximum upload setting.
	 * @return The speed in Kb/sec.
	 */
	public int getAutoSpeedMaxUploadSpeed()
	{
		PluginInterface autoSpeedInterface = getAutoSpeedPluginInterface();
		if( autoSpeedInterface != null )
			return autoSpeedInterface.getPluginconfig().getPluginIntParameter( AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR );

		if(getAzureusBuiltInAutoSpeedEnabled())
			return pluginConfig.getUnsafeIntParameter( BUILT_IN_AUTO_SPEED_MAX_UPLOAD_CONFIG_VAR );

		return -1;
	}
	
	/**
	 * Gets effective max upload speed, i.e. normal max upload speed
	 * if no AutoSpeed, else AutoSpeed max upload speed
	 * @return The speed in Kb/sec.
	 */
	public int getEffectiveMaxUploadSpeed()
	{
		if(isAutoSpeedEnabled()) {
			return getAutoSpeedMaxUploadSpeed();
		}
		else {
			return getAzureusGlobalUploadSpeed();
		}
	}

	/**
	 * Sets effective max upload speed, i.e. normal max upload speed
	 * if no AutoSpeed, else AutoSpeed max upload speed
	 */
	public void setEffectiveMaxUploadSpeed(int speed) {
		if(isAutoSpeedEnabled()) {
			setAutoSpeedMaxUploadSpeed(speed);
		}
		else {
			setAzureusGlobalUploadSpeed(speed);
		}
	}
 

	
	/**
	 * Returns true if the user has already been warned about AutoSpeed/SpeedScheduler behavior.
	 */
	public boolean hasUserBeenWarnedAboutAutoSpeed()
	{
		return this.pluginConfig.getPluginBooleanParameter( AUTO_SPEED_WARNED_PARAM );
	}
	
	/**
	 * Stores whether the user has already been warned about AutoSpeed/SpeedScheduler behavior. This
	 * is persistent between application shutdown/startup.
	 */	
	public void setUserHasBeenWarnedAboutAutoSpeed( boolean hasBeenWarned )
	{
		this.pluginConfig.setPluginParameter( AUTO_SPEED_WARNED_PARAM, hasBeenWarned );
	}
}
