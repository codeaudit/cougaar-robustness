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

Caveats:
  - the timeouts aren't implemented yet, so won't return FAILURE.
  - doesn't use UDP yet
  - only supports one target per request at the moment
  - haven't tested with ABA
  - haven't converted to logging yet
  
To run the test, open two cmd windows, cd to test\configs\heartbeat_test in each, and
run "Node MonitoredAgentNode" in one, and "Node ManagerAgentNode" in the other.

The following should be printed in the windows after three iterations:

------------------
MonitoredAgentNode
------------------

-HeartbeatServerPlugin.execute: received new HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatServerPlugin.execute: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = ACCEPTED
       lastHbSent = Wed May 29 20:03:29 GMT 2002))

+..HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = ACCEPTED
       lastHbSent = Wed May 29 20:03:29 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = HEARTBEAT
       lastHbSent = Wed May 29 20:03:39 GMT 2002))

+..HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = HEARTBEAT
       lastHbSent = Wed May 29 20:03:39 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = null
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = HEARTBEAT
       lastHbSent = Wed May 29 20:03:49 GMT 2002))

----------------
ManagerAgentNode
----------------

HeartbeatTesterPlugin.setupSubscriptions: added HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
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
   roundTripTime = 0)

HeartbeatRequesterPlugin.prepareHealthReports: new HeartbeatRequest received =
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
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
   roundTripTime = 0)

HeartbeatRequesterPlugin.execute: published new HbReq =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = MonitoredAgent
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatRequesterPlugin.execute: published changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
   source = ManagerAgent
   target = MonitoredAgent
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = SENT
   timeSent = Wed May 29 20:03:29 GMT 2002
   timeReceived = null
   roundTripTime = 0)

HeartbeatTesterPlugin.execute: received changed HeartbeatRequest
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
   source = ManagerAgent
   target = MonitoredAgent
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = SENT
   timeSent = Wed May 29 20:03:29 GMT 2002
   timeReceived = null
   roundTripTime = 0)

HeartbeatTesterPlugin.execute: status = SENT, ignored.
+-HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = MonitoredAgent
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = ACCEPTED
       lastHbSent = Wed May 29 20:03:29 GMT 2002))

HeartbeatRequesterPlugin.updateHeartbeatRequest: published changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
   source = ManagerAgent
   target = MonitoredAgent
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = ACCEPTED
   timeSent = Wed May 29 20:03:29 GMT 2002
   timeReceived = Wed May 29 20:03:29 GMT 2002
   roundTripTime = 360)

HeartbeatTesterPlugin.execute: received changed HeartbeatRequest
(HeartbeatRequest:
   uid = ManagerAgent/1022702609062
   source = ManagerAgent
   target = MonitoredAgent
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = ACCEPTED
   timeSent = Wed May 29 20:03:29 GMT 2002
   timeReceived = Wed May 29 20:03:29 GMT 2002
   roundTripTime = 360)

HeartbeatTesterPlugin.execute: status = ACCEPTED.
.-HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = MonitoredAgent
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = HEARTBEAT
       lastHbSent = Wed May 29 20:03:39 GMT 2002))

..HeartbeatRequesterPlugin.prepareHealthReports: published new HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent, timeReceived = Wed May 29 20:03:39 GMT 2002, percentLate = -0.86)
    ])

HeartbeatTesterPlugin.execute: received HeartbeatHealthReport
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent, timeReceived = Wed May 29 20:03:39 GMT 2002, percentLate = -0.86)
    ])

-HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022702609063
   source = ManagerAgent
   target = MonitoredAgent
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022702609062
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent
       status = HEARTBEAT
       lastHbSent = Wed May 29 20:03:49 GMT 2002))

.HeartbeatRequesterPlugin.prepareHealthReports: published new HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent, timeReceived = Wed May 29 20:03:49 GMT 2002, percentLate = -0.81)
    ])

HeartbeatTesterPlugin.execute: received HeartbeatHealthReport
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent, timeReceived = Wed May 29 20:03:49 GMT 2002, percentLate = -0.81)
    ])