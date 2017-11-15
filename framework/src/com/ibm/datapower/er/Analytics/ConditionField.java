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

package com.ibm.datapower.er.Analytics;

public class ConditionField {
	private int mFieldPosition = 0;
	private String mFieldValue = "";
	private String mParsedFieldValue = "";
	private boolean mIsParsedFieldValue = false;
	
	public enum REG_GROUP_TYPE {
		MATCH_ALLREGGROUP(1),
		MATCH_ORDERED(2),
		MATCH_ALL_RESULT(3), // ALL_RESULT and ORDERED inherit ALLREGGROUP, COUNT and NONE do not use regular expression grouping 
		MATCH_COUNT(4),
		MATCH_SUM(5),
		MATCH_NONE(6);

		private final int type;
	    private REG_GROUP_TYPE(int type) {
	        this.type = type;
	    }

	    public int getType() {
	        return type;
	    }
	}
	private String mRegGroup = "";
	private REG_GROUP_TYPE mRegGroupType = REG_GROUP_TYPE.MATCH_NONE;
	private String mConditionName = "";
	private String mOperation = "";
	private String mValue = "";
	private String mCondRegExp = "";
	private String mCondNextOperation = "";
	private boolean mConditionOperAnd = false;
	
	private String mConversionType = "";
	
	private String mOverrideValue = "";
	
	public ConditionField(int fieldPos, String fieldPosValue, String reggroup, String condName, String oper, String value, String condRegEXP, String nextOper, 
			String conversionType, String overrideValueSetting)
	{
		setFieldPosition(fieldPos);
		setFieldValue(fieldPosValue);
		setRegGroup(reggroup);
		setConditionName(condName);
		setOperation(oper);
		setValue(value);
		setConditionRegEXP(condRegEXP);
		setConditionNextOperation(nextOper);
		setConversionType(conversionType);
		setOverrideValue(overrideValueSetting);
	}
	
	public ConditionField()
	{
		
	}

	public void setFieldPosition(int pos)
	{
		mFieldPosition = pos;
	}

	public void setFieldValue(String fieldpos)
	{
		mFieldValue = fieldpos;
		if ( getFieldValue().startsWith("{") )
		{
			try
			{
			mParsedFieldValue = getFieldValue().substring(1, getFieldValue().length() - 1);
			mIsParsedFieldValue = true;
			}catch(Exception ex) { } // catching just in case this substring pull fails
		}
	}

	public void setRegGroup(String reggroup)
	{
		mRegGroup = reggroup;
		String regGroupLwr = mRegGroup.toLowerCase();

		switch(regGroupLwr)
		{
		case "all":
			mRegGroupType = REG_GROUP_TYPE.MATCH_ALLREGGROUP;
			break;
		case "allresult":
			mRegGroupType = REG_GROUP_TYPE.MATCH_ALL_RESULT;
			break;
		case "ordered":
			mRegGroupType = REG_GROUP_TYPE.MATCH_ORDERED;
			break;
		case "count":
			mRegGroupType = REG_GROUP_TYPE.MATCH_COUNT;
			break;
		case "sum":
			mRegGroupType = REG_GROUP_TYPE.MATCH_SUM;
			break;
		}
	}
	
	public void setConditionName(String condName)
	{
		mConditionName = condName;
	}
	
	public void setOperation(String oper)
	{
		mOperation = oper;
	}
	
	public void setValue(String val)
	{
		mValue = val;
	}

	public void setConditionRegEXP(String regexp)
	{
		mCondRegExp = regexp;
	}

	public void setConditionNextOperation(String nextOper)
	{
		mCondNextOperation = nextOper;

		// this is for determining whether the next condition must be
		// met (AND) or if otherwise it is optional (OR)
		// in the case of an OR statement if the first condition fails
		// we can try the second if one is available
		if (mCondNextOperation.toLowerCase().equals("and"))
			mConditionOperAnd = true;
	}
	public void setConversionType(String convType)
	{
		mConversionType = convType;
	}
	public void setOverrideValue(String value)
	{
		mOverrideValue = value;
	}

	public int getFieldPosition() { return mFieldPosition; }
	public String getFieldValue() { return mFieldValue; }
	public String getParsedFieldValue() { return mParsedFieldValue; }
	public boolean isParsedFieldValue() { return mIsParsedFieldValue; }
	public String getRegGroup() { return mRegGroup; }
	public REG_GROUP_TYPE getRegGroupType() { return mRegGroupType; }
	public String getConditionName() { return mConditionName; }
	public String getOperation() { return mOperation; }
	public String getValue() { return mValue; }
	public String getConditionRegEXP() { return mCondRegExp; }
	public String getConditionNextOperation() { return mCondNextOperation; }
	public boolean getConditionOperAnd() { return mConditionOperAnd; }
	public String getConversionType() { return mConversionType; }
	public String getOverrideValue() { return mOverrideValue; }
}
