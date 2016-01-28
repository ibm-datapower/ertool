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

package com.ibm.datapower.er;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * A special input stream to be used when processing the FirmwareVersion MIMEPart

 * A class representing the Information from Firmware Version. 
 * 
 * @author Alex Teyssandier
 *
 */

public class FirmwareInputStream extends InputStream {
    InputStream mInputStream;
    ErrorReportDetails mErrorReportDetails;
    ByteArrayOutputStream mByteArrayOut;
    ByteArrayInputStream mByteArrayIn;

    /**
     * Default constructor. called the InputStream constructor
     */
    public FirmwareInputStream() {
        super();
    }
    
    /**
     * Constructor that sets the InputStream and ErrorReportDetails
     * Will also load the ErrorReportDetails with the appropriate information
     * @param is the InputStream to set mInputStream to
     * @param erd the ErrorReportDetails to set the mErrorReportDetails to
     * @throws SAXException 
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public FirmwareInputStream(InputStream is, ErrorReportDetails erd) throws SAXException, IOException, ParserConfigurationException {
        mInputStream = is;
        mErrorReportDetails = erd;
        mByteArrayOut = new ByteArrayOutputStream();
    }
    /**
     * fills in the mByteArrayInputStream with the information from mInputStream
     * uses mByteArrayInputStream in a DOMParser to parse the XML and fill in the ErrorReportDetails
     * 
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public void loadERD() {
        try {            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(mByteArrayIn);              
            doc.getDocumentElement().normalize();
                        
            
            Element eElement = (Element) doc.getElementsByTagName("FirmwareVersion").item(0);
        
            mErrorReportDetails.setSerial(getValueByTag("Serial", eElement));
            mErrorReportDetails.setVersion(getValueByTag("Version", eElement));
            mErrorReportDetails.setBuild(getValueByTag("Build", eElement));
            mErrorReportDetails.setBuildDate(getValueByTag("BuildDate", eElement));
            mErrorReportDetails.setWatchdogBuild(getValueByTag("WatchdogBuild", eElement));
            mErrorReportDetails.setInstalledDPOS(getValueByTag("InstalledDPOS", eElement));
            mErrorReportDetails.setRunningDPOS(getValueByTag("RunningDPOS", eElement));
            mErrorReportDetails.setXMLAccelerator(getValueByTag("XMLAccelerator", eElement));
            mErrorReportDetails.setMachineType(getValueByTag("MachineType", eElement));
            mErrorReportDetails.setModelType(getValueByTag("ModelType", eElement));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ParserConfigurationException pce){
            pce.printStackTrace();
        }
   }

    /**
     * Gets Value of an XML element by tag name
     * @param sTag the string value representation of the tag name
     * @param eElement the element in which to look for the tag nane
     * @return String value of the XML element
     */
    public static String getValueByTag(String sTag, Element eElement){
    	try
    	{
        NodeList nlList= eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0); 
        
        // null check added May 30th (DisplayMessage was an empty node)
        if ( nValue == null )
        	return "";
        
        return nValue.getNodeValue();    
    	}catch(Exception ex)
    	{
    	}
    	
    	return "";
     }
    
    @Override
    public int read() throws IOException   {
        int b = mInputStream.read();        
        //if b is not endoffile
        if(b != -1){
            //add b to byte array outputstream; 
            mByteArrayOut.write(b);
        } else {                 
            mByteArrayIn = new ByteArrayInputStream(mByteArrayOut.toByteArray());            
            loadERD();
        }
        return b;
    }
    @Override
    public int read(byte[] byteArray) throws IOException   {
        int bytesRead = mInputStream.read(byteArray);
        
        mByteArrayOut.write(byteArray, 0, bytesRead);
        mByteArrayIn = new ByteArrayInputStream(mByteArrayOut.toByteArray());
        
        loadERD(); 
        return bytesRead;
    }

}
