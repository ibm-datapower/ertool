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

package com.ibm.datapower.er.Transactions;

import java.util.Comparator;

public class LogMessageSort implements Comparator<LogMessage> {
	@Override
	public int compare(LogMessage o1, LogMessage o2) {
		String condO1 = (String) o1.mLogMsg.get("timestamp");
		String condO2 = (String) o2.mLogMsg.get("timestamp");

		if (condO1 != null && condO2 != null)
			return condO1.compareTo(condO2);

		return 0;
	}
}