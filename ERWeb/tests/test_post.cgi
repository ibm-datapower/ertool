#!/usr/bin/perl -w 

###used to test LWP Posting to a site

use CGI;
use HTTP::Request::Common;
use LWP::Simple;
use LWP;

my $thedata = "";

my ($bytesread, $buffer);
my $numbytes = 1024;
my $totalbytes = 0;
my $filename = "error-report.130025Z.20110513144220EDT.txt.gz";
open FILE, "<$filename" or die $!;

    while ($bytesread = read(FILE,  $buffer, $numbytes)) {
         $totalbytes += $bytesread;
         $thedata = $thedata . $buffer;
    }

    my %URLParameters = (
        "contents" => "$thedata",
        "filename" => "posted-stream",
        "process_type" => "fullHTML",
    );


    my $LWPAgent = LWP::UserAgent->new;
    my $url = "http://localhost/ertool/ersite.cgi";
    my $Request = POST($url, Content_Type => 'form-data', Content => \%URLParameters);
    my $Response = $LWPAgent->request($Request);
print "Content-type: text/html\n\n";
    print($Response->content);



