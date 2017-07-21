package speedscheduler;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import java.io.*;

/**
 * This class allows us to conveniently draw images on SWT widgets (like composites).
 */
public class ImageUtils
{
	/**
	 * Constructs and returns an Image object based on the specified
	 * Display object, Class that will use the image, and location
	 * of the image.  
	 * @param display The SWT Display
	 * @param classIn The class that will make use of the object (use this.getClass())
	 * @param location The URL of the image relative to the specified class, ie
	 * 			"images/clock.png". This may be inside a jar. That's okay, if it's
	 * 			in the same jar as the specified class.
	 * @return The Image object.
	 */
    public static Image getImage( Display display, Class classIn, String location )
    {
    	Log.println( "ImageUtils.getImage( " + display + ", " + classIn + ", " + location + " ) ", Log.DEBUG );
    	if( null == display || null == classIn || null == location || location.trim().length() == 0 ) 
    		return null;
        InputStream is = classIn.getResourceAsStream( location );
        if( null == is )
        	return null;
        Image img = new Image( display, is );
        return img;
    }

    /**
     * No instantiations.
     */
    private ImageUtils()
    {
    }
}
