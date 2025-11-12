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

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.FontMetrics;

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

	private int maxFormulaRuntimeSec = 300;
	private boolean retrieveAllFiles = true;
	
	private boolean selectStage[] = { false /* 0) input file selected */, false /* 1) output file selected */};
	private Label errReportLbl = null, outputLbl = null, analyticsLbl = null;
	
	private double charWidthDiff = 0.0;
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

		GC gc = new GC(disp);
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		
		int charWidth = fontMetrics.getAverageCharWidth();
		shell.setText("ERGUI");
		if(charWidth < 9) {
			charWidth = 0;
		}
		charWidthDiff = (double)charWidth / 12.0;
		int sizeWidth = 685;
		int sizeHeight = 550;
		// set header of ui
		sizeWidth += (int)((double)sizeWidth*charWidthDiff);
		sizeHeight += (int)((double)sizeHeight*charWidthDiff);
		

		instantiateUI();

		shell.setSize(sizeWidth, sizeHeight);
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
	
	public int OffsetPixel(double val) {
		return (int)(val+charWidthDiff*val);
	}

	protected void createAnalyticsUI(TabFolder folder, TabItem itm) {
		final Composite ui = new Composite(folder, SWT.BORDER);
		itm.setControl(ui);

		errReportLbl = new Label(ui, SWT.NORMAL);
		errReportLbl.setText("Error Report: ??");

		analyticsLbl = new Label(ui, SWT.NORMAL);
		analyticsLbl.setText("Analytics File: " + getAnalyticsFile());
		analyticsLbl.setBounds(OffsetPixel(20.0), OffsetPixel(305.0), OffsetPixel(500.0), OffsetPixel(30.0));

		final Button errReportBtn = new Button(ui, SWT.PUSH);
		errReportBtn.setText("Browse Error Report");
		errReportBtn.setBounds(OffsetPixel(20.0), OffsetPixel(100.0), OffsetPixel(190.0), OffsetPixel(30.0));
		errReportBtn.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				displayInputFileDialog(e);
			}
		});

		outputLbl = new Label(ui, SWT.NORMAL);
		outputLbl.setText("Output File: ??");
		outputLbl.setBounds(OffsetPixel(20.0), OffsetPixel(275.0), OffsetPixel(500.0), OffsetPixel(30.0));

		final Button outputBtn = new Button(ui, SWT.PUSH);
		outputBtn.setText("Browse Output File");
		outputBtn.setBounds(OffsetPixel(20.0), OffsetPixel(140.0), OffsetPixel(190.0), OffsetPixel(30.0));
		outputBtn.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				displayOutputFileDialog(e);
			}
		});

		final Button analyticsBtn = new Button(ui, SWT.PUSH);
		analyticsBtn.setText("Browse Analytics XML (Opt.)");
		analyticsBtn.setBounds(OffsetPixel(20.0), OffsetPixel(180.0), OffsetPixel(190.0), OffsetPixel(30.0));
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
		radios[0].setBounds(OffsetPixel(10.0), OffsetPixel(35.0), OffsetPixel(140.0), OffsetPixel(30.0));
		radios[0].setSelection(true);

		radios[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				formatHTML = true;
			}
		});

		radios[1] = new Button(ui, SWT.RADIO);
		radios[1].setText("TEXT");
		radios[1].setBounds(OffsetPixel(10.0), OffsetPixel(60.0), OffsetPixel(140.0), OffsetPixel(30.0));

		radios[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				formatHTML = false;
			}
		});

		Button[] radioMatchResult = new Button[3];
		final Group resultGroup = new Group(ui, SWT.NULL);
		resultGroup.setText("Result Details (Debug Formulas)");
		resultGroup.setLayout(new RowLayout(SWT.VERTICAL));
		resultGroup.setLocation(OffsetPixel(235.0), OffsetPixel(35.0));
		resultGroup.setSize(OffsetPixel(225.0), OffsetPixel(150.0));

		radioMatchResult[0] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[0].setSelection(true);
		radioMatchResult[0].setText("No Matched Details");
		radioMatchResult[0].setSelection(true);

		radioMatchResult[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.HIDEALL;
			}
		});

		radioMatchResult[1] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[1].setText("All Matched Details");

		radioMatchResult[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.SHOWALL;
			}
		});

		radioMatchResult[2] = new Button(resultGroup, SWT.RADIO);
		radioMatchResult[2].setText("Default Matched Details");

		radioMatchResult[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				printConditions = PRINT_MET_CONDITIONS.HIDEDEFAULT;
			}
		});

		final Label logLevelLbl = new Label(resultGroup, SWT.NORMAL);
		logLevelLbl.setText("Log Level: ");

		final Combo comboLogLevel = new Combo(resultGroup, SWT.READ_ONLY);

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
		

		String runTimeOpts[] = { "300", "600", "900", "1200", "120", "60" };

		final Label formulaRunTimeLbl = CreateLabel(ui,"Max Formula Runtime(Seconds): ",OffsetPixel(235.0),OffsetPixel(195.0),OffsetPixel(175.0),OffsetPixel(30.0));

		final Combo formulaRunTimeCombo = CreateComboBox(ui,runTimeOpts,OffsetPixel(415.0),OffsetPixel(191.0),OffsetPixel(90.0),OffsetPixel(30.0));

		formulaRunTimeCombo.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				int itm = formulaRunTimeCombo.getSelectionIndex();
				maxFormulaRuntimeSec = Integer.parseInt(formulaRunTimeCombo.getItem(itm));
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int itm = formulaRunTimeCombo.getSelectionIndex();
				maxFormulaRuntimeSec = Integer.parseInt(formulaRunTimeCombo.getItem(itm));
			}
		});

		String booleanOpts[] = { "true", "false" };
		
		final Label retrieveFilesLbl = CreateLabel(ui,"Retrieve All Files: ",OffsetPixel(235.0),OffsetPixel(225.0),OffsetPixel(175.0),OffsetPixel(30.0));

		final Combo retrieveFilesCombo = CreateComboBox(ui,booleanOpts,OffsetPixel(415.0),OffsetPixel(220.0),OffsetPixel(90.0),OffsetPixel(30.0));

		retrieveFilesCombo.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				int itm = retrieveFilesCombo.getSelectionIndex();
				retrieveAllFiles = Boolean.parseBoolean(retrieveFilesCombo.getItem(itm));
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int itm = retrieveFilesCombo.getSelectionIndex();
				retrieveAllFiles = Boolean.parseBoolean(retrieveFilesCombo.getItem(itm));
			}
		});

		final Button runButton = new Button(ui, SWT.PUSH);
		runButton.setText("Run");
		runButton.setBounds(OffsetPixel(20.0), OffsetPixel(335.0), OffsetPixel(80.0), OffsetPixel(30.0));
		runButton.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (selectStage[0] == false )
				{
					JOptionPane.showMessageDialog(null, "You must select an input file (error report, post mortem, etc.)", "Hold on!", JOptionPane.INFORMATION_MESSAGE);
					displayInputFileDialog(null);
				}
				if (selectStage[1] == false)
				{
					JOptionPane.showMessageDialog(null, "You must select an output file (destination directory and html/txt filename)", "Hold on!", JOptionPane.INFORMATION_MESSAGE);
					displayOutputFileDialog(null);
				}
				
				if ( selectStage[0] && selectStage[1] )
					shell.close();
				else
					JOptionPane.showMessageDialog(null, "Either an input or output file was not selected, run aborted.", "ERROR!", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	
	private Label CreateLabel(Composite ui, String labelText, int x, int y, int width, int height)
	{
		Label lbl = new Label(ui, SWT.NORMAL);
		lbl.setText(labelText);
		lbl.setBounds(x,y, width, height);
		return lbl;
	}
	
	private Combo CreateComboBox(Composite ui, String[] opts, int x, int y, int width, int height)
	{
		Combo comboBox = new Combo(ui, SWT.READ_ONLY);

		comboBox.setBounds(x,y,width,height);

		for (int idx = 0; idx < opts.length; idx++) {
			comboBox.add(opts[idx]);
		}

		comboBox.select(0);
		return comboBox;
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
		radios[0].setBounds(OffsetPixel(10.0), OffsetPixel(35.0), OffsetPixel(140.0), OffsetPixel(30.0));
		radios[0].setSelection(true);

		radios[0].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				transxEnabled = true;
			}
		});

		radios[1] = new Button(ui, SWT.RADIO);
		radios[1].setText("Off");
		radios[1].setBounds(OffsetPixel(10.0), OffsetPixel(60.0), OffsetPixel(140.0), OffsetPixel(30.0));

		radios[1].addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				transxEnabled = false;
			}
		});

		final Label outputLbl = new Label(ui, SWT.NORMAL);
		outputLbl.setText("Time Zone:");
		outputLbl.setBounds(OffsetPixel(40.0), OffsetPixel(105.0), OffsetPixel(80.0), OffsetPixel(30.0));

		final Combo combo = new Combo(ui, SWT.READ_ONLY);

		combo.setBounds(OffsetPixel(135.0), OffsetPixel(105.0), OffsetPixel(140.0), OffsetPixel(30.0));

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
	
	private void displayInputFileDialog(MouseEvent e)
	{
		FileDialog fd = new FileDialog(shell, SWT.MULTI);
		fd.setText("Open");
		fd.setFilterPath("C:/");
		String[] filterExt = { "*.txt;*.txt.gz;*.tar.gz;*.tgz;*.tar;*.zip", "*.txt.gz", "*.tar.gz", "*.tgz", "*.tar", "*.zip", "*.*" };
		fd.setFilterExtensions(filterExt);
		String newErrReport = fd.open();
		
		if ( newErrReport != null && newErrReport.length() > 0 )
		{
			selectStage[0] = true;
			errorReport = newErrReport;
		}

		reports.clear(); // we don't append they have to select all
							// on one sweep

		String[] files = fd.getFileNames();
		for (int i = 0; i < files.length; i++)
			reports.add(files[i]);
		
		if (errorReport != null && errorReport.length() > 0) {
			if (!analyticFilePrevSet)
			{
				REPORT_TYPE outType = AnalyticsProcessor.detectReportType(errorReport);
				String fileDir = AnalyticsProcessor.getReportRulesFile(outType, "autodetect");
				analyticsFile = fileDir;
				analyticsLbl.setText("Analytics File: " + analyticsFile);
			}
			errReportLbl.setText("Error Report: " + errorReport);
		}
	}
	
	private void displayOutputFileDialog(MouseEvent e)
	{
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
		{
			selectStage[1] = true;
			outFile = newFile;
			outputLbl.setText("Output File: " + outFile);
		}
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
	
	public int getFormulaMaxRuntimeSeconds() {
		return maxFormulaRuntimeSec;
	}
	
	public boolean getRetrieveAllFiles() {
		return retrieveAllFiles;
	}
}
