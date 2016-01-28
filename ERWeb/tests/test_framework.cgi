#!/usr/bin/perl

#used for testing framework outside of perl ersite.cgi

use DBI;
use CGI;
use strict;
use CGI::Carp qw(fatalsToBrowser);


my $path = "../framework/";
my $cgi = new CGI;

chdir($path) or die "Cant chdir to $path $!";



my $file = $ARGV[0];
my $mode = $ARGV[1];
my $property_file = "properties/zipfile.properties";

if(defined($ARGV[2])){
   #property_file = $ARGV[2];
}

if(index($mode, "-")<0){
   $mode = "-".$mode;
}

#my $result = `./alex_ertool -i $file -p $process_property_file`;
#my $result = `./alex_ertool -i ../$file -z 2>&1`;
my $result = `./alex_ertool -i ../$file $mode -p $property_file 2>&1`;


#open FILE, "mreport.html" or die $!;
#my $output = "";
#my $ch;
#while (<FILE>) {
# 	chomp;
#        $output = $output.$_."\n";
#}
#close (FILE);



print $cgi->header(-type=>"application/x-zip; name=\"$file.zip\"",
                       -content_disposition=> "attachment; filename=\"$file.zip\"");
print $result;

