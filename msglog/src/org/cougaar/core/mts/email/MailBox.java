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
 * 14 July 2001: Marked serializable, added new constructor. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;


/**
 *  Data object for a mailbox used to send or receive email.
**/

public class MailBox implements java.io.Serializable
{
  private String protocol;
  private String serverHost;
  private String serverPort = "-1";  // -1 means use default port to JavaMail
  private String emailAddress;
  private String username;
  private String password;
  private String folder;             // Note: POP3 only supports "INBOX" (case-insensitive)

  private MailMessageHeader boxHeader;

  public MailBox ()
  {}

  public MailBox (String serverHost, String username)
  {
    this (null, serverHost, null, username, null, null);
  }

  public MailBox (String protocol, String serverHost, String serverPort)
  {
    this (protocol, serverHost, serverPort, null, null, null);
  }

  public MailBox (String protocol, String serverHost, String serverPort, String username, String password, String folder)
  {
    this.protocol = protocol;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.username = username;
    this.password = password;
    this.folder = folder;
  }

  public String getProtocol ()  
  {
    return protocol;
  }

  public void setProtocol (String protocol)
  {
    this.protocol = protocol;
  }

  public String getServerHost ()  
  {
    return serverHost;
  }

  public void setServerHost (String serverHost)
  {
    this.serverHost = serverHost;
  }

  public String getServerPort ()  
  {
    return serverPort;
  }

  public int getServerPortAsInt ()  
  {
    int port = -1;  // -1 means default port to JavaMail
    try  { port = Integer.valueOf(serverPort).intValue(); } catch (Exception e) {}
    return port;
  }

  public void setServerPort (String serverPort)
  {
    this.serverPort = serverPort;
  }

  public void setServerPort (int serverPort)
  {
    this.serverPort = "" + serverPort;
  }

  public String getEmailAddress ()  
  {
    return emailAddress;
  }

  public void setEmailAddress (String emailAddress)
  {
    this.emailAddress = emailAddress;
  }

  public String getUsername ()  
  {
    return username;
  }

  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getPassword ()  
  {
    return password;
  }

  public void setPassword (String password)
  {
    this.password = password;
  }

  public String getFolder ()  
  {
    return folder;
  }

  public void setFolder (String folder)
  {
    this.folder = folder;
  }

  public MailMessageHeader getBoxHeader ()  
  {
    return boxHeader;
  }

  public void setBoxHeader (MailMessageHeader h)  
  {
    boxHeader = h;
  }

  public String toString ()
  {
    return "[" + "\n" +
           "     protocol= " + protocol + "\n" +
           "   serverHost= " + serverHost + "\n" +
           "   serverPort= " + serverPort + "\n" +
           " emailAddress= " + emailAddress + "\n" +
           "     username= " + username + "\n" +
           "     password= " + password + "\n" +
           "       folder= " + folder + "\n" +
                               (boxHeader != null ? boxHeader+"\n" : "") +
           "]";
  }

  public String toStringDiscreet ()
  {
    return "[" + "\n" +
           "     protocol= " + protocol + "\n" +
           "   serverHost= " + serverHost + "\n" +
           "   serverPort= " + serverPort + "\n" +
           " emailAddress= " + emailAddress + "\n" +
           "     username= " + username + "\n" +
           "     password= " + "*******" + "\n" +
           "       folder= " + folder + "\n" +
                               (boxHeader != null ? boxHeader+"\n" : "") +
           "]";
  }
}
