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

package com.ibm.datapower.er.Analytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import com.ibm.datapower.er.Analytics.AnalyticsProcessor.PRINT_MET_CONDITIONS;
import com.ibm.datapower.er.Analytics.ConditionsNode.LogLevelType;

public class AnalyticsResults {

	public static void PrintResultsXML(AnalyticsProcessor analytics, ArrayList<ConditionsNode> formulaConditionsMet,
			PRINT_MET_CONDITIONS printConditions, PrintStream stream, String versionAttrib, String outFile,
			boolean printResults) {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (dBuilder == null)
			return;

		Document doc = dBuilder.newDocument();

		// root element
		Element rootElement = doc.createElement("ERToolResults");

		Element allResultsElement = doc.createElement("Results");

		ArrayList<ConditionsNode> metConditionsList = reorderMetConditions(analytics, outFile, formulaConditionsMet);
		Element resultElement = doc.createElement("FormulaResult");
		int lastFormulaID = -1;
		boolean resetElement = false;
		for (int z = 0; z < metConditionsList.size(); z++) {
			ConditionsNode curNode = (ConditionsNode) metConditionsList.get(z);

			if (curNode.getInternalFormulaID() != lastFormulaID) {
				if (lastFormulaID != -1) {
					allResultsElement.appendChild(resultElement);
					resultElement = doc.createElement("FormulaResult");
				}

				resultElement.setAttribute("LogLevel", curNode.getLogLevel().toString());

				if (curNode.getFormulaID().length() > 0)
					resultElement.setAttribute("FormulaID", curNode.getFormulaID());

				if (curNode.mURLs.size() > 0) {
					Element urlBaseElement = doc.createElement("RelatedURL");

					for (int u = 0; u < curNode.mURLs.size(); u++) {
						Element urlElement = doc.createElement("URL");
						urlElement.setTextContent(curNode.mURLs.get(u).mURL);

						if (curNode.mURLs.get(u).mDescription.length() > 0)
							urlElement.setAttribute("Description", curNode.mURLs.get(u).mDescription);
						urlBaseElement.appendChild(urlElement);
					}

					resultElement.appendChild(urlBaseElement);
				}

				resetElement = false;
			}
			Element currentElement = doc.createElement("Result");
			lastFormulaID = curNode.getInternalFormulaID();
			String dispMsg = "";
			String dispName = "";

			if (curNode.getDisplayName().length() > 0) {
				Element dispElement = doc.createElement("DisplayName");
				dispName = curNode.getDisplayName().replaceAll("<.*?>", "");
				dispElement.setTextContent(dispName);
				currentElement.appendChild(dispElement);
			}
			if (curNode.getDisplayMessage().length() > 0) {
				Element dispElement2 = doc.createElement("DisplayMessage");
				dispMsg = curNode.getDisplayMessage().replaceAll("<.*?>", "");
				dispElement2.setTextContent(dispMsg);
				currentElement.appendChild(dispElement2);
			}

			String pipedData = "";
			if (dispName.contains("||"))
				pipedData = dispName;
			else if (dispMsg.contains("||"))
				pipedData = dispMsg;

			Element mappedConditionResults = doc.createElement("MappedConditions");
			for (int i = 0; i < curNode.getMappedConditions().size(); i++) {
				for (Map.Entry<String, MappedCondition> entry : curNode.getMappedConditions().entrySet()) {
					MappedCondition mc = (MappedCondition) entry.getValue();
					if (mc.MappedConditionNameOriginalCase.length() < 1)
						continue;

					if (mc.MappedConditionPosition == i) {
						Element mappedCondition = doc.createElement("MappedCondition");
						mappedCondition.setAttribute("Name", mc.MappedConditionNameOriginalCase);
						mappedCondition.setAttribute("Value", mc.MappedConditionValue);
						mappedConditionResults.appendChild(mappedCondition);
						break;
					}
				}
			}

			resultElement.appendChild(currentElement);
			if (curNode.getMappedConditions().size() > 0)
				currentElement.appendChild(mappedConditionResults);
			resetElement = true;
		}
		if (resetElement)
			allResultsElement.appendChild(resultElement);

		rootElement.appendChild(allResultsElement);
		doc.appendChild(rootElement);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			StringWriter writer = new StringWriter();

			// transform document to string
			transformer.transform(new DOMSource(doc), new StreamResult(writer));

			String xmlString = writer.getBuffer().toString();

			stream.print(xmlString);
		} catch (Exception ex) {

		}
		stream.close();
	}

	public static void PrintResults(AnalyticsProcessor analytics, ArrayList<ConditionsNode> formulaConditionsMet,
			PRINT_MET_CONDITIONS printConditions, PrintStream stream, String versionAttrib, String outFile,
			String formatType, boolean printResults) {

		if (printResults) {
			// print output to display
			boolean formatTypeTxt = true;

			if (formatType.equals("xml")) {
				PrintResultsXML(analytics, formulaConditionsMet, printConditions, stream, versionAttrib, outFile,
						printResults);
				return;
			}
			// format type html check, if so print html header
			else if (formatType.equals("html")) {
				PrintHtmlHeader(analytics, stream, versionAttrib);
				PrintSidebarHtmlHeader(stream, formulaConditionsMet);
				formatTypeTxt = false;
			} else {
				PrintTextHeader(stream, versionAttrib);
			}

			int lastFormulaID = -1;

			boolean formulaStart = false;

			double sumValue = 0.0;

			ArrayList<ConditionsNode> metConditionsList = reorderMetConditions(analytics, outFile,
					formulaConditionsMet);
			boolean pipedContent = false;
			for (int z = 0; z < metConditionsList.size(); z++) {
				ConditionsNode curNode = (ConditionsNode) metConditionsList.get(z);

				ConditionsNode nextNode = null;

				// this is passed on to PrintConditionResultHtml, lastFormulaID
				// gets updated before we call this function
				if (curNode.getDisplayName().length() < 1)
					continue;

				// sumValue was originally set lower down, but the first result would always be
				// wiped out because of that
				if (curNode.getInternalFormulaID() != lastFormulaID)
					sumValue = 0.0;

				if (curNode.getSumCondition().length() > 0) {
					String val = curNode.getCondition(curNode.getSumCondition());
					try {
						double dbl = Double.parseDouble(val);
						sumValue += dbl;
					} catch (Exception ex) {
					}
				}

				int prevFormulaID = lastFormulaID;

				int nextPos = z + 1;
				if (nextPos < metConditionsList.size())
					nextNode = metConditionsList.get(nextPos);

				if (curNode.getInternalFormulaID() != lastFormulaID) {

					if (!formatTypeTxt) {
						if (lastFormulaID > -1)
							PrintFormulaEnd(stream, pipedContent);

						PrintFormulaStart(stream, curNode, nextNode, z, metConditionsList);
					}
					lastFormulaID = curNode.getInternalFormulaID();
					formulaStart = true;
				} else
					formulaStart = false;

				if (formatTypeTxt)
					PrintConditionResultText(stream, curNode, printConditions, formulaStart);
				else
					pipedContent = PrintConditionResultHtml(stream, curNode, nextNode, printConditions, prevFormulaID,
							formulaStart, sumValue);
			}

			// if html we close out with the end elements
			if (!formatTypeTxt) {
				PrintFormulaEnd(stream, pipedContent);
				PrintHtmlFooter(stream);
			}

			if (metConditionsList.size() == 0) {
				if (formatTypeTxt)
					stream.println("No results found.");
				else
					stream.println("<b>No Results Found</b>");
			}
			if (outFile.length() > 0)
				stream.close();
		}
	}

	private static void PrintTextHeader(PrintStream stream, String versionAttrib) {
		stream.println("[ ERTool Analytics Report, Rules Version: " + versionAttrib + " ]");
		stream.println("");
	}

	private static void PrintConditionResultText(PrintStream stream, ConditionsNode node,
			PRINT_MET_CONDITIONS printConditions, boolean formulaStart) {
		String dispName = node.getDisplayName().replaceAll("<.*?>", "");
		String dispMsg = node.getDisplayMessage().replaceAll("<.*?>", "");

		if (node.getCondenseCount() > 1)
			dispName += ", Count: " + node.getCondenseCount();

		boolean pipeTableUsedName = false, pipeTableUsedMsg = false;

		if (dispName.contains("||")) {
			pipeTableUsedName = true;
		}

		if (dispMsg.contains("||")) {
			pipeTableUsedMsg = true;
		}

		// TODO: this is not good formatting, need to explore tracking size of each
		// field before even going through the table
		// and establish appropriate spacing and utilize String.format spacing
		dispName = TranslatePipeStringToTextTable(node, dispName);
		dispMsg = TranslatePipeStringToTextTable(node, dispMsg);

		if ((!pipeTableUsedName && !pipeTableUsedMsg) || (pipeTableUsedMsg && formulaStart))
			stream.println("[" + node.getLogLevel().toString() + "] " + dispName);
		else if (pipeTableUsedName) {
			if (formulaStart)
				stream.println("[" + node.getLogLevel().toString() + "] " + dispName);
			else
				stream.println("\t " + dispName);
		}

		if (node.mURLs.size() > 0 && formulaStart) {
			stream.println("- URLs related to topic:");
			for (int u = 0; u < node.mURLs.size(); u++)
				stream.println("\t" + node.mURLs.get(u).toString());
		}

		if (formulaStart && pipeTableUsedMsg) {
			for (int i = 0; i < node.getMappedConditions().size(); i++) {
				for (Map.Entry<String, MappedCondition> entry : node.getMappedConditions().entrySet()) {
					MappedCondition mc = (MappedCondition) entry.getValue();
					if (mc.MappedConditionPosition == i) {
						stream.print("\t" + mc.MappedConditionNameOriginalCase);
						break;
					}
				}
			}
			stream.println();
		}

		if (node.getDisplayMessage().length() > 0)
			stream.println("\t" + dispMsg);

		if (printConditions == PRINT_MET_CONDITIONS.SHOWALL
				|| (!node.isOmitPrintedConditions() && printConditions == PRINT_MET_CONDITIONS.HIDEDEFAULT)) {
			stream.println("- Matched Conditions:");
			for (int m = 0; m < node.matchedConditions.size(); m++)
				stream.println("\t" + node.matchedConditions.get(m));
		}

		if (!pipeTableUsedMsg)
			stream.println();
	}

	private static void PrintHtmlHeader(AnalyticsProcessor analytics, PrintStream stream, String versionAttrib) {
		stream.println("<html><head><title>ERTool Analytics Report, Rules Version: " + versionAttrib + "</title>");

		ReadFileToStream(analytics, stream, "Analytics/analyticsjsheader.txt");
		ReadFileToStream(analytics, stream, "Analytics/analyticsform.txt");
	}

	private static void PrintSidebarHtmlHeader(PrintStream stream, ArrayList<ConditionsNode> nodes) {
		stream.println(
				"<div id=\"sidebar\"><div id=\"sidebar_inner\"><a name=\"sidebartab2\" id=\"filterbutton\" onClick=\"updateSidebar();\">Hide Sidebar</a><div id=\"sidebarcontents\">");

		// moved search to sidebar to make things more fluid with search/reset
		stream.println(
				"<input type=\"checkbox\" name=\"casesensitive\" onchange=\"updateFieldsBySearch(getFieldByName('searchfield',0).value)\">"
						+ "Case-Sensitive Search: <input type=\"text\" name=\"searchfield\" onpaste=\"updateFieldsBySearch(this.value)\""
						+ " onchange=\"updateFieldsBySearch(this.value)\" oninput=\"updateFieldsBySearch(this.value)\"/><input type=\"button\" onclick=\""
						+ "getFieldByName('searchfield',0).value=''; loadDefault(); updateFieldsBySearch('');\" value=\"Reset\">");
		// all sidebar related content here

		boolean firstMatch = true;
		for (int i = 0; i < nodes.size(); i++) {
			ConditionsNode node = (ConditionsNode) nodes.get(i);
			if (node.getCategories().toLowerCase().contains("sidebarresult")) {
				if (firstMatch) {
					stream.println("<table border='1'>");
					firstMatch = false;
				}
				stream.println("<tr><td align='center'>");

				stream.println("<b>" + node.getDisplayName() + "</b></td>");
				if (node.getDisplayMessage().length() > 0)
					stream.println("<td><p>" + node.getDisplayMessage() + "</p></td>");

				stream.println("</tr>");
			}
		}
		if (!firstMatch) {
			stream.println("</table>");
		}
		stream.println("</div></div></div>");
	}

	private static void PrintFormulaStart(PrintStream stream, ConditionsNode node, ConditionsNode nextNode, int curPos,
			ArrayList<ConditionsNode> metConditionsList) {

		String multipleNodes = "";

		// check if there is more than one entry to modify the header
		if (nextNode != null && node.getInternalFormulaID() == nextNode.getInternalFormulaID()) {
			multipleNodes = "... + more";
			int count = 0;
			for (int i = curPos; i < metConditionsList.size(); i++) {
				ConditionsNode tmpNode = (ConditionsNode) metConditionsList.get(i);
				if (tmpNode.getInternalFormulaID() != node.getInternalFormulaID())
					break;
				count += 1;
			}
			multipleNodes += "[" + count + "]";
		}

		String dispType = "block";

		// the CollapseResult attribute set at the formula level means we want
		// to not display the results unless the
		// user clicks to expand
		if (node.isCollapseSet())
			dispType = "none";

		String logLevelLwr = node.getLogLevel().toString().toLowerCase();
		stream.println("<div onClick=\"showHide('div" + node.getInternalFormulaID() + "')\" id=\"div"
				+ node.getInternalFormulaID() + "header\" class=\"textHeader\" fieldtype=\"" + node.getCategories()
				+ ";" + logLevelLwr + "\" formulaid=\"" + node.getFormulaID()
				+ "\" style=\"cursor:hand; cursor:pointer\"><b>" + node.getDisplayName()// .replace("<",
																						// "[").replace(">", "]")
				+ multipleNodes + "</b></div><div id=\"div" + node.getInternalFormulaID()
				+ "\" class=\"textBody\" fieldtype=\"" + node.getCategories() + ";" + logLevelLwr
				+ "\" style=\"display:" + dispType + "\"><table border='1'>");
	}

	private static void PrintFormulaEnd(PrintStream stream, boolean pipedContent) {
		if (pipedContent)
			stream.println("</table></td></table></div>");
		else
			stream.println("</table></div>");
	}

	private static void PrintHtmlFooter(PrintStream stream) {
		stream.println("</body></html>");
	}

	private static boolean PrintConditionResultHtml(PrintStream stream, ConditionsNode node, ConditionsNode nextNode,
			PRINT_MET_CONDITIONS printConditions, int prevFormulaID, boolean formulaStart, double sumValue) {
		// determine if we are going to print the 'go to top' link
		boolean printEnd = false;
		boolean endFormula = false;
		// double pipe used to create tables within formula results
		boolean pipedContent = false;

		String dispName = node.getDisplayName();
		String dispMsg = node.getDisplayMessage();

		if (dispName.contains("||") || dispMsg.contains("||")) {
			pipedContent = true;
		}

		if (nextNode == null || nextNode != null && nextNode.getInternalFormulaID() != node.getInternalFormulaID())
			endFormula = true;

		if ((nextNode == null && prevFormulaID > 0 && prevFormulaID == node.getInternalFormulaID())
				|| (nextNode != null && nextNode.getInternalFormulaID() != node.getInternalFormulaID()
						&& prevFormulaID == node.getInternalFormulaID()))
			printEnd = true;

		// if we printed the log level string by the urls on top, otherwise we
		// put it on the first conditionsnode
		boolean printedLogLevel = false;

		if (formulaStart && node.mURLs.size() > 0) {
			printedLogLevel = true;
			stream.println("<tr class='headertr'><td><p><b>" + node.getLogLevel().toString() + "</b></p></td>");

			stream.println("<td align='center'><b>Related URLs</b>");
			if (!pipedContent)
				stream.println("</td><td><ul>");
			else // we keep it all in one td so as to not crunch the piped table
				stream.println("<ul>");

			for (int u = 0; u < node.mURLs.size(); u++) {
				stream.println("<li>" + node.mURLs.get(u).mDescription + "<a href=\"" + node.mURLs.get(u).mURL + "\">"
						+ node.mURLs.get(u).mURL + "</a>");
			}

			if (!pipedContent)
				stream.println("</ul></td></tr>");
			else
				stream.println("</ul>");
		}

		if (node.getCondenseCount() > 1)
			dispName += ", Count: " + node.getCondenseCount();

		if (formulaStart || !pipedContent)
			stream.println("<tr>");

		// if we didn't print the log level with the URLs line do it here
		// instead
		if (formulaStart && !printedLogLevel)
			stream.println("<td><p><b>" + node.getLogLevel().toString() + "</b></p></td>");
		else if (formulaStart || !pipedContent)
			stream.println("<td></td>");

		if (formulaStart || !pipedContent)
			stream.println("<td align='center'>");

		if (dispName.contains("||") || dispMsg.contains("||")) {
			if (formulaStart) {
				if (dispMsg.length() > 0 && dispName.length() > 0) {
					// we only list the first formula result in the header as a general example of
					// what the table displays
					stream.println("<table border='1'><tr class='headertr'><td><b>" + dispName + "</b></td></tr>");
					dispName = "";
				}

				stream.println("<table border='1'><tr class='headertr'>");
				for (int i = 0; i < node.getMappedConditions().size(); i++) {
					for (Map.Entry<String, MappedCondition> entry : node.getMappedConditions().entrySet()) {
						MappedCondition mc = (MappedCondition) entry.getValue();
						if (mc.MappedConditionPosition == i) {
							stream.println("<td><b>" + mc.MappedConditionNameOriginalCase + "</b></td>");
							break;
						}
					}
				}
				stream.println("</tr>");
			} else {
				// we only list the first formula result in the header as a general example of
				// what the table displays
				if (dispMsg.length() > 0 && dispName.length() > 0)
					dispName = "";
			}

			dispName = TranslatePipeStringToHtmlTable(node, dispName);
			dispMsg = TranslatePipeStringToHtmlTable(node, dispMsg);
		}

		if (node.getPopupFileName().length() > 0) {
			stream.println("<a target=\"wind" + Math.random() + "\" href=\"" + node.getPopupFileName() + "?"
					+ node.getURIAttributes() + "')\">| Details |</a> ");
		}

		if ((pipedContent && dispName.length() > 0) || !pipedContent)
			stream.println("<b>" + dispName + "</b></td>");

		if (dispMsg.length() > 0) {
			if (!pipedContent)
				stream.println("<td><p>" + dispMsg + "</p></td></tr>");
			else
				stream.println(dispMsg);
		}

		if (printConditions == PRINT_MET_CONDITIONS.SHOWALL
				|| (!node.isOmitPrintedConditions() && printConditions == PRINT_MET_CONDITIONS.HIDEDEFAULT)) {
			stream.println("<tr><td></td><td align='center'><b>Matched Conditions</b></td><td><p>");
			for (int m = 0; m < node.matchedConditions.size(); m++)
				stream.println("" + node.matchedConditions.get(m) + "<br>");

			stream.println("</p></td></tr><tr><td></td></tr>");
		}

		if (endFormula) {
			if (pipedContent) {
				stream.println("</table>");
			}
			if (sumValue != 0.0) {
				DecimalFormat formatter = new DecimalFormat("0.000");
				String sumTotal = formatter.format(sumValue);
				stream.println("<tr><td></td><td align='center'><b>Sum of " + node.getSumCondition() + " is " + sumTotal
						+ "</b></td>");
				stream.println("<td><ul><p></td></tr>");
			}
		}

		if (printEnd) {
			stream.println("<tr><td></td><td><a href=\"#div" + node.getInternalFormulaID()
					+ "\">Go to Formula Top</a></td></tr>");
		}

		return pipedContent;
	}

	private static String TranslatePipeStringToHtmlTable(ConditionsNode node, String message) {
		if (message.contains("||")) {
			message = message.replace("||", "</td><td>");

			message = "<tr><td>" + message + "</td></tr>";
		}

		return message;
	}

	private static String TranslatePipeStringToTextTable(ConditionsNode node, String message) {
		return message.replace("||", "\t");
	}

	private static void CreateSubFile(AnalyticsProcessor analytics, String dirFile, String popupFile) {
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

	private static void ReadFileToStream(AnalyticsProcessor analytics, PrintStream stream, String filename) {
		BufferedReader input = null;

		try {
			input = new BufferedReader(
					new InputStreamReader(analytics.getClass().getClassLoader().getResourceAsStream(filename)));
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

	private static ArrayList<ConditionsNode> reorderMetConditions(AnalyticsProcessor analytics, String outFile,
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
