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

import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.w3c.dom.Element;

import com.ibm.datapower.er.FirmwareInputStream;
import com.ibm.datapower.er.Analytics.AnalyticsProcessor;
import com.ibm.datapower.er.Analytics.ConditionField;
import com.ibm.datapower.er.Analytics.ConditionsNode;
import com.ibm.datapower.er.Analytics.DocumentSection;
import com.ibm.datapower.er.Analytics.AnalyticsFunctions;

public class RunFormula implements Callable {

	public DocumentSection documentSet = null;
	public ArrayList<ConditionsNode> condNodes;
	public Formula formula;
	public int cidSectionID;
	public Element expElement;
	public boolean expressionsMatched = false;
	public boolean nextExpressionAnd = false;
	public int formulaPos;
	public AnalyticsProcessor mProc = null;
	public int docID = 0;
	public boolean anyExpressionsMatched = false;
	public ArrayList<ConditionField> cFields = null;
	public String xPathQuery = "";
	public String isSectionVariable = "";
	public boolean bIsSectionVariable = true;
	public String regExp = "";
	public String idxSearch = ""; // narrow down document by indexOf instead of regexp
	public String requiredFile = ""; // narrow down document by indexOf instead of regexp
	
	public RunFormula(AnalyticsProcessor proc, DocumentSection docSet,
			int docPos, ArrayList<ConditionsNode> nodes, Formula formulaData,
			int sectionID, Element ele, boolean expressionAnd, ArrayList<ConditionField> fields, String reqFile) {
		docID = docPos;
		mProc = proc;
		documentSet = docSet;
		condNodes = nodes;
		formula = formulaData;
		cidSectionID = sectionID;
		expElement = ele;
		nextExpressionAnd = expressionAnd;
		formulaPos = (int) formula.getItem("ID").getObject();

		requiredFile = reqFile;
		
		loadVars();
		
		cFields = fields;
	}

	public RunFormula(AnalyticsProcessor proc, DocumentSection docSet,
			int docPos, ArrayList<ConditionsNode> nodes, Formula formulaData,
			int sectionID, Element ele, boolean expressionAnd,
			ArrayList<ConditionField> fields, boolean multicore) {
		docID = docPos;
		mProc = proc;
		documentSet = docSet;
		condNodes = nodes;
		formula = formulaData;
		cidSectionID = sectionID;
		expElement = ele;
		nextExpressionAnd = expressionAnd;
		formulaPos = (int) formula.getItem("ID").getObject();
		cFields = fields;

		loadVars();
		
		if (!multicore)
			run();

	}
	
	private void loadVars()
	{
		// this is the XPath expression of the value we are trying to extract to
		// compare against our condition
		xPathQuery = AnalyticsFunctions.getAttributeByTag("Section", "XPath", expElement,
				cidSectionID);

		// if set to false then we don't use it for the Condition:SectionName
		// variable in parsing the message
		isSectionVariable = AnalyticsFunctions.getAttributeByTag("Section", "SectionName",
				expElement, 0);

		// this is kind of a reverse operand, if its false then we don't want to
		// use it, otherwise we leave the old functionality/design of always
		// true
		if (isSectionVariable.toLowerCase().equals("false"))
			bIsSectionVariable = false;

		// This is the regular expression we use to break apart the value in
		// the error report (from xPath)
		regExp = FirmwareInputStream.getValueByTag("RegExp",
				expElement);

		// This is the regular expression we use to break apart the value in
		// the error report (from xPath)
		idxSearch = FirmwareInputStream.getValueByTag("IndexSearch",
				expElement);
	}

	public Object call() {
		run();
		return (Object) this;
	}

	public void run() {
		anyExpressionsMatched = mProc.handleRunFormula(this);
	}
	
	public Formula getFormula() {
	return formula;	
	}
}
