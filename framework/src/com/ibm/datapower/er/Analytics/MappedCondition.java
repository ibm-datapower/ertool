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

public class MappedCondition implements Cloneable, java.io.Serializable  {
	public String MappedConditionName = "";
	public String MappedConditionValue = "";
	public String MappedConditionNameOriginalCase = "";
	public int MappedConditionPosition = 0;

	public enum MAPPED_TABLE_POSITION {
		NO_SET(-2),
		AUTO_SET(-1);
		
		private final int type;
	    private MAPPED_TABLE_POSITION(int type) {
	        this.type = type;
	    }

	    public int getType() {
	        return type;
	    }
	}
	
	public MappedCondition(String lowercaseConditionName, String conditionValue, String originalcaseConditionName, int conditionPosition )
	{
		MappedConditionName = lowercaseConditionName;
		MappedConditionValue = conditionValue;
		MappedConditionNameOriginalCase = originalcaseConditionName;
		MappedConditionPosition = conditionPosition;
	}

	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		MappedCondition mapCond = new MappedCondition(MappedConditionName, MappedConditionValue, MappedConditionNameOriginalCase, MappedConditionPosition);
		return mapCond;
	}
}
