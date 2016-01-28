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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.ibm.datapower.er.mgmt.Base64;

/**
 * A parts processor that outputs the  contents of each file into filename in a temporary directory in
 * 
 * @author alex
 *
 */

public class PartsProcessorZIP implements IPartsProcessor{
    public static String mPath = null;
    IPartInfo mMimePart = null;   
    String mFilename = null;

    /**
     * Contructor that sets the temporary directory 
     * @param tempdir
     * @throws IOException
     */
    public PartsProcessorZIP() throws IOException {
	}
    
	/**
	 * Process the mimepart and will output the information to the given PrintWriter.
	 * 
	 * 
	 * @param mimePart the mimepart to be processed
	 * @param writer the output printwriter to output the data. In this special zip processor case, the PrintWriter does not matter because all parts are written out to files and then consolidated by an external class
	 * 
	 */
	public void process(IPartInfo mimePart, PrintWriter writer) throws IOException {
	    mMimePart = mimePart;	    

	    //check if mimeparts are null, if they are, no need to continue
	    if(mimePart.getContentID() == null){
	        return;
	    }
        boolean decode = isBase64();
	    InputStream in = mimePart.getBodyStream();
	    createFile(in, decode);	    
	}
	/**
	 * Creates a file inside the mPath directory of the mime part being processed
	 * 
	 * 
	 * @param in the input stream containing the mime data
	 * @param decode indicates whether data needs to be decoded from base64 or not
	 * @throws IOException
	 */
	private void createFile(InputStream in, boolean decode) throws IOException{
	    String filename = getFileName();	    
	    
	    String extension = getFileExtension();
            
            if (filename.startsWith("autopdzip")) {
                int lastSeparator = filename.lastIndexOf('/');
                filename = filename.substring(lastSeparator+1);
            } else {
	        filename = filename.concat(extension);
            } 

            String fullfile = mPath.concat(File.separator).concat(filename);
	    FileOutputStream fos = new FileOutputStream(fullfile);	      
	    BufferedReader bis = new BufferedReader(new InputStreamReader(in)); 
	    String nextLine = "";
	        
	    while((nextLine = bis.readLine()) != null){	        
            if(decode == true){
                byte[] bytes = Base64.Decode(nextLine);
                fos.write(bytes);
            }else {
                fos.write(nextLine.getBytes());
                fos.write('\n');
            }
	    }
	        
	    fos.close();
	}
	/**
	 * gets either the attachment file name if the mime mimepart has one, or creates a filename based on the content ID
	 * 
	 * @return the filename for the temporary file
	 */
	private String getFileName(){
	    String attachmentFilename = mMimePart.getAttachmentFilename();
	    String  contentID = mMimePart.getContentID();
	    
	    String filename = null;
	        
	    if(attachmentFilename != null){
	        filename = attachmentFilename;
        } else if (contentID != null && contentID.indexOf('<') != -1) {
            //parse content ID
            int start= contentID.indexOf('<')+1;
            int end = contentID.indexOf('@');
            filename = contentID.substring(start, end);     
        } 
	    return filename;
	}
	/**
	 * determined the correct file extension for mime part based on either contentType or Content ID
	 * .pcap for background packet capture parts  
	 * .txt for plain text
	 * .xml for xml content type
	 * @return the file extension associated with the given mime part 
	 */
	private String getFileExtension(){
	    
	    String contentID = mMimePart.getContentID();
	    String contentType = mMimePart.getContentType();
	    
	    String extension = "";
	    if(contentID.indexOf("BackgroundPacketCapture") >= 0){
            extension = ".pcap";
        } else if(contentType.indexOf("text/plain") >= 0){
            extension = ".txt";
        } else if(contentType.indexOf("text/xml") >= 0){
            extension = ".xml";             
        }
	    return extension;
	}
	/**
	 * Determines wither the mimepart is in base64 encoding nand needs to be decoded
	 * 
	 * @return true if the mime part is in base 64 encoding and needs to be decoded, false otherwise
	 */
	private boolean isBase64(){
	    boolean base64 = false;
	    String encoding = mMimePart.getContentTransferEncoding();
	    if(encoding.equals("base64") ){
            //decode it
           base64 = true;
	    }    
        return base64;    
        
	}

}
