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
