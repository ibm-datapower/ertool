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

package com.ibm.datapower.er.mgmt;

import java.io.Reader;
import java.io.Writer;

/**
 * The Message interface provides methods to create SOMA requests.  
 * <p> 
 * This interface is the glue between the SoapMessage and 
 * SoapManagmentRequest classes.  Most of the methods from this
 * interface refers to the SoapManagementRequest class.
 * 
 * @author DataPower Technology, Inc.
 * @author Dana Numkena
 *
 */
public interface Message {
    
    /**
     * Write message to writer object
     * 
     * @param Writer to write message out to
     * @throws ERMgmtIOException
     */
    public void write(Writer writer) throws ERMgmtIOException;
    
    /**
     * Parse response message pointed by reader object
     * 
     * @param Reader to read response from
     * @throws ERMgmtIOException
     */
    public void parse(Reader reader) throws ERMgmtIOException;
}
