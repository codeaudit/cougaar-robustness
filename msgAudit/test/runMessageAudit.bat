set LIBPATHS=.\compiled;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar;%COUGAAR_INSTALL_PATH%\sys\log4j.jar;%COUGAAR_INSTALL_PATH%\lib\msglog_common.jar;%COUGAAR_INSTALL_PATH%\lib\msgAudit.jar

java -classpath %LIBPATHS% org.cougaar.tools.robustness.audit.msgAudit.TrafficAuditor  -config LogPointInfo.xml -port 7887 

pause