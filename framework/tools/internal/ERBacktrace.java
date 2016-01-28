/*
 * IBM Confidential
 * OCO Source Materials
 * IBM WebSphere DataPower Appliances
 * Copyright IBM Corporation 2010
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 */

import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.IOException;

/*
 * Invoke backtrace decoder via HTTP
 */
public class ERBacktrace {

    /*
     * Read a backtrace input stream and return a decoded input stream.
     */
    public static InputStream decode(InputStream in) 
    {
		return in;
    }
}
