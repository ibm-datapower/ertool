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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TransxXML {

	public static void writeTransactionsToXml(
			ParseTransx parsex, ArrayList<Transaction> transactions, String dir) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document doc = builder.newDocument();
			
			// base level 'Transactions'
			Element root = doc.createElement("Transactions");
			doc.appendChild(root);

			for (int i = 0; i < transactions.size(); i++) {
				Transaction transx = transactions.get(i);
				// sub-element Transaction
				Element transaction = doc.createElement("Transaction");

				// Transaction attributes (hardcoded)
				createAttribute(doc, transaction, "StartTime",
						transx.getStartTime());
				createAttribute(doc, transaction, "EndTime",
						transx.getEndTime());
				createAttribute(doc, transaction, "TransactionID",
						transx.getTransID());
				createAttribute(doc, transaction, "LogLevel",
						transx.getLogLevel());
				createAttribute(doc, transaction, "LogType",
						transx.getLogType());

				// Message level in Transactions
				for (int m = 0; m < transx.mMessages.size(); m++) {
					Element message = doc.createElement("Message");
					LogMessage msg = transx.mMessages.get(m);
					
					// pull out the HashMap we created in the Transaction and parse it out as attributes
					Iterator it = msg.mLogMsg.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();

						createAttribute(doc, message, pair);
					}
					// add each message to the transaction we are currently parsing
					transaction.appendChild(message);
				}
				// add the transaction to the Transactions list
				root.appendChild(transaction);
			}

			TransformerFactory xformFactory = TransformerFactory.newInstance();
			Transformer xform = xformFactory.newTransformer();
			DOMSource src = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(dir + "transactions.xml"));
			xform.transform(src, result);
			} catch (Exception ex) {

		}
	}

	private static void ReadFileToStream(ParseTransx parse,
			PrintStream stream, String filename) {
		BufferedReader input = null;

		try {
			input = new BufferedReader(new InputStreamReader(parse
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
	
	public static void createAttribute(Document doc, Element element,
			Map.Entry pair) {
		Attr keyattr = doc.createAttribute((String) pair.getKey());
		keyattr.setValue((String) pair.getValue());
		element.setAttributeNode(keyattr);
	}

	public static void createAttribute(Document doc, Element element,
			String key, String value) {
		Attr keyattr = doc.createAttribute(key);
		keyattr.setValue(value);
		element.setAttributeNode(keyattr);
	}
}
