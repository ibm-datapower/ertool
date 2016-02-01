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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.datapower.er.Analytics.ConditionsNode.LogLevelType;
import com.ibm.datapower.er.Analytics.Structure.Formula;
import com.ibm.datapower.er.Analytics.Structure.RunFormula;

public class AnalyticsFunctions {

	public static ConditionsNode cloneNode(ConditionsNode clone,
			RunFormula formula) {
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
		if (formula.bIsSectionVariable
				&& node.getCondition("SectionName") == null) {
			String sectionName = generateFileFromContent(formula.documentSet);
			node.addCondition("sectionname",
					formula.documentSet.GetSectionName());
		}

		SetupNode(node, formula);
		return node;
	}

	public static ConditionsNode createResultNode(RunFormula formula,
			ConditionsNode cloneNode, int curPos) {
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

	public static ConditionsNode determineNode(RunFormula formula,
			ConditionsNode cloneNode, int curPos, int fieldPos) {
		ConditionsNode node = null;
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
		if (formula.bIsSectionVariable
				&& node.getCondition("SectionName") == null) {
			String sectionName = generateFileFromContent(formula.documentSet);
			node.addCondition("sectionname",
					formula.documentSet.GetSectionName());
		}

		SetupNode(node, formula);
		return node;
	}


	public static String generateFileFromContent(DocumentSection section) {
		String cidName = section.GetSectionName().replace("[", "")
				.replace("]", "");

		String output = cidName;
		
		File file = new File(AnalyticsProcessor.outputFileName);
		File parentDir = file.getParentFile(); // get parent dir

		String dir = "";

		if (parentDir != null)
			dir = parentDir.getPath();

		if (dir.contains(":\\"))
			dir += "\\" + AnalyticsProcessor.GENERATED_FILES_DIR + "\\";
		else if (dir.length() > 0)
			dir += "/" + AnalyticsProcessor.GENERATED_FILES_DIR + "/";

		String ext = "";

		if (section.IsXMLSection())
			ext = ".xml";

		if (dir.length() > 0) {
			File dstFile = null;

			String endFileName = parseFileNameFromCid(cidName, dir, ext);

			dstFile = new File(dir + endFileName);

			String newFileName = "<a href=\""
					+ AnalyticsProcessor.GENERATED_FILES_DIR + "/"
					+ endFileName + "\">" + section.GetSectionName() + "</a>";
			output = newFileName;
			if (!dstFile.exists()) {
				NodeList nl = section.GetDocument()
						.getElementsByTagName("Root");
				if (nl.getLength() > 0) {
					try {
						FileOutputStream fso = new FileOutputStream(dstFile);
						byte[] data = nl.item(0).getTextContent().getBytes();
						fso.write(data, 0, data.length);
						fso.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						Transformer transformer = TransformerFactory
								.newInstance().newTransformer();
						Result outData = new StreamResult(dstFile);
						Source input = new DOMSource(section.GetDocument());

						transformer.transform(input, outData);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						output = section.GetSectionName();
						e.printStackTrace();
					}
				}
			}
		}

		return output;
	}

	public static String parseFileNameFromCid(String cidName, String dir,
			String optExt) {
		String sectionName = cidName.replace("<", "").replace(">", "");

		String endFileName = sectionName;
		if (sectionName.contains("@")) {
			endFileName = sectionName.substring(0, sectionName.indexOf("@"))
					+ optExt;

			File dirMk = new File(dir);
			if (!dirMk.exists())
				dirMk.mkdirs();
		} else {
			if (sectionName.contains(".gz"))
				sectionName = sectionName.replace(".gz", ".txt");

			endFileName = sectionName + optExt;

			if (sectionName.contains("/")) {
				String dirAddition = sectionName.substring(0,
						sectionName.lastIndexOf("/"));
				File dirMk = new File(dir + dirAddition);
				if (!dirMk.exists())
					dirMk.mkdirs();
			}
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

	public static String getAttributeByTag(String sTag, String attributeName,
			Element eElement, int nodeID) {

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
		SetupNode(node, formula.getFormula(), (String) formula.getFormula()
				.getItem("DisplayMessage").getObject(), formula.formulaPos);
	}

	public static void SetupNode(ConditionsNode node, Formula formula,
			String dispMessage, int formulaPos) {
		node.setDisplayName((String) formula.getItem("Name").getObject());
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
		String conditionFieldPosition = getAttributeByName(condNode,
				"FieldPosition");

		// used as the value position parsed from FieldPosition
		int pos = 0;

		// determine the field position specified as an attribute in the
		// Condition element (FieldPosition)
		try {
			pos = Integer.parseInt(conditionFieldPosition);
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
		String conditionNextOperation = getAttributeByName(condNode,
				"NextOperation");

		// conversion operation
		String conversionType = getAttributeByName(condNode, "Conversion")
				.toLowerCase();

		ConditionField field = new ConditionField(pos, conditionFieldPosition,
				regGroupValue, conditionName, conditionOperation,
				conditionValue, conditionRegEXP, conditionNextOperation,
				conversionType);

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
	public static void populatePassConditionNode(ConditionsNode node,
			String logLevelLwr, Formula formula,
			ArrayList<ConditionsNode> formulasMet,
			ArrayList<ConditionsNode> othersMet) {

		boolean topPositionRes = (boolean) formula.getItem("TopPosition")
				.getObject();
		NodeList urlNodes = (NodeList) formula.getItem("UrlNodes").getObject();
		boolean collapseResult = (boolean) formula.getItem("CollapseResult")
				.getObject();
		String categories = (String) formula.getItem("Categories").getObject();
		String popup = (String) formula.getItem("Popup").getObject();
		String sortConditionName = (String) formula.getItem("SortCondition")
				.getObject();
		String sortMethod = (String) formula.getItem("SortMethod").getObject();
		String sortOption = (String) formula.getItem("SortOption").getObject();
		String sumCondition = (String) formula.getItem("SumCondition")
				.getObject();
		String condenseCondition = (String) formula
				.getItem("CondenseCondition").getObject();

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
				String urlDesc = AnalyticsFunctions.getAttributeByName(urlNode,
						"description");
				String matchID = AnalyticsFunctions.getAttributeByName(urlNode,
						"FormulaIDMatch");

				// see if we want a previously matched formula before showing
				// this url
				if (matchID.length() > 0 && formulasMet != null) {
					if ((!isFormulaMatched(matchID, formulasMet) && othersMet == null)
							|| (othersMet != null && !isFormulaMatched(matchID,
									othersMet))) {
						continue; // we didn't pass the previous criteria, skip
					}
				}
				node.mURLs.add(new ReferenceURL(urlDesc, urlNode
						.getTextContent()));
			}
		}
	}

	public static boolean isFormulaMatched(String formulaID,
			ArrayList<ConditionsNode> matchedFormulas) {
		if (formulaID.length() < 1)
			return false;

		for (int z = 0; z < matchedFormulas.size(); z++) {
			ConditionsNode curNode = (ConditionsNode) matchedFormulas.get(z);

			if (curNode.getFormulaID().length() > 0
					&& curNode.getFormulaID().equals(formulaID))
				return true;
		}

		return false;
	}

	public static ArrayList<ConditionsNode> condenseConditions(
			ArrayList<ConditionsNode> tmpConditionMetList) {
		HashMap<String, ConditionsNode> results = new HashMap<String, ConditionsNode>();

		for (int i = 0; i < tmpConditionMetList.size(); i++) {
			ConditionsNode node = tmpConditionMetList.get(i);
			if (node.getCondenseConditionName().length() < 1)
				return tmpConditionMetList;
			else {
				String condValue = node.getCondition(node
						.getCondenseConditionName());
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

	public static ConditionsNode findNodeByValue(
			Map<String, ConditionsNode> tmpConditionMetList,
			String conditionValue) {
		Object obj = tmpConditionMetList.get(conditionValue);
		return (ConditionsNode) obj;
	}

}
