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

import java.io.InputStream;
import java.util.Map;


public class ReportProcessorPartInfo implements IPartInfo 
{
	/**
	 * @see IPartInfo#getType()
	 */
	public String getType() {
		return mType;
	}
	
	/**
	 * @see IPartInfo#getContentID()
	 */
	public String getContentID() {
		String cid = null;
		if (mHeaders != null)
		{
			Object value = mHeaders.get(H_CONTENT_ID);
			cid = value != null ? value.toString().trim() : null;
		}
		return cid;
	}
	
	/**
	 * @see IPartInfo#getContentType()
	 */
	public String getContentType() {
		String ctype = null;
		if (mHeaders != null)
		{
			Object value = mHeaders.get(H_CONTENT_TYPE);
			
			ctype = value != null ? value.toString().trim() : null;
		}
		return ctype;
	}
	
	/**
	 * @see IPartInfo#getContentTransferEncoding
	 */
	public String getContentTransferEncoding() {
		String cte = null;
		if (mHeaders != null)
		{
			Object value = mHeaders.get(H_CONTENT_XFER_ENC);
			cte = value != null ? value.toString().trim() : null;
		}
		return cte;
	}
	
	/**
     * @see IPartInfo#getContentTransferEncoding
     */
    public String getContentDisposition() {
        String cd = null;
        if (mHeaders != null)
        {
            Object value = mHeaders.get(H_CONTENT_DISPOSITION);
            cd = value != null ? value.toString().trim() : null;
        }
        return cd;
    }
    
    /**
     * @see IPartInfo#getContentTransferEncoding
     */
    public String getAttachmentFilename() {
        String cda = getContentDisposition();
        if(cda != null){
            int index = cda.indexOf("filename=")+9;
            cda = cda.substring(index);
        }

        return cda;
    }
    
	

	/**
	 * @see IPartInfo#getHeaders()
	 */
	public Map getHeaders() {
		return mHeaders;
	}

	/**
	 * @see IPartInfo#getBodyStream()
	 */
	public InputStream getBodyStream() {
	    return mBodyStream;
	}
	
	/**
     * @see IPartInfo#setInputStream()
     *  
     */
    public void setInputStream(InputStream is) {
        mBodyStream = is;
    }
    
    /**
     * @see IPartInfo#getErrorReportDetails()
     *  
     */
    public ErrorReportDetails getErrorReportDetails() {
        return mERDetails;
    }
	/**
	 * Package-protected Constructor
	 * @param type
	 * @param erInfo 
	 */
	public ReportProcessorPartInfo(String type, Map headers, InputStream stream, ErrorReportDetails erDetails)
	{
		mType = type;
		mHeaders = headers;
		mBodyStream = stream;
		mERDetails = erDetails;
	}

	private String mType;
	private Map mHeaders;
	private InputStream mBodyStream;
	private ErrorReportDetails mERDetails;
}
