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
	String[] filesToTest = { "C:\\pmr's\\40399\\recreate\\error-report.6803814.20130919114309529EDT.txt.gz.tmp"
	/*
	 * "C:\\pmr's\\68489\\68489.227.000.error-report.6801579.20140821105058812PDT.txt.gz"
	 * ,
	 */
	/*
	 * "C:\\pmr's\\29899.122.000.report",
	 * "C:\\pmr's\\51936\\HIGHCPUp05error-report.6801503.20130223114027020PST.txt.gz.tmp"
	 * ,
	 * "C:\\pmr's\\23719.499.000.error-report.68A4492.20120214162716EST.txt.gz",
	 * "C:\\pmr's\\17205\\error-report.68A2778.20130304113011531PST.txt.gz.tmp",
	 * "c:\\pmr's\\01672.227.000.error-report1.txt.gz.tmp",
	 * "c:\\pmr's\\69153.379.000.error-reportprd01.txt.gz.tmp",
	 * "C:\\pmr's\\41641\\error-report.txt.gz.tmp",
	 * "C:\\pmr's\\25041\\25041.499.000.tx2dpxb18afterrebooterror-report.txt.gz.tmp"
	 * /*,
	 * "C:\\pmr's\\40399\\recreate\\error-report.6803814.20130919114309529EDT.txt.gz.tmp"
	 * ,
	 */
	/* "C:\\pmr's\\40399\\error-report.68A0778.20130903113713562EDT.txt.gz.tmp" */};

	public void testParseTransx() {
		ParseTransx transx = new ParseTransx();
		transx.SetTransactionRulesFile("c:\\dpgithub\\dptransx.xml");
		for (int f = 0; f < filesToTest.length; f++) {
			transx.setFileLocation(filesToTest[f]);
			transx.doParse("c:\\testout.txt", "EDT");
		}
	}
}
