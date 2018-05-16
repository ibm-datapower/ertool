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

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.ibm.datapower.er.Analytics.AnalyticsProcessor;
import com.ibm.datapower.er.Analytics.AnalyticsProcessor.PRINT_MET_CONDITIONS;
import com.ibm.datapower.er.Analytics.AnalyticsProcessor.REPORT_TYPE;

public class erGUI {
	private Display disp;
	private Shell shell;
	private String errorReport;
	private ArrayList<String> reports = new ArrayList<String>();
	private String outFile;
	private boolean formatHTML;
	private String analyticsFile;
	private boolean analyticFilePrevSet;
	private PRINT_MET_CONDITIONS printConditions;
	private String transxTimeZone;
	private boolean transxEnabled;

	private String logLevel;
	protected String[] logLevels = { "info", "debug", "none" };

	public erGUI() {
		// set defaults
		formatHTML = true;
		errorReport = "";
		analyticsFile = "";
		analyticsFile = getAnalyticsFile();
		outFile = "";
		analyticFilePrevSet = false;
		printConditions = PRINT_MET_CONDITIONS.HIDEALL;
		transxEnabled = true;
		transxTimeZone = "EST";
		logLevel = "info";

		// create display and shell for gui
		disp = new Display();
		shell = new Shell(disp);

		// set header of ui
		shell.setText("ERGUI");

		instantiateUI();

		shell.setSize(550, 425);
		shell.setLocation(300, 300);

		shell.open();

		while (!shell.isDisposed()) {
			if (!disp.readAndDispatch()) {
				disp.sleep();
			}
		}

		disp.dispose();
	}

	protected TabFolder InstTabFolder(int width, int height) {
		final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);

		tabFolder.setSize(width, height);

		return tabFolder;
	}

	protected TabItem InstTabItem(TabFolder folder, String title) {
		final TabItem tabItem = new TabItem(folder, SWT.NULL);
		tabItem.setText(title);
		return tabItem;
	}

	public void instantiateUI() {
		final TabFolder folder = InstTabFolder(1600, 1200);
		final TabItem analyticsItm = InstTabItem(folder, "Analytics");
		createAnalyticsUI(folder, analyticsItm);
		final TabItem transxItm = InstTabItem(folder, "Transactions");
		createTransactionsUI(folder, transxItm);
	}

	protected void createAnalyticsUI(TabFolder folder, TabItem itm) {
		final Composite ui = new Composite(folder, SWT.BORDER);
		ui.setLayout(new FillLayout());
		itm.setControl(ui);

		final Label errReportLbl = new Label(ui, SWT.NORMAL);
		errReportLbl.setText("Error Report: ??");
		errReportLbl.setBounds(20, 220, 500, 30);

		final Label analyticsLbl = new Label(ui, SWT.NORMAL);
		analyticsLbl.setText("Analytics File: " + getAnalyticsFile());
		analyticsLbl.setBounds(20, 280, 500, 30);

		final Button errReportBtn = new Button(ui, SWT.PUSH);
		errReportBtn.setText("Browse Error Report");
		errReportBtn.setBounds(20, 100, 190, 30);
		errReportBtn.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(shell, SWT.MULTI);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.txt;*.txt.gz;*.tar.gz;*.tgz;*.tar;*.zip", "*.txt.gz", "*.tar.gz", "*.tgz", "*.tar", "*.zip", "*.*" };
				fd.setFilterExtensions(filterExt);
				String newErrReport = fd.open();
				
				if ( newErrReport != null )
					errorReport = newErrReport;

				reports.clear(); // we don't append they have to select all
									// on one sweep

				String[] files = fd.getFileNames();
				for (int i = 0; i < files.length; i++)
					reports.add(files[i]);
				
				if (errorReport != null && !analyticFilePrevSet) {
					REPORT_TYPE outType = AnalyticsProcessor.detectReportType(errorReport);
					String fileDir = AnalyticsProcessor.getReportRulesFile(outType, "autodetect");
					analyticsFile = fileDir;
					analyticsLbl.setText("Analytics File: " + analyticsFile);
				}

				errReportLbl.setText("Error Report: " + errorReport);
			}
		});

		final Label outputLbl = new Label(ui, SWT.NORMAL);
		outputLbl.setText("Output File: ??");
		outputLbl.setBounds(20, 250, 500, 30);

		final Button outputBtn = new Button(ui, SWT.PUSH);
		outputBtn.setText("Browse Output File");
		outputBtn.setBounds(20, 140, 190, 30);
		outputBtn.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setText("Open");
				fd.setFilterPath("C:/");

				String[] filterExtHtml = { "*.html", "*.*" };
				String[] filterExtTxt = { "*.txt", "*.*" };
				if (formatHTML)
					fd.setFilterExtensions(filterExtHtml);
				else
					fd.setFilterExtensions(filterExtTxt);

				String newFile = fd.open();
				if ( newFile != null )
					outFile = newFile;
				
				outputLbl.setText("Output File: " + outFile);
			}
		});

		final Button analyticsBtn = new Button(ui, SWT.PUSH);
		analyticsBtn.setText("Browse Analytics XML (Opt.)");
		analyticsBtn.setBounds(20, 180, 190, 30);
		analyticsBtn.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.xml", "*.*" };
				fd.setFilterExtensions(filterExt);
				String newFile = fd.open();
				if ( newFile != null )
					analyticsFile = newFile;
				
				analyticsLbl.setText("Analytics File: " + analyticsFile);
				if (analyticsFile != null)
					analyticFilePrevSet = true;

			}
		});

		Button[] radios = new Button[2];

		radios[0] = new Button(ui, SWT.RADIO);
		radios[0].setSelection(true);
		radios[0].setText("HTML");
		radios[0].setBounds(10, 35, 140, 30);
		radios[0].setSelection(true);

		radios[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				formatHTML = true;
			}
		});

		radios[1] = new Button(ui, SWT.RADIO);
		radios[1].setText("TEXT");
		radios[1].setBounds(10, 60, 140, 30);

		radios[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				formatHTML = false;
			}
		});

		Button[] radioMatchResult = new Button[3];
		final Group resultGroup = new Group(ui, SWT.NULL);
		resultGroup.setText("Result Details (Debug Formulas)");
		resultGroup.setLayout(new RowLayout(SWT.VERTICAL));
		resultGroup.setLocation(235, 35);
		resultGroup.setSize(225, 150);

		radioMatchResult[0] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[0].setSelection(true);
		radioMatchResult[0].setText("No Matched Details");
		radioMatchResult[0].setBounds(15, 55, 200, 30);
		radioMatchResult[0].setSelection(true);

		radioMatchResult[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.HIDEALL;
			}
		});

		radioMatchResult[1] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[1].setText("All Matched Details");
		radioMatchResult[1].setBounds(15, 80, 200, 30);

		radioMatchResult[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.SHOWALL;
			}
		});

		radioMatchResult[2] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[2].setText("Default Matched Details");
		radioMatchResult[2].setBounds(15, 105, 200, 30);

		radioMatchResult[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.HIDEDEFAULT;
			}
		});

		final Label logLevelLbl = new Label(resultGroup, SWT.NORMAL);
		logLevelLbl.setText("Log Level: ");
		logLevelLbl.setBounds(20, 25, 80, 30);

		final Combo comboLogLevel = new Combo(resultGroup, SWT.READ_ONLY);

		comboLogLevel.setBounds(105, 20, 90, 30);

		for (int idx = 0; idx < logLevels.length; idx++) {
			comboLogLevel.add(logLevels[idx]);
		}

		comboLogLevel.select(0);

		comboLogLevel.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				int itm = comboLogLevel.getSelectionIndex();
				logLevel = logLevels[itm];
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int itm = comboLogLevel.getSelectionIndex();
				logLevel = logLevels[itm];
			}
		});

		final Button runButton = new Button(ui, SWT.PUSH);
		runButton.setText("Run");
		runButton.setBounds(20, 310, 80, 30);
		runButton.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				shell.close();
			}
		});
	}

	protected String[] timeZones = { "EST", "MIT", "HST", "AST", "PST", "MST", "CST", "IET", "PRT", "CNT", "AGT", "BET",
			"GMT", "UTC", "WET", "CET", "ECT", "MET", "ART", "CAT", "EET", "EAT", "NET", "PLT", "IST", "BST", "VST",
			"CTT", "PRC", "JST", "ROK", "ACT", "AET", "NST" };

	protected void createTransactionsUI(TabFolder folder, TabItem itm) {
		final Composite ui = new Composite(folder, SWT.BORDER);
		itm.setControl(ui);

		Button[] radios = new Button[2];

		radios[0] = new Button(ui, SWT.RADIO);
		radios[0].setSelection(true);
		radios[0].setText("On");
		radios[0].setBounds(10, 35, 140, 30);
		radios[0].setSelection(true);

		radios[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				transxEnabled = true;
			}
		});

		radios[1] = new Button(ui, SWT.RADIO);
		radios[1].setText("Off");
		radios[1].setBounds(10, 60, 140, 30);

		radios[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				transxEnabled = false;
			}
		});

		final Label outputLbl = new Label(ui, SWT.NORMAL);
		outputLbl.setText("Time Zone:");
		outputLbl.setBounds(40, 105, 80, 30);

		final Combo combo = new Combo(ui, SWT.READ_ONLY);

		combo.setBounds(135, 105, 140, 30);

		for (int idx = 0; idx < timeZones.length; idx++) {
			combo.add(timeZones[idx]);
		}

		combo.select(0);

		combo.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				int itm = combo.getSelectionIndex();
				transxTimeZone = timeZones[itm];
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int itm = combo.getSelectionIndex();
				transxTimeZone = timeZones[itm];
			}
		});
	}

	public String getErrorReportFileName() {
		return errorReport;
	}

	public ArrayList<String> getReports() {
		return reports;
	}

	public String getOutputFile() {
		return outFile;
	}

	public String getAnalyticsFile() {
		String current = "";
		if (analyticsFile.length() < 1) {
			try {
				current = new java.io.File(".").getCanonicalPath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return current + "\\Analytics.xml";
		} else
			return analyticsFile;
	}

	public boolean getHTMLFormat() {
		return formatHTML;
	}

	public PRINT_MET_CONDITIONS getPrintConditions() {
		return printConditions;
	}

	public String getTransxTimeZone() {
		return transxTimeZone;
	}

	public boolean getTransactionEnabled() {
		return transxEnabled;
	}

	public String getLogLevel() {
		return logLevel;
	}
}
