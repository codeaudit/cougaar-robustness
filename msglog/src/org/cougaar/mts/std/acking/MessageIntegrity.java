/*
 * <copyright>
 *  Copyright 2002,2004 Object Services and Consulting, Inc. (OBJS),
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
 * 20 Aug 2002: Created. (OBJS)
 */

package org.cougaar.mts.std.acking;

import java.util.Enumeration;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.std.AgentID;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.std.MessageUtils;

class MessageIntegrity
{
  public static boolean areMessageAttributesOK (AttributedMessage msg, LoggingService log)
  {
    //  Go through all the message attributes and check that they are present if they 
    //  need to be (as far as we can tell) and perhaps that they have a valid value.

    String msgString = MessageUtils.toString (msg);

    String type = MessageUtils.getMessageType (msg);

    if (type == null)
    {
      log.error ("MessageIntegrity: Missing message type attribute: " +msgString);
      return false;
    }

    int i;
    String validTypes[] = MessageUtils.getValidMessageTypes();
    for (i=0; i<validTypes.length; i++) if (type.equals (validTypes[i])) break;

    if (i == validTypes.length)
    {
      log.error ("MessageIntegrity: Message not a valid type ("+type+"): " +msgString);
      return false;
    }

    if (!MessageUtils.hasMessageNumber (msg))
    {
      log.error ("MessageIntegrity: Missing message number attribute: " +msgString);
      return false;
    }

    Ack ack = MessageUtils.getAck (msg);

    if (ack == null)
    {
      log.error ("MessageIntegrity: Message is missing embedded ack: " +msgString);
      return false;
    }
    
    int msgNum = MessageUtils.getMessageNumber (msg);

    if (ack.isAck() && msgNum < 0)
    {
      if (!MessageUtils.isPingMessage (msg))  // pings are an exception
      {
        log.error ("MessageIntegrity: Invalid msg number for type: " +msgString);
        return false;
      }
    }

    if (ack.isPureAck() && msgNum >= 0)
    {
      log.error ("MessageIntegrity: Invalid pure ack msg number: " +msgString);
      return false;
    }

    if (ack.isPureAckAck() && msgNum >= 0)
    {
      log.error ("MessageIntegrity: Invalid pure ack-ack msg number: " +msgString);
      return false;
    }

    if (ack.isSomePureAck() && !MessageUtils.haveSrcMsgNumber(msg))
    {
      log.error ("MessageIntegrity: Missing src msg number attribute: " +msgString);
      return false;
    }

    if (ack.getSendCount() < 1)
    {
      int sendCount = ack.getSendCount();
      log.error ("MessageIntegrity: Msg has bad sendCount (" +sendCount+ ")" +msgString);
      return false;
    }

    if (ack.isAck() && ack.getResendMultiplier() < 1)
    {
      log.error ("MessageIntegrity: Msg has bad resendMultiplier" +msgString);
      return false;
    }

    try
    {
      if (ack.getSpecificAcks() != null)
      {
        for (Enumeration a=ack.getSpecificAcks().elements(); a.hasMoreElements(); )
        {
          NumberList.checkListValidity ((AckList) a.nextElement());
        }
      }

      if (ack.getLatestAcks() != null)
      {
        for (Enumeration a=ack.getLatestAcks().elements(); a.hasMoreElements(); )
        {
          NumberList.checkListValidity ((AckList) a.nextElement());
        }
      }
    }
    catch (Exception e)
    {
      log.error ("MessageIntegrity: Msg contains invalid acks: " +msgString);
      return false;
    }

    AgentID fromAgent = MessageUtils.getFromAgent (msg);

    if (fromAgent == null)
    {
      log.error ("MessageIntegrity: Missing from agent attribute: " +msgString);
      return false;
    }

    String fromNode = fromAgent.getNodeName();

    if (fromNode == null || fromNode.equals(""))
    {
      log.error ("MessageIntegrity: Invalid from node in from agent: " +msgString);
      return false;
    }
    
    AgentID toAgent = MessageUtils.getToAgent (msg);

    if (toAgent == null)
    {
      log.error ("MessageIntegrity: Missing to agent attribute: " +msgString);
      return false;
    }

    String toNode = toAgent.getNodeName();

    if (toNode == null || toNode.equals(""))
    {
      log.error ("MessageIntegrity: Invalid to node in to agent: " +msgString);
      return false;
    }

    return true;  // msg attributes ok (as far as we know, which is not everything)
  }
}
