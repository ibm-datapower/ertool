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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * This class provides an object as the SOAP message.
 * <p>
 * Primarily, this class creates the SOAP envelope.  Indirectly, this class
 * calls the SoapManagementRequest class to create the SOMA request.
 * <p>
 * This class provides methods to write the SOAP message to streaming output
 * and parse responses.  
 * 
 * @author DataPower Technology, Inc.
 * @author Dana Numkena
 *
 */
public class SoapEnvelope implements Message {

    // name spaces
    private static final String NS_URI_SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/";

    // enveloped message
    private Message mMessage;

    /**
     * Constructor
     * 
     * @param message content of the SOAP envelope element, 
     * or the SOMA request.
     */
    public SoapEnvelope(Message message) {
        mMessage = message;
    }

    /**
     * Write the SOMA request
     * <p>
     * This method writes the SOAP envelope and indirectly writes 
     * the SOMA request.
     * 
     * @param wout Writer object pointing to the SOAP request.
     * 
     * @throws ERMgmtIOException
     * 
     * @see Message#write
     */
    public void write(Writer wout) throws ERMgmtIOException {
        try {
            wout.write("<?xml version='1.0'  encoding=\"UTF-8\"?>\r\n");
            wout.write("<SOAP-ENV:Envelope ");
            wout.write("xmlns:SOAP-ENV=\"" + NS_URI_SOAP_ENV + "\">\r\n");
            wout.write("<SOAP-ENV:Body>\r\n");
            mMessage.write(wout);
            wout.write("</SOAP-ENV:Body>\r\n");
            wout.write("</SOAP-ENV:Envelope>\r\n");
        } catch (IOException e) {
            throw new ERMgmtIOException("soap envelope write failure" + e.toString());
        }
    }

    /**
     * Parse the SOAP response.
     * 
     * @param reader a Reader object pointing to the SOAP response 
     * 
     * @throws ERMgmtIOException
     * 
     * @see Message#parse
     */
    public void parse(Reader reader) throws ERMgmtIOException {
        try {
            
            String soapBodyStartTag = "<env:Body>";
            boolean bodyFound = false;
        
            // *********************
            // search for <env:Body> 
            // *********************
        
            // search for the <env:Body>
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
                if (sb.indexOf(soapBodyStartTag) != -1) {
                    bodyFound = true;
                    break;
                }
            }

            // if body was not found
            if (!bodyFound) {
                throw new IOException("Malformed SOAP response - no body tag");
            }

            // mark <env:Body> start 
            reader.mark(0);
            sb.delete(0, sb.length());        // delete contents in buffer

            // ***************
            // Check for fault
            // ***************
        
            // read the next tag
            String soapFaultStartTag = "<env:Fault>";
            boolean tagClose = false;

            // read dp:response tag
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
                if ((char) c == '>') {
                    tagClose = true;
                    break;
                }
            }

            // if tag has no end tag
            if (!tagClose) {
                throw new ERMgmtIOException("Malformed SOAP response - no end tag");
            }

            // if dp:response have a SOAP fault element
            if (sb.toString().startsWith(soapFaultStartTag)) {
            
                // read remaining part of the response
                while ((c = reader.read()) != -1) {
                    sb.append((char) c);
                }

                // parse fault string and throw exception
                String faultString = parseFaultString(sb.toString());
                TraceHelper.trace("Received SOAP fault '" + faultString +"'");
                throw new IOException(faultString);
            }

            // reset the <env:Body> element
            reader.reset();
        
            // ***************************
            // no fault, parse the body element
            // ***************************
        
            // parse the body element, or parse the SOMA response
            mMessage.parse(reader);
            
        } catch (IOException e) {
            throw new ERMgmtIOException("soap envelope parse failure." + e.toString());
        }
    }

    /**
     * Gets the <faultstring> content from the SOAP response. 
     * 
     * @param response the SOAP response message
     * 
     * @return fault string content
     * 
     * @throws IOException
     */
    private String parseFaultString(String response) throws IOException {
        
        String startTag = "<faultstring>";
        String endTag = "</faultstring>";
        
        int start = response.indexOf(startTag) + startTag.length();
        int end = response.indexOf(endTag);

        if ((start == -1) || (end == -1)) {
            throw new IOException("Malformed fault response");
        }

        return response.substring(start, end);
    }

}
