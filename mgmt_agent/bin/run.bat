@echo off

REM --------------------------------------------
REM No need to edit anything past here
REM --------------------------------------------
set _BUILDFILE=%BUILDFILE%
set BUILDFILE=%COUGAAR_INSTALL_PATH%\robustness\build.xml

:final

set _CLASSPATH=%CLASSPATH%
if exist %JAVA_HOME%\lib\tools.jar set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CLASSPATH%;%COUGAAR_INSTALL_PATH%\sys\ant.jar;

java -classpath %CLASSPATH% org.apache.tools.ant.Main -emacs -buildfile %BUILDFILE% %1 %2 %3 %4 %5 %6 %7 %8 %9

goto end

:end
set BUILDFILE=%_BUILDFILE%
set _BUILDFILE=
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=
