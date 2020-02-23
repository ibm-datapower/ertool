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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import com.ibm.datapower.er.mgmt.Base64;



public class PartsProcessorsHTML extends PartsProcessorsXForm
{
    public PartsProcessorsHTML()
    {
        super("src/erHTML.xsl");
    }

    public void process(IPartInfo mimePart, PrintWriter writer) throws IOException 
    {
        String sectionName = mimePart.getContentID();        
        String contentType = mimePart.getContentType();
        String encoding = mimePart.getContentTransferEncoding();
                

        if (sectionName != null && sectionName.indexOf('<') != -1) {
            sectionName = sectionName.substring(sectionName.indexOf('<') + 1);
            sectionName = sectionName.substring(0, sectionName.indexOf('@'));		
        }
        if (mimePart.getType() == IPartInfo.MIME_BODYPART) {
            writer.print("<br/><br/>");
            writer.println("<span class=\"element\">");
            writer.println("<a name=\"" + sectionName + "\">");
            writer.println(sectionName);
            writer.println("</a>");
            writer.println("</span>");
                       

            if (contentType.startsWith("text/xml") || contentType.startsWith("application/xop+xml")) {
                super.process(mimePart,writer);
            } else if (encoding.indexOf("base64") >= 0){
                Properties configproperties = new Properties();
                configproperties.load(new FileInputStream("properties/config.properties"));   
                writer.print("<br><a href=\"getbinaryfile.cgi?filename="+ binaryBody(mimePart, writer, "", true, false) + "\"");
                //writer.print("<br><a href=\"framework/tmp/" + binaryBody(mimePart, writer) + "\"");
                writer.print("\">click here to download binary from external server (only if running from erweb)</a>");
            } else {
                writer.println("<pre>");
                nonXMLBody(mimePart, writer);
                writer.println("</pre>");
            }
        }
        else if (mimePart.getType() == IPartInfo.MIME_PREAMBLE) {
            preamble(writer);
        }
        else if (mimePart.getType() == IPartInfo.MIME_EPILOGUE) {
            epilogue(writer);
        }
        else {
            throw new IllegalArgumentException("MIME Part Type unexpected: " + mimePart.getType());
        }
    }

    private void preamble(PrintWriter writer)
    {
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Error Report</title>");
        writer.println("  <meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"/>");
        writer.print("  <style>");
        writer.print("body      { background-color: #FFFFFF; }");
        writer.print(".element  { color: #5B7A9D; font-size:150%}");
        writer.print(".text     { color: #586AAD; }");
        writer.print("th {background-color:#4477bb}");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
    }

    private void epilogue(PrintWriter writer)
    {
        writer.println("</body>");
        writer.println("</html>");
    }

    private void nonXMLBody(IPartInfo mimePart, PrintWriter writer) throws IOException 
    {
        String contentEnc = mimePart.getContentTransferEncoding();
        InputStream in = mimePart.getBodyStream();

        if (contentEnc != null && contentEnc.equalsIgnoreCase("base64")) {
            //outputHex(new Base64.InputStream(in), writer);
            //outputBinary(in, writer);
        }
        else {
            for (int ch = in.read(); ch >= 0; ch = in.read())
                writer.write(ch);
        }


    }
	
    public static String binaryBody(IPartInfo mimePart, PrintWriter writer, String optDir, boolean base64Encode, boolean lineReturn) throws IOException {
        InputStream in = mimePart.getBodyStream();
        
        if ( in == null )
        	return "";
        
        String nextLine;
        String extension = "";
        String contentID = mimePart.getContentID();
        String filename = "Base64File";
        if(contentID != null){
            filename = contentID.substring(contentID.indexOf('<')+1, contentID.indexOf('@'));
        }

        File directory = null;
        
        if ( optDir.length() > 0 )
        	directory = new File(optDir);
        else
        {
        	directory = new File(System.getProperty("user.dir"));
            directory = new File((directory.getAbsolutePath()).concat(File.separator).concat("tmp"));
        }
        
        if(!directory.exists()){
            directory.mkdir();
        }
        if(contentID.indexOf("BackgroundPacketCapture") >= 0){
            extension = ".pcap";
        }
        
		File temp = File.createTempFile(filename, extension, directory);
		try {

			OutputStream s = Files.newOutputStream(temp.toPath(), StandardOpenOption.APPEND);
			BufferedReader bis = new BufferedReader(new InputStreamReader(in));
			String sep = System.getProperty("line.separator");

			while ((nextLine = bis.readLine()) != null) {
				if (!base64Encode) // if base64Encode is true we leave the base64 encoding, otherwise we remove it
				{
					byte[] bytes = Base64.Decode(nextLine);
					s.write(bytes, 0, bytes.length);

					if (lineReturn)
						s.write(sep.getBytes(), 0, sep.getBytes().length);
				} else {
					s.write(nextLine.getBytes(), 0, nextLine.getBytes().length);

					if (lineReturn)
						s.write(sep.getBytes(), 0, sep.getBytes().length);
				}
			}
			s.close();
		} catch (Exception ex) {
			temp.delete();
		}
        //return the filename for the link
        return temp.getName();
//        return temp.getAbsolutePath();
        
    }

    private void outputHex(InputStream in, PrintWriter writer) throws IOException  
    {
        int line[] = new int[16];
        int count = 0;
        int i,j;
        String hex;
        int addr = 0;

        //  Process each character in input stream
        for (int ch = in.read();; ch = in.read()) 
        {
            if (ch >= 0)
                line[count++] = ch; 

            //  write a line if at 16 bytes or end of stream
            if (count >= 16 || ch < 0)
            {
                //  Limit HTML dump
                if (addr>0x800)
                {
                    writer.println("\nHTML formatting truncated.  To see full section, format as TEXT.");
                    break;
                }
                //  8 digit address
                hex = Integer.toHexString(addr);
                addr += 16;
                for (j=hex.length()-8; j<hex.length(); j++)
                    if (j<0)
                        writer.write((int) '0');
                    else
                        writer.write((int) hex.codePointAt(j));
                writer.write((int) ':');

                //  Hex data
                for (i=0; i<16; i++)
                {
                    writer.write((int) ' ');
                    if (i<count)
                        hex = Integer.toHexString(line[i]);
                    else
                        hex = "  ";
                    for (j=hex.length()-2; j<hex.length(); j++)
                        if (j<0)
                            writer.write((int) '0');
                        else
                            writer.write((int) hex.codePointAt(j));
                }
                writer.write((int) ' ');
                writer.write((int) ' ');

                //  character data (if in printable range)
                for (i=0; i<count; i++)
                {
                    if (line[i]==0x3c)  //  HTML tags will confuse browser
                        writer.write("&lt;");
                    else if (line[i] >= 0x20 && line[i] <= 0x7E)
                        writer.write(line[i]);
                    else
                        writer.write((int) '.');
                }
                writer.write((int) '\n');
                count = 0;
            }
            if (ch < 0) break;
        }
        writer.println("");
    }


}
