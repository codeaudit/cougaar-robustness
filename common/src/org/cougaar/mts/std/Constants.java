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
 * 23 Feb 2004: Port to 11.0
 * 14 May 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

/**
 * Collection of commonly used constants
 */

public final class Constants
{
  //  Message attributes
    public static final String MSG_TYPE =              "MessageType";
    public static final String SEND_TIMEOUT =          "MessageSendTimeout";
    public static final String MSG_TYPE_HEARTBEAT =    "MessageTypeHeartbeat";
    public static final String MSG_TYPE_PING =         "MessageTypePing";
    public static final String MSG_TYPE_PURE_ACK =     "MessageTypePureAck";
    public static final String MSG_TYPE_PURE_ACK_ACK = "MessageTypePureAckAck";
    public static final String MSG_TYPE_TRAFFIC_MASK = "MessageTypeTrafficMasking";

    public static final String AUDIT_ATTRIBUTE_NUMSEQ = "AuditSeqNum"; 
    public static final String AUDIT_ATTRIBUTE_FROM_INCARNATION = "AuditSenderIncarNum";
    public static final String AUDIT_ATTRIBUTE_FROM_NODE = "AuditSenderNode";

}


