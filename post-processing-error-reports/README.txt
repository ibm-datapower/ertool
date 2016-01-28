IBM WebSphere DataPower Appliances
Copyright IBM Corporation 2012

README

Datapower Datapower Error Report Post-Processing Tool.

errReportPostProcessing.pl
----------------
The errReportPostProcessing.pl is a Perl script that appends English text
to each line of the error report in default-log section for a given non-English error report.

Prerequisites
----------------
Before you can run this post-processing tool, perl model will need to be 
installed on your system. Perl can run on both Linux (Unix) systems and 
Windows (Win32).  The latest version of Perl can be downloaded from perl.org.

RUN
---
Run the script as follows
./errReportPostProcessing.pl <directory> <input-file-path> <output-file-path>

<directory>
	The path for the directory containing the English msgcat XML files
<input-file-path>
	The file path for the non-English error report file(.txt)
<output-file-path>
	The file path for the output error report file
	
It will create a new error report file(Path:<output-file-path>) with English text appended in default-log section. 

Example
---
./errReportPostProcessing.pl <dp_source_root>/datapower/msgcat/xml /dp/error-report.txt /dp/error-report-postprocessed.txt



