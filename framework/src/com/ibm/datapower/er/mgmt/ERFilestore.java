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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class handles get-filestore operations including sending requests
 * and parsing responses.
 * 
 * @author Dana Numkena
 *
 */

public class ERFilestore {
    
    DeviceProfile mSettings;
    String mModified;
    boolean mbIsErrorReportPresent;
    
    /**
     * Constructor 
     * 
     * @param communication settings of appliance
     */
    public ERFilestore(DeviceProfile settings) {
        mSettings = settings;
    }
    
    /**
     * determines if an error report is present on the appliance
     * 
     * returns true if an error report 
     */
    public boolean isErrorReportPresent() {
        return mbIsErrorReportPresent;
    }
    
    /**
     * Gets the modification date of the error report
     * 
     * returns modification date string
     */
    public String getModified() {
        return mModified;
    }
    
    /**
     * SAX parse handler to search for error report files in 
     * the get-filestore response and get informatoin on the 
     * error report files.
     * 
     * @author Dana Numkena
     */
    private static class GetFilestoreParseHandler extends DefaultHandler {

        private boolean mbFile = false;
        private boolean mbErrorReport = false;
        private boolean mbIsErrorReportPresent = false;
        private boolean mbModified = false;
        private String mModified = null;

        // constructor
        public GetFilestoreParseHandler() {
            
        }
        
        /**
         * Determines if appliance has an error report
         * 
         * @return true if an error report is present on the appliance
         */
        public boolean isErrorReportPresent() {
            return mbIsErrorReportPresent;
        }
        /**
         * Gets the modification date of the error report.
         * 
         * @return modification date
         */
        public String getModified() {
            return mModified;
        }

        /* (non-Javadoc)
        * @see org.xml.sax.ContentHandler#characters(char[], int, int)
        */
        public void characters(char[] arg0, int start, int length)
            throws SAXException {
            
            if(mbModified == true) {
                mModified = new String(arg0, start, length);
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

            if (qualifiedName.equalsIgnoreCase("file")) {
                
                //System.out.println("name: " + atts.getValue("name"));
                if((atts.getValue("name").compareTo("error-report.txt.gz") == 0) 
                        || (atts.getValue("name").compareTo("error-report.txt") ==0 )) {
                    mbErrorReport = true;
                    mbIsErrorReportPresent = true;
                }
                
                mbFile = true;
            }
            else if(qualifiedName.equalsIgnoreCase("modified") && mbErrorReport == true) {
                mbModified = true;
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

            if(qualifiedName.equalsIgnoreCase("file") && mbFile == true) {
                mbFile = false;
                mbErrorReport = false;
            }
            else if(qualifiedName.equalsIgnoreCase("modified") && mbErrorReport == true) {
                mbModified = false;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            
        }

    }
    
    /**
     * Refreshes directory information on temporary:///.
     * 
     * @throws ERMgmtException
     */
    public void refresh() throws ERMgmtException {

        SoapManagementRequest soma_request = null;
        
        
        // setup for connection
        ERConnection connection = new ERConnection(mSettings);
        
        // create a soap request
        soma_request = new SoapManagementRequest();
        
        // create get-filestore request on temporary:///
        soma_request.getFilestore("temporary:", false, false);
        
        // transmit the request
        connection.send(soma_request);
        
        // issue the request
        connection.send(soma_request);
        
        // process the get-filestore response
        try { 
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
        
            GetFilestoreParseHandler get_filestore_parser = new GetFilestoreParseHandler();
            parser.parse(
                    soma_request.getResponse(),
                    get_filestore_parser);
            
            // get information from the reponse
            mbIsErrorReportPresent = get_filestore_parser.isErrorReportPresent();
            mModified = get_filestore_parser.getModified();
            
        } catch (Throwable e) {
            throw new ERMgmtSAXException("parsing get-filestore response failed. " + e);
        }
        
    }

}
