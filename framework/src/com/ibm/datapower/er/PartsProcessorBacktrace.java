/**
* Copyright 2014-2020 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package com.ibm.datapower.er;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Class for submitting the backtrace to releng's server and processing the backtrace
 * @author alex
 *
 */
public class PartsProcessorBacktrace implements IPartsProcessor{

    /**
     * Default constructor
     * loads the lists of sidecars from the properties file, connects to dp-releng
     */
    public PartsProcessorBacktrace() {
        try{           
            sidecarNames.load(new FileInputStream("properties/sidecar.properties"));
            configproperties.load(new FileInputStream("properties/config.properties"));
        } catch (FileNotFoundException e) {            
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Prepares the URLConnection instance.
     * @param mimePart The MIME part of the backtrace.  Used for reading any information
     * 				   that is useful in preparing the connection instance.
     * 
     * @throws IOException if connection cannot be made
     */
    private void prepConnection(IPartInfo mimePart) throws IOException
    {
        url = new URL(configproperties.getProperty("decodeurl"));
        conn = url.openConnection();
        conn.setDoOutput(true);
        
        ErrorReportDetails erDetails = mimePart.getErrorReportDetails();
        
        // Read the firmware version so that we can discern whether or not we're dealing with a modern or a legacy backtrace.
        Matcher versionRegex = Pattern.compile("\\.\\d\\.\\d\\.\\d\\.\\d").matcher((CharSequence) erDetails.getVersion());
        versionRegex.find();
        String versionMajorStr = versionRegex.group().substring(1, 2);
        int versionMajor = Integer.parseInt(versionMajorStr);
        boolean isLegacy = versionMajor < 5;
       
        HashMap<String, String> headers = new HashMap<String, String>();
        
        // If we're dealing with a pre 5.x.x.x error report, the backtrace
        // included in it is in the legacy format.  Set a header so that
        // releng CGI knows to use the legacy decoder on it.
        if ( isLegacy ) headers.put("BT_LEGACY", "1");
                
        if ( headers.size() > 0 )
		{
		    Set<String> keys = headers.keySet();
		    for (String k : keys)
		    {
		    	conn.setRequestProperty( k, headers.get(k) );
		    }
		}

        Logger.getRootLogger().info("Connecting to " + url.getHost());
    }
    
    /**
     * process the backtrace mimepart
     * 
     * @param mimePart the mimePart 
     * @param writer the parsed error report output stream writer
     */
    public void process(IPartInfo mimePart, PrintWriter writer) throws IOException 
    {
    	
    	InputStream in = null;
        BufferedReader bis = null; 
        OutputStreamWriter outWriter = null;
        ByteArrayOutputStream baos = null;
    	
        try 
    	{
            // Initialize the HTTP connection instance.
            prepConnection(mimePart);
    		
            // Open a reader to read the backtrace from MIME part
            // and a writer to write it as HTTP message payload.
    		in = mimePart.getBodyStream();
            bis = new BufferedReader(new InputStreamReader(in)); 
            outWriter = new OutputStreamWriter(conn.getOutputStream());
            
            //the byte arrayoutputstream to hold the original backtrace
            baos = new ByteArrayOutputStream();
            String nextLine;
            
            // Read each line and attempt to detect whether or not it represents a sidecar backtrace header.
            while((nextLine = bis.readLine()) != null)
            {
            	// Sidecar backtrace headers aren't as straightforward for the releng CGI scripts
            	// to understand, as they are not in a standard format.  The isSidecar method
            	// provides as its output, a standard-like header that simplifies the work that
            	// releng needs to do to figure out how to decode the backtrace.  This may or
            	// may not apply to the modern style.
                String newPlatform = isSidecar(nextLine);                
                if(newPlatform != null)
                {
                    nextLine = processERInfo(newPlatform, mimePart);                    
                }

                //append it to the outputstreamwriter
                outWriter.write(nextLine + "\n");                
                baos.write(nextLine.getBytes());
                baos.write('\n');
            }
            
            Logger.getRootLogger().info("Writing to " + url.getHost());
            
            outWriter.flush();
            String result = getResult();
            
            if(result.equals("Symbol file not found - backtrace not decoded\n"))
            {
                //writer.write(baos.toString());
                result = result.concat(baos.toString());
            }
            
            // Write the output to the running parsed error report.
            writer.write(result);
        } 
    	catch (IOException e) 
    	{
            e.printStackTrace();
            System.out.println("exception");
        }
    	finally
    	{
    		// Always close streams in a finally block.
    		if (in != null) in.close();
    		if (bis != null) bis.close();
            if (outWriter != null) outWriter.close();    		
    	}
    }
    

    /**
     * checks a string against the Sidecar properties file. if properties file key matches, then it returns the sidecar name that will show up in releng
     * 
     * @param stringToCheck the string which should match a global_backtrace_header of a sidecar.
     * @return the name that erbacktraceframework will be able to use to find the symbols file, null if no matching sidecar was found
     */
    private String isSidecar(String stringToCheck){        
        String sideCarName = null;
        
        StringTokenizer propertyNames = new StringTokenizer(sidecarNames.getProperty("order"),  " ");
        while(propertyNames.hasMoreTokens()){
            String key = propertyNames.nextToken();            
            if(stringToCheck.matches(".*"+sidecarNames.getProperty(key)+".*")){
                sideCarName = key;
                break;    
            }
        }        
        return sideCarName;
    }
    /**
     * Gets the result of submitting the backtrace to dpreleng as a string
     * 
     * @return resulting backtrace, or if the backtrace could not be decoded, returns: "Symbol file not found"
     * @throws IOException 
     */
    private String getResult() throws IOException{
        int ch;
        String sIn = new String("Symbol file not found - backtrace not decoded\n");
        String sRes = new String("");
        
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
        
        
        InputStream testResult = conn.getInputStream();
        Logger.getRootLogger().info("Obtaining result");
        
        for (ch = testResult.read(); ch >= 0; ch = testResult.read()) {
            sRes += (char) ch;
        }
        Logger.getRootLogger().info("Finished obtaining result");
        if (sRes.indexOf("symbols are 0 bytes long") >= 0)
        {   
            testResult.close();
            return sIn;
            
        }else{
            // no error detected, return decoded data from URL
            testResult.close();
            //remove first line containing location of temp file
            
            int startIndex = sRes.indexOf("/tmp");            
            int endIndex = sRes.indexOf('\n', startIndex);
            sRes = sRes.substring(endIndex+1);
            
            return sRes;
        }
    }
    
    /**
     * Uses the mimePart to build a string that will produce the correct URL for the the sidecar symbosl file
 
     * @param newPlatform the name of the sidecar as it shows up in the filename of the sidecar symbols file
     * @param mimePart
     * @return the line that dp-releng site can correctly process to get the symbols file url
     */
    private String processERInfo(String newPlatform, IPartInfo mimePart){
        ErrorReportDetails erdetails = mimePart.getErrorReportDetails();
        String buildInfo = "";
        buildInfo += erdetails.getVersion();
        buildInfo += " Platform ";
        buildInfo += newPlatform;
        buildInfo += " Build ";
        buildInfo += erdetails.getBuild();
        buildInfo += " on ";
        buildInfo += erdetails.getBuildDate();
                
        return buildInfo;
    }
    

    URL url;
    URLConnection conn;
    Properties sidecarNames = new Properties();
    Properties configproperties = new Properties();
    
}
