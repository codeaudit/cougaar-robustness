set MYDOMAINS=
set MYCLASSES=org.cougaar.bootstrap.Bootstrapper org.cougaar.core.node.Node
set MYMEMORY=-Xms100m -Xmx300m

REM set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.core.logging.config.filename=logging.conf -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.StatisticsAspect,org.cougaar.core.mts.HeartbeatPiggybackerAspect -Dorg.cougaar.message.transport.aspects.heartbeatPiggybacker.msgTransitTime=2500

REM ,org.cougaar.core.mts.AgentStatusAspect,org.cougaar.core.mts.CountBytesStreamsAspect.java 

REM -Dorg.cougaar.core.persistence.enable=true -Dorg.cougaar.core.persistence.clear=true -Dorg.cougaar.core.persistence.lazyInterval=10000

REM set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.core.logging.config.filename=logging.conf -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.StatisticsAspect,org.cougaar.core.mts.TraceAspect -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.LoopbackLinkProtocol,org.cougaar.core.mts.RMILinkProtocol

REM set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.core.logging.config.filename=logging.conf -Dorg.cougaar.message.transport.policy=org.cougaar.core.mts.AdaptiveLinkSelectionPolicy -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.NameSupportTimeoutAspect,org.cougaar.core.mts.ShowTrafficAspect,org.cougaar.core.mts.RMISocketControlAspect,org.cougaar.core.mts.StatisticsAspect,org.cougaar.core.mts.RMISendTimeoutAspect,org.cougaar.core.mts.MessageOrderingAspect,org.cougaar.core.mts.MessageSendHistoryAspect,org.cougaar.core.mts.acking.MessageAckingAspect,org.cougaar.core.mts.RTTAspect,org.cougaar.core.mts.MessageNumberingAspect,org.cougaar.core.mts.TraceAspect -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.LoopbackLinkProtocol,org.cougaar.core.mts.RMILinkProtocol 

REM -Dorg.cougaar.message.transport.mts.AgentID.callTimeout=5000 -Dorg.cougaar.core.wp.server.successTTD=15000 -Dorg.cougaar.core.wp.server.failTTD=15000 -Dorg.cougaar.core.wp.server.expireTTD=600000

REM set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.core.logging.config.filename=loggingConfig.steve -Dorg.cougaar.message.transport.policy=org.cougaar.core.mts.AdaptiveLinkSelectionPolicy -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.MessageSendHistoryAspect,org.cougaar.core.mts.acking.MessageAckingAspect,org.cougaar.core.mts.RTTAspect,org.cougaar.core.mts.MessageNumberingAspect,org.cougaar.core.mts.MessageOrderingAspect 

set MYPROPERTIES=-Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar -Dorg.cougaar.system.path=%COUGAAR3RDPARTY% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Duser.timezone=GMT -Dorg.cougaar.core.agent.startTime=08/10/2005 -Dorg.cougaar.class.path=%COUGAAR_DEV_PATH% -Dorg.cougaar.workspace=%COUGAAR_WORKSPACE% -Dorg.cougaar.core.logging.config.filename=log.props -Dorg.cougaar.message.transport.policy=org.cougaar.core.mts.AdaptiveLinkSelectionPolicy -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.MsglogEnablingAspect,org.cougaar.core.mts.LastSpokeErrorAspect,org.cougaar.core.mts.GossipAspect,org.cougaar.core.mts.GossipStatisticsServiceAspect,org.cougaar.core.mts.ShowTrafficAspect,org.cougaar.core.mts.StatisticsAspect,org.cougaar.core.mts.MessageSendHistoryAspect,org.cougaar.core.mts.acking.MessageAckingAspect,org.cougaar.core.mts.RTTAspect,org.cougaar.core.mts.MessageNumberingAspect,org.cougaar.core.mts.MessageOrderingAspect -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.LoopbackLinkProtocol,org.cougaar.core.mts.RMILinkProtocol -Dorg.cougaar.core.persistence.enable=true -Dorg.cougaar.core.persistence.clear=true -Dorg.cougaar.core.persistence.lazyInterval=20000

REM ,org.cougaar.core.mts.RMISocketControlAspect,
REM ,org.cougaar.core.mts.RMISendTimeoutAspect

REM -Dorg.cougaar.message.protocol.email.inboxes.NodeA=pop3://node1:passwd@atom:110 -Dorg.cougaar.message.protocol.email.outboxes.NodeA=smtp://node1:passwd@atom:25 -Dorg.cougaar.message.protocol.email.inboxes.NodeB=pop3://node2:passwd@atom:110 -Dorg.cougaar.message.protocol.email.outboxes.NodeB=smtp://node2:passwd@atom:25 -Dorg.cougaar.message.protocol.email.inboxes.NodeEmpty=pop3://node3:passwd@atom:110 -Dorg.cougaar.message.protocol.email.outboxes.NodeEmpty=smtp://node3:passwd@atom:25 -Dorg.cougaar.message.protocol.email.inboxes.NodeC=pop3://node4:passwd@atom:110 -Dorg.cougaar.message.protocol.email.outboxes.NodeC=smtp://node4:passwd@atom:25 

REM -Dorg.cougaar.core.persistence.enable=true -Dorg.cougaar.core.persistence.clear=false -Dorg.cougaar.core.persistence.lazyInterval=10000

REM Put this in Node.bat
REM -Dorg.cougaar.core.logging.log4j.appender.SECURITY.File=/mnt/local/side/workspace/log4jlogs/"%1".log

REM -Dorg.cougaar.core.wp.server.successTTD=15000 -Dorg.cougaar.core.wp.server.failTTD=15000 -Dorg.cougaar.core.wp.server.expireTTD=600000

REM ,org.cougaar.core.mts.email.OutgoingEmailLinkProtocol,org.cougaar.core.mts.email.IncomingEmailLinkProtocol

REM org.cougaar.core.mts.NameSupportTimeoutAspect,

REM -Dorg.cougaar.message.transport.aspects.RMISendTimeoutAspect.connectTimeout=2000 -Dorg.cougaar.message.transport.aspects.RMISendTimeoutAspect.readTimeout=2000 -Dorg.cougaar.message.transport.aspects.RMISendTimeoutAspect.writeTimeout=2000 -Dorg.cougaar.message.protocol.email.incoming.socketTimeout=2000 -Dorg.cougaar.message.protocol.email.outgoing.socketTimeout=2000 -Dorg.cougaar.message.transport.aspects.NameSupportTimeoutAspect.timeout=10000

REM org.cougaar.core.mts.socket.IncomingSocketLinkProtocol,org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol,
REM -Dorg.cougaar.message.transport.debug=all

REM -Dorg.cougaar.core.mts.logging.Log4JHost=127.0.0.1 -Dorg.cougaar.core.mts.logging.Log4JPort=7887 -Dorg.cougaar.core.mts.logging.Log4JLevel=DEBUG -Dorg.cougaar.message.transport.aspects.trafficaudit.includeLocalMsgs=true 

REM -Dorg.cougaar.message.protocol.udp.outgoing.socketTimeout=1000 -Dorg.cougaar.core.util.ConfigFinder.verbose=true 

REM -Dorg.cougaar.message.protocol.email.debugMail=true -Dmail.debug=true

REM old mailbox properties
REM -Dorg.cougaar.message.protocol.email.inboxes.NodeA=pop3,atom,110,node1,passwd -Dorg.cougaar.message.protocol.email.outboxes.NodeA=smtp,atom,25,-,-,- -Dorg.cougaar.message.protocol.email.inboxes.NodeB=pop3,atom,110,node2,passwd -Dorg.cougaar.message.protocol.email.outboxes.NodeB=smtp,atom,25,-,-,- 

REM -Dorg.cougaar.message.protocol.udp.outgoing.inbandAckSoTimeout=1000

REM -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.TrafficAuditAspect,org.cougaar.core.mts.NameSupportCacheAspect,org.cougaar.core.mts.RMISocketControlAspect,org.cougaar.core.mts.StatisticsAspect,org.cougaar.core.mts.RMISendTimeoutAspect,org.cougaar.core.mts.MessageOrderingAspect,org.cougaar.core.mts.MessageSendHistoryAspect,org.cougaar.core.mts.acking.MessageAckingAspect,org.cougaar.core.mts.RTTAspect,org.cougaar.core.mts.MessageNumberingAspect

REM other Aspects
REM org.cougaar.core.mts.NameSupportTimeoutAspect,
REM org.cougaar.core.mts.MessageProtectionAspect,
REM org.cougaar.core.mts.ShowTrafficAspect,
REM org.cougaar.core.mts.TraceAspect
REM org.cougaar.core.mts.StationTraceAspect
REM org.cougaar.core.mts.TrafficAuditAspect,

REM to avoid security exception when it tries to read .mailcap
REM -Duser.home=%COUGAAR_INSTALL_PATH%\workspace

REM other Protocols
REM org.cougaar.core.mts.socket.IncomingSocketLinkProtocol,org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol,
REM org.cougaar.core.mts.udp.IncomingUDPLinkProtocol,org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol,
REM org.cougaar.core.mts.RMILinkProtocol,
REM org.cougaar.core.mts.SSLRMILinkProtocol
REM org.cougaar.core.mts.email.OutgoingEmailLinkProtocol,org.cougaar.core.mts.email.IncomingEmailLinkProtocol

REM tuning props
REM org.cougaar.message.transport.aspects.acking.msgAgeWindowInMinutes=3000
REM # org.cougaar.message.transport.aspects.acking.skipIncarnationCheck=false
REM # org.cougaar.message.transport.aspects.rtt.startDelay=5
REM # org.cougaar.message.transport.aspects.rtt.percentChangeLimit=0.25
REM # rmi cost is 1000, sslrmi is 2000
REM # org.cougaar.message.protocol.udp.cost=499 
REM # org.cougaar.message.protocol.udp.cost=750
REM # org.cougaar.message.protocol.socket.cost=1000
REM # org.cougaar.message.transport.mts.topology.callTimeout=500
REM # org.cougaar.message.transport.policy.adaptive.commStartDelaySeconds=0
REM # org.cougaar.message.transport.aspects.managedNameSupport.callTimeout=500
REM #org.cougaar.message.transport.aspects.SendTimeoutAspect.fastTimeout=10000
REM #org.cougaar.message.transport.aspects.SendTimeoutAspect.slowTimeout=20000
REM org.cougaar.message.transport.aspects.SendTimeoutAspect.fastTimeout=1000
REM org.cougaar.message.transport.aspects.SendTimeoutAspect.slowTimeout=5000
REM #XXXX org.cougaar.message.protocol.udp.localhost=$HOSTNAME
REM #XXXX org.cougaar.message.protocol.socket.localhost=$HOSTNAME

REM # java.rmi.server.logCalls=true
REM # sun.rmi.transport.connectionTimeout=2000

REM # Email props
REM org.cougaar.message.protocol.email.useFQDNs=false
REM org.cougaar.message.protocol.email.mailServerPollTimeSecs=10
REM org.cougaar.message.protocol.email.initialReadDelaySecs=20
REM # org.cougaar.message.protocol.email.cost=5000
REM org.cougaar.message.protocol.email.cost=10000
REM org.cougaar.message.protocol.email.pop3.connectionTimeoutSecs=10
REM org.cougaar.message.protocol.email.smtp.connectionTimeoutSecs=10
REM org.cougaar.message.protocol.email.debugMail=false
REM org.cougaar.message.protocol.email.outgoing.maxMessageSizeKB=1000






