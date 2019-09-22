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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
/**
 * A class to print out the Backtrace with surrounding html code. Will not print out start <HTML> tags
 * @author alex
 *
 */
public class PartsProcessorBacktraceZIP extends PartsProcessorBacktrace {
    String mTempDir = "";
    /**
     * Constructor for Parts Processor Backtrace
     */
    public PartsProcessorBacktraceZIP() {
        super();
    }

    public void setTempDir(String sTempDir){
        mTempDir = sTempDir;
    }
    
    /**
     * Inserts the target links and <pre> formatting tags on the output from partsProcessorBacktrace
     * 
     * @see PartsProcessorBacktrace
     */
    public void process(IPartInfo mimePart, PrintWriter writer) throws IOException{
        
        String contentType = mimePart.getContentType();
        String contentID = mimePart.getContentID();  
        String extension = "";
        String mFilename = "";
        
        
              
        int start = contentID.indexOf('<')+1;
        int end = contentID.indexOf('@');
        mFilename  = contentID.substring(start, end);
        
        if(contentType.indexOf("text/plain") >= 0){
            extension = ".txt";
        } else if(contentType.indexOf("text/xml") >= 0){
            extension = ".xml";             
        }
        mFilename = mFilename.concat(extension);
               
        String fullfile = mTempDir.concat(File.separator).concat(mFilename); 
        FileOutputStream fos = new FileOutputStream(fullfile);   
        
        PrintWriter tempWriter = new PrintWriter(fos);
        
        super.process(mimePart, tempWriter);
        
    }

}
