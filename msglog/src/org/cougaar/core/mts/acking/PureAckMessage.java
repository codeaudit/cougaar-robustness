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
 * 27 Sep 2002: Add reception exception and inband ack creator. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 * 17 May 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;

import org.cougaar.core.mts.*;


public class PureAckMessage extends AttributedMessage
{
  private Exception receptionException;

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
    String fromNode = MessageUtils.getFromAgentNode (msg);
    pureAck.setLatestAcks (MessageAckingAspect.getAcksToSend (fromNode));
    return new PureAckMessage (msg, pureAck);
  }

  public PureAckMessage (PureAckMessage pureAckMsg)
  {
    super (pureAckMsg);
  }

  public void setReceptionException (Exception e)
  {
    receptionException = e;
  }

  public Exception getReceptionException ()
  {
    return receptionException;
  }

  public String toString ()
  {
    return MessageUtils.toString (this);
  }
}
