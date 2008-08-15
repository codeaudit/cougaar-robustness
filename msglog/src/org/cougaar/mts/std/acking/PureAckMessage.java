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
 * 12 Feb 2003: Port to 10.0 (OBJS)
 * 27 Sep 2002: Add inband acking support. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 * 17 May 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.acking;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.std.MessageUtils;

public class PureAckMessage extends AttributedMessage
{
  private static final String RECEPTION_EXCEPTION = "MessageReceptionException";
  private static final String RECEPTION_NODE =      "MessageReceptionNode";
  private static final String INBAND_NODE_TIME =    "InbandNodeTime";

  public PureAckMessage () {}  // needed for incoming deserialization

  public PureAckMessage (AttributedMessage msg, PureAck pureAck)
  {
    //  Set pure ack msg to head back to where the source message came from

    super (new NullMessage (msg.getTarget(), msg.getOriginator()));

    //  Set key attributes

    MessageUtils.setMessageTypeToPureAck (this);
    MessageUtils.setAck (this, pureAck);
    MessageUtils.setFromAgent (this, MessageUtils.getToAgent (msg));
    MessageUtils.setToAgent (this, MessageUtils.getFromAgent (msg));
    MessageUtils.setSrcMsgNumber (this, MessageUtils.getMessageNumber (msg));

    //  Set back pointer

    pureAck.setMsg (this);
  }

  public static PureAckMessage createInbandPureAckMessage (AttributedMessage msg)
  {
    PureAck pureAck = new PureAck();

    if (msg != null)
    {
      String fromNode = MessageUtils.getFromAgentNode (msg);
      pureAck.setLatestAcks (MessageAckingAspect.getAcksToSend (fromNode));
    }
    else
    {
      MessageAddress originator = MessageAddress.getMessageAddress ("Unknown"); //100
      MessageAddress target = MessageAddress.getMessageAddress ("Unknown"); //100
      msg = new AttributedMessage (new NullMessage (originator, target));
    }

    return new PureAckMessage (msg, pureAck);
  }

  public PureAckMessage (PureAckMessage pureAckMsg)
  {
    super (pureAckMsg);
  }

  public void setReceptionException (Exception e)
  {
    setAttribute (RECEPTION_EXCEPTION, e);
  }

  public Exception getReceptionException ()
  {
    return (Exception) getAttribute (RECEPTION_EXCEPTION);
  }

  public boolean hasReceptionException ()
  {
    return (getReceptionException() != null);
  }

  public void setReceptionNode (String node)
  {
    setAttribute (RECEPTION_NODE, node);
  }

  public String getReceptionNode ()
  {
    return (String) getAttribute (RECEPTION_NODE);
  }

  public void setInbandNodeTime (int time)
  {
    setAttribute (INBAND_NODE_TIME, new Integer(time));
  }

  public int getInbandNodeTime ()
  {
    Integer i = (Integer) getAttribute (INBAND_NODE_TIME);
    return (i != null ? i.intValue() : -1);
  }

  public String toString ()
  {
    return MessageUtils.toString (this);
  }
}
