/*
 * <copyright>
 *  Copyright 2001-2004 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 *
 * CHANGE RECORD
 *  1 Mar 2004: Port to 11.0
 * 12 Feb 2003: Style police (OBJS)
 * 12 Feb 2003: Port to 10.0 (OBJS)
 * 02 Oct 2002: Add a receiver queue to manage incoming message threads. (OBJS)
 * 27 Sep 2002: Add inband acking. (OBJS)
 * 22 Sep 2002: Revamp for new serialization & socket closer. (OBJS)
 * 18 Aug 2002: Various enhancements for Cougaar 9.4.1 release. (OBJS)
 * 11 Jun 2002: Move to Cougaar threads. (OBJS)
 * 16 May 2002: Port to Cougaar 9.2.x (OBJS)
 * 08 Apr 2002: Port to Cougaar 9.1.0 (OBJS)
 * 21 Mar 2002: Port to Cougaar 9.0.0 (OBJS)
 * 02 Jan 2002: Added idleTimeoutSecs property. (OBJS)
 * 11 Dec 2001: Revamp reusable threads, add reusable threads properties. (OBJS)
 * 09 Nov 2001: Two main changes: close idle sockets (when SO timeout occurs),
 *              and loop to restore failed server sockets. (OBJS) 
 * 05 Nov 2001: Get name of local host from the new localhost property if
 *              it is defined, else use our own method of determining the
 *              local host (Note: since FQDN names are not available on
 *              Windows until Java 1.4, our method currently gets the IP
 *              address of the current host). (OBJS)
 * 29 Oct 2001: Conditionally print "<S" on successful message receipt. (OBJS)
 * 25 Oct 2001: Changed to a no header ObjectInputStream to help fix socket 
 *              stream corruption bug. (OBJS)
 * 23 Oct 2001: Added "terminating socket" debug msg.  (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol, add debug
 *              property. (OBJS)
 * 25 Sep 2001: Port to Cougaar 8.4.1 (OBJS)
 * 14 Sep 2001: Port to Cougaar 8.4. (OBJS)
 * 22 Aug 2001: Revamped for new 8.3.1 component model. (OBJS)
 * 11 Jul 2001: Added initial name server support. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Vector;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.DataIntegrityException;
import org.cougaar.mts.std.DataValidityException;
import org.cougaar.mts.std.MessageDeserializationException;
import org.cougaar.mts.std.MessageSerializationUtils;
import org.cougaar.mts.std.MessageUtils;
import org.cougaar.mts.std.RTTService;
import org.cougaar.mts.std.SocketClosingService;
import org.cougaar.mts.std.acking.AckList;
import org.cougaar.mts.std.acking.MessageAckingAspect;
import org.cougaar.mts.std.acking.NumberList;
import org.cougaar.mts.std.acking.PureAckMessage;
import org.cougaar.mts.std.acking.PureAckAck;
import org.cougaar.mts.std.acking.PureAckAckMessage;

/**
 * IncomingSocketLinkProtocol is an IncomingLinkProtocol which receives
 * Cougaar messages from other nodes in the society via raw Internet sockets.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.core.mts.socket.IncomingSocketLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. 
 * -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol,
 * org.cougaar.core.mts.socket.IncomingSocketLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.socket.localhost</b> 
 * Setting this property to the hostname (e.g. FQDN) of the local host 
 * causes the socket to be registered using that hostname instead of 
 * the default, the local host's IP address.  This property is particularly
 * useful when an address-translating firewall separates Cougaar nodes.
 * <br>(e.g. -Dorg.cougaar.message.protocol.socket.localhost=ul118.isotic.org)           
 * <p>
 * <b>org.cougaar.message.protocol.socket.debug</b> 
 * If true, prints debug information to System.out.
 * */

public class IncomingSocketLinkProtocol extends IncomingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-socket";

  private static SocketSpec emptySpec; //100

  private static final String localhost;
  private static final boolean doInbandAcking;
  private static final boolean doInbandRTTUpdates;
  private static final boolean useMessageDigest;
  private static final String messageDigestType;
  private static final int socketTimeout;
  private static final int firstMsgSoTimeout;
  private static final int subsequentMsgsSoTimeout;
  private static final int inbandAckAckSoTimeout;
  private static final int serverSocketTimeout;
  private static final int backlog;
  private static final int minThreads;
  private static final int maxThreads;
  private static final int idleTimeout;
  private static final int numInvalidMsgs;
  private static final int timeWindowSecs;
  private static final int minPortNumber;
  private static final int maxPortNumber;

  private static LoggingService log;
  private static int RID;

  private SocketClosingService socketCloser;
  private RTTService rttService;
  private String nodeID;
  private boolean showTraffic;
  private SocketSpec socketSpecs[];
  private SocketSpec mySocketSpec;
  private Vector serverSocketListeners;
  //102B private EventWindow serverSocketMoveTrigger;
  private WorkQueue receiverQueue;
  //102B private ThreadService threadService;
  private MessageAddress myAddress;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.socket.localhost";
    localhost = System.getProperty (s, getLocalHost());

    s = "org.cougaar.message.protocol.socket.doInbandAcking";
    doInbandAcking = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.socket.doInbandRTTUpdates";
    doInbandRTTUpdates = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.socket.useMessageDigest";
    useMessageDigest = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.socket.messageDigestType";
    messageDigestType = System.getProperty (s, "MD5");

    s = "org.cougaar.message.protocol.socket.incoming.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"10000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.firstMsgSoTimeout";
    firstMsgSoTimeout = Integer.valueOf(System.getProperty(s,"500")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.subsequentMsgsSoTimeout";
    subsequentMsgsSoTimeout = Integer.valueOf(System.getProperty(s,"100")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.inbandAckAckSoTimeout";
    inbandAckAckSoTimeout = Integer.valueOf(System.getProperty(s,"3000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketTimeout";
    serverSocketTimeout = Integer.valueOf(System.getProperty(s,"0")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketBacklog";
    backlog = Integer.valueOf(System.getProperty(s,"50")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.receiverQueueMinThreads";
    minThreads = Integer.valueOf(System.getProperty(s,"1")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.receiverQueueMaxThreads";
    maxThreads = Integer.valueOf(System.getProperty(s,"5")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.receiverQueueIdleThreadTimeout";
    idleTimeout = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketMoveTriggerNumInvalidMsgs";
    numInvalidMsgs = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketMoveTriggerTimeWindowSecs";
    timeWindowSecs = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.protocol.udp.incoming.minPortNumber";
    minPortNumber = Integer.valueOf(System.getProperty(s,"1024")).intValue();

    s = "org.cougaar.message.protocol.udp.incoming.maxPortNumber";
    maxPortNumber = Integer.valueOf(System.getProperty(s,"65536")).intValue();
  }
 
  public IncomingSocketLinkProtocol ()
  {
    serverSocketListeners = new Vector();
  }

  public void load () 
  {
    super_load();
    log = loggingService;

    if (doInfo()) log.info ("Creating " + this);
    if (doInfo()) log.info ("Using " +localhost+ " as the name of the local host");

    if (socketTimeout > 0 || serverSocketTimeout > 0)
    {
      ServiceBroker sb = getServiceBroker();
      socketCloser = (SocketClosingService) sb.getService (this, SocketClosingService.class, null);
      if (socketCloser == null) log.error ("Cannot do socket timeouts - socket closing service not available!");
    }

    //100
    try {
      //100 emptySpec = new SocketSpec("",""); 
      emptySpec = new SocketSpec("localhost","0"); 
    } catch (URISyntaxException e) {
      String s = "load: error creating emptySpec for " + this;
      log.error(s);
      log.error(stackTraceToString(e));
      throw new RuntimeException(s);
    }

    //  Get incoming socket portnumber(s) from a property?
    String port = "0";   // port of 0 means auto-choose port number

    //  Create a single socket spec for now
    socketSpecs = new SocketSpec[1];

    //100
    try {
      socketSpecs[0] = new SocketSpec (localhost, port);
    } catch (URISyntaxException e) {
      String s = "failure creating SocketSpec for host=" + localhost + ", port=" + port;
      log.error(s);
      log.error(stackTraceToString(e));
      throw new RuntimeException(s);
    }

    if (serverSocketTimeout > 0)
    {
      log.error ("Server socket timeouts not yet supported");
    }

    if (doInbandRTTUpdates)
    {
      rttService = (RTTService) getServiceBroker().getService (this, RTTService.class, null);
    }

    nodeID = getRegistry().getIdentifier();

    String s = "org.cougaar.core.mts.ShowTrafficAspect";
    showTraffic = (getAspectSupport().findAspect(s) != null);

    if (startup() == false)
    {
      String str = "Failure starting up " + this;
      log.error (str);
      throw new RuntimeException (str);
    }
  }

  public synchronized boolean startup ()
  {
    shutdown();

    //  Create the receiver work queue

    receiverQueue = new WorkQueue (minThreads, maxThreads, idleTimeout, log); 

    //  Create any sockets in socketSpecs

    int count = 0;

    for (int i=0; i<socketSpecs.length; i++) 
    {
      int port = createServerSocketListener (socketSpecs[i].getPortAsInt());
      try { //100
        socketSpecs[i].setPort (port);  // potentially new port number
      } catch (URISyntaxException e) {
        String s = "startup: failure creating URI with port=" + port;
        log.error(s);
        log.error(stackTraceToString(e));
        throw new RuntimeException(s);
      }
      if (port > 0) count++;          // if port < 1 bad port
    }

    //  If any sockets were specified, make sure at least one is working

    return (socketSpecs.length > 0) ? (count > 0) : true;
  }

  public synchronized void shutdown ()
  {
    myAddress = null;
    unregisterSocketSpec();

    //  Shut down the receiver work queue

    if (receiverQueue != null)
    {
      receiverQueue.shutdown();
      receiverQueue = null;
    }

    //  Remove and quit all of the server socket listners

    for (Enumeration e = serverSocketListeners.elements(); e.hasMoreElements(); ) 
    {
       ServerSocketListener listener = (ServerSocketListener) e.nextElement();
       serverSocketListeners.remove (listener);
       listener.quit();
    }
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  private void registerSocketSpec (SocketSpec spec)
  {
    //  Update the name server.  Note: Allow any exceptions to be thrown.
  
    mySocketSpec = spec;
    URI uri = mySocketSpec.getURI(); //100
    MessageAddress address = MessageAddress.getMessageAddress (nodeID+ "(MTS" +PROTOCOL_TYPE+ ")"); //100
    getNameSupport().registerAgentInNameServer (uri, address, PROTOCOL_TYPE); //100
  }

  private void unregisterSocketSpec ()
  {
    //  Update the name server

    try
    {
      if (mySocketSpec == null) return;
      MessageAddress address = MessageAddress.getMessageAddress (nodeID+ "(MTS" +PROTOCOL_TYPE+ ")"); //100
      getNameSupport().unregisterAgentInNameServer (mySocketSpec.getURI(), address, PROTOCOL_TYPE); //100
    }
    catch (Exception e)
    {
      log.error ("unregisterSocketSpec: " +stackTraceToString(e));
    }
  }

  public final void registerClient (MessageTransportClient client) 
  {
    //  Useful in adaptive link selection to know if link is good for agent

    try 
    {
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().registerAgentInNameServer (emptySpec.getURI(), clientAddress, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error ("registerClient: " +stackTraceToString(e));
    }
  }

  public final void unregisterClient (MessageTransportClient client) 
  { 
    //  Useful in adaptive link selection to know if link is good for agent

    try 
    {
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().unregisterAgentInNameServer (emptySpec.getURI(), clientAddress, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error ("unregisterClient: " +stackTraceToString(e));
    }
  }

  public final void registerMTS (MessageAddress address)
  {
    //  Of uknown utility

    try 
    {
      getNameSupport().registerAgentInNameServer (emptySpec.getURI(), address, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error ("registerMTS: " +stackTraceToString(e));
    }
  }

  private int createServerSocketListener (int port)
  {
    ServerSocketListener listener = null;

    try
    {
      listener = new ServerSocketListener (port);
      port = listener.getPort();  // port possibly updated

      Schedulable thread = threadService.getThread (this, listener, "ServerSock_"+port);
      //102B Thread thread = new Thread (listener, "ServerSock_"+port);
      thread.start();

      registerSocketSpec (new SocketSpec (localhost, port));
      serverSocketListeners.add (listener);

      if (doDebug()) log.debug ("Incoming server socket created on port " +port);
    }
    catch (Exception e)
    {
      log.error ("Creating server socket listener on port " +port+ ": " +stackTraceToString(e));
      if (listener != null) listener.quit();
      listener = null;
      return -1;
    }

    return port;
  }

  private void destroyServerSocketListener (ServerSocketListener listener)
  {
    if (listener == null) return;
    int port = listener.getPort();

    try
    {
      if (doDebug()) log.debug ("Server socket listener on port " +port+ " destroyed");
      listener.quit();
      // unregisterSocketSpec();   // synchronization problem?
      serverSocketListeners.remove (listener);
    }
    catch (Exception e)
    {
      log.error ("Error destroying server socket on port " +port+ ": " +e);
    }
  }

  private class ServerSocketListener implements Runnable
  { 
    private ServerSocket serverSock;
    private EventWindow moveTrigger;
    private boolean quitNow;

    public ServerSocketListener (int port) throws IOException
    {
      //  Create a server socket at the specified port or at a random port

      int tryN = 0;
      int origPort = port;
      serverSock = null;

      while (serverSock == null)
      {
        tryN++;

        if (origPort == 0) port = getRandomNumber (minPortNumber, maxPortNumber);
        else if (tryN > 5) port = 0;  // let system decide

        try
        {
          serverSock = new ServerSocket (port, backlog);
        }
        catch (SocketException e)
        {
          // port most likely in use
          if (log.isDebugEnabled()) log.debug(null,e);
        }
      }

      moveTrigger = new EventWindow (numInvalidMsgs, timeWindowSecs*1000);
    }

    private int getRandomNumber (int min, int max)
    {
      return min + (int) Math.rint (Math.random() * (max-min));
    }

    public int getPort ()
    {
      return (serverSock != null) ? serverSock.getLocalPort() : -1;
    }

    public synchronized void quit ()
    {
      quitNow = true;

      if (serverSock != null)
      {
        try { serverSock.close(); } catch (Exception e) {
          if (log.isDebugEnabled()) log.debug(null,e);}
        serverSock = null;
      }
    }

    public void run () 
    {
      while (!quitNow)
      {
        String s = "Unexpected exception, restarting thread";

        try
        { 
          try 
          { 
            doRun(); 
          } 
          catch (Exception e) 
          {
            if (!quitNow)
            {
              s += ": " + stackTraceToString (e);
              log.error (s);
            }
          }
        }
        catch (Exception e)
        {
          try { e.printStackTrace(); } catch (Exception ex) { 
            if (log.isDebugEnabled()) log.debug(null,e);}
        }
      }
    }

    private void doRun () 
    {
      if (serverSock == null) return;

      while (!quitNow)
      {
        try 
        {
          //  If we are getting too many invalid messages, its time to move to another port

          if (moveTrigger.hasTriggered())
          {
            if (doWarn()) log.warn ("Too many invalid messages, moving server socket");
            receiverQueue.flush();  // flush any outstanding work (likely more bad msgs)
            quitNow = true;
            break;
          }

          //  Sit and wait for an incoming connection request

          Socket socket = serverSock.accept();
          if (!doInbandAcking) socket.shutdownOutput();  // we'll be only reading

          //  Add a new message listener to our work queue

          receiverQueue.add (new MessageInListener (socket, moveTrigger));
        }
        catch (Exception e) 
        {
          if (doWarn()) log.warn ("Waiting for or processing new connection: " +stackTraceToString(e));
          quitNow = true;
          break;
        }
      }

      //  Close the server socket

      if (serverSock != null)
      {
        try { serverSock.close(); } catch (Exception e) {
          if (log.isDebugEnabled()) log.debug(null,e);}
        serverSock = null;
      }

      //  Destroy this server socket listener and create another

      destroyServerSocketListener (this);
      createServerSocketListener (0);  // new server socket on new port
    }
  }

  private synchronized static int getNextReceiveID ()  // for debugging purposes
  {
    return RID++;
  }

  private class MessageInListener implements QuitableRunnable
  { 
    private Socket socket;
    private EventWindow serverSocketMoveTrigger;
    private BufferedInputStream socketIn;
    private BufferedOutputStream socketOut;
    private String sockString, rid;
    private boolean quitNow;

    public MessageInListener (Socket socket, EventWindow serverSocketMoveTrigger) throws IOException
    {
      this.socket = socket;
      this.serverSocketMoveTrigger = serverSocketMoveTrigger;

      if (doDebug()) 
      {
        sockString = socket.toString();
        log.debug ("Creating new msg in listener for " +sockString);
      }

      socketIn = new BufferedInputStream (socket.getInputStream());
      if (doInbandAcking) socketOut = new BufferedOutputStream (socket.getOutputStream());
    }

    public void quit ()
    {
      quitNow = true;
    }

    public void run () 
    {
      boolean firstMsg = true;
      byte[] msgBytes = null;
      long receiveTime = 0, sendTime = 0;
 
      while (!quitNow)
      {
        //  Sit and wait for a message till socket timeout.  We distinguish between
        //  first and subsequent message timeouts because the first message will incur
        //  the socket connection overhead while any others won't, and we are in a race
        //  condition with the message sender - he has to send the message fast enough
        //  after establishing the connection to catch this side of the connection 
        //  still alive.  
        //
        //  The subsequentMsgsSoTimeout property controls the likelihood of multiple
        //  messages over this connection.  Be careful about setting it to 0, you may
        //  not reliably know if there are any other messages in the pipe that you
        //  will just be throwing away if you do so.

        try 
        {
          socket.setSoTimeout (firstMsg? firstMsgSoTimeout : subsequentMsgsSoTimeout);
          firstMsg = false;
          scheduleSocketClose (socket, socketTimeout);

          //  Read a message

          if (doDebug())
          {  
             rid = "r" +getNextReceiveID()+ " ";
             log.debug (rid+ "Waiting for msg from " +sockString);
          }

          msgBytes = MessageSerializationUtils.readByteArray (socketIn);
          receiveTime = now();
          if (doDebug()) log.debug (rid+ "Waiting for msg done " +sockString);
          unscheduleSocketClose (socket);
          if (showTraffic) System.out.print("<S");
        }
        catch (InterruptedIOException e)
        {
          //  Socket SO timeout (set above).  Socket is still good, but we will close
          //  it as a timeout has been reached.

          if (doDebug()) log.debug (rid+ "Closing socket due to SO timeout: " +e);
          quitNow = true;
          break;
        }
        catch (EOFException e)
        { 
          //  Typically raised when the party at the  other end closes their connection.  
          //  If the OutgoingSocketLinkProtocol has oneSendPerConnection set to true, it will
          //  close its connection after each message send and this will happen all the time.

          if (doDebug()) log.debug (rid+ "Remote socket closed: " +e);
          quitNow = true;
          break;
        }
        catch (DataValidityException e)
        {
          //  A non-Cougaar message.  We should not be getting these.  Counts towards the 
          //  server socket move trigger.

          if (doDebug()) log.debug (rid+ "Non-Cougaar message received (msg ignored)");
          serverSocketMoveTrigger.addEvent();            
          quitNow = true;
          break;
        }
        catch (DataIntegrityException e)
        {
          if (doDebug()) log.debug (rid+ "Incomplete or damaged message received (msg ignored): "+e);
          quitNow = true;
          break;
        }
        catch (Exception e)
        { 
          //  Some other exception.  Prints the stack trace for more detail.

          if (doDebug()) log.debug (rid+ "Terminating socket exception: " +stackTraceToString(e));
          quitNow = true;
          break;
        }

        //  Deserialize the read bytes into a Cougaar message

        AttributedMessage msg = null;
        Exception exception = null;

        try
        {
          msg = MessageSerializationUtils.readMessageFromByteArray (msgBytes);
        }
        catch (MessageDeserializationException e)
        {
          if (doWarn()) log.warn (rid+ "Deserialization exception (msg ignored): " +e);
          exception = e;

          //  Certain kinds of deserialization errors occur to invalid messages being
          //  received, messages that should not be being sent to us, and so they count
          //  towards the server socket move trigger.

          Throwable cause = exception.getCause();

          if (cause instanceof DataValidityException)
          {
            serverSocketMoveTrigger.addEvent();            
            quitNow = true;
            break;
          }
        }

        //  Set receive time for RTT service

        if (rttService != null) rttService.setMessageReceiveTime (msg, receiveTime);

        //  Deliver the message if we have one

        String msgString = null;

        if (msg != null)
        {
          if (doDebug()) 
          {
            InetAddress addr = socket.getInetAddress();
            int port = socket.getPort();
            msgString = MessageUtils.toString (msg);
            log.debug (rid+ "From " +addr+ ":" +port+ " read " +msgString);
          }

          try
          {
            getDeliverer().deliverMessage (msg, msg.getTarget());
          }
          catch (MisdeliveredMessageException e)
          { 
            if (doDebug()) log.debug (rid+ "Delivery exception for " +msgString+ ": " +e);
            exception = e;
          }
          catch (Exception e)
          { 
            if (doDebug()) log.debug (rid+ "Exception delivering " +msgString+ ": " +stackTraceToString(e));
            exception = e;
          }
        }

        //  Optionally do inband acking

        if (doInbandAcking && (msg == null || MessageUtils.isAckableMessage (msg)))
        {
          try
          {
            //  Send an ack

            byte[] ackBytes = createAck (msg, receiveTime, exception);
            scheduleSocketClose (socket, socketTimeout);
            if (doDebug()) log.debug (rid+ "Sending ack thru " +sockString);
            sendTime = now();
            MessageSerializationUtils.writeByteArray (socketOut, ackBytes);
            if (doDebug()) log.debug (rid+ "Sending ack done " +sockString);
            unscheduleSocketClose (socket);

            //  See if we get an ack-ack back  
            //
            //  Note:  If there was an reception exception with the original message, 
            //  we don't wait for an ack-ack because there is not much point as well
            //  as the fact that it may be a non-Cougaar entity that sent the original
            //  message and waiting for an ack-ack from them is more than useless.

            if (exception == null)
            {
              scheduleSocketClose (socket, socketTimeout);
              if (doDebug()) log.debug (rid+ "Waiting for ack-ack from " +sockString);
              byte[] ackAckBytes = MessageSerializationUtils.readByteArray (socketIn);
              receiveTime = now();
              if (doDebug()) log.debug (rid+ "Waiting for ack-ack done " +sockString);
              unscheduleSocketClose (socket);
              PureAckAckMessage paam = processAckAck (rid, ackAckBytes);

              //  Update inband RTT

              if (rttService != null)
              {
                String node = MessageUtils.getFromAgentNode (msg);
                String recvLink = IncomingSocketLinkProtocol.this.toString();
                int rtt = ((int)(receiveTime - sendTime)) - paam.getInbandNodeTime();
                rttService.updateInbandRTT (node, recvLink, rtt);
              }
            }
            else
            {
              if (doDebug()) log.debug (rid+ "Not waiting for ack-ack due to " +
                "message reception exception " +sockString);
            }
          }
          catch (Exception e)
          {
            //  Any acking that did not complete will be taken care of in later acking

            if (doDebug()) log.debug (rid+ "Inband acking stopped for " +sockString+ ": " +e);
            quitNow = true;
            break;
          }
        }
      }

      //  Cleanup

      closeSocket();
      if (doDebug()) log.debug (rid+ "End of msg in listener for " +sockString);
    }

    private byte[] createAck (AttributedMessage msg, long receiveTime, Exception exception) throws Exception
    {
      PureAckMessage pam = PureAckMessage.createInbandPureAckMessage (msg);

      if (exception != null) 
      {
        pam.setReceptionException (exception);
        pam.setReceptionNode (nodeID);
      }

      pam.setInbandNodeTime ((int)(now()-receiveTime));
      byte ackBytes[] = MessageSerializationUtils.writeMessageToByteArray (pam, getDigest());
      return ackBytes;
    }

    private PureAckAckMessage processAckAck (String rid, byte[] ackAckBytes) throws Exception
    {
      AttributedMessage msg = MessageSerializationUtils.readMessageFromByteArray (ackAckBytes);
      PureAckAckMessage paam = (PureAckAckMessage) msg;
      PureAckAck pureAckAck = (PureAckAck) MessageUtils.getAck (paam);
      Vector specificAcks = pureAckAck.getSpecificAcks();
      Vector latestAcks = pureAckAck.getLatestAcks();
      String fromNode = MessageUtils.getFromAgentNode (paam);

      if (doDebug())
      {
        StringBuffer sbuf = null, lbuf = null;

        if (specificAcks != null)
        {
          sbuf = new StringBuffer();
          AckList.printAcks (sbuf, "specific", specificAcks);
        }

        if (latestAcks != null)
        {
          lbuf = new StringBuffer();
          AckList.printAcks (lbuf, "  latest", latestAcks);
        }

        if (sbuf != null || lbuf != null)
        {
          StringBuffer buf = new StringBuffer();
          buf.append ("Got inband ack-ack:\n");
          if (sbuf != null) buf.append (sbuf);
          if (lbuf != null) buf.append ("\n"+lbuf);
          log.debug (rid+buf.toString());
        }
        else log.debug (rid+ "Got an empty inband ack-ack!");
      }

      if (specificAcks != null)
      {
        for (Enumeration a=specificAcks.elements(); a.hasMoreElements(); )
          NumberList.checkListValidity ((AckList) a.nextElement());

        MessageAckingAspect.removeAcksToSend (fromNode, specificAcks);
      }

      if (latestAcks != null)
      {
        for (Enumeration a=latestAcks.elements(); a.hasMoreElements(); )
          NumberList.checkListValidity ((AckList) a.nextElement());

        MessageAckingAspect.addReceivedAcks (fromNode, latestAcks);
      }

      return paam;
    }

    private void closeSocket ()
    {
      if (socket != null)
      {
        try { socket.close(); } catch (Exception e) {
          if (log.isDebugEnabled()) log.debug(null,e);}

        socket = null;
        socketOut = null;
        socketIn = null;
      } 
    } 
  }

  private void scheduleSocketClose (Socket socket, int timeout)
  {
    if (socketCloser != null) socketCloser.scheduleClose (socket, timeout);
  }

  private void unscheduleSocketClose (Socket socket)
  {
    if (socketCloser != null) socketCloser.unscheduleClose (socket);
  }

  private static String getLocalHost ()
  {
    //  Not till Java 1.4 can we get the fully qualified domain name
    //  (FQDN) for hosts (via the new InetAddress getCanonicalHostname())
    //  when running on Windows.  So for now we will return the host
    //  as an IP address.  If that doesn't work, such as in firewall
    //  situations, the localhost property can be used to set the
    //  name or IP address to use (see above).

    try
    {
      // return InetAddress.getLocalHost().getHostAddress();    // hostname as IP address
      return InetAddress.getLocalHost().getCanonicalHostName(); // now using 1.4
    }
    catch (Exception e)
    {
      log.error ("Getting FQDN for localhost: " +stackTraceToString(e));
      throw new RuntimeException (e.toString());
    }
  }

  private static MessageDigest getDigest () throws java.security.NoSuchAlgorithmException
  {
    return (useMessageDigest ? MessageDigest.getInstance(messageDigestType) : null);
  }

  private boolean doDebug ()
  {
    return (log != null && log.isDebugEnabled());
  }

  private boolean doInfo ()
  {
    return (log != null && log.isInfoEnabled());
  }

  private boolean doWarn ()
  {
    return (log != null && log.isWarnEnabled());
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
