/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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

package org.cougaar.core.mts.socket;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.acking.*;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


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

  private static final String localhost;
  private static final boolean doInbandAcking;
  private static final boolean useMessageDigest;
  private static final String messageDigestType;
  private static final int firstMsgSoTimeout;
  private static final int subsequentMsgsSoTimeout;
  private static final int socketTimeout;
  private static final int serverSocketTimeout;
  private static final int backlog;

  private static LoggingService log;
  private SocketClosingService socketCloser;
  private boolean showTraffic;
  private SocketSpec socketSpecs[];
  private SocketSpec mySocket;
  private Vector serverSocketListeners;
  private Vector messageInListeners;
  private ThreadService threadService;
  private MessageAddress myAddress;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.socket.localhost";
    localhost = System.getProperty (s, getLocalHost());

    s = "org.cougaar.message.transport.socket.doInbandAcking";
    doInbandAcking = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.socket.useMessageDigest";
    useMessageDigest = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.socket.messageDigestType";
    messageDigestType = System.getProperty (s, "MD5");

    s = "org.cougaar.message.protocol.socket.incoming.firstMsgSoTimeout";
    firstMsgSoTimeout = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.subsequentMsgsSoTimeout";
    subsequentMsgsSoTimeout = Integer.valueOf(System.getProperty(s,"100")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"10000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketTimeout";
    serverSocketTimeout = Integer.valueOf(System.getProperty(s,"0")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.serverSocketBacklog";
    backlog = Integer.valueOf(System.getProperty(s,"50")).intValue();
  }
 
  public IncomingSocketLinkProtocol ()
  {
    serverSocketListeners = new Vector();
    messageInListeners = new Vector();

    //  Get server socket portnumber(s) from a property?

    String port = "0";   // port of 0 means auto-choose port number

    //  Create a single socket spec for now

    socketSpecs = new SocketSpec[1];
    socketSpecs[0] = new SocketSpec (localhost, port);
  }

  public void load () 
  {
    super_load();
    log = loggingService;

    if (log.isInfoEnabled()) log.info ("Creating " + this);
    if (log.isInfoEnabled()) log.info ("Using " +localhost+ " as the name of the local host");

    if (socketTimeout > 0 || serverSocketTimeout > 0)
    {
      ServiceBroker sb = getServiceBroker();
      socketCloser = (SocketClosingService) sb.getService (this, SocketClosingService.class, null);
      if (socketCloser == null) log.error ("Cannot do socket timeouts - socket closing service not available!");
    }

    if (serverSocketTimeout > 0)
    {
      log.error ("Server socket timeouts not yet supported");
    }

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

    //  Create any sockets in socketSpecs

    int count = 0;

    for (int i=0; i<socketSpecs.length; i++) 
    {
      int port = createServerSocket (socketSpecs[i].getPortAsInt());
      socketSpecs[i].setPort (port);  // potentially new port number
      if (port > 0) count++;          // if port < 1 bad port
    }

    //  If any sockets were specified, make sure at least one is working

    return (socketSpecs.length > 0) ? (count > 0) : true;
  }

  public synchronized void shutdown ()
  {
    myAddress = null;
    unregisterSocketSpec();

    //  Remove and quit all of the server socket listners and message-in threads

    for (Enumeration e = serverSocketListeners.elements(); e.hasMoreElements(); ) 
    {
       ServerSocketListener listener = (ServerSocketListener) e.nextElement();
       serverSocketListeners.remove (listener);
       listener.quit();
    }

    for (Enumeration e = messageInListeners.elements(); e.hasMoreElements(); ) 
    {
       MessageInListener listener = (MessageInListener) e.nextElement();
       messageInListeners.remove (listener);
       listener.quit();
    }
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  private void registerSocketSpec (String host, int port)
  {
    //  Update the name server
  
    mySocket = new SocketSpec (host, port);
    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().registerAgentInNameServer (mySocket, nodeAddress, PROTOCOL_TYPE);
  }

  private void unregisterSocketSpec ()
  {
    //  Update the name server

    if (mySocket == null) return;  // no socket spec to unregister
    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().unregisterAgentInNameServer (mySocket, nodeAddress, PROTOCOL_TYPE);
  }

  public final void registerClient (MessageTransportClient client) 
  {
    try 
    {
      if (mySocket == null) return;  // no socket spec to register
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().registerAgentInNameServer (mySocket, clientAddress, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("registerClient: " +e);
    }
  }

  public final void unregisterClient (MessageTransportClient client) 
  { 
    try 
    {
      if (mySocket == null) return;  // no socket spec to unregister
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().unregisterAgentInNameServer (mySocket, clientAddress, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("unregisterClient: " +e);
    }
  }

  public final void registerMTS (MessageAddress addr)
  {
    try 
    {
      if (mySocket == null) return;  // no socket spec to register
      getNameSupport().registerAgentInNameServer (mySocket, addr, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("registerMTS: " +e);
    }
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
	return threadService;
  }

  private int createServerSocket (int port)
  {
    ServerSocketListener listener = null;

    try
    {
      listener = new ServerSocketListener (port);
      port = listener.getPort();  // port possibly updated

      // Schedulable thread = threadService().getThread (this, listener, "ServerSock_"+port);
      Thread thread = new Thread (listener, "ServerSock_"+port);
      thread.start();

      registerSocketSpec (localhost, port);
      serverSocketListeners.add (listener);

      if (log.isDebugEnabled()) log.debug ("Created server socket on port " +port);
    }
    catch (Exception e)
    {
      log.error ("Error creating server socket on port " +port+ ": " +stackTraceToString(e));
      if (listener != null) listener.quit();
      listener = null;
      return -1;
    }

    return port;
  }

  private void destroyServerSocket (ServerSocketListener listener)
  {
    if (listener == null) return;
    int port = listener.getPort();

    try
    {
      if (log.isDebugEnabled()) log.debug ("Server socket destroyed on port " +port);
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
    private boolean quitNow;

    public ServerSocketListener (int port) throws IOException
    {
      serverSock = new ServerSocket (port, backlog);
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
        try { serverSock.close(); } catch (Exception e) {}
        serverSock = null;
      }
    }

    public void run() 
    {
      if (serverSock == null) return;

      while (!quitNow)
      {
        try 
        {
          //  Sit and wait for an incoming socket connect request

          Socket s = serverSock.accept();
          if (!doInbandAcking) s.shutdownOutput();  // we'll be only reading

          //  Create a message listener and get a thread 
          //  to run it in and kick it off.

          MessageInListener listener = new MessageInListener (s);
          // Schedulable thread = threadService().getThread (this, listener, "MessageIn_"+s.getPort());
          Thread thread = new Thread (listener, "MessageIn_"+s.getPort());
          thread.start();
          messageInListeners.add (listener);
        }
        catch (Exception e) 
        {
          if (log.isWarnEnabled()) log.warn ("Processing new connection request: " +e);
          quitNow = true;
          break;
        }
      }

      if (serverSock != null)
      {
        try { serverSock.close(); } catch (Exception e) {}
        serverSock = null;
      }

      //  Destroy this server socket and create another

      destroyServerSocket (this);
      createServerSocket (0);  // new server socket on new port
    }
  }

  private class MessageInListener implements Runnable
  { 
    private Socket socket;
    private BufferedInputStream socketIn;
    private BufferedOutputStream socketOut;
    private boolean quitNow;

    public MessageInListener (Socket s) throws IOException
    {
      if (log.isDebugEnabled()) log.debug ("Creating new msg in listener for " +s);

      socket = s;
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

          //  Read a message

          scheduleSocketClose (socket, socketTimeout);
          if (log.isDebugEnabled()) log.debug ("Waiting for msg over " +socket);
          msgBytes = MessageSerializationUtils.readByteArray (socketIn);
          if (log.isDebugEnabled()) log.debug ("Waiting for msg done " +socket);
          unscheduleSocketClose (socket);

          if (showTraffic) System.err.print ("<S");
        }
        catch (InterruptedIOException e)
        {
          //  Socket SO timeout (set above).  Socket is still good, but we will close
          //  it as a timeout has been reached.

          if (log.isDebugEnabled()) log.debug ("Closing socket due to SO timeout: " +e);
          quitNow = true;
          break;
        }
        catch (EOFException e)
        { 
          //  Typically raised when the party at the  other end closes their connection.  
          //  If the OutgoingSocketLinkProtocol has oneSendPerConnection set to true, it will
          //  close its connection after each message send and this will happen all the time.

          if (log.isDebugEnabled()) log.debug ("Remote socket closed: " +e);
          quitNow = true;
          break;
        }
        catch (DataIntegrityException e)
        {
          if (log.isWarnEnabled()) log.warn ("Non-Cougaar message received (msg ignored)");
          quitNow = true;
          break;
        }
        catch (Exception e)
        { 
          //  Some other exception.  Prints the stack trace for more detail.

          if (log.isDebugEnabled()) log.debug ("Terminating socket exception: " +stackTraceToString(e));
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
        catch (MessageIntegrityException e)
        {
          if (log.isWarnEnabled()) log.warn ("Message integrity exception deserializing msg (msg ignored): " +e);
          exception = e;
        }
        catch (ClassCastException e)
        {
          if (log.isWarnEnabled()) log.warn ("Got non-AttributedMessage msg (msg ignored): " +e);
          exception = e;
        }
        catch (Exception e)
        {
          if (log.isWarnEnabled()) log.warn ("Deserialization exception (msg ignored): " +e);
          exception = e;
        }

        //  Deliver the message if we have one

        String msgString = null;

        if (msg != null)
        {
          if (log.isDebugEnabled()) 
          {
            InetAddress addr = socket.getInetAddress();
            int port = socket.getPort();
            msgString = MessageUtils.toString (msg);
            log.debug ("From " +addr+ ":" +port+ " read " +msgString);
          }

          try
          {
            getDeliverer().deliverMessage (msg, msg.getTarget());
          }
          catch (MisdeliveredMessageException e)
          { 
            if (log.isDebugEnabled()) log.debug ("Delivery exception for " +msgString+ ": " +e);
            exception = e;
          }
          catch (Exception e)
          { 
            if (log.isWarnEnabled()) log.warn ("Exception delivering " +msgString+ ": " +stackTraceToString(e));
            exception = e;
          }
        }

        //  Optionally do inband acking

        if (doInbandAcking && MessageUtils.isRegularMessage (msg))
        {
          try
          {
            //  Send an ack

            byte[] ackBytes = createAck (msg, exception);
            scheduleSocketClose (socket, socketTimeout);
            if (log.isDebugEnabled()) log.debug ("Sending ack over " +socket);
            MessageSerializationUtils.writeByteArray (socketOut, ackBytes);
            if (log.isDebugEnabled()) log.debug ("Sending ack done " +socket);
            unscheduleSocketClose (socket);

            //  See if we get an ack-ack back

            scheduleSocketClose (socket, socketTimeout);
            if (log.isDebugEnabled()) log.debug ("Waiting for ack-ack over " +socket);
            byte[] ackAckBytes = MessageSerializationUtils.readByteArray (socketIn);
            if (log.isDebugEnabled()) log.debug ("Waiting for ack-ack done " +socket);
            unscheduleSocketClose (socket);
            processAckAck (ackAckBytes);
          }
          catch (Exception e)
          {
            //  Any acking that did not complete will be taken care of in regular acking

            if (log.isDebugEnabled()) log.debug ("Inband acking stopped: " +e);
            quitNow = true;
            break;
          }
        }
      }

      //  Cleanup

      if (log.isDebugEnabled()) log.debug ("Removing msg in listener for " +socket);
      closeSocket();
      messageInListeners.remove (this);
    }

    private byte[] createAck (AttributedMessage msg, Exception exception) throws Exception
    {
      PureAckMessage pam = PureAckMessage.createInbandPureAckMessage (msg);
      if (exception != null) pam.setReceptionException (exception);
      byte ackBytes[] = MessageSerializationUtils.writeMessageToByteArray (pam, getDigest());
      return ackBytes;
    }

    private void processAckAck (byte[] ackAckBytes) throws Exception
    {
      AttributedMessage msg = MessageSerializationUtils.readMessageFromByteArray (ackAckBytes);
      PureAckAckMessage paam = (PureAckAckMessage) msg;
      PureAckAck pureAckAck = (PureAckAck) MessageUtils.getAck (paam);
      Vector specificAcks = pureAckAck.getSpecificAcks();
      Vector latestAcks = pureAckAck.getLatestAcks();
      String fromNode = MessageUtils.getFromAgentNode (paam);

      if (log.isDebugEnabled())
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
          log.debug (buf.toString());
        }
        else log.debug ("Got an empty inband ack-ack!");
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
    }

    private void closeSocket ()
    {
      if (socket != null)
      {
        try { socket.close(); } catch (Exception ee) {}

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

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
