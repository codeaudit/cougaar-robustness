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

import org.cougaar.core.mts.*;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
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

  private static LoggingService log;
  private static boolean showTraffic;

  private static final String localhost;
  private static final int firstMsgSoTimeout;
  private static final int subsequentMsgsSoTimeout;

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

    s = "org.cougaar.message.protocol.socket.incoming.firstMsgSoTimeout";
    firstMsgSoTimeout = Integer.valueOf(System.getProperty(s,"1000")).intValue();

    s = "org.cougaar.message.protocol.socket.incoming.subsequentMsgsSoTimeout";
    subsequentMsgsSoTimeout = Integer.valueOf(System.getProperty(s,"100")).intValue();
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
    if (log.isInfoEnabled()) log.info ("Using " +localhost+ " as name of local host");

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
      Schedulable thread = threadService().getThread (this, listener, "ServerSock_"+port);
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
      serverSock = new ServerSocket (port);
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
          s.shutdownOutput();  // we only read

          //  Create a message listener and get a thread 
          //  to run it in and kick it off.

          MessageInListener listener = new MessageInListener (s);
          Schedulable thread = threadService().getThread (this, listener, "MessageIn_"+s.getPort());
          thread.start();
          messageInListeners.add (listener);
        }
        catch (Exception e) 
        {
          if (log.isWarnEnabled()) log.warn ("Server socket exception: " +e);
          quitNow = true;
          break;
        }
      }

      if (serverSock != null)
      {
        try { serverSock.close(); } catch (Exception e) {}
        serverSock = null;
      }

      //  HACK!  For now, destroy this server socket and create another.
      //  Will work this out better later.

      destroyServerSocket (this);
      createServerSocket (0);  //  HACK!!! port num should come from orig
    }
  }

  private class MessageInListener implements Runnable
  { 
    private Socket socket;
    private NoHeaderInputStream socketIn;
    private boolean quitNow;

    public MessageInListener (Socket s)
    {
      if (log.isDebugEnabled()) log.debug ("New in socket created: " +s);
      socket = s;
    }

    public void quit ()
    {
      quitNow = true;

      if (socketIn != null)
      {
        try { socketIn.close(); } catch (Exception e) {}
      }
    }

    public void run() 
    {
      try
      {
        socketIn = new NoHeaderInputStream (socket.getInputStream());
      }
      catch (Exception e)
      { 
        log.error ("Error creating new socketIn: " +e);
        messageInListeners.remove (this);
        return;
      }

      boolean firstMsg = true;
 
      while (!quitNow)
      {
        AttributedMessage msg = null;

        try 
        {
          //  Sit and wait for a message (till socket timeout).  We distinguish between
          //  first and subsequent message timeouts because the first message will incur
          //  the socket connection overhead while the others won't, and we are in a race
          //  condition with the message sender - he has to send the message fast enough
          //  after establishing the connection to catch this side of the connection 
          //  still alive.

          socket.setSoTimeout (firstMsg? firstMsgSoTimeout : subsequentMsgsSoTimeout);
          firstMsg = false;

          ByteArrayObject msgObject = (ByteArrayObject) socketIn.readObject();
          if (showTraffic) System.err.print ("<S");

          //  Deserialize bytes into a Cougaar message

          Object obj = getObjectFromBytes (msgObject.getBytes());

          try
          {
            msg = (AttributedMessage) obj;  // possible cast exception
          }
          catch (Exception e)
          {
            if (log.isWarnEnabled()) log.warn ("Got non-AttributedMessage msg! (msg ignored): " +e);
            continue;
          }

          if (log.isDebugEnabled()) 
          {
            InetAddress addr = socket.getInetAddress();
            int port = socket.getPort();
            log.debug ("From " +addr+ ":" +port+ " read " +MessageUtils.toString(msg));
          }
        }
        catch (InterruptedIOException e)
        {
          //  Socket SO timeout (set above).  Socket still good, but we will close
          //  it as it has not been used in a while.

          if (log.isDebugEnabled()) log.debug ("Closing socket due to SO timeout: " +e);
          quitNow = true;
          break;
        }
        catch (EOFException e)
        { 
          //  Typically a socket exception raised when the party at the  other end 
          //  closes their socket connection.  If the OutgoingSocketLinkProtocol has
          //  oneSendPerConnection set to true, you will get these all the time.

          if (log.isDebugEnabled()) log.debug ("Remote socket closed: " +e);
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

        //  Deliver the message.  Nobody to send exceptions to, so we just log them.

        try
        {
          if (msg != null) getDeliverer().deliverMessage (msg, msg.getTarget());
        }
        catch (MisdeliveredMessageException e)
        { 
          log.error ("Got MisdeliveredMessageException for " +MessageUtils.toString(msg)+ ": " +e);
        }
        catch (Exception e)
        { 
          log.error ("Exception delivering " +MessageUtils.toString(msg)+ ": " +stackTraceToString(e));
        }
      }

      //  Cleanup

      if (socketIn != null)
      {
        try { socketIn.close(); } catch (Exception e) {}
        socketIn = null;
      }

      messageInListeners.remove (this);
    }
  }

  private static Object getObjectFromBytes (byte[] data) 
  {
	ObjectInputStream ois = null;
	Object obj = null;

	try 
    {
      ByteArrayInputStream bais = new ByteArrayInputStream (data);
	  ois = new ObjectInputStream (bais);
	  obj = ois.readObject();
	} 
    catch (Exception e) 
    {
      if (log.isWarnEnabled()) log.warn ("Deserialization exception: " +stackTraceToString(e));
      return null;
	}
	
	try { ois.close(); } catch (IOException e) {}

	return obj;
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

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
