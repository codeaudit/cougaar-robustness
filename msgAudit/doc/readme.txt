
Summary
-------
MessageAudit is a separate external application that collects and displays
special log messages embedded within Cougaar classes. This enables one to 
monitor logging in real time. Currently, it is used primarily to monitor
successful retrieval of Messages from sender to receiver. However, it could be
used for more general debugging purposes.

While this is sufficient for some purposes, the real benefit of MessageAudit 
is when it is used in conjunction with OBJS' Dynamic Java Software Instrumentation
tool -- ProbeMeister. ProbeMeister enables one to insert logging statements in
Cougaar code on-the-fly, while Cougaar is up & running. MessageAudit will 
then display these messages as they are generated. This saves one the need to stop
an execution, add debug/logging statements, recompile, and then restart.
Moreover, using ProbeMeister one can dynamically remove any of the logging
statements they've inserted using ProbeMeister.

This readme first describes the use of MessageAudit, and then provides an overview
of use with ProbeMeister (a separably licensable tool).

* MessageAudit also includes an offline analysis capability using "TrafficAudit".
  This analyzes traffic logs for missing messages. See the usage in TrafficAudit 
  for details on command line arguments.


MessageAudit Intro
------------------
The basic capability of MessageAudit enables one to send log messages via
a socket to a gui to watch them being generated in "real time." Log messages 
need to be of the form:

        LogEventRouter.getRouter().routeEvent( <LogEventWrapper object> );

Thus, the Log messages need to use a special LogEventWrapper class to be consummable
by MessageAudit. This wasn't possible with Cougaar events.
[The LogEvent classes are located in the OBJS Common module.]

** Look at MsgLog's MessageTrafficAudit for an example of how to use MessageAudit.


Creating Log Messages
---------------------

To log a message one does the following:


       1) Create a data structure to hold the log-specific data

        String[] data = 
           {"TYPE", "TRAFFIC_EVENT", "lpName", tag, "time", ""+now(), "from", from, "to", to, "num", numS};


         where  the variables in the data structure "data" can be modified.
         They are:
              tag - is the log point name used to identify the origination point of the log message
              now() - current time. Could use Cougaar time if desired
              from - the originating agent -- cannot be null
              to   - the destination agent -- cannot be null
              numS - the sequence # of the message, allowing messages to be grouped.

       2) Wrap the data in a LogEventWrapper

            LogEventWrapper lew = 
               new LogEventWrapper(log, LoggingService.INFO, data, null, "LP");

       3) Emit the message             

            LogEventRouter.getRouter().routeEvent(lew);


Operational Note
----------------

When MessageAudit is enabled, the logged messages are sent to the specified socket.
Otherwise, the messages are emitted (as is normally done) to the Cougaar logger 
that is passed into the LogEventWrapper. So, using this logging approach maintains 
the customary behavior of Cougaar logging when MessageAudit is not activated.

Configuring Cougaar to use MessageAudit
---------------------------------------

1. Insert debug / log statements as described above & as shown in the cited example.

2. Turn on MessageAudit by including the following properties when starting
   each participating node:

REM ***********************
REM *** MESSAGE AUDIT ON
REM ***********************
set MESSAGE_AUDIT=-Dorg.cougaar.core.mts.logging.Log4JHost=10.0.0.1 -Dorg.cougaar.core.mts.logging.Log4JPort=7887 -Dorg.cougaar.core.mts.logging.Log4JLevel=DEBUG 

REM ** The value in configs/common/alpreg.ini may affect the operation of socket-based  
REM    logging.
REM ** If running on one PC with no network, the Log4JHost & the value in the ini file   
REM    should be the same.


org.cougaar.core.mts.logging.Log4JHost      - specifies the host that is running the 
                                              MessageAudit application.

org.cougaar.core.mts.logging.Log4JPort      - specifies the port that MessageAudit's socket
                                              is running on.

org.cougaar.core.mts.logging.Log4JLevel     - specifies the logging level of the messages to
                                              emit (used exactly as Cougaar logging levels).

** The port value must be the same value passed into MessageAudit when it is started.


3. Start MessageAudit - This must be started first, since the socket needs to be created
                        before Cougaar can sent events to it. Details on this are below.

4. Start Cougaar.


Configuring & Starting MessageAudit
-----------------------------------

1. See the file /test/runMessageAudit.bat to see the classpaths involved. The command line to 
   start MessageAudit is:

      java -classpath %LIBPATHS% 
           org.cougaar.tools.robustness.audit.msgAudit.TrafficAuditor 
           -config LogPointInfo.xml  
           -port 7887 

	- The port # is the one used/explain above (Log4JPort)

	- The "-config" file, LogPointInfo.xml, specifies information about the already embedded 
	   log messages. Details on this below (see "Configuration Files").


When you run MessageAudit, a window appears that shows the list of Agents that have emitted 
MessageAudit-compliant log messages (at first it is empty). This list automatically grows as
log messages are captured and processed. 

As MessageAudit was specifically designed to monitor the successful retrieval of messages from
sender to receiver, the window displays the number of msgs sent and received, as well as the
difference -- or the number outstanding.

The configuration file one supplies on the command-line defines the "log points" that represent 
message traffic. There are two message stacks -- a send stack, and a receive stack. While this 
file is not necessary, MessageAUdit uses the specifications to make a determination as to when 
a message is considered received. It also enables the user to filter log messages and only watch
the log messages s/he is interested in. It is possible to watch log messages being generated 
without concern as to whether a message is received or not -- the log messages don't even need
to be related to messaging, but the interface has been implemented toward this purpose.

Messages that have not been described in the configuration file are displayed along with the
described ones. However, they play no role in determining if a message has been received. 
Thus, the configuration file does not restrict the reception of undocumented log messages.
Using the GUI, one can view these "Log Points" and redefine which log point should be used to 
make the determination as to whether a message has been successfully received, if desired.

To view the received messages, one double-clicks on a given agent, and the message details 
are then displayed.


Configuration Files
-------------------

Basically, one defines a set of LogPoints -- known points at which log msgs are currently 
generated (statically defined LogEventWrappers) from within the Cougaar code. Each LogPoint 
is either in the Send stack or recv stack, and has an assigned order # (according to execution order).
Numbers should be sequential and increasing for the send stack, and decreasing to 0 for the 
receive stack. An example is in /test/LogPointInfo.xml file.

In the MessageAudit LogPoint configuration gui window, you can them designate which receive 
stack log point is the "final" point -- the point at which a message should be considered to
be successfully received. This is important only for counting messages as being successfully 
received & has no other operational effect.

If you use ProbeMeister to insert new log points (log messages) into the Cougaar code, these
points will automatically show up in the configuration gui as the messages are received. You may
then redefine the "final" log point to be one of these new log points, if so desired. 
 

Dynamic Insertion of LogPoints
------------------------------

Using OBJS's ProbeMeister one can dynamically insert new log messages / debug statements into
running Cougaar code & then watch for them to appear in MessageAudit. This enables one to short-
circuit the standard debugging cycle entirely. 

ProbeMeister contains a special software probe that will insert a user-customized log message 
(LogEventWrapper) at any point in the code. To work, the user need only select the class from
a list in ProbeMeister, then the method, and then drag and drop the probe into the position in
the method. From that point on, that modified method will emit the log message whenever execution
flow dictates. Added probes can be just as easily removed.

ProbeMeister is separately licensable from OBJS.

















