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
 * 26 Apr 2002: Created from socket link protocol. (OBJS)
 */

package org.cougaar.core.mts.udp;

import java.io.*;
import java.util.*;
import java.net.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;

/**
 *  Outgoing UDP Link Protocol - send messages via UDP
 */

public class OutgoingUDPLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";
  public static final int    MAX_UDP_MSG_SIZE = 64*1024;

  private static final int protocolCost;

  private LoggingService log;
  private Hashtable specCache, addressCache;
  private HashMap links;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.cost";  // one way
    protocolCost = Integer.valueOf(System.getProperty(s,"4000")).intValue();  // was 750
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
    if (log.isInfoEnabled()) log.info ("Creating " + this);

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
    private DatagramSocket datagramSocket;

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
      if (msg == null) return protocolCost;  // forced HACK
      return (addressKnown(destination) ? protocolCost : Integer.MAX_VALUE);
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
      datagramSocket = null;
    }

    public synchronized MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
      //  Dump our cached data on message send retries

      if (MessageUtils.getSendTry (msg) > 1) dumpCachedData();

      //  Get datagram socket spec for destination

      DatagramSocketSpec destSpec = getDatagramSocketSpec (destination);

      if (destSpec == null)
      {
        String s = "No nameserver info for " +destination;
        if (log.isWarnEnabled()) log.warn (s);
        throw new NameLookupException (new Exception (s));
      }

      //  Send message via udp

      boolean success = false;
      Exception save = null;

      try 
      {
        success = sendMessage (msg, destSpec);
      } 
      catch (Exception e) 
      {
        if (log.isDebugEnabled()) log.debug ("sendMessage: " +stackTraceToString(e));
        save = e;
      }

      //  Dump our cached data on failed sends and throw an exception

      if (success == false)
      {
        dumpCachedData();
        Exception e = (save==null ? new Exception ("UDP sendMessage unsuccessful") : save);
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
      if (log.isDebugEnabled()) log.debug ("Sending " +MessageUtils.toString(msg));

      //  NOTE:  UDP is an unreliable communications protocol.  We just send the message
      //  on its way and maybe it gets there.  Acking will tell us if it does or not.

      //  Serialize the message into a byte array

      byte msgBytes[] = toBytes (msg);
      if (msgBytes == null) return false;

      //  Make sure the message will fit into the 64 KB datagram packet size limitation

      if (msgBytes.length >= getMaxMessageSizeInBytes())
      {
        if (log.isWarnEnabled())
        {
          log.warn ("Msg exceeds " +(getMaxMessageSizeInBytes()/1024)+ " KB max UDP " +
            "message size! (" +(msgBytes.length/1024)+ " KB): " +MessageUtils.toString(msg));
        }

        MessageUtils.setMessageSize (msg, msgBytes.length);  // size stops this link selection
        return false;
      }

      //  Build the datagram for the message

      InetAddress addr = spec.getInetAddress();
      int port = spec.getPortAsInt();
      DatagramPacket dp = new DatagramPacket (msgBytes, msgBytes.length, addr, port); 

      //  Make and connect new socket as needed

      if (datagramSocket == null) 
      {
        if (log.isDebugEnabled()) 
          log.debug ("New datagram socket to " +addr+ ":" +port+ " for " + this);

        datagramSocket = new DatagramSocket();
        datagramSocket.connect (addr, port);  // new in Java 1.4
      }

      //  Send the message.  If the target agent has moved to another node,
      //  we may get an IllegalArgumentException because the socket is connected
      //  to an out of date addr & port in relation to the datagram.  If we
      //  detect this situation we re-connect the socket and try the send again.

      for (int tryN=1; tryN<=2; tryN++) // try at most twice
      {
        try
        {
          datagramSocket.send (dp);
          break; // success
        }
        catch (Exception e)
        {
          if (tryN == 1 && e instanceof IllegalArgumentException)
          {
            if (log.isDebugEnabled()) 
              log.debug ("Reconnecting datagram socket to " +addr+ ":" +port+ " for " + this);

            datagramSocket.connect (addr, port);
          }
          else 
          {
            datagramSocket.close();
            datagramSocket = null;
            throw (e);
          }
        }
      }
 
      return true;  // send successful
    }

    private synchronized byte[] toBytes (AttributedMessage msg)  // serialization has needed sync before
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = null;

      try 
      {
        oos = new ObjectOutputStream (baos);
        oos.writeObject (msg);
        oos.flush();
      } 
      catch (Exception e) 
      {
        if (log.isWarnEnabled()) log.warn ("Serialization exception for " +MessageUtils.toString(msg)+
          ": " +stackTraceToString(e));
        return null;
      }

      try 
      {
        oos.close();
      } 
      catch (Exception e) {}

      return baos.toByteArray();
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
