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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;



public class PartsProcessorsXForm implements IPartsProcessor
{
	public PartsProcessorsXForm(String xslPath)
	{
		try {
	        mXslPath = xslPath;
	        mXformFactory = TransformerFactory.newInstance();
            mXformer = mXformFactory.newTransformer(new StreamSource(new File(mXslPath)));
        } 
		catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
	}
	
	public void process(IPartInfo mimePart, PrintWriter writer) throws IOException {
		try {
	        Result result = new StreamResult(writer);
	        Source xmlSource = new StreamSource(mimePart.getBodyStream());
		    mXformer.transform(xmlSource, result);
		} 
		catch (TransformerException e) {
			throw new IOException(e);
		}
	}
	
	TransformerFactory mXformFactory;
	Transformer mXformer;
	String mXslPath;
}
