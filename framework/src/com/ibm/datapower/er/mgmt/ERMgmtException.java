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

//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * General exception from ERMgmt.
 * <p>
 * This is a superclass to a number of ERMgmt exceptions.
 * 
 * @author Dana Numkena
 */

public class ERMgmtException extends Exception {
    
    //private static Logger logger=Logger.getLogger("er.mgmt.exception.log"); 
    
    ERMgmtException (String a) {
        super(a);        
        //logger.log(Level.WARNING, a);
        //System.out.print(a);
    }
    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the Throwable.getMessage() method).
     * @param e       the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    ERMgmtException (String message, Throwable e){
    	super(message, e);
    }
}
