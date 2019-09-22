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

import com.ibm.datapower.er.ERFramework;

public class DocumentSection {
	public DocumentSection(Document doc, String cidName, String outFileExtension, ERFramework framework, int phase, 
			String phaseFileName) {
		mCidDoc = doc;
		// need to remove the tags so it shows up in HTML
		mOrigCidName = cidName;
		mCidName = cidName.replace("<", "[").replace(">", "]");

		if (outFileExtension != null)
			mOutExtension = outFileExtension;

		NodeList nl = doc.getElementsByTagName("Root");
		if (nl.getLength() == 0)
			mIsXML = true;

		mFramework = framework;

		mPhase = phase;
		
		mPhaseFileName = phaseFileName;
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

	public String GetOutExtension() {
		return mOutExtension;
	}

	public ERFramework GetFramework() {
		return mFramework;
	}

	public int GetPhase() {
		return mPhase;
	}
	
	public String GetPhaseFileName() {
		return mPhaseFileName;
	}

	private Document mCidDoc = null;
	private String mCidName = "";
	private String mOrigCidName = "";
	private boolean mIsXML = false;
	private int mCacheHits = 0;
	private String mOutExtension = "";
	private ERFramework mFramework = null;
	public int mPhase = 0;
	private String mPhaseFileName = "";
}