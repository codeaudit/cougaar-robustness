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
 * 22 Sep 2002: Revamp for new serialization & socket closer. (OBJS)
 * 18 Aug 2002: Various enhancements for Cougaar 9.4.1 release. (OBJS)
 * 26 Apr 2002: Created from socket link protocol. (OBJS)
 */

package org.cougaar.core.mts.udp;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.acking.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.component.ServiceBroker;

/**
 *  Outgoing UDP Link Protocol - send Cougaar messages via UDP
 */

public class OutgoingUDPLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";
  public static final int    MAX_UDP_MSG_SIZE = (64*1024)-128;
  public static final int    MAX_ACK_MSG_SIZE = 4*1024;  // for inband acks

  private static final String localhost;
  private static final int linkCost;
  private static final boolean doInbandAcking;
  private static final boolean doInbandRTTUpdates;
  private static final boolean useMessageDigest;
  private static final String messageDigestType;
  private static final int socketTimeout;
  private static final int inbandAckSoTimeout;

  private LoggingService log;
  private SocketClosingService socketCloser;
  private RTTService rttService;
  private Hashtable specCache, addressCache;
  private HashMap links;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.localhost";
    localhost = System.getProperty (s, getLocalHost());

    s = "org.cougaar.message.protocol.udp.cost";  // one way
    linkCost = Integer.valueOf(System.getProperty(s,"4000")).intValue();  // was 750

    s = "org.cougaar.message.protocol.udp.doInbandAcking";
    doInbandAcking = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.udp.doInbandRTTUpdates";
    doInbandRTTUpdates = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.useMessageDigest";
    useMessageDigest = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.messageDigestType";
    messageDigestType = System.getProperty (s, "MD5");

    s = "org.cougaar.message.protocol.udp.outgoing.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.protocol.udp.outgoing.inbandAckSoTimeout";
    inbandAckSoTimeout = Integer.valueOf(System.getProperty(s,"2000")).intValue();
  }
 
  public OutgoingUDPLinkProtocol ()
  {
    specCache = new Hashtable();
    addressCache = new Hashtable();
    links = new HashMap();
  }

  public void load ()
  {
    super_load();

    log = loggingService;
    if (doInfo()) log.info ("Creating " + this);

    if (socketTimeout > 0)
    {
      ServiceBroker sb = getServiceBroker();
      socketCloser = (SocketClosingService) sb.getService (this, SocketClosingService.class, null);
      if (socketCloser == null) log.error ("Cannot do socket timeouts - SocketClosingService not available!");
    }

    if (doInbandRTTUpdates)
    {
      rttService = (RTTService) getServiceBroker().getService (this, RTTService.class, null);
    }

    if (startup() == false)
    {
      String str = "Failure starting up " + this;
      log.error (str);
      throw new RuntimeException (str);
    }
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  public synchronized boolean startup ()
  {
    shutdown();
    return true;
  }

  public synchronized void shutdown ()
  {}

  private DatagramSocketSpec lookupDatagramSocketSpec (MessageAddress address) throws NameLookupException
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof DatagramSocketSpec)
      {
        DatagramSocketSpec spec = (DatagramSocketSpec) obj;

        try
        {
          spec.setInetAddress (getInetAddress (spec.getHost()));
        }
        catch (Exception e)
        {
          throw new NameLookupException (e);
        }

        return spec;
      }
      else
      {
        log.error ("Invalid DatagramSocketSpec in nameserver lookup!");
      }
    }

    return null;
  }

  private DatagramSocketSpec getDatagramSocketSpecByNode (String node) throws NameLookupException
  {
    return getDatagramSocketSpec (new MessageAddress (node+ "(MTS" +PROTOCOL_TYPE+ ")"));
  }

  private DatagramSocketSpec getDatagramSocketSpec (MessageAddress address) throws NameLookupException
  {
    synchronized (specCache)
    {
      DatagramSocketSpec spec = (DatagramSocketSpec) specCache.get (address);
      if (spec != null) return spec;
      spec = lookupDatagramSocketSpec (address);
      if (spec != null) specCache.put (address, spec);
      return spec;
    }
  }

  private InetAddress getInetAddress (String host) throws UnknownHostException
  {
    synchronized (addressCache)
    {
      InetAddress addr = (InetAddress) addressCache.get (host);
      if (addr != null) return addr;
      addr = InetAddress.getByName (host);
      if (addr != null) addressCache.put (host, addr);
      return addr;
    }
  }

  private synchronized void clearCaches ()
  {
    specCache.clear();
    addressCache.clear();    
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (getDatagramSocketSpec (address) != null);
    } 
    catch (Exception e) 
    {
      return false;
    }
  }

  public DestinationLink getDestinationLink (MessageAddress address) 
  {
    DestinationLink link = (DestinationLink) links.get (address);

    if (link == null) 
    {
      link = new UDPOutLink (address);
      link = (DestinationLink) attachAspects (link, DestinationLink.class);
      links.put (address, link);
    }

    return link;
  }

  public static long getMaxMessageSizeInBytes ()
  {
    return MAX_UDP_MSG_SIZE;
  }

  class UDPOutLink implements DestinationLink 
  {
    private MessageAddress destination;
    private DatagramSocket dsocket;
    private String dsockString = null;

    public UDPOutLink (MessageAddress dest) 
    {
      destination = dest;
    }

    public MessageAddress getDestination() 
    {
      return destination;
    }

    public String toString ()
    {
      return OutgoingUDPLinkProtocol.this + "-dest:" + destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingUDPLinkProtocol.class;
    }
   
    public int cost (AttributedMessage msg) 
    {
      if (msg == null) return linkCost;  // forced HACK
      return (addressKnown(destination) ? linkCost : Integer.MAX_VALUE);
    }

    public Object getRemoteReference ()
    {
      return null;
    }

    public void addMessageAttributes (MessageAttributes attrs) 
    {}

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return true;
    }

    private synchronized void dumpCachedData ()
    {
      clearCaches();
      dsocket = null;
    }

    public synchronized MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
      //  Dump our cached data on message send retries

      if (MessageUtils.getSendTry (msg) > 1) dumpCachedData();

      //  Get datagram socket spec for destination node.  Note we use node instead
      //  of agent as the receiver can move his socket to another port at will, making
      //  any and all client (ie. agent) socket spec registrations out of date.

      String toNode = MessageUtils.getToAgentNode (msg);
      DatagramSocketSpec destSpec = getDatagramSocketSpecByNode (toNode);

      if (destSpec == null)
      {
        String s = "No DatagramSocketSpec in nameserver for node " +toNode;
        if (log.isWarnEnabled()) log.warn (s);
        throw new NameLookupException (new Exception (s));
      }

      //  Send message via udp

      boolean success = false;
      Exception ex = null;

      try 
      {
        success = sendMessage (msg, destSpec);
      } 
      catch (Exception e) 
      {
        if (doDebug()) log.debug ("sendMessage exception: " +stackTraceToString(e));
        ex = e;
      }

      //  Dump our cached data on failed sends and throw an exception

      if (success == false)
      {
        dumpCachedData();
        Exception e = (ex != null ? ex : new Exception ("UDP sendMessage unsuccessful"));
        throw new CommFailureException (e);
      }

      //  Successful send

	  MessageAttributes successfulSend = new SimpleMessageAttributes();
      String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
	  successfulSend.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return successfulSend;
    }
   
    private synchronized boolean sendMessage (AttributedMessage msg, DatagramSocketSpec spec) throws Exception
    {
      if (doDebug()) log.debug ("Sending " +MessageUtils.toString(msg));

      //  NOTE:  UDP is an unreliable communications protocol.  We just send the message
      //  on its way and maybe it gets there.  Acking will tell us if it does or not.

      //  Serialize the message into a byte array.  Depending on properties set, we possibly help
      //  insure message integrity via a message digest (eg an embedded MD5 hash of the message).

      byte msgBytes[] = MessageSerializationUtils.writeMessageToByteArray (msg, getDigest());
      if (msgBytes == null) return false;

      //  Make sure the message will fit into the 64KB datagram packet size limitation
      //  NOTE:  While 64KB is the theoretical maximum, the practical maximum may be 
      //  much smaller, such as perhaps 8KB.  Will need to add code to monitor & test
      //  the actual largest msg that can be successfully sent to a particular destination.

      if (msgBytes.length >= getMaxMessageSizeInBytes())
      {
        int msgSizeKB = msgBytes.length/1024;

        if (log.isWarnEnabled())
        {
          log.warn ("Msg exceeds " +(getMaxMessageSizeInBytes()/1024)+ "KB max UDP " +
            "message size! (" +msgSizeKB+ "KB): " +MessageUtils.toString(msg));
        }

        MessageUtils.setMessageSize (msg, msgBytes.length);  // prevent this link selection
        throw new MessageSizeException ("Message too large for UDP (" +msgSizeKB+ "KB)");
      }

      //  Build the datagram for the message

      InetAddress addr = spec.getInetAddress();
      int port = spec.getPortAsInt();
      DatagramPacket packet = new DatagramPacket (msgBytes, msgBytes.length, addr, port); 

      //  Make and connect a new datagram socket as needed
      //  About connecting datagram sockets from the JavaDoc:

      /*  Connects the socket to a remote address for this socket. When a socket is connected 
          to a remote address, packets may only be sent to or received from that address. 
          By default a datagram socket is not connected.

          If the remote destination to which the socket is connected does not exist, or is 
          otherwise unreachable, and if an ICMP destination unreachable packet has been 
          received for that address, then a subsequent call to send or receive may throw a 
          PortUnreachableException. Note, there is no guarantee that the exception will be 
          thrown.

          A caller's permission to send and receive datagrams to a given host and port are 
          checked at connect time. When a socket is connected, receive and send will not 
          perform any security checks on incoming and outgoing packets, other than matching 
          the packet's and the socket's address and port. On a send operation, if the 
          packet's address is set and the packet's address and the socket's address do not 
          match, an IllegalArgumentException will be thrown. 
      */

      if (dsocket == null || dsocket.isClosed()) 
      {
        if (doDebug()) log.debug ("Creating datagram socket to " +destination+ " with " +spec);

        dsocket = new DatagramSocket();
        scheduleSocketClose (dsocket, socketTimeout);
        dsocket.connect (addr, port);  // new in Java 1.4

        if (doDebug()) 
        {
          dsockString = datagramSocketToString (dsocket);
          log.debug ("Created datagram socket " +dsockString);
        }
      }
      else scheduleSocketClose (dsocket, socketTimeout);

      //  Send the message.  If the target agent has moved to another node,
      //  we may get an IllegalArgumentException because the socket is connected
      //  to an out of date addr & port in relation to the datagram.  If we
      //  detect this situation we re-connect the socket and try the send again.

      PureAckMessage pam = null;
      long sendTime = 0, receiveTime = 0;
  
      for (int tryN=1; tryN<=2; tryN++) // try at most twice
      {
        try
        {
          if (doDebug()) log.debug ("Sending " +msgBytes.length+ " byte msg thru " +dsockString);
          sendTime = now();
          dsocket.send (packet);
          if (doDebug()) log.debug ("Sending " +msgBytes.length+ " byte msg done " +dsockString);
          break; // success
        }
        catch (Exception e)
        {
          if (tryN == 1 && e instanceof IllegalArgumentException)
          {
            if (doDebug()) log.debug ("Reconnecting " +dsockString+ " to " +spec+ " for " +destination);
            dsocket.connect (addr, port);
          }
          else 
          {
            dsocket.close();
            dsocket = null;
            throw (e);
          }
        }
      }

      //  Optionally do inband acking

      if (doInbandAcking && MessageUtils.isAckableMessage (msg))
      {
        try
        {
          //  See if we get an ack

          if (packet.getData().length < MAX_ACK_MSG_SIZE) packet.setData (new byte[MAX_ACK_MSG_SIZE]);
          if (doDebug()) log.debug ("Waiting for ack from " +dsockString);
          dsocket.setSoTimeout (inbandAckSoTimeout);
          dsocket.receive (packet);
          receiveTime = now();
          if (doDebug()) log.debug ("Waiting for ack done " +dsockString);
          pam = processAck (packet.getData());

          //  Send an ack-ack
          //
          //  The inband acking protocol is that if the ack back reports a message
          //  reception exception then no ack-ack is sent for the ack.

          if (!pam.hasReceptionException())
          {
            packet.setData (createAckAck (pam, receiveTime));
            if (doDebug()) log.debug ("Sending ack-ack thru " +dsockString);
            dsocket.send (packet);
            if (doDebug()) log.debug ("Sending ack-ack done " +dsockString);
          }
          else
          {
            Exception e = pam.getReceptionException();

            if (doWarn()) 
            {
              String node = pam.getReceptionNode();
              log.warn ("Got reception exception from node " +node+ " " +dsockString+ ": " +e);
            }

            throw e;
          }
        }
        catch (Exception e)
        {
          //  Any acking that did not complete will be taken care of in later acking

          if (doDebug()) log.debug ("Inband acking stopped for " +dsockString+ ": " +e);

          //  Selectively close the socket 

          if (pam == null || !pam.hasReceptionException()) closeSocket();
          else unscheduleSocketClose (dsocket); 

          //  Selectively throw exceptions back up

          if (e instanceof MessageDeserializationException) throw e;
        } 
      }

      //  Allow socket reuse

      if (dsocket != null && !dsocket.isClosed()) unscheduleSocketClose (dsocket); 

      //  Update the inband RTT if possible

      if (rttService != null && pam != null)
      {
        DestinationLink sendLink = this;
        String node = MessageUtils.getToAgentNode (msg);
        int rtt = ((int)(receiveTime - sendTime)) - pam.getInbandNodeTime();
        rttService.updateInbandRTT (sendLink, node, rtt);
      }

      return true;  // msg send successful
    }

    private PureAckMessage processAck (byte[] ackBytes) throws Exception
    {
      AttributedMessage msg = MessageSerializationUtils.readMessageFromByteArray (ackBytes);
      PureAckMessage pam = (PureAckMessage) msg;

      if (pam.hasReceptionException()) return pam;  // no more ack to process

      PureAck pureAck = (PureAck) MessageUtils.getAck (pam);
      Vector latestAcks = pureAck.getLatestAcks();

      if (latestAcks != null)
      {
        if (doDebug())
        {
          StringBuffer buf = new StringBuffer();
          AckList.printAcks (buf, "  latest", latestAcks);
          log.debug ("Got inband ack:\n" +buf);
        }

        for (Enumeration a=latestAcks.elements(); a.hasMoreElements(); )
          NumberList.checkListValidity ((AckList) a.nextElement());

        String fromNode = MessageUtils.getFromAgentNode (pam);
        MessageAckingAspect.addReceivedAcks (fromNode, latestAcks);
      }
      else if (doDebug()) log.debug ("Got empty inband ack!");

      return pam;
    }

    private byte[] createAckAck (PureAckMessage pam, long receiveTime) throws Exception
    {
      PureAckAckMessage paam = PureAckAckMessage.createInbandPureAckAckMessage (pam);
      paam.setInbandNodeTime ((int)(now()-receiveTime));
      byte ackAckBytes[] = MessageSerializationUtils.writeMessageToByteArray (paam, getDigest());
      return ackAckBytes;
    }

    private void scheduleSocketClose (DatagramSocket socket, int timeout)
    {
      if (socketCloser != null) socketCloser.scheduleClose (socket, timeout);
    }

    private void unscheduleSocketClose (DatagramSocket socket)
    {
      if (socketCloser != null) socketCloser.unscheduleClose (socket);
    }

    private void closeSocket ()
    {
      if (dsocket != null)
      {
        try { dsocket.close(); } catch (Exception e) {}
        dsocket = null;
      } 
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
