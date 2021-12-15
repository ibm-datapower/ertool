/*
 * IBM Confidential
 * OCO Source Materials
 * IBM WebSphere DataPower Appliances
 * Copyright IBM Corporation 2010
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 */

package internal;
		 
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.*;
import org.apache.logging.log4j.*;
import com.ibm.datapower.er.ErrorReportDetails;
import com.ibm.datapower.er.IPartInfo;
import com.ibm.datapower.er.mgmt.ERMgmtException;
import com.ibm.datapower.er.mgmt.ERTrustManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;

/*
 * Invoke backtrace decoder via HTTP
 */
public class ERBacktrace {
    

    private SSLSocketFactory createCustomSSLSocketFactory() throws ERMgmtException {
        SSLSocketFactory sslSocketFactory = null;
        try{
            TrustManager[] tm = null;
            ERTrustManager myTrustManager = new ERTrustManager(true);
            tm = new TrustManager[] { myTrustManager };
        
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, tm, null);
            sslSocketFactory = sslContext.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        } catch (GeneralSecurityException e) {

        }

        
        return sslSocketFactory;    
    }
    /**
     * Prepares the URLConnection instance.
     * @param mimePart The MIME part of the backtrace.  Used for reading any information
     * 				   that is useful in preparing the connection instance.
     * 
     * @throws IOException if connection cannot be made
     */
    private void prepConnection() throws IOException
    {
        url = new URL(configproperties.getProperty("decodeurl"));
        conn = url.openConnection();
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(300000);
        rndBoundary = (int) (Math.random() * 1000);
        /*
        HashMap<String, String> headers = new HashMap<String, String>();
 
        if ( headers.size() > 0 )
		{
		    Set<String> keys = headers.keySet();
		    for (String k : keys)
		    {
		    	conn.setRequestProperty( k, headers.get(k) );
		    }
		}*/
        conn.setRequestProperty("Content-Type" ,"multipart/form-data; boundary=---------------------------" + rndBoundary);
        LogManager.getRootLogger().info("Connecting to " + url.toString());
    }
    
    /**
     * process the backtrace mimepart
     * 
     * @param mimePart the mimePart 
     * @param writer the parsed error report output stream writer
     */
    public InputStream process(InputStream in) throws IOException 
    {
        BufferedReader bis = null; 
        OutputStreamWriter outWriter = null;
        
        InputStream res = in;
        try 
    	{
            // Initialize the HTTP connection instance.
        	try {
        	createCustomSSLSocketFactory();
        	}catch(Exception ex) {
        		
        	}
            prepConnection();
    		
            // Open a reader to read the backtrace from MIME part
            // and a writer to write it as HTTP message payload.
            outWriter = new OutputStreamWriter(conn.getOutputStream());


            outWriter.write("-----------------------------" + rndBoundary + "\n");
            outWriter.write("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"Backtrace\"\n");
            outWriter.write("Content-Type: application/octet-stream\n\n");

            String input = IOUtils.toString(in);
            
            outWriter.write(input.toString() + "\n");
            outWriter.write("-----------------------------" + rndBoundary + "--\n");
            
            LogManager.getRootLogger().info("Writing to " + url.toString());
            
            outWriter.flush();
            outWriter.close();
            String result = getResult(conn);
            
            
            String test = "Output/decoded file -- <a href='";
            int idxOutput = result.indexOf(test);
            String result2 = result;
            if(idxOutput > 0)
            {
            	int startIdx = idxOutput + test.length();
            	int endIdx = result.indexOf("'",startIdx+1);
            	if(endIdx > startIdx) {
            		String newURL = result.substring(startIdx, endIdx);

            		String decodeURL = configproperties.getProperty("decodeurl");
            		decodeURL = decodeURL.substring(0,decodeURL.lastIndexOf("/") + 1);
            		String finalURL = decodeURL + newURL;
            		URL newurl = new URL(finalURL);
                    URLConnection newurlcon = newurl.openConnection();
                    newurlcon.setDoInput(true);
                    newurlcon.setConnectTimeout(60000);
                    newurlcon.setReadTimeout(60000);
                    InputStream isres = newurlcon.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(isres));
                    StringBuffer sb = new StringBuffer();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    res = new ByteArrayInputStream(sb.toString().getBytes());
            	}
            }
        } 
    	catch (IOException e) 
    	{
            e.printStackTrace();
        }
    	finally
    	{
    		// Always close streams in a finally block.
    		if (bis != null) bis.close();
            if (outWriter != null) outWriter.close();    		
    	}
        
        return res;
    }
    
    /**
     * Gets the result of submitting the backtrace to dpreleng as a string
     * 
     * @return resulting backtrace, or if the backtrace could not be decoded, returns: "Symbol file not found"
     * @throws IOException 
     */
    private String getResult(URLConnection urlcon) throws IOException{
        int ch;
        String sIn = new String("Symbol file not found - backtrace not decoded\n");
        
        // The following, or a variation of it, is more efficient, but requires the CGI to return a content
        // length header.  If we do that, we can simply allocate a a char buffer the size of the
        // response and read the entire thing in at once, which is done at the native layer, instead
        // of iteratively in Java.  The current method suffers the from the vector problem as the 
        // compiler has no way of predicting the size of the output, so it replaces instances like
        // this with a StringBuffer whose size is scaled dynamically.  This means lots of time used
        // on reallocating, deleting, and copying operations going on under the hood, back and forth
        // between the JVM and the native layer.
        //BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        //int len = conn.getContentLength();
        //char[] cBuff = new char[len];
        //reader.read(cBuff, 0, len);
        //String sRes = new String(cBuff);
        
        try {
        InputStream testResult = urlcon.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                testResult));
        String sRes = "";
        String line = null;
        while ((line = reader.readLine()) != null) {
            sRes += line + "\n";
        }
        if (sRes.indexOf("symbols are 0 bytes long") >= 0)
        {   
            testResult.close();
            return sIn;
            
        }else{
            // no error detected, return decoded data from URL
            testResult.close();
            return sRes;
        }
        }catch(SocketTimeoutException ste) {
        	return sIn;
        }
    }

    URL url;
    URLConnection conn;
    Properties configproperties = new Properties();
    int rndBoundary = 999;
    /*
     * Read a backtrace input stream and return a decoded input stream.
     */
    public InputStream decode(InputStream in) 
    {
    	String path = "";
		try {
			  path = getClass()
			          .getProtectionDomain()
			          .getCodeSource()
			          .getLocation()
			          .toURI()
			          .getPath();
			  
			  path = path.substring(0, path.lastIndexOf("/"));
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        try {
        configproperties.load(new FileInputStream(path + "/properties/config.properties"));
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
        
		try {
			return process(in);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return in;
		}
    }
}
