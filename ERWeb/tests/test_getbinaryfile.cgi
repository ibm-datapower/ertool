#!/usr/bin/perl -w 

##used for testing the processing of binary data

use CGI;
use HTTP::Request::Common;
use LWP::Simple;
use LWP;

my $cgi = new CGI;
my $path = "tmp/";
my $filename = $cgi->param("filename");
my @fileholder;

my $fullfile = $path.$filename;


#open(FILE, "<$fullfile") || die ('cannot open file');
#@fileholder = <FILE>; 
#close (FILE) or die ('error closing file');

print $cgi->header(-type=>"application/octet-stream; name=\"$filename\"", -content_disposition=> "attachment; filename=\"$filename\"");
print `cat $fullfile`;
exit;
