
The HeartbeatPiggybacker simply combines all heartbeats generated on 
a given node & attempts to piggyback them onto other messages destine 
for the same node. If no such messages are found before a HB timeout 
approaches, all HBs to a given node are packed up and sent in a single
message.

Activation
----------
To activate the HeartbeatPiggybacker one must include the class in the 
list of aspects to be loaded:

	org.cougaar.core.mts.HeartbeatPiggybackerAspect 

Test
----
To test whether the HeartbeatPiggybacker is functioning one must turn
on debugging in the logging configuration, e.g.

	log4j.category.org.cougaar.core.mts.HeartbeatPiggybackerAspect=DEBUG

One should first run the heartbeat test and use the output to compare to. 
Then, to test the HeartbeatPiggybacker, activate the aspect.

What will be seen when the HeartbeatPiggybacker is active is that the 
heartbeats emitted by monitoredNodes are being collected up and sent together.
Two possible situations could arise:

  1) A non-heartbeat msg is seen & is going to the ManagerNode. If heartbeats 
     are waiting in the queue, they will be embedded in this message.

  2) No message is seen before a hearbeat's SEND_TIMEOUT - 10 seconds is reached.
     In this case, all pending heartbeats are packed up into one message and sent
     to the ManagerNode.

Output
------

To test this the output (after initialization) in the MonitoredNode will look something like the 
following:

21:44:58,365 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() Saw HB from MonitoredAgent3

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= calculating deliverby = now() + 65secs

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() created new queue for ManagerAgentNode

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() Saw HB from MonitoredAgent2

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= calculating deliverby = now() + 65secs

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() added to a current queue: ManagerAgentNode

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() Saw HB from MonitoredAgent1

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= calculating deliverby = now() + 65secs

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= handleHeartbeat() added to a current queue: ManagerAgentNode

21:44:58,375 DEBUG HeartbeatPiggybackerAspect PB========= HB sending queue going to wait

21:46:03,378 DEBUG HeartbeatPiggybackerAspect PB==>>>>>>>> Time's up, HB sending queue forwarded HBs to ManagerAgentNode

The above shows 3 heartbeats being generated, queued for delivery & finally sent after
the timeout deadline apporaches SEND_TIMEOUT-10 seconds.




In the ManagerNode console, one will no longer see '+'s generated after initialization 
(caused by the MonitoredNodeAgents emitting heartbeats) since the heartbeats are now 
filtered off any arriving messages and sent directly to the Metrics Service.

The ManagerNode node.log will contain entries showing that the heartbeats have been received
and that they have been sent to the Metrics Service. 

21:48:18,341 DEBUG HeartbeatPiggybackerAspect PB========= deliverMessage() found msg with heartbeats

21:48:18,341 DEBUG HeartbeatPiggybackerAspect PB========= deliverHeartbeats() added to queue for delivery

21:48:18,341 DEBUG HeartbeatPiggybackerAspect PB========= delivery queue thread sent HB to metrics

21:48:18,341 DEBUG HeartbeatPiggybackerAspect PB========= delivery queue thread sent HB to metrics

21:48:18,341 DEBUG HeartbeatPiggybackerAspect PB========= delivery queue thread sent HB to metrics











