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
 * 24 Aug  2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;


/**
 *  Data object used by the email transport to store information
 *  in the name servers for nodes to describe themselves so they
 *  can be communicated with by the email transport.
**/

public class MailData implements java.io.Serializable
{
  private String nodeID;
  private MailBox inbox;

  public MailData (String nodeID, MailBox inbox)
  {
    this.nodeID = nodeID; 
    this.inbox = inbox;
  }

  public String getNodeID ()
  {
    return nodeID;
  }

  public MailBox getInbox ()
  {
    return inbox;
  }

  public String toString ()
  {
    return "nodeID: " +nodeID+ "\ninbox: " + inbox.toStringDiscreet();
  }
}
