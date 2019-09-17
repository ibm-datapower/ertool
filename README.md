# Error Report Tool (ERTool)

ERTool is a java package for data analysis which simplifies processing large data-sets. This primarily focuses on the multipart-data obtained in a DataPower Error Report (.txt.gz), MQ Appliance (txt.gz) through Analytics.xml. Additionally supports Cast Iron/API Management (.tar.gz formatted files/directory lists) through postmortem.xml.

## Prerequisites

* JDK 1.7 or later
* Apache Ant (Tested with 1.9.6, but likely work with 1.8.1 or later)

## Building

After you obtain the source either via a Git client or by downloading the repository zip file, you can build the code using Apache Ant. Enter the framework directory and issue the command below (assumes Apache Ant is in the path):

```
ant -buildfile build.xml
```

The `ErrorReport.jar` will be found in `dist/`.

## Running

For GUI:

```
java -jar ErrorReport.jar -gui
```

Special note regarding running the GUI on macOS: You will need to specify `-XstartOnFirstThread` in the vmargs:

```
java -XstartOnFirstThread -jar ErrorReport.jar -gui
```

For Text/CLI with no logging:

```
java -jar ErrorReport.jar -file "error-report.txt.gz" -analyticsfile Analytics.xml -loglevel none 1> "destinationfile.txt"
```

For Text/CLI with logging:

```
java -jar ErrorReport.jar -file "error-report.txt.gz" -analyticsfile Analytics.xml -loglevel info -outfile "destinationfile.txt"
```

- In the CLI the `-format HTML` argument can be used to generate an HTML report to the destination file.
* If `ErrorReport.jar` runs out of memory, increase the available JVM by adding `-Xmx8096m` as an argument to `java`.
* If Stack Overflow Exceptions occur increase the stack size of the JVM by adding `-Xss8m` as an argument to `java`.
* In cases of Stack Overflow some formulas will require large stack sizes, in testing some have required 512M for 100K lines parsed in regular expressions.
* The `loglevel` argument can be set to `none`|`info`|`debug` to provide more information when testing formulas.

## Contributing

If you want to contribute to the project, you will need to fill in the appropriate Contributor License agreement which can be found under the CLA directory. Follow the directions inside the document so that any submissions can be properly accepted into the repository.

## License

The code is licensed under the terms of the Apache License 2.0. See the acompanying 'LICENSE' file for further details.
