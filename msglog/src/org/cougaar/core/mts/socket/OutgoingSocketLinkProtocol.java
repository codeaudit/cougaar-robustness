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
 * 16 May 2002: Port to Cougaar 9.2.x (OBJS)
 * 08 Apr 2002: Port to Cougaar 9.1.x (OBJS)
 * 21 Mar 2002: Port Cougaar 9.0.0 (OBJS)
 * 02 Jan 2002: Added connectTimeoutSecs property. (OBJS)
 * 11 Dec 2001: Save backup of cached socket data from name server. (OBJS) 
 * 02 Dec 2001: Implement getRemoteReference (8.6.2.0) (OBJS)
 * 20 Nov 2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 09 Nov 2001: Alter sendMessage to throw exception in case of second
 *              failure, for debugging purposes. (OBJS)
 * 25 Oct 2001: Fix stream corruption bug by adopting no header object IO
 *              streams and reseting stream for each object.  Bug only 
 *              manifests itself if output stream not used shortly after 
 *              transport initialization.  Do not know yet know why this
 *              is happening, it is not use of SO timeout.  Testing shows
 *              reset is needed for each object send, just at stream 
 *              creation doesn't work.  This modification to the object
 *              IO streams was needed anyways at some point to deal with
 *              the possible case of object evolution happening sooner
 *              than stream reconstruction (for long lived connections),
 *              as object references would then be invalidated. (OBJS)
 * 23 Oct 2001: Remove extraneous synchonized statement on sendMessage 
 *              method. (OBJS)
 * 21 Sep 2001: Make call to sendMessage class synchronized. (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol, add debug and
 *              cost properties. (OBJS)
 * 25 Sep 2001: Port to Cougaar 8.4.1 (OBJS)
 * 14 Sep 2001: Port to Cougaar 8.4 (OBJS)
 * 22 Aug 2001: Revamped for new 8.3.1 component model. (OBJS)
 * 11 Jul 2001: Added initial name server support. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.socket;

import java.io.*;
import java.util.*;
import java.net.Socket;

import org.cougaar.util.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ThreadService;


/**
 * OutgoingSocketLinkProtocol is an OutgoingLinkProtocol which uses Internet
 * sockets (TCP) to send Cougaar messages from the current node to other nodes in the society.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol,
 * org.cougaar.core.mts.socket.IncomingSocketLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.socket.cost</b>
 * The cost function of the DestinationLink inner subclass defaults to 500, so 
 * that, using the default MinCostLinkSelectionPolicy, it will be chosen first, before 
 * RMILinkProtocol, OutgoingEmailLinkProtocol, and NNTPLinkProtocol, which are
 * less efficient.  When using AdaptiveLinkSelectionPolicy, cost is
 * one of the factors that are used to select a protocol. To modify the default
 * cost, set this property to an integer. 
 * <br>(e.g. -Dorg.cougaar.message.protocol.socket.cost=1250)
 * <p>
 * <b>org.cougaar.message.protocol.socket.debug</b> 
 * If true, prints debug information to System.out.
 * */

public class OutgoingSocketLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-socket";

  private static final boolean debug;
  private static final int protocolCost;
  private static final int connectTimeout;
  private static final Object sendLock = new Object();

  private HashMap links, sockets;
  private MyThreadService myThreadService;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.socket.debug";
    debug = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.socket.cost";
    protocolCost = Integer.valueOf(System.getProperty(s,"500")).intValue();

    s = "org.cougaar.message.protocol.socket.connectTimeoutSecs";
    connectTimeout = Integer.valueOf(System.getProperty(s,"5")).intValue();
  }
 
  public OutgoingSocketLinkProtocol ()
  {
    System.err.println ("Creating " + this);

    links = new HashMap();
    sockets = new HashMap();

    //  Transport initialization

    if (startup() == false)
    {
      throw new RuntimeException ("Failure starting up OutgoingSocketLinkProtocol!");
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

  private SocketSpec lookupSocketSpec (MessageAddress address)
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof SocketSpec)
      {
        return (SocketSpec) obj;
      }
      else
      {
        System.err.println ("OutgoingSocketLinkProtocol: invalid obj in lookup!");
      }
    }

    return null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (lookupSocketSpec (address) != null);
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
    Socket s = (Socket) sockets.get (address);

    return "Socket[dst=" +s.getInetAddress()+  ":" +s.getPort() + 
                 ",src=" +s.getLocalAddress()+ ":" +s.getLocalPort()+ "]";
  }

  class Link implements DestinationLink 
  {
    private MessageAddress destination;
    private NoHeaderOutputStream messageOut;

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
      return OutgoingSocketLinkProtocol.this + "-destination:" + destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingSocketLinkProtocol.class;
    }
   
    public int cost (AttributedMessage message) 
    {
      // return protocolCost;  // pre 8.6.1

      //  Calling lookupSocketSpec() is a hack to perform the canSendMessage()
      //  kind of method within the cost function rather than in the adaptive
      //  link selection policy code, where we believe it makes more sense.

      try 
      {
        lookupSocketSpec (destination);
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
    {
      // TBD
    }

    public boolean retryFailedMessage (AttributedMessage msg, int retryCount)
    {
      return true;
    }
   
    public MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
/*
if (message instanceof MessageAckingAspect.AckEnvelope)
{
  MessageAckingAspect.AckEnvelope env = (MessageAckingAspect.AckEnvelope)message;

  int num = env.getMessageNumber();
  int sndCount = env.getSendCount();
  String target = env.getTargetNode();

  if (target.equals("PerformanceNodeA") && sndCount == 1 && num == 5) return;
//if (sndCount == 1 && num == 5) return;
}
*/
      //  Get socket address of destination 

      SocketSpec destSpec = lookupSocketSpec (destination);

      //  Try sending the message over the socket

      synchronized (sendLock)
      {
        boolean success = false;
        Exception save = null;

        try 
        {
          success = sendMessage (msg, destSpec);
        } 
        catch (Exception e) 
        {
          save = e;
        }

        if (success == false)
        {
           Exception e = (save==null ? new Exception ("socket sendMessage unsuccessful") : save);
           throw new CommFailureException (e);
        }

        MessageAttributes result = new SimpleMessageAttributes();
        String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
        result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
        return result;
      }
    }

    //  Send Cougaar message out to another node via socket

    private boolean sendMessage (AttributedMessage msg, SocketSpec destSpec) throws Exception
    {
      if (debug) 
      {
        System.err.println ("\nOutgoingSocket: send " +MessageUtils.toString(msg));
      }

      //  It appears that the only way we can determine whether our message
      //  output stream is still valid is to try using it.  So we try it,
      //  and if it fails, we create a new output stream and try again.

//long start = System.currentTimeMillis();
      
      boolean success = false;

      for (int tryN=1; success==false && tryN<=2; tryN++) // try at most twice
      {
        try
        {
          if (messageOut == null) messageOut = getMessageOutputStream (destSpec);

          messageOut.reset();  // important!! Fixes stream corruption bug!

          messageOut.writeObject (msg);
          messageOut.flush();

          success = true;
        }
        catch (Exception e)
        {
          //  Out stream has failed somehow.  Close it and null it out
          //  so that it will get reconstructed next time.

          if (messageOut != null)
          {
            try { messageOut.close(); } catch (Exception me) {}
            messageOut = null;
          }

          //  If this is the second try, go ahead and throw the exception
          //  so that the caller can see it.

          if (tryN == 2) throw (e);
        }
      }

//long end = System.currentTimeMillis();
//System.out.println ("Socket transmit (ms): " +(end-start));

      return success;
    }

    private NoHeaderOutputStream getMessageOutputStream (SocketSpec dest) 
      throws CommFailureException
    {
      //  We use a timeout on establishing a connection because if there
      //  is a network or some other kind of problem we could just hang
      //  here for awhile, perhaps indefinitely, otherwise.
      
      String host = dest.getHost();
      int port = dest.getPortAsInt();
      
      try 
      { 
        if (myThreadService == null) myThreadService = new MyThreadService (this, getThreadService(this));
        Socket socket = TimedSocket.getSocket (host, port, connectTimeout*1000, myThreadService);
        sockets.put (destination, socket);
        socket.shutdownInput();  // not essential
        return new NoHeaderOutputStream (socket.getOutputStream());
      }
      catch (Exception e) 
      {
        throw new CommFailureException (e);
      }
    }

    private ThreadService getThreadService (Object obj) 
    {
      ServiceBroker sb = getServiceBroker();
      threadService = (ThreadService) sb.getService (obj, ThreadService.class, null);
      return threadService;
    }
  }
}
