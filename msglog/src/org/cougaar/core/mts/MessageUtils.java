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
 * 14 May 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.mts.acking.Ack;
import org.cougaar.core.mts.acking.AgentID;
import org.cougaar.core.mts.acking.PureAckMessage;
import org.cougaar.core.mts.acking.PureAckAckMessage;


public final class MessageUtils
{
  public static final String MSG_NUM = "MessageNumber";
  public static final String FROM_AGENT = "FromAgent";
  public static final String TO_AGENT = "ToAgent";
  public static final String ACK = "Ack";
  public static final String MSG_TYPE = "MessageType";
  public static final String MSG_TYPE_LOCAL = "MessageTypeLocal";
  public static final String MSG_TYPE_HEARTBEAT = "MessageTypeHeartbeat";
  public static final String MSG_TYPE_PING = "MessageTypePing";
  public static final String MSG_TYPE_TMASK = "MessageTypeTrafficMasking";
  public static final String SEND_TIMEOUT = "SendTimeout";
  public static final String SEND_DEADLINE = "SendDeadline";
  public static final String SRC_MSG_NUM = "SrcMessageNumber";

  // from TrafficMaskingGeneratorAspect.java
  private static final String FAKE = "org.cougaar.message.transport.isfake";


  public static MessageAddress getOriginatorAgent (AttributedMessage msg)
  {
    return msg.getOriginator();
  }

  public static String getOriginatorAgentAsString (AttributedMessage msg)
  {
    return getOriginatorAgent(msg).toString();
  }

  public static MessageAddress getTargetAgent (AttributedMessage msg)
  {
    return msg.getTarget();
  }

  public static String getTargetAgentAsString (AttributedMessage msg)
  {
    return getTargetAgent(msg).toString();
  }

  public static boolean hasMessageNumber (AttributedMessage msg)
  {
    return (msg.getAttribute (MSG_NUM) != null);
  }

  public static int getMessageNumber (AttributedMessage msg)
  {
    //  If the message does not have a message number attribute
    //  we assume it is a local message and return 0.

    Integer num = (Integer) msg.getAttribute (MSG_NUM);
    if (num != null) return num.intValue();
    return 0;  // could perhaps return -1, but have hasMessageNumber() if needed
  }

  public static boolean isAckableMessage (AttributedMessage msg)
  {
    return (getMessageNumber (msg) != 0);
  }

  public static void setFromAgent (AttributedMessage msg, AgentID fromAgent)
  {
    msg.setAttribute (FROM_AGENT, fromAgent);
  }

  public static AgentID getFromAgent (AttributedMessage msg)
  {
    return (AgentID) msg.getAttribute (FROM_AGENT);
  }

  public static String getFromAgentNode (AttributedMessage msg)
  {
    AgentID fromAgent = getFromAgent (msg);
    return (fromAgent != null ? fromAgent.getNodeName() : null);
  }

  public static void setToAgent (AttributedMessage msg, AgentID toAgent)
  {
    msg.setAttribute (TO_AGENT, toAgent);
  }

  public static AgentID getToAgent (AttributedMessage msg)
  {
    return (AgentID) msg.getAttribute (TO_AGENT);
  }

  public static String getToAgentNode (AttributedMessage msg)
  {
    AgentID toAgent = getToAgent (msg);
    return (toAgent != null ? toAgent.getNodeName() : null);
  }

  public static void setAck (AttributedMessage msg, Ack ack)
  {
    msg.setAttribute (ACK, ack);
  }

  public static Ack getAck (AttributedMessage msg)
  {
    return (Ack) msg.getAttribute (ACK);
  }

  static void setMessageType (AttributedMessage msg, String type)
  {
    msg.setAttribute (MSG_TYPE, type);
  }

  public static String getMessageType (AttributedMessage msg)
  {
    return (String) msg.getAttribute (MSG_TYPE);
  }

  public static boolean isRegularMessage (AttributedMessage msg)
  {
    return (getMessageType (msg) == null);    
  }

  public static boolean isLocalMessage (AttributedMessage msg)
  {
    String mtype = getMessageType (msg);
    return (mtype != null && mtype.equals (MSG_TYPE_LOCAL));
  }

  public static void setMessageTypeToLocal (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_LOCAL);
  }

  public static boolean isHeartbeatMessage (AttributedMessage msg)
  {
    String mtype = getMessageType (msg);
    return (mtype != null && mtype.equals (MSG_TYPE_HEARTBEAT));
  }

  public static void setMessageTypeToHeartbeat (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_HEARTBEAT);
  }

  public static boolean isPingMessage (AttributedMessage msg)
  {
    String mtype = getMessageType (msg);
    return (mtype != null && mtype.equals (MSG_TYPE_PING));
  }

  public static void setMessageTypeToPing (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_PING);
  }

  public static boolean isTrafficMaskingMessage (AttributedMessage msg)
  {
/*
    String mtype = getMessageType (msg);
    return (mtype != null && mtype.equals (MSG_TYPE_TMASK));
*/
    Boolean fake = (Boolean) msg.getAttribute (FAKE);
    return (fake != null ? fake.booleanValue() : false);
  }
/*
  public static void setMessageTypeToTrafficMasking (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_TMASK);
  }
*/
  public static boolean isRegularAckMessage (AttributedMessage msg)
  {
    Ack ack = getAck (msg);
    return (ack != null ? ack.isRegularAck() : false);
  }

  public static boolean isPureAckMessage (AttributedMessage msg)
  {
    return (msg.getClass() == PureAckMessage.class);
  }

  public static boolean isPureAckAckMessage (AttributedMessage msg)
  {
    return (msg.getClass() == PureAckAckMessage.class);
  }

  public static boolean isSomePureAckMessage (AttributedMessage msg)
  {
    return (isPureAckMessage(msg) || isPureAckAckMessage(msg));
  }

  public static void setSendTimeout (AttributedMessage msg, int millisecs)
  {
    msg.setAttribute (SEND_TIMEOUT, new Integer (millisecs));
  }

  public static int getSendTimeout (AttributedMessage msg)
  {
    Integer millisecs = (Integer) msg.getAttribute (SEND_TIMEOUT);
    if (millisecs != null) return millisecs.intValue();
    return 0;  // 0 = no timeout
  }

  public static void setSendDeadline (AttributedMessage msg, long deadline)
  {
    msg.setLocalAttribute (SEND_DEADLINE, new Long (deadline));  // NOTE: local attribute
  }

  public static long getSendDeadline (AttributedMessage msg)
  {
    Long deadline = (Long) msg.getAttribute (SEND_DEADLINE);  // NOTE: local attribute
    if (deadline != null) return deadline.longValue();
    return Long.MAX_VALUE;
  }

  public static void setSrcMsgNumber (AttributedMessage msg, int msgNum)
  {
    msg.setAttribute (SRC_MSG_NUM, new Integer (msgNum));
  }

  public static int getSrcMsgNumber (AttributedMessage msg)
  {
    Integer num = (Integer) msg.getAttribute (SRC_MSG_NUM);
    if (num != null) return num.intValue();
    return 0;
  }

  public static String getSequenceID (AttributedMessage msg)
  {
    AgentID fromAgent = getFromAgent (msg);
    AgentID toAgent = getToAgent (msg);
    return AgentID.makeSequenceID (fromAgent, toAgent);    
  }

  public static String toString (AttributedMessage msg)
  {
    if (msg != null)
    {
      String specialType = "";

      if (isPureAckMessage (msg))
      {
        specialType = " (PureAckMsg for Msg " +getSrcMsgNumber(msg)+ ")";
      }
      else if (isPureAckAckMessage (msg))
      {
        specialType = " (PureAckAckMsg for PureAckMsg " +getSrcMsgNumber(msg)+ ")";
      }

      String msgNum = (hasMessageNumber (msg) ? ""+getMessageNumber (msg) : "[]");
      String key = AgentID.makeSequenceID (getFromAgent(msg), getToAgent(msg));
      return "Msg " + msgNum + specialType + " of " + key;
    }
    else return "[null msg]";
  }

  public static String toShortString (AttributedMessage msg)
  {
    if (msg != null)
    {
      String specialType = "";

      if (isPureAckMessage (msg))
      {
        specialType = " (PureAckMsg for Msg " +getSrcMsgNumber(msg)+ ")";
      }
      else if (isPureAckAckMessage (msg))
      {
        specialType = " (PureAckAckMsg for PureAckMsg " +getSrcMsgNumber(msg)+ ")";
      }

      String msgNum = (hasMessageNumber (msg) ? ""+getMessageNumber (msg) : "[]");
      return "Msg " + msgNum + specialType;
    }
    else return "[null msg]";
  }
}
