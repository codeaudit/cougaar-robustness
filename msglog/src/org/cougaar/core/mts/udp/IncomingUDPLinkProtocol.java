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
import java.net.*;
import java.util.*;

import org.cougaar.core.mts.*;
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

  private static LoggingService log;
  private static boolean showTraffic;

  private static final String localhost;

  private DatagramSocketSpec datagramSocketSpecs[];
  private DatagramSocketSpec myDatagramSocketSpec;
  private Vector datagramSocketListeners;
  private ThreadService threadService;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.localhost";
    localhost = System.getProperty (s, getLocalHost());
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

    for (int i=0; i<datagramSocketSpecs.length; i++) 
    {
      int port = createDatagramSocket (datagramSocketSpecs[i].getPortAsInt());
      datagramSocketSpecs[i].setPort (port);  // potentially new port number
      if (port > 0) count++;                  // if port < 1 bad port
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

  private void registerDatagramSocketSpec (String host, int port)
  {
    //  Update the name server
  
    unregisterDatagramSocketSpec();  // HACK! re-register clients & mts too?????
    myDatagramSocketSpec = new DatagramSocketSpec (host, port);
    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().registerAgentInNameServer (myDatagramSocketSpec, nodeAddress, PROTOCOL_TYPE);
  }

  private void unregisterDatagramSocketSpec ()
  {
    if (myDatagramSocketSpec == null) return;  // no socket spec to unregister

    //  Update the name server

    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().unregisterAgentInNameServer (myDatagramSocketSpec, nodeAddress, PROTOCOL_TYPE);
  }

  public final void registerClient (MessageTransportClient client) 
  {
    if (myDatagramSocketSpec == null) return;  // no socket spec to register

    try 
    {
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().registerAgentInNameServer (myDatagramSocketSpec, clientAddress, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("registerClient: " +stackTraceToString(e));
    }
  }

  public final void unregisterClient (MessageTransportClient client) 
  { 
    if (myDatagramSocketSpec == null) return;  // no socket spec to unregister

    try 
    {
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().unregisterAgentInNameServer (myDatagramSocketSpec, clientAddress, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("unregisterClient: " +stackTraceToString(e));
    }
  }

  public final void registerMTS (MessageAddress addr)
  {
    if (myDatagramSocketSpec == null) return;  // no socket spec to register

    try 
    {
      getNameSupport().registerAgentInNameServer (myDatagramSocketSpec, addr, PROTOCOL_TYPE);
    } 
    catch (Exception e) 
    {
      log.error ("registerMTS: " +stackTraceToString(e));
    }
  }

  private int createDatagramSocket (int port)
  {
    DatagramSocketListener listener = null;

    try
    {
      listener = new DatagramSocketListener (port);
      port = listener.getPort();  // port possibly updated

      Schedulable thread = threadService().getThread (this, listener, "DatagramIncomingSock_"+port);
      thread.start();

      registerDatagramSocketSpec (localhost, port);
      datagramSocketListeners.add (listener);

      if (log.isInfoEnabled()) log.info ("Incoming datagram socket created on port " +port);
    }
    catch (Exception e)
    {
      log.error ("Error creating datagram socket on port " +port+ ":\n" +stackTraceToString(e));
      if (listener != null) listener.quit();
      listener = null;
      return -1;
    }

    return port;
  }

  private void destroyDatagramSocket (DatagramSocketListener listener)
  {
    if (listener == null) return;

    int port = listener.getPort();

    try
    {
      listener.quit();
//    unregisterSocketSpec();   // synchronization problem?
      datagramSocketListeners.remove (listener);
      if (log.isDebugEnabled()) log.debug ("Datagram socket destroyed on port " +port);
    }
    catch (Exception e)
    {
      if (log.isWarnEnabled()) log.warn ("Error destroying datagram socket on port " +listener.getPort()+ ": " +e);
    }
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
	return threadService;
  }

  private class DatagramSocketListener implements Runnable
  { 
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private boolean quitNow;

    public DatagramSocketListener (int port) throws IOException
    {
      datagramSocket = new DatagramSocket (port);

      byte[] buf = new byte[64*1024];
      datagramPacket = new DatagramPacket (buf, buf.length);
    }

    public int getPort ()
    {
      return (datagramSocket != null) ? datagramSocket.getLocalPort() : -1;
    }

    public synchronized void quit ()
    {
      quitNow = true;

      if (datagramSocket != null)
      {
        try { datagramSocket.close(); } catch (Exception e) {}
        datagramSocket = null;
      }
    }

    public void run() 
    {
      if (datagramSocket == null) return;

      while (!quitNow)
      {
        AttributedMessage msg = null;

        try 
        {
          //  Wait for an incoming packet

          datagramSocket.receive (datagramPacket);
          if (showTraffic) System.err.print ("<U");

          //  Convert packet into a Cougaar message

          Object obj = getObjectFromBytes (datagramPacket.getData());

          try
          {
            msg = (AttributedMessage) obj;  // possible cast exception
          }
          catch (Exception e)
          {
            if (log.isWarnEnabled()) log.warn ("Got non AttributedMessage msg! (ignored): " +e);
            continue;
          }

          if (log.isDebugEnabled()) 
          {
            log.debug ("From " +showAddress(datagramPacket)+ " read " +MessageUtils.toString(msg));
          }
        }
        catch (Exception e)
        { 
          //  Typically a socket exception raised when the party at the
          //  other end closes their socket connection, but this is not
          //  the case here with datagram sockets.

          if (log.isDebugEnabled()) 
          {
            log.debug ("Terminating datagram socket exception:\n" +stackTraceToString(e));
          }

          quitNow = true;
          break;
        }

        //  Deliver the message

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

      if (datagramSocket != null)
      {
        try { datagramSocket.close(); } catch (Exception e) {}
        datagramSocket = null;
      }

      //  HACK!  For now, destroy this datagram socket and create another.
      //  Will work this out better later.

      destroyDatagramSocket (this);
      createDatagramSocket (0);  // port num should come from orig?
    }
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
      throw new RuntimeException (e.toString());
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

  private static String showAddress (DatagramPacket dp)
  {
    return dp.getSocketAddress().toString().substring(1);  // drop leading '/'
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
