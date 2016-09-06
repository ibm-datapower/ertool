/**
* Copyright 2014-2016 IBM Corp.
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

package com.ibm.datapower.er.mgmt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class provides an object to the XML Management Interface.  
 * <p>
 * This class provides high-level commands to the appliance.
 * 
 * @author Dana Numkena
 * 
 */

public class ERXmlMgmt {
    
    private DeviceProfile mSettings = null;
    private String mDownloadDirectory = null;
    private Logger mLogger = Logger.getLogger("com.ibm.datapower.er.mgmt");

    /**
     * Constructor
     * 
     * @param sIPAddress ip address of the appliance
     * 
     * @param nPort port number of the XML interface of the appliance
     * 
     * @param sUser user name
     * 
     * @param sPassword password
     * 
     * @throws ERMgmtInvalidPortException
     */
    public ERXmlMgmt  (
            String sIPAddress, 
            int nPort, 
            String sUser, 
            String sPassword,
            String sCertificate,
            boolean allowUntrustedCertificates) throws ERMgmtInvalidPortException
    {
        mSettings = new DeviceProfile();
        mSettings.setHostname(sIPAddress);
        mSettings.setPort(nPort);
        mSettings.setUser(sUser);
        mSettings.setPassword(sPassword);
        mSettings.setCertificate(sCertificate);
        mSettings.setAllowUntrustedCert(allowUntrustedCertificates);
    }
    
    /**
     * Constructor
     * 
     * @param sIPAddress ip address of the appliance
     * 
     * @param nPort port number of the XML interface of the appliance
     * 
     * @param sUser user name
     * 
     * @param sPassword password
     * 
     * @throws ERMgmtInvalidPortException
     */
    public ERXmlMgmt  (
            String sIPAddress, 
            String sPort, 
            String sUser, 
            String sPassword,
            String sCertificate) throws ERMgmtInvalidPortException
    {
        mSettings = new DeviceProfile();
        mSettings.setHostname(sIPAddress);
        try {
            mSettings.setPort(Integer.parseInt(sPort));
        } catch (NumberFormatException e ) {
            throw new ERMgmtInvalidPortException("Invalid Port " + sPort + ".  Port must be a number.");
        }
        
        mSettings.setUser(sUser);
        mSettings.setPassword(sPassword);
        mSettings.setCertificate(sCertificate);
    }
    
    /**
     * Sets the download directory.
     * 
     * @param directory the download directory
     */
    public void setDirectory(String directory){
        mDownloadDirectory = directory;
    }

    
    /**
     * SAX parse handler to retrieve the content from get-status 
     * version version element in the request.
     * 
     * @author Dana Numkena
     */
    public static class GetVersionParseHandler extends DefaultHandler {

        private String mVersion = null;
        private boolean mbDPStatus = false;
        private boolean mbVersion = false;
        private boolean mbVersionVersion = false;

        // constructor
        public GetVersionParseHandler() {
            
        }
        
        /**
         * Gets the get-status version version element
         * 
         * @return content from get-status version version element
         */
        public String GetVersionVersion() {
            return mVersion;
        }

        /* (non-Javadoc)
        * @see org.xml.sax.ContentHandler#characters(char[], int, int)
        */
        public void characters(char[] arg0, int start, int length)
            throws SAXException {
            if(mbVersionVersion == true)
            {
                mVersion = new String(arg0, start, length);
            }
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qualifiedName,
            Attributes atts)
            throws SAXException {

            if (qualifiedName.equalsIgnoreCase("dp:status")) {
                mbDPStatus = true;
            }
            else if(qualifiedName.equalsIgnoreCase("Version") && mbDPStatus == true && mbVersion == false)
            {
                mbVersion = true;
            }
            else if(qualifiedName.equalsIgnoreCase("Version") && mbVersion == true && mbVersionVersion == false)
            {
                mbVersionVersion = true;
            }            
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(
            String namespaceURI,
            String localName,
            String qualifiedName)
            throws SAXException {

            if(qualifiedName.equalsIgnoreCase("Version") && mbVersionVersion == true) {
                mbVersionVersion = false;
            }
            else if(qualifiedName.equalsIgnoreCase("Version") && mbVersion == true) {
                mbVersion = false;
            }
            else if (qualifiedName.equalsIgnoreCase("dp:status")) {
                mbDPStatus = false;
            }
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            
        }
    }
    
    /**
     * SAX parse handler for get-file response.
     * <p>
     * This handler decodes the downloaded file from base64 to binary.
     * 
     * @author Dana Numkena
     */
    public static class DownloadParseHandler extends DefaultHandler {

        private Base64.OutputStream mBase64DecodeStream = null;
        private OutputStream mOutStream;

        /**
         * Constructor 
         * 
         * @param out stream pointing to the response
         */
        public DownloadParseHandler(OutputStream out) {
            mOutStream = out;
        }

        /* (non-Javadoc)
        * @see org.xml.sax.ContentHandler#characters(char[], int, int)
        */
        public void characters(char[] arg0, int start, int length)
            throws SAXException {
            if (mBase64DecodeStream != null) {                

                String convert = new String(arg0, start, length);
                try {
                    mBase64DecodeStream.write(
                        convert.getBytes(),
                        0,
                        convert.length());
                } catch (IOException e) {
                    TraceHelper.trace(
                        "Error decoding file content: " + e.getMessage());
                }
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(
            String namespaceURI,
            String localName,
            String qualifiedName)
            throws SAXException {

            if (qualifiedName.equalsIgnoreCase("dp:file")) {
                cleanup();
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qualifiedName,
            Attributes atts)
            throws SAXException {

            if (qualifiedName.equalsIgnoreCase("dp:file")) {
                mBase64DecodeStream =
                    new Base64.OutputStream(mOutStream, Base64.DECODE);
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            cleanup();
        }

        /**
         * Cleanup decoder stream
         */
        public synchronized void cleanup() {
            if (mBase64DecodeStream != null) {
                try {
                    mBase64DecodeStream.flush();
                    mBase64DecodeStream.close();
                } catch (IOException e) {
                    // do nothing
                }
                mBase64DecodeStream = null;
            }
        }
    }
    
    /**
     * Determine if the appliance firmware version equals 
     * the specified V.R.M. or later.
     * 
     * @param nVersion version to check against
     * 
     * @param nRelease release to check against
     * 
     * @param nModificatoin modification to check against
     * 
     * @return boolean true if appliance firmware matches specified V.R.M or later
     * 
     * @throws ERMgmtException
     */
    
    public boolean checkVersionOrLater(
            int nVersion,
            int nRelease,
            int nModification ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an objeect for the soap message
        soma_request = new SoapManagementRequest();
        
        // create a request for firmware version
        soma_request.getStatus("Version");
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("get-status version result="+sResult);
        }
        // if response is not OK
        else if(sResult.equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("get-status version result="+sResult);
        }
        
        // Read the response XML document
        try {
            GetVersionParseHandler get_version_version_parser = new GetVersionParseHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(
                    soma_request.getResponse(),
                    get_version_version_parser);
            
            sResult = get_version_version_parser.GetVersionVersion();
        } catch (Throwable e) {    
            throw new ERMgmtSAXException("Unable to parse get-status verson. " + e);
        }
        
        // extract the version number
        mLogger.info("DataPower Firmware version: " + sResult);
        String sModel = sResult.substring(0, sResult.indexOf(".")-2);
        String sTemp = sResult.substring(sResult.indexOf(".")+1);
        String sVersion = sTemp.substring(0, sTemp.indexOf("."));
        sTemp = sTemp.substring(sTemp.indexOf(".")+1);
        String sRelease = sTemp.substring(0, sTemp.indexOf("."));
        sTemp = sTemp.substring(sTemp.indexOf(".")+1);
        String sModification = sTemp.substring(0, sTemp.indexOf("."));
        
        TraceHelper.trace("Detected Version: " + sVersion + "." + sRelease + "." + sModification);
        TraceHelper.trace("Desired Version: " + nVersion + "." + nRelease + "." + nModification);
        
        // check version or later
        int V = Integer.parseInt(sVersion);
        int R = Integer.parseInt(sRelease);
        int M = Integer.parseInt(sModification);

        boolean isGreater = (V  > nVersion) || 
        (V == nVersion && R  > nRelease) ||
        (V == nVersion && R == nRelease && M >= nModification);
        //check: if isGreater returns false, check if sModel == XE, if so, then switch is greter to true
        if(isGreater == false){
            if(sModel.equalsIgnoreCase("XE")){
                isGreater = true;
            }
        } 
       return isGreater;     
    }
    
    /**
     * Determines if the firmware is 3.8.1 or later
     * 
     * @throws ERMgmtException
     * 
     * @return true if firmware is 3.8.1 or later
     */
    
    public boolean is_3_8_1_or_later() throws ERMgmtException {
        
        return checkVersionOrLater(3,8,1);
        
    }
    
    /**
     * generates an error report.
     * <p>
     * Sends a do-action ErrorReport request to the appliance.  The method
     * will not return until either the error report is generated or 
     * a timeout occurs.
     * 
     * @param bRedirectToDirectory if true, generates report to temporary directory regardless
     * of Error Report Settings and without modifying the settings.
     * 
     * @throws ERMgmtException
     */
    
    public void generateErrorReport(boolean bRedirectToTemporary, boolean generateReport) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request;
        String sTimestamp0 = null;
        String sTimestamp1 = null;
        ERFilestore er = null;
        boolean bPollTemporary = false;            // assume firmware 3.8.1
        String sUploadStatus1 = null;
        ERFailureNotificationStatus fns = null;
        
        // if older firmware
        if(is_3_8_1_or_later() == false) {
            
            // poll temporary
            bPollTemporary = true;
            
        } else {
            
            // poll FNS
            bPollTemporary = false;
        }
        
        // if in temporary directory
        if(bPollTemporary == true) {
            
            // use filestore for timestamping
            er = new ERFilestore(mSettings);
            
            // get file store information of the temporary directory
            er.refresh();
            
            // get timestamp of existing error report
            sTimestamp0 = er.getModified();
            
            TraceHelper.trace("error report filestore timestamp0: " + sTimestamp0);
        } 
        // else error report is going remote
        else {
            // use fns for timestamping
            fns = new ERFailureNotificationStatus(mSettings);
            
            // get fns
            fns.getFailureNotificationStatus();
            
            if(fns.getLatestEntry() != null) {
                sTimestamp0 = fns.getLatestEntry().getDate();
            } else {
                sTimestamp0 = null;
            }
            
            TraceHelper.trace("error report fns timestamp0: " + sTimestamp0);
            
        }
        
        // setup for connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create a soap request
        soma_request = new SoapManagementRequest();

        // set up generate Error Report request
        if ( generateReport )
        {
        	soma_request.doActionErrorReport(bRedirectToTemporary);
        
	        // transmit the request
	        connection.send(soma_request);            
	        
	        // get result of the error report generation
	        sResult = soma_request.getResult();
        }
        
        // check for authentication failure
        if(generateReport && sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("do-action errorreport result="+sResult);
        } 
        // check if response is not OK
        else if(generateReport && sResult.indexOf("OK") == -1)
        {
            throw new ERMgmtBadResponseException("do-action errorreport result=" + sResult);
        }
            
        int nRetries = 0;
        boolean bRetry = true;
        try {
            // while waiting for new error report or time out
            while((bRetry == true) && (nRetries < 60)) {
                
                if(nRetries != 0) {
                    TraceHelper.trace("wait for error report.  Retry = " + nRetries);
                    Thread.sleep(1000);    // wait for one second
                    
                }
                
                if(bPollTemporary == true) {
                    er.refresh();                        // refresh the filestore information
                    sTimestamp1 = er.getModified();        // get the error report time stamp
                } else {
                    fns.getFailureNotificationStatus();                    // refresh fns
                    sTimestamp1 = fns.getLatestEntry().getDate();       // get latest timestamp
                    sUploadStatus1 = fns.getLatestEntry().getUploadStatus();    // get latest status
                }
                
                TraceHelper.trace("error report timestamp " + sTimestamp1);
                
                // if polling temporary
                if(bPollTemporary == true && generateReport == true) {
                    if((sTimestamp1 == null) || (sTimestamp1.equals(sTimestamp0) == false)) {
                        // got error report
                        bRetry = false;
                    }
                } 
                else {
                    TraceHelper.trace("error report upload status " + sUploadStatus1);
                    
                    // if there is a new error report 
                    if(!generateReport || (sTimestamp1 == null) || (sTimestamp1.equals(sTimestamp0) == false)) {
                        
                        // if in-progress
                        if(sUploadStatus1.equalsIgnoreCase("in-progress") == true) {
                            // keep waiting
                        }
                        // else if the new error report succeeded
                        else if(sUploadStatus1.equalsIgnoreCase("success") == true) {
                               // stop waiting
                            bRetry = false;
                        }                         
                        // else check for failure
                        else if(fns.get2ndLatestEntry() != null) {
                            
                            // if initial error report failed and the second attempt failed
                            if(sTimestamp1.equalsIgnoreCase(fns.get2ndLatestEntry().getDate()) == true
                                && sUploadStatus1.equalsIgnoreCase("failure")
                                && fns.get2ndLatestEntry().getUploadStatus().equalsIgnoreCase("failure")) {
                                
                                // no error report and don't wait
                                bRetry = false;
                                
                            }
                        }
                    }
                }
                                
                nRetries++;
            }
        } catch (InterruptedException e) {
            throw new ERMgmtFileException("sleep interrupted: unable to verify if error report is present." + e.getMessage());
        }
            
        // if waiting for five minutes seconds
        if(nRetries == 60) {
            throw new ERMgmtFileException("error report not created from do-action errorreport.");
        }
        
    }
        
    /**
     * Downloads an error report from temporary:///.
     * <p>
     * This method should be called during non-FNS mode.
     * 
     * @return file path of the downloaded error report
     * 
     * @throws ERMgmtException
     */
    
    public String getErrorReport() throws ERMgmtException {
        boolean b3_8_1_or_later = is_3_8_1_or_later();
        String sFilename;
        
           
        // if 3.8.1 or later
        if(b3_8_1_or_later == true) {
                
            // use the 3.8.1 file convention
            sFilename = "error-report.txt.gz";
                
        }
        // if 3.8.0 or earlier
        else {
                
            // use the 3.8.0 file convention
            sFilename = "error-report.txt";
        }
            
        // download the file
        getFile(sFilename, "temporary:///", mDownloadDirectory);       
        
        String sPath = null;
        
        if(mDownloadDirectory != null) {
            sPath = mDownloadDirectory + "/" + sFilename;
        }
        else {
            sPath = sFilename;
        }
                    
        return sPath;
    }
        
    
    /**
     * Downloads multiple error reports from the appliance.
     * <p>
     * The Failure Notification Status(FNS) has precedence.  If there is an FNS,
     * this method will download error reports based on FNS.  If no FNS, 
     * then this method will attempt to get the report from temporary:///
     * <p>
     * If Java 1.4 is running, ERPluginHtml cannot create html versions 
     * of error reports.  If Java 1.4 is running, bLinkToHtml should be set to
     * false.
     * 
     * @param bLinkToHtml if true, create hyperlinks to html version of error reports
     * 
     * @throws ERMgmtException
     */
    
    public void getErrorReports(boolean bLinkToHtml) throws ERMgmtException {
        
        String sFilename;
        boolean bFnsPresent = false;    
        
        ERFailureNotificationStatus fns = new ERFailureNotificationStatus(mSettings);        
        boolean b3_8_1_or_later = is_3_8_1_or_later();
        
        // if 3.8.1 or later
        if (b3_8_1_or_later == true)
        {       
            try {
                // get get-status FailureNoificationStatus
                fns.getFailureNotificationStatus();
            } catch (ERMgmtException e) {
                bFnsPresent = false;
            }
            
            // check if there was an FNS
            bFnsPresent = fns.isPresent();
        }
        
        // if fns present
        if(bFnsPresent == true) {
                                                    
            int i;
            FileWriter fwIndex;
                
            // create index.html file which will contain fns
            File fileIndex = new File(mDownloadDirectory + "/index.html");
            try {
                fwIndex = new FileWriter(fileIndex, false);
            } catch (Throwable e) {
                throw new ERMgmtFileException("Unable to open " + mDownloadDirectory + "/index.html");
            }
            BufferedWriter bwIndex = new BufferedWriter(fwIndex);
            PrintWriter pwIndex = new PrintWriter (bwIndex, true);
            
            // get timestamp
            Date currentDate = new Date();
            String sTimestamp = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL).format(currentDate);
            
            // download fns in html format
            pwIndex.println("<html>");
            pwIndex.println("<head>");
            pwIndex.println("<style type=\"text/css\">");
            pwIndex.println(".style1 { ");
            pwIndex.println("    font-size: x-large;");
            pwIndex.println("}");
            pwIndex.println("</style>");
            pwIndex.println("</head>");
            pwIndex.println("<body>");
            pwIndex.println("<p><span class=\"style1\"><strong>Failure Notification Status</strong></span></p>");
            pwIndex.println("<p>As of " + sTimestamp + "</p>");
            pwIndex.println("<table border=1 style=\"width: 800px\">");
            pwIndex.println("<tr bgcolor=\"A797FF\">");
            pwIndex.println("<th scope=col>Date</th>");
            pwIndex.println("<th scope=col>Reason</th>");
            pwIndex.println("<th scope=col>Update Status</th>");
            pwIndex.println("<th scope=col>Location</th>");
            pwIndex.println("</tr>");
            
            // for each fns element
            for(i = 0; i < fns.size(); i++) {               
    
                pwIndex.println("<tr>");
                pwIndex.println("<td style=\"width: 75\">" + fns.getElement(i).getDate() + "</td>");
                pwIndex.println("<td style=\"width: 100\">" + fns.getElement(i).getReason() + "</td>");
                pwIndex.println("<td style=\"width: 100\">" + fns.getElement(i).getUploadStatus() + "</td>");
              
                // if protocol points to appliance
                if((fns.getElement(i).getProtocol().equalsIgnoreCase("temporary:///") == true)
                        || (fns.getElement(i).getProtocol().equalsIgnoreCase("local:///") == true )) {
                        
                    boolean bFileReceived = true;
                    
                    // get filename of error report
                    sFilename = fns.getElement(i).getFilename();
                   
                    // download the file
                    try {
                        getFile(sFilename, fns.getElement(i).getPath(), mDownloadDirectory);
                    } catch (ERMgmtBadResponseException e) {
                        bFileReceived = false;
                    }
                    
                    // if file was received
                    if(bFileReceived == true) {
                        
                        pwIndex.println("<td style=\"width: 400\">");
                        
                        if(bLinkToHtml == true) {
                            // write hyperlink to html file
                            String sHtmlFilename = sFilename.substring(0,sFilename.lastIndexOf(".txt.gz")) + ".html";
                            pwIndex.println("<a href=./" + sHtmlFilename + ">" + fns.getElement(i).getLocation() + "</a>");
                        }
                        else {
                               // write hyperlink to original file
                            pwIndex.println(fns.getElement(i).getLocation());                           
                        }

                        pwIndex.println("<br/>");
                        pwIndex.println("File Type:  ");
                        
                        if(bLinkToHtml == true) {
                                                        
                            // write hyperlink to html file
                            String sHtmlFilename = sFilename.substring(0,sFilename.lastIndexOf(".txt.gz")) + ".html";
                            pwIndex.println("<a href=./" + sHtmlFilename + ">" + "html" + "</a>");
                            
                            pwIndex.println(" | ");
                        }
                        
                        pwIndex.println("<a href=./" + sFilename + ">" + "txt.gz" + "</a>");
                        
                        pwIndex.println("</td>");
                        
                    }
                    else {
                        // write name of file without hyperlink
                        pwIndex.println("<td style=\"width: 400\">" + fns.getElement(i).getLocation() + "</td>");
                    }
                        
                }
                // just notify the location of the error report
                else {                        
                    pwIndex.println("<td style=\"width: 400\">" + fns.getElement(i).getLocation() + "</td>");
                }               
            }
            
            pwIndex.println("</table>");
            pwIndex.println("</body>");
            pwIndex.println("</html>");
                
            pwIndex.flush();
            pwIndex.close();
                        
        } 
        // otherwise fns is not enabled
        else {      
            // use non-fns method 
            getErrorReport();       
        } 
    }
    
    /**
     * Downloads a file from appliance to the download directory.
     * <p>
     * This method uses the get-file request.
     * 
     * @param sFilename filename to download
     * 
     * @param sDeviceDirectory source directory on appliance
     * 
     * @param sDownloadDirectory download directory
     * 
     * @throws ERMgmtException
     */
    
    public void getFile(
            String sFilename, 
            String sDeviceDirectory, 
            String sDownloadDirectory) throws ERMgmtException {
    	getFile(sFilename, sDeviceDirectory, sDownloadDirectory, "default");
    }

    public void getFile(
            String sFilename, 
            String sDeviceDirectory, 
            String sDownloadDirectory,
            String sDomain) throws ERMgmtException {
        
        SoapManagementRequest soma_request = null;
        ERConnection connection = null;
        String sResult;
        
       
        // create object for connection to the appliance
        connection = new ERConnection(mSettings);
    
        // create object for soap message
        soma_request = new SoapManagementRequest();
    
        // set domain
        soma_request.setDomain(sDomain);
        
        // send get-file request
        soma_request.getFile(sDeviceDirectory + sFilename);
    
        // issue to the request
        connection.send(soma_request);
        
        // get result of the collection
        sResult = soma_request.getResult();
        TraceHelper.trace("sResult: " + sResult);
            
        // check for authentication failure
        if(sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("getFile : Authentication failure");
        }
        // check if the response is not OK
        else if(sResult.indexOf("OK") == -1) {
            if(sResult.equalsIgnoreCase("Cannot read the specified file") == true)
                throw new ERMgmtBadResponseException("getFile : Cannot read the specified file : " + sDeviceDirectory + sFilename);
            else
                throw new ERMgmtBadResponseException("getFile :" + sResult);
        }

        parseOutputFile(soma_request, sDownloadDirectory, sFilename);
    }

    public void parseOutputFile(SoapManagementRequest soma_request, 
    		String sDownloadDirectory, String sFilename) throws ERMgmtSAXException
    {
        File fFileErrorReport;
        
        // if download directory specified
        if(sDownloadDirectory != null) {                               
            fFileErrorReport = new File(sDownloadDirectory + "/" + sFilename);
        }
        // else none was specified
        else {
            fFileErrorReport = new File(sFilename);
        }
        
        // process the get-file response
        try { 
            FileOutputStream outStream = new FileOutputStream(fFileErrorReport);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(
                soma_request.getResponse(),
                new DownloadParseHandler(outStream));

            outStream.flush();
            outStream.close();            
        } catch (Throwable e) {
            throw new ERMgmtSAXException("parsing get-file response failed. " + e);
        }
    }
    
    /**
     * Queries the appliance for the existence of an error report.
     * <p>
     * This method will check for a report in an FNS.  If there is no FNS,
     * then the method will check for temporary:///error-report.txt.gz.
     * 
     * @throws ERMgmtException
     * 
     * @return true if error report is present, otherwise false
     */
    
    public boolean isErrorReportExisting() throws ERMgmtException {
        
        boolean bErrorReportExisting = false;
        boolean bFnsSupported = false;
        ERFailureNotificationStatus fns = null;
        
        fns = new ERFailureNotificationStatus(mSettings);
        
        // request for FNS
        try {
            fns.getFailureNotificationStatus();
        } catch (ERMgmtException e) {
            bFnsSupported = false;
        }
        
        // get if FNS is supported
        bFnsSupported = !fns.isNotSupported();
        
        // if fns is supported  
        if(bFnsSupported == true)
        {
            // check if FNS is reporting an error report
            bErrorReportExisting = fns.isPresent();
        }
        // if failure notification status is not enabled
        else
        {               
            ERFilestore fs = new ERFilestore(mSettings);
            
            // get the filestore information
            fs.refresh();
            
            // determine if error report is present on the appliance
            bErrorReportExisting = fs.isErrorReportPresent();
        }
        
        return bErrorReportExisting;
    } 
    

    public boolean doActionRequest( String domain, String body ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an object for the soap message
        soma_request = new SoapManagementRequest();

        // set custom domain name if available
        soma_request.setDomain(domain);
        
        soma_request.doAction(body);
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.trim().indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("doActionRequest result="+sResult);
        }
        // if response is not OK
        else if(sResult.trim().equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("doActionRequest result="+sResult);
        }
        
        return true;
    }
    
    /**
     */
    
    public boolean setConfigRequest(
            String type, String domain, String body ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an object for the soap message
        soma_request = new SoapManagementRequest();
        
        // create a request for firmware version
        soma_request.createConfigRequest(type, domain, body);
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("setConfigRequest result="+sResult);
        }
        // if response is not OK
        else if(sResult.equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("setConfigRequest result="+sResult);
        }
        
        return true;
    }
    /**
     */
    
    public boolean setBackupRequest(
            String format, String body, String outFileName ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an object for the soap message
        soma_request = new SoapManagementRequest();
        
        // create a request for firmware version
        soma_request.createBackupRequest(format, body);
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("setBackupRequest result="+sResult);
        }
        // if response is not OK
        else if(sResult.equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("setBackupRequest result="+sResult);
        }

        parseOutputFile(soma_request, mDownloadDirectory, outFileName);
        
        return true;
    }
    

    public boolean setFileRequest( String domain, String filename, String body ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an object for the soap message
        soma_request = new SoapManagementRequest();

        soma_request.setFile(domain, filename, body);
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.trim().indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("setFile result="+sResult);
        }
        // if response is not OK
        else if(sResult.trim().equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("setFile result="+sResult);
        }
        
        return true;
    }
    

    /**
     * Pull any status provider by class
     * 
     * @param className class name of status provider we want to pull
     * 
     * @return File file of response from status provider
     * 
     * @throws ERMgmtException
     */
    
    public File getStatusByClassName(
            String className, String domainName ) throws ERMgmtException {
        
        String sResult = null;
        SoapManagementRequest soma_request = null;
        
        // create object for the connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create an objeect for the soap message
        soma_request = new SoapManagementRequest();
        
        // set custom domain name if available
        soma_request.setDomain(domainName);
        
        // create a request for firmware version
        soma_request.getStatus(className);
        
        // send the message and receive a response
        connection.send(soma_request);   
        
        sResult = soma_request.getResult();
        
        // check for authentication failure
        if(sResult.indexOf("Authentication failure") != -1) {
            throw new ERMgmtAuthenticationException("get-status version result="+sResult);
        }
        // if response is not OK
        else if(sResult.equalsIgnoreCase("OK") == false) {
            throw new ERMgmtBadResponseException("get-status version result="+sResult);
        }
        
       return soma_request.getResponse();     
    }
    
    public DeviceProfile getSettings() { return mSettings; }
    
}
