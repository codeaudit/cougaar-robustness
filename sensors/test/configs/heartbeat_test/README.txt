This configuration includes two agents, ManagerAgent and MonitoredAgent,
each running in their own Node, ManagerAgentNode and MonitoredAgentNode.

The plugin org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin is 
loaded in MonitoredAgent.ini.

The plugin org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin is 
loaded, and the sensors Domain is declared, in ManagerAgent.ini.

The sample user plugin HeartbeatTesterPlugin is also loaded in ManagerAgent.ini,
and it is passed six arguments, (MonitoredAgent, 10000, 10000, 1000, false, 50.0).

The first is the name of the agent that should send heartbeats.  

The second is the timeoutin milliseconds (ms) for the request. If the 
request isn't answered in time, a status of FAILED will be returned to the
requestor.  0 means "no timeout".

The third is the frequency (ms) that heartbeats should be sent.

The fourth is the timeout (ms) for the heartbeats themselves.

The fifth is whether to report only out-of-spec (i.e. late) heartbeats.  False
means report on all requested heartbeats.  

If the fifth is true, the sixth is the percentage out-of-spec a heartbeat
must be before it is reported.  50.0 means that a heartbeat must be more than
50% later in arriving than is specified by the frequency.  So, in this case,
the frequency in 10000ms (a heartbeat should arrive every 10 seconds).  If a
hearbeat arrives in 11 seconds it is late, but won't be reported.  If a
heartbeat hasn't arrived in 15 seconds, it will be reported.

The timeouts aren't implemented yet.

To run the test, open two cmd windows, cd to test\configs\heartbeat_test in each, and
run "Node MonitoredAgentNode" in one, and "Node ManagerAgentNode" in the other.

The following should be printed in the windows and repeated every 10 seconds:

------------------
MonitoredAgentNode
------------------

..-HeartbeatServerPlugin.execute: received HbReq = (HbReq:
    uid = ManagerAgent/1022600211734
    source = ManagerAgent
    target = null
    content = (HbReqContent:
    heartbeatRequestUID = ManagerAgent/1022600211733
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    lastHbSent = Wed Dec 31 23:59:59 GMT 1969
)
    response = null
)
HeartbeatServerPlugin.execute: published changed HbReq = (HbReq:
    uid = ManagerAgent/1022600211734
    source = ManagerAgent
    target = null
    content = (HbReqContent:
    heartbeatRequestUID = ManagerAgent/1022600211733
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    lastHbSent = Tue May 28 15:36:52 GMT 2002
)
    response = (HbReqResponse:
   responder = MonitoredAgent
   status = ACCEPTED)
)
+HeartbeatServerPlugin.execute: received HbReq = (HbReq:
    uid = ManagerAgent/1022600211734
    source = ManagerAgent
    target = null
    content = (HbReqContent:
    heartbeatRequestUID = ManagerAgent/1022600211733
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    lastHbSent = Tue May 28 15:36:52 GMT 2002
)
    response = (HbReqResponse:
   responder = MonitoredAgent
   status = ACCEPTED)
)

----------------
ManagerAgentNode
----------------

HeartbeatTesterPlugin.setupSubscriptions: added HeartbeatRequest = (HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = NEW
    timeSent = null
    timeReceived = null
    roundTripTime = 0
)
HeartbeatRequesterPlugin.execute: new HeartbeatRequest received = (HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = NEW
    timeSent = null
    timeReceived = null
    roundTripTime = 0
)
HeartbeatRequesterPlugin.sendHbReq: published new HbReq = (HbReq:
    uid = ManagerAgent/1022600211734
    source = ManagerAgent
    target = MonitoredAgent
    content = (HbReqContent:
    heartbeatRequestUID = ManagerAgent/1022600211733
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    lastHbSent = Wed Dec 31 23:59:59 GMT 1969
)
    response = null
)
HeartbeatRequesterPlugin.sendHbReq: published changed HeartbeatRequest = (HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = SENT
    timeSent = Tue May 28 15:36:52 GMT 2002
    timeReceived = null
    roundTripTime = 0
)
+HeartbeatTesterPlugin.execute: received changed HeartbeatRequest
(HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = SENT
    timeSent = Tue May 28 15:36:52 GMT 2002
    timeReceived = null
    roundTripTime = 0
)
HeartbeatTesterPlugin.execute: status = SENT, ignored.
-HeartbeatRequesterPlugin.execute: changed HbReq received = (HbReq:
    uid = ManagerAgent/1022600211734
    source = ManagerAgent
    target = MonitoredAgent
    content = (HbReqContent:
    heartbeatRequestUID = ManagerAgent/1022600211733
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    lastHbSent = Wed Dec 31 23:59:59 GMT 1969
)
    response = (HbReqResponse:
   responder = MonitoredAgent
   status = ACCEPTED)
)
HeartbeatRequesterPlugin.updateHeartbeatRequest: published changed HeartbeatRequest = (HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = ACCEPTED
    timeSent = Tue May 28 15:36:52 GMT 2002
    timeReceived = Tue May 28 15:36:52 GMT 2002
    roundTripTime = 350
)
HeartbeatTesterPlugin.execute: received changed HeartbeatRequest
(HeartbeatRequest:
    uid = ManagerAgent/1022600211733
    source = ManagerAgent
    target = MonitoredAgent
    reqTimeout = 10000
    hbFrequency = 10000
    hbTimeout = 1000
    onlyOutOfSpec = false
    percentOutOfSpec = 50.0
    status = ACCEPTED
    timeSent = Tue May 28 15:36:52 GMT 2002
    timeReceived = Tue May 28 15:36:52 GMT 2002
    roundTripTime = 350
)
HeartbeatTesterPlugin.execute: status = ACCEPTED.