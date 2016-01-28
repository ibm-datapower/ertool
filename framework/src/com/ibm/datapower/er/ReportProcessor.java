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

package com.ibm.datapower.er;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.james.mime4j.parser.Field;
import org.apache.james.mime4j.parser.MimeTokenStream;
import org.xml.sax.SAXException;


/**
 * Processor Engine for Error Reports. Allows easy association of customized
 * processors for each and all parts of an Error Report.
 * 
 * <p>Sample usage - no custom processors, does nothing.
 * <pre>
 * ReportProcessor p = new ReportProcessor();
 * p.load("errorreport.txt.gz");
 * p.setOutputStream(System.out);
 * p.run();
 * </pre>
 * 
 * <p>Another sample - HTML processor set as default
 * <pre>
 * ReportProcessor p = new ReportProcessor();
 * p.load("errorreport.txt.gz");
 * p.addPartsProcessor(new PartsProcessorsHTML(),null);
 * p.setOutputStream(System.out);
 * p.run();
 * </pre>
 *
 */
public class ReportProcessor
{
    /**
     * Installs a custom processor for one or all types of MIME parts.
     * 
     * If a null Content-ID is provided, the custom processor will be set
     * as the default processor. If a given part does not contain an associated
     * custom processor, the default processor is used.
     * 
     * @param proc  object instance to process MIME part(s) 
     * @param cid   MIME part Content-ID to be associate (or null)
     */
    @SuppressWarnings("unchecked")
	void addPartsProcessor(IPartsProcessor proc, String cid) {
    	if (cid == null || cid.isEmpty()) {
    		mDefaultPartProcessor = proc;    		
    	}
    	else {
    		mPartProcessors.put(cid,proc);
    	}
    }
    
    /**
     * Installs a custom preprocessor for the entire report
     * @param proc  object instance to process MIME part(s) 
     * @param cid   MIME part Content-ID to be associate (or null)
     */
    @SuppressWarnings("unchecked")
    void setPrePostProcessor(PrePostReportProcessor ppproc) {
        mPrePostProcessor = ppproc;
    }

    /**
     * Prepares the Error Report as an InputStream for processing 
     * @param filename
     * @throws IOException
     */
	void load(String filename) throws IOException
	{
		mReport = new FileInputStream(filename);
		if (filename.toLowerCase().endsWith(".gz")) 
			mReport = new GZIPInputStream(mReport);
	}
    
	/**
	 * Sets the output stream that will contain the processed Error Report.
	 * 
	 * @param stream
	 */
	void setOutputStream(OutputStream stream) 
	{
		mOutputStream = stream;
	}
	
   /**
     * Sets the output stream that will contain the processed Error Report.
     * 
     * @param stream
     */
    void setFilename(String filename) 
    {
        mFilename = filename;
    }
	
	/**
	 * Kicks off the Processor Engine.
	 * 
	 * <p>Internal details:
	 * The anatomy of a DataPower Error Report:
	 * <pre>
	 * T_START_MESSAGE        -> Stream reading started
	 *
	 *    T_START_HEADER      -> Message part header started
	 *       T_FIELD (*)      -> top part fields (for example MIME-Version)     
	 *    T_END_HEADER  
	 *
	 *    T_START_MULTIPART   -> A multipart body is being parsed
	 *       T_PREAMBLE       -> Preamble Contents (body of the first part, before first boundary - "DataPower Error Report for domain default"
	 *       
	 *       T_START_BODYPART -> Beginning of a body part
	 *          T_START_HEADER      -> Message part header started
	 *             T_FIELD (*)      -> multipart part fields (Content-Type, Content-transfer-encoding, Content-ID)     
	 *          T_END_HEADER
	 *          T_BODY              -> Part itself
	 *       T_END_BODYPART 
	 *       
	 *       T_EPILOGUE       -> Multipart Epilogue (getInputStream) (empty in ErrorReport)
	 *
	 *    T_END_MULTIPART 
	 *
	 * T_END_MESSAGE       -> Stream at the end of the message
	 * T_END_OF_STREAM     -> End of File
	 * </pre>
	 * @throws Exception 
	 * 
     * @see http://james.apache.org/mime4j/apidocs/org/apache/james/mime4j/parser/MimeTokenStream.html
	 */
	@SuppressWarnings("unchecked")
	void run() throws Exception
	{
		int state;
		MimeTokenStream mtstream = null;
		HashMap headers = null;
		Field field = null;
		IPartInfo partInfo = null;
		PrintWriter sysout = null;
		
		//do the preprocessing
		mOutputStream = mPrePostProcessor.getOutputStreamFromFilename(mFilename);
	    sysout = new PrintWriter(mOutputStream);
	    
		mPrePostProcessor.preProcess();
		
		
		// creates a MIME4J stream to parse all parts into tokens
		mtstream = new MimeTokenStream();
		mtstream.parse(mReport);
		
    	// run through parsed tokens
		
    	for (state = mtstream.getState(); state != MimeTokenStream.T_END_OF_STREAM; state = mtstream.next()) 
    	{   
    	    switch (state) 
    		{
    		case MimeTokenStream.T_BODY:
    			partInfo = new ReportProcessorPartInfo(IPartInfo.MIME_BODYPART, headers, mtstream.getInputStream(), mERDetails);
    			process(partInfo,sysout);
    			headers = null; partInfo = null;    			
    			break;

    		case MimeTokenStream.T_FIELD:  
    			field = mtstream.getField();
    			headers.put(field.getName(),field.getBody());
    			field = null;
    			break;
    			
    		case MimeTokenStream.T_START_HEADER: 
    			headers = new HashMap();    			
    			break;
    			
    		case MimeTokenStream.T_END_HEADER: 
    		    break;

    		case MimeTokenStream.T_PREAMBLE:      
    			partInfo = new ReportProcessorPartInfo(IPartInfo.MIME_PREAMBLE, headers, mtstream.getInputStream(), mERDetails);
    			process(partInfo,sysout);
    			headers = null; partInfo = null;    			
    			break;
    			
    		case MimeTokenStream.T_EPILOGUE: 
    			partInfo = new ReportProcessorPartInfo(IPartInfo.MIME_EPILOGUE, headers, mtstream.getInputStream(), mERDetails);
    			process(partInfo,sysout);
    			headers = null; partInfo = null;
    			break;
    		default:
    		}
    	}
    	mPrePostProcessor.postProcess();
	
	}
	
	/**
	 * Locates and invokes the proper IPartsProcessor to taken on processing 
	 * of this MIME part
	 * @param mimePart MIME document part information
	 * @param writer   output print writer
	 * @throws IOException failure to read MIME part or write to output writer.
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	private void process(IPartInfo mimePart, PrintWriter writer) throws IOException, SAXException, ParserConfigurationException {		
	    String contentID = mimePart.getContentID();	    
		IPartsProcessor partProcessor = null;		
		if (contentID != null){
		    if(contentID.equals("<FirmwareVersion@datapower.ibm.com>")){
		        FirmwareInputStream fis = new FirmwareInputStream(mimePart.getBodyStream(), mERDetails);
		        mimePart.setInputStream(fis);
		    }		    
			partProcessor = (IPartsProcessor) mPartProcessors.get(contentID);
		}
		if (partProcessor == null){		    
			partProcessor = mDefaultPartProcessor;			
		}
		try {
			partProcessor.process(mimePart,writer);
		}
		finally {
			writer.flush();
		}
	}	

	@SuppressWarnings("unchecked")
	public ReportProcessor() {
		// initialize
		mReport = null;
		mPartProcessors = new HashMap();
		mERDetails = new ErrorReportDetails();
		
		// instantiate the default parts processor, does nothing
		mDefaultPartProcessor = new IPartsProcessor() {
			public void process(IPartInfo mimePart, PrintWriter writer) throws IOException {			    
			    String contentID = mimePart.getContentID();				    
			    if(contentID != null){
			        if(contentID.equals("<FirmwareVersion@datapower.ibm.com>")){			             
		                 InputStream is = mimePart.getBodyStream();
		                 byte[] b = new byte[4096];
		                 is.read(b);
			        }
	        
			    }
			}
		};
	}

	private InputStream mReport = null;
	private IPartsProcessor mDefaultPartProcessor = null;
	private ErrorReportDetails mERDetails = null;
	@SuppressWarnings("unchecked")
	private HashMap mPartProcessors;
	private OutputStream mOutputStream = null;
	private String mFilename = null;
	private PrePostReportProcessor mPrePostProcessor = null;
}
