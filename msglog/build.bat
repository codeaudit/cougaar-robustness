@echo off

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
javac -deprecation -d lib -classpath %LIBPATHS% %MTS% %EMAIL% %UDP% %SOCKET% %ACKING% && jar cf %COUGAAR_INSTALL_PATH%\lib\msglog.jar -C lib ./org

jarsigner.exe -keystore signingCA.keystore -storepass keystore %COUGAAR_INSTALL_PATH%\lib\msglog.jar privileged



