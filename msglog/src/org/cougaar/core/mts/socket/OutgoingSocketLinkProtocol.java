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
import java.net.*;
import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;
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

  private static final int protocolCost;
  private static final int connectTimeout;

  private LoggingService log;
  private MyThreadService myThreadService;
  private Hashtable specCache, addressCache;
  private HashMap links;

private int cnt=0;  // temp

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.socket.cost";  // one way
    protocolCost = Integer.valueOf(System.getProperty(s,"1000")).intValue();

    s = "org.cougaar.message.protocol.socket.connectTimeoutSecs";
    connectTimeout = Integer.valueOf(System.getProperty(s,"5")).intValue();
  }
 
  public OutgoingSocketLinkProtocol ()
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

  private SocketSpec getSocketSpec (MessageAddress address) throws NameLookupException
  {
    synchronized (specCache)
    {
      SocketSpec spec = (SocketSpec) specCache.get (address);
      if (spec != null) return spec;
      spec = lookupSocketSpec (address);
      specCache.put (address, spec);
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
      addressCache.put (host, addr);
      return addr;
    }
  }

  private synchronized void clearCaches ()
  {
    specCache.clear();
    addressCache.clear();    
  }

  private SocketSpec lookupSocketSpec (MessageAddress address) throws NameLookupException
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof SocketSpec)
      {
        SocketSpec spec = (SocketSpec) obj;

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
        log.error ("Invalid SocketSpec in nameserver lookup!");
      }
    }

    return null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (getSocketSpec (address) != null);
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
    private NoHeaderOutputStream socketOut;

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
      return OutgoingSocketLinkProtocol.this + "-dest:" + destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingSocketLinkProtocol.class;
    }
   
    public int cost (AttributedMessage msg) 
    {
      try 
      {
        if (msg != null) getSocketSpec (destination);  // HACK binding
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

    public boolean retryFailedMessage (AttributedMessage msg, int retryCount)
    {
      return true;
    }
   
    private void dumpCachedData ()
    {
      clearCaches();
      socketOut = null;
    }

    public MessageAttributes forwardMessage (AttributedMessage msg) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
      //  Dump our cached data on message send retries

      if (MessageUtils.getSendTry (msg) > 1) dumpCachedData();

      //  Get socket spec for destination

      SocketSpec destSpec = getSocketSpec (destination);

      //  Send message via socket

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

      //  Dump our cached data on failed sends and throw an exception

      if (success == false)
      {
        dumpCachedData();
        Exception e = (save==null ? new Exception ("socket sendMessage unsuccessful") : save);
        throw new CommFailureException (e);
      }

      //  Successful send

	  MessageAttributes successfulSend = new SimpleMessageAttributes();
      String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
	  successfulSend.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return successfulSend;
    }

    //  Send Cougaar message out to another node via socket

    private boolean sendMessage (AttributedMessage msg, SocketSpec spec) throws Exception
    {
      if (log.isDebugEnabled()) log.debug ("Sending " +MessageUtils.toString(msg));

      //  Serialize the message into a byte buffer

      byte msgBytes[] = toBytes (msg);
      if (msgBytes == null) return false;

      //  Since it is a hassle to read variable length arrays on the other side, we wrap
      //  the byte buffer in a simple object.

      ByteArrayObject msgObject = new ByteArrayObject (msgBytes);

      //  It appears that the only way we can determine whether our socket
      //  connection is still valid is to try using it.  So we try it,
      //  and if it fails, we create a new connected socket and try again.
      
      for (int tryN=1; tryN<=2; tryN++) // try at most twice
      {
        try
        {
          if (socketOut == null) 
          {
            socketOut = getSocketOutputStream (spec);
            tryN = 3;  // only try once after new socket
          }

          socketOut.reset();
          socketOut.writeObject (msgObject);
          socketOut.flush();

          break;  // success
        }
        catch (Exception e)
        {
          //  The send has failed somehow.  Close the socket and null it out
          //  so that it will get reconstructed next time.

          if (socketOut != null)
          {
            try { socketOut.close(); } catch (Exception ee) {}
            socketOut = null;
          }

          //  If this is the second send try, go ahead and throw the exception
          //  so that the caller can see it.

          if (tryN >= 2) throw (e);
        }
      }

      return true;  // send successful
    }

    private NoHeaderOutputStream getSocketOutputStream (SocketSpec spec) throws CommFailureException
    {
      //  We use a timeout on establishing a socket connection because if
      //  there is a network or some other kind of problem we could otherwise
      //  potentially hang here for awhile, perhaps indefinitely.
      
      String host = spec.getHost();
      InetAddress addr = spec.getInetAddress();
      int port = spec.getPortAsInt();

      if (log.isDebugEnabled()) log.debug ("Making new socket to " +addr+ ":" +port+ " for " +this);
      
      try 
      { 
        if (myThreadService == null) myThreadService = new MyThreadService (this, getThreadService(this));
        Socket socket = TimedSocket.getSocket (host, addr, port, connectTimeout*1000, myThreadService);
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
      threadService = (ThreadService) getServiceBroker().getService (obj, ThreadService.class, null);
      return threadService;
    }
  }

  private synchronized byte[] toBytes (Object data)  // serialization has needed sync before
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
