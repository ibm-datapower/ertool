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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.ibm.datapower.er.ERFramework;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.lang.Runtime;
import java.util.ArrayList;

public class TestAnalytics extends TestCase {

	String[] filesToTest = { "C:\\pmr's\\17897\\apr17\\error-report.txt.tmp" , "c:\\pmr's\\19068.L6Q.000.error-report.7830379.20150504114908440EDT.txt.gz.tmp"};
	  public static void printUsage(Runtime runtime) {
		    long total, free, used;

		    total = runtime.totalMemory();
		    free  = runtime.freeMemory();
		    used = total - free;

		    System.out.println("\nTotal Memory: " + total + "\t Used: " + used);
		    System.out.println("Percent Used: " + ((double)used/(double)total)*100 + 
		    		"\t Percent Free: " + ((double)free/(double)total)*100);
		  }
	  
	  public void testWriteCase()
	  {
		  	AnalyticsProcessor analytics = new AnalyticsProcessor();
		  	ERFramework framework = new ERFramework();

			for(int f=0;f<filesToTest.length;f++)
			{
				framework.setFileLocation(filesToTest[f]);
				System.out.println("\n- Building Test Cases: " + filesToTest[f]);
			try {
				ArrayList<ConditionsNode> results = analytics.loadAndParse("C:\\DPGithub\\Analytics.xml", framework, false, "txt", "", "");
				for (int i=0;i<results.size();i++)
				{
					ByteArrayOutputStream buffer = ObjectComparison.WriteObjectToBuffer(results.get(i));
					FileOutputStream outStream = new FileOutputStream("C:\\pmr's\\testcases\\testcase." + f + ".test." + i + ".bin");
					outStream.write(buffer.toByteArray());
					outStream.close();
				}
				Assert.assertTrue(true);
			} catch (Exception ex) { }
			}
			
	  }
	  
	  public void testReadCase()
	  {
		  	AnalyticsProcessor analytics = new AnalyticsProcessor();
		  	ERFramework framework = new ERFramework();

			for(int f=0;f<filesToTest.length;f++)
			{
				framework.setFileLocation(filesToTest[f]);
				System.out.println("\n- Running Test Cases: " + filesToTest[f]);
			try {
				ArrayList<ConditionsNode> results = analytics.loadAndParse("C:\\DPGithub\\Analytics.xml", framework, false, "txt", "", "");
				boolean compare = ObjectComparison.CompareObjectToFile(results, "C:\\pmr's\\44068\\testcase." + f + ".test");
				
				Assert.assertTrue(compare);
			} catch (Exception ex) { System.out.println(ex.getMessage());} 
			}
	  }
	  
	  
	  public void testOrderedAnalytics() throws SAXException
	  {

		    Runtime runtime;

		    runtime = Runtime.getRuntime();
			AnalyticsProcessor analytics = new AnalyticsProcessor();
			ERFramework framework = new ERFramework();

				framework.setFileLocation("C:\\pmr's\\93684\\93684.49R.000.error-report.6801337.20150204201852402PST.txt");
				long startTime = System.nanoTime();
				try {
					analytics.loadAndParse("C:\\DPGithub\\testsched.xml", framework, true, "txt", "", "");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Assert.assertTrue(false);
				}
				long endTime = System.nanoTime();
				double difference = (endTime - startTime)/1e6;
				System.out.println("took " + difference + " milliseconds to complete.");

		  
	  }

		public void testLoadAnalytics() throws SAXException
		{
		    Runtime runtime;

		    runtime = Runtime.getRuntime();
			AnalyticsProcessor analytics = new AnalyticsProcessor();
			ERFramework framework = new ERFramework();

			for(int i=0;i<filesToTest.length;i++)
			{
				framework.setFileLocation(filesToTest[i]);
				System.out.println("\n- File to test: " + filesToTest[i]);
				long startTime = System.nanoTime();
				try {
					analytics.loadAndParse("C:\\work\\DPGithub\\Analytics.xml", framework, false, "txt", "", "");
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Assert.assertTrue(false);
				}
				long endTime = System.nanoTime();
				double difference = (endTime - startTime)/1e6;
				System.out.println("- File: " + filesToTest[i] + " took " + difference + " milliseconds to complete.");

			   // printUsage(runtime);
			}
			
			Assert.assertTrue(true);
		}
		
		public void testDirectoryRun() throws SAXException
		{
		    Runtime runtime;

		    runtime = Runtime.getRuntime();
			AnalyticsProcessor analytics = new AnalyticsProcessor();
			ERFramework framework = new ERFramework();

	        File dir = new File("c:\\pmr's\\");

	        File[] files = dir.listFiles(new FilenameFilter() {
	               public boolean accept(File dir, String fileName) {
	                   if(fileName.contains("report"))
	                	   return true;
	                   else
	                	   return false;
	               }
	        });
	        
			for(int i=0;i<files.length;i++)
			{
				framework.setFileLocation(files[i].getPath());
				System.out.println("\n- File to test: " + files[i].getPath());
				long startTime = System.nanoTime();
				try {
					analytics.loadAndParse("C:\\DPGithub\\Analytics.xml", framework, false, "txt", "", "");
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Assert.assertTrue(false);
				}
				long endTime = System.nanoTime();
				double difference = (endTime - startTime)/1e6;
				System.out.println("- File: " + files[i].getPath() + " took " + difference + " milliseconds to complete.");

			   // printUsage(runtime);
			}
			
			Assert.assertTrue(true);
		}
	
}
