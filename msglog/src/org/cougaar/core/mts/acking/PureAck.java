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
 * 23 Apr  2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import org.cougaar.core.mts.*;


//  Child of Ack

public class PureAck extends Ack
{
  private transient long ackSendableTime;
  private transient long sendDeadline;

  public PureAck ()
  {
    super (null);  // msg gets set by PureAckMessage
  }

  public synchronized void setAckSendableTime (long time)
  {
    ackSendableTime = time;
  }

  public synchronized long getAckSendableTime ()
  {
    return ackSendableTime;
  }

  public synchronized void setSendDeadline (long time)
  {
    sendDeadline = time;
  }

  public synchronized long getSendDeadline ()
  {
    return sendDeadline;
  }

  public boolean stillAckingSrcMsg ()
  {
    return AckList.find (getLatestAcks(), MessageUtils.getSrcMsgNumber(getMsg()));
  }
}
