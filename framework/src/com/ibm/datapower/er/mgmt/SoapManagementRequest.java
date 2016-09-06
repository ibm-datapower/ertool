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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * This class provides an object for the SOMA request.
 * <p>
 * This class provides methods to write the requests and parse the response
 * element from the SOAP message.
 * 
 * @author DataPower Technology, Inc.
 * @author Dana Numkena
 * 
 */
public class SoapManagementRequest implements Message {

    public static final String sDefaultManagmentURI = "/service/mgmt/current";

    private static final String DP_URI_SOAP_MGMT_REQ = "http://www.datapower.com/schemas/management";

    public static final String OK = "OK";

    public static final String AUTH_ERROR = "Authentication failure";

    // operations
    public static final int GET_STATUS = 0;

    public static final int GET_CONFIG = 1;

    public static final int GET_LOG = 2;

    public static final int GET_FILESTORE = 3;

    public static final int GET_FILE = 4;

    public static final int GET_SIGNON = 5;

    public static final int SET_CONFIG = 6;

    public static final int DEL_CONFIG = 7;

    public static final int DO_ACTION = 8;

    public static final int SET_FILE = 9;

    public static final int EXPORT = 10;

    public static final int IMPORT = 11;

    public static final int BACKUP = 12;

    public static final int GET_VERSION = 13;

    public static final int MODIFY_CONFIG = 14;
    
    public static final int DO_BACKUP = 15;

    // type of request
    private int mType;

    // domain
    private String mDomain;

    // type of object
    private String mObjectType;

    // name of object
    private String mObjectName;

    // location of filestore
    private String mLocation;

    // message body
    private String mBody;

    // message attributes
    private String mAttribute;

    // response string OK or error string
    private String mResult;

    // response stream
    private File mResponse;


    /**
     * Sets a do-action element with ErrorReport body
     * 
     */
    public void doActionErrorReport(boolean bRedirectToTemporary) {
        
        TraceHelper.trace("soap-management: do-action");
        mType = DO_ACTION;

        // create action header
        mBody = "<" + "ErrorReport" + ">";
        
        // if generate to the temporary directory
        if(bRedirectToTemporary == true) {
            mBody = mBody + "\n<RedirectToTemporary>on</RedirectToTemporary>\n";
        }

        // action object trailer
        mBody = mBody + "</" + "ErrorReport" + ">\r\n";
    }

    /**
     * Sets a do-action element with ErrorReport body
     * 
     */
    public void doAction(String body) {
        TraceHelper.trace("soap-management: do-action");
        mType = DO_ACTION;

        // action object trailer
        mBody = body;
    }
    

    /**
     * Sets a set-file element with ErrorReport body
     * 
     */
    public void setFile(String domain, String filename, String body) {
        TraceHelper.trace("soap-management: set-file");
        
        mType = SET_FILE;

        if ( domain != null && domain.length() > 0 )
        	mDomain = domain;
        
        if ( body != null && body.length() > 0 )
        	mBody = body;
        
        mObjectName = filename;
    }

    /**
     * Sets a set-file element with ErrorReport body
     * 
     */
    public void setDomain(String domain) {
        TraceHelper.trace("soap-management: set-domain");
        
        if ( domain != null && domain.length() > 0 )
        	mDomain = domain;
    }

    /**
     * Sets the get-filestore element and attributes
     * 
     * @param location value of the location attribute
     * 
     * @param annotated value of the annotated attribute
     * 
     * @param layout value of the layout-only attribute 
     */
    public void getFilestore(String location, boolean annotated, boolean layout) {
        
        TraceHelper.trace("soap-management: get-filestore " + location);
        mType = GET_FILESTORE;
        mLocation = location;
        mAttribute = "";
        
        if (annotated)
            mAttribute = mAttribute + " annotated=\"true\"";
        if (layout)
            mAttribute = mAttribute + " layout-only=\"true\"";
    }

    /**
     * Sets the get-file element and attributes
     * 
     * @param filename value of the name attribute
     */
    public void getFile(String filename) {
        TraceHelper.trace("soap-management: get-file " + filename);
        
        mType = GET_FILE;
        mObjectName = filename;
    }

    public void getFile(String domain, String filename) {
        TraceHelper.trace("soap-management: get-file " + filename);

        if ( domain != null && domain.length() > 0 )
        	mDomain = domain;
        
        mType = GET_FILE;
        mObjectName = filename;
    }

    
    /**
     * Sets the get-status element and attributes
     *  
     * @param objectType value of the class attribute
     */
    public void getStatus(String objectType) {
        TraceHelper.trace("soap-management: get-status " + objectType);
        mType = GET_STATUS;
        mObjectType = objectType;
    }

    /**
     * Sets the get-status class=version request
     */
    public void getVersion() {
        TraceHelper.trace("soap-management: get-version ");
        mType = GET_VERSION;
    }

    /**
     * Gets the class attribute.
     * 
     * @return the class attribute
     */
    private String getClassAttribute() {
        if (mObjectType == null)
            return "";
        return " class=\"" + mObjectType + "\"";
    }

    /**
     * Gets the name attribute.
     * 
     * @return the name attribute
     */
    private String getNameAttribute() {
        if (mObjectName == null)
            return "";
        return " name=\"" + mObjectName + "\"";
    }

    /**
     * Gets the location attribute.
     * 
     * @return the location attribute 
     */
    private String getLocationAttribute() {
        if (mLocation == null)
            return "";
        return " location=\"" + mLocation + "\"";
    }

    /**
     * Write the SOMA request to the message object.
     * 
     * @param wout Writer object pointing to the SOAP request message
     * 
     * @throws IOException
     */
    private void writeRequestElement(Writer wout) throws IOException {
        if (mType == GET_VERSION) {
            wout.write("<dp:get-status class=\"Version\"/>");
        } else if (mType == GET_STATUS) {
            wout.write("<dp:get-status" + getClassAttribute() + "/>");
        } else if (mType == GET_LOG) {
            wout.write("<dp:get-log" + getNameAttribute() + "/>");
        } else if (mType == GET_FILESTORE) {
            wout.write("<dp:get-filestore" + getLocationAttribute() + mAttribute + "/>");
        } else if (mType == GET_FILE) {
            wout.write("<dp:get-file" + getNameAttribute() + "/>");
        } else if (mType == SET_FILE) {
            wout.write("<dp:set-file" + getNameAttribute() + ">\r\n");
            wout.write(mBody);
            wout.write("</dp:set-file>");
        }else if (mType == DEL_CONFIG) {
            wout.write("<dp:del-config>\r\n");
            wout.write(mBody);
            wout.write("</dp:del-config>");
        } else if (mType == SET_CONFIG) {
            wout.write("<dp:set-config>\r\n");
            wout.write(mBody);
            wout.write("</dp:set-config>");
        } else if (mType == DO_ACTION) {
            wout.write("<dp:do-action>\r\n");
            wout.write(mBody);
            wout.write("</dp:do-action>\r\n");
        } else if (mType == GET_CONFIG) {
            wout.write("<dp:get-config" + getClassAttribute()
                    + getNameAttribute() + "/>");
        }
        else if (mType == MODIFY_CONFIG) {
            wout.write("<dp:modify-config>\r\n");
            wout.write(mBody);
            wout.write("</dp:modify-config>");
        }else if (mType == DO_BACKUP) {
            wout.write("<dp:do-backup format=\"" + mObjectType + "\">\r\n");
            wout.write(mBody);
            wout.write("</dp:do-backup>");
        }
        else {
            TraceHelper.trace("Invalid request element type");
            wout.write("");
        }
    }

    /**
     * Write SOMA request element to the SOAP request message object. 
     * 
     * @param wout Writer object pointing to the SOAP message
     * 
     * @throws ERMgmtIOException
     * 
     * @see Message#write
     */
    public void write(Writer wout) throws ERMgmtIOException {
        try {
            wout.write("<dp:request");
            if (mDomain != null) {
                wout.write(" domain=\"" + mDomain + "\"");
            }
            wout.write(" xmlns:dp=\"" + DP_URI_SOAP_MGMT_REQ + "\">\r\n");
            writeRequestElement(wout);
            wout.write("\r\n");
            wout.write("</dp:request >\r\n");
        } catch (IOException e) {
            throw new ERMgmtIOException("soap mgmt request write error: " + e.toString());
        }
    }

    /**
     * Parses SOMA response for a result or response.
     * 
     * @param reader Reader object pointing to the response message.
     * 
     * @throws ERMgmtIOException
     * 
     * @see Message#parse
     */
    public void parse(Reader reader) throws ERMgmtIOException {
        
        boolean resultTag = false;
        boolean responseTag = false;

        // copy next tag into string buffer
        
        try {
            // **************************************
            // search for either result or response tag
            // **************************************
        
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);

                // if result tag was found
                if (sb.indexOf("<dp:result") != -1) {
                
                    // indicate result tag
                    resultTag = true;
                    break;
                
                    // else if response tag was found
                } else if ((sb.indexOf("<dp:status") != -1)
                        || (sb.indexOf("<dp:config") != -1)
                        || (sb.indexOf("<dp:log") != -1)
                        || (sb.indexOf("<dp:file") != -1)
                        || (sb.indexOf("<dp:import") != -1)
                        || (sb.indexOf("<dp:filestore") != -1)) {
                
                    // indicate response tag
                    responseTag = true;
                    break;
                
                }
            }

            // if neither result or response tags were found.
            if (!resultTag && !responseTag) {
                throw new ERMgmtIOException("Malformed or missing response element");
            }

            // if result tag
            if (resultTag) {
            
                // copy the rest from the input stream
                while ((c = reader.read()) != -1) {
                    sb.append((char) c);
                }
            
                TraceHelper.trace("Result element received");
            
                // save result content 
                parseResultString(sb.toString());
            
            // else it was a response tag
            } else {
            
                // give an OK as the result
                mResult = OK;
            
                TraceHelper.trace("Response element received");
            };
        } catch(IOException e) {
            throw new ERMgmtIOException("soap mgmt request parse error: " + e.toString());
        }
    }

    /**
     * Extract and save the content of the result element.
     * 
     * @param response response message
     * 
     * @throws ERMgmtIOException
     */
    private void parseResultString(
            String response) throws ERMgmtIOException {
        
        String startTag = "<dp:result>";
        String endTag = "</dp:result>";

        int start = response.indexOf(startTag) + startTag.length();
        int end = response.indexOf(endTag);
        if ((start == -1) || (end == -1)) {
            throw new ERMgmtIOException("Malformed result element");
        }

        mResult = response.substring(start, end);
        TraceHelper.trace("Received response '" + mResult + "'");
    }

    /**
     * Get the contents of the result element.
     * 
     * @return result content
     */
    public String getResult() {
        return mResult;
    }

    /**
     * Stores response file to a temporary file.
     * 
     * @param InputStream stream pointing to the response
     * 
     * @throws IOException
     */
    public void storeResponse(InputStream in) throws IOException {
        
        // temp file to write response to
        mResponse = File.createTempFile("dpmgmt", null);

        // copy response into temp file
        FileOutputStream to = new FileOutputStream(mResponse);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead);
        }

        // flush response to temp file
        to.flush();
        
        // close temp file 
        to.close();
    }
    
    /**
     * Gets the file handle to the response.
     * 
     * @return File object of response
     */
    public File getResponse() {
        return mResponse;
    }
    /**
     * Returns the String representation of the request ObjectType
     * return String the mObjectType as a string
     * 
     */
    public String toString(){
    	return mObjectType;
    }

    /**
     * Sets the config request
     */
    public void createConfigRequest(String type, String domain, String body) {
        TraceHelper.trace("soap-management: createConfigRequest ( " + type + ")");
        if ( type.equals("modify-config") )
        	mType = MODIFY_CONFIG;
        else if ( type.equals("set-config") )
        	mType = SET_CONFIG;
        else if ( type.equals("del-config") )
        	mType = DEL_CONFIG;
        else
        	mType = GET_CONFIG;
        
        if ( domain.length() > 0 )
        	mDomain = domain;
        
        if ( body.length() > 0 )
        	mBody = body;
    }


    /**
     * Sets the backup request
     */
    public void createBackupRequest(String format, String body) {
        TraceHelper.trace("soap-management: createConfigRequest ( " + format + ")");

        String formatLwr = format.toLowerCase();
        
        // add handling for bad format provided?
        if ( !formatLwr.equals("xml") && !formatLwr.equals("zip"))
        	return;
        
        mType = DO_BACKUP;
        
        mObjectType = format;
        
        if ( body.length() > 0 )
        	mBody = body;
    }
    
}