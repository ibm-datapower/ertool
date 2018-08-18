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

package com.ibm.datapower.er.Analytics.Structure;

import org.w3c.dom.Element;
import com.ibm.datapower.er.Analytics.Structure.ItemObject.OBJECT_TYPE;
import com.ibm.datapower.er.Analytics.AnalyticsFunctions;

public class Expression extends ItemStructure {

	public Expression(Element eElement)
	{
		// Used to determine the importance if this expression is matched
		String logLevel = eElement.getAttribute("LogLevel");
		addItem("LogLevel", logLevel.toLowerCase(), OBJECT_TYPE.STRING);

		// this is used to determine if the next expression in the formula
		// needs to be met with this one (AND)
		// or if otherwise either can be matched independently (OR) if a
		// second expression exists in the formula.
		String expNextOperation = eElement.getAttribute("NextOperation").toLowerCase();
		
		if ( expNextOperation.equals("and") )
			addItem("NextOperationAnd", true, OBJECT_TYPE.BOOLEAN);
		else
			addItem("NextOperationAnd", false, OBJECT_TYPE.BOOLEAN);

		// Used to determine the importance if this expression is matched
		addItem("FormulaIDMatch", eElement.getAttribute("FormulaIDMatch"), OBJECT_TYPE.STRING);

		// Used to determine the importance if this expression is matched
		addItem("RequiredFile", eElement.getAttribute("RequiredFile"), OBJECT_TYPE.STRING);
		

		// this will determine what grouping of expressions we are using in
		// this formula
		// when we see the expression group change from its previous value
		// then we restart the expressions/conditions matched
		// we require these to be in order (eg. expressiongroup1,
		// expressiongroup2) in the xml file
		addItem("ExpressionGroup", eElement.getAttribute("ExpressionGroup"), OBJECT_TYPE.STRING);
		

		// this is the CID Section in the document that we need to parse
		// from XML to determine
		// if the expression is a valid match
		addItem("CIDSectionName", AnalyticsFunctions.getAttributeByTag("Section", "Name", eElement, 0), OBJECT_TYPE.STRING);

		// this is if the section is a wildcard meaning it is not an exact
		// match, eg.
		// Domain sections such as WSMAgentStatus
		// (TestDomain-WSMAgentStatus).
		addItem("SectionWildcard", Boolean.parseBoolean(AnalyticsFunctions.getAttributeByTag("Section", "Wildcard",eElement, 0)), OBJECT_TYPE.BOOLEAN);

		addItem("SectionMIME", Boolean.parseBoolean(AnalyticsFunctions.getAttributeByTag("Section", "MIME", eElement, 0)), OBJECT_TYPE.BOOLEAN);

		addItem("SectionBase64", Boolean.parseBoolean(AnalyticsFunctions.getAttributeByTag("Section", "Base64", eElement, 0)), OBJECT_TYPE.BOOLEAN);

		addItem("SectionLineReturn", Boolean.parseBoolean(AnalyticsFunctions.getAttributeByTag("Section", "LineReturn", eElement, 0)), OBJECT_TYPE.BOOLEAN);
		
		addItem("Extension", AnalyticsFunctions.getAttributeByTag("Section", "Extension", eElement, 0), OBJECT_TYPE.STRING);
	}
}
