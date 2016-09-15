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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import com.ibm.datapower.er.ERFramework;
import com.ibm.datapower.er.ErrorReportDetails;
import com.ibm.datapower.er.IPartInfo;
import com.ibm.datapower.er.PartsProcessorsHTML;
import com.ibm.datapower.er.ReportProcessorPartInfo;
import com.ibm.datapower.er.Analytics.ConditionField.REG_GROUP_TYPE;
import com.ibm.datapower.er.Analytics.Structure.DSCacheEntry;
import com.ibm.datapower.er.Analytics.Structure.Expression;
import com.ibm.datapower.er.Analytics.Structure.Formula;
import com.ibm.datapower.er.Analytics.Structure.ItemObject;
import com.ibm.datapower.er.Analytics.Structure.RunFormula;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class AnalyticsProcessor {
	public enum PRINT_MET_CONDITIONS {
		HIDEALL, HIDEDEFAULT, SHOWALL
	}

	public enum REPORT_TYPE {
		UNKNOWN, DATAPOWER_REPORT, POSTMORTEM, // Todo: We need some sort of
												// break down, but I think most
												// post mortems come from Cast
												// Iron whether its XC10/API
												// Mgmt
	}

	public AnalyticsProcessor() {
		BasicConfigurator.configure();
		Logger logger = Logger.getRootLogger();
		logger.setLevel(Level.INFO);
		// this fixes invalid argument issues when there is only one processor
		// available
		int procs = Runtime.getRuntime().availableProcessors() / 2;
		if (procs < 1)
			procs = 1;

		eService = Executors.newFixedThreadPool(procs);
	}

	/**
	 * Prepares Analytics as an InputStream for processing
	 * 
	 * @param String
	 *            filename - file name that contains Analytics rules (xml)
	 * @param ERFramework
	 *            er - base class for handling error report functionality
	 * @param printResults
	 *            boolean - whether or not we should print results to screen
	 * @param formatType
	 *            String - when printing results this is the format type, text
	 *            is default, "html" optional
	 * @throws IOException
	 * @throws SAXException
	 */
	public ArrayList<ConditionsNode> loadAndParse(String filename,
			ERFramework er, boolean printResults, String formatType,
			String outFile, String printConditions, String logLevel)
			throws IOException, SAXException {

		// start by setting log level to debug if its passed to loadAndParse
		// our default log level is INFO (in the AnalyticsProcessor constructor)
		Logger logger = Logger.getRootLogger();
		String logLvlSetting = logLevel.toLowerCase();

		switch (logLvlSetting) {
		case "debug":
			logger.setLevel(Level.DEBUG);
			break;
		case "none":
			logger.setLevel(Level.OFF);
			break;
		}

		// if the filename didn't include .xml then we must not have a filename
		// just a directory

		Logger.getRootLogger().info(
				"AnalyticsProcessor::loadAndParse filename " + filename);
		Logger.getRootLogger().info(
				"AnalyticsProcessor::loadAndParse outFile " + outFile);
		Logger.getRootLogger().info(
				"AnalyticsProcessor::loadAndParse printResults: "
						+ Boolean.toString(printResults) + ", formatType: "
						+ formatType + ", printConditions: " + printConditions
						+ ", logLevel: " + logLevel);

		if (!filename.endsWith(".xml")
				|| filename.toLowerCase().equals("autodetect")) {
			mReportType = detectReportType(er.getFileLocation());
			filename = getReportRulesFile(mReportType, filename);
			Logger.getRootLogger().info(
					"AnalyticsProcessor::loadAndParse autodetect ReportType: "
							+ mReportType);
			Logger.getRootLogger().info(
					"AnalyticsProcessor::loadAndParse autodetect filename: "
							+ filename);
		}

		Logger.getRootLogger().info(
				"AnalyticsProcessor::loadAndParse instantiate FileInputStream");
		mAnalytics = new FileInputStream(filename);
		mFramework = er;
		mCacheList = new ArrayList<XPathCache>();
		mCurRegCache = new ArrayList<RegEXPCache>();
		mFormatType = formatType.toLowerCase();
		outputFileName = outFile;

		String printCond = printConditions.toLowerCase();
		if (printCond.equals("hideall"))
			mprintConditionsSetting = PRINT_MET_CONDITIONS.HIDEALL;
		else if (printCond.equals("showall"))
			mprintConditionsSetting = PRINT_MET_CONDITIONS.SHOWALL;

		Logger.getRootLogger().info(
				"AnalyticsProcessor::loadAndParse parse starting.");

		return parse(printResults, outFile);
	}

	/**
	 * Based on the input file, the tool determines what rules file to use
	 * 
	 * @param REPORT_TYPE
	 *            inType - determine report type (datapower er, post-mortem,
	 *            etc.)
	 * @param String
	 *            filename - the filepath/dir passed in by the user
	 */
	public static String getReportRulesFile(REPORT_TYPE inType, String filename) {
		String file = "Analytics.xml";
		if (inType == REPORT_TYPE.POSTMORTEM)
			file = "postmortem.xml";
		String current = "";

		if (filename.toLowerCase().equals("autodetect")) {
			try {
				current = new java.io.File(".").getCanonicalPath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (current.startsWith("/"))
				return current + "/" + file;
			else
				return current + "\\" + file;
		} else if (filename.endsWith("\\") || filename.endsWith("/"))
			return filename + file;
		else if (filename.indexOf("/") == 0)
			return filename + "/" + file;
		else
			return filename + "\\" + file;
	}

	/**
	 * Determines the report type of the input file
	 * 
	 * @param String
	 *            filename - the input filename provided by user
	 */
	public static REPORT_TYPE detectReportType(String fileName) {
		REPORT_TYPE outType = REPORT_TYPE.UNKNOWN;
		if (fileName.endsWith(".zip")) {
			ZipInputStream inputStream = null;
			try {
				inputStream = new ZipInputStream(new FileInputStream(fileName));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (inputStream != null) {
				// find out what type of report if its a zip
				ZipEntry entry;
				try {
					entry = inputStream.getNextEntry();
					// we only care about the first entry really, we can't
					// handle multiple docs less its a post mortem .tar.gz
					if (entry != null) {
						if (entry.getName().endsWith(".tar.gz")
								|| entry.getName().startsWith("var/")
								|| entry.getName().startsWith("usr/"))
							outType = REPORT_TYPE.POSTMORTEM;
						else
							outType = REPORT_TYPE.DATAPOWER_REPORT;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} // end of ".zip" block
		else if (fileName.endsWith(".tar.gz"))
			outType = REPORT_TYPE.POSTMORTEM;
		else
			outType = REPORT_TYPE.DATAPOWER_REPORT;

		return outType;
	}

	/* parseRegExpResult handles the various logic for Analytics Conditions */
	private void parseRegExpResult(RunFormula formula, ConditionField field,
			RegEXPCache cache, ConditionsNode node, String value,
			String conditionalValue, String conditionValue, int fieldPos,
			int curGroupPos, int modPos, ConditionsNode regAllCloneNode,
			boolean prevConditionAnd) {

		// first IF statement is for handling matchOrderedGroup (Ordered)
		// condition
		// Eg.
		// <RegExp>((xml-manager \"([a-zA-Z-_.0-9]{0,40})\"|schedule-rule
		// \"([a-zA-Z-_.0-9]{0,40})\"\s\"([0-9]{0,10})\")|exit)</RegExp>
		// <Condition FieldPosition="3" RegGroup="Ordered"
		// ConditionName="ObjectName" Operation="NotEqualTo" Value=""
		// NextOperation="And"/>
		// <Condition FieldPosition="4" RegGroup="Ordered"
		// ConditionName="ConfigInfo" Operation="NotEqualTo" Value=""
		// NextOperation="And"/>
		// <Condition FieldPosition="5" RegGroup="Ordered"
		// ConditionName="TimeSeconds" Operation="NotEqualTo" Value=""/>

		if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_ORDERED
				&& value != null) {
			if (regAllCloneNode != null) {
				node = AnalyticsFunctions.cloneNode(regAllCloneNode, formula);
			} else
				node = AnalyticsFunctions.instantiateNode(formula);

			if (field.isParsedFieldValue()) {
				synchronized (ERFramework.mDocBuilderFactory) {
					value = getNodeValue(formula.documentSet.GetDocument(),
							field.getParsedFieldValue(), curGroupPos, node);
				}
			}

			if (conditionValue.startsWith("{")) {
				synchronized (ERFramework.mDocBuilderFactory) {
					conditionalValue = getNodeValue(
							formula.documentSet.GetDocument(),
							conditionValue.substring(1,
									conditionValue.length() - 1), curGroupPos,
							node);
				}
			}

			boolean condMatched = parseConditionValue(formula, field, value,
					conditionalValue, node, curGroupPos, modPos);

			ArrayList<ConditionsNode> tmpList = new ArrayList<ConditionsNode>();

			if (condMatched) {
				String ordValue = null;
				boolean continueMatch = true;
				ConditionsNode tmpNode = null;
				while (continueMatch && cache.getMatcher().find()) {
					for (int ordCount = fieldPos + 1; ordCount < formula.cFields
							.size(); ordCount++) {

						if (ordCount < 2)
							tmpNode = null;

						ConditionField ordField = formula.cFields.get(ordCount);

						boolean ordCondOperAnd = false;
						if (ordField.getConditionNextOperation().toLowerCase()
								.equals("and"))
							ordCondOperAnd = true;
						try {
							ordValue = cache.getMatcher().group(
									ordField.getFieldPosition());
						} catch (Exception ex) {
							ex.printStackTrace();
							continueMatch = false;
							break;
						}

						if (ordValue == null) {
							continueMatch = false;
							break;
						}

						if (tmpNode == null) {
							try {
								tmpNode = (ConditionsNode) node.clone();

								formula.condNodes.add(formula.condNodes.size(),
										tmpNode);

								tmpList.add(tmpNode);
							} catch (CloneNotSupportedException e) {
								// TODO
								// Auto-generated
								// catch block
								e.printStackTrace();
							}
						}

						if (field.isParsedFieldValue()) {

							synchronized (ERFramework.mDocBuilderFactory) {
								value = getNodeValue(
										formula.documentSet.GetDocument(),
										field.getParsedFieldValue(),
										curGroupPos, node);
							}
						}

						boolean operMatched = parseConditionValue(formula,
								ordField, ordValue, ordField.getValue(),
								tmpNode, curGroupPos, modPos);

						if (!operMatched
								&& (formula.nextExpressionAnd
										|| prevConditionAnd || ordCondOperAnd)) {
							tmpNode.setExpressionsFailed(true);
						}

						tmpNode.setConditionFound(true);
						tmpNode.setConditionsMet(node.getConditionsMet() + 1);
					} // while loop
				} // for loop

				for (int t = 0; t < tmpList.size(); t++) {
					ConditionsNode tmp = (ConditionsNode) tmpList.get(t);
					if (!tmp.isExpressionsFailed())
						tmp.setExpressionsMet(tmp.getExpressionsMet() + 1);
				}
			} // cond matched

		}

		// END OF matchOrderedGroup (Ordered) group

		// take a hex string (without 0x) and
		// convert to a long
		// Eg. transaction id is hexadecimal, take the value (minus 0x) - we can
		// translate the hexadecimal value into a long
		// <Condition FieldPosition="1" RegGroup="All" Conversion="hex2long"
		// ConditionName="TransID" Operation="NotEqualTo" Value="0"
		// NextOperation="And">xactid 0x([a-zA-Z-_.0-9]+)</Condition>
		if (field.getConversionType().equals("hex2long")) {
			try {
				long res = Long.parseLong(value, 16);
				value = Long.toString(res);
			} catch (Exception ex) {
			}
		}

		// BEGIN matchAllRegGroup (RegGroup="All")
		// if the condition is a RegGroup of
		// 'All' it means we iterate through
		// as if these were unique
		// conditionsnodes. This expression uses
		// regular expressions
		// instead of xpath to separate the
		// information for matching conditions
		if (field.getRegGroupType().getType() < REG_GROUP_TYPE.MATCH_COUNT
				.getType()) {
			boolean operMatched = false;
			if (curGroupPos > 1) {
				ConditionsNode tmpNode = null;
				if ((curGroupPos - 1) < formula.condNodes.size())
					tmpNode = formula.condNodes.get(curGroupPos - 1);

				if (tmpNode == null) {
					boolean overrideNode = false;
					// we use the clone node if
					// we got just a single node
					// result last time
					// and match all group is
					// being used
					if (field.getRegGroupType().getType() < REG_GROUP_TYPE.MATCH_COUNT
							.getType() && regAllCloneNode != null) {
						try {
							node = (ConditionsNode) regAllCloneNode.clone();
							overrideNode = true;
						} catch (CloneNotSupportedException e) {
							// TODO
							// Auto-generated
							// catch block
							e.printStackTrace();
						}
					}

					// set to true if we cloned
					// a single node result from
					// a previous expression
					if (!overrideNode) {
						node = AnalyticsFunctions.instantiateNode(formula);
					}

					formula.condNodes.add(formula.condNodes.size(), node);
				} else
					node = tmpNode;
			}

			if (field.isParsedFieldValue()) {

				synchronized (ERFramework.mDocBuilderFactory) {
					value = getNodeValue(formula.documentSet.GetDocument(),
							field.getParsedFieldValue(), curGroupPos, node);
				}
			}

			if (conditionValue.startsWith("{")) {

				synchronized (ERFramework.mDocBuilderFactory) {
					conditionalValue = getNodeValue(
							formula.documentSet.GetDocument(),
							conditionValue.substring(1,
									conditionValue.length() - 1), curGroupPos,
							node);
				}
			}

			operMatched = parseConditionValue(formula, field, value,
					conditionalValue, node, curGroupPos, modPos);

			if (!operMatched
					&& (formula.nextExpressionAnd || prevConditionAnd || field
							.getConditionOperAnd())) {
				node.setExpressionsFailed(true);
			}
		}
		// END RegGroup="All"

	}

	/*
	 * parseFieldCondition iterates to create each node for the Condition we are
	 * currently testing
	 */

	private void parseFieldCondition(RunFormula formula, ConditionField field,
			ConditionsNode cloneNode, ConditionsNode regAllCloneNode,
			NodeList resultList, int fieldPos, boolean prevConditionAnd,
			int regGroupValue, int totalResults) {
		int regGroupPos = regGroupValue;

		String conditionValue = field.getValue();

		// used as the value position parsed from FieldPosition
		int pos = field.getFieldPosition();

		// This is the regular expression we use to break apart the
		// value in the error report (from xPath)
		String conditionRegEXP = field.getConditionRegEXP();

		ConditionsNode node = null;

		double sumCondition = 0.0;

		for (int curPos = 0; curPos < totalResults; curPos++) {

			node = AnalyticsFunctions.determineNode(formula, cloneNode, curPos,
					fieldPos);

			if (node == null || node.isReqExpressionFailed()) // we failed a
				// previous
				// expression so
				// skip it
				continue;

			// if the previous condition was "or" we can reset and try
			// the expression again with the next set of conditions
			if (!prevConditionAnd)
				node.setExpressionsFailed(false);
			else {
				// if a previous Condition in this expression is required
				// (AND Operand) -- if it was never met, we should skip
				// over attempting this next condition to save time
				if (node.getConditionsMet() < 1) {
					Logger.getRootLogger().debug(
							"AnalyticsProcessor::parseFieldCondition formula : "
									+ formula.getFormula().getIdentifier()
									+ " -- document: "
									+ formula.documentSet
											.GetOriginalSectionName()
									+ ", no previous conditions met");
					continue;
				}
			}

			// if we have matched expressions necessary to pass this
			// entry for the formula skip
			// if we missed a match and it was required skip as well
			if (node.isExpressionsMet())
				continue;
			// this stops checking additional nodes if we already failed
			// an 'AND' condition
			else if (field.getRegGroupType().getType() > REG_GROUP_TYPE.MATCH_ALL_RESULT
					.getType() && node.isExpressionsFailed())
				continue;
			else if (node.isConditionFound())
				continue;

			// modPos gets set if the condition we are trying to match
			// only has one element result
			// and the previous conditions were multiple element matches
			int modPos = curPos;
			if (formula.condNodes.size() > resultList.getLength()
					&& resultList.getLength() == 1)
				modPos = 0;

			// get the current element from the xml section of the error
			// report
			Node resultNode = resultList.item(modPos);

			String value = null;

			// determine what the string value of the element is for
			// comparison

			if (resultNode != null)
				value = resultNode.getNodeValue();

			// no good if we don't have a value to compare to
			if (value == null) {
				// find the inner text since it must not be an attribute
				// that was matched
				if (resultNode != null)
					value = resultNode.getTextContent();

				// nothing to compare to continue to the next node
				if (value == null)
					continue;
			}

			if (formula.idxSearch.length() > 0
					&& value.indexOf(formula.idxSearch) < 0)
				continue;

			// we do not want to override the base value because it may
			// be xpath and we need to parse a new position
			String conditionalValue = conditionValue;

			// if the condition we provide in analytics is an xpath
			// query
			// we find out its actual value and pass it
			if (conditionValue.startsWith("{")) {

				synchronized (ERFramework.mDocBuilderFactory) {
					conditionalValue = getNodeValue(
							formula.documentSet.GetDocument(),
							conditionValue.substring(1,
									conditionValue.length() - 1), curPos, node);
				}
			}

			int curGroupPos = 0;
			// the current condition might require regular expressions
			// to break down the value from the error report
			// enter this if loop if the regular expression field is
			// something other than '*'. '*' will not parse.
			if (field.getFieldValue() != null
					&& (formula.regExp.length() > 0
							&& !formula.regExp.equals("*") || (conditionRegEXP
							.length() > 0))) {

				String regEXPUse = formula.regExp;

				// if we have a condition passed where regular
				// expression needs to be done
				if (conditionRegEXP.length() > 0
						&& !conditionRegEXP.equals("*")) {
					regEXPUse = conditionRegEXP;
				}

				if (!conditionRegEXP.equals("*")) // this means unless
													// regexp is defined
													// at the condition
													// level, and it
													// isn't *, we
													// compile the
													// pattern
				{
					String curRegEXP = parseStringWithConditions(
							formula.documentSet, regEXPUse, modPos, node);

					Logger.getRootLogger()
							.debug("AnalyticsProcessor::parseFieldCondition formula : "
									+ formula.getFormula().getIdentifier()
									+ " -- Pattern: "
									+ regEXPUse
									+ ", modPos: " + modPos + ", node: " + node);

					RegEXPCache cache = getRegExpCache(formula, curRegEXP,
							value);

					if (cache == null) // we got no data from the reg exp cache
					{
						Logger.getRootLogger().debug(
								"AnalyticsProcessor::parseFieldCondition formula : "
										+ formula.getFormula().getIdentifier()
										+ " -- Pattern: " + regEXPUse
										+ " -- no cache data found");

						/*
						 * we need to make sure the match all is reviewing all
						 * nodes obtained from the previous result set* not
						 * doing this will cause some node results to be skipped
						 */
						if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_ALL_RESULT
								&& formula.condNodes.size() > totalResults)
							totalResults = formula.condNodes.size();
						continue;
					}

					// if it is less than 0 the user did not set it or
					// it is 'all' in either case we iterate through all
					// available groups
					if (regGroupPos < 0 && cache.getMatcher() != null)
						regGroupPos = cache.getMatcher().groupCount();
					else
						regGroupPos += 1; // increment one so we
											// actually go into the
											// while loop

					Logger.getRootLogger().debug(
							"AnalyticsProcessor::parseFieldCondition formula : "
									+ formula.getFormula().getIdentifier()
									+ " -- Pattern: " + regEXPUse
									+ " -- attempt getMatcher().find");

					// determine if the field exists and grab it
					if (cache.getMatcher().groupCount() > 0
							&& curGroupPos < regGroupPos) {
						try {
							while (cache.getMatcher().find()
							// count, all result, reg all group, ordered all
							// apply
									&& (field.getRegGroupType().getType() < REG_GROUP_TYPE.MATCH_NONE
											.getType())
									|| (field.getRegGroupType().getType() > REG_GROUP_TYPE.MATCH_ALL_RESULT
											.getType() && curGroupPos < regGroupPos)) {

								curGroupPos++;

								if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_COUNT)
									continue;

								try {
									value = cache.getMatcher().group(pos);

									Logger.getRootLogger().debug(
											"AnalyticsProcessor::parseFieldCondition formula : "
													+ formula.getFormula()
															.getIdentifier()
													+ " -- Pattern: "
													+ regEXPUse
													+ ", value matched: "
													+ value + ", position: "
													+ pos + ", curGroupPos: "
													+ curGroupPos
													+ ", current field: "
													+ field);

									if (!formula.documentSet.IsXMLSection()
											&& field.getRegGroupType() == REG_GROUP_TYPE.MATCH_SUM) {
										sumCondition += Double
												.parseDouble(value);
									}
								} catch (Exception ex) {
									continue;
								}

								if (!formula.documentSet.IsXMLSection()
										&& field.getRegGroupType() == REG_GROUP_TYPE.MATCH_SUM)
									continue;

								parseRegExpResult(formula, field, cache, node,
										value, conditionalValue,
										conditionValue, fieldPos, curGroupPos,
										modPos, regAllCloneNode,
										prevConditionAnd);
							}
						} catch (StackOverflowError ex) {
							ex.printStackTrace();
						}
					}

					Logger.getRootLogger().debug(
							"AnalyticsProcessor::parseFieldCondition formula : "
									+ formula.getFormula().getIdentifier()
									+ " -- Pattern: " + regEXPUse
									+ " -- completed getMatcher().find");

				}
			}

			if (formula.documentSet.IsXMLSection()
					&& field.getRegGroupType() == REG_GROUP_TYPE.MATCH_SUM) {
				try {
					sumCondition += Double.parseDouble(value);
				} catch (Exception ex) {

				}

				// we need to loop through to get the sum before we send the
				// result
				if (curPos < (totalResults - 1))
					continue;
			}

			// need to handle regexp break-up of the value here

			boolean operationMatched = false;

			// the reggroup was set to 'Count' which means we use the
			// value of how many entries exist (count++ of ent)
			switch (field.getRegGroupType()) {
			case MATCH_COUNT:
				value = Integer.toString(curGroupPos);
				break;
			case MATCH_SUM:
				value = Double.toString(sumCondition);
				break;
			}

			if (field.getRegGroupType().getType() > REG_GROUP_TYPE.MATCH_ALL_RESULT
					.getType()) {
				// since we did not iterate into the loop to set the
				// value we need to check here again to convert
				// from hex to long
				// take a hex string (without 0x) and convert to a long
				if (field.getConversionType().equals("hex2long")) {
					try {
						long res = Long.parseLong(value, 16);
						value = Long.toString(res);
					} catch (Exception ex) {
					}
				}

				// determine if xml section
				if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_COUNT
						&& formula.documentSet.IsXMLSection())
					value = Integer.toString(totalResults);

				if (field.isParsedFieldValue()) {

					synchronized (ERFramework.mDocBuilderFactory) {
						value = getNodeValue(formula.documentSet.GetDocument(),
								field.getParsedFieldValue(), curPos, node);
					}
				}

				operationMatched = parseConditionValue(formula, field, value,
						conditionalValue, node, curPos, modPos);

				if (!operationMatched
						&& (formula.nextExpressionAnd || prevConditionAnd || field
								.getConditionOperAnd())) {
					node.setExpressionsFailed(true);
					continue;
				}
			}

			if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_ALL_RESULT
					&& formula.condNodes.size() > totalResults)
				totalResults = formula.condNodes.size();
		}
	}

	/**
	 * handleRunFormula() handles looping for each condition field in the
	 * formula
	 */
	public boolean handleRunFormula(RunFormula formula) {
		DocumentSection section = formula.documentSet;
		ArrayList<ConditionsNode> conditionsMetList = formula.condNodes;
		String xPathQuery = formula.xPathQuery;

		// instantiate our xpath
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList resultList = null;
		/*
		 * attempt to run xpath, if it fails just ignore and check if the
		 * resultList is still null* we do not care if we can't match the xpath
		 * it just means it did not exist in the doc
		 */

		try {
			XPathExpression expr = (XPathExpression) xpath.compile(xPathQuery);

			synchronized (ERFramework.mDocBuilderFactory) {
				resultList = (NodeList) expr.evaluate(section.GetDocument(),
						XPathConstants.NODESET);
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		if (resultList == null) // we are done we have nothing to parse
			return true;

		// amount of conditions that must be met for any results
		int conditionCountRequirement = 0;

		boolean prevConditionAnd = false;

		// determining if the current resultList xpath has less conditional
		// elements to match, later we will determine through modPos
		// the correct position of the conditional
		// but only if the resultList has a length of 1 (eg VersionInfo or
		// other single element nodesets)
		int totalResults = resultList.getLength();

		if (conditionsMetList.size() > totalResults)
			totalResults = conditionsMetList.size();

		// this node will be used if the previous expression resultList
		// iterated had only one item and a following expression
		// resultList returns more than one, we will pass the conditions
		// previously passed by cloning this ConditionsNode
		ConditionsNode cloneNode = null;

		// we use this alternatively if we are using regexp instead of
		// xpath, there might be multiple results
		ConditionsNode regAllCloneNode = null;

		// Determine if the previous resultList was only one item and the
		// next expression has more than one result
		// persist the conditions passed in the previous iteration to all in
		// the current
		try {
			// we only do this if there was one previous result
			if (conditionsMetList.size() == 1) {
				// if this fails no need to continue
				regAllCloneNode = (ConditionsNode) conditionsMetList.get(0)
						.clone();

				// we have to meet these conditions to use in xpath (have 1
				// result previously and more than one now)
				if (totalResults > 1) {
					cloneNode = (ConditionsNode) conditionsMetList.get(0)
							.clone();
				}
			}
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		boolean matchOrderedGroup = false;

		for (int fieldPos = 0; fieldPos < formula.cFields.size(); fieldPos++) {

			if (matchOrderedGroup)
				break;

			// used to determine the group number for the position parsed
			// from the regular expression
			int regGroupPos = -1;

			ConditionField field = (ConditionField) formula.cFields
					.get(fieldPos);

			// if the current condition has a regular expression
			// it can break itself down to match nodes by iteration
			// (0..1..2..3)
			// regGroupValue of 'All' means we use existing nodes or
			// instantiate them as if it was an iterative list from xpath
			// otherwise if the regGroup has a numerical value we get that
			// group and return the fieldposition value from that group
			// iteration
			String regGroupValue = field.getRegGroup();

			// if this is an ordered group we break out of the first field
			// result
			if (field.getRegGroupType() == REG_GROUP_TYPE.MATCH_ORDERED)
				matchOrderedGroup = true;

			try {
				regGroupPos = Integer.parseInt(regGroupValue);
			} catch (Exception ex) {
				regGroupPos = -1;
			}

			// !!!parse document!!!

			parseFieldCondition(formula, field, cloneNode, regAllCloneNode,
					resultList, fieldPos, prevConditionAnd, regGroupPos,
					totalResults);

			// we checked a condition, add it to the requirement count
			conditionCountRequirement += 1;

			// we must match the next condition, just keep going
			if (field.getConditionOperAnd()) {
				prevConditionAnd = true;
			} else {
				// this is an 'or' condition, or it is the end condition.
				// See if we matched up.
				prevConditionAnd = false;

				// use if the next expression is required and break out of
				// checking further conditions
				// if none pass this expression
				boolean noNodeMatchedExpression = true;

				// next expression is an 'or' so we don't have to match
				if (!formula.nextExpressionAnd)
					noNodeMatchedExpression = false;

				// no more conditions to worry about see if we matched up
				for (int p = 0; p < conditionsMetList.size(); p++) {
					ConditionsNode curNode = conditionsMetList.get(p);

					// we already handled this condition before
					if (curNode.isConditionFound()) {
						noNodeMatchedExpression = false;
						continue;
					}

					if (curNode.getConditionsMet() >= conditionCountRequirement) {
						noNodeMatchedExpression = false;
						curNode.setConditionFound(true);
						curNode.setExpressionsMet(curNode.getExpressionsMet() + 1);

						// determine if xml section
						if ((field.getRegGroupType() == REG_GROUP_TYPE.MATCH_COUNT || field
								.getRegGroupType() == REG_GROUP_TYPE.MATCH_SUM)
								&& section.IsXMLSection())
							break;

					} 
					// if we have additional Conditions to test then we continue ('OR' Condition statement handling)
					else if (fieldPos+1 == formula.cFields.size() && formula.nextExpressionAnd
							&& curNode.isExpressionsFailed()) {
						// otherwise we have reached the end of our testable conditions, we failed to match
						curNode.setReqExpressionFailed(true);
					}
				} // end for loop conditionsMetList

				// since we were in an 'or' statement we no longer have to
				// make that condition
				conditionCountRequirement--;

				// we didn't match anything and the expression was required
				// to pass the formula
				if (noNodeMatchedExpression)
					return false;

			} // end else statement (non-and situations, "or" condition, or
				// end condition)
		} // end for loop conditionNodes

		return true;
	}

	/**
	 * parseConditionValue() handles parsing the value for the ConditionsNode to
	 * check if the current condition is met, return false if the condition
	 * cannot be met
	 * 
	 * @param conditionalValue
	 *            String - This is the value we need to match and is provided in
	 *            the Analytics xml attribute 'Value'
	 * @param value
	 *            String - This is the value to match up with the current
	 *            ConditionsNode (this is the comparison to conditionalValue
	 *            from the error report)
	 * @param node
	 *            ConditionsNode - this is the node that tracks all the
	 *            conditions we met in this expression
	 * @param curGroupPos
	 *            int - this is the iteration position (c) from parseConditions,
	 *            the ConditionsNode position on the ArrayList
	 * @param modPos
	 *            int - this is the alternate position for when we have offset
	 *            results between expressions
	 * @return Boolean, true if succeed, false if conditions are not met
	 */
	private boolean parseConditionValue(RunFormula formula,
			ConditionField field, String value, String conditionalValue,
			ConditionsNode node, int curGroupPos, int modPos) {
		if (value == null)
			return false;

		boolean operationMatched = false;
		// check which operation we need to perform, bring it to lower case for
		// comparison
		String conditionOperLwr = field.getOperation().toLowerCase();
		switch (conditionOperLwr) {
		case "equalsto": {

			if (value.equals(conditionalValue)) {
				operationMatched = true;
			}
			break;
		}
		case "contains": {
			if (value.contains(conditionalValue)) {
				operationMatched = true;
			}
			break;
		}
		case "notcontain": {
			if (!value.contains(conditionalValue)) {
				operationMatched = true;
			}
			break;
		}
		case "notequalto": {
			if (!value.equals(conditionalValue)) {
				operationMatched = true;
			}
			break;
		}
		case "lessthan": {

			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				if (reportVal < val) {
					operationMatched = true;
				}
			} catch (Exception ex) {
			}
			break;
		}
		case "greaterthan": {

			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				if (reportVal > val) {
					operationMatched = true;
				}
			} catch (Exception ex) {
			}
			break;
		}
		// properties below are for creating new condition names
		case "append": {

			try {
				value = value + conditionalValue;
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		case "divideby": {
			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				double outVal = reportVal / val;
				value = Double.toString(outVal);
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		case "multiply": {
			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				double outVal = reportVal * val;
				value = Double.toString(outVal);
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		case "add": {
			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				double outVal = reportVal + val;
				value = Double.toString(outVal);
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		case "subtract": {
			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				double outVal = reportVal - val;
				value = Double.toString(outVal);
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		case "modulus": {
			try {
				double val = Double.parseDouble(conditionalValue);
				double reportVal = Double.parseDouble(value);
				double outVal = reportVal % val;
				value = Double.toString(outVal);
				operationMatched = true;
			} catch (Exception ex) {
			}
			break;
		}
		}

		// well we matched the operation now lets parse the header/body values
		// for user to display
		// the { } sections will have xpath that may apply to a field in the
		// cidDoc
		if (operationMatched) {
			node.setConditionsMet(node.getConditionsMet() + 1);
			node.addCondition(field.getConditionName().toLowerCase(), value);
			node.matchedConditions.add(formula.xPathQuery + "[" + curGroupPos
					+ "] '" + value + "' is " + field.getOperation() + " '"
					+ conditionalValue + "'");
			node.setDisplayName(parseMessage(formula.documentSet,
					node.getDisplayName(), modPos, value,
					field.getConditionName(), formula.bIsSectionVariable));
			node.setDisplayMessage(parseMessage(formula.documentSet,
					node.getDisplayMessage(), modPos, value,
					field.getConditionName(), formula.bIsSectionVariable));
			node.appendURI(field.getConditionName() + "=" + value);
		}

		return operationMatched;
	}

	private void handleMimeSection(Formula formula, Expression exp,
			ArrayList<ConditionsNode> formulaExpressionsMet) {

		String cidName = "", logLevelLwr = "", extension = "";
		int formulaPos = 0;
		boolean base64 = false, lineReturn = false;
		try {
			ItemObject obj = null;

			if ( (obj = exp.getItem("CIDSectionName")) != null )
				cidName = (String) obj.getObject();

			if ( (obj = exp.getItem("LogLevel")) != null )
				logLevelLwr = (String) obj.getObject();

			if ( (obj = formula.getItem("ID")) != null )
				formulaPos = (int) obj.getObject();
			// leave the base64 encoding for the output (internal tool
			// decodes through base64 on some only)
			if ( (obj = exp.getItem("SectionBase64")) != null )
				base64 = (boolean) obj.getObject();

			// used to determine if we should include line returns
			if ( (obj = exp.getItem("SectionLineReturn")) != null )
				lineReturn = (boolean) obj.getObject();

			if ( (obj = exp.getItem("Extension")) != null )
				extension = (String) obj.getObject();
		} catch (Exception ex) {
			// if we can't find a variable abort out
			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parseFormula formula : "
							+ formula.getIdentifier()
							+ " -- handleMimeSection failed pulling formula variables");
		}

		InputStream is;
		try {
			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parseFormula formula : "
							+ formula.getIdentifier()
							+ " -- handleMimeSection with out file: "
							+ outputFileName);
			is = mFramework.getCidAsInputStream(cidName, true);
			HashMap headers = new HashMap();
			headers.put("Content-ID", cidName);
			ErrorReportDetails details = new ErrorReportDetails();
			ReportProcessorPartInfo partInfo = new ReportProcessorPartInfo(
					IPartInfo.MIME_BODYPART, headers, is, details);
			File file = new File(outputFileName);
			File parentDir = file.getParentFile(); // get parent dir
			String dir = parentDir.getPath();
			if (dir.contains(":\\"))
				dir += "\\" + GENERATED_FILES_DIR + "\\";
			else
				dir += "/" + GENERATED_FILES_DIR + "/";

			String fileName = "";
			try {
				// add an extension to the output file
				String ext = "";
				if (extension != null)
					ext = extension;

				Logger.getRootLogger()
						.debug("AnalyticsProcessor::parseFormula formula : "
								+ formula.getIdentifier()
								+ " -- handleMimeSection parseFileName: cidName "
								+ cidName + ", Directory " + dir
								+ ",  Extension " + ext);

				String endFileName = AnalyticsFunctions.parseFileNameFromCid(
						cidName, dir, ext);
				File endFile = new File(dir + endFileName);
				if (!endFile.exists()) {
					fileName = PartsProcessorsHTML.binaryBody(partInfo, null,
							dir, base64, lineReturn);
					if (fileName.length() > 0) {
						File curFile = new File(dir + fileName);
						curFile.renameTo(endFile);
						fileName = endFileName;
					}
				} else
					fileName = endFileName;
			} catch (Exception e) {
				e.printStackTrace();
			}

			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parseFormula formula : "
							+ formula.getIdentifier()
							+ " -- handleMimeSection SetupNode: fileName "
							+ fileName);
			if (fileName.length() > 0) {
				ConditionsNode node = new ConditionsNode();

				node.addCondition("SectionName", cidName);

				AnalyticsFunctions.SetupNode(node, formula, (String) formula
						.getItem("DisplayMessage").getObject()
						+ ": file available <a href=\""
						+ GENERATED_FILES_DIR
						+ "/" + fileName + "\" >here</a>", formulaPos);
				AnalyticsFunctions.populatePassConditionNode(node, logLevelLwr,
						formula, formulaExpressionsMet, null);
				node.setExpressionsMet(true);
				formulaExpressionsMet.add(node);
			}
		} catch (Exception e1) {

		}

		// we only translate MIME sections into binary so we won't do
		// anything else, just continue to the next expression
	}

	/**
	 * Handles the base formula, pulling the expressions that need to be tested
	 * against the Section (cid), then testing the conditions that must be met
	 * for the expression to succeed
	 * 
	 * @param formulaExpressionsMet
	 *            ArrayList<ConditionsNode> - returned results where the formula
	 *            succeeds on an error report entry
	 * @param otherFormulasMatched
	 *            ArrayList<ConditionsNode> - used for comparison of already
	 *            matched formulas
	 * @return Boolean, true if succeed, false if parsing fails (no fail
	 *         conditions at this time)
	 */
	private boolean parseFormula(Formula formula,
			ArrayList<ConditionsNode> formulaExpressionsMet,
			ArrayList<ConditionsNode> otherFormulasMatched) {
		mCurRegCache.clear();
		mCacheList.clear();
		System.gc(); // this gets us to frequently flush memory, formula
		// processing is very memory intensive, this should help
		// keep us under the VM memory limits.

		/*
		 * need an ArrayList to be instantiated here which will track each node
		 * in the resultList from parseConditions, this ArrayList has to be
		 * updated for each expression to note if the node matches all
		 * conditions and passes (thus incrementing the expressions matched
		 * counter) once through expected expressions then check if the entry
		 * should be added it is a nested ArrayList because we can have more
		 * than one document section analyzed through the formula at the same
		 * time
		 */

		ArrayList<ArrayList<ConditionsNode>> conditionDocMetList = new ArrayList<ArrayList<ConditionsNode>>();

		// this is the count of expressions that must be matched
		int expressionsRequirementCount = 0;

		// variable used to determine if after an expression (required per
		// 'and') is checked and no nodes match we just break out of the formula
		boolean expressionsMatched = false;

		String lastExpGroupValue = "";

		NodeList expressionNodes = (NodeList) formula.getItem("Expression")
				.getObject();

		Logger.getRootLogger().debug(
				"AnalyticsProcessor::parseFormula formula : "
						+ formula.getIdentifier() + " -- has "
						+ expressionNodes.getLength()
						+ " Expression nodes for parsing.");

		for (int expNodeID = 0; expNodeID < expressionNodes.getLength(); expNodeID++) {
			Node expNode = expressionNodes.item(expNodeID);
			Element expElement = (Element) expNode;

			Expression exp = new Expression(expElement);

			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parseFormula formula : "
							+ formula.getIdentifier()
							+ " -- instantiated expression #" + expNodeID);

			// Used to determine the importance if this expression is matched
			String logLevelLwr = (String) exp.getItem("LogLevel").getObject();

			if (logLevelLwr != null)
				logLevelLwr = logLevelLwr.toLowerCase();

			// this is used to determine if the next expression in the formula
			// needs to be met with this one (AND)
			// or if otherwise either can be matched independently (OR) if a
			// second expression exists in the formula.
			boolean nextExpressionAnd = (boolean) exp.getItem(
					"NextOperationAnd").getObject();

			// Used to determine the importance if this expression is matched
			String formulaIDMatch = (String) exp.getItem("FormulaIDMatch")
					.getObject();

			if (formulaIDMatch.length() > 0) {
				// if we are trying to match a previous formula and cannot we
				// continue on
				if (!AnalyticsFunctions.isFormulaMatched(formulaIDMatch,
						formulaExpressionsMet)
						&& !AnalyticsFunctions.isFormulaMatched(formulaIDMatch,
								otherFormulasMatched)) {

					Logger.getRootLogger().debug(
							"AnalyticsProcessor::parseFormula formula : "
									+ formula.getIdentifier()
									+ " -- formulaIDMatch succeeded for "
									+ formulaIDMatch);

					if (nextExpressionAnd)
						expressionsRequirementCount += 1; // we are failing an
															// expression here,
															// increment
					continue;
				}

				Logger.getRootLogger().debug(
						"AnalyticsProcessor::parseFormula formula : "
								+ formula.getIdentifier()
								+ " -- formulaIDMatch failed for "
								+ formulaIDMatch);
			}

			// this will determine what grouping of expressions we are using in
			// this formula
			// when we see the expression group change from its previous value
			// then we restart the expressions/conditions matched
			// we require these to be in order (eg. expressiongroup1,
			// expressiongroup2) in the xml file
			String expExpressionGroup = (String) exp.getItem("ExpressionGroup")
					.getObject();

			// if we are resetting all the conditions / expressions met due to a
			// new expression group
			boolean flushingCounts = false;
			if (expExpressionGroup != null) {
				if (!lastExpGroupValue.equals(expExpressionGroup)) {
					flushingCounts = true;
					expressionsRequirementCount = 0;
				}

				lastExpGroupValue = expExpressionGroup;
			}

			// this is the CID Section in the document that we need to parse
			// from XML to determine
			// if the expression is a valid match

			String cidName = (String) exp.getItem("CIDSectionName").getObject();

			expressionsRequirementCount += 1;

			ArrayList<DocumentSection> documentSet = new ArrayList<DocumentSection>();
			boolean wildcardValue = (boolean) exp.getItem("SectionWildcard")
					.getObject();

			// used to determine if we want to retrieve a MIME section opposed
			// to a DocumentSection (with XML)
			if ((boolean) exp.getItem("SectionMIME").getObject()) {
				handleMimeSection(formula, exp, formulaExpressionsMet);
				Logger.getRootLogger().debug(
						"AnalyticsProcessor::parseFormula formula : "
								+ formula.getIdentifier()
								+ " -- handleMimeSection completed.");
				continue;
			}

			int cidSectionID = -1;
			// see if we can find the document and its xml, if not then ignore
			// the exceptions.
			do {
				cidSectionID++;
				cidName = AnalyticsFunctions.getAttributeByTag("Section",
						"Name", expElement, cidSectionID);
				String readNextSection = AnalyticsFunctions.getAttributeByTag(
						"Section", "ReadNextSection", expElement, cidSectionID);
				String extension = AnalyticsFunctions.getAttributeByTag(
						"Section", "Extension", expElement, cidSectionID);

				if (cidName.length() > 0)
					PullDocSection(cidName, documentSet, wildcardValue,
							extension);
				else
					break;

				if ((!readNextSection.toLowerCase().equals("true"))
						&& documentSet.size() > 0)
					break;
			} while (true);

			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parseFormula formula : "
							+ formula.getIdentifier()
							+ " -- document sets matched: "
							+ documentSet.size());

			List<Future> futureList = new ArrayList<Future>();

			for (int docID = 0; docID < documentSet.size(); docID++) {
				// get the current set of conditional nodes (in relation to the
				// xml document section we are loading)
				// if we had a wildcard to match multiple sections then we have
				// to track independently the conditions that are passing
				// for each section so that iterators do not collide
				ArrayList<ConditionsNode> condNodes = null;
				try {
					if (documentSet.size() == 1
							&& conditionDocMetList.size() > 1) {
						condNodes = new ArrayList<ConditionsNode>();
						for (int i = 0; i < conditionDocMetList.size(); i++)
							condNodes.addAll(conditionDocMetList.get(i));
					} else
						condNodes = conditionDocMetList.get(docID);

					// this means we had a new group of expressions we want to
					// test, so we reset everything that hasn't already matched
					if (flushingCounts) {
						// reset all the condition nodes expressions met for a
						// new round
						for (int c = 0; c < condNodes.size(); c++) {
							ConditionsNode node = condNodes.get(c);
							if (node.isExpressionsMet())
								continue;

							node.setExpressionsMet(0);
							node.setConditionsMet(0);
						}
					}
				} catch (Exception e) {
					// this exception will be thrown from time to time, ignore
				} finally {
					if (condNodes == null) {
						condNodes = new ArrayList<ConditionsNode>();

						// Sep 17 2014 - RegExp (RegGroup None) - RegExp
						// (RegGroup All) transfer
						if (docID > 0 && expNodeID > 0) {
							// grab the prev node and see if we are required as
							// part of the operation
							Node prevExpNode = expressionNodes
									.item(expNodeID - 1);
							String conditionNextOperation = AnalyticsFunctions
									.getAttributeByName(prevExpNode,
											"NextOperation").toLowerCase();

							if (conditionNextOperation.equals("and")) {

								// get the first position node and clone it
								ArrayList<ConditionsNode> list = (ArrayList<ConditionsNode>) conditionDocMetList
										.get(0);
								for (int cn = 0; cn < list.size(); cn++)
									try {
										condNodes.add((ConditionsNode) list
												.get(cn).clone());
									} catch (CloneNotSupportedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
							}
						}
						// end of Sep 17 2014 changes

						conditionDocMetList.add(docID, condNodes);
					}
				}

				ArrayList<ConditionField> fields = new ArrayList<ConditionField>();
				NodeList conditionNodes = expElement
						.getElementsByTagName("Condition");

				for (int cNode = 0; cNode < conditionNodes.getLength(); cNode++) {
					Node condNode = conditionNodes.item(cNode);
					ConditionField field = AnalyticsFunctions
							.getConditionField(condNode);
					fields.add(field);
				}

				DocumentSection documentSect = documentSet.get(docID);

				Logger.getRootLogger().debug(
						"AnalyticsProcessor::parseFormula formula : "
								+ formula.getIdentifier()
								+ " -- RunFormula instantiating for "
								+ documentSect.GetSectionName());
				futureList.add(eService.submit(new RunFormula(this,
						documentSect, docID, condNodes, formula, cidSectionID,
						expElement, nextExpressionAnd, fields)));

			} // end of for documentSet loop

			Object taskResult;
			for (Future future : futureList) {
				try {
					try {
						taskResult = future.get(900, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						// TODO Auto-generated catch block
						future.cancel(true);

						Logger.getRootLogger().info(
								"AnalyticsProcessor::parseFormula formula : "
										+ formula.getIdentifier()
										+ " -- RunFormula timed out.");
						// e.printStackTrace();
						continue;
					}
					RunFormula run = (RunFormula) taskResult;
					// we are setting this variable only if we find matching
					// results, otherwise we skip since there is more than one
					// document
					// that may be checked (default value is false)
					if (run.anyExpressionsMatched)
						expressionsMatched = true;

					// if the expression is not "AND" requiring a next set of
					// conditions in its own expression to also match
					// then we check which conditions have succeeded in matching
					// this expression
					if (!nextExpressionAnd) {
						for (int c = 0; c < run.condNodes.size(); c++) {
							ConditionsNode node = run.condNodes.get(c);

							// we announced this node before
							if (node.isExpressionsMet())
								continue;

							// if the expressions met by the distinct conditions
							// then we are ok with displaying the result
							if (!node.isExpressionsFailed()
									&& node.getExpressionsMet() >= expressionsRequirementCount) {
								node.setExpressionsMet(true);

								node.setOmitPrintedConditions((boolean) formula
										.getItem("OmitConditions").getObject());

								AnalyticsFunctions.populatePassConditionNode(
										node, logLevelLwr, formula,
										formulaExpressionsMet,
										otherFormulasMatched);

								formulaExpressionsMet.add(node);
							} // end if for expressions matched
						} // end for loop conditionMetList

						// since we are in an 'or' expression we don't need to
						// match
						// it any longer
						if (run.docID == documentSet.size() - 1)
							expressionsRequirementCount--;

					} // end if statement matching non-and conditions
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				}
			}

			if (nextExpressionAnd && !expressionsMatched)
				break;
		} // end for loop for expressionList

		return true;
	}

	/*
	 * parse() handles the main loading of the Analytics documentand
	 * subsequently begins processing the error report formulas
	 */
	private ArrayList<ConditionsNode> parse(boolean printResults, String outFile)
			throws SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc;

		ArrayList<ConditionsNode> formulaConditionsMet = new ArrayList<ConditionsNode>();

		Logger.getRootLogger().debug(
				"AnalyticsProcessor::parse DocBuilder loading xml formulas.");

		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(mAnalytics);
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e) {
			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parse DocBuilder loading xml failed with message: "
							+ e.getMessage());
			e.printStackTrace();
			return formulaConditionsMet;
		}

		Element eElement = (Element) doc.getElementsByTagName("Analytics")
				.item(0);

		String versionAttrib = "Unknown";

		if (eElement != null)
			versionAttrib = eElement.getAttribute("version");

		Logger.getRootLogger().info(
				"AnalyticsProcessor::parse version: " + versionAttrib);

		for (int i = 0; i < eElement.getChildNodes().getLength(); i++) {
			Node formulaNode = eElement.getChildNodes().item(i); // formula
																	// nodes
			if (formulaNode.getNodeType() != Node.ELEMENT_NODE)
				continue;

			// base element from document
			Element formulaElement = (Element) formulaNode;

			Formula formula = new Formula(formulaElement, i);

			Logger.getRootLogger().debug(
					"AnalyticsProcessor::parse instantiated formula : "
							+ formula.getIdentifier());

			long startTime = System.nanoTime();

			ArrayList<ConditionsNode> tmpConditionMetList = new ArrayList<ConditionsNode>();

			// determine our results
			parseFormula(formula, tmpConditionMetList, formulaConditionsMet);

			// sort the results
			tmpConditionMetList = AnalyticsFunctions
					.condenseConditions(tmpConditionMetList);
			String sortOpt = (String) formula.getItem("SortOption").getObject();
			String sortMethod = (String) formula.getItem("SortMethod")
					.getObject();

			if (sortOpt.length() < 1 && sortMethod.length() > 0) {
				if (sortMethod.equals("reverse"))
					Collections.reverse(tmpConditionMetList);
			}

			Collections.sort(tmpConditionMetList, new ConditionSort());

			// add to the overall list
			formulaConditionsMet.addAll(tmpConditionMetList);

			// Todo: Store time diff result for reference/statistics?
			long endTime = System.nanoTime();
			double difference = (endTime - startTime) / 1e6;
			if (mDebug) {
				String subStr = (String) formula.getItem("Name").getObject();
				if (subStr.length() > 80)
					subStr = subStr.substring(0, 80);
				Logger.getRootLogger().info(
						"AnalyticsProcessor::parse Formula completion "
								+ subStr + " took " + difference
								+ " milliseconds to complete.");
			}
		}

		PrintStream stream = new PrintStream(System.out);

		if (outFile.length() > 0)
			stream = new PrintStream(new FileOutputStream(outFile, false));

		AnalyticsResults.PrintResults(this, formulaConditionsMet,
				mprintConditionsSetting, stream, versionAttrib, outFile,
				mFormatType, printResults);

		return formulaConditionsMet;
	}

	/**
	 * Takes in a string and throws in any xpath or conditions (which we already
	 * cached) {Condition:xyz}
	 * 
	 * @param section
	 *            DocumentSection - current xml document from error report
	 *            (parsing the formula)
	 * @param message
	 *            String - Message requiring parsing for {xpath} attributes
	 * @param matchedPosition
	 *            int - matchedPosition is the current position (of element) in
	 *            the cidDoc xml we are matching
	 * @param node
	 *            ConditionsNode - this is the node reference which has the
	 *            Condition historical values (for parsing)
	 */
	private String parseStringWithConditions(DocumentSection section,
			String message, int matchedPosition, ConditionsNode node) {
		String str = "";

		for (int i = 0; i < message.length();) {
			// find an xpath denoted section eg. {
			int idx = message.indexOf("{", i);
			if (idx > -1) {
				// find the end of the xpath "}"
				str += message.substring(i, idx);
				int endIdx = message.indexOf("}", idx + 1);

				if (endIdx > idx) {
					// pull the xpath minus the brackets
					String xpath = message.substring(idx + 1, endIdx);

					String output = "";

					// check if we are trying to take a value we parsed from
					// regular expressions
					// and pass as a display value
					boolean conditionBasedValue = false;

					int idxColon = xpath.indexOf(":");
					if (idxColon > 0 && idxColon < xpath.length()) {
						String frontPartString = xpath.substring(0, idxColon);
						String endPartString = xpath.substring(idxColon + 1)
								.toLowerCase();
						if (frontPartString.toLowerCase().equals("condition")) {

							if (endPartString.equals("sectionname")) {
								output = AnalyticsFunctions
										.generateFileFromContent(section);
								conditionBasedValue = true;
							} else {
								output = node.getCondition(endPartString);
								conditionBasedValue = true;
							}
						}
					}

					// if parsing for a condition value was not the use-case
					// use the xpath value and check in the document at the
					// position we are currently parsing
					if (!conditionBasedValue) {
						synchronized (ERFramework.mDocBuilderFactory) {
							output = getNodeValue(section.GetDocument(), xpath,
									matchedPosition, null);
						}
					}

					// if no result reset the string with the xpath
					if (output == null)
						str += "{" + xpath + "}";
					else
						str += output;

					// update the iterator position
					i = endIdx + 1;
				}
			}
			// we could not find a position in the doc that has a bracket so
			// just pull the rest into the result message
			else if (idx < 0) {
				str += message.substring(i);
				break;
			}
		}

		return str;
	}

	/**
	 * Handles the base formula, pulling the expressions that need to be tested
	 * against the Section (cid), then testing the conditions that must be met
	 * for the expression to succeed
	 * 
	 * @param section
	 *            DocumentSection - current xml document from error report
	 *            (parsing the formula)
	 * @param message
	 *            String - Message requiring parsing for {xpath} attributes
	 * @param matchedPosition
	 *            int - matchedPosition is the current position (of element) in
	 *            the cidDoc xml we are matching
	 * @param conditionParsedValue
	 *            String - this is the value we took from the error report and
	 *            applied regular expresion to
	 * @param conditionName
	 *            String - the conditionName assigned to the condition for
	 *            passing parsed values
	 * @return String, returns the output after xpath sections that can match
	 *         are met
	 */
	private String parseMessage(DocumentSection section, String message,
			int matchedPosition, String conditionParsedValue,
			String conditionName, boolean isSectionNameVar) {
		String str = "";

		for (int i = 0; i < message.length();) {
			// find an xpath denoted section eg. {
			int idx = message.indexOf("{", i);
			if (idx > -1) {
				// find the end of the xpath "}"
				str += message.substring(i, idx);
				int endIdx = message.indexOf("}", idx + 1);

				if (endIdx > idx) {
					// pull the xpath minus the brackets
					String xpath = message.substring(idx + 1, endIdx);

					String output = "";

					// check if we are trying to take a value we parsed from
					// regular expressions
					// and pass as a display value
					boolean conditionBasedValue = false;

					int idxColon = xpath.indexOf(":");
					if (idxColon > 0 && idxColon < xpath.length()) {
						String frontPartString = xpath.substring(0, idxColon);
						String endPartString = xpath.substring(idxColon + 1)
								.toLowerCase();
						if (frontPartString.toLowerCase().equals("condition")) {

							if (isSectionNameVar
									&& endPartString.equals("sectionname")) {
								output = AnalyticsFunctions
										.generateFileFromContent(section);
								conditionBasedValue = true;
							} else if (endPartString.equals(conditionName
									.toLowerCase())) {
								output = conditionParsedValue;
								conditionBasedValue = true;
							}
						}
					}

					// if parsing for a condition value was not the use-case
					// use the xpath value and check in the document at the
					// position we are currently parsing
					if (!conditionBasedValue) {

						synchronized (ERFramework.mDocBuilderFactory) {
							output = getNodeValue(section.GetDocument(), xpath,
									matchedPosition, null);
						}
					}

					// if no result reset the string with the xpath
					if (output == null)
						str += "{" + xpath + "}";
					else
						str += output;

					// update the iterator position
					i = endIdx + 1;
				}
			}
			// we could not find a position in the doc that has a bracket so
			// just pull the rest into the result message
			else if (idx < 0) {
				str += message.substring(i);
				break;
			}
		}

		return str;
	}

	private NodeList getDocCachedList(Document cidDoc, String xPathQuery) {
		// might want to make this a hash map someday.
		for (int i = 0; i < mCacheList.size(); i++) {
			XPathCache cache = mCacheList.get(i);
			if (cache.getCidDoc() == cidDoc
					&& cache.getXPathQuery().equals(xPathQuery))
				return cache.getNodeList();
		}
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList resultList = null;
		// simple cache for avoiding retrieving multiple times
		try {
			XPathExpression expr = (XPathExpression) xpath.compile(xPathQuery);
			resultList = (NodeList) expr.evaluate(cidDoc,
					XPathConstants.NODESET);
			mCacheList.add(new XPathCache(resultList, cidDoc, xPathQuery));
		} catch (XPathExpressionException e) {

		}

		return resultList;
	}

	private RegEXPCache getRegExpCache(RunFormula formula, String regexpQuery,
			String value) {
		// might want to make this a hash map someday.
		try {
			for (int i = 0; i < mCurRegCache.size(); i++) {
				RegEXPCache cache = mCurRegCache.get(i);
				if (cache.getRegEXPQuery().equals(regexpQuery)
						&& cache.getRunFormula().documentSet == formula.documentSet
						// checking the string value will stop XML sections from
						// sending the first node-set matched back
						&& cache.getQueryValue().equals(value)) {
					cache.getMatcher().reset();
					Logger.getRootLogger().debug(
							"Cache of pattern found for document: "
									+ formula.documentSet
											.GetOriginalSectionName()
									+ " - pattern: " + regexpQuery);
					return cache;
				}
			}
		} catch (Exception ex) {

		}

		// pass the regular expression value we have
		// from the expression
		Pattern pattern = null;
		try {
			Logger.getRootLogger().debug(
					"Compiling pattern and attempting match for document: "
							+ formula.documentSet.GetOriginalSectionName()
							+ " - pattern: " + regexpQuery);
			pattern = Pattern.compile(regexpQuery);
			Matcher matcher = pattern.matcher(value);
			RegEXPCache cache = new RegEXPCache(formula, regexpQuery, value,
					matcher);
			mCurRegCache.add(cache);
			return cache;
		} catch (Exception ex) {

		}

		return null;
	}

	private String getNodeValue(Document cidDoc, String xPathQuery,
			int position, ConditionsNode node) {
		NodeList resultList = getDocCachedList(cidDoc, xPathQuery);

		if (node != null) {
			int idxColon = xPathQuery.indexOf(":");
			if (idxColon > 0 && idxColon < xPathQuery.length()) {
				String frontPartString = xPathQuery.substring(0, idxColon);
				String endPartString = xPathQuery.substring(idxColon + 1)
						.toLowerCase();
				if (frontPartString.toLowerCase().equals("condition")) {
					return node.getCondition(endPartString);
				}
			}
		}

		if (resultList == null)
			return null;

		Node resultNode = resultList.item(position);

		if (resultNode == null)
			return null;

		String value = resultNode.getNodeValue();

		// no good if we don't have a value to compare to
		if (value == null) {
			value = resultNode.getTextContent();

			if (value == null)
				return "";
		}

		return value;
	}

	private void PullDocSection(String cidName,
			ArrayList<DocumentSection> documentSet, boolean wildcardValue,
			String extension) {
		DSCacheEntry entry = mDocumentSections.get(cidName);
		if (entry != null && entry.wildcardValue == wildcardValue
				&& entry.extension == extension) {
			documentSet.addAll(entry.documentSet);
			return; // we are good, don't bother with the rest!
		} else if (entry != null) // we got an entry back, but the cached entry
									// isn't valid for us
		{
			mDocumentSections.remove(cidName);
			entry = null;
		}

		try {
			mFramework.getCidListAsDocument(cidName, documentSet,
					wildcardValue, extension);

			boolean noCache = false;
			for (int i = 0; i < documentSet.size(); i++) {
				DocumentSection section = documentSet.get(i);
				if (!section.IsXMLSection()) {
					noCache = true;
					break;
				}
			}

			if (noCache)
				return;

			// create a cached entry to re-use
			entry = new DSCacheEntry();
			entry.cidName = cidName;
			entry.documentSet = documentSet;
			entry.wildcardValue = wildcardValue;
			entry.extension = extension;

			mDocumentSections.put(cidName, entry);
		} catch (Exception e) {

		}
	}

	private InputStream mAnalytics = null;
	private ERFramework mFramework = null;
	private String mFormatType = "txt";
	private boolean mDebug = true; // get formula runtimes in console
	private PRINT_MET_CONDITIONS mprintConditionsSetting = PRINT_MET_CONDITIONS.HIDEDEFAULT;
	// tracks a list of entries from previous xpath queries on a document
	private ArrayList<XPathCache> mCacheList = null;
	private ArrayList<RegEXPCache> mCurRegCache = null; // used for running a
														// loop of conditions
														// with the same regexp
	private REPORT_TYPE mReportType; // determined type of report we are reading

	public static String outputFileName = "";
	public static String GENERATED_FILES_DIR = "related_files";

	private ExecutorService eService = null;
	
	private Hashtable<String, DSCacheEntry> mDocumentSections = new Hashtable<String, DSCacheEntry>();
} // end AnalyticsProcessor class
