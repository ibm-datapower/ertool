#!/usr/bin/perl

# IBM Confidential
# IBM WebSphere DataPower Appliances
# Copyright IBM Corporation 2012

# ---------------------------------------------------------------------------
#
#   Copyright (c) 2012 DataPower Technology, Inc. All Rights Reserved
#
# THIS IS UNPUBLISHED PROPRIETARY TRADE SECRET SOURCE CODE OF DataPower
# Technology, Inc.
#
# The copyright above and this notice must be preserved in all copies of
# the source code. The copyright notice above does not evidence any actual
# or intended publication of such source code. This source code may not be
# copied, compiled, disclosed, distributed, demonstrated or licensed except
# as expressly authorized by DataPower Technology, Inc.
#
# Name:           errReportPostProcessing.pl
# Description:    Appends English text to each line of the error report in default-log section for a given non-English error report.
#
# ---------------------------------------------------------------------------

# Usage example
# errReportPostProcessing.pl <directory> <input-file-path> <output-file-path>

use strict;
use XML::Parser;
use utf8;

# check arguments
unless ($ARGV[0] && $ARGV[1] && $ARGV[2]){
	print "Command syntax error !!\n";
	printCmdUsuage();
	exit -1;
}

## get first argugment
my $msgcatxml_dir= $ARGV[0];
opendir(XML_ROOTDIR, $msgcatxml_dir) or die "can not open directory $msgcatxml_dir: $!\n";

my %msgid_hash = ();
my $class_id= -1;
my $result_id= -1;
my $severiy_id= -1;	
my $type_id= -1;
my $entry_id= -1;

while (my $xmlfile = readdir(XML_ROOTDIR)) {
	# We only want files
	next unless (-f "$msgcatxml_dir/$xmlfile");
	# Use a regular expression to find files ending in .xml
	next unless ($xmlfile =~ m/\.xml$/);	
	
	my $parser;
	
	
	if($xmlfile ne "result.xml"){
		# initialize the parser
		$parser = XML::Parser->new( Handlers => 
											 {
											  Start=>\&handle_log_start,
											  Char=>\&char_log_handler,
											  End=>\&handle_log_end
											 });		
	}else{
		# initialize the parser
		$parser = XML::Parser->new( Handlers => 
											 {
											  Start=>\&handle_result_start,
											  Char=>\&char_result_handler,
											  End=>\&handle_result_end
											 });											 
	}
	$parser->parsefile("$msgcatxml_dir/$xmlfile");		
}
close XML_ROOTDIR;

my $error_report_file= $ARGV[1];
my $outputfile=$ARGV[2];
open (REPORTFILE, "$error_report_file") or die "can not open $error_report_file: $!\n";
open REPORTFILEOUT, ">$outputfile" or die "can not open file $outputfile: $!\n";
binmode(REPORTFILE, ":encoding(utf8)");
binmode(REPORTFILEOUT, ':encoding(utf8)');

# Match MIME sections that have a file attachment named "default-log" or "<domainname>-log"
my $start_process = "";
while (my $line = <REPORTFILE>){
	# chop all trailing tab, new line and spaces
	$line =~ s/[ \t\r\n]+$//g;
	
	# check and see if $line contians boundary end tag pattern
	if($line =~ m/^----dp([0-9]{12})/){
		$start_process="";
		print REPORTFILEOUT "$line\n";
		next;
	}

	if($line =~ m/Content-Disposition: attachment; filename=default-log/){
		$start_process="true";
		print REPORTFILEOUT "$line\n";
		next;
	}

	# check and see if $line contains msgid pattern
	if($line =~ m/(.*?)0x([0-9a-f]{8})(.*)/ && $start_process eq "true"){
		my $msgid = "0x$2";
		my $msg = $msgid_hash{"$msgid"};

		if($msg ne ""){
			# append english message
			print REPORTFILEOUT "$line (Message in English: $msg)\n";
			next;
		}
	}
	print REPORTFILEOUT "$line\n";
}
close REPORTFILE;
close REPORTFILEOUT;

#############################
# Print command syntax and example
# ###########################
sub printCmdUsuage {
	
	print <<CMD_USUAGE;
	
Command Syntax: ./errReportPostProcessing.pl <directory> <input-file-path> <output-file-path>

<directory>
	The path for the directory containing the English msgcat XML files
<input-file-path>
	The file path for the non-English error report file(.txt)
<output-file-path>
	The file path for the output error report file
	
CMD_USUAGE
}

# process result.xml
#
sub handle_result_start {
    my( $expat, $element, %attrs ) = @_;

	if($element eq "class"){
	    if( %attrs ) {
			while( my( $key, $value ) = each( %attrs )) {
				if($key eq "id"){		
					$class_id=$value;
				}
			}
		}
    }elsif($element eq "result"){
	    if( %attrs ) {
			while( my( $key, $value ) = each( %attrs )) {
				if($key eq "severity"){
					$severiy_id = getSeverityID($value);
				}elsif($key eq "id"){
					$result_id=$value;
				}
			}
		}
    }	
}

sub handle_result_end {
    my( $expat, $element) = @_;
	
	if($element eq "class"){
		$class_id= -1;
		$result_id= -1;
		$severiy_id= -1;
    }elsif($element eq "result"){
		$result_id= -1;
		$severiy_id= -1;	
    }elsif($element eq "text"){
		$result_id= -1;
		$severiy_id= -1;	
    }
}

# process other msg xml files
#
sub handle_log_start {
    my( $expat, $element, %attrs ) = @_;
 
    # ask the expat object about our position
    my $line = $expat->original_string;
	
	if($element eq "type"){
	    if( %attrs ) {
			while( my( $key, $value ) = each( %attrs )) {
				if($key eq "id"){
					$type_id=$value;
				}
			}
		}
    }elsif($element eq "entry"){
	    if( %attrs ) {
			while( my( $key, $value ) = each( %attrs )) {
				if($key eq "id"){				
					$entry_id=$value;
				}
			}
		}
    }	
}

sub handle_log_end {
    my( $expat, $element) = @_;
	
	if($element eq "type"){
		$entry_id= -1;
		$type_id= -1;
    }elsif($element eq "entry"){
		$entry_id= -1;
    }elsif($element eq "text"){
		$entry_id= -1;;	
    }
}

sub char_log_handler {
	my ($p, $data) = @_;
    my $element = $p->current_element;

	if($element eq "text"){
		if($type_id != -1 && $entry_id != -1){
			my $makeid = LOGMSGIDMake($type_id,$entry_id);
			$msgid_hash{"$makeid"} = "$data";			
		}				
	}
}

sub char_result_handler {
    my ($p, $data) = @_;
    my $element = $p->current_element;

	if($element eq "text"){
		if ($class_id != -1 && $result_id != -1 && $severiy_id != -1){
			my $makeid = ResultMake($class_id, $severiy_id, $result_id);
			$msgid_hash{"$makeid"} = "$data";
		}			
	}
}

# return unique msgcat hex id
sub LOGMSGIDMake($$){
	my($t, $i ) = @_;
	my $msg_id =((0x1 << 31) | ((($t) & 0xff) << 21) | (($i) & 0xffff));
	return sprintf("0x%08x",$msg_id);
}

# return msgcat type
sub LOGMSGIDtype($){
	my($e) = @_;
	($e) = hex($e);
	return  ((($e) >> 21) & 0xff);
}

# return msgcat id
sub LOGMSGIDid($){
	my($e) = @_;
	($e) = hex($e);
	return  (($e) & 0xffff);
}

sub LOGMSGIDccf($){
	my($e) = @_;
	($e) = hex($e);
	return  ((($e) >> 31) & 0x1);
}

sub ResultMake($$$){
	my($c, $s, $i ) = @_;
	$result_id =(((($c) & 0x7ff) << 20) | ((($s) & 0xf) << 16) | (($i) & 0xffff));
	return sprintf("0x%08x",$result_id);
}

sub getSeverityID($){
	my($severity) = @_;
	
	if($severity eq "emergency"){
		return 0;
	}elsif($severity eq "alert"){
		return 1;	
	}elsif($severity eq "critical"){
		return 2;	
	}elsif($severity eq "error"){
		return 3;	
	}elsif($severity eq "warning"){
		return 4;	
	}elsif($severity eq "notice"){
		return 5;	
	}elsif($severity eq "info"){
		return 6;	
	}elsif($severity eq "debug"){
		return 7;	
	}else{
		return -1;
	}
}
exit 0;
