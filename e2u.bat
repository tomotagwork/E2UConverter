rem Sample:
rem    .\e2u.bat -i .\91_testdata\ -o .\92_testoutput\ -e txt -c IBM-1399


@echo off
java -jar "%~dp003_implementation\e2uconverter\target\e2uconverter-1.0.0.jar" %*
