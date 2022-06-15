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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.xml.sax.SAXException;

import com.ibm.datapower.er.Analytics.AnalyticsProcessor;
import com.ibm.datapower.er.Analytics.ERMimeSection;
import com.ibm.datapower.er.ERCommandLineArgs.CommandLineEvent;
import com.ibm.datapower.er.Transactions.ParseTransx;
import com.ibm.datapower.er.mgmt.ERXmlMgmt;
import com.ibm.datapower.er.mgmt.ERMgmtException;
import com.ibm.logging.icl.Level;
import com.ibm.logging.icl.Logger;
import com.ibm.logging.icl.LoggerFactory;

public final class ERTool implements Runnable, ERCommandLineArgs.CommandLineListener {
	ResourceBundle msgs;
	boolean explode;
	boolean generate;
	boolean capture;
	ArrayList<String> fileNames = new ArrayList<String>();
	String format;
	String prefix;
	String cid;
	String ipaddr;
	int port;
	String user;
	String password;
	String analyticsFile;
	String outFile = "";
	String printConditions = "";
	String transxTimeFormat = ""; // used for setting the EST/UTC conversion
	int	runFormulaMaxTimeSeconds = 0;
	boolean printTransactions = false;
	boolean printTransactionsInXML = false;
	boolean retrieveAllFiles = false;
	String logLevel = "info";
	InputStream in;
	ERFramework fm;
	boolean gui;
	// signifies the end of the usage entries in ERMessages.properties
	// (ERTOOL...I)
	private final int USAGE_PROPERTIES_END = 55;

	/**
	 * Constructor
	 **/
	ERTool(String[] args) throws IllegalArgumentException {
		// initialize class member variables
		explode = false;
		generate = false;
		capture = false;
		format = "";
		prefix = "";
		cid = "";
		in = null;
		ipaddr = null;
		port = 5550;
		user = "admin";
		password = "admin";
		analyticsFile = "";
		fm = new ERFramework(0);
		gui = false;

		if (args.length > 0 && args[0].equals("-gui")) {
			args[0] = ""; // this resets to blank so the command line just spits
							// out help with no error

			gui = true;

			args = GetGUIArgs();
		}

		// Get message strings from properties file
		msgs = ResourceBundle.getBundle("ERMessages");

		// parse the parameters provide including the cfg (if any)
		ERCommandLineArgs params = new ERCommandLineArgs(args);
		params.addCommandLineListener(this);
		String usage = "";
		for (int i = 1; i <= USAGE_PROPERTIES_END; i++) {
			Integer ii = new Integer(i);
			if (i < 10)
				usage += msgs.getString("ERTOOL00" + ii.toString() + "I");
			else
				usage += msgs.getString("ERTOOL0" + ii.toString() + "I");
			if (i < USAGE_PROPERTIES_END)
				usage += "\n";
		}
		params.setUsageText(usage);
		params.parse();
	}

	public String[] GetGUIArgs() {
		String[] args = null;

		erGUI gui = new erGUI();
		String errReport = gui.getErrorReportFileName();
		if (errReport != null && errReport.length() > 0) {
			transxTimeFormat = gui.getTransxTimeZone();
			printTransactions = gui.getTransactionEnabled();
			analyticsFile = gui.getAnalyticsFile();
			outFile = gui.getOutputFile();
			runFormulaMaxTimeSeconds = gui.getFormulaMaxRuntimeSeconds();
			retrieveAllFiles = gui.getRetrieveAllFiles();

			if (gui.getHTMLFormat())
				format = "HTML";
			else
				format = "TEXT";

			logLevel = gui.getLogLevel();

			args = new String[2];
			args[0] = "-file";
			args[1] = errReport;

			int endPos = errReport.lastIndexOf("/");
			if (endPos < 1)
				endPos = errReport.lastIndexOf("\\");

			String dirList = errReport.substring(0, endPos + 1);
			ArrayList<String> reports = gui.getReports();
			if (reports.size() > 1) {
				for(int i=0;i<reports.size();i++)
					fileNames.add(dirList + reports.get(i));
			}
			
			switch (gui.getPrintConditions()) {
			case SHOWALL: {
				printConditions = "showall";
				break;
			}
			case HIDEDEFAULT: {
				printConditions = "hidedefault";
				break;
			}
			case HIDEALL: {
				printConditions = "hideall";
				break;
			}
			}

		}

		return args;
	}

	/**
	 * Retrieve or format report as specified in command line
	 **/
	public void run() {

		// if appliance IP and port has been specified
		if (generate == true || capture == true) {
			try {
				// open xml mgmt interface
				ERXmlMgmt xml_mgmt = new ERXmlMgmt(ipaddr, port, user, password, null, true);

				// check for firmware 3.8.1
				if (xml_mgmt.is_3_8_1_or_later() == false) {
					// if not, exit - unsupported error report
					erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL003E", "3.8.1");
					return;
				}

				// if --generate option was issued
				if (generate == true) {
					// generate error report on the appliance
					xml_mgmt.generateErrorReport(true, true);
				}

				// get error report and return file location of error report
				String file = xml_mgmt.getErrorReport();
				erLogger.log(Level.INFO, ERTool.class, "run", "ERMessages", "ERTOOL018E", file + " created");
				fileNames.add(file);
			} catch (ERMgmtException e) {
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL004E", e.getMessage());
			} catch (NumberFormatException e) {
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL005E");
			}

		}

		// Explode report sections into individual files
		if (fileNames.size() > 0 && explode == true) {
			if (prefix.length() > 0) {
				fm.setFileLocation(fileNames.get(0));
				try {
					fm.expand(prefix, cid);
				} catch (Exception e) {
					erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL006E", e);
				}
			} else
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL019E");
		}

		ERFramework mainFramework = null;
		
		boolean ranAnalytics = false;
		// a analytics xml parser file was passed and we know the error report
		// location
		// lets do some xml parsing
		if (analyticsFile.length() > 0 && fileNames.size() > 0) {
			ranAnalytics = true;
			AnalyticsProcessor analytics = new AnalyticsProcessor();
			ArrayList<ERFramework> frameworks = new ArrayList<ERFramework>();

			for (int i = 0; i < fileNames.size(); i++) {
				ERFramework newfw = new ERFramework(i + 1);
				newfw.setFileLocation(fileNames.get(i));
				newfw.setRetrieveAllFiles(retrieveAllFiles);
				frameworks.add(newfw);
			}

			if (fileNames.size() == 1)
				frameworks.get(0).SetID(0);
			if ( fileNames.size() > 0 )
				mainFramework = frameworks.get(0);
			

			try {
				analytics.loadAndParse(analyticsFile, frameworks, true, format, outFile, printConditions, logLevel, runFormulaMaxTimeSeconds);
				if (gui && outFile.length() > 0) {
					File htmlFile = new File(outFile);

					try {
						// open the default web browser for the HTML page
						Desktop.getDesktop().browse(htmlFile.toURI());
					} catch (Exception ex) {
						erLogger.log(Level.WARNING, ERTool.class, "main", "ERMessages", "ERTOOL018E", "Opening "
								+ htmlFile.getName() + " in an application failed, must be opened manually.");
					}
				}
			} catch (IOException e) {
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL020E", e);
			} catch (SAXException e) {
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL020E", e);
			}
		}

		if (printTransactions) {
			if ( mainFramework == null || ( mainFramework != null && !mainFramework.IsPostMortem()) )
			{
				ParseTransx transx = new ParseTransx();
				transx.setFileLocation(fileNames.get(0));
				if (analyticsFile.endsWith("\\") || analyticsFile.endsWith("/"))
					transx.SetTransactionRulesFile(analyticsFile + "dptransx.xml");
				transx.doParse(outFile, transxTimeFormat, printTransactionsInXML, logLevel);
				transx = null;
				System.gc();
			}
		}
		
		// Details of specified section
		if (!ranAnalytics && fileNames.size() > 0 && cid.length() > 0) {
			fm.setFileLocation(fileNames.get(0));
			if (format.length() > 0) {
				try {
					fm.outputCid(format, cid, System.out);
				} catch (Exception e) {
					erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL007E", format + "\n" + e);
				}
			} else {
				try {
					ERMimeSection mime = fm.getCidAsInputStream(cid, false, 0, 0, false);
					if (mime != null)
						in = mime.mInput;
					else
						in = null;

					if (in != null) {
						showStream(in);
						in.close();
					} else
						erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL008E", cid);
				} catch (Exception e) {
					erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL009E", "\n" + e);
				}
			}
		}

		// None of the above, must be summary
		else if (!ranAnalytics && fileNames.size() > 0) {
			fm.setFileLocation(fileNames.get(0));
			try {
				if (format.length() == 0 && capture)
					; // Do not format captured file by default
				else if (format.length() == 0 && generate)
					; // Do not format generated file by default
				else if (format.equals("HTML"))
					fm.outputReport(format, System.out);
				else if (format.length() == 0 || format.equals("TEXT"))
					fm.outputReport(format, System.out);
				else
					erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL002E", format);
			} catch (Exception e) {
				erLogger.log(Level.ERROR, ERTool.class, "run", "ERMessages", "ERTOOL010E", "\n" + e);
			}
		}

	}

	/**
	 * Record command line options for later processing
	 **/
	public void performCommand(CommandLineEvent cle) {
		// -help Display command line help.
		if (cle.getSwitch().equals("-help")) {
			Usage();
		}

		// -explode <file prefix> write file for each section
		else if (cle.getSwitch().equals("-explode")) {
			prefix = cle.getSwitchValue();
			explode = true;
		}

		// -file <filename> parse a local error-report.
		else if (cle.getSwitch().equals("-file")) {
			String valFile = cle.getSwitchValue();

			if ( !fileNames.contains(valFile))
				fileNames.add(valFile);
			if (valFile.length() <= 0) {
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL001E");
			}
		} else if (cle.getSwitch().equals("-file2")) {
			String valFile = cle.getSwitchValue();

			if ( !fileNames.contains(valFile))
				fileNames.add(valFile);
			if (valFile.length() <= 0) {
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL001E");
			}
		}

		// -format HTML|TEXT|CSV output report format.
		else if (cle.getSwitch().equals("-format")) {
			format = cle.getSwitchValue();
			if (format.length() <= 0 || !(format.equals("HTML") || format.equals("TEXT") || format.equals("CSV")))
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL002E", format);
		} else if (cle.getSwitch().equals("-loglevel")) {
			logLevel = cle.getSwitchValue();
			logLevel = logLevel.toLowerCase();
			if (logLevel.length() <= 0
					|| !(logLevel.equals("info") || logLevel.equals("debug") || logLevel.equals("none")))
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL002E", logLevel);
		}

		// -xsl <format>,<cid>,<filename> XSL style sheet.
		else if (cle.getSwitch().equals("-xsl")) {
			String xslMatrix = cle.getSwitchValue();
			int pos;
			String xslPath;

			// Could be a complex matrix of style sheets separated by "!"
			do {
				pos = xslMatrix.indexOf("!");
				if (pos >= 0) {
					xslPath = xslMatrix.substring(0, pos);
					xslMatrix = xslMatrix.substring(pos + 1);
				} else
					xslPath = xslMatrix;
				processXslOption(xslPath);
			} while (pos >= 0);
		}

		// -section <content id> report on specified section.
		else if (cle.getSwitch().equals("-section")) {
			cid = cle.getSwitchValue();
			if (cid.length() <= 0)
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL011E");
		}

		// capture existing FFDC device created error-reports or
		// generate a new one using mustgather.
		else if (cle.getSwitch().equals("-generate") || cle.getSwitch().equals("-capture")) {

			// get ipaddr and maybe port number
			String ipaddr_port = cle.getSwitchValue();

			// if no value
			if (ipaddr_port.length() <= 0)
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL012E");

			// if a port number is specified
			if (ipaddr_port.contains(":")) {
				ipaddr = ipaddr_port.substring(0, ipaddr_port.indexOf(":"));
				if (ipaddr.length() <= 0) {
					erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL012E");
				}

				String sport = ipaddr_port.substring(ipaddr_port.indexOf(":") + 1);
				if (sport.length() <= 0) {
					erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL012E");
					port = Integer.parseInt(sport);
				}
			} else {
				// ip address was specified
				ipaddr = ipaddr_port;
			}

			// if generate was specified
			if (cle.getSwitch().equals("-generate"))
				generate = true;
			// otherwise it was capture
			else
				capture = true;
		}

		// specify username
		else if (cle.getSwitch().equals("-user")) {
			user = cle.getSwitchValue();
			if (user.length() <= 0)
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL013E");
		}

		// specify password
		else if (cle.getSwitch().equals("-password")) {
			password = cle.getSwitchValue();
			if (password.length() <= 0)
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL014E");
		}
		// -analyticsfile <filename> parse a local error-report.
		else if (cle.getSwitch().equals("-analyticsfile")) {
			analyticsFile = cle.getSwitchValue();
			if (analyticsFile.length() <= 0) {
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL001E");
			}
		}

		// -outfile <filename> writes output to file for analytics
		else if (cle.getSwitch().equals("-outfile")) {
			outFile = cle.getSwitchValue();
			if (outFile.length() <= 0) {
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL001E");
			}
		}
		// -printconditions allows showing results (showall), hiding all
		// (hideall) or (default) setting
		else if (cle.getSwitch().equals("-printconditions")) {
			printConditions = cle.getSwitchValue();
			if (printConditions.length() <= 0) {
				erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL001E");
			}
		}

		else if (cle.getSwitch().equals("-transactions")) {
			printTransactions = true;
		} else if (cle.getSwitch().equals("-transxml")) {
			printTransactionsInXML = true;
		} else if (cle.getSwitch().equals("-timeformat")) {
			transxTimeFormat = cle.getSwitchValue();
		}
		else if (cle.getSwitch().equals("-formulamaxrunseconds")) {
			try
			{
			runFormulaMaxTimeSeconds = Integer.parseInt(cle.getSwitchValue());
			}catch(Exception ex) { }
		}
		else if (cle.getSwitch().equals("-retrieveallfiles")) {
			int val = 0;
			try
			{
			val = Integer.parseInt(cle.getSwitchValue());
			}catch(Exception ex) { }
			
			if ( val == 1 )
				retrieveAllFiles = true;
			else
				retrieveAllFiles = Boolean.parseBoolean(cle.getSwitchValue());
		}
		// invalid option
		else {
			erLogger.log(Level.ERROR, ERTool.class, "performCommand", "ERMessages", "ERTOOL015E", cle.getSwitch());
			Usage();
		}
	}

	/*
	 * Process a single <format>,<cid>,<filename> XSL style sheet
	 */
	private void processXslOption(String xslPath) {
		// Could be just the style sheet file name
		if (xslPath.indexOf(",") < 0) {
			if (xslPath.length() <= 0)
				erLogger.log(Level.ERROR, ERTool.class, "processXslOption", "ERMessages", "ERTOOL016E");
			fm.setCidXslPath(xslPath);
		}

		// or format, cid, file name
		else {
			String xslFormat = xslPath.substring(0, xslPath.indexOf(","));
			String xslCid = xslPath.substring(xslFormat.length() + 1, xslPath.lastIndexOf(","));
			xslPath = xslPath.substring(xslPath.lastIndexOf(",") + 1);
			if (xslFormat.equals("*")) {
				fm.setCidXslPath("HTML", xslCid, xslPath);
				fm.setCidXslPath("TEXT", xslCid, xslPath);
				fm.setCidXslPath("CSV", xslCid, xslPath);
			} else if (xslFormat.equals("HTML"))
				fm.setCidXslPath(xslFormat, xslCid, xslPath);
			else if (xslFormat.equals("TEXT"))
				fm.setCidXslPath(xslFormat, xslCid, xslPath);
			else if (xslFormat.equals("CSV"))
				fm.setCidXslPath(xslFormat, xslCid, xslPath);
			else
				erLogger.log(Level.ERROR, ERTool.class, "processXslOption", "ERMessages", "ERTOOL002E", xslFormat);
		}

	}

	/**
	 * Main program - invokes constructor and run() method.
	 **/
	public static void main(String[] args) {
		ERTool ertool = null;

		try {
			ertool = new ERTool(args);
			ertool.run(); // normal processing
		} catch (IllegalArgumentException e) {
			erLogger.log(Level.ERROR, ERTool.class, "main", "ERMessages", "ERTOOL018E", e.getMessage());
			System.exit(1);
		}

		System.exit(0);

	}

	/**
	 * Display a returned stream to the console
	 **/
	private void showStream(InputStream in) {
		try {
			for (int ch = in.read(); ch >= 0; ch = in.read())
				System.out.write(ch);
		} catch (Exception e) {
			erLogger.log(Level.ERROR, ERTool.class, "showStream", "ERMessages", "ERTOOL017E");
		}
	}

	/**
	 * Display command line usage
	 **/
	private void Usage() {
		String usage = "";
		for (int i = 1; i <= USAGE_PROPERTIES_END; i++) {
			Integer ii = new Integer(i);
			if (i < 10)
				usage += msgs.getString("ERTOOL00" + ii.toString() + "I");
			else
				usage += msgs.getString("ERTOOL0" + ii.toString() + "I");
			if (i < USAGE_PROPERTIES_END)
				usage += "\n";
		}
		erLogger.log(Level.INFO, ERTool.class, "Usage", "ERMessages", "ERTOOL018E", usage);
	}

	public static Logger logger() {
		return erLogger;
	}

	private static final Logger erLogger = LoggerFactory.getLogger(ERTool.class.getPackage().getName());
}
