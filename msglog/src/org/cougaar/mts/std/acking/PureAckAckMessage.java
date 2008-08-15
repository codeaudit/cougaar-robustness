/*
 * <copyright>
 *  Copyright 2001,2002,2004 Object Services and Consulting, Inc. (OBJS),
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
 * 01 Mar 2004: Port to 11.0. (OBJS)
 * 27 Sep 2002: Add inband ack creator. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 * 20 May 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.acking;

import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.std.MessageUtils;

public class PureAckAckMessage extends PureAckMessage
{
  private static final String INBAND_ACK_SEND_TIME = "InbandAckSendTime";

  public PureAckAckMessage () {}  // needed for incoming deserialization

  public PureAckAckMessage (AttributedMessage msg, PureAckAck pureAckAck)
  {
    super (msg, pureAckAck);
    MessageUtils.setMessageTypeToPureAckAck (this);
    pureAckAck.setMsg (this);
  }

  public PureAckAckMessage (PureAckAckMessage pureAckAckMsg)
  {
    super (pureAckAckMsg);
  }

  public static PureAckAckMessage createInbandPureAckAckMessage (PureAckMessage pam)
  {
    PureAck pureAck = (PureAck) MessageUtils.getAck (pam);
    PureAckAck pureAckAck = new PureAckAck (pureAck);
    String fromNode = MessageUtils.getFromAgentNode (pam);
    pureAckAck.setLatestAcks (MessageAckingAspect.getAcksToSend (fromNode));
    PureAckAckMessage paam = new PureAckAckMessage (pam, pureAckAck);
    paam.setInbandAckSendTime (0);  // temp HACK
    return paam;
  }

  public boolean isInbandAckAck ()
  {
    return hasInbandAckSendTime();  // bit of a HACK
  }

  public void setInbandAckSendTime (long time)
  {
    setAttribute (INBAND_ACK_SEND_TIME, new Long (time));
  }

  public long getInbandAckSendTime ()
  {
    Long t = (Long) getAttribute (INBAND_ACK_SEND_TIME);
    return (t != null ? t.longValue() : -1);
  }

  public boolean hasInbandAckSendTime ()
  {
    return (getAttribute (INBAND_ACK_SEND_TIME) != null);
  }
}
