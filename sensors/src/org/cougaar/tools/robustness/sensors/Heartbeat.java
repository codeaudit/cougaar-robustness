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
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * A Heartbeat Relay, the Blackboard object that is generated 
 * by the HeartbeatServerPlugin in response to a HbReq from the
 * HeartbeatRequesterPlugin.
 **/
public class Heartbeat implements Relay.Source, Relay.Target, XMLizable
{
  private UID uid;
  private MessageAddress source;
  private MessageAddress target;
  private Object content;
  private Object response;
  private transient Set _targets;

  /**
   * @param uid UID of this Heartbeat object
   * @param source MessageAddress of sending agent
   * @param target MessageAddress of target agent
   * @param content HeartbeatContent to be sent with Heartbeat
   * @param response initial response
   */
  public Heartbeat(UID uid,
              MessageAddress source,
              MessageAddress target,      
              Object content,
              Object response) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.content = content;
    this.response = response;

    this._targets = 
     ((target != null) ?
      Collections.singleton(target) :
      Collections.EMPTY_SET);
  }

  // Unique Object implementation

  /**
  * UIDs are not permitted to be set except by constructor, so this method throws an exception.
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
  * Get the targets for this Relay.
  **/
  public Set getTargets() {
    return _targets;
  }

  /**
  * Get the content from this Relay.  
  * A HeartbeatContent object is returned as an Object.
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
      return new Heartbeat(uid, source, null, content, null);
    }

    private Object readResolve() {
      return INSTANCE;
    }
  };

  /**
  * Get a factory for creating the target. 
  **/
  public TargetFactory getTargetFactory() {
    return SimpleRelayFactory.INSTANCE;
  }

  /**
  * Update the source with the new response.
  **/
  public int updateResponse(MessageAddress t, Object response) {
    // assert response != null
    if (!(response.equals(this.response))) {
      this.response = response;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Relay.Target implementation

  /**
  * Get the address of the Agent holding the Source copy of this Relay. 
  */
  public MessageAddress getSource() {
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
    } else if (!(o instanceof Heartbeat)) {
      return false;
    } else {
      UID u = ((Heartbeat) o).getUID();
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
    return "(Heartbeat: " + uid + ", " + source + ", " + target + ", " + content + ", " + response + ")";
  }

}
