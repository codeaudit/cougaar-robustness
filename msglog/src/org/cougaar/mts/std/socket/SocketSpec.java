/*
 * <copyright>
 *  Copyright 2001-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 12 Feb 2003: Ported to 10.0 (OBJS)
 * 18 Aug 2002: Added inet address field. (OBJS)
 * 11 Jul 2001: Marked serializable, added equals method. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.socket;

import java.net.InetAddress;
import java.net.URI;  //100
import java.net.URISyntaxException;  //100


/**
 *  SocketSpec holds information about the host, port, and 
 *  inet address for a socket connection.
 */

public class SocketSpec implements java.io.Serializable
{
  private String host;
  private String port;
  private URI uri; //100
  private final static String scheme = "tcp"; //100

  private transient InetAddress inetAddress;

  public SocketSpec (String port)
    throws URISyntaxException //100
  {
    this ("localhost", port);
  }

  public SocketSpec (String host, int port) 
    throws URISyntaxException //100
  {
    this.host = host;
    this.port = ""+port;
    cacheURI(); //100
  }

  public SocketSpec (String host, String port)
    throws URISyntaxException //100
  {
    this.host = host;
    this.port = port;
    cacheURI(); //100
  }

  public SocketSpec (URI uri) //100
  {
    this.host = uri.getHost();
    this.port = "" + uri.getPort();
    this.uri = uri;
  }
  
  private void cacheURI () //100
    throws URISyntaxException
  {
    this.uri = new URI (scheme, null, host, getPortAsInt(), null, null, null);
  }

  public String getHost ()  
  {
    return host;
  }

  public void setHost (String host)
    throws URISyntaxException //100
  {
    this.host = host;
    cacheURI(); //100
  }

  public String getPort ()  
  {
    return port;
  }

  public int getPortAsInt ()  
  {
    int int_port = -1;  // -1 means no port specified
    try  { int_port = Integer.valueOf(port).intValue(); } catch (Exception e) {}
    return int_port;
  }

  public void setPort (String port)
    throws URISyntaxException //100
  {
    this.port = port;
    cacheURI(); //100
  }

  public void setPort (int port)
    throws URISyntaxException //100
  {
    this.port = "" + port;
    cacheURI(); //100
  }

  public InetAddress getInetAddress ()
  {
    return inetAddress;
  }

  public void setInetAddress (InetAddress addr)
  {
    inetAddress = addr;
  }

  public URI getURI () //100
  {
    return uri;
  }

  public boolean equals (Object obj)
  {
    if ((obj instanceof SocketSpec) == false) return false;

    SocketSpec a = this;
    SocketSpec b = (SocketSpec) obj;

    String ahost = (a.host != null) ? a.host : "";
    String bhost = (b.host != null) ? b.host : "";
    if (ahost.equals(bhost) == false) return false; 

    String aport = (a.port != null) ? a.port : "";
    String bport = (b.port != null) ? b.port : "";
    if (aport.equals(bport) == false) return false; 

    return true;
  }

  public String toString ()
  {
    //100 return "SocketSpec[" +host+ ":" +port+ "]";
    return uri.toString(); //100
  }
}
