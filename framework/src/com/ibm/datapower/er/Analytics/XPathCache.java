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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XPathCache {
	public XPathCache(NodeList xpathList, Document cidDoc, String xpath)
	{
		cacheXPathExpList = xpathList;
		cacheXPathDoc = cidDoc;
		cacheXPathQuery = xpath;
	}
	
	public NodeList getNodeList() { return cacheXPathExpList; }
	public Document getCidDoc() { return cacheXPathDoc; }
	public String getXPathQuery() { return cacheXPathQuery; }
	private NodeList cacheXPathExpList = null;
	private Document cacheXPathDoc = null;
	private String cacheXPathQuery = "";
	
}
