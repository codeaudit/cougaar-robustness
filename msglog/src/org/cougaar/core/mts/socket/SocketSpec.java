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
 * 11 July 2001: Marked serializable, added equals method. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.socket;


/**
 *  SocketSpec holds information about the host and port for a
 *  socket connection.
**/

public class SocketSpec implements java.io.Serializable
{
  private String host;
  private String port;

  public SocketSpec (String port)
  {
    this ("localhost", port);
  }

  public SocketSpec (String host, int port)
  {
    this (host, ""+port);
  }

  public SocketSpec (String host, String port)
  {
    this.host = host;
    this.port = port;
  }

  public String getHost ()  
  {
    return host;
  }

  public void setHost (String host)
  {
    this.host = host;
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
  {
    this.port = port;
  }

  public void setPort (int port)
  {
    this.port = "" + port;
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
    return "socket[" +host+ ":" +port+ "]";
  }
}
