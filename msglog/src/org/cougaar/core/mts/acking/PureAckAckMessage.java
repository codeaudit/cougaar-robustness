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
 * 27 Sep 2002: Add inband ack creator. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 * 20 May 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;

import org.cougaar.core.mts.*;


public class PureAckAckMessage extends PureAckMessage
{
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
    return new PureAckAckMessage (pam, pureAckAck);
  }
}
