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

import java.util.concurrent.Callable;

public class RunTransaction implements Callable {

	public ParseTransx mTransactions = null;
	public String mLogName = "";
	
	public RunTransaction(ParseTransx transx, String logName)
	{
		mTransactions = transx;
		mLogName = logName;
	}
	@Override
	public Object call() throws Exception {
		run();
		return (Object) this;
	}

	public void run() {
			mTransactions.runTransaction(mLogName);
	}

}
