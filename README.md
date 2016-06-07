# Error Report Tool (ERTool)

ERTool is a java package for data analysis which simplifies processing large data-sets.
This primarily focuses on the DataPower Error Report and Cast Iron/API Management 
among other products post-mortem. 

## Prerequisites

* JDK 1.7 or later
* Apache Ant (Tested with 1.9.6, but likely work with 1.8.1 or later)

## Building

After you obtain the source either via a Git client or by downloading the repository zip file,
you can build the code using Apache Ant. Enter the framework directory and issue the command below (assumes
Apache Ant is in the path):

    ant -buildfile build.xml

* The ErrorReport.jar will be found in dist/
* For GUI: java -cp ErrorReport.jar com.ibm.datapower.er.ERTool -gui
* For Text/CLI: java -cp ertool.jar com.ibm.datapower.er.ERTool -file "error-report.txt.gz" -analyticsfile Analytics.xml 1> "destinationfile.txt"
* In the CLI the '-format HTML' argument can be used to generate an HTML report to the destination file.
* If ErrorReport.jar runs out of memory, increase the available JVM by adding '-Xmx4096m' as an argument to java.
* If Stack Overflow Exceptions occur increase the stack size of the JVM by adding '-Xss4m' as an argument to Java.

## Contributing

If you want to contribute to the project, you will need to fill in the appropriate Contributor 
License agreement which can be found under the CLA directory. Follow the directions inside the
document so that any submissions can be properly accepted into the repository.

## License

The code is licensed under the terms of the Apache License 2.0. See the acompanying 'LICENSE' file
for further details.