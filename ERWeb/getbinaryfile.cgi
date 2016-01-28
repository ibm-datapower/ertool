#!/usr/bin/perl -w 

##used for getting background packet capture objects

use CGI;
use HTTP::Request::Common;
use LWP::Simple;
use LWP;

my $cgi = new CGI;
my $path = "framework/tmp/";
my $fullfile = $cgi->param("filename");
my @fileholder;

#my $filename = substr($fullfile, rindex($fullfile, '/'));
my $filename = $path.$fullfile;

#open(FILE, "<$fullfile") || die ('cannot open file');
#@fileholder = <FILE>; 
#close (FILE) or die ('error closing file');

print $cgi->header(-type=>"application/octet-stream; name=\"$fullfile\"", -content_disposition=> "attachment; filename=\"$fullfile\"");
print `cat $filename`;
exit;
