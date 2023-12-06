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

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

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

		String removedCidName = GetSectionName().replace("[", "").replace("]", "");

		String resSectionName = removedCidName;
		if (resSectionName.contains("@datapower.ibm.com")) {
			resSectionName = resSectionName.replace("@datapower.ibm.com", "");
		}

		mParsedCidName = resSectionName;

		if (outFileExtension != null)
			mOutExtension = outFileExtension;

		NodeList nl = doc.getElementsByTagName("Root");
		if (nl.getLength() == 0) {
			mIsXML = true;

			synchronized (ERFramework.mDocBuilderFactory) {
				Transformer transformer;
				try {
					transformer = TransformerFactory.newInstance().newTransformer();

					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					Source input = new DOMSource(doc);

					transformer.transform(input, new StreamResult(outputStream));

					byte[] bytes = outputStream.toByteArray();
					SetBytes(bytes);
				} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			byte[] data = nl.item(0).getTextContent().getBytes();
			SetBytes(data);
		}

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

	public void SetBytes(byte[] bytes) {
		mData = bytes;
	}

	public byte[] GetBytes() {
		return mData;
	}

	public String GetParsedSectionName() {
		return mParsedCidName;
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
	private String mParsedCidName = "";
	private byte[] mData = null;
}