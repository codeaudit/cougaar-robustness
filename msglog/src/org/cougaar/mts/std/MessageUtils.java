/*
 * <copyright>
 *  Copyright 2002-2004 Object Services and Consulting, Inc. (OBJS),
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
 * 01 May 2003: Added removeMessageNumber in support of UC1. (102B)
 * 12 Mar 2003: Moved some constants to org.cougaar.core.mts.Constants
 * 14 May 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.std.acking.Ack;
import org.cougaar.mts.std.acking.PureAckMessage;
import org.cougaar.mts.std.acking.PureAckAckMessage;

/**
 * Collection of utility methods for messages
 */

public final class MessageUtils
{
  //  Message attributes

  //102B public static final String MSG_TYPE =              "MessageType";
  public static final String MSG_NUM =               "MessageNumber";
  public static final String FROM_AGENT =            "MessageFromAgent";
  public static final String TO_AGENT =              "MessageToAgent";
  public static final String ACK =                   "MessageAck";
  //102B public static final String SEND_TIMEOUT =          "MessageSendTimeout";
  public static final String SEND_DEADLINE =         "MessageSendDeadline";
  public static final String SEND_PROTOCOL_LINK =    "MessageSendProtocolLink";
  public static final String SRC_MSG_NUM =           "MessageSrcMsgNumber";
  public static final String MSG_SIZE =              "MessageSize";
  public static final String LOCAL_MSG =             "MessageLocal";   

  //  Message attribute values

  public static final String MSG_TYPE_REGULAR =      "MessageTypeRegular";
  //1045B public static final String MSG_TYPE_LOCAL =        "MessageTypeLocal";
  //102B public static final String MSG_TYPE_HEARTBEAT =    "MessageTypeHeartbeat";
  //102B public static final String MSG_TYPE_PING =         "MessageTypePing";
  public static final String MSG_TYPE_TRAFFIC_MASK = "MessageTypeTrafficMasking";
  public static final String MSG_TYPE_PURE_ACK =     "MessageTypePureAck";
  public static final String MSG_TYPE_PURE_ACK_ACK = "MessageTypePureAckAck";

  // from TrafficMaskingGeneratorAspect.java
  private static final String FAKE = "org.cougaar.message.transport.isfake";

  private static final String validMessageTypes[] =
  {
    MSG_TYPE_REGULAR,
    //1045B MSG_TYPE_LOCAL,
    Constants.MSG_TYPE_HEARTBEAT,
    Constants.MSG_TYPE_PING,
    MSG_TYPE_TRAFFIC_MASK,
    MSG_TYPE_PURE_ACK,
    MSG_TYPE_PURE_ACK_ACK
  };

  static void setMessageType (AttributedMessage msg, String type)
  {
    msg.setAttribute (Constants.MSG_TYPE, type);
  }

  public static String getMessageType (AttributedMessage msg)
  {
    return (String) msg.getAttribute (Constants.MSG_TYPE);
  }

  public static boolean hasMessageType (AttributedMessage msg)
  {
    return (msg.getAttribute (Constants.MSG_TYPE) != null);
  }

  public static String[] getValidMessageTypes ()
  {
    return validMessageTypes;
  }

  public static String getMessageTypeLetter (AttributedMessage msg)
  {
    if (isRegularMessage(msg))        return "n";  // normal
    //1045B if (isLocalMessage(msg))          return "l";
    if (isHeartbeatMessage(msg))      return "h";
    if (isPingMessage(msg))           return "p";
    if (isTrafficMaskingMessage(msg)) return "t";
    if (isPureAckMessage(msg))        return "a";
    if (isPureAckAckMessage(msg))     return "k";
                                      return "?";  // unknown
  }

  public static String getMessageTypeString (AttributedMessage msg)
  {
    if (isRegularMessage(msg))        return "Regular";
    //1045B if (isLocalMessage(msg))          return "Local";
    if (isHeartbeatMessage(msg))      return "Heartbeat";
    if (isPingMessage(msg))           return "Ping";
    if (isTrafficMaskingMessage(msg)) return "TrafficMasking";
    if (isPureAckMessage(msg))        return "PureAck";
    if (isPureAckAckMessage(msg))     return "PureAckAck";
                                      return "?";  // unknown
  }

  public static boolean isRegularMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (MSG_TYPE_REGULAR));
  }

  public static void setMessageTypeToRegular (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_REGULAR);
  }

/* //1045B 
  public static boolean isLocalMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (MSG_TYPE_LOCAL));
  }

  public static void setMessageTypeToLocal (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_LOCAL);
  }
*/ //1045B 

  //1045B 
  public static boolean isLocalMessage (AttributedMessage msg)
  {
    Boolean islocal = (Boolean)msg.getAttribute(LOCAL_MSG);
    return (islocal != null && islocal.equals(Boolean.TRUE));
  }

  //1045B 
  public static void setLocalMessage (AttributedMessage msg, boolean isLocal)
  {
    msg.setAttribute(LOCAL_MSG, new Boolean(isLocal));
  }

  public static boolean isHeartbeatMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (Constants.MSG_TYPE_HEARTBEAT));
  }

  public static void setMessageTypeToHeartbeat (AttributedMessage msg)
  {
    setMessageType (msg, Constants.MSG_TYPE_HEARTBEAT);
  }

  public static boolean isPingMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (Constants.MSG_TYPE_PING));
  }

  public static void setMessageTypeToPing (AttributedMessage msg)
  {
    setMessageType (msg, Constants.MSG_TYPE_PING);
  }

  public static boolean isTrafficMaskingMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    if (type != null && type.equals (MSG_TYPE_TRAFFIC_MASK)) return true;
    return (msg.getAttribute (FAKE) != null);
  }

  public static void setMessageTypeToTrafficMasking (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_TRAFFIC_MASK);
  }

  public static void setMessageTypeToPureAck (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_PURE_ACK);
  }

  public static boolean isPureAckMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (MSG_TYPE_PURE_ACK));
  }

  public static void setMessageTypeToPureAckAck (AttributedMessage msg)
  {
    setMessageType (msg, MSG_TYPE_PURE_ACK_ACK);
  }

  public static boolean isPureAckAckMessage (AttributedMessage msg)
  {
    String type = getMessageType (msg);
    return (type != null && type.equals (MSG_TYPE_PURE_ACK_ACK));
  }

  public static boolean isSomePureAckMessage (AttributedMessage msg)
  {
    return (isPureAckMessage(msg) || isPureAckAckMessage(msg));
  }

  public static boolean hasMessageNumber (AttributedMessage msg)
  {
    return (msg.getAttribute (MSG_NUM) != null);
  }

  public static int getMessageNumber (AttributedMessage msg)
  {
    //  If the message does not have a message number we return 0

    Integer num = (Integer) msg.getAttribute (MSG_NUM);
    if (num != null) return num.intValue();
    return 0;  // have method hasMessageNumber() if needed
  }

  //102B
  public static void removeMessageNumber (AttributedMessage msg)
  {
    msg.removeAttribute(MSG_NUM);
  }

  public static boolean isAckableMessage (AttributedMessage msg)
  {
    if (msg == null) return false;
    if (isSomePureAckMessage (msg)) return false;
    if (!hasMessageNumber (msg)) return false;
    if (getMessageNumber (msg) == 0) return false;
    return true;
  }

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

  public static String getAckingSequenceID (AttributedMessage msg)
  {
    AgentID fromAgent = getFromAgent (msg);
    AgentID toAgent = getToAgent (msg);
    return AgentID.makeAckingSequenceID (fromAgent, toAgent);    
  }

  public static void setAck (AttributedMessage msg, Ack ack)
  {
    msg.setAttribute (ACK, ack);
  }

  public static Ack getAck (AttributedMessage msg)
  {
    return (Ack) msg.getAttribute (ACK);
  }

  public static int getSendTry (AttributedMessage msg)
  {
    Ack ack = getAck (msg);
    return (ack != null ? ack.getSendTry() : -1); 
  }

  public static int getSendCount (AttributedMessage msg)
  {
    Ack ack = getAck (msg);
    return (ack != null ? ack.getSendCount() : 0);
  }

  public static void setMessageSize (AttributedMessage msg, int size)
  {
    msg.setLocalAttribute (MSG_SIZE, new Integer (size));  // note LOCAL attribute
  }

  public static int getMessageSize (AttributedMessage msg)
  {
    Integer size = (Integer) msg.getAttribute (MSG_SIZE);  // note LOCAL attribute
    return (size != null ? size.intValue() : -1);
  }

  public static void setSendTimeout (AttributedMessage msg, int millisecs)
  {
    msg.setAttribute (Constants.SEND_TIMEOUT, new Integer (millisecs));
  }

  public static int getSendTimeout (AttributedMessage msg)
  {
    Integer millisecs = (Integer) msg.getAttribute (Constants.SEND_TIMEOUT);
    if (millisecs != null) return millisecs.intValue();
    return -1;  // no timeout
  }

  public static boolean haveSendTimeout (AttributedMessage msg)
  {
    return (msg.getAttribute (Constants.SEND_TIMEOUT) != null);
  }

  public static void setSendDeadline (AttributedMessage msg, long deadline)
  {
    msg.setLocalAttribute (SEND_DEADLINE, new Long (deadline));  // note LOCAL attribute
  }

  public static long getSendDeadline (AttributedMessage msg)
  {
    Long deadline = (Long) msg.getAttribute (SEND_DEADLINE);     // note LOCAL attribute
    if (deadline != null) return deadline.longValue();
    return Long.MAX_VALUE;  // no deadline
  }

  public static boolean haveSendDeadline (AttributedMessage msg)
  {
    return (msg.getAttribute (SEND_DEADLINE) != null);
  }

  public static void setSendProtocolLink (AttributedMessage msg, String protocolLink)
  {
    msg.setAttribute (SEND_PROTOCOL_LINK, protocolLink);
  }

  public static String getSendProtocolLink (AttributedMessage msg)
  {
    return (String) msg.getAttribute (SEND_PROTOCOL_LINK);
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

  public static boolean haveSrcMsgNumber (AttributedMessage msg)
  {
    return (msg.getAttribute (SRC_MSG_NUM) != null);
  }

  public static boolean isNewMessage (AttributedMessage msg)
  {
    if (hasMessageNumber (msg)) return false;
    if (getFromAgent(msg) != null || getToAgent(msg) != null) return false;
    return true;
  }

  public static String toShortString (AttributedMessage msg)
  {
    if (msg == null) return "[null msg]";

    String specialType = "";

    if (isLocalMessage (msg))
    {
      specialType = " (LocalMsg)";
    }
    else if (isPureAckMessage (msg))
    {
      specialType = " (PureAckMsg for Msg " +getSrcMsgNumber(msg)+ ")";
    }
    else if (isPureAckAckMessage (msg))
    {
      specialType = " (PureAckAckMsg for PureAckMsg " +getSrcMsgNumber(msg)+ ")";
    }
    else if (isHeartbeatMessage (msg))
    {
      specialType = " (HeartbeatMsg)";
    }
    else if (isPingMessage (msg))
    {
      specialType = " (PingMsg)";
    }
    else if (isTrafficMaskingMessage (msg))
    {
      specialType = " (TrafficMaskingMsg)";
    }

    String msgNum = (hasMessageNumber(msg) ? ""+getMessageNumber(msg) : "[]");
    return "Msg " + msgNum + specialType;
  }

  public static String toString (AttributedMessage msg)
  {
    String hdr = toShortString (msg);
    if (msg == null) return hdr;
    String key = AgentID.makeAckingSequenceID (getFromAgent(msg), getToAgent(msg));
    String bonus = (key.equals("null::null")? (" ("+msg.getOriginator()+" to "+msg.getTarget()+")") : "");
    return hdr + " of " + key + bonus;
  }

  public static String toShortSequenceID (AttributedMessage msg)
  {
    if (msg == null) return "[null msg]";
    return AgentID.makeShortSequenceID (getFromAgent(msg), getToAgent(msg));
  }
}
