@echo off

if not exist doc mkdir doc

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\build.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\glm.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\servlet.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\manager.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\msglog_common.jar

set FILES=src\org\cougaar\tools\robustness\sensors\*.java
set FILES=%FILES% src\org\cougaar\core\mts\*.java


@echo on

javadoc -d .\doc\api -classpath %LIBPATHS% %FILES%







