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
import org.cougaar.core.service.LoggingService;

/**
 *  Outgoing UDP Link Protocol
**/

public class OutgoingUDPLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";
  public static final int    MAX_UDP_MSG_SIZE = 64*1024;

  private static final int protocolCost;
  private static final Object sendLock = new Object();

  private LoggingService log;
  private DatagramSocket datagramSocket;
  private HashMap links;

private int cnt = 0;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.cost";
    protocolCost = Integer.valueOf(System.getProperty(s,"166")).intValue();
  }
 
  public OutgoingUDPLinkProtocol ()
  {
    links = new HashMap();
  }

  public void load ()
  {
    super_load();

    log = loggingService;
    if (log.isInfoEnabled()) log.info ("Creating " + this);

    if (startup() == false)
    {
      throw new RuntimeException ("Failure starting up " +this);
    }
  }

  public synchronized boolean startup ()
  {
    shutdown();
    return true;
  }

  public synchronized void shutdown ()
  {}

  public String toString ()
  {
    return this.getClass().getName();
  }

  private DatagramSocketSpec lookupDatagramSocketSpec (MessageAddress address) throws NameLookupException
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof DatagramSocketSpec)
      {
        DatagramSocketSpec spec = (DatagramSocketSpec) obj;

        //  Cache inet addresses?

        try
        {
          spec.setInetAddress (InetAddress.getByName (spec.getHost()));
        }
        catch (Exception e)
        {
          throw new NameLookupException (e);
        }

        return spec;
      }
      else
      {
        loggingService.error ("Invalid DatagramSocketSpec in lookup!");
      }
    }

    return null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (lookupDatagramSocketSpec (address) != null);
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
    private DatagramSocket datagramSocket;

    Link (MessageAddress dest) 
    {
      destination = dest;
    }

    public MessageAddress getDestination() 
    {
      return destination;
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
      //  Calling lookup spec is a hack to perform a name server lookup
      //  kind of method within the cost function rather than in the adaptive
      //  link selection policy code, where we believe it makes more sense.

      try 
      {
        lookupDatagramSocketSpec (destination);
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

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return true;
    }

    public MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
      //  Get socket spec for destination

      DatagramSocketSpec spec = lookupDatagramSocketSpec (destination);

      //  Send message via udp

      synchronized (sendLock)
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
          success = sendMessage (msg, spec);
        } 
        catch (Exception e) 
        {
          save = e;
        }

        if (success == false)
        {
          datagramSocket = null;  // force new socket creation
          Exception e = (save==null ? new Exception ("UDP sendMessage unsuccessful") : save);
          throw new CommFailureException (e);
        }

		MessageAttributes result = new SimpleMessageAttributes();
        String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
		result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
        return result;
      }
    }
   
    private boolean sendMessage (AttributedMessage msg, DatagramSocketSpec spec) throws Exception
    {
      if (log.isDebugEnabled()) log.debug ("Sending " +MessageUtils.toString(msg));

      //  Since UDP datagram sockets are unreliable we just send the message on its way 
      //  and maybe it gets there.  Acking will tell us if it does or not.

      byte msgBytes[] = toBytes (msg);
      if (msgBytes == null) return false;

      //  Make sure message will fit into 64 KB datagram packet limitation

      if (msgBytes.length >= MAX_UDP_MSG_SIZE)
      {
        //  HACK!!!  Need better way to handle the UDP 64kb packet length limitation
        //  The problem with this is that it pollutes the send history of UDP with false 
        //  errors - size limitation it is not a UDP send error.

        if (log.isWarnEnabled()) 
          log.warn ("Msg exceeds 64kb datagram limit! (" +msgBytes.length+ "): " +MessageUtils.toString(msg));

        MessageUtils.setMessageSize (msg, msgBytes.length);  // avoid udp selection again for this msg
        return false;
      }

      //  Build the datagram

      InetAddress addr = spec.getInetAddress();
      int port = spec.getPortAsInt();
      DatagramPacket dp = new DatagramPacket (msgBytes, msgBytes.length, addr, port); 

      //  Make and connect new socket as needed

      if (datagramSocket == null) 
      {
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
            datagramSocket.connect (addr, port);
          }
          else throw (e);
        }
      }
 
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
