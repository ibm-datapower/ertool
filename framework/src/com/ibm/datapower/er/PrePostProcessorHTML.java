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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.*;


public class PrePostProcessorHTML implements PrePostReportProcessor {

    OutputStream mOutputStream;
    

    public PrePostProcessorHTML(){
        mOutputStream = null;
    }
    
    
    @Override
    public void postProcess() throws IOException {        
        if(mOutputStream != null){
            mOutputStream.close();           
        }
    }

    @Override
    public void preProcess() throws Exception {
        LogManager.getRootLogger().debug("Running HTML PreProcessor");        
    }


    @Override
    public OutputStream getOutputStreamFromFilename(String filename) throws FileNotFoundException {        
        if (filename == null){
            mOutputStream = System.out;
        } else {
            mOutputStream = new FileOutputStream(filename);
        }
        return mOutputStream;               
    }
     

}
