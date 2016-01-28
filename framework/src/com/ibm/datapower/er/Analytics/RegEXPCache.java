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

import java.util.regex.Matcher;

import com.ibm.datapower.er.Analytics.Structure.RunFormula;

public class RegEXPCache {
	public RegEXPCache(RunFormula formula, String regexp, String invalue, Matcher inmatch)
	{
		regEXPQuery = regexp;
		matcher = inmatch;
		queryValue = invalue;
		rFormula = formula;
	}
	
	public void setRunFormula(RunFormula formula) { rFormula = formula; }
	
	public Matcher getMatcher() { return matcher; }
	public String getRegEXPQuery() { return regEXPQuery; }
	public String getQueryValue() { return queryValue; }
	public RunFormula getRunFormula() { return rFormula; }
	
	private RunFormula rFormula = null;
	private Matcher matcher = null;
	private String regEXPQuery = "";
	private String queryValue = "";
}
