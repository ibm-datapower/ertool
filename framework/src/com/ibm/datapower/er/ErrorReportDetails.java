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

/**
 * A class representing the Information from Firmware Version. 
 * 
 * @author Alex Teyssandier
 *
 */
public class ErrorReportDetails {
    String mSerial;
    String mVersion;
    String mBuild;
    String mBuildDate;
    String mWatchdogBuild;
    String mRunningDPOS;
    String mXMLAccelerator;
    String mMachineType;
    String mModelType; 
        
    
    public void setSerial(String serial){
        mSerial = serial;
    }
    public void setVersion(String version){
        mVersion = version;
    }
    public void setBuild(String build){
        mBuild = build;
    }
    public void setBuildDate(String buildDate){
        mBuildDate = buildDate;
    }
    public void setWatchdogBuild(String watchdogBuild){
        mWatchdogBuild = watchdogBuild;
    }
    public String setInstalledDPOS(String installedDPOS){
        return mRunningDPOS;
    }
    public void setRunningDPOS(String runningDPOS){
        mRunningDPOS = runningDPOS;
    }
    public void setXMLAccelerator(String xmlAccelerator){
        mXMLAccelerator = xmlAccelerator;
    }
    public void setMachineType(String machineType){
        mMachineType = machineType;
    }
    public void setModelType(String modelType){
        mModelType = modelType;
    }
    public String getSerial(){
        return mSerial;
    }
    public String getVersion(){
        return mVersion;
    }
    public String getBuild(){
        return mBuild;
    }
    public String getBuildDate(){
        return mBuildDate;
    }
    public String getWatchdogBuild(){
        return mWatchdogBuild;
    }
    public String getRunningDPOS(){
        return mRunningDPOS;
    }
    public String getInstalledDPOS(){
        return mRunningDPOS;
    }
    public String getXMLAccelerator(){
        return mXMLAccelerator;
    }
    public String getMachineType(){
        return mMachineType;
    }
    public String getModelType(){
        return mModelType;
    }
    
    
}
