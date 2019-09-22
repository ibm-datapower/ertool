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



public interface IPartsProcessor 
{

	
	/**
	 * Process each of the parts of a MIME document, including the Preamble,
	 * the Epilogue and each Parts of the body.
	 * 
	 * @param mimePart MIME document part information
	 * @param writer Character-based output stream for writing post-processed
	 *               information about the part processed
	 *               
	 * @throws IOException may be throws if there are problems reading the MIME
	 *                     part or writing post processing information to the
	 *                     output writer.
	 */
	void process( IPartInfo mimePart, PrintWriter writer ) throws IOException;
}
