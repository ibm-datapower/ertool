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

import java.io.OutputStream;

public interface PrePostReportProcessor 
{
	
	/**
	 * Will do preprocessing of the entire report for example, HTML headers, directory creation, etc
	 * @throws Exception 
	 *  
	 */
	void preProcess() throws Exception;
   
	/**
     * Will do postprocessing of the entire report, zip file creation for example
	 * @throws Exception 
     *  
     */
    void postProcess() throws Exception;
    
    /**
     * Will get the output stream from the filename to use as the output stream for the report
     * @throws Exception 
     *  
     */
    OutputStream getOutputStreamFromFilename(String filename) throws Exception;
	
}
