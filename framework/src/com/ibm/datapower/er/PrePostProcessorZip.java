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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.*;

public class PrePostProcessorZip implements PrePostReportProcessor {
    //output stream for the zip report (the report processor will use system.out)
    ZipOutputStream mZipOutputStream;
    
    File[] filenames;
    File mTempDir;

    public PrePostProcessorZip(){
        mZipOutputStream = null;
    }
    
    
    @Override
    public void postProcess() throws IOException {        
        loadFileNames();
        createZip();
    }

    @Override
    public void preProcess() throws Exception {
        mTempDir = createTemporaryDirectory(); 
        PartsProcessorZIP.mPath = mTempDir.getAbsolutePath();;
    }
    @Override
    public OutputStream getOutputStreamFromFilename(String filename){
        //use the filename to create the output stream for the zip report, 
        //but return system.out as the output stream for the report processor        
        if(filename == null){
            mZipOutputStream = new ZipOutputStream(System.out);
        }else{
            String fullzipfile = filename;
            File zipfile = new File(fullzipfile);
            if(zipfile.isDirectory()){
                fullzipfile = filename.concat(File.separator).concat("report"+Long.toString(System.nanoTime())+".zip");
                zipfile = new File(fullzipfile);
            }
            try {                
                mZipOutputStream = new ZipOutputStream(new FileOutputStream(zipfile));
            } catch (FileNotFoundException e) {
                //if file is not found, just make it system.out
                LogManager.getRootLogger().debug(zipfile.getAbsoluteFile() +  " not found, setting output to System.out");
                System.out.println("file not found");
                mZipOutputStream = new ZipOutputStream(System.out);
            }
        }
        return System.out;
    }
    
    /**
     * creates the temporary directory to hold all the processed files. 
     * @return the File representing the temporary directory
     * @throws IOException if the temporary directory cannot be creates
     */
    public File createTemporaryDirectory() throws IOException{
        File temp;      
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        LogManager.getRootLogger().debug("Creating temp file: " + temp.getAbsolutePath());
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();
        return temp;    
    }  
    
    /**
     * obtain the list of all the filenames in the temporary directory to zip up into a zipfile
     */
    public void loadFileNames(){  
        filenames = mTempDir.listFiles();        
    }
    /**
     * Creates a Zip with the output as either System.out, or a defined output file
     * 
     *  
     * @throws IOException
     */
    public void createZip() throws IOException{
        //Create a buffer for reading the files
        byte[] buf = new byte[1024];
        

        try {            

            for (int i=0; i<filenames.length; i++) {                
                FileInputStream in = new FileInputStream(filenames[i]);
                String name = filenames[i].getName();
                mZipOutputStream.putNextEntry(new ZipEntry(name));
                int len;
                while ((len = in.read(buf)) > 0) {
                    mZipOutputStream.write(buf, 0, len);                    
                }
                mZipOutputStream.closeEntry();
                in.close();
            }

        } catch (IOException e) {
            throw e;
        } finally{
            if(mZipOutputStream != null){                
                mZipOutputStream.flush();
                mZipOutputStream.close();    
            }
                        
        }

    }




}
