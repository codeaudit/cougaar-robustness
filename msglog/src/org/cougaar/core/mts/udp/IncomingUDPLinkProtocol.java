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
 * 30 Sep 2002: Add inband acking. (OBJS)
 * 22 Sep 2002: Revamp for new serialization. (OBJS)
 * 26 Apr 2002: Created from socket link protocol. (OBJS)
 */

package org.cougaar.core.mts.udp;

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
 * IncomingUDPLinkProtocol is an IncomingLinkProtocol which receives
 * Cougaar messages from other nodes in the society via raw datagram (UDP) sockets.
 * */

public class IncomingUDPLinkProtocol extends IncomingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";
  public static final int    MAX_DATAGRAM_PACKET_SIZE = 64*1024;

  private static final DatagramSocketSpec emptySpec = new DatagramSocketSpec ("","");

  private static final String localhost;
  private static final boolean doInbandAcking;
  private static final boolean doInbandRTTUpdates;
  private static final boolean useMessageDigest;
  private static final String messageDigestType;
  private static final int numInvalidMsgs;
  private static final int timeWindowSecs;
  private static final int minPortNumber;
  private static final int maxPortNumber;

  private static LoggingService log;
  private SocketClosingService socketCloser;
  private RTTService rttService;
  private String nodeID;
  private static boolean showTraffic;
  private DatagramSocketSpec datagramSocketSpecs[];
  private DatagramSocketSpec myDatagramSocketSpec;
  private Vector datagramSocketListeners;
  private ThreadService threadService;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.localhost";
    localhost = System.getProperty (s, getLocalHost());

    s = "org.cougaar.message.protocol.udp.doInbandAcking";
    doInbandAcking = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.udp.doInbandRTTUpdates";
    doInbandRTTUpdates = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.useMessageDigest";
    useMessageDigest = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.messageDigestType";
    messageDigestType = System.getProperty (s, "MD5");

    s = "org.cougaar.message.protocol.udp.incoming.socketMoveTriggerNumInvalidMsgs";
    numInvalidMsgs = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.protocol.udp.incoming.socketMoveTriggerTimeWindowSecs";
    timeWindowSecs = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.protocol.udp.incoming.minPortNumber";
    minPortNumber = Integer.valueOf(System.getProperty(s,"1024")).intValue();

    s = "org.cougaar.message.protocol.udp.incoming.maxPortNumber";
    maxPortNumber = Integer.valueOf(System.getProperty(s,"65536")).intValue();
  }
 
  public IncomingUDPLinkProtocol ()
  {
    datagramSocketListeners = new Vector();

    //  Get incoming socket portnumber(s) from a property?

    String port = "0";   // port of 0 means auto-choose port number

    //  Create a single socket spec for now

    datagramSocketSpecs = new DatagramSocketSpec[1];
    datagramSocketSpecs[0] = new DatagramSocketSpec (localhost, port);
  }

  public void load () 
  {
    super_load();
    log = loggingService;

    if (log.isInfoEnabled()) log.info ("Creating " + this);
    if (log.isInfoEnabled()) log.info ("Using " +localhost+ " as the name of the local host");

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

    //  Create any sockets in socketSpecs

    int count = 0;

    for (int i=0; i<datagramSocketSpecs.length; i++) 
    {
      int port = createDatagramSocketListener (datagramSocketSpecs[i].getPortAsInt());
      datagramSocketSpecs[i].setPort (port);  // potentially new port number
      if (port > 0) count++;                  // if port < 1 is bad port
    }

    //  If any sockets were specified, make sure at least one is working

    return (datagramSocketSpecs.length > 0) ? (count > 0) : true;
  }

  public synchronized void shutdown ()
  {
    unregisterDatagramSocketSpec();

    //  Remove and quit all of the socket listeners

    for (Enumeration e = datagramSocketListeners.elements(); e.hasMoreElements(); ) 
    {
       DatagramSocketListener listener = (DatagramSocketListener) e.nextElement();
       datagramSocketListeners.remove (listener);
       listener.quit();
    }
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  private void registerDatagramSocketSpec (DatagramSocketSpec spec)
  {
    //  Update the name server.  Note: Allow any exceptions to be thrown.
  
    myDatagramSocketSpec = spec;
    MessageAddress address = new MessageAddress (nodeID+ "(MTS" +PROTOCOL_TYPE+ ")");
    getNameSupport().registerAgentInNameServer (myDatagramSocketSpec, address, PROTOCOL_TYPE);
  }

  private void unregisterDatagramSocketSpec ()
  {
    //  Update the name server

    try
    {
      if (myDatagramSocketSpec == null) return;
      MessageAddress address = new MessageAddress (nodeID+ "(MTS" +PROTOCOL_TYPE+ ")");
      getNameSupport().unregisterAgentInNameServer (myDatagramSocketSpec, address, PROTOCOL_TYPE);
    }
    catch (Exception e)
    {
      log.error ("unregisterDatagramSocketSpec: " +stackTraceToString(e));
    }
  }

  public final void registerClient (MessageTransportClient client) 
  {
    //  Useful in adaptive link selection to know if link is good for agent

    try 
    {
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().registerAgentInNameServer (emptySpec, clientAddress, PROTOCOL_TYPE);
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
      getNameSupport().unregisterAgentInNameServer (emptySpec, clientAddress, PROTOCOL_TYPE);
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
      getNameSupport().registerAgentInNameServer (emptySpec, address, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("registerMTS: " +stackTraceToString(e));
    }
  }

  private int createDatagramSocketListener (int port)
  {
    DatagramSocketListener listener = null;

    try
    {
      listener = new DatagramSocketListener (port);
      port = listener.getPort();  // port possibly updated

      // Schedulable thread = threadService().getThread (this, listener, "DatagramIncomingSock_"+port);
      Thread thread = new Thread (listener, "DatagramIncomingSock_"+port);
      thread.start();

      registerDatagramSocketSpec (new DatagramSocketSpec (localhost, port));
      datagramSocketListeners.add (listener);

      if (log.isInfoEnabled()) log.info ("Incoming datagram socket created on port " +port);
    }
    catch (Exception e)
    {
      log.error ("Error creating datagram socket on port " +port+ ": " +stackTraceToString(e));
      if (listener != null) listener.quit();
      listener = null;
      return -1;
    }

    return port;
  }

  private void destroyDatagramSocketListener (DatagramSocketListener listener)
  {
    if (listener == null) return;

    int port = listener.getPort();

    try
    {
      listener.quit();
//    unregisterSocketSpec();   // synchronization problem?
      datagramSocketListeners.remove (listener);
      if (doDebug()) log.debug ("Datagram socket destroyed on port " +port);
    }
    catch (Exception e)
    {
      if (doWarn()) log.warn ("Error destroying datagram socket on port " +listener.getPort()+ ": " +e);
    }
  }

  private class DatagramSocketListener implements Runnable
  { 
    private DatagramSocket dsocket;
    private DatagramPacket packet;
    private EventWindow moveTrigger;
    private boolean quitNow;
    private String dsockString;
    
    public DatagramSocketListener (int port) throws IOException
    {
      //  Create a datagram socket at the specified port or at a random port

      int tryN = 0;
      int origPort = port;
      dsocket = null;

      while (dsocket == null)
      {
        tryN++;

        if (origPort == 0) port = getRandomNumber (minPortNumber, maxPortNumber);
        else if (tryN > 5) port = 0;  // let system decide

        try
        {
          dsocket = new DatagramSocket (port);
        }
        catch (SocketException e)
        {
          // port most likely in use
        }
      }

      if (doDebug() || doInfo()) dsockString = datagramSocketToString (dsocket);

      //  We set our datagram packet buffer to the maximum possible size becase if a
      //  datagram is larger than the packet buffer it is truncated to fit the buffer and
      //  the rest of the data is thrown away (not recoverable by a subsequent read).

      byte[] buf = new byte[MAX_DATAGRAM_PACKET_SIZE];
      packet = new DatagramPacket (buf, buf.length);

      moveTrigger = new EventWindow (numInvalidMsgs, timeWindowSecs*1000);
    }

    private int getRandomNumber (int min, int max)
    {
      return min + (int) Math.rint (Math.random() * (max-min));
    }

    public int getPort ()
    {
      return (dsocket != null) ? dsocket.getLocalPort() : -1;
    }

    public synchronized void quit ()
    {
      quitNow = true;

      if (dsocket != null)
      {
        try { dsocket.close(); } catch (Exception e) {}
        dsocket = null;
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
          try { e.printStackTrace(); } catch (Exception ex) { /* !! */ }
        }
      }
    }

    public void doRun() 
    {
      long receiveTime = 0, sendTime = 0;

      while (!quitNow)
      {
        try 
        {
          //  If we are getting too many invalid messages, its time to move to another port

          if (moveTrigger.hasTriggered())
          {
            if (doWarn()) log.warn ("Too many invalid messages, moving incoming datagram socket");
            quitNow = true;
            break;
          }

          //  Sit and wait for an incoming UDP packet

          if (doDebug()) log.debug ("Waiting for msg from " +dsockString);
          dsocket.receive (packet);
          receiveTime = now();
          if (doDebug()) log.debug ("Waiting for msg done " +dsockString);
          if (showTraffic) System.err.print ("<U");
        }
        catch (Exception e)
        { 
          //  Typically an EOF exception is raised when the party at the
          //  other end closes their socket connection, but this is not
          //  the case here with datagram sockets.

          if (doDebug()) log.debug ("Terminating socket exception:\n" +stackTraceToString(e)); 
          quitNow = true;
          break;
        }

        //  Deserialize the read bytes into a Cougaar message.  We deserialize here 
        //  instead of in the MessageInProcessor so we don't have to copy or realloc
        //  the packet buffer.  Since UDP messages are limited in size (64KB max),
        //  this should not be too much of a delay to getting back to reading msgs.

        AttributedMessage msg = null;
        Exception exception = null;

        try
        {
          msg = MessageSerializationUtils.readMessageFromByteArray (packet.getData());
        }
        catch (MessageDeserializationException e)
        {
          if (doWarn()) log.warn ("Deserialization exception (msg ignored): " +e);
          exception = e;

          //  Certain kinds of deserialization errors occur to invalid messages being
          //  received, messages that should not be being sent to us, and so they count
          //  towards the socket move trigger.

          Throwable cause = exception.getCause();

          if (cause instanceof DataValidityException)
          {
            moveTrigger.addEvent();
            continue;  // no inband acks for these
          }
        }

        //  We handle inband ack-ack messages separately

        if (isInbandAckAck (msg))
        {
          //  Note:  If there was an reception exception with the original message there
          //  will not be any inband ack-ack coming back for the inband ack sent. 

          if (doDebug()) log.debug ("Received inband ack-ack " +dsockString);
          PureAckAckMessage paam;

          try
          {
            paam = processAckAck (msg);
          }
          catch (Exception e)
          {
            if (doDebug()) log.debug ("Error processing inband ack-ack (ack-ack ignored): " +e);
            quitNow = true;
            return;
          }

          //  Update inband RTT

          if (rttService != null)
          {
            String node = MessageUtils.getFromAgentNode (msg);
            String recvLink = IncomingUDPLinkProtocol.this.toString();
            sendTime = paam.getInbandAckSendTime();
            int rtt = ((int)(receiveTime - sendTime)) - paam.getInbandNodeTime();
            rttService.updateInbandRTT (node, recvLink, rtt);
          }
        }
        else
        {
          //  Set receive time for RTT service

          if (rttService != null) rttService.setMessageReceiveTime (msg, receiveTime);

          //  Deliver the message if it is non-null

          String msgString = null;

          if (msg != null)
          {
            if (doDebug()) 
            {
              msgString = MessageUtils.toString (msg);
              log.debug ("From " +dsockString+ " read " +msgString);
            }

            try
            {
              getDeliverer().deliverMessage (msg, msg.getTarget());
            }
            catch (MisdeliveredMessageException e)
            { 
              if (doDebug()) log.debug ("Delivery exception for " +msgString+ ": " +e);
              exception = e;
            }
            catch (Exception e)
            { 
              if (doDebug()) log.debug ("Exception delivering " +msgString+ ": " +stackTraceToString(e));
              exception = e;
            }
          }

          //  Optionally do inband acking

          if (doInbandAcking && (msg == null || MessageUtils.isAckableMessage (msg)))
          {
            try
            {
              //  Send an ack

              sendTime = now();  // about as close as we can get to the send
              byte[] ackBytes = createAck (msg, receiveTime, exception, sendTime);
              SocketAddress addr = packet.getSocketAddress();
              DatagramPacket ackPacket = new DatagramPacket (ackBytes, ackBytes.length, addr);
              if (doDebug()) log.debug ("Sending ack thru " +dsockString);
              dsocket.send (ackPacket);
              if (doDebug()) log.debug ("Sending ack done " +dsockString);
            }
            catch (Exception e)
            {
              //  Any acking that did not complete will be taken care of in later acking

              if (doDebug()) log.debug ("Inband acking stopped for " +dsockString+ ": " +e);
            }
          }
        } 
      }

      //  Cleanup

      if (dsocket != null)
      {
        try { dsocket.close(); } catch (Exception e) {}
        dsocket = null;
      }

      //  Destroy this server socket listener and create another on a new port

      destroyDatagramSocketListener (this);
      createDatagramSocketListener (0);
    }

    private boolean isInbandAckAck (AttributedMessage msg)
    {
      if (msg == null) return false;
      if (!MessageUtils.isPureAckAckMessage(msg)) return false;
      return ((PureAckAckMessage)msg).isInbandAckAck();
    }

    private byte[] createAck (AttributedMessage msg, long receiveTime, Exception ex, long sendTime) 
      throws Exception
    {
      PureAckMessage pam = PureAckMessage.createInbandPureAckMessage (msg);

      if (ex != null) 
      {
        pam.setReceptionException (ex);
        pam.setReceptionNode (nodeID);
      }

      // pam.setInbandAckSendTime (sendTime);
      pam.setInbandNodeTime ((int)(now()-receiveTime));
      byte ackBytes[] = MessageSerializationUtils.writeMessageToByteArray (pam, getDigest());
      return ackBytes;
    }

    private PureAckAckMessage processAckAck (AttributedMessage msg) throws Exception
    {
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

      return paam;
    }
  }

  private static String getLocalHost ()
  {
    try
    {
      // return InetAddress.getLocalHost().getHostAddress();    // hostname as IP address
      return InetAddress.getLocalHost().getCanonicalHostName(); // now using 1.4
    }
    catch (Exception e)
    {
      throw new RuntimeException (e.toString());
    }
  }

  private static String datagramSocketToString (DatagramSocket ds)
  {
    if (ds == null) return "null";
    String r = ds.isConnected() ? "remote:"+ds.getInetAddress()+":"+ds.getPort()+" " : "";
    String l = ds.isBound() ? "local:"+localhost+":"+ds.getLocalPort() : "";
    return "DatagramSocket[" +r+l+ "]";
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
