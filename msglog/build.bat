@echo off

REM "<copyright>"
REM " Copyright 2001 BBNT Solutions, LLC"
REM " Copyright 2002 Object Services and Consulting, Inc."
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

rem Script to compile msglog

rem compile the code
if not exist lib mkdir lib
set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar
set LIBPATHS=%LIBPATHS%;sys\mail.jar

set MTS=src\org\cougaar\core\mts\*.java
set EMAIL=src\org\cougaar\core\mts\email\*.java
set UDP=src\org\cougaar\core\mts\udp\*.java
set SOCKET=src\org\cougaar\core\mts\socket\*.java
set ACKING=src\org\cougaar\core\mts\acking\*.java

@echo on
javac -deprecation -d lib -classpath %LIBPATHS% %MTS% %EMAIL% %UDP% %SOCKET% %ACKING% && jar cf %COUGAAR_INSTALL_PATH%\sys\msglog.jar -C lib ./org
