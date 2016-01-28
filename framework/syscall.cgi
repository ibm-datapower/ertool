#!/usr/bin/perl -w

use CGI;

my $var = `$WORKDIR/datapower/isa/framework/ertool -file error-report.txt.gz -format HTML -xsl HTML,*,./src/erHTML.xsl`;


print $var;

