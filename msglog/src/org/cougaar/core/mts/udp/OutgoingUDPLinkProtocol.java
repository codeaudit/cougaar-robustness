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
 * 26 Apr 2002: Created from socket link protocol. (OBJS)
 */

package org.cougaar.core.mts.udp;

import java.io.*;
import java.util.*;
import java.net.*;

import org.cougaar.core.mts.*;

/**
 *  Outgoing UDP Link Protocol
**/

public class OutgoingUDPLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";

  private static final boolean debug;
  private static final int protocolCost;

  private static final Object sendLock = new Object();

  private DatagramSocket datagramSocket;
  private HashMap links;

private int cnt = 0;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.debug";
    debug = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.cost";
    protocolCost = Integer.valueOf(System.getProperty(s,"166")).intValue();
  }
 
  public OutgoingUDPLinkProtocol ()
  {
    System.err.println ("Creating " + this);

    links = new HashMap();

    //  Transport initialization

    if (startup() == false)
    {
      throw new RuntimeException ("Failure starting up OutgoingUDPLinkProtocol!");
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
  {
    // TBD
  }

  private DatagramSocketSpec lookupDatagramSocketSpec (MessageAddress address)
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof DatagramSocketSpec)
      {
        return (DatagramSocketSpec) obj;
      }
      else
      {
        System.err.println ("OutgoingUDPLinkProtocol: invalid obj in lookup!");
      }
    }

    return null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return lookupDatagramSocketSpec (address) != null;
    } 
    catch (Exception e) 
    {
      // System.err.println ("Failed in addressKnown: " +e);
      // e.printStackTrace();
    }

    return false;
  }

  public DestinationLink getDestinationLink (MessageAddress address) 
  {
    DestinationLink link = (DestinationLink) links.get (address);

    if (link == null) 
    {
      link = new Link (address);
      link = (DestinationLink) attachAspects (link, DestinationLink.class);
      links.put (address, link);
    }

    return link;
  }
  
  public Object getRemoteReference (MessageAddress address) 
  {
    return null;
  }

  class Link implements DestinationLink 
  {
    private MessageAddress destination;
    private DatagramSocketSpec spec, savedSpec;
    private DatagramSocket datagramSocket;
    private boolean connected;

    Link (MessageAddress dest) 
    {
      destination = dest;
    }

    public MessageAddress getDestination() 
    {
      return destination;
    }

    private void cacheDatagramSocketSpec () throws NameLookupException, UnregisteredNameException
    {
      if (spec == null)
      {
        try 
        {
          spec = lookupDatagramSocketSpec (destination);
          spec.setInetAddress (InetAddress.getByName (spec.getHost()));
        }
        catch (Exception e) 
        {
          // System.err.println ("OutgoingUDP: Error doing name or addr lookup: " +e);
          // throw new  NameLookupException (e);
        }

        if (spec != null) savedSpec = spec;
        else spec = savedSpec;

        if (spec == null) throw new UnregisteredNameException (destination);
      }
    }

    public String toString ()
    {
      return OutgoingUDPLinkProtocol.this + "-destination:" + destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingUDPLinkProtocol.class;
    }
   
    public int cost (AttributedMessage message) 
    {
      //  Calling cache spec is a hack to perform a name server lookup
      //  kind of method within the cost function rather than in the adaptive
      //  link selection policy code, where we believe it makes more sense.

      try 
      {
        cacheDatagramSocketSpec();
        return protocolCost;
      } 
      catch (Exception e) 
      {
        return Integer.MAX_VALUE;
      }                          
    }

    public Object getRemoteReference ()
    {
      return null;
    }

    public void addMessageAttributes (MessageAttributes attrs) 
    {}

    public MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
      cacheDatagramSocketSpec();

      synchronized (sendLock)  // important!!  -- change in 9.2?
      {
        boolean success = false;
        Exception save = null;

        try 
        {
/*
//  Testing hack
String toNode = MessageUtils.getToAgentNode(msg);
cnt++;
boolean isRegMsg = MessageUtils.isRegularAckMessage (msg);
if (toNode.equals("PerformanceNodeB") && isRegMsg && (cnt > 4 && cnt < 7)) success = true;
else
*/
          success = sendMessage (msg);
        } 
        catch (Exception e) 
        {
          save = e;
        }

        if (success == false)
        {
          spec = null;            // force name server recache
          datagramSocket = null;  // force new socket creation
          connected = false;      // force new connection

          Exception e = (save==null ? new Exception ("UDP sendMessage unsuccessful") : save);
          throw new CommFailureException (e);
        }

		MessageAttributes result = new SimpleMessageAttributes();
        String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
		result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
        return result;
      }
    }

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return true;  // ???
    }
   
    //  Send Cougaar message out to another node via socket

    private boolean sendMessage (AttributedMessage msg) throws Exception
    {
      if (debug) 
      {
        System.err.println ("\nOutgoingUDP: sending " +MessageUtils.toString(msg));
      }

      //  Since UDP datagram sockets are unconnected (unlike TCP sockets), we
      //  just send the message on its way and maybe it gets there (UDP is 
      //  unreliable by design, we only know by acking).
      //  UPDATE: Now in Java 1.4 datagram sockets can be connected.

//long start = System.currentTimeMillis();

      byte msgBytes[] = toBytes (msg);
      if (msgBytes == null) return false;

if (msgBytes.length >= 64*1024)
{
  //  HACK!!! Need better way to handle UDP 64kb packet length limitation

  if (debug)
  {
    System.err.println ("\nOutgoingUDP: msg exceeds 64kb limit! : " +msg);
  }
  
  return false;
}

      InetAddress addr = spec.getInetAddress();
      int port = spec.getPortAsInt();
      DatagramPacket dp = new DatagramPacket (msgBytes, msgBytes.length, addr, port); 

      if (datagramSocket == null) datagramSocket = new DatagramSocket();
/*
      if (!connected)
      {
        datagramSocket.connect (addr, port);
        connected = true;
      }
*/
      datagramSocket.send (dp);

//long end = System.currentTimeMillis();
//System.out.println ("UDP transmit (ms): " +(end-start));

      return true;  // send successful
    }

    private byte[] toBytes (Object data) 
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = null;

      try 
      {
	    oos = new ObjectOutputStream (baos);
	    oos.writeObject (data);
        oos.flush();
      } 
      catch (Exception e) 
      {
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
}
