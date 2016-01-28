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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class Transaction {

	public enum LogLevel {
		DEBUG, NOTICE, INFO, WARNING, ERROR, CRITICAL
	}

	private String mTransID = "";
	protected String mStartTime = "";
	protected String mEndTime = "";
	protected String mTranslatedStartTime = "";
	protected String mTranslatedEndTime = "";
	protected String mLogType = "";
	protected LogLevel mLogLevel = LogLevel.NOTICE;
	protected String mLogLevelStr = "notice";
	protected String mLogTimeFormat = "";

	protected ArrayList<LogMessage> mMessages = new ArrayList<LogMessage>();
	protected StringBuffer mTransactionLogs = new StringBuffer();

	public Transaction(String transactionID) {
		mTransID = transactionID;
	}

	public void setLogTimeFormat(String format) {
		mLogTimeFormat = format;
	}

	public void setStartTime(String startTime) {
		mStartTime = startTime;
	}

	public void setTranslatedStartTime(String startTime) {
		mTranslatedStartTime = startTime;
	}

	public void setTranslatedEndTime(String endTime) {
		mTranslatedEndTime = endTime;
	}

	public void setLogLevel(String loglevel) {
		LogLevel newLevel = mLogLevel; // notice will drop to the bottom don't
										// bother checking it
		if (loglevel.equals("debug") && mLogLevel != LogLevel.WARNING
				&& mLogLevel != LogLevel.ERROR
				&& mLogLevel != LogLevel.CRITICAL)
			newLevel = LogLevel.DEBUG;
		if (loglevel.equals("info") && mLogLevel != LogLevel.WARNING
				&& mLogLevel != LogLevel.ERROR
				&& mLogLevel != LogLevel.CRITICAL)
			newLevel = LogLevel.INFO;
		else if (loglevel.equals("warn") && mLogLevel != LogLevel.ERROR
				&& mLogLevel != LogLevel.CRITICAL)
			newLevel = LogLevel.WARNING;
		else if (loglevel.equals("error") && mLogLevel != LogLevel.CRITICAL)
			newLevel = LogLevel.ERROR;
		else if (loglevel.equals("critic"))
			newLevel = LogLevel.CRITICAL;

		if (mLogLevel != newLevel) {
			mLogLevelStr = loglevel;
			mLogLevel = newLevel;
		}
	}

	protected String tryTimePattern(String pattern, String value,
			String resultPattern) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			Date date = sdf.parse(value);
			sdf.setTimeZone(TimeZone.getTimeZone(mLogTimeFormat));
			sdf.applyPattern(resultPattern + " '" + mLogTimeFormat + "'");

			return sdf.format(date).toString();
		} catch (Exception ex) {

		}

		return value;
	}

	public String translateTimeZone(String dateinfo) {
		if (dateinfo.length() < 1 || mLogTimeFormat.length() < 1
				|| mLogTimeFormat.equals(" ")
				|| (mLogTimeFormat.equals("Z") && dateinfo.endsWith("Z")))
			return dateinfo;

		String outTime = tryTimePattern("yyyyMMdd'T'HHmmss'Z'", dateinfo,
				"dd MMM yyyy HH:mm:ss");
		if (outTime.equals(dateinfo))
			outTime = tryTimePattern("yyyyMMdd'T'HHmmss.SSS'Z'", dateinfo,
					"dd MMM yyyy HH:mm:ss.SSS");
		return outTime;
	}

	public LogMessage addLogMessage(String line, HashMap strings) {
		LogMessage logMsg = new LogMessage(strings);

		String timestamp = (String) strings.get("timestamp");

		String translatedTime = translateTimeZone(timestamp);
		logMsg.mLogMsg.put("translatedtimestamp", translatedTime);
		if (timestamp != null && timestamp.length() > 1) {
			if (mStartTime.length() < 1
					|| (timestamp.endsWith("Z") && mStartTime
							.compareTo(timestamp) > 0)) {
				mStartTime = timestamp;

				mTranslatedStartTime = translatedTime;
			}
			if (mEndTime.length() < 1
					|| (mEndTime.endsWith("Z") && timestamp.compareTo(mEndTime) > 0)) {
				mEndTime = timestamp;

				mTranslatedEndTime = translatedTime;
			}

			if (translatedTime.length() > 1
					&& !translatedTime.equals(timestamp)) {
				logMsg.mLogMsg.remove("message");
				logMsg.mLogMsg.put("message",
						line.replace(timestamp, translatedTime));
			}
		}

		String loglevel = (String) strings.get("level");
		if (loglevel != null)
			setLogLevel(loglevel);

		if (mLogType.length() < 1) {
			String logtype = (String) strings.get("type");
			if (logtype != null)
				mLogType = logtype;
		}

		mMessages.add(logMsg);

		return logMsg;
	}

	public void OrderTransactions() {
		mTransactionLogs = new StringBuffer();

		// order it by date
		Collections.sort(mMessages, new LogMessageSort());
		for (int i = 0; i < mMessages.size(); i++) {
			LogMessage msg = mMessages.get(i);
			String line = (String) msg.mLogMsg.get("message");
			if (line == null)
				continue;

			mTransactionLogs
					.append(line + System.getProperty("line.separator"));
		}
	}

	public String getTransID() {
		return mTransID;
	}

	public String getStartTime() {
		return mStartTime;
	}

	public String getTranslatedStartTime() {
		return mTranslatedStartTime;
	}

	public String getTranslatedEndTime() {
		return mTranslatedEndTime;
	}

	public String getEndTime() {
		return mEndTime;
	}

	public String getLogType() {
		return mLogType;
	}

	public String getLogLevel() {
		return mLogLevelStr;
	}

	public StringBuffer getMessageBuffer() {
		return mTransactionLogs;
	}
}
