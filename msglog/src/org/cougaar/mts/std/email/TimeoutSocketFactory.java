/*
 * <copyright>
 *  Copyright 2002-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 14 Mar 2003: Commented out unscheduleSocketClose (style police).
 * 23 Sep 2002: Created. (OBJS)
 */

package org.cougaar.mts.std.email;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import javax.net.SocketFactory;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.std.SocketClosingService;

public class TimeoutSocketFactory extends SocketFactory
{
  private ServiceBroker sb;
  private LoggingService log;
  private SocketClosingService socketCloser;
  private int socketTimeout;

  private TimeoutSocketFactory () {}

  protected TimeoutSocketFactory (ServiceBroker sb, int socketTimeout)
  {
    this.sb = sb;
    this.socketTimeout = socketTimeout;

    if (sb == null) throw new RuntimeException ("Null service broker!");

    log = (LoggingService) sb.getService (this, LoggingService.class, null);
    if (log.isDebugEnabled()) log.debug ("Creating " +this);

    socketCloser = (SocketClosingService) sb.getService (this, SocketClosingService.class, null);
    if (socketCloser == null) log.error ("Cannot do socket timeouts - SocketClosingService not available!");
  }
  
  public Socket createSocket ()
  {
    return new Socket();
  }

  public Socket createSocket (InetAddress address, int port) throws IOException
  {
    InetSocketAddress remoteAddr = new InetSocketAddress (address, port);               
    return createTimeoutSocket (remoteAddr);
  }

  public Socket createSocket (InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
  {
    InetSocketAddress remoteAddr = new InetSocketAddress (address, port);               
    InetSocketAddress localAddr  = new InetSocketAddress (localAddress, localPort);
    return createTimeoutSocket (remoteAddr, localAddr);
  }

  public Socket createSocket (String host, int port) throws IOException
  {
    InetSocketAddress remoteAddr = new InetSocketAddress (host, port);               
    return createTimeoutSocket (remoteAddr);
  }

  public Socket createSocket (String host, int port, InetAddress localHost, int localPort) throws IOException
  {
    InetSocketAddress remoteAddr = new InetSocketAddress (host, port);               
    InetSocketAddress localAddr  = new InetSocketAddress (localHost, localPort);
    return createTimeoutSocket (remoteAddr, localAddr);
  }

  private Socket createTimeoutSocket (SocketAddress remoteAddr) throws IOException
  {
    return createTimeoutSocket (remoteAddr, null);
  }

  private Socket createTimeoutSocket (SocketAddress remoteAddr, SocketAddress localAddr) throws IOException
  {
    Socket socket = new Socket();
    scheduleSocketClose (socket, socketTimeout);
    if (localAddr != null) socket.bind (localAddr);
    socket.connect (remoteAddr, socketTimeout);  // timeout as unconnected socket close raises no exceptions
    return socket;
  }

  private void scheduleSocketClose (Socket socket, int timeout)
  {
    if (socketCloser != null) socketCloser.scheduleClose (socket, timeout);
  }

/* //102B
  private void unscheduleSocketClose (Socket socket)
  {
    if (socketCloser != null) socketCloser.unscheduleClose (socket);
  }
*/

  public String toString ()
  {
    return getClass().getName();
  }
}
