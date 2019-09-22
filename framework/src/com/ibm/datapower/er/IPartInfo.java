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

import java.io.InputStream;
import java.util.Map;


public interface IPartInfo 
{
	String H_CONTENT_ID = "Content-ID";
	String H_CONTENT_TYPE = "Content-Type";
	String H_CONTENT_DISPOSITION = "Content-Disposition";
	String H_CONTENT_XFER_ENC = "Content-transfer-encoding";
	
	/**
	 * The Preamble of a MIME message is usually not used by any MIME 
	 * processors, but rather just informational text for non-MIME readers.
	 * 
	 * The DataPower Error Report preamble information may contain headers and
	 * an input stream to read its text. 
	 * 
	 * At the time of this writing, the DataPower Error Report preamble contains
	 * the following text "DataPower Error Report for domain ..." 
	 */
	final String MIME_PREAMBLE = "PREAMBLE";

	/**
	 * The Epilogue of a MIME message is usually not used by any MIME 
	 * processors.
	 * 
	 * The DataPower Error Report currently has no epilogue information.
	 */
	final String MIME_BODYPART = "BODYPART";

	/**
	 * The Epilogue of a MIME message is usually not used by any MIME 
	 * processors.
	 * 
	 * The DataPower Error Report currently has no epilogue information.
	 */
	final String MIME_EPILOGUE = "EPILOGUE";
	
	/**
	 * Indicates what MIME Part type this is. It returns a string for easier
	 * printing, but it will always be one of the Constants defined in this
	 * in interface
	 * @return one of [MIME_PREAMBLE|MIME_EPILOGUE|MIME_BODYPART]
	 */
	String getType();
	
	/**
	 * Easy access to the Content-ID header.
	 * @return
	 */
	String getContentID();
	
	/**
	 * Easy access to the Content-Type header.
	 * @return
	 */
	String getContentType();
	
	/**
	 * Easy access to the Content-Transfer-Encoding header.
	 * @return
	 */
	String getContentTransferEncoding();
	
   /**
     * Easy access to the Content-Disposition header.
     * @return
     */
    String getContentDisposition();
    
    /**
     * Easy access to the attachment filename from the Content-Disposition header.
     * @return
     */
    String getAttachmentFilename();

	/**
	 * Retrieves all headers associates with this part.
	 * @return a Map containing the headers or null if no headers are available
	 */
	Map getHeaders();
	
	/**
	 * Retrieves the InputStream that access the body of this part. 
	 * @return an input stream to read the body
	 */
	InputStream getBodyStream();
	
   /**
     * Retrieves the ErrorReportDetails about this part 
     * @return an ErrorReportDetails with the information about the part
     */
    ErrorReportDetails getErrorReportDetails();
    
    /**
     * Resets the inputStream to a custom input stream
     * @param the input stream to replace the current InputStream
     */
    void setInputStream(InputStream is);
}
