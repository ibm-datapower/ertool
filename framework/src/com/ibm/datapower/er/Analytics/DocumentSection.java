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

import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class DocumentSection {
	public DocumentSection(Document doc, String cidName) {
		mCidDoc = doc;
		// need to remove the tags so it shows up in HTML
		mOrigCidName = cidName;
		mCidName = cidName.replace("<", "[").replace(">", "]");

		NodeList nl = doc.getElementsByTagName("Root");
		if (nl.getLength() == 0)
			mIsXML = true;
	}

	public void SetInputStream(InputStream stream) {
		mInputStream = stream;
	}

	public InputStream GetInputStream() {
		return mInputStream;
	}

	public Document GetDocument() {
		return mCidDoc;
	}

	public String GetSectionName() {
		return mCidName;
	}

	public String GetOriginalSectionName() {
		return mOrigCidName;
	}

	public boolean IsXMLSection() {
		return mIsXML;
	}

	public int GetCacheHits() {
		return mCacheHits;
	}

	public void HitCache() {
		mCacheHits++;
	}

	private Document mCidDoc = null;
	private String mCidName = "";
	private String mOrigCidName = "";
	private InputStream mInputStream = null;
	private boolean mIsXML = false;
	private int mCacheHits = 0;
}