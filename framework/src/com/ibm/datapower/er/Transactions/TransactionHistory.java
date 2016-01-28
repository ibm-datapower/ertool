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

import java.util.HashMap;

public class TransactionHistory {

	protected HashMap<String, Transaction> mTransactions = new HashMap<String, Transaction>();
	protected String mLogTimeFormat = "";

	public TransactionHistory() {

	}

	public void AppendTransaction(String transID, String line, HashMap strings) {
		String transIDToUse = transID;

		// check if there was a global transaction ID defined
		String globalTID = (String) strings.get("gtid");

		// if we have a global tid, replace our usage
		if (globalTID != null && globalTID.length() > 0)
			transIDToUse = globalTID;

		Transaction transx = GetTransaction(transIDToUse);
		if (transx == null)
			transx = AddTransaction(transIDToUse);

		transx.addLogMessage(line, strings);
	}

	public Transaction GetTransaction(String tid) {
		Transaction transx = (Transaction) mTransactions.get(tid);
		return transx;
	}

	private Transaction AddTransaction(String tid) {
		Transaction transx = new Transaction(tid);
		transx.setLogTimeFormat(mLogTimeFormat);
		mTransactions.put(tid, transx);
		return transx;
	}

	public HashMap getTransactionMap() {
		return mTransactions;
	}

	public void setLogFormat(String format) {
		mLogTimeFormat = format;
	}
}
