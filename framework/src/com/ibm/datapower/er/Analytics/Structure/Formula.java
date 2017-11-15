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

import com.ibm.datapower.er.FirmwareInputStream;
import com.ibm.datapower.er.Analytics.Structure.ItemObject.OBJECT_TYPE;

public class Formula extends ItemStructure {

	public Formula(Element fElement, int id, boolean multiDocs) {
		addItem("ID", id, OBJECT_TYPE.INTEGER);

		addItem("Element", fElement, OBJECT_TYPE.ELEMENT);
		// the header 'name' of the formula, this will require parsing of {
		// } tag sections which denote XML sections we pull from
		// expressions
		// <Name>This is an error!</Name>

		String dispName = FirmwareInputStream.getValueByTag("Name", fElement);
		mMultiDocs = multiDocs;
		if (multiDocs && dispName != null && dispName.length() > 0) {
			addItem("Name", "{Condition:ReportFile} - " + dispName, OBJECT_TYPE.STRING);
		} else {
			addItem("Name", dispName, OBJECT_TYPE.STRING);
		}

		// This is the description message which serves as a subnode to
		// provide more detail of the error, it also requires parsing of { }
		// tags.
		// <DisplayMessage>data for more detail</DisplayMessage>
		addItem("DisplayMessage", FirmwareInputStream.getValueByTag("DisplayMessage", fElement), OBJECT_TYPE.STRING);

		// List of URLs the user can use to understand the problem better
		// <URL description="Blah blah: " >http://somelink/</URL>
		addItem("UrlNodes", fElement.getElementsByTagName("URL"), OBJECT_TYPE.NODELIST);

		// If set to 'true' then we omit printing the conditions met by the
		// formula
		String omitPrintedConditions = fElement.getAttribute("OmitPrintingMetConditions");
		addItem("OmitConditions", Boolean.parseBoolean(omitPrintedConditions), OBJECT_TYPE.BOOLEAN);

		// Used to determine if we want to override the position
		String topPositionRes = fElement.getAttribute("TopPositionResult");
		addItem("TopPosition", Boolean.parseBoolean(topPositionRes), OBJECT_TYPE.BOOLEAN);

		// Expressions to match on, LogLevel: Critical, Error, Warning, Notice.
		// NextOperation of And / Or.
		// <Expression LogLevel="Severity" NextOperation="And|Or
		addItem("Expression", fElement.getElementsByTagName("Expression"), OBJECT_TYPE.NODELIST);

		// grab the collapse attribute and see if we want the result to be
		// collapsed in HTML
		String collapseAttrib = fElement.getAttribute("CollapseResult");
		addItem("CollapseResult", Boolean.parseBoolean(collapseAttrib), OBJECT_TYPE.BOOLEAN);

		// grab the categories that will allow matching in html
		addItem("FormulaID", fElement.getAttribute("FormulaID"), OBJECT_TYPE.STRING);

		// grab the categories that will allow matching in html
		addItem("Categories", fElement.getAttribute("Categories").toLowerCase(), OBJECT_TYPE.STRING);

		// name of the condition to sort by
		addItem("SortCondition", fElement.getAttribute("SortCondition").toLowerCase(), OBJECT_TYPE.STRING);

		// method of sort (ascending/descending) default is ascending
		addItem("SortMethod", fElement.getAttribute("SortMethod").toLowerCase(), OBJECT_TYPE.STRING);

		// sort option (eg timestamp formatting) YYYYMMDD etc.
		addItem("SortOption", fElement.getAttribute("SortOption"), OBJECT_TYPE.STRING);
		// leave this as-is do not lower case! Case-sensitive string.

		addItem("Popup", fElement.getAttribute("Popup"), OBJECT_TYPE.STRING);

		// name of the condition to make a sum ofPopup
		addItem("SumCondition", fElement.getAttribute("SumCondition").toLowerCase(), OBJECT_TYPE.STRING); // force
																											// lower
																											// case

		// name of the condition to make a sum ofPopup
		addItem("CondenseCondition", fElement.getAttribute("CondenseCondition").toLowerCase(), OBJECT_TYPE.STRING); // force
																													// lower
																													// case
	}

	public String getIdentifier() {
		if (mIdent.length() > 0)
			return mIdent;

		String formulaID = (String) getItem("FormulaID").getObject();
		if (formulaID != null && formulaID.length() > 0) {
			mIdent = formulaID;
			return formulaID;
		}

		String shortFormulaName = (String) getItem("Name").getObject();

		if (shortFormulaName == null || shortFormulaName.length() < 1)
			shortFormulaName = "Unknown";

		int length = 20;
		if (shortFormulaName.length() > length)
			shortFormulaName = shortFormulaName.substring(0, length);

		mIdent = shortFormulaName;

		return shortFormulaName;
	}

	private String mIdent = "";
	public boolean mMultiDocs = false;
}
