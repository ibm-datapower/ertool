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

package com.ibm.datapower.er.Analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ibm.datapower.er.Analytics.MappedCondition.MAPPED_TABLE_POSITION;

public class ConditionsNode implements Cloneable, java.io.Serializable {

	public enum LogLevelType {
		INFO, WARNING, ERROR, CRITICAL
	}

	public enum ConditionSortType {
		DEFAULT, TIMESTAMP
	}

	@Override
	public String toString() {
		return "ConditionsNode{mDisplayName=" + mDisplayName
				+ ", mDisplayMessage=" + mDisplayMessage
				+ ", mFormulaID=" + mFormulaID
				+ ", mConditionsMetCount=" + mConditionsMetCount
				+ ", mExpressionsMetCount=" + mExpressionsMetCount
				+ ", mConditionFound=" + mConditionFound + ", mExpressionsMet="
				+ mExpressionsMet + ", mExpressionsFailed="
				+ mExpressionsFailed + "}";
	}

	public int getConditionsMet() {
		return mConditionsMetCount;
	}

	public void setConditionsMet(int conditionCount) {
		mConditionsMetCount = conditionCount;
	}

	public int getExpressionsMet() {
		return mExpressionsMetCount;
	}

	public void setExpressionsMet(int expressionsMet) {
		mExpressionsMetCount = expressionsMet;
	}

	// set if we have matched a condition and we plan to add it to the end
	// results, no more processing required
	public boolean isConditionFound() {
		return mConditionFound;
	}

	public void setConditionFound(boolean val) {
		mConditionFound = val;
	}

	public boolean isExpressionsMet() {
		return mExpressionsMet;
	}

	public void setExpressionsMet(boolean val) {
		mExpressionsMet = val;
	}

	// used for handling the current expression
	public boolean isExpressionsFailed() {
		return mExpressionsFailed;
	}

	public void setExpressionsFailed(boolean val) {
		mExpressionsFailed = val;
	}

	// used if we tried a group of conditions, failed the expression and are
	// moving to another expression
	public boolean isReqExpressionFailed() {
		return mRequiredExpressionFailed;
	}

	public void setReqExpressionFailed(boolean val) {
		mRequiredExpressionFailed = val;
	}


	public String getFormulaID() {
		return mFormulaID;
	}

	public void setFormulaID(String val) {
		mFormulaID = val;
	}
	
	public String getDisplayName() {
		return mDisplayName;
	}

	public void setDisplayName(String val) {
		mDisplayName = val;
	}

	public String getDisplayMessage() {
		return mDisplayMessage;
	}

	public void setDisplayMessage(String val) {
		mDisplayMessage = val;
	}

	public LogLevelType getLogLevel() {
		return mLogType;
	}

	public void setLogLevel(LogLevelType type) {
		mLogType = type;
	}

	public int getInternalFormulaID() {
		return mInternalID;
	}

	public void setInternalFormulaID(int id) {
		mInternalID = id;
	}

	public boolean isOmitPrintedConditions() {
		return mOmitPrintingMatchedConditions;
	}

	public void setOmitPrintedConditions(boolean val) {
		mOmitPrintingMatchedConditions = val;
	}

	public boolean isTopCondition() {
		return mIsTopCondition;
	}

	public void setTopCondition(boolean val) {
		mIsTopCondition = val;
	}

	public boolean isCollapseSet() {
		return mCollapseResult;
	}

	public void setCollapseSet(boolean val) {
		mCollapseResult = val;
	}

	public String getCategories() {
		return mCategories;
	}

	public void setCategories(String val) {
		mCategories = val;
	}

	// todo: URL encode? Then the javascript files will also have to be designed
	// to handle encoded as well
	public void appendURI(String attrib) {
		if (uriArguments.length() > 0)
			uriArguments += "&";
		uriArguments += attrib;
	};

	public String getURIAttributes() {
		return uriArguments;
	}

	public void setPopupFileName(String popup) {
		mPopupFileName = popup;
	}

	public String getPopupFileName() {
		return mPopupFileName;
	}

	public void addCondition(String condName, String value, int forcePosition) {
		// instead of bothering to check if it exists, just remove if its there
		// or not
		// we overwrite entries instead of adding the same name
		mMappedConditions.remove(condName);

		// -1 = passed as 'set me'
		// -2 = passed as 'dont use me for pipe tables in AnalyticsResults'
		
		int posToSet = forcePosition;
		if ( posToSet == MAPPED_TABLE_POSITION.AUTO_SET.getType() )
			posToSet = ConditionsPosition;

		MappedCondition mc = new MappedCondition(condName.toLowerCase(), value, condName, posToSet);
		
		if ( forcePosition == MAPPED_TABLE_POSITION.AUTO_SET.getType() )
			ConditionsPosition++;
		else if (forcePosition > 0 && ConditionsPosition < forcePosition)
			ConditionsPosition = forcePosition + 1;

		// add the new entry
		mMappedConditions.put(condName.toLowerCase(), mc);
	}
	
	public void addCondition(String condName, String value) {
		addCondition(condName, value, MAPPED_TABLE_POSITION.AUTO_SET.getType());
	}

	public Map<String,MappedCondition> getMappedConditions() { return mMappedConditions; }
	
	public String getCondition(String condName) {
		MappedCondition mc = mMappedConditions.get(condName.toLowerCase());
		String val = null;
		if ( mc != null )
			val = mc.MappedConditionValue;
		
		return val;
	}

	public void setCondenseConditionName(String condName) {
		mCondenseConditionName = condName;
	}

	public String getCondenseConditionName() {
		return mCondenseConditionName;
	}
	
	public void setCondenseCount(int count) {
		mCondenseConditionCount = count;
	}

	public int getCondenseCount() {
		return 	mCondenseConditionCount;
	}
	
	public void setSortConditionName(String condName) {
		mSortConditionName = condName;
	}

	public String getSortConditionName() {
		return mSortConditionName;
	}

	public void setSortMethod(String method) {
		mSortMethod = method;
	}

	public String getSortMethod() {
		return mSortMethod;
	}

	public void setSortOption(String value) {
		// for now we go off the basis that only timestamp formatting and
		// default are supported
		if (value.length() > 0)
			mSortType = ConditionSortType.TIMESTAMP;
		mSortOpt = value;
	}

	public String getSortOption() {
		return mSortOpt;
	}

	public ConditionSortType getSortType() {
		return mSortType;
	}


	public void setSumCondition(String method) {
		mSumCondition = method;
	}

	public String getSumCondition() {
		return mSumCondition;
	}

	public int getConditionID() {
		return mConditionID;
	}

	private int mConditionsMetCount = 0;
	private int mExpressionsMetCount = 0;
	private boolean mConditionFound = false;
	private boolean mExpressionsMet = false;
	private boolean mExpressionsFailed = false;
	private boolean mRequiredExpressionFailed = false;
	private int mInternalID = 0; // used to determine placement between other
									// formulas tested

	private int mConditionID = 0; // tracks unique formula result (conditionsnode) so we can properly sort the results
	
	private String mFormulaID = "";
	private String mDisplayName = "";
	private String mDisplayMessage = "";
	private String mCategories = "";
	private String mSortConditionName = "";
	private String mSortMethod = "ascending"; // default ascending, optional is
												// descending, set in
												// ConditionSort.java

	private String mCondenseConditionName = "";
	private int mCondenseConditionCount = 0;
	
	private ConditionSortType mSortType = ConditionSortType.DEFAULT; // timestamp
																		// will
																		// have
																		// custom
																		// sort
																		// opt
	private String mSortOpt = ""; // this will be the custom timestamp format
									// (or maybe later other formatting?)

	private String mSumCondition = ""; // name of the sum condition
	
	private String uriArguments = "";
	private String mPopupFileName = "";

	// show this condition on the top
	private boolean mIsTopCondition = false;

	// if we add a gui probably change this to a class/struct so we hold the url
	// distinct of the description msg
	public ArrayList<ReferenceURL> mURLs = new ArrayList<ReferenceURL>();

	public ArrayList<String> matchedConditions = new ArrayList<String>();

	private LogLevelType mLogType = LogLevelType.INFO;

	// this is set if we want to omit the printed results for conditions met
	private boolean mOmitPrintingMatchedConditions = false;

	// this is set to true if we want to collapse the result
	private boolean mCollapseResult = false;

	// track the condition results (ConditionName defined) we get off of this
	// node
	private Map<String, MappedCondition> mMappedConditions = new HashMap<String, MappedCondition>();

	private static int LastConditionID = 0;

	public ArrayList<Integer> previousPositionsMatched = new ArrayList<Integer>();
	public ArrayList<Integer> previousFailedPositions = new ArrayList<Integer>();
	
	private int ConditionsPosition = 0;
	
	public boolean nodeHasCloned = false;
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		ConditionsNode node = (ConditionsNode) super.clone();
		node.matchedConditions = (ArrayList<String>) matchedConditions.clone();
		// reset all our operations to make sure we still process as if the node
		// is just instantiated
		node.setConditionFound(false);
		node.setExpressionsMet(false);
		node.setConditionsMet(0);
		node.ReinstantiateMappedNodes(this.mMappedConditions);
		return (Object) node;
	}
	
	public void ReinstantiateMappedNodes(Map<String,MappedCondition> prevConditions)
	{
		this.mMappedConditions = new HashMap<String, MappedCondition>();
		for(Map.Entry<String, MappedCondition> entry : prevConditions.entrySet()) {
			try {
				this.mMappedConditions.put(entry.getKey(), (MappedCondition)entry.getValue().clone());
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void DumpMappedNodes()
	{
		for(Map.Entry<String, MappedCondition> entry : mMappedConditions.entrySet()) {
			Logger.getRootLogger().debug(entry.getKey() + " : " + entry.getValue().MappedConditionValue);
		}
	}
	
	public ConditionsNode()
	{
		LastConditionID++;
		mConditionID = LastConditionID;
	}
}
