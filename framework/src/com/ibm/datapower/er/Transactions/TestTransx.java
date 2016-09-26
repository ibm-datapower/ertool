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

package com.ibm.datapower.er.Transactions;

import junit.framework.TestCase;

public class TestTransx extends TestCase {
	String[] filesToTest = { "" };

	public void testParseTransx() {
		ParseTransx transx = new ParseTransx();
		transx.SetTransactionRulesFile("c:\\dpgithub\\dptransx.xml");
		for (int f = 0; f < filesToTest.length; f++) {
			transx.setFileLocation(filesToTest[f]);
			transx.doParse("c:\\testout.txt", "EDT", false, "info");
		}
	}
}
