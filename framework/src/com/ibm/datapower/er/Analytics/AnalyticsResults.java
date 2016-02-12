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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.ibm.datapower.er.Analytics.AnalyticsProcessor.PRINT_MET_CONDITIONS;
import com.ibm.datapower.er.Analytics.ConditionsNode.LogLevelType;

public class AnalyticsResults {

	public static void PrintResults(AnalyticsProcessor analytics,
			ArrayList<ConditionsNode> formulaConditionsMet,
			PRINT_MET_CONDITIONS printConditions, PrintStream stream,
			String versionAttrib, String outFile, String formatType,
			boolean printResults) {

		if (printResults) {
			// print output to display
			boolean formatTypeTxt = true;

			// format type html check, if so print html header
			if (formatType.equals("html")) {
				PrintHtmlHeader(analytics, stream, versionAttrib);
				formatTypeTxt = false;
			} else {
				PrintTextHeader(stream, versionAttrib);
			}

			int lastFormulaID = -1;

			boolean printURLs = false;

			double sumValue = 0.0;
			
			ArrayList<ConditionsNode> metConditionsList = reorderMetConditions(
					analytics, outFile, formulaConditionsMet);
			for (int z = 0; z < metConditionsList.size(); z++) {
				ConditionsNode curNode = (ConditionsNode) metConditionsList
						.get(z);

				ConditionsNode nextNode = null;

				// this is passed on to PrintConditionResultHtml, lastFormulaID
				// gets updated before we call this function
				if ( curNode.getDisplayName().length() < 1 )
					continue;
				
				if ( curNode.getSumCondition().length() > 0 )
				{
					String val = curNode.getCondition(curNode.getSumCondition());
					try {
						double dbl = Double.parseDouble(val);
						sumValue += dbl;
					}catch(Exception ex)  { }
				}
				
				int prevFormulaID = lastFormulaID;
				if (formatTypeTxt)
					PrintConditionResultText(stream, curNode, printConditions);
				else {
					int nextPos = z + 1;
					if (nextPos < metConditionsList.size())
						nextNode = metConditionsList.get(nextPos);

					if (curNode.getInternalFormulaID() != lastFormulaID) {
						if (lastFormulaID > -1)
						{
							PrintFormulaEnd(stream);
							sumValue = 0.0;
						}
						
						PrintFormulaStart(stream, curNode, nextNode);
						lastFormulaID = curNode.getInternalFormulaID();
						printURLs = true;
					} else
						printURLs = false;

					PrintConditionResultHtml(stream, curNode, nextNode,
							printConditions, prevFormulaID, printURLs, sumValue);
				}
			}

			// if html we close out with the end elements
			if (!formatTypeTxt) {
				PrintFormulaEnd(stream);
				PrintHtmlFooter(stream);
			}

			if ( metConditionsList.size() == 0 )
			{
				if (formatTypeTxt )
					stream.println("No results found.");
				else
					stream.println("<b>No Results Found</b>");
			}
			if (outFile.length() > 0)
				stream.close();
		}
	}

	private static void PrintTextHeader(PrintStream stream, String versionAttrib) {
		stream.println("[ ERTool Analytics Report, Rules Version: "
				+ versionAttrib + " ]");
		stream.println("");
	}

	private static void PrintConditionResultText(PrintStream stream,
			ConditionsNode node, PRINT_MET_CONDITIONS printConditions) {
		String dispName = node.getDisplayName().replaceAll("<.*?>", "");
		String dispMsg = node.getDisplayMessage().replaceAll("<.*?>", "");

		if ( node.getCondenseCount() > 1 )
			dispName += ", Count: " + node.getCondenseCount();
		
		stream.println("[" + node.getLogLevel().toString() + "] "
				+ dispName);
		if (node.getDisplayMessage().length() > 0)
			stream.println("\t" + dispMsg);

		if (node.mURLs.size() > 0) {
			stream.println("- URLs related to topic:");
			for (int u = 0; u < node.mURLs.size(); u++)
				stream.println("\t" + node.mURLs.get(u).toString());
		}

		if (printConditions == PRINT_MET_CONDITIONS.SHOWALL
				|| (!node.isOmitPrintedConditions() && printConditions == PRINT_MET_CONDITIONS.HIDEDEFAULT)) {
			stream.println("- Matched Conditions:");
			for (int m = 0; m < node.matchedConditions.size(); m++)
				stream.println("\t" + node.matchedConditions.get(m));
		}

		stream.println();
	}

	private static void PrintHtmlHeader(AnalyticsProcessor analytics,
			PrintStream stream, String versionAttrib) {
		stream.println("<html><head><title>ERTool Analytics Report, Rules Version: "
				+ versionAttrib + "</title>");

		ReadFileToStream(analytics, stream, "Analytics/analyticsjsheader.txt");
		ReadFileToStream(analytics, stream, "Analytics/analyticsform.txt");
	}

	private static void PrintFormulaStart(PrintStream stream,
			ConditionsNode node, ConditionsNode nextNode) {

		String multipleNodes = "";

		// check if there is more than one entry to modify the header
		if (nextNode != null
				&& node.getInternalFormulaID() == nextNode
						.getInternalFormulaID())
			multipleNodes = "... + more";

		String dispType = "block";

		// the CollapseResult attribute set at the formula level means we want
		// to not display the results unless the
		// user clicks to expand
		if (node.isCollapseSet())
			dispType = "none";

		String logLevelLwr = node.getLogLevel().toString().toLowerCase();
		stream.println("<div onClick=\"showHide('div"
				+ node.getInternalFormulaID()
				+ "')\" id=\"div" + node.getInternalFormulaID() 
				+ "header\" class=\"textHeader\" fieldtype=\"" + node.getCategories()
				+ ";" + logLevelLwr + "\" formulaid=\"" + node.getFormulaID()
				+ "\" style=\"cursor:hand; cursor:pointer\"><b>"
				+ node.getDisplayName()// .replace("<", "[").replace(">", "]")
				+ multipleNodes + "</b></div><div id=\"div"
				+ node.getInternalFormulaID()
				+ "\" class=\"textBody\" fieldtype=\"" + node.getCategories()
				+ ";" + logLevelLwr + "\" style=\"display:" + dispType
				+ "\"><table border='1'>");
	}

	private static void PrintFormulaEnd(PrintStream stream) {
		stream.println("</table></div>");
	}

	private static void PrintHtmlFooter(PrintStream stream) {
		stream.println("</body></html>");
	}

	private static void PrintConditionResultHtml(PrintStream stream,
			ConditionsNode node, ConditionsNode nextNode,
			PRINT_MET_CONDITIONS printConditions, int prevFormulaID,
			boolean printURLs, double sumValue) {
		// determine if we are going to print the 'go to top' link
		boolean printEnd = false;
		boolean endFormula = false;
		if ( nextNode == null || nextNode != null && nextNode.getInternalFormulaID() != node.getInternalFormulaID() )
			endFormula = true;
		
		if ((nextNode == null && prevFormulaID > 0 && prevFormulaID == node
				.getInternalFormulaID())
				|| (nextNode != null
						&& nextNode.getInternalFormulaID() != node
								.getInternalFormulaID() && prevFormulaID == node
						.getInternalFormulaID()))
			printEnd = true;

		// if we printed the log level string by the urls on top, otherwise we
		// put it on the first conditionsnode
		boolean printedLogLevel = false;

		if (printURLs && node.mURLs.size() > 0) {
			printedLogLevel = true;
			stream.println("<tr class='headertr'><td><p><b>" + node.getLogLevel().toString()
					+ "</b></p></td>");

			stream.println("<td align='center'><b>Related URLs</b></td>");
			stream.println("<td><ul>");
			for (int u = 0; u < node.mURLs.size(); u++) {
				stream.println("<li>" + node.mURLs.get(u).mDescription + "<a href=\""
						+ node.mURLs.get(u).mURL + "\">"
						+ node.mURLs.get(u).mURL + "</a>");
			}
			stream.println("</ul></td></tr>");
		}
		
		String dispName = node.getDisplayName();
		String dispMsg = node.getDisplayMessage();

		if ( node.getCondenseCount() > 1 )
			dispName += ", Count: " + node.getCondenseCount();
		
		stream.println("<tr>");

		// if we didn't print the log level with the URLs line do it here
		// instead
		if (printURLs && !printedLogLevel)
			stream.println("<td><p><b>" + node.getLogLevel().toString()
					+ "</b></p></td>");
		else
			stream.println("<td></td>");

		stream.println("<td align='center'>");

		if (node.getPopupFileName().length() > 0) {
			stream.println("<a target=\"wind" + Math.random() + "\" href=\""
					+ node.getPopupFileName() + "?" + node.getURIAttributes()
					+ "')\">| Details |</a> ");
		}

		stream.println("<b>" + dispName + "</b></td>");
		if (dispMsg.length() > 0)
			stream.println("<td><p>" + dispMsg + "</p></td></tr>");

		if (printConditions == PRINT_MET_CONDITIONS.SHOWALL
				|| (!node.isOmitPrintedConditions() && printConditions == PRINT_MET_CONDITIONS.HIDEDEFAULT)) {
			stream.println("<tr><td></td><td align='center'><b>Matched Conditions</b></td><td><p>");
			for (int m = 0; m < node.matchedConditions.size(); m++)
				stream.println("" + node.matchedConditions.get(m) + "<br>");

			stream.println("</p></td></tr><tr><td></td></tr>");
		}

		if ( endFormula && sumValue != 0.0 )
		{
			DecimalFormat formatter = new DecimalFormat("0.000");
			String sumTotal = formatter.format(sumValue);
			stream.println("<tr><td></td><td align='center'><b>Sum of " + node.getSumCondition() + " is " + sumTotal + "</b></td>");
			stream.println("<td><ul><p></td></tr>");
		}
		
		if (printEnd)
		{		
				stream.println("<tr><td></td><td><a href=\"#div"
						+ node.getInternalFormulaID()
						+ "\">Go to Formula Top</a></td></tr>");
		}

	}

	private static void CreateSubFile(AnalyticsProcessor analytics,
			String dirFile, String popupFile) {
		String fileName = dirFile + popupFile;

		File file = new File(fileName);
		if (!file.exists()) {
			PrintStream stream = null;
			try {
				stream = new PrintStream(new FileOutputStream(fileName, false));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (stream != null) {
				ReadFileToStream(analytics, stream, "Analytics/" + popupFile);
				stream.close();
			}
		}
	}

	private static void ReadFileToStream(AnalyticsProcessor analytics,
			PrintStream stream, String filename) {
		BufferedReader input = null;

		try {
			input = new BufferedReader(new InputStreamReader(analytics
					.getClass().getClassLoader().getResourceAsStream(filename)));
		} catch (Exception ex) {

		}

		String s = "";

		if (input == null)
			return;

		try {
			while ((s = input.readLine()) != null) {
				stream.println(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<ConditionsNode> reorderMetConditions(
			AnalyticsProcessor analytics, String outFile,
			ArrayList<ConditionsNode> inNodes) {
		ArrayList<ConditionsNode> overrideList = new ArrayList<ConditionsNode>();

		// general categories
		ArrayList<ConditionsNode> infoList = new ArrayList<ConditionsNode>();
		ArrayList<ConditionsNode> warnList = new ArrayList<ConditionsNode>();
		ArrayList<ConditionsNode> errorList = new ArrayList<ConditionsNode>();
		ArrayList<ConditionsNode> criticalList = new ArrayList<ConditionsNode>();
		for (int i = 0; i < inNodes.size(); i++) {
			ConditionsNode node = inNodes.get(i);

			if (node.getPopupFileName().length() > 0) {
				File file = new File(outFile);
				File parentDir = file.getParentFile(); // get parent dir
				String dir = parentDir.getPath();
				if (dir.contains(":\\"))
					dir += "\\";
				else
					dir += "/";

				CreateSubFile(analytics, dir, node.getPopupFileName());
			}

			if (node.isTopCondition())
				overrideList.add(node);
			else if (node.getLogLevel() == LogLevelType.INFO)
				infoList.add(node);
			else if (node.getLogLevel() == LogLevelType.WARNING)
				warnList.add(node);
			else if (node.getLogLevel() == LogLevelType.ERROR)
				errorList.add(node);
			else if (node.getLogLevel() == LogLevelType.CRITICAL)
				criticalList.add(node);
		}

		ArrayList<ConditionsNode> newList = new ArrayList<ConditionsNode>();
		newList.addAll(overrideList);
		newList.addAll(criticalList);
		newList.addAll(errorList);
		newList.addAll(warnList);
		newList.addAll(infoList);
		return newList;
	}

}
