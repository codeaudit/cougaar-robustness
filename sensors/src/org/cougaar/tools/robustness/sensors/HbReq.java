/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * Copyright 2002 Object Services and Consulting, Inc.
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.tools.robustness.sensors;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.mts.MessageUtils;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.persist.NotPersistable;

/**
 * A Heartbeat Request Relay, the Blackboard object that is passed
 * between HeartbeatRequesterPlugin to HeartbeatServerPlugin.
 **/
public class HbReq implements Relay.Source, Relay.Target, XMLizable, NotPersistable
{
  private UID uid;
  private MessageAddress source;
  private transient Set targets;
  private Object content;
  private Object response;
  private boolean heartbeat = false;

  /**
   * @param uid UID of this HbReq object
   * @param source MessageAddress of sending agent
   * @param targets Set of MessageAddresses of target agents
   * @param content HbReqContent to be sent with HbReq
   * @param response initial response
   */
  public HbReq(UID uid,
               MessageAddress source,
               Set targets,      
               Object content,
               Object response) {
    this.uid = uid;
    this.source = source;
    this.content = content;
    this.response = response;
    if (targets == null) {
      this.targets = Collections.EMPTY_SET;
    } else {
      // add attributes to targets
      this.targets = new HashSet();
      Iterator iter = targets.iterator();
      while (iter.hasNext()) {
        MessageAddress addr = (MessageAddress)iter.next();
        MessageAttributes attrs = addr.getQosAttributes();
        try {
          if (attrs == null) {
            Class[] classes = new Class[2];
            classes[0] = MessageAttributes.class;
            classes[1] = String.class;
            Constructor x = addr.getClass().getConstructor(classes);
            attrs = new SimpleMessageAttributes();
            String addrStr = addr.getAddress();
            Object[] args = new Object[2];
            args[0] = attrs;
            args[1] = addrStr;
            addr = (MessageAddress)x.newInstance(args);
          }  
          // a ping is acked, but not sequenced
          attrs.setAttribute(MessageUtils.MSG_TYPE, MessageUtils.MSG_TYPE_PING);
          if (content instanceof HbReqContent) {
            long timeout = ((HbReqContent)content).getReqTimeout();
            if (timeout > 0) {
              attrs.setAttribute(MessageUtils.SEND_TIMEOUT, new Integer((int)timeout));
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        this.targets.add(addr);
      }
    }
  }

  // Unique Object implementation

  /**
  * UIDs are not permitted to be set except by constructor, so this
  * method throws an exception.
  *
  * @throws java.lang.RuntimeException
  **/
  public void setUID(UID uid) {
    throw new RuntimeException("Attempt to change UID");
  }

  /**
  * Get the UID for this object.
  *
  * @return org.cougaar.core.util.UID
  **/
  public UID getUID() {
    return uid;
  }

  // Relay.Source implementation

  /**
  * Get the addresses of the target agents to which this Relay should be sent.
  **/
  public Set getTargets() {
    return ((targets == null) ? Collections.EMPTY_SET : targets);
  }

  /**
  * Get an object representing the value of this Relay suitable for transmission. 
  * A HbReqContent object is returned as an Object.
  **/
  public Object getContent() {
    return content;
  }

  private static final class SimpleRelayFactory
  implements TargetFactory, java.io.Serializable {

    public static final SimpleRelayFactory INSTANCE = 
      new SimpleRelayFactory();

    private SimpleRelayFactory() {}

    /**
    * Convert the given content and related information into a Target
    * that will be published on the target's blackboard. 
    **/
    public Relay.Target create(
        UID uid, 
        MessageAddress source, 
        Object content,
        Token token) {
      return new HbReq(
          uid, source, null, content, null);
    }

    private Object readResolve() {
      return INSTANCE;
    }
  };

  /**
  * Get a factory for creating the target. 
  */
  public TargetFactory getTargetFactory() {
    return SimpleRelayFactory.INSTANCE;
  }

  /**
  * Update the source with the new response.
  */
  public int updateResponse(MessageAddress t, Object response) {
    this.response = response;
    return Relay.RESPONSE_CHANGE;
  }
/*
    // assert response != null
    if (!(response.equals(this.response))) {
      this.response = response;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }
*/

  // Relay.Target implementation

  /**
  * Get the address of the Agent holding the Source copy of this Relay. 
  */  
  public MessageAddress getSource() {
    // Because message attributes are transient, I can't just put them
    // on the source address on the source side, so I put them on when
    // this method is called on the target side.
    MessageAddress addr = source;
    MessageAttributes attrs = addr.getQosAttributes();
    if (attrs == null) {
      try {
        Class[] classes = new Class[2];
        classes[0] = MessageAttributes.class;
        classes[1] = String.class;
        Constructor x = addr.getClass().getConstructor(classes);
        attrs = new SimpleMessageAttributes();
        String addrStr = addr.getAddress();
        Object[] args = new Object[2];
        args[0] = attrs;
        args[1] = addrStr;
        addr = (MessageAddress)x.newInstance(args);
        if (content instanceof PingContent) {
          long timeout = ((PingContent)content).getTimeout();
          if (timeout > 0)
            attrs.setAttribute(MessageUtils.SEND_TIMEOUT, new Integer((int)timeout));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (attrs != null) {
      if (heartbeat == true) {
        // heartbeats are not sequenced, acked or resent
        attrs.setAttribute(MessageUtils.MSG_TYPE, MessageUtils.MSG_TYPE_HEARTBEAT);
      } else {
        // pings are acked and resent, but not sequenced
        attrs.setAttribute(MessageUtils.MSG_TYPE, MessageUtils.MSG_TYPE_PING);
      }
    }
    this.source = addr;
    return source;
  }

  /**
  * Get the current Response for this target. 
  */
  public Object getResponse() {
    return response;
  }

  /**
  * Update the target with the new content.
  */
  public int updateContent(Object content, Token token) {
    // assert content != null
    if (!(content.equals(this.content))) {
      this.content = content;
      return CONTENT_CHANGE;
    }
    return NO_CHANGE;
  }

  /**
  * XMLizable method for UI, other clients
  */
  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
  }

  /**
  * Returns true if this object's UID equals the argument's UID.
  */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof HbReq)) {
      return false;
    } else {
      UID u = ((HbReq) o).getUID();
      return uid.equals(u);
    }
  }

  /**
  * Returns a hash code based on this object's UID.
  */
  public int hashCode() {
    return uid.hashCode();
  }

  /**
  * Returns a String represention for this object.
  */
  public String toString() {
    return "\n" +
           "(HbReq:\n" +
           "   uid = " + uid + "\n" +
           "   source = " + source + "\n" +
           "   targets = " + targets + "\n" +
           "   content = " + content + "\n" +
           "   response = " + response + ")";
  }

  /** 
  * Convert this into a Heartbeat.
  */
  public void convertToHeartbeat() {
    heartbeat = true;
  }

}

