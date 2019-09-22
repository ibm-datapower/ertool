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

import java.io.IOException;
import java.io.PrintWriter;
/**
 * A class to print out the Backtrace with surrounding html code. Will not print out start <HTML> tags
 * @author alex
 *
 */
public class PartsProcessorBacktraceHTML extends PartsProcessorBacktrace {

    /**
     * Constructor for Parts Processor Backtrace
     */
    public PartsProcessorBacktraceHTML() {
        super();
    }
    
    /**
     * Inserts the target links and <pre> formatting tags on the output from partsProcessorBacktrace
     * 
     * @see PartsProcessorBacktrace
     */
    public void process(IPartInfo mimePart, PrintWriter writer) throws IOException{
        String sectionName = mimePart.getContentID();        
        int start = sectionName.indexOf('<')+1;
        int end = sectionName.indexOf('@');
        sectionName = sectionName.substring(start, end);
        
        writer.print("<br/><br/>");
        writer.println("<span class=\"element\">");
        writer.println("<a name=\"" + sectionName + "\">");
        writer.println(sectionName);
        writer.println("</a>");
        writer.println("</span>");
        writer.write("<pre>");
        super.process(mimePart, writer);
        writer.write("</pre>");
    }

}
