set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar
REM set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\msgaudit.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\Robustness_objs_msgAudit_HEAD_B10_2.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\msglog_common.jar

REM java -classpath %LIBPATHS% org.cougaar.tools.robustness.audit.msgAudit.TrafficAuditor -config LogPointInfo.xml -port 7887
java -classpath %LIBPATHS% LogPointAnalyzer.TrafficAuditor -config LogPointInfo.xml -port 7887

