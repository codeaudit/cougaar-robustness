This configuration includes two agents, ManagerAgent and MonitoredAgent,
each running in their own Node, ManagerAgentNode and MonitoredAgentNode.

The plugin org.cougaar.tools.robustness.sensors.PingServerPlugin is 
loaded in MonitoredAgent.ini.

The plugin org.cougaar.tools.robustness.sensors.PingRequestorPlugin is 
loaded, and the sensors Domain is declared, in ManagerAgent.ini.

The sample user plugin PingTesterPlugin is also loaded in ManagerAgent.ini,
and it is passed two arguments, (MonitoredAgent, 5000).  The first is the
name of the agent to be pinged, and the second is the timeout.  A timeout
of 0 means "no timeout".  A positive integer indicates the number of 
milliseconds to wait before returning a FAILED status to the requestor.  

Logging is turned on by adding -Dorg.cougaar.core.logging.config.filename=logging.props
to MYPROPERTIES in %COUGAAR_INSTALL_PATH%\bin\setarguments.bat.  The default log 
setting in logging.props is INFO.  To get full visibility into this module's operation,
as in the attached dumps, set it to DEBUG, by changing the line:
  log4j.category.org.cougaar.tools.robustness.sensors=INFO,A1
to
  log4j.category.org.cougaar.tools.robustness.sensors=DEBUG,A1

To run the test, open two cmd windows, cd to test\configs\ping_test in each, and
run "Node MonitoredAgentNode" in one, and "Node ManagerAgentNode" in the other.

The following should be printed in the windows:

MonitoredAgentNode

--PingServerPlugin.execute: received Ping = (Ping: ManagerAgent/1021477705049, ManagerAgent, null, (PingContent: ManagerAgent/1021477705048, 0), null)
PingServerPlugin.execute: published changed Ping = (Ping: ManagerAgent/1021477705049, ManagerAgent, null, (PingContent: ManagerAgent/1021477705048, 0), Got it!)
+-

ManagerAgentNode

PingTesterPlugin.setupSubscriptions: added PingRequest = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 0, null, null, 0)
PingRequesterPlugin.execute: new PingRequest received = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 0,   null, null, 0)
PingRequesterPlugin.sendPing: published new Ping = (Ping: ManagerAgent/1021477705049, ManagerAgent, MonitoredAgent, (PingContent: ManagerAgent/1021477705048, 0), null)
PingRequesterPlugin.sendPing: published changed PingRequest = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 1, Wed May 15 11:48:25 EDT 2002, null, 0)
PingTesterPlugin.execute: received changed PingRequest = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 1, Wed May 15 11:48:25 EDT 2002, null, 0)
PingTesterPlugin.execute: status = SENT, ignored.
++-PingRequesterPlugin.execute: changed Ping received = (Ping: ManagerAgent/1021477705049, ManagerAgent, MonitoredAgent, (PingContent: ManagerAgent/1021477705048, 0), Got it!)
PingRequesterPlugin.updatePingRequest: published changed PingRequest = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 2, Wed May 15 11:48:25 EDT 2002, Wed May 15 11:48:25 EDT 2002, 351)
PingRequesterPlugin.updatePingRequest: removed Ping = (Ping: ManagerAgent/1021477705049, ManagerAgent, MonitoredAgent, (PingContent: ManagerAgent/1021477705048, 0), Got it!)
+PingTesterPlugin.execute: received changed PingRequest = (PingRequest: ManagerAgent/1021477705048, ManagerAgent, MonitoredAgent, 0, 2, Wed May 15 11:48:25 EDT 2002, Wed May 15 11:48:25 EDT 2002, 351)
PingTesterPlugin.execute: status = RECEIVED.
PingTesterPlugin.execute: timeSent = Wed May 15 11:48:25 EDT 2002
PingTesterPlugin.execute: timeReceived = Wed May 15 11:48:25 EDT 2002
PingTesterPlugin.execute: roundTripTime = 351
PingTesterPlugin.execute: timeSent = Wed May 15 11:48:25 EDT 2002