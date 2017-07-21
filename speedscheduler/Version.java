package speedscheduler;

/** 
 * Simple class for storing the version of the SpeedScheduler.
 */
class Version
{
    private static int major = 1;
    private static int minor = 7;

    /**
     * Get the printable, readable version string.
     * @return The string
     */
    public static String getVersion()
    {
		return new StringBuffer()
			.append( major )
			.append( "." )
			.append( minor ).toString();
    }
}

