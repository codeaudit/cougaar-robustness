This configuration includes four agents, ManagerAgent, MonitoredAgent1,
MonitoredAgent2, and MonitoredAgent3.  The first runs in the Node
ManagerAgentNode, and the other three run in MonitoredAgentNode.

The plugin org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin is 
loaded in MonitoredAgent1.ini, MonitoredAgent2.ini, and MonitoredAgent3.ini.

The plugin org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin is 
loaded, and the sensors Domain is declared, in ManagerAgent.ini.

The sample user plugin HeartbeatTesterPlugin is also loaded in ManagerAgent.ini,
and it is passed eight arguments:
(10000, 10000, 1000, false, 50.0, MonitoredAgent1, MonitoredAgent2, MonitoredAgent3)

The first is the timeoutin milliseconds (ms) for the request. If the 
request isn't answered in time, a status of FAILED will be returned to the
requestor.  0 means "no timeout".

The second is the frequency (ms) that heartbeats should be sent.

The third is the timeout (ms) for the heartbeats themselves.

The fourth is whether to report only out-of-spec (i.e. late) heartbeats.  False
means report on all requested heartbeats.  

If the fourth is true, the fifth is the percentage out-of-spec a heartbeat
must be before it is reported.  50.0 means that a heartbeat must be more than
50% later in arriving than is specified by the frequency.  So, in this case,
the frequency in 10000ms (a heartbeat should arrive every 10 seconds).  If a
hearbeat arrives in 11 seconds it is late, but won't be reported.  If a
heartbeat hasn't arrived in 15 seconds, it will be reported.

The rest are the names of the agents that should send heartbeats. 

To run the test, open two cmd windows, cd to test\configs\heartbeat_test in each, and
run "Node MonitoredAgentNode" in one, and "Node ManagerAgentNode" in the other.

The following should be printed in the windows after each monitored agent has 
sent out two heartbeats:

------------------
MonitoredAgentNode
------------------

HeartbeatServerPlugin.execute: received new HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatServerPlugin.execute: received new HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatServerPlugin.execute: received new HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatServerPlugin.execute: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatServerPlugin.execute: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatServerPlugin.execute: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))
+++..
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
+
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
+
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
+..
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))
+
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))
+
HeartbeatServerPlugin.processHeartbeats: processing HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))

HeartbeatServerPlugin.processHeartbeats: published changed HbReq =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = []
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))

----------------
ManagerAgentNode
----------------

HeartbeatTesterPlugin.setupSubscriptions: added HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
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
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
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
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response = null)

HeartbeatRequesterPlugin.execute: published changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = SENT
   timeSent = Fri May 31 02:00:07 GMT 2002
   timeReceived = null
   roundTripTime = 0)
+++
HeartbeatTesterPlugin.execute: received changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = SENT
   timeSent = Fri May 31 02:00:07 GMT 2002
   timeReceived = null
   roundTripTime = 0)
+HeartbeatTesterPlugin.execute: status = SENT, ignored.
+---
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = ACCEPTED
       lastHbSent = Fri May 31 02:00:08 GMT 2002))

HeartbeatRequesterPlugin.updateHeartbeatRequest: published changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = ACCEPTED
   timeSent = Fri May 31 02:00:07 GMT 2002
   timeReceived = Fri May 31 02:00:08 GMT 2002
   roundTripTime = 641)

HeartbeatTesterPlugin.execute: received changed HeartbeatRequest =
(HeartbeatRequest:
   uid = ManagerAgent/1022810407288
   source = ManagerAgent
   target = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   reqTimeout = 10000
   hbFrequency = 10000
   hbTimeout = 1000
   onlyOutOfSpec = false
   percentOutOfSpec = 50.0
   status = ACCEPTED
   timeSent = Fri May 31 02:00:07 GMT 2002
   timeReceived = Fri May 31 02:00:08 GMT 2002
   roundTripTime = 641)
HeartbeatTesterPlugin.execute: status = ACCEPTED.
..-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:18 GMT 2002))
.
HeartbeatRequesterPlugin.prepareHealthReports: published new HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent3, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -0.060000002)
    (HeartbeatEntry: source = MonitoredAgent2, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -0.26)
    (HeartbeatEntry: source = MonitoredAgent1, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -1.36)
    ])

HeartbeatTesterPlugin.execute: received HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent3, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -0.060000002)
    (HeartbeatEntry: source = MonitoredAgent2, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -0.26)
    (HeartbeatEntry: source = MonitoredAgent1, timeReceived = Fri May 31 02:00:18 GMT 2002, percentLate = -1.36)
    ])
.-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent1
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))
-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent2
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))
-
HeartbeatRequesterPlugin.execute: changed HbReq received =
(HbReq:
   uid = ManagerAgent/1022810407289
   source = ManagerAgent
   targets = [MonitoredAgent3, MonitoredAgent2, MonitoredAgent1]
   content =
    (HbReqContent:
       heartbeatRequestUID = ManagerAgent/1022810407288
       reqTimeout = 10000
       hbFrequency = 10000
       hbTimeout = 1000)
   response =
    (HbReqResponse:
       responder = MonitoredAgent3
       status = HEARTBEAT
       lastHbSent = Fri May 31 02:00:28 GMT 2002))
.
HeartbeatRequesterPlugin.prepareHealthReports: published new HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent3, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = 0.39)
    (HeartbeatEntry: source = MonitoredAgent2, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = 0.29)
    (HeartbeatEntry: source = MonitoredAgent1, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = -0.21)
    ])

HeartbeatTesterPlugin.execute: received HeartbeatHealthReport =
(HeartbeatHealthReport:
   [(HeartbeatEntry: source = MonitoredAgent3, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = 0.39)
    (HeartbeatEntry: source = MonitoredAgent2, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = 0.29)
    (HeartbeatEntry: source = MonitoredAgent1, timeReceived = Fri May 31 02:00:28 GMT 2002, percentLate = -0.21)
    ])