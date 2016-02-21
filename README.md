# mHealth-WAX9 Converter
A standalone app written in Java to convert raw Accelerometer data from WAX9 File Format into mHealth format.

Description
-----------
This repository contains Java source code, executables and sample wax9 data to convert a WAX9 file into mHealth-compliant CSV files.

Content description:
- **sample-data/**: Contains a csv file and a binary file from Axivity devices. WAX9 file format specifications can be found here: [Wax9 Developer Guide, pg. 7](http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf#page=7).
- **src/**: Contains a Java project with the full source code.
- **WAX9Parser.jar**: Is an executable jar file accessible from the command line to convert a wax9 file to mHealth-compliant CSV file.


Requirements
------------
To run the executable (WAX9Parser.jar), download and install the latest [Java JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html) if you do not have it (most systems have JRE). 


Usage
-----
Download the WAX9Parser.jar file, open a command prompt and type a command with the following usage pattern:
```ShellSession
java -jar WAX9Parser.jar [INPUT WAX9 FILE] [OUTPUT CSV DIRECTORYPATH] [SPLIT/NO_SPLIT]
```

- **[INPUT WAX9 FILE]**: (required) Relative or absolute path for a GT3X file.
- **[OUTPUT CSV DIRECTORY PATH]**: (required) Relative or absolute path of the directory for the mHealth CSV output file (Ending the path with a '/' is optional).
- **[SPLIT/NO_SPLIT]**: (required) Generate mHealth output in one big file or in multiple hourly files.


Example Commands
----------------
1- Open a terminal or command prompt.

2- Navigate to the directory where the downloaded GT3XParser.jar is located.

3- Type the following command: 
```ShellSession
java -jar WAX9Parser.jar sample-data/exampleData.bin /home/user/Documents/ SPLIT
```

4- An mHealth CSV file should be generated in the specified output directory with decoded GT3X data. For example:
```ShellSession
/home/user/Documents/WAX9.ACCEL.65535.2014-11-18-14-06-13-046.csv
```

or in the case where the "SPLIT" option is used, hourly mHealth CSV files will be generated:

```ShellSession
/home/user/Documents/WAX9.ACCEL.65535.2014-11-18-14-06-13-046.csv
/home/user/Documents/WAX9.ACCEL.65535.2014-11-18-15-00-00-000.csv
/home/user/Documents/WAX9.ACCEL.65535.2014-11-18-16-06-00-000.csv
...
```

Links
-----
- SPADESLab - http://www.spadeslab.com/
- QMedic - http://www.qmedichealth.com/
- WAX9 Developer Guide: http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf
- mHealth File Format Specification - TBA
