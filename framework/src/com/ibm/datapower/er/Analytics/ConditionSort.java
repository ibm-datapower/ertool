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

import java.text.SimpleDateFormat;
import java.util.Comparator;

import com.ibm.datapower.er.Analytics.ConditionsNode.ConditionSortType;

public class ConditionSort implements Comparator<ConditionsNode> {
	@Override
	public int compare(ConditionsNode o1, ConditionsNode o2) {
		// if no sort condition is given, we will use the internal conditions node ID which we instantiate when the node is created
		if (o1.getSortConditionName().length() < 1 || o2.getSortConditionName().length() < 1)
		{
			if (!o1.getSortMethod().equals("descending")) {
			if ( o1.getConditionID() > o2.getConditionID() )
				return 1;
			else
				return 0;
			}
			else
			{
				if ( o1.getConditionID() > o2.getConditionID() )
					return -1;
				else
					return 0;
			}
		}

		String condO1 = "";
		String condO2 = "";
		if (o1.getSortConditionName().equals("condensecondition")
				&& o2.getSortConditionName().equals("condensecondition")) {
			condO1 = Integer.toString(o1.getCondenseCount());
			condO2 = Integer.toString(o2.getCondenseCount());
		} else {
			condO1 = o1.getCondition(o1.getSortConditionName());
			condO2 = o2.getCondition(o2.getSortConditionName());
		}
		boolean useTimestampCompare = false;

		SimpleDateFormat fmt = null;
		if (o1.getSortType() == ConditionSortType.TIMESTAMP && o1.getSortOption().length() > 0) {
			fmt = new SimpleDateFormat(o1.getSortOption());
			useTimestampCompare = true;
		}

		try {
			if (condO1 != null && condO2 != null) {
				if (!o1.getSortMethod().equals("descending")) {
					if (useTimestampCompare) {
						return fmt.parse(condO1).compareTo(fmt.parse(condO2));
					} else {
						try {
							double num1 = Double.parseDouble(condO1);
							double num2 = Double.parseDouble(condO2);
							if (num1 > num2)
								return 1;
							else if (num2 > num1)
								return -1;
							else
								return 0;

						} catch (Exception ex) {
						}
						return condO1.compareTo(condO2);
					}
				} else {
					if (useTimestampCompare) {
						try {
							int parseValue = fmt.parse(condO2).compareTo(fmt.parse(condO1));
							return parseValue;
						} catch (Exception ex) {
							if (!o1.getSortMethod().equals("descending"))
								return -1;
							else
								return 1;
						}
					} else {
						try {
							double num1 = Double.parseDouble(condO1);
							double num2 = Double.parseDouble(condO2);
							if (num2 > num1)
								return 1;
							else if (num1 > num2)
								return -1;
							else
								return 0;
						} catch (Exception ex) {
						}
						return condO2.compareTo(condO1);
					}
				}
			}
		} catch (Exception ex) {

		}
		return 0;
	}
}