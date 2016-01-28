#!/usr/bin/perl

use CGI ;
use strict;
use POSIX qw( WNOHANG );
use CGI::Carp qw(fatalsToBrowser);
use Socket;

my $cgi = new CGI;

my $java_path = "./framework/";
my $upload_path = "./uploaded/";
my $report_file;
my $process_type;
my $filecontents;
my $headercontents;
my $pageVisits;
my $filesProcessed = 0;
my $output_buffer = "";
my $resultfh;
my $headerfh;
my $header_buffer = "";


&main();


sub main{    
    $report_file = $cgi->param("filename");
    $process_type = $cgi->param('process_type');
    $filecontents = $cgi->param('contents');
    
    #request was passed with streamed filecontents
    if (defined($filecontents)) {
          save_stream($filecontents);
    } #request passed via commandline 
    elsif($ARGV[$0] eq "-p"){
        $filecontents = "";
        while(<STDIN>){
	    my $line = $_;
	    chomp ($line);
	    $filecontents = $filecontents . $line . "\n";
        }
        $process_type = "backtraceOnly";
        save_stream($filecontents);
    } #request was made through webupload
      elsif($cgi->param){
          save_file()
    } 
    #no error report request was made, print the webupload page
    else {
        &mainUploadPage();
    }
}

####################################################################
###  Print the main upload page
####################################################################
sub mainUploadPage($){
    print $cgi->header(-type=>'text/html');
    print $cgi->start_html(-title=>'Upload an Error Report');

    &updatePageVisit();
    &updateUniqueErrorReports();
    &printInst();

    print $cgi->start_multipart_form(-action=>'ersite.cgi', -method=>"POST", id=>"myform");
    print $cgi->input({-name=>'filename', -type=>'file', -size=>'40'}), '<br>', "\n", 
          $cgi->radio_group(-name=>'process_type', -values=>['fullHTML','backtraceOnly', 'zip'], -default=>'fullHTML', -linebreak=>'true', -labels=>{fullHTML => 'Full HTML Error Report', backtraceOnly => 'Decode Backtrace Only', zip=>'Zip file'}),
          $cgi->input({-type=>"submit", -value=>"Submit", -id=>"submitbutton", -onclick=>""}), "\n";

   print $cgi->end_multipart_form;
   
   print $cgi->center("$filesProcessed unique error reports have been processed. ");


   print $cgi->end_html;
}

####################################################################
#
#  Print the instructions for uploading the error report
#
####################################################################
sub printInst(){
 print $cgi->h3("Upload an Error Report for Processing");

 print $cgi->p("Upload an error report to either view just the decoded backtrace, or the entire error report in a formatted HTML page.");
 print $cgi->start_ul;
 print $cgi->li("Full HTML Error Report: processes a text only error report into a conveniently indexed html page with decoded backtrace", "\n\n");
 print $cgi->li("Backtrace Only: will process the backtrace only and present the textual output", "\n\n");
 print $cgi->li("Zip file: will break up the error report and package all parts into a zip file for download", "\n\n");
 print $cgi->end_ul;
 print $cgi->p("Steps to upload:");
 print $cgi->start_ol;
 print $cgi->li("Select Browse to browse for the error report on your system. ");
 print $cgi->li("Choose which mode to process the report");
 print $cgi->li("Click Submit and wait a few moments");
 print $cgi->li("View and download your processed error report");
 print $cgi->end_ol;
 print $cgi->p("Requirements");
 print $cgi->start_ul;
 print $cgi->li("<b>Error report must be from firmware version 3.8.1 and above</b>", "\n\n");
 print $cgi->li("<b>You must submit an entire error report, not just a backtrace file</b>", "\n\n");
 print $cgi->end_ul;
 print $cgi->p("This is an experimental service based on the ERFramework and ISA Plugin technology  <br>Please send any comments, issues, problems, or suggestions to William or Alex at ateyssa\@us.ibm.com");
 print $cgi->p("");

}

###############################################################################################
#                                                                                             #
# save an error report streamed from either bugzilla (for example) or the command line        #
#                                                                                             #
###############################################################################################
sub save_stream(){
    #save file in uploaded/ directory as 
    my $filename = "streamed-error-report.".int(rand(100)) .  "txt";
    my $contents = $_[0];

#make directory unless it already exists
mkdir "$upload_path", 0777 unless -d "$upload_path";


    my $file = "$upload_path$filename";
    open (OUTFILE, ">", "$file") or do {errorResult("Couldn't open $file for writing"); die "Couldn't open $file for writing: $!";};
    print OUTFILE $contents;
    close OUTFILE;
    &processFile($file);
}

###############################################################################################
#                                                                                             #
# save an error report uploaded from the webinterface into the uploaded/ directory            #
#                                                                                             #    
###############################################################################################
sub save_file() {
    my ($bytesread, $buffer);
    my $numbytes = 1024;
    my $totalbytes = 0;
    my $filename = $cgi->upload('filename');
    my $untainted_filename;
   
    if (!$filename) {
        print $cgi->p('You must enter a filename before you can upload it');
	exit;
    }
    # Untaint $filename
    if ($filename =~ /^([-\@:\/\\\w.\(\)]+)$/) {
            $untainted_filename = $1;
    } else {
        errorResult("Invalid filename: $filename.");       
        die "Invalid filename: $filename."
    }

    if ($untainted_filename =~ m/\.\./) {
        errorResult("Invalid filename: $filename");
        die "Invalid filename: $filename"
    }
    my $file = "$upload_path$untainted_filename";

   open (OUTFILE, ">", "$file") or do {errorResult("Couldn't open $file for writing"); die "Couldn't open $file for writing: $!";};

    while ($bytesread = read($filename, $buffer, $numbytes)) {
         $totalbytes += $bytesread;
         print OUTFILE $buffer;
    }
    close OUTFILE;

    if((!defined($bytesread)) or (!defined($totalbytes))){       
        errorResult("Error reading file");
        die "Error reading file";
    }else{
         &processFile($file);
    }

    
}
###############################################################################################
#                                                                                             #
# process the file given by the argument.                                                     #
# Will fork in order to process the report and write to apache log (this prevents timeouts)   #
#                                                                                             # 
#                                                                                             #    
###############################################################################################
sub processFile(){
    chdir($java_path) or do {errorResult("error attempting to change directory"); die "Cant chdir to $java_path $!";};
    my $file = $_[0];
    if (my $pid = fork ) {  # parent
       # wait for child to finish while writing to apache log
        while (1) {
            warn("keeping script alive....");
            last if (waitpid($pid, WNOHANG) > 0);
            sleep(2);
        }
    } else { # child
       # perform time consuming computation here.
        open $resultfh, ">", \$output_buffer or die "could not open buffer";
        if($process_type eq "backtraceOnly"){
            backtraceOnly($file);
        }elsif($process_type eq "fullHTML"){
            fullHTML($file);
        }elsif($process_type eq "zip"){
            zipFile($file);	      
        }
    }


    chdir("../") or do {errorResult("error attempting to change directory"); die "Cant chdir to ../ $!";};  
    print $output_buffer;
}

###############################################################################################
#                                                                                             #
# process the the file into a zip file                                                        #
#                                                                                             #    
###############################################################################################
sub zipFile($){
    my $file = $_[0];
    my $process_property_file = "./properties/zipfile.properties";
    my $filename = substr($file, index($file, "/")+1);
    my $result = `./ertool -i ../$file -z -p $process_property_file 2>&1`;

    if($? != 0){
        &logData($file, $result, $?);
        &errorResult($result);
        return;
    }

    &logData($file, $result);
    
    print $resultfh $cgi->header(-type=>"application/x-zip; name=\"$filename.zip\"", -content_disposition=> "attachment; filename=\"$filename.zip\""). "\r\n";
    print $resultfh <<ZIPDATA 
    $result
ZIPDATA
}


###############################################################################################
#                                                                                             #
# process the the file into html output                                                       #
#                                                                                             #    
###############################################################################################
sub fullHTML($){
    my $file = $_[0];
    my $process_property_file = "./properties/htmldefault.properties";
    my $result = `./ertool -i ../$file -p $process_property_file`;
    if($? != 0){
        &logData($file, $result, $?);
        &errorResult($result);
        return;
    }

    &logData($file, $result);

    print $resultfh $cgi->header(-type=>'text/html')."\n";
    print $resultfh <<ENDHTML;
    $result
ENDHTML


}

###############################################################################################
#                                                                                             #
# process the file to output only the backtrace                                               #
#                                                                                             #    
###############################################################################################
sub backtraceOnly($){
    my $file = $_[0];
    my $process_property_file = "./properties/backtraceonly.properties";
    my $result = `./ertool -i ../$file -p $process_property_file`;
    if($? != 0){
        &logData($file, $result, $?);
        &errorResult($result);
        return;
    }


    &logData($file, $result);


    print $resultfh $cgi->header(-type=>'text/plain')."\n";
    print $resultfh <<ENDHTML;
    $result
ENDHTML

}

sub errorResult(){
    print $cgi->header(-type=>'text/html');
    print $cgi->start_html(-title=>'Error');
    print $cgi->h3("Error in processing your request.");
    if(defined($_[0])){
          print "The following error has occurred: <br> <pre>";
          print "$_[0]";
          print "</pre> <br><br>";
    }
    print "There has been an error processing your report. Please click ",$cgi->a( {-href=>"ersite.cgi"}, "here"), " to return and try again<br><br>"; 
    print $cgi->p("Information about this error has been logged");   
    print $cgi->end_html; 
}

sub logData(){

#    ----------------------------------------------
#    datetimestamp   -  19Apr11 20:25:35 
#    ipaddress       -  10.126.24.9
#    hostname        -  dp.swg.usma.ibm.com
#    fileuploaded    -  error-report1233934.txt
#    uploadedMode    -  backtrace only
#    JavaException   -  yes/no
#    BT Processed?        -  yes/no
#    ----------------------------------------------   
    my @timeData = localtime(time);
    my $timestamp = @timeData[4]."/".@timeData[3]."/". (@timeData[5]+1900) ." ".@timeData[2].":".@timeData[1].":".@timeData[0];
    my $ip = $cgi->remote_host();
    my $iaddr = inet_aton($ip); # or whatever address
    my $hostname = gethostbyaddr($iaddr, *AF_INET);
    my $filename = substr($_[0], index($_[0], "/"));
    my $backtraceDecoded = "yes";
    my $javaError = "no";
    my $logfile = "../logs/my.log";
    my $statsfile = "../logs/stats.log";
    my $javaresult = "";

    if(index($_[1], "backtrace not decoded") >= 0){
       $backtraceDecoded = "no";
    }

    if(defined($_[2])){
       $javaError = "yes";
       $javaresult = $_[1];
       $backtraceDecoded = "no"
    }

    
    open (LOGFILE, ">>", "$logfile") or die "Couldn't open $logfile for writing: $!";
    print LOGFILE "\n------------------------------------------------------\n";
    print LOGFILE "Processed On: $timestamp\n";
    print LOGFILE "IP Address: $ip\n";
    print LOGFILE "Hostname: $hostname\n";
    print LOGFILE "Error Report File: $upload_path$filename\n";
    print LOGFILE "Processing Mode: $process_type\n";
    print LOGFILE "Backtrace Processed: $backtraceDecoded\n";
    print LOGFILE "Java Error: $javaError\n";
    print LOGFILE "$javaresult\n";
    print LOGFILE "------------------------------------------------------\n";
    close(LOGFILE);

    #update the stats log

    #find log file. 
    
    open (STATSFILE, "<", "$statsfile") or die "Couldn't open $statsfile for reading: $!";
    my @lines = <STATSFILE>;
    close(STATSFILE);
    my $fullER = substr($lines[1], index($lines[1], ':')+2, -1), ;
    my $BTOnly = substr($lines[2], index($lines[2], ':')+2, -1), ;
    my $BTFails = substr($lines[3], index($lines[3], ':')+2, -1), ;
    my $javafails = substr($lines[3], index($lines[3], ':')+2, -1), ;
    $fullER = int($fullER);
    $BTOnly = int($BTOnly);
    $BTFails = int($BTFails);
    $javafails = int($javafails);

    if($process_type eq "backtraceOnly"){
       $BTOnly = $BTOnly+1;   
    }
    if($process_type eq "fullHTML"){
       $fullER = $fullER+1;
    }
    if($backtraceDecoded eq "no"){
       $BTFails = $BTFails+1;
    }
    if($javaError eq "yes"){
       $javafails = $javafails+1;
    }

    open (STATSFILE, ">", "$statsfile") or die "Couldn't open $statsfile for writing: $!";
    print STATSFILE "Full Error Reports: ".$fullER."\n";
    print STATSFILE "Backtrace Only Error Reports: ".$BTOnly."\n";
    print STATSFILE "Failed Backtraces: $BTFails\n";
    print STATSFILE "Java Failures: $javafails\n";
    close(STATSFILE);
}

sub updateUniqueErrorReports(){
    my $dir = "$upload_path";
    my @files = <$dir/*>;
    my $count = @files;
    $filesProcessed = $count;
}

sub updatePageVisit(){
    my $visitsfile = "logs/visits.log";
    open (STATSFILE, "<", "$visitsfile") or die "Couldn't open $visitsfile for reading: $!";
    my @lines = <STATSFILE>;
    close(STATSFILE);

    $pageVisits = $lines[0];
    $pageVisits = $pageVisits+1;

    open (STATSFILE, ">", "$visitsfile") or die "Couldn't open $visitsfile for writing: $!";
    print STATSFILE "$pageVisits";
    close(STATSFILE);
}

 

