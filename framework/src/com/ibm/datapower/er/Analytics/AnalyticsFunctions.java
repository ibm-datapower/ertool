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

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.datapower.er.ERFramework;
import com.ibm.datapower.er.Analytics.ConditionsNode.LogLevelType;
import com.ibm.datapower.er.Analytics.MappedCondition.MAPPED_TABLE_POSITION;
import com.ibm.datapower.er.Analytics.Structure.Formula;
import com.ibm.datapower.er.Analytics.Structure.RunFormula;

public class AnalyticsFunctions {

	public static ConditionsNode cloneNode(ConditionsNode clone, RunFormula formula) {
		ConditionsNode node;
		try {
			node = (ConditionsNode) clone.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// pass the message
		// 'header'
		// and 'body'. These
		// will be
		// parsed as conditions
		// are
		// met to pull xpath
		// arguments.
		if (formula.bIsSectionVariable && node.getCondition("SectionName") == null) {
			String sectionName = generateFileFromContent(formula.documentSet);
			setupNodeVariables(formula, node, sectionName);
		}

		SetupNode(node, formula);
		return node;
	}

	public static ConditionsNode createResultNode(RunFormula formula, ConditionsNode cloneNode, int curPos) {
		ConditionsNode node = null;
		if (cloneNode != null) {
			try {
				node = (ConditionsNode) cloneNode.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			node = instantiateNode(formula);
		}

		// add to the current condNodes, this is stored
		// for all expressions in a formula
		if (node != null)
			formula.condNodes.add(curPos, node);

		return node;
	}

	public static ConditionsNode determineNode(RunFormula formula, ConditionsNode cloneNode, int curPos, int fieldPos) {
		ConditionsNode node = null;
		boolean customFilePull = formula.requiredFile.length() > 0 ? true : false;
		// find if we have a matching node for this position in the
		// parsed xml section from the error report
		try {
			node = formula.condNodes.get(curPos);
		} catch (Exception ex) {
			node = null;
		}

		// no node exists so instantiate a new one to track what
		// conditions match to this original section element
		if (node == null) {
			// if we are pulling a specific file we do not instantiate new nodes, we use existing nodes to match additional field conditions
			if ( customFilePull )
				return null;
			
			node = createResultNode(formula, cloneNode, curPos);
		} else if (fieldPos == 0) {
			// we are starting over a new iteration and this is an
			// existing node, reset the conditions
			// we have a new set of conditions in another expression
			// to match for it to be a successful hit
			node.setConditionsMet(0);
			node.setConditionFound(false);
		}
		return node;
	}

	public static ConditionsNode instantiateNode(RunFormula formula) {
		ConditionsNode node = new ConditionsNode();
		// pass the message
		// 'header'
		// and 'body'. These
		// will be
		// parsed as conditions
		// are
		// met to pull xpath
		// arguments.
		if (formula.bIsSectionVariable && node.getCondition("SectionName") == null) {
			String sectionName = generateFileFromContent(formula.documentSet);
			setupNodeVariables(formula, node, sectionName);
		}

		SetupNode(node, formula);
		return node;
	}

	public static String generateFileFromContent(DocumentSection section) {
		String cidName = section.GetSectionName().replace("[", "").replace("]", "");

		String sectionName = cidName;
		if (sectionName.contains("@datapower.ibm.com")) {
			sectionName = sectionName.replace("@datapower.ibm.com", "");
		}

		String output = sectionName;

		File file = new File(AnalyticsProcessor.outputFileName);
		File parentDir = file.getParentFile(); // get parent dir

		String dir = "";

		if (parentDir != null)
			dir = parentDir.getPath();

		dir = AnalyticsFunctions.buildDirectoryString(section.GetFramework(), dir, section.GetPhase());

		String ext = "";

		if (section.IsXMLSection())
			ext = ".xml";

		if (dir.length() > 0) {
			File dstFile = null;

			String endFileName = parseFileNameFromCid(cidName, dir, ext, 0);

			dstFile = new File(dir + endFileName);

			String newFileName = "";

			String subDir = buildSubDirectoryString(section.GetFramework(), section.GetPhase());

			if (subDir.length() < 1) {
				newFileName = "<a href=\"" + AnalyticsProcessor.GENERATED_FILES_DIR + "/" + endFileName + "\">"
						+ sectionName + "</a>";
			} else {
				newFileName = "<a href=\"" + AnalyticsProcessor.GENERATED_FILES_DIR + "/" + subDir + "/" + endFileName
						+ "\">" + sectionName + "</a>";
			}
			
			output = newFileName;
			
			if (!dstFile.exists()) {
				NodeList nl = null;

				synchronized (ERFramework.mDocBuilderFactory) {
					nl = section.GetDocument().getElementsByTagName("Root");
				}
				if (nl != null && nl.getLength() > 0) {
					try {
						int bufferSize = 512 * 1024;
						OutputStream streamOut = new BufferedOutputStream(
								Files.newOutputStream(dstFile.toPath(), StandardOpenOption.CREATE_NEW,
										StandardOpenOption.WRITE),
						                          bufferSize);
						byte[] data = nl.item(0).getTextContent().getBytes();
						streamOut.write(data, 0, data.length);
						streamOut.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						File dirs = new File(dir);
						dirs.mkdirs();
					} catch (Exception ex) {
					}
					try {
						Transformer transformer = TransformerFactory.newInstance().newTransformer();
						Result outData = new StreamResult(dstFile);

						synchronized (ERFramework.mDocBuilderFactory) {
							Source input = new DOMSource(section.GetDocument());
							transformer.transform(input, outData);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						output = cidName;
						e.printStackTrace();
					}
				}
			}
		}

		return output;
	}

	public static String parseFileNameFromCid(String cidName, String dir, String optExt, int enum_) {
		String sectionName = cidName.replace("<", "").replace(">", "");

		String actualExt = optExt;
		if ( enum_ > 0 )
		{
			actualExt =  optExt.replace("." + enum_, "");
		}
		
		String endFileName = sectionName;
		if (sectionName.contains("@")) {
				endFileName = sectionName.substring(0, sectionName.indexOf("@"));

				if(!endFileName.endsWith(actualExt)) {
					endFileName += actualExt;
				}

			File dirMk = new File(dir);
			if (!dirMk.exists())
				dirMk.mkdirs();
		} else {
			if (sectionName.contains(".gz"))
				sectionName = sectionName.replace(".gz", ".txt");

			if(!sectionName.endsWith(actualExt)) {
				endFileName = sectionName + actualExt;
			}
			else {
				endFileName = sectionName;
			}

			if (sectionName.contains("/")) {
				String dirAddition = sectionName.substring(0, sectionName.lastIndexOf("/"));
				File dirMk = new File(dir + dirAddition);
				if (!dirMk.exists())
					dirMk.mkdirs();
			}
		}
		
		if(endFileName.matches("[a-zA-Z]{1}\\:.*")) {
			endFileName = endFileName.substring(endFileName.lastIndexOf("\\") + 1);
		}
		else if(endFileName.startsWith("/")) {
			endFileName = endFileName.substring(endFileName.lastIndexOf("/") + 1);
		}

		return endFileName;
	}

	public static String getAttributeByName(Node node, String attributeName) {

		NamedNodeMap map = node.getAttributes();
		Node attrib = map.getNamedItem(attributeName);

		// if the attribute does not exist lets not crash, try/catch instead.
		String attribValue = "";

		try {
			attribValue = attrib.getNodeValue();
		} catch (Exception ex) {
		}

		return attribValue;
	}

	public static String getAttributeByTag(String sTag, String attributeName, Element eElement, int nodeID) {

		// if the attribute does not exist lets not crash, try/catch instead.
		String attribValue = "";

		try {
			NodeList nlList = eElement.getElementsByTagName(sTag);
			Node nValue = (Node) nlList.item(nodeID);
			NamedNodeMap map = nValue.getAttributes();
			Node attrib = map.getNamedItem(attributeName);

			attribValue = attrib.getNodeValue();
		} catch (Exception ex) {
		}

		return attribValue;
	}

	public static void SetupNode(ConditionsNode node, RunFormula formula) {
		SetupNode(node, formula, (String) formula.getFormula().getItem("DisplayMessage").getObject(),
				formula.formulaPos, null);
	}

	public static void SetupNode(ConditionsNode node, RunFormula formula, String dispMessage, int formulaPos,
			ERFramework framework) {

		String baseMsg = (String) formula.getFormula().getItem("Name").getObject();
		if (formula.getFormula().mMultiDocs && framework != null) {
			baseMsg = baseMsg.replace("{Condition:ReportFile} - ", "");
			int id = framework.GetID();

			if (formula.documentSet.GetPhase() > 0) {
				id = formula.documentSet.GetPhase();
			}

			String dispName = "ReportFile-" + id;
			baseMsg = dispName + " " + baseMsg;
		}

		node.setDisplayName(baseMsg);
		node.setDisplayMessage(dispMessage);
		node.setInternalFormulaID(formulaPos);
		node.setFormulaID((String) formula.getFormula().getItem("FormulaID").getObject());
	}

	public static void SetupNode(ConditionsNode node, Formula formula, String dispMessage, int formulaPos,
			ERFramework framework, int phase) {

		String baseMsg = (String) formula.getItem("Name").getObject();
		if (formula.mMultiDocs && framework != null) {
			baseMsg = baseMsg.replace("{Condition:ReportFile} - ", "");
			int id = framework.GetID();

			if (phase > 0) {
				id = phase;
			}

			String dispName = "ReportFile-" + id;
			baseMsg = dispName + " " + baseMsg;
		}

		node.setDisplayName(baseMsg);
		node.setDisplayMessage(dispMessage);
		node.setInternalFormulaID(formulaPos);
		node.setFormulaID((String) formula.getItem("FormulaID").getObject());
	}

	public static ConditionField getConditionField(Node condNode) {
		// this is the value that we want to compare against in the
		// error report (XPath) value
		String conditionValue = getAttributeByName(condNode, "Value");

		// this is the operation type that we want to perform (is our
		// xpath value greaterthan, lessthan, etc)
		String conditionOperation = getAttributeByName(condNode, "Operation");

		// this is the position against regular expression, RegExp of *
		// means we just always use all of the value
		String conditionFieldPosition = getAttributeByName(condNode, "FieldPosition");

		// used as an alternative to FieldPosition to set alternative input
		String inValueSetting = getAttributeByName(condNode, "InValue");

		// used as the value position parsed from FieldPosition
		int pos = 0;

		// determine the field position specified as an attribute in the
		// Condition element (FieldPosition)
		boolean parsedField = false;
		try {
			pos = Integer.parseInt(conditionFieldPosition);
			parsedField = true;
		} catch (Exception ex) {
		}

		String conditionName = getAttributeByName(condNode, "ConditionName");

		String regGroupValue = getAttributeByName(condNode, "RegGroup");

		// This is the regular expression we use to break apart the
		// value in the error report (from xPath)
		String conditionRegEXP = condNode.getTextContent();

		// this is for determining whether the next condition must be
		// met (AND) or if otherwise it is optional (OR)
		// in the case of an OR statement if the first condition fails
		// we can try the second if one is available
		String conditionNextOperation = getAttributeByName(condNode, "NextOperation");

		// conversion operation
		String conversionType = getAttributeByName(condNode, "Conversion").toLowerCase();

		int defaultMapPosition = MAPPED_TABLE_POSITION.AUTO_SET.getType();
		// conversion operation
		String mappedPosition = getAttributeByName(condNode, "TableMapPosition").toLowerCase();
		try {
			defaultMapPosition = Integer.parseInt(mappedPosition);
		} catch (Exception ex) {
		}
		
		ConditionField field = new ConditionField(pos, conditionFieldPosition, regGroupValue, conditionName,
				conditionOperation, conditionValue, conditionRegEXP, conditionNextOperation, conversionType,
				inValueSetting, defaultMapPosition);

		return field;
	}

	/**
	 * populatePassConditionNode() If a ConditionsNode meets matches required
	 * for condition elements and expression elements then this will populate
	 * the log level and the urls related
	 * 
	 * @param node
	 *            ConditionsNode - node which passed the arguments inside
	 *            analytics
	 * @param logLevelLwr
	 *            String - log level string (in lower case) for setting the enum
	 * @param topPositionRes
	 *            boolean - set to true if we want these met conditions to be at
	 *            the top of the results
	 * @param urlNodes
	 *            NodeList - URLs that give more detail to the problem
	 * @param collapseResult
	 *            boolean - collapses the results in HTML by default (meant for
	 *            long lists)
	 * @param categories
	 *            String - gives the list of categories this matches against
	 * @param popup
	 *            String - custom popups filename (in the src/Analytics dir) to
	 *            create details of results
	 * @param sortConditionName
	 *            String - this lets us determine what we should sort all the
	 *            result nodes by
	 * @param sortMethod
	 *            String - which way we should order ascending or descending
	 * @param sortOption
	 *            String - this is the formatting we use to determine sorting
	 *            (right now timestamp formatting)
	 * @return void
	 */
	public static void populatePassConditionNode(ConditionsNode node, String logLevelLwr, Formula formula,
			ArrayList<ConditionsNode> formulasMet, ArrayList<ConditionsNode> othersMet) {

		boolean topPositionRes = (boolean) formula.getItem("TopPosition").getObject();
		NodeList urlNodes = (NodeList) formula.getItem("UrlNodes").getObject();
		boolean collapseResult = (boolean) formula.getItem("CollapseResult").getObject();
		String categories = (String) formula.getItem("Categories").getObject();
		String popup = (String) formula.getItem("Popup").getObject();
		String sortConditionName = (String) formula.getItem("SortCondition").getObject();
		String sortMethod = (String) formula.getItem("SortMethod").getObject();
		String sortOption = (String) formula.getItem("SortOption").getObject();
		String sumCondition = (String) formula.getItem("SumCondition").getObject();
		String condenseCondition = (String) formula.getItem("CondenseCondition").getObject();

		if (logLevelLwr.equals("warning"))
			node.setLogLevel(LogLevelType.WARNING);
		else if (logLevelLwr.equals("error"))
			node.setLogLevel(LogLevelType.ERROR);
		else if (logLevelLwr.equals("critical"))
			node.setLogLevel(LogLevelType.CRITICAL);

		// used to determine if we want to override its position when we print
		// results
		if (topPositionRes)
			node.setTopCondition(topPositionRes);

		node.setCollapseSet(collapseResult);

		node.setCategories(categories);

		node.setCondenseConditionName(condenseCondition);

		node.setSortConditionName(sortConditionName);

		node.setPopupFileName(popup);

		if (sortMethod.length() > 0)
			node.setSortMethod(sortMethod);

		node.setSortOption(sortOption);

		node.setSumCondition(sumCondition);

		if (node.mURLs.size() < 1) {
			// supply urls into the node since we passed the formula
			for (int u = 0; u < urlNodes.getLength(); u++) {
				Node urlNode = urlNodes.item(u);
				String urlDesc = AnalyticsFunctions.getAttributeByName(urlNode, "description");
				String matchID = AnalyticsFunctions.getAttributeByName(urlNode, "FormulaIDMatch");

				// see if we want a previously matched formula before showing
				// this url
				if (matchID.length() > 0 && formulasMet != null) {
					if ((!isFormulaMatched(matchID, formulasMet) && othersMet == null)
							|| (othersMet != null && !isFormulaMatched(matchID, othersMet))) {
						continue; // we didn't pass the previous criteria, skip
					}
				}
				node.mURLs.add(new ReferenceURL(urlDesc, urlNode.getTextContent()));
			}
		}
	}

	public static boolean isFormulaMatched(String formulaID, ArrayList<ConditionsNode> matchedFormulas) {
		if (formulaID.length() < 1)
			return false;
		
		boolean oppositeFormulaCheck = false;
		
		// if we don't match the intended formula return true since we wan't to do opposite boolean check (!)
		if ( formulaID.startsWith("!") )
		{
			oppositeFormulaCheck = true;
			formulaID = formulaID.substring(1);
		}

		for (int z = 0; z < matchedFormulas.size(); z++) {
			ConditionsNode curNode = (ConditionsNode) matchedFormulas.get(z);

			if (curNode.getFormulaID().length() > 0 && curNode.getFormulaID().equals(formulaID))
			{
				if ( oppositeFormulaCheck )
					return false;
				else
					return true;
			}
		}
		
		if ( oppositeFormulaCheck )
			return true;

		return false;
	}

	public static ArrayList<ConditionsNode> condenseConditions(ArrayList<ConditionsNode> tmpConditionMetList) {
		HashMap<String, ConditionsNode> results = new HashMap<String, ConditionsNode>();

		for (int i = 0; i < tmpConditionMetList.size(); i++) {
			ConditionsNode node = tmpConditionMetList.get(i);
			if (node.getCondenseConditionName().length() < 1)
				return tmpConditionMetList;
			else {
				String condValue = node.getCondition(node.getCondenseConditionName());
				ConditionsNode prevNode = findNodeByValue(results, condValue);
				if (prevNode == null) {
					node.setCondenseCount(1);
					results.put(condValue, node);
				} else
					prevNode.setCondenseCount(prevNode.getCondenseCount() + 1);
			}
		}

		ArrayList<ConditionsNode> newList = new ArrayList<ConditionsNode>();
		newList.addAll(results.values());

		return newList;
	}

	public static ConditionsNode findNodeByValue(Map<String, ConditionsNode> tmpConditionMetList,
			String conditionValue) {
		Object obj = tmpConditionMetList.get(conditionValue);
		return (ConditionsNode) obj;
	}

	public static void setupNodeVariables(ERFramework framework, ConditionsNode node, String sectionName, String fileName) {
		String reportFile = "";
		if (sectionName.contains("@datapower.ibm.com")) {
			sectionName = sectionName.replace("@datapower.ibm.com", "");
		}
		if (framework.GetID() > 0) {
			reportFile = "ReportFile" + framework.GetID();
		}
		node.addCondition("SectionName", sectionName, MAPPED_TABLE_POSITION.NO_SET.getType());
		node.addCondition("ReportFile", reportFile, MAPPED_TABLE_POSITION.NO_SET.getType());
		node.addCondition("ReportFileName", fileName, MAPPED_TABLE_POSITION.NO_SET.getType());
	}
	
	public static void setupNodeVariables(RunFormula formula, ConditionsNode node, String sectionName) {
		setupNodeVariables(formula.documentSet.GetFramework(),node,sectionName,formula.documentSet.GetPhaseFileName());
	}

	public static String buildSubDirectoryString(ERFramework mFramework, int phase) {
		String subDir = "";
		if (mFramework.getFileLocation().length() > 0) {
			String fileLoc = mFramework.getFileLocation();

			boolean backSlash = false;

			int idx = fileLoc.lastIndexOf("/");
			if (idx < 0) {
				idx = fileLoc.lastIndexOf("\\");
				backSlash = true;
			}

			if (idx > 0 && (idx + 1) < fileLoc.length())
				subDir = fileLoc.substring(idx + 1);

			if (phase > 0) {
				if (backSlash)
					subDir += "\\" + phase;
				else
					subDir += "/" + phase;
			}
		}

		return subDir;
	}

	public static String buildDirectoryString(ERFramework mFramework, String dir, int phase) {
		String subDir = buildSubDirectoryString(mFramework, phase);

		if (subDir.length() < 1) {
			if (dir.contains(":\\"))
				dir += "\\" + AnalyticsProcessor.GENERATED_FILES_DIR + "\\";
			else if (dir.length() > 0)
				dir += "/" + AnalyticsProcessor.GENERATED_FILES_DIR + "/";
		} else {
			if (dir.contains(":\\"))
				dir += "\\" + AnalyticsProcessor.GENERATED_FILES_DIR + "\\" + subDir + "\\";
			else if (dir.length() > 0)
				dir += "/" + AnalyticsProcessor.GENERATED_FILES_DIR + "/" + subDir + "/";
		}

		return dir;
	}
	
	public static NodeList retrieveNodeListFromSingleVal(XPathExpression expr, XPath xpath, String xPathQuery,
			DocumentSection section, String errMsgFromNodeListRetr) {
		NodeList resultList = null;
		try {
			if (expr == null)
				expr = (XPathExpression) xpath.compile(xPathQuery);

			synchronized (ERFramework.mDocBuilderFactory) {
				if (errMsgFromNodeListRetr.contains("#NUMBER")) {
					Number resNum = (Number) expr.evaluate(section.GetDocument(), XPathConstants.NUMBER);
					resultList = AnalyticsFunctions.makeNodeListTextNode(section, resNum.toString());
				}
				else if (errMsgFromNodeListRetr.contains("#STRING")) {
					String resStr = (String) expr.evaluate(section.GetDocument(), XPathConstants.STRING);
					resultList = AnalyticsFunctions.makeNodeListTextNode(section, resStr);
				}
				else if (errMsgFromNodeListRetr.contains("#BOOLEAN")) {
					Boolean resBool = (Boolean) expr.evaluate(section.GetDocument(), XPathConstants.BOOLEAN);
					resultList = AnalyticsFunctions.makeNodeListTextNode(section, resBool.toString());
				}
			}
		} catch (Exception ie) {
			ie.printStackTrace();
		}
		return resultList;
	}
	
	public static NodeList makeNodeListTextNode(DocumentSection section, String nodeValue)
	{
		Node node = section.GetDocument().createElement("Root");
		Node node2 = section.GetDocument().createElement("Result");
		node2.setTextContent(nodeValue);
		node.appendChild(node2);
		return node.getChildNodes();
	}
}
