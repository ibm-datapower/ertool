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

package com.ibm.datapower.er.mgmt;

import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * The ERFailureNoificationStatus class provides an object to hold 
 * Failure Notification Status information from the appliance. 
 * <p>
 * This class provides methods to query the appliance for the Failure
 * Notification Status and stores the query locally.
 * 
 * @author Dana Numkena
 *
 */

public class ERFailureNotificationStatus {
    static  String DP_STATUS = "dp:status";
    static  String FAILURE_NOTIFICATION_STATUS = "FailureNotificationStatus2";
    static  String DATE = "Date";
    static  String REASON = "Reason";
    static  String UPLOAD_STATUS = "UploadStatus";
    static  String LOCATION = "Location";

    boolean mbIsPresent = false;
    boolean mbNotSupported = false;
    DeviceProfile mSettings;
    Vector maFnsElements = new Vector(); 
    
    /**
     * The FailureNotificatoinStatusElement class holds information
     * from a FailureNotificationStatus element from a get-status
     * FailureNotificationStatus.
     * 
     * @author Dana Numkena
     *
     */
    public static class FailureNotificationStatusElement {
        String mReason;
        String mUploadStatus;
        String mLocation;
        String mProtocol;
        String mFilename;
        String mPath;
        String mDate; 
        int    mEntryNumber;
        
        /**
         * Get location of error report
         * 
         * @return location of error report
         */
        public String getLocation() {
            return mLocation;
        }
        
        /**
         * Gets protocol used to store the error report.
         * 
         * @return protocol
         */
        public String getProtocol() {
            return mProtocol;
        }
        
        /**
         * Gets filename of error report
         * 
         * @return filename
         */
        public String getFilename() {
            return mFilename;
        }
        
        /**
         * Gets path of error report.
         * 
         * The path includes the protocol. 
         * 
         * @return directory path to the error report
         */
        public String getPath() {
            return mPath;
        }
        
        /**
         * Gets the reason for the error report generation
         * 
         * @return reason
         */
        public String getReason() {
            return mReason;
        }
        
        /**
         * Gets where uploading was successful.
         * 
         * @return upload status
         */
        public String getUploadStatus() {
            return mUploadStatus;
        }
        
        /**
         * Gets the date when the error report was generated.
         * 
         * @return date
         */
        
        public String getDate() {
            return mDate;
        }
    }
    
    /**
     * Constructor 
     * 
     * @param communication settings of appliance
     */
    public ERFailureNotificationStatus(DeviceProfile settings) {
        mSettings = settings;
    }
    
    /**
     * Is there an FNS element present.
     * 
     * @return true if FNS element is present.
     */
    public boolean isPresent() {
        return mbIsPresent;
    }
    
    /**
     * Is an FNS not supported.
     * 
     * @return true if NFS is not supported by appliance
     */
    public boolean isNotSupported() {
        return mbNotSupported;
    }
    
    /**
     * Gets the number of FNS elements in the response
     * 
     * @return number of the FNS elements
     */
    
    public int size() {
        return maFnsElements.size();    
    }
    
    /**
     * Gets the FNS element from the corresponding index.
     * 
     * @param index index number in the FNS array
     * @return an indexed element
     */
    public FailureNotificationStatusElement getElement(int index) {
        return (FailureNotificationStatusElement)maFnsElements.get(index);
    }
    
    /**
     * Gets the 2nd latest FNS entry
     * 
     * @return 2nd latest fns
     */
    public FailureNotificationStatusElement get2ndLatestEntry() throws ERMgmtException {
        int nIndex;
        FailureNotificationStatusElement latest = null;
        
        if(maFnsElements.size() < 2) return null;
        
        for(nIndex = 0; nIndex < maFnsElements.size(); nIndex++) {
            
            FailureNotificationStatusElement test;
            test = (FailureNotificationStatusElement)maFnsElements.get(nIndex);
            TraceHelper.trace("index=" + test.mEntryNumber + " mDate=" + test.mDate + " upload status=" + test.mUploadStatus);
            
            latest = (FailureNotificationStatusElement)maFnsElements.get(nIndex);
            if(latest.mEntryNumber == maFnsElements.size() - 2 ) {
                return latest;
            }
            
        }
        
        throw new ERMgmtException("get2ndLatestEntry expected entry number not present");
        
    }
    
    /**
     * Gets the latest FNS entry
     * 
     * @return latest fns.  If no existing entry, returns null.
     */
    public FailureNotificationStatusElement getLatestEntry() throws ERMgmtException {
        int nIndex;
        FailureNotificationStatusElement latest = null;
        
        // if there is no entry
        if(maFnsElements.size() == 0) {
            // return nothing
            return null;
        }
        
        for(nIndex = 0; nIndex < maFnsElements.size(); nIndex++) {
            
            FailureNotificationStatusElement test;
            test = (FailureNotificationStatusElement)maFnsElements.get(nIndex);
            TraceHelper.trace("index=" + test.mEntryNumber + " mDate=" + test.mDate + " upload status=" + test.mUploadStatus);
            
            latest = (FailureNotificationStatusElement)maFnsElements.get(nIndex);
            if(latest.mEntryNumber == maFnsElements.size() -1 ) {
                return latest;
            }
            
        }
        
        throw new ERMgmtException("getLatestEntry expected entry number not present");
        
    }
    
    /**
     * SAX parse handler for get-status FailureNotificationStatus response.
     * 
     * @author Dana Numkena
     */
    private class FNSHandler extends DefaultHandler 
    {
        private boolean mbDPStatus = false;
        private boolean mbFailureNotificationStatus = false;
        private boolean mbReason = false;
        private boolean mbUploadStatus = false;
        private boolean mbLocation = false;
        private boolean mbDate = false;
        FailureNotificationStatusElement mFnse;
        int mNumEntries = 0;
        //StringBuilder Not supported on Java 1.4.2, use StringBuffer instead
        //private StringBuilder mCDataBuffer;
        private StringBuffer mCDataBuffer;

        /*
         *  constructor
         */
        public FNSHandler() 
        {
            // clear out FailureNotificationStatus table
            maFnsElements.clear();
            mCDataBuffer = new StringBuffer(128);
        }
        
        /**
         * Determines if the FNS reports an error report.
         * 
         * @return true if FNS reports an error report
         */
        public boolean IsErrorReportPresent() {
            if (maFnsElements.size() > 0) {
                return  true;
            }
            
            return false;
        }

        /* (non-Javadoc)
        * @see org.xml.sax.ContentHandler#characters(char[], int, int)
        */
        public void characters(char[] ch, int start, int length) throws SAXException 
        {
            mCDataBuffer.append(ch, start, length);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qualifiedName,
            Attributes atts)
            throws SAXException 
        {
            mCDataBuffer.setLength(0);
                        
            
            if (qualifiedName.equalsIgnoreCase(DP_STATUS)) {
                mbDPStatus = true;
            }
            else if(qualifiedName.equalsIgnoreCase(FAILURE_NOTIFICATION_STATUS) && mbDPStatus == true) {
                mbFailureNotificationStatus = true;
                mFnse = new FailureNotificationStatusElement();
                mFnse.mEntryNumber = mNumEntries;
            }
            else if(qualifiedName.equalsIgnoreCase(DATE) && mbFailureNotificationStatus == true) {
                mbDate = true;
            }
            else if(qualifiedName.equalsIgnoreCase(REASON) && mbFailureNotificationStatus == true) {
                mbReason = true;
            }
            else if(qualifiedName.equalsIgnoreCase(UPLOAD_STATUS) && mbFailureNotificationStatus == true) {
                mbUploadStatus = true;
            }
            else if(qualifiedName.equalsIgnoreCase(LOCATION) && mbFailureNotificationStatus == true) {
                mbLocation = true;
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

            // finished processing element, if any character data was cached,
            // it must now be saved to the proper member variable
            if (mCDataBuffer.length() > 0)
            {
                if(mbDate == true) 
                {
                    mFnse.mDate = mCDataBuffer.toString();
                } 
                else if(mbReason == true) 
                {
                    mFnse.mReason = mCDataBuffer.toString();
                }
                else if(mbUploadStatus == true) 
                {
                    mFnse.mUploadStatus = mCDataBuffer.toString();
                }
                else if(mbLocation == true) 
                {
                    mFnse.mLocation = mCDataBuffer.toString();
                    
                    if(mFnse.mLocation.indexOf("///") != -1) 
                    {
                        mFnse.mProtocol = mFnse.mLocation.substring(0, mFnse.mLocation.indexOf("///")) + "///";
                        mFnse.mFilename = mFnse.mLocation.substring(mFnse.mLocation.indexOf("///") + 3);
                    } 
                    else 
                    {
                        mFnse.mProtocol = mFnse.mLocation.substring(0, mFnse.mLocation.indexOf("//")) + "//";
                        mFnse.mFilename = mFnse.mLocation.substring(mFnse.mLocation.indexOf("//") + 2);    
                    }
                    
                    // if there is a path to the error report
                    if(mFnse.mFilename.lastIndexOf('/') != -1) 
                    {
                        mFnse.mPath = mFnse.mProtocol + mFnse.mFilename.substring(0, mFnse.mFilename.lastIndexOf('/')) + "/";
                        mFnse.mFilename = mFnse.mFilename.substring(mFnse.mFilename.lastIndexOf('/') + 1);
                        
                    } 
                    else 
                    {
                        mFnse.mPath = mFnse.mProtocol;
                    }
                }
            }
            
            if(qualifiedName.equalsIgnoreCase(FAILURE_NOTIFICATION_STATUS) && mbDPStatus == true) {
                mbFailureNotificationStatus = false;
                maFnsElements.add(mFnse);
                mNumEntries++;
            }
            else if(qualifiedName.equalsIgnoreCase(DP_STATUS)) {
                mbDPStatus = false;
            }
            else if(qualifiedName.equalsIgnoreCase(DATE) && mbFailureNotificationStatus == true) {
                mbDate = false;
            }
            else if(qualifiedName.equalsIgnoreCase(REASON) && mbFailureNotificationStatus == true) {
                mbReason = false;
            }
            else if(qualifiedName.equalsIgnoreCase(UPLOAD_STATUS) && mbFailureNotificationStatus == true) {
                mbUploadStatus = false;
            }
            else if(qualifiedName.equalsIgnoreCase(LOCATION) && mbFailureNotificationStatus == true) {
                mbLocation = false;
            }
            
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            
        }
    }
    

    
    /**
     * Send a get-status FailureNotificationStatus request 
     * and creates a local copy of the response.
     * 
     * @throws ERMgmtException
     */
    
    public void getFailureNotificationStatus() throws ERMgmtException {
         
        SoapManagementRequest soma_request = null;
        ERConnection connection = null;        
        String sResult;
        FNSHandler fns_parser = null;
        Logger logger = Logger.getLogger("com.ibm.datapower.er");
        
        // create object for connection to the appliance
        connection = new ERConnection(mSettings);
        
        // create object for soap message
        soma_request = new SoapManagementRequest();
            
        // send get-status failurenotificationstatus        
        soma_request.getStatus(FAILURE_NOTIFICATION_STATUS);
         
        try {
            //issue the request
            logger.info("Sending soma request " + soma_request.toString());
            connection.send(soma_request);
        } catch (ERMgmtHttpNotOKException e) {        
        	
        	try{
        		//if getting FailureNotificationStatus2, we try reverting back to the original FailureNotificationStatus
        		FAILURE_NOTIFICATION_STATUS = "FailureNotificationStatus";        	
        		
        		soma_request.getStatus(FAILURE_NOTIFICATION_STATUS);
                //issue the request
                logger.info("Sending soma request " + soma_request.toString());
                connection.send(soma_request);
        	} catch (ERMgmtHttpNotOKException e2){
        		
        		mbNotSupported = true;
                mbIsPresent = false;
                throw new ERMgmtHttpNotOKException("getFailureNotificationStatus: " + e.getMessage());	
        	}        	          
        }    
                    
        // get result of the collection
        sResult = soma_request.getResult();
        logger.info("Result:" + sResult);
            
        // check if response is OK 
        if(sResult.indexOf("OK") == -1)
        {
            mbIsPresent = false;
            throw new ERMgmtBadResponseException("getFailureNotificationStatus: bad response: " + sResult);
        }
        
        try {    
            // process the get-status failurenotificationstatus response
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            fns_parser = new FNSHandler();
            parser.parse(
                    soma_request.getResponse(),
                    fns_parser);            
        } catch (Throwable e) {
        	e.printStackTrace();
            throw new ERMgmtSAXException("getFailureNotificationStatus: parsing response: " + e);
        }
        
        mbIsPresent = fns_parser.IsErrorReportPresent();
         
    }
}
