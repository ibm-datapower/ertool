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

package com.ibm.datapower.er;

// Utility imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.io.BufferedInputStream;
// File input stream and compression imports
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// MIME content transfer encoding
import com.ibm.datapower.er.mgmt.Base64;

// XML parsing imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.CharacterData;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
// SAX like MIME parsing imports
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.parser.MimeTokenStream;
import org.apache.james.mime4j.MimeException;
import org.apache.commons.lang3.StringEscapeUtils;

// Dynamic class loading imports
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.datapower.er.Analytics.AnalyticsFunctions;
// Analytics imports (document section + sorting support)
import com.ibm.datapower.er.Analytics.DocSort;
import com.ibm.datapower.er.Analytics.DocumentSection;
import com.ibm.datapower.er.Analytics.ERMimeSection;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;


/*
 * Default constructor
 */
public class ERFramework extends ClassLoader {
	public ERFramework(int id) {
		mID = id;
		if (!ERFrameworkRun.mIsLoggerConfigured) {
		    Configurator.initialize(new DefaultConfiguration());
		    Configurator.setRootLevel(Level.INFO);
			ERFrameworkRun.mIsLoggerConfigured = true;
		}

		mFileLocation = "";
		mOriginalStream = null;
		mtStream = null;
		mLine = 0;
		msgs = ResourceBundle.getBundle("ERMessages");
		mXslFormatList = new Vector();
		mXslCidList = new Vector();
		mXslPathList = new Vector();

		// Load backtrace class or note as not present
		try {
			mBacktraceClass = loadClass("ERBacktrace");
			mBacktraceObject = mBacktraceClass.newInstance();
		} catch (ClassNotFoundException e) {
			// System.err.println(e);
			mBacktraceClass = null;
			mBacktraceObject = null;
		} catch (IllegalAccessException e) {
			// System.err.println(e);
			mBacktraceClass = null;
			mBacktraceObject = null;
		} catch (InstantiationException e) {
			// System.err.println(e);
			mBacktraceClass = null;
			mBacktraceObject = null;
		}
	}

	/**
	 * Set the location of the local error-report.tx.gz
	 * 
	 * @param file
	 *            Absolute or relative location to error-report.txt.gz
	 */
	public void setFileLocation(String file) {
		mFileLocation = file;
		EstablishHighPhase();
	}

	/**
	 * Get the location previously set
	 */
	public String getFileLocation() {
		return mFileLocation;
	}

	/**
	 * Set location of the XSL style sheet
	 * 
	 * @param format
	 *            Output format associated with stylesheet
	 * @param cid
	 *            Content ID associated with stylesheet
	 * @param path
	 *            Absolute or relative location of XSL style sheet
	 */
	public void setCidXslPath(String format, String cid, String path) {
		mXslFormatList.addElement(format);
		mXslCidList.addElement(cid);
		mXslPathList.addElement(path);
	}

	public void setCidXslPath(String path) {
		setCidXslPath("HTML", "*", path);
	}

	/**
	 * Get location of the XSL style sheet
	 * 
	 * @param format
	 *            Output format associated with stylesheet
	 * @param cid
	 *            Content ID associated with stylesheet
	 * @return Path to XSL style sheet
	 */
	public String getCidXslPath(String format, String cid) {
		String thisFormat;
		String thisCid;
		String thisPath;
		Enumeration vFormat = mXslFormatList.elements();
		Enumeration vCid = mXslCidList.elements();
		Enumeration vPath = mXslPathList.elements();
		String ret = "";
		while (vPath.hasMoreElements()) {
			thisFormat = vFormat.nextElement().toString();
			thisCid = vCid.nextElement().toString();
			thisPath = vPath.nextElement().toString();
			if (thisFormat.equals(format) && (thisCid.equals("*") || thisCid.equals(cid) || cid.equals("*")))
				ret = thisPath;
		}
		return ret;
	}

	/**
	 * Locate an ErrorReport section by content ID and return an InputStream.
	 * The first byte returned by the InputStream is the first byte of the
	 * entity data after the entity headers. InputStream sets EOF on the last
	 * byte of this entity and does not consume beyond the terminating entity
	 * boundary.
	 * 
	 * Closing the InputStream should close the file.
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to stream
	 * @return InputStream pointing to the section located by CID
	 */
	public ERMimeSection getCidAsInputStream(String cid, boolean returnMimeStream, int phase, int iter, boolean omit_decode_cache) throws ERException {
		boolean sectionFound = false;
		String sectionName = "";
		if (!omit_decode_cache) {
			Enumeration<String> en = mDecodedCache.keys();
			while (en.hasMoreElements()) {
				String key = en.nextElement();
				Pattern pattern = Pattern.compile(cid);
				Matcher matcher = pattern.matcher(key);
				if (matcher.find()) {
					ERMimeSection ers = mDecodedCache.get(key);
					if (ers != null && ers.mPhase == phase && ers.mIterator == iter) {
						try {
							ers.mInput.reset();
						} catch (IOException e) {
							continue;
						}
						return ers;
					}
				}
			}
		}

			boolean succeed = erParse(phase, false);

			if (!succeed)
				return null;

			int count = 0;
			
			boolean noFields = true;
			// run through parsed tokens
			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {
					case MimeTokenStream.T_BODY:
						if (sectionFound == true) {
							if ( count < iter )
							{
								count++;
								sectionFound = false;
								continue;
							}
							if (mContentEncoding.equalsIgnoreCase("base64")) {
								if (returnMimeStream)
									return new ERMimeSection(mtStream.getInputStream(), phase, iter, sectionName);
								else {
									InputStream streamOut = new Base64.InputStream(mtStream.getInputStream());
									return new ERMimeSection(streamOut, phase, iter, sectionName);
								}
							} else if (returnMimeStream)
								return new ERMimeSection(mtStream.getInputStream(), phase, iter, sectionName);
							else
								return new ERMimeSection(decodeBacktrace(sectionName, mtStream.getInputStream()), phase, iter, sectionName);
						}
						break;
					case MimeTokenStream.T_FIELD:
						setContentType();
						if (cid.length() > 0 && mtStream.getField().getName().equals("Content-ID")) {
							noFields = false;

							Pattern pattern = Pattern.compile(cid);
							Matcher matcher = pattern.matcher(mtStream.getField().getBody());
							if (matcher.find()) {
								sectionFound = true;
								sectionName = mtStream.getField().getBody().trim();
							}
						}
						break;
					default:
					}
				}
			} catch (IOException e) {

			} catch (MimeException e) {

			}

			if(noFields && !mIsPostMortem) {
				Pattern pattern = Pattern.compile(cid);
				Matcher matcher = pattern.matcher(mFileLocation);
				// try to read it just in as a file
				if (matcher.find()) {
					return new ERMimeSection(LoadFileStream(), phase);
				}
			}

		// section not found, return null stream
		return null;
	}

	/**
	 * Break down an error report to get the Content-ID sections and return in
	 * an ArrayList
	 * 
	 * 
	 * @param cid
	 *            Content ID for finding wildcard entries
	 * @return ArrayList<String> matched cid sections
	 */
	public ArrayList<String> getMatchesToCid(String cid) throws ERException {

		ArrayList<String> matches = new ArrayList<String>();
		for (int f = 0; f < MAX_ZIPPED_FILES; f++) {
			// parse input file
			boolean succeed = erParse(f, false);

			if (!succeed)
				break;

			cid = cid.toLowerCase();

			boolean noFields = true;

			// run through parsed tokens
			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {
					case MimeTokenStream.T_FIELD:
						setContentType();
						if (cid.length() > 0 && mtStream.getField().getName().equals("Content-ID")) {
							noFields = false;
							if (mtStream.getField().getBody().toLowerCase().indexOf(cid) != -1)
								matches.add(mtStream.getField().getBody());
						}
						break;
					}
				}
			} catch (IOException e) {

			} catch (MimeException e) {

			}

			// try to read it just in as a file
			if (noFields && !mIsPostMortem && mFileLocation.indexOf(cid) != -1) {
				matches.add(mFileLocation);
			}
		}

		return matches;
	}

	/**
	 * Take a non XML section and make it XML just by adding a open and close
	 * Root element
	 * 
	 * 
	 * @param stream
	 *            InputStream to encapsulate in xml
	 * @return InputStream in XML format
	 */
	public InputStream inputStreamXmlEncapsulate(InputStream stream) {
		String frontNode = "<Root>";
		String endNode = "</Root>";

		Future<String> ioCall = streamToString(stream);
		String sectionData = "";
		try {
			sectionData = ioCall.get(60,TimeUnit.SECONDS);
		}
		catch (OutOfMemoryError e) {
			LogManager.getRootLogger().error("ERFramework::inputStreamXmlEncapsulate FAILED DUE TO OUT OF MEMORY ON "
					+ e.getMessage());
			System.exit(0);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			LogManager.getRootLogger().error("ERFramework::inputStreamXmlEncapsulate -- stream to string failed (Likely timed out on IOUtils.toString, it failed us!).");
			e.printStackTrace();
		}
		
		IOUtils.closeQuietly(stream);

		sectionData = sectionData.replaceAll("\\u0000", "");
		String escapedXml = StringEscapeUtils.escapeXml(sectionData);

		List<InputStream> xmlStreamList = Arrays.asList(new ByteArrayInputStream(frontNode.getBytes()),
				(InputStream) new ByteArrayInputStream(escapedXml.getBytes()),
				new ByteArrayInputStream(endNode.getBytes()));
		InputStream endStream = new SequenceInputStream(Collections.enumeration(xmlStreamList));

		return endStream;
	}
	
	private final ExecutorService pool_ = Executors.newFixedThreadPool(1);
	public Future<String> streamToString(final InputStream stream)
	{
		return pool_.submit(new Callable<String>() {
		@Override
		public String call() throws Exception {
		String sectionData = "";
		try {
			sectionData = IOUtils.toString(stream, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sectionData;
		}
		});
	}

	/**
	 * Locate an ErrorReport section by content ID and return an InputStream.
	 * The first byte returned by the InputStream is the first byte of the
	 * entity data after the entity headers. InputStream sets EOF on the last
	 * byte of this entity and does not consume beyond the terminating entity
	 * boundary.
	 * 
	 * Closing the InputStream should close the file.
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to stream
	 * @param cidList
	 *            ArrayList DocumentSection - list of sections found and
	 *            returned
	 * @param wildcard
	 *            boolean - If multiple sections should be returned, true,
	 *            otherwise false to return first section found
	 * @param addedExtension
	 *            String - Add extension filename to output file
	 * @return InputStream pointing to the section located by CID
	 */
	public void getCidListAsDocument(String cid, ArrayList<DocumentSection> cidList, boolean wildcard,
			String addedExtension, boolean omit_decode_cache) throws ERException {
		boolean sectionFound = false;
		String sectionName = "";
		
		for (int f = 0; f < MAX_ZIPPED_FILES; f++) {
			// parse input file
			
			boolean succeed = erParse(f, false);

			if (!succeed)
				break;

			String curSectionName = "";

			// used for building table of sections that exist or not
			Hashtable<String, Boolean> mSectionsExist = null;
			try {
				mSectionsExist = mSectionList.get(f);
			} catch (Exception ex) {
				// captures length issues and things of that nature with ArrayList get.. we just
				// want null if it doesn't exist.
			}

			// instantiate the table if it does not exist for use further below
			if (mSectionsExist == null) {
				// if the mSectionList does not have previous built up entries we need to add
				// empty hashtables in place
				if (f > mSectionList.size())
					for (int t = mSectionList.size(); t < f; t++)
						mSectionList.add(t, new Hashtable<String, Boolean>());

				mSectionsExist = new Hashtable<String, Boolean>();
				mSectionList.add(f, mSectionsExist);
				sectionListLive = false;
			}

			// if this is a postmortem we follow different rules to get what we
			// want
			if (mIsPostMortem) {
				// if on a previous wildcard run we passed through entire archive, we can
				// establish if the cid exists or not
				if (!checkArchiveSections(mSectionsExist, cid, wildcard))
					continue; // check other files (f)

				ArchiveEntry ent = null;
				try {
					while ((ent = mArchiveStream.getNextEntry()) != null) {
						Boolean existRes = mSectionsExist.get(ent.getName());
						if (existRes == null) {
							mSectionsExist.put(ent.getName(), true);
						}
						// either wildcard (return multiple entries) is not set
						// and
						// the cid is exact, or we check the
						// cid to see if it 'contains' the cid phrase
						/*
						 * april 22 2018 - updated or operand to include wildcard on wildcard attribute
						 * needs to be honored to try a wildcard match, wildcard off means no wildcard
						 * attempt
						 */
						boolean cidMatch = false;

						if ((!wildcard && ent.getName().equals(cid))
								|| (wildcard && ent.getName().indexOf(cid) != -1)) {
							cidMatch = true;
						}
						if (!cidMatch && (existRes != null || !mRetrieveAllFiles))
							continue;
						else if ( mRetrieveAllFiles && wildcard )
							mTriggeredPullFiles = true;

						curSectionName = ent.getName();
						LogManager.getRootLogger().debug("ERFramework::getCidListAsDocument -- getCidListAsDocument for "
								+ cid + " found a result of " + curSectionName);

						Boolean res = mOutOfMemSections.get(curSectionName);
						if (res != null && res == true) {
							LogManager.getRootLogger().info(
									"ERFramework::getCidListAsDocument(postmortem) - Skipping load attempt section as it previously caused out of memory: "
											+ curSectionName);
						} else {
							InputStream inArchStream = readArchiveFile(mArchiveStream);

							// take care of the gzip files inside the
							// .tar.gz,
							// iterative files
							if (ent.getName().endsWith(".gz"))
								inArchStream = new GZIPInputStream(inArchStream);

							InputStream encapsulatedStream = null;

							// xml support inside post mortems
							if (ent.getName().endsWith(".xml"))
								encapsulatedStream = inArchStream;
							else
								// else throw the data into XML for the
								// DocSection
								encapsulatedStream = inputStreamXmlEncapsulate(inArchStream);

							if (encapsulatedStream != null) {
								try {
									DocumentSection section = new DocumentSection(getDOM(encapsulatedStream),
											ent.getName(), addedExtension, this, mPhase, mPhaseFile);
									if (cidMatch)
										cidList.add(section);
									AnalyticsFunctions.generateFileFromContent(section);
								} catch (Exception ex) {
									// if we fail lets not skip out on the
									// rest of
									// the possibilities
								}
							}
							// we only return one entry because wildcard isn't
							// set
							if (!wildcard)
								break;
						}
					}
				} catch (IOException ex) {

				} catch (OutOfMemoryError e) {
					if (curSectionName.length() > 0)
						mOutOfMemSections.put(curSectionName, true);
					LogManager.getRootLogger().info(
							"ERFramework::getCidListAsDocument(postmortem) - Out of Memory error has occurred in retrieving the document section: "
									+ curSectionName);
				}

				if (wildcard) {
					sectionListLive = true;
				}

				if (cidList.size() < 1) {
					Boolean existRes = mSectionsExist.get(cid);
					if (existRes == null) {
						mSectionsExist.put(cid, false);
					}
				}
				// this is to get the sections in the proper order (file,
				// file.1,
				// file.2 etc)
				Collections.sort(cidList, new DocSort());

				if (wildcard && mTriggeredPullFiles && mRetrieveAllFiles)
				{
					mRetrieveAllFiles = false;
				}
				return; // we don't go any further because we have our list from
						// the
						// postmortem
			}

			// run through parsed tokens
			boolean noFields = true;

			if (!omit_decode_cache) {
				Enumeration<String> en = mDecodedCache.keys();
				while (en.hasMoreElements()) {
					String key = en.nextElement();
					Pattern pattern = Pattern.compile(cid);
					Matcher matcher = pattern.matcher(key);
					if (matcher.find()) {
						ERMimeSection ers = mDecodedCache.get(key);
						if (ers != null) {
							try {
								ers.mInput.reset();
							} catch (IOException e) {
								continue;
							}
							DocumentSection section = new DocumentSection(getDOM(inputStreamXmlEncapsulate(ers.mInput)),
									ers.mCidName, addedExtension, this, mPhase, mPhaseFile);
							cidList.add(section);

							if (!wildcard)
								break;
						}
					}
				}

				if (cidList.size() > 0) {
					return;
				}
			}

			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {
					case MimeTokenStream.T_BODY:
						Boolean existRes = mSectionsExist.get(curSectionName);
						if (existRes == null) {
							mSectionsExist.put(curSectionName, true);
						}
						if (!sectionFound && (existRes != null || !mRetrieveAllFiles)) {
							continue;
						}
						else if ( mRetrieveAllFiles && wildcard )
							mTriggeredPullFiles = true;

						if (sectionFound == true || existRes == null) {

							Boolean res = mOutOfMemSections.get(curSectionName);
							if (res != null && res == true) {
								LogManager.getRootLogger().info(
										"ERFramework::getCidListAsDocument(mime) - Skipping load attempt section as it previously caused out of memory: "
												+ curSectionName);
							} else {
								LogManager.getRootLogger().debug("Reading body of section: " + curSectionName);
								DocumentSection section = null;
								if (sectionName.contains("backtrace")) {
									section = new DocumentSection(
											getDOM(inputStreamXmlEncapsulate(
													decodeBacktrace(sectionName, mtStream.getInputStream()))),
											curSectionName, addedExtension, this, mPhase, mPhaseFile);
								} else if (mContentType.contains("text/plain")) {
									InputStream encapsulatedStream = inputStreamXmlEncapsulate(
											mtStream.getInputStream());
									if (encapsulatedStream != null) {
										section = new DocumentSection(getDOM(encapsulatedStream), curSectionName,
												addedExtension, this, mPhase, mPhaseFile);
									}
								} else if (mContentEncoding.equalsIgnoreCase("base64")) {
									section = new DocumentSection(
											getDOM(new Base64.InputStream(mtStream.getInputStream())), curSectionName,
											addedExtension, this, mPhase, mPhaseFile);
								} else {
									section = new DocumentSection(getDOM(mtStream.getInputStream()), curSectionName,
											addedExtension, this, mPhase, mPhaseFile);
								}

								if (section != null) {
									if (existRes == null)
										AnalyticsFunctions.generateFileFromContent(section);
									// resolves attempting to run formula against each section (when we really just want to create files for unmatched results)
									if ( sectionFound )
										cidList.add(section);
								}
							}
							if (!wildcard)
								break;
						}
						break;
					case MimeTokenStream.T_FIELD:
						sectionFound = false; // reset the sectionFound flag
												// since
												// we are in a new section
						setContentType();
						if (cid.length() > 0 && mtStream.getField().getName().equals("Content-ID")) {
							noFields = false;
							curSectionName = mtStream.getField().getBody().trim();

							Pattern pattern = Pattern.compile(cid);
							Matcher matcher = pattern.matcher(mtStream.getField().getBody());
							if (matcher.find()) {
								LogManager.getRootLogger().debug("Identified section: " + curSectionName);
								sectionFound = true;
								sectionName = curSectionName;
							}
						}
						break;
					default:
					}
				}
			} catch (IOException e) {

			} catch (MimeException e) {

			} catch (OutOfMemoryError e) {
				if (curSectionName.length() > 0)
					mOutOfMemSections.put(curSectionName, true);
				LogManager.getRootLogger().info(
						"ERFramework::getCidListAsDocument(mime) - Out of Memory error has occurred in retrieving the document section: "
								+ curSectionName);
			}

			// try to read it just in as a file
			if(noFields && !mIsPostMortem) {
				Pattern pattern = Pattern.compile(cid);
				Matcher matcher = pattern.matcher(mFileLocation);
				if (matcher.find()) {
					InputStream encapsulatedStream = LoadFileStream();
					if (encapsulatedStream != null) {
						DocumentSection section = new DocumentSection(getDOM(encapsulatedStream), mFileLocation,
								addedExtension, this, mPhase, mPhaseFile);
						cidList.add(section);
					}
				}
			}
		}
		
		if (wildcard && mTriggeredPullFiles && mRetrieveAllFiles)
		{
			mRetrieveAllFiles = false;
		}
	}
	/**
	 * Check if document sections will exist to matchup current
	 * report file.  If not bypass these sections
	 */
	public boolean checkArchiveSections(Hashtable<String, Boolean> sectionExist, String cid, boolean wildcard)
	{
		if ( !sectionListLive )
			return true;
		
			if ( !wildcard )
			{
				Boolean existRes = sectionExist.get(cid);
				if ( existRes != null && existRes == false )
				{							
					LogManager.getRootLogger().debug("ERFramework::getCidListAsDocument -- sections does not exist for " + cid + ", skipping.");
					return false;
				}
				else if ( !wildcard && existRes == null )
				{
					LogManager.getRootLogger().debug("ERFramework::getCidListAsDocument -- sections does not exist for (no wildcard) " + cid + ", skipping.");
					return false;
				}
			}
			else // wildcard handling
			{
				boolean matches = false;
				for (Entry<String, Boolean> entry : sectionExist.entrySet()) {
					if ( entry.getKey().indexOf(cid) > -1 )
					{
						matches = true;
						break;
					}
				}
				
				if ( !matches )
				{
					LogManager.getRootLogger().debug("ERFramework::getCidListAsDocument -- sections does not exist for (wildcarded) " + cid + ", skipping.");
					return false;
				}
			}
			
			return true;
	}

	/**
	 * Explode error report into individual files - one per section.
	 * 
	 * @param prefix
	 *            String to prepend to each file name.
	 */
	public void expand(String prefix) throws ERException {
		expand(prefix, "");
	}

	/**
	 * Same as above with option to write a single section.
	 * 
	 * @param prefix
	 *            String to prepend to each file name.
	 * @param cid
	 *            Section ID to process - "" for all sections.
	 */
	public void expand(String prefix, String cid) throws ERException {
		String thisCid = "";
		FileWriter out = null;

		for (int f = 0; f < MAX_ZIPPED_FILES; f++) {
			// parse input file
			boolean succeed = erParse(f, false);

			if (!succeed)
				break;

			// run through parsed tokens
			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {

					case MimeTokenStream.T_BODY:
						if (cid.length() == 0 || cid.indexOf(thisCid) >= 0) {
							InputStream in = decodeBacktrace(cid, mtStream.getInputStream());

							// process as plain text
							out = new FileWriter(prefix + "." + thisCid);
							for (int ch = in.read(); ch >= 0; ch = in.read()) {
								out.write(ch);
							}
							in.close();
							out.close();

						}
						break;

					case MimeTokenStream.T_FIELD:
						setContentType();
						if (mtStream.getField().getName().equals("Content-ID")) {
							// extract section name
							thisCid = mtStream.getField().getBody().trim();
							if (thisCid.indexOf('<') >= 0)
								thisCid = thisCid.substring(thisCid.indexOf('<') + 1);
							if (thisCid.indexOf('@') >= 0)
								thisCid = thisCid.substring(0, thisCid.indexOf('@'));
						}
						break;
					default:
					}
				}
			} catch (FileNotFoundException e) {
				throw new ERFrameworkFileException(msgs.getString("bad_filename") + " - expand " + e.toString());
			} catch (IOException e) {
				throw new ERFrameworkIOException(msgs.getString("token_error") + " expand " + e.toString());
			} catch (MimeException e) {
				throw new ERFrameworkMimeException(msgs.getString("token_error") + " expand " + e.toString());
			}
		}
	}

	/**
	 * Locate an ErrorReport section by content ID and return a parsed XML
	 * Document (DOM tree).
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to parse
	 * @return Document (DOM tree) representing the parsed XML entity
	 */
	public Document getCidAsXML(String cid) throws ERException {
		ERMimeSection section = getCidAsInputStream(cid, false, 0, 0, false);
		if (section != null)
			return getDOM(section.mInput);

		return null;
	}

	/**
	 * Locate an ErrorReport section by content ID that is not xml and
	 * encapsulate Document (DOM tree).
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to parse
	 * @return Document (DOM tree) representing the parsed XML entity
	 */
	public Document getNonXmlCidAsXML(String cid) throws ERException {
		ERMimeSection mime = getCidAsInputStream(cid, false, 0, 0, false);

		if (mime == null || mime.mInput == null)
			return null;
		else
			return getDOM(inputStreamXmlEncapsulate(mime.mInput));
	}

	/**
	 * Output the complete report using the format specified
	 * 
	 * @param format
	 *            Format of the intended result
	 * @param out
	 *            OutputStream to write the result to
	 * @throws IOException
	 */
	public void outputReport(String format, OutputStream out) throws ERException, IOException {
		if (format.equals("HTML")) {
			outputReportAsHTML(out);
		}

		else if (format.equals("TEXT")) {
			// Straight copy if no XSL translation for TEXT
			if (getCidXslPath(format, "*").length() == 0) {
				try {
					InputStream in = new FileInputStream(mFileLocation);
					if (mFileLocation.endsWith(".gz") || mFileLocation.endsWith(".GZ"))
						in = new GZIPInputStream(in);
					InputStreamReader inR = new InputStreamReader(in);
					outputPlainText(inR, out);
					inR.close();
				} catch (IOException e) {
					throw new ERFrameworkFileException(
							msgs.getString("bad_filename") + " - outputReport " + e.toString());
				}
			}
			// Apply XSL translation to at least one XML section
			else
				outputReportAsTEXT(out);
		}

		else
			throw new ERFrameworkParseException(msgs.getString("bad_format") + " - outputReport ");
	}

	/**
	 * Output a section using the format specified
	 * 
	 * @param format
	 *            Format of the intended result
	 * @param cid
	 *            Content ID of the ErrorReport entity to parse
	 * @param out
	 *            OutputStream to write the result to
	 */
	public void outputCid(String format, String cid, OutputStream out) throws ERException {
		String xslPath;
		ERMimeSection mime = getCidAsInputStream(cid, false, 0, 0, false);
		OutputStreamWriter outW;

		if (mime == null || mime.mInput == null)
			return;

		try {
			if (format.equals("HTML")) {
				// See if section is XML first. If not, output as plain text.
				if (isXMLsection()) {
					outW = new OutputStreamWriter(out);
					outputXMLasXSLT(mime.mInput, outW, getCidXslPath(format, cid));
					outW.close();
				} else {
					if (mContentEncoding.equalsIgnoreCase("base64"))
						outputHex(mime.mInput, new OutputStreamWriter(out), true);
					else
						outputPlainText(new InputStreamReader(mime.mInput, encoding()), out);
				}
			}

			else if (format.equals("TEXT")) {
				if (isXMLsection()) {
					xslPath = getCidXslPath(format, cid);
					if (xslPath.length() > 0) {
						outW = new OutputStreamWriter(out);
						outputXMLasXSLT(mime.mInput, outW, xslPath);
						outW.close();
					} else {
						if (mContentEncoding.equalsIgnoreCase("base64"))
							outputHex(mime.mInput, new OutputStreamWriter(out), false);
						else
							outputPlainText(new InputStreamReader(mime.mInput, encoding()), out);
					}
				} else {
					if (mContentEncoding.equalsIgnoreCase("base64"))
						outputHex(mime.mInput, new OutputStreamWriter(out), false);
					else
						outputPlainText(new InputStreamReader(mime.mInput, encoding()), out);
				}
			}

			else if (format.equals("CSV")) {
				// See if section is XML first. If not, output as plain text.
				if (isXMLsection()) {
					xslPath = getCidXslPath(format, cid);
					if (xslPath.length() > 0) {
						outW = new OutputStreamWriter(out);
						outputXMLasXSLT(mime.mInput, outW, xslPath);
						outW.close();
					} else
						outputXMLasCSV(mime.mInput, out);
				} else
					outputPlainText(new InputStreamReader(mime.mInput, encoding()), out);
			}

			else
				throw new ERFrameworkParseException(msgs.getString("bad_format") + " - outputCid() ");
		} catch (IOException e) {
			throw new ERFrameworkIOException("outputCid " + e.toString());
		}
	}

	/**
	 * Decode a backtrace stream.
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to parse
	 * @param in
	 *            Undecoded input stream
	 * @return Decoded input stream (undecoded if not backtrace section or no
	 *         backtrace decoder)
	 */
	public InputStream decodeBacktrace(String cid, InputStream in) {
		internal.ERBacktrace bt = new internal.ERBacktrace();
		return bt.decode(in);
	}

	/**
	 * Return a parsed XML Document (DOM tree) for a given input stream.
	 * 
	 * @param cid
	 *            Content ID of the ErrorReport entity to parse
	 * @return Document (DOM tree) representing the parsed XML entity
	 */
	private Document getDOM(InputStream in) throws ERException {
		try {
			DocumentBuilder db = mDocBuilderFactory.newDocumentBuilder();
			Document result = db.parse(new InputSource(new InputStreamReader(in, encoding())));
			return result;
		} catch (IOException e) {
			throw new ERFrameworkIOException(" getCidAsXML " + e.toString());
		} catch (ParserConfigurationException e) {
			throw new ERFrameworkParseException("getCidAsXML " + e.toString());
		} catch (SAXException e) {
			throw new ERFrameworkXMLException("getCidAsXML " + e.toString());
		}
	}

	/**
	 * Output the complete report in HTML format
	 * 
	 * @param out
	 *            OutputStream to write the result to
	 * @throws IOException
	 */
	private void outputReportAsHTML(OutputStream out) throws ERException, IOException {
		InputStream in = null;
		String sectionName = "";
		OutputStreamWriter outW = new OutputStreamWriter(out);

		for (int f = 0; f < MAX_ZIPPED_FILES; f++) {
			// parse input file
			boolean succeed = erParse(f, false);

			if (!succeed)
				break;
			mLine = -1;
			// HTML header output is handled by java, not erHTML.xsl in order to
			// keep HTML output clean and not repetitive
			lineOut("<html>\n<head>", outW);
			lineOut("<title>Error Report</title>", outW);
			lineOut("  <meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"/>", outW);
			lineOut("<style>" + "body      { background-color: #FFFFFF; }"
					+ ".element  { color: #5B7A9D; font-size:150%}" + ".text     { color: #586AAD; }"
					+ "th {background-color:#4477bb}" + "</style>", outW);
			lineOut("</head>\n<body>", outW);

			// run through parsed tokens
			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {
					case MimeTokenStream.T_BODY:
						in = decodeBacktrace(sectionName, mtStream.getInputStream());
						if (isXMLsection()) {
							// Printing of the section header is not handled by
							// erHTML.xsl
							lineOut("<br/><br/><span class=\"element\">", outW);
							lineOut("<a name=\"" + sectionName + "\">", outW);
							lineOut(sectionName, outW);
							lineOut("</a></span>", outW);
							// Outputs html using XSL template
							outputXMLasXSLT(in, outW, getCidXslPath("HTML", sectionName));
						}

						else {
							// Printing of the section header is not handled by
							// erHTML.xsl
							lineOut("<br/><br/><span class=\"element\">", outW);
							lineOut("<a name=\"" + sectionName + "\">", outW);
							lineOut(sectionName, outW);
							lineOut("</a></span><pre>", outW);

							if (mContentEncoding.equalsIgnoreCase("base64"))
								outputHex(new Base64.InputStream(in), outW, true);
							else
								for (int ch = in.read(); ch >= 0; ch = in.read())
									outW.write(ch);
							lineOut("</pre>", outW);
						}

						in.close();
						break;
					case MimeTokenStream.T_FIELD:
						setContentType();
						if (mtStream.getField().getName().equals("Content-ID")) {
							sectionName = mtStream.getField().getBody();
							sectionName = sectionName.substring(sectionName.indexOf('<') + 1);
							sectionName = sectionName.substring(0, sectionName.indexOf('@'));
						}
						break;
					default:
					}
				}
				lineOut("</body>\n</html>", outW);
				outW.close();
			} catch (IOException e) {
				throw new ERFrameworkIOException(msgs.getString("token_error") + "outputReportAsHTML " + e.toString());
			} catch (MimeException e) {
				throw new ERFrameworkMimeException(
						msgs.getString("token_error") + "outputReportAsHTML " + e.toString());
			}
		}
	}

	/**
	 * Output the complete report in TEXT format
	 * 
	 * @param out
	 *            OutputStream to write the result to
	 */
	private void outputReportAsTEXT(OutputStream out) throws ERException {
		InputStream in = null;
		String sectionName = "";
		OutputStreamWriter outW = new OutputStreamWriter(out);

		for (int f = 0; f < MAX_ZIPPED_FILES; f++) {
			// parse input file
			boolean succeed = erParse(f, false);

			if (!succeed)
				break;
			// run through parsed tokens
			try {
				for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
						.next()) {
					switch (state) {
					case MimeTokenStream.T_BODY:
						in = decodeBacktrace(sectionName, mtStream.getInputStream());
						mLine = -1;

						lineOut("\n\n" + sectionName, outW);
						if (isXMLsection()) {
							String xslPath = getCidXslPath("TEXT", sectionName);
							if (xslPath.length() > 0) {
								outputXMLasXSLT(in, outW, xslPath);
							} else
								for (int ch = in.read(); ch >= 0; ch = in.read())
									outW.write(ch);
						} else {
							if (mContentEncoding.equalsIgnoreCase("base64"))
								outputHex(new Base64.InputStream(in), outW, false);
							else
								for (int ch = in.read(); ch >= 0; ch = in.read())
									outW.write(ch);
						}
						in.close();
						break;
					case MimeTokenStream.T_FIELD:
						setContentType();
						if (mtStream.getField().getName().equals("Content-ID")) {
							sectionName = mtStream.getField().getBody();
							sectionName = sectionName.substring(sectionName.indexOf('<') + 1);
							sectionName = sectionName.substring(0, sectionName.indexOf('@'));
						}
						break;
					default:
					}
				}
				outW.close();
			} catch (IOException e) {
				throw new ERFrameworkIOException(msgs.getString("token_error") + "outputReportAsHTML " + e.toString());
			} catch (MimeException e) {
				throw new ERFrameworkMimeException(
						msgs.getString("token_error") + "outputReportAsHTML " + e.toString());
			}
		}
	}

	/**
	 * Output a section as plain text
	 * 
	 * @param in
	 *            InputStreamReader of the section body
	 * @param out
	 *            OutputStream to write the result to
	 */
	private void outputPlainText(InputStreamReader in, OutputStream out) throws ERException {
		try {
			// Writer needed to handle character encoding properly
			OutputStreamWriter outW = new OutputStreamWriter(out);
			for (int ch = in.read(); ch >= 0; ch = in.read()) {
				outW.write(ch);
			}
			outW.write((int) '\n');
			outW.close();
		} catch (IOException e) {
			throw new ERFrameworkIOException("outputPlainText " + e.toString());
		}
	}

	/**
	 * Output a section as hexadecimal dump
	 * 
	 * @param in
	 *            InputStream of the section body
	 * @param out
	 *            <xsl:value-of select="name(..)"/> OutputStreamWriter to write
	 *            the result to
	 * @param html
	 *            true if HTML format (converts < to &lt;)
	 */
	private void outputHex(InputStream in, OutputStreamWriter outW, boolean html) throws ERException {
		int line[] = new int[16];
		int count = 0;
		int i, j;
		String hex;
		int addr = 0;
		try {
			// Process each character in input stream
			for (int ch = in.read();; ch = in.read()) {
				if (ch >= 0)
					line[count++] = ch;

				// write a line if at 16 bytes or end of stream
				if (count >= 16 || ch < 0) {
					// Limit HTML dump
					if (html && addr > 0x800) {
						outW.write("\nHTML formatting truncated.  To see full section, format as TEXT.\n");
						break;
					}
					// 8 digit address
					hex = Integer.toHexString(addr);
					addr += 16;
					for (j = hex.length() - 8; j < hex.length(); j++)
						if (j < 0)
							outW.write((int) '0');
						else
							outW.write((int) hex.codePointAt(j));
					outW.write((int) ':');

					// Hex data
					for (i = 0; i < 16; i++) {
						outW.write((int) ' ');
						if (i < count)
							hex = Integer.toHexString(line[i]);
						else
							hex = "  ";
						for (j = hex.length() - 2; j < hex.length(); j++)
							if (j < 0)
								outW.write((int) '0');
							else
								outW.write((int) hex.codePointAt(j));
					}
					outW.write((int) ' ');
					outW.write((int) ' ');

					// character data (if in printable range)
					for (i = 0; i < count; i++) {
						if (html && line[i] == 0x3c) // HTML tags will confuse
														// browser
							outW.write("&lt;");
						else if (line[i] >= 0x20 && line[i] <= 0x7E)
							outW.write(line[i]);
						else
							outW.write((int) '.');
					}
					outW.write((int) '\n');
					count = 0;
				}
				if (ch < 0)
					break;
			}
			outW.write((int) '\n');
		} catch (IOException e) {
			throw new ERFrameworkIOException("outputHex " + e.toString());
		}
	}

	/**
	 * Output an XML section in CSV format
	 * 
	 * @param in
	 *            InputStream containing section contents
	 * @param out
	 *            OutputStream to write the result to
	 */
	private void outputXMLasCSV(InputStream in, OutputStream out) throws ERException {
		try {
			Document doc = getDOM(in);
			mLine = 0;
			OutputStreamWriter outW = new OutputStreamWriter(out);
			dom_to_csv((Node) doc.getDocumentElement(), "", outW);
			outW.close();
		} catch (IOException e) {
			throw new ERFrameworkIOException("outputXMLasCSV " + e.toString());
		} catch (SAXException e) {
			throw new ERFrameworkXMLException("outputXMLasCSV " + e.toString());
		}
	}

	/**
	 * Output an XML section in HTML format
	 * 
	 * @param in
	 *            InputStream containing section contents
	 * @param out
	 *            OutputStreamWriter to write the result to
	 * @param xslPath
	 *            Path to XSL file
	 */
	private void outputXMLasXSLT(InputStream in, OutputStreamWriter out, String xslPath) throws ERException {
		if (xslPath.length() == 0)
			throw new ERFrameworkXMLException(msgs.getString("no_xslname"));
		try {
			File xsltFile = new File(xslPath);
			if (xsltFile == null)
				throw new ERFrameworkXMLException(msgs.getString("no_xslname"));
			Source xmlSource = new StreamSource(new InputStreamReader(in, encoding()));
			Source xsltSource = new StreamSource(xsltFile);
			Result result = new StreamResult(out);
			TransformerFactory transFact = TransformerFactory.newInstance();
			Transformer trans = transFact.newTransformer(xsltSource);
			trans.transform(xmlSource, result);
		} catch (TransformerException e) {
			throw new ERFrameworkXMLException("outputXMLasXSLT " + e.toString());
		} catch (UnsupportedEncodingException e) {
			throw new ERFrameworkXMLException("outputXMLasXSLT " + "'" + encoding() + "' " + e.toString());
		}
	}

	/**
	 * Write DOM tree to output CSV file
	 */
	private void dom_to_csv(Node root, String prefix, OutputStreamWriter out) throws IOException, SAXException {
		// based on node type, append node name to prefix or output text
		if (root instanceof Element) {
			prefix += "," + ((Element) root).getTagName();
		} else if (root instanceof CharacterData) {
			String data = ((CharacterData) root).getData().trim();
			if (!data.equals("")) {
				lineOut(prefix + "," + data, out);
			}
		} else {
			prefix += "," + root.getClass().getName();
		}

		// output each attribute value on a separate line
		NamedNodeMap attrs = root.getAttributes();
		if (attrs != null) {
			int len = attrs.getLength();
			for (int i = 0; i < len; i++) {
				Node attr = attrs.item(i);
				if (attr instanceof Attr) {
					lineOut(prefix + "," + attr.getNodeName() + "," + ((Attr) attr).getValue(), out);
				}
			}
		}

		// recursively process subtree
		if (root.hasChildNodes()) {
			NodeList children = root.getChildNodes();
			if (children != null) {
				int len = children.getLength();
				for (int i = 0; i < len; i++) {
					dom_to_csv(children.item(i), prefix, out);
				}
			}
		}
	}

	/*
	 * Identify current section as XML or not
	 */
	private boolean isXMLsection() {
		if (mContentType.indexOf("xml") >= 0) {
			if (!mContentType.contains(";"))
				return true;
			if (mContentType.indexOf(";") > mContentType.indexOf("xml"))
				return true;
		}
		return false;
	}

	/*
	 * Identify character encoding for current section
	 */
	private String encoding() {
		String encoding = "UTF-8";
		if (mContentType.indexOf("charset=") >= 0) {
			encoding = mContentType.substring(mContentType.indexOf("charset=") + 8);
			if (encoding.indexOf(";") > 0)
				encoding = encoding.substring(0, encoding.indexOf(";"));
		}
		return encoding;
	}

	/**
	 * Output a numbered line of text
	 */
	private void lineOut(String txt, OutputStreamWriter out) throws IOException {
		if (mLine >= 0) {
			mLine++;
			Integer inum = new Integer(mLine);
			String num = inum.toString();
			for (int j = 0; j < num.length(); j++)
				out.write((int) num.charAt(j));
		}
		for (int i = 0; i < txt.length(); i++)
			out.write((int) txt.charAt(i));
		out.write((int) '\n');
	}

	/**
	 * Attempts post-mortem decompression (.tar.gz) to variable mArchiveStream
	 * otherwise we parse MIME token stream to variable mtStream Start Start
	 * Additional Decompressions (takes place after erParse() called) ZIP -->
	 * .tar.gz --> .gz (iteration files in .tar.gz) - post mortem --> .txt.gz -
	 * error report --> .txt - error report
	 * determinePhase is ran upon the setFileLocation->EstablishHighPhase to determine
	 * if multiple compressed files need to be processed
	 */
	private boolean erParse(int attempt, boolean determinePhase) throws ERException {
		boolean mBasePostMortem = false; // if its a zip file post mortem
											// without .tar.gz archive
		mIsPostMortem = false;
		mPhase = attempt;

		mPhaseFile = this.getFileLocation();
		String oldFile = mPhaseFile;
		boolean skipPhase = false;
		boolean attemptsMade = false;
		int maxAttempts = 0;
		try {
			mtStream = new MimeTokenStream();

			InputStream stream = new FileInputStream(mFileLocation);

			InputStream zippedStream = null; // file pulled from zip

			// attempt a zip decode for post mortem
			if (mFileLocation.endsWith(".zip")) {
				ZipInputStream inputStream = null;
				try {
					inputStream = new ZipInputStream(new FileInputStream(mFileLocation));
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				InputStream tmpStream = null;
				if (inputStream != null) {
					// find out what type of report if its a zip
					ZipEntry entry = inputStream.getNextEntry();
					try {
						for (int i = 0; i < attempt; i++) {
							attemptsMade = true;
							entry = inputStream.getNextEntry();
							if (entry != null && entry.getName().endsWith(".zip")) {
								// since we have a zip match inside the main archive then we set maxAttempts to the current iteration
								maxAttempts = i;
								tmpStream = readArchiveFile(inputStream);
								ZipInputStream inputStream2 = new ZipInputStream(tmpStream);
								entry = inputStream2.getNextEntry();
								skipPhase = true;
							}
							
							if (entry == null)
							{
								/* we are aborting out cause this is likely not a postmortem (or its a dual zip with ER+postmortem
								* or ER+ER or postmortem + postmortem combination, we still need to honor the full list to pull
								* results from those unique files
								*/
								if (determinePhase && i > mHighestPhase)
									mHighestPhase = i;
								
								return false;
							}
						}
						// we only care about the first entry really, we can't
						// handle multiple docs less its a post mortem .tar.gz
						// which is handled in getCidListAsDocument
						if (entry != null) {
							if (entry.getName().startsWith("var/") || entry.getName().startsWith("etc/"))
								mBasePostMortem = true;

							if (skipPhase && tmpStream != null) {
								stream = tmpStream;
								stream.reset();
								try {
									inputStream = new ZipInputStream(stream);
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							} else {
								if (stream != null)
									stream.close();

								oldFile = mPhaseFile;
								
								mPhaseFile = entry.getName();
								stream = readArchiveFile(inputStream);
								
								// verify if the file can be parsed as an error report (mime document) or not
								/*if ( !oldFile.equals(mPhaseFile) && !mBasePostMortem )
								{
									try
									{
									mtStream.parse(stream);
	
									boolean matchFound = false;
										for (int state = mtStream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtStream
												.next()) {
											switch (state) {
											case MimeTokenStream.T_BODY:
													if (mtStream.getField().getBody() != null) {
														matchFound  = true;
													}
													break;
												default:
													break;
											}
											if ( matchFound )
												break;
										}
									}
									catch(Exception ex)
									{
										System.out.println("failed " + ex.getMessage());
										mBasePostMortem = true;
									}
								}*/
							}
							zippedStream = stream;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} // end of ".zip" block

			if (attempt > 0 && !attemptsMade)
				return false;
			// only update if we are in the initial parsing of the file on startup
			else if (determinePhase && maxAttempts > mHighestPhase)
				mHighestPhase = maxAttempts;
			
			// we can abort out at this point, we are just determining how many inner files we need to parse for results
			if ( determinePhase )
				return true;

			// attempted gzip decode, if it fails then parse text as is
			try {
				boolean contLoop = true; // we disable this outer loop once we
											// have dug in deep enough to not
											// find gzip files
				boolean gzipStage = true;

				TarArchiveInputStream tarInputStream = null;

				while (contLoop) { // we will loop until we don't need to dig
									// any further than the postmortem with the
									// file contents of the linux filesystem
					try {
						// determine if this is a .tar.gz file
						if (gzipStage)
							tarInputStream = new TarArchiveInputStream(new GZIPInputStream(stream));
						else
							tarInputStream = new TarArchiveInputStream(stream);

						TarArchiveEntry ent = null;

						while ((ent = tarInputStream.getNextTarEntry()) != null) {
							String name = ent.getName();
							if (contLoop && name.endsWith(".gz")) {
								stream = readArchiveFile(tarInputStream);
							} else {
								if (gzipStage)
									mArchiveStream = tarInputStream;
								else
									mArchiveStream = new TarArchiveInputStream(new FileInputStream(mFileLocation));

								/*
								 * we dug into the .tar.gz and found the source
								 * post mortem
								 */
								contLoop = false;
								mIsPostMortem = true; // this is a postmortem,
														// make it so
								break;
							}
						}
						if (!gzipStage)
							contLoop = false;
					} catch (Exception ex) {
						// failed both the gzip stage and non-gzip stage, break
						// out
						if (!gzipStage)
							break;

						// if we failed first time around, don't do gzip
						if (tarInputStream == null) {
							if (!skipPhase) {
								stream = new FileInputStream(mFileLocation);
							}
							gzipStage = false;
						} else
							break;
					}

				}

				if (mBasePostMortem) {
					try {
						if (!skipPhase) {
							stream = new FileInputStream(mFileLocation);
						} else
							stream.reset();

						BufferedInputStream buf = new BufferedInputStream(stream);
						mArchiveStream = new ArchiveStreamFactory().createArchiveInputStream(buf);
						mIsPostMortem = true;
					} catch (ArchiveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (!mIsPostMortem) {
					if (zippedStream != null) {
						// we already tried to use it in the post-mortem .tar.gz
						// decode, so we have to reset the buffer
						zippedStream.reset();
						stream = new GZIPInputStream(zippedStream);
					} else {
						stream = new FileInputStream(mFileLocation);
						stream = new GZIPInputStream(stream);
					}
				}
			} catch (IOException ex) {
				stream = new FileInputStream(mFileLocation);
			}

			if (!mIsPostMortem)
				mtStream.parse(stream);

		} catch (IOException e) {
			throw new ERFrameworkIOException(msgs.getString("mime_error") + " erParse() " + e.toString());
		}

		return true;
	}

	/**
	 * Set content type for current section
	 */
	private void setContentType() {
		if (mtStream.getField().getName().equalsIgnoreCase("Content-Type"))
			mContentType = mtStream.getField().getBody().trim();
		if (mtStream.getField().getName().equalsIgnoreCase("Content-Transfer-Encoding"))
			mContentEncoding = mtStream.getField().getBody().trim();
	}

	private InputStream readArchiveFile(InputStream archiveStream) {
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		try {
			while ((len = archiveStream.read(buffer)) != -1) {
				outBuffer.write(buffer, 0, len);
			}
		} catch (IOException ex) {

		}
		ByteArrayInputStream newStream = new ByteArrayInputStream(outBuffer.toByteArray());

		return (InputStream) newStream;
	}

	private InputStream LoadFileStream() {
		try {
			mOriginalStream = new FileInputStream(mFileLocation);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InputStream encapsulatedStream = inputStreamXmlEncapsulate(mOriginalStream);
		return encapsulatedStream;
	}

	public int GetID() {
		return mID;
	}

	public void SetID(int id) {
		mID = id;
	}

	public int GetHighestPhase() {
		return mHighestPhase;
	}
	
	public void EstablishHighPhase() {
		// determine up to 255 files to be parsed individually
		try {
			erParse(MAX_ZIPPED_FILES, true);
		} catch (ERException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean IsPostMortem() { return mIsPostMortem; }


	public void setRetrieveAllFiles(boolean res) {
		mRetrieveAllFiles = res;
	}

	public boolean getRetrieveAllFiles() {
		return mRetrieveAllFiles;
	}
	
	private String mFileLocation; // name of input file
	private Vector mXslFormatList; // List of style sheet formats
	private Vector mXslCidList; // List of style sheet sections
	private Vector mXslPathList; // List of style sheets
	private MimeTokenStream mtStream; // MIME token stream
	private int mLine; // line # for .csv files
	private ResourceBundle msgs; // Error messages
	private String mContentType = ""; // MIME content type for current section
	private String mContentEncoding; // MIME content transfer encoding for
										// current section
	private Class mBacktraceClass; // Dynamically loaded backtrace class
	private Object mBacktraceObject; // Dynamically loaded backtrace object

	private InputStream mOriginalStream; // what the original stream we loaded
											// from
	// post mortem control
	private ArchiveInputStream mArchiveStream; // this is the archive stream
												// of the internal files
												// (breaks out from .tar.gz
												// ..)
	private boolean mIsPostMortem = false; // this is set if we infact know this
	// is a post mortem report
	private int mPhase = 0;
	private int mHighestPhase = 0;
	private String mPhaseFile = "";

	public static DocumentBuilderFactory mDocBuilderFactory = DocumentBuilderFactory.newInstance();

	/*
	 * Prevents going OOM continuously on the same sections in the
	 * report/post-mortem
	 * 
	 */
	Hashtable<String, Boolean> mOutOfMemSections = new Hashtable<String, Boolean>();
	
	// track all the active document contents we are reviewing via erParse
	ArrayList<Hashtable<String, Boolean>> mSectionList = new ArrayList<Hashtable<String, Boolean>>();
	// if the current file has the Hashtable above loaded or not
	boolean sectionListLive = false;
	
	private static int MAX_ZIPPED_FILES = 255;
	private int mID = -1;
	
	// if we want to pull all files and put it into a generated dir for Analytics functionality
	private boolean mRetrieveAllFiles = false;
	private boolean mTriggeredPullFiles = false;

	public Hashtable<String, ERMimeSection> mDecodedCache = new Hashtable<String, ERMimeSection>();
}
