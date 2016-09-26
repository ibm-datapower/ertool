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


import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.datapower.er.ERCommandLineArgs;

public class ERFrameworkRun{
    
	public static boolean mIsLoggerConfigured = false;
	
    public static void main(String[] args) throws Exception{
        // log4j setup
        BasicConfigurator.configure();
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        mIsLoggerConfigured = true;
        
        ERFrameworkRun erf = new ERFrameworkRun(args);                
        erf.run();
    }

       ERCommandLineArgs mParams;
       String mOutputReport;
       String mInputErrorReport;
       String mProcessorFile;
       Properties mProperties;
       
       ReportProcessor mReportProcessor;
       

        
       public ERFrameworkRun(String[] args){
            mParams = new ERCommandLineArgs(args);
            mOutputReport = null;
            mInputErrorReport = null;
            mProcessorFile = null;
            mProperties = null;

            mReportProcessor = new ReportProcessor();
        }
        public int run() throws Exception{
            if(loadCLA() == -1){
                return -1;
            }
            
            mProperties = new Properties();
            mProperties.load(new FileInputStream(mProcessorFile)); 
            
            
            String prepostprocessor = mProperties.getProperty("report.prepostprocessor");                
            Class toLoad = Class.forName(prepostprocessor);               
            PrePostReportProcessor ip = (PrePostReportProcessor) toLoad.newInstance();
            
            
            mReportProcessor.load(mInputErrorReport);
            mReportProcessor.setPrePostProcessor(ip);
            loadPartsProcessors();
            mReportProcessor.setFilename(mOutputReport);
            mReportProcessor.run();
                                   
            return 0;
        }
        
        public int loadCLA(){
            String usage = "A tool for processing error reports using a given properties mProcessorFile\n" +
            "ertool [OPTIONS] -i ERROR REPORT FILE \n" +
            "OPTIONS\n" +
            "    -p PROPERTIES_FILENAME\n" +
            "          The special properties file indicating custom parts processor to use.\n" +
            "    -i ERROR_REPORT_FILE\n" +
            "          The location of the error report to be processed.\n" +
            "    -o OUTPUT_FILE\n" +
            "          The destination location of the zip file or html file for the processed error report\n" +
            "    -v\n" +
            "          Verbose output of program\n";
            mParams.setUsageText(usage);
            mParams.parse();
            
            
            if(!mParams.containsKey("-i")){                
                Logger.getRootLogger().fatal("Error: you must specify an input file");
                return -1;
            } else{
                mInputErrorReport = mParams.getProperty("-i");
            }        
            if(mParams.containsKey("-o")){
                mOutputReport = mParams.getProperty("-o");            
            }
            if(mParams.containsKey("-p")){
                mProcessorFile = mParams.getProperty("-p");
            } else {                
                mProcessorFile = "properties/htmldefault.properties";
            }
            if(!mParams.containsKey("-v")){   
                Logger.getRootLogger().setLevel(Level.OFF);
            }
            return 0;
        }    
        
        @SuppressWarnings("unchecked")
        public void loadPartsProcessors() throws Exception {
            Enumeration<String> keys =  (Enumeration<String>) mProperties.propertyNames();
            //for every key
            while(keys.hasMoreElements()){
                String key = keys.nextElement();
                int start;

                String processorType = null;
                Class toLoad = null;
                
                if((start = key.indexOf("PartsProcessor.")) < 0){
                    key = null;
                } else {
                    processorType = mProperties.getProperty(key);
                    try {
                        toLoad = Class.forName(processorType);    
                    } catch (ClassNotFoundException e){
                        Logger.getRootLogger().debug(processorType +  "not found, skipping...");
                        continue;
                    }
                    key = key.substring(start+15);
                }

                
                if(key == null){
                    continue;
                }
                else if(key.equals("default")){
                    mReportProcessor.addPartsProcessor((IPartsProcessor) toLoad.newInstance(), null);
                } else {
                    mReportProcessor.addPartsProcessor((IPartsProcessor) toLoad.newInstance(), key);
                }
            }
        } 
       
 
 
    
}

