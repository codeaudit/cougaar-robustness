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
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


/**
 * IncomingUDPLinkProtocol is an IncomingLinkProtocol which receives
 * Cougaar messages from other nodes in the society via raw datagram (UDP) sockets.
 * */

public class IncomingUDPLinkProtocol extends IncomingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-UDP";

  private static final boolean debug;
  private static final String localhost;

  private static boolean showTraffic;

  private DatagramSocketSpec datagramSocketSpecs[];
  private DatagramSocketSpec myDatagramSocketSpec;
  private Vector datagramSocketListeners;
  private ThreadService threadService;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.udp.debug";
    debug = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.protocol.udp.localhost";
    localhost = System.getProperty (s, getLocalHost());
  }
 
  public IncomingUDPLinkProtocol ()
  {
    System.err.println ("Creating " + this);

    datagramSocketListeners = new Vector();

    //  Get incoming socket portnumber(s) from a property?

    String port = "0";   // port of 0 means auto-choose port number

    //  Create a single socket spec for now

    datagramSocketSpecs = new DatagramSocketSpec[1];
    datagramSocketSpecs[0] = new DatagramSocketSpec (localhost, port);

    //  Check if ShowTrafficAspect is loaded

    String sta = "org.cougaar.core.mts.ShowTrafficAspect";
//    showTraffic = (getAspectSupport().findAspect(sta) != null);
//  showTraffic = (AspectSupportImpl.instance().findAspect(sta) != null);
/*
    //  HACK!!!  See setNameSupport() as to why this is commented out 

    //  Start things going

    if (startup() == false)
    {
      throw new RuntimeException ("Problem starting IncomingUDPLinkProtocol");
    }
*/
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  public void setNameSupport (NameSupport nameSupport) 
  {
    //  HACK - shortcoming of transport construction - name support not avail till
    //  after transport constructor called - (name support should be universal 
    //  service) - a problem with an incoming transport as the only method called
    //  is the constructor basically.

    if (getNameSupport() == null)
    {
      throw new RuntimeException ("IncomingUDPLinkProtocol: nameSupport is null!");
    }

    if (startup() == false)
    {
      throw new RuntimeException ("Problem starting IncomingUDPLinkProtocol");
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

    //  Name support not available till after transport construction  -- still in 9.0.0?

    if (getNameSupport() == null) return;  

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
      System.err.println ("\nIncomingUDPLinkProtocol: registerClient");
      e.printStackTrace();
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
      System.err.println ("\nIncomingUDPLinkProtocol: unregisterClient");
      e.printStackTrace();
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
      System.err.println ("\nIncomingUDPLinkProtocol: registerMTS");
      e.printStackTrace();
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

      if (debug) 
      {
        System.err.println ("IncomingUDP: Datagram socket created on port " +port);
      }
    }
    catch (Exception e)
    {
      System.err.println ("\nIncomingUDP: Error creating datagram socket on port " +port+ ": ");
      e.printStackTrace();

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

      if (debug) 
      {
        System.err.println ("IncomingUDP: Datagram socket destroyed on port " +port);
      }
    }
    catch (Exception e)
    {
      System.err.println ("\nError destroying datagram socket on port " +listener.getPort()+ ": ");
      e.printStackTrace();
    }
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	ServiceBroker sb = getServiceBroker();
	threadService = (ThreadService) sb.getService (this, ThreadService.class, null);
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
        try 
        {
          //  Wait for an incoming packet

          datagramSocket.receive (datagramPacket);

//long start = System.currentTimeMillis();

          //  Convert packet into a Cougaar message

          Object obj = fromBytes (datagramPacket.getData());

          AttributedMessage msg = null;

          try
          {
            msg = (AttributedMessage) obj;
          }
          catch (Exception e)
          {
            System.err.println ("\nIncomingUDP: got non AttributedMessage! (ignored)");
            continue;
          }

//long end = System.currentTimeMillis();
//System.out.println ("UDP receive (ms): " +(end-start));

          if (showTraffic) System.err.print ("<S");

          if (debug) 
          {
            System.out.println ("\nIncomingUDP: datagram packet is from " + showAddress (datagramPacket));
            System.out.println ("IncomingUDP: read " +MessageUtils.toString(msg));
          }

          //  Deliver the message

          getDeliverer().deliverMessage (msg, msg.getTarget());
        }
        catch (MisdeliveredMessageException e)
        { 
          //  Not socket's fault - this exception comes from the deliverMessage call above

          System.err.println ("\nIncomingUDP: got MisdeliveredMessageException: " + e);
        }
        catch (Exception e)
        { 
          //  Typically a socket exception raised when the party at the
          //  other end closes their socket connection.

// not with udp sockets!

        if (debug) System.err.println ("\nIncomingUDP: Terminating socket exception: " + e);
/*
System.err.println ("\nIncomingUDP: Terminating socket exception: ");
e.printStackTrace();
*/
          quitNow = true;
          break;
        }
      }

      if (datagramSocket != null)
      {
        try { datagramSocket.close(); } catch (Exception e) {}
        datagramSocket = null;
      }

      //  HACK!  For now, destroy this server socket and create another.
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

  public static Object fromBytes (byte[] data) 
  {
	ObjectInputStream ois = null;
	Object obj = null;

	try 
    {
      ByteArrayInputStream bais = new ByteArrayInputStream (data);
	  ois = new ObjectInputStream (bais);
	  obj = ois.readObject();
	} 
    catch (ClassNotFoundException cnfe) 
    {
System.err.println ("\nIncomingUDP: fromBytes cnfe exception: ");
cnfe.printStackTrace();
	    return null;
	}
    catch (Exception e) 
    {
System.err.println ("\nIncomingUDP: fromBytes exception: ");
e.printStackTrace();
	    return null;
	}
	
	try 
    {
	    ois.close();
	} 
    catch (IOException e) {}

	return obj;
  }

  private static String showAddress (DatagramPacket dp)
  {
    return dp.getSocketAddress().toString();
  }
}
