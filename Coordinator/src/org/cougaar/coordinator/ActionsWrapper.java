/*
 * ActionsWrapper.java
 *
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */


package org.cougaar.coordinator;

import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.Set;
import java.util.Collections;

/**
 *
 * @author  Administrator
 *
 * This class wraps a Action so that the Action can be relayed to the coordinator
 * in a controlled fashion. The ActionRelayManager regulates if and when a given Action 
 * is relayed.
 *
 */
public class ActionsWrapper implements NotPersistable, Serializable, Relay.Source, Relay.Target, UniqueObject
{
    
    //Transients will 
    private transient Set targets = Collections.EMPTY_SET;     
    private transient boolean isLocal = false; 
    private Action action;
    private transient Set newPermittedValues;
    private UID uid = null;
    MessageAddress source = null;
    private transient Logger log;
    
    /** Creates a new instance of ActionsWrapper */
    public ActionsWrapper(Action a, MessageAddress source, MessageAddress target, UID uid) {
        action = a;
        setSourceAndTarget(source, target);
        this.setUID(uid);
        log = Logging.getLogger(getClass());    
    }

    //protected boolean local = true;
    //protected Relay.Token owner = null;
    
    
  
  // UniqueObject implementation
  public UID getUID() {
    return uid;
  }

  /**
   * Helper method to return content as an Action
   **/
  public Action getAction() {
    return action;
  }
  
  // Initialization methods

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID");
    this.uid = uid;
  }

  /**
   * Set the message address of the source & target. This implementation
   * presumes that there is one target. Set these values if this object needs to be
   * relayed.
   * @param source the address of the source agent.
   * @param target the address of the target agent.
   **/
  public void setSourceAndTarget(MessageAddress source, MessageAddress target) {
    targets = Collections.singleton(target);
    this.source = source;
  }
  
  // Relay.Source implementation -------------------------------------------
  /**
   * Get all the addresses of the target agents to which this Relay
   * should be sent. For this implementation this is always a
   * singleton set contain just one target.
   **/
  public Set getTargets() {
    return targets;
  }

  /**
   * Get an object representing the value of this Relay suitable
   * for transmission. This implementation uses itself to represent
   * its Content.
   **/
  public Object getContent() {
    return action;
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
    public Relay.Target create(UID uid, 
			       MessageAddress source, 
			       Object content,
			       Token token) {
	// copy the content (the Action)
	Action a = null;
	try {
	    Class c = content.getClass();
	    Class [] classes = new Class[1];
	    classes[0] = c;
	    Object[] args = new Object[1];
	    args[0] = (Action)content;
	    Object o = c.getConstructor(classes).newInstance(args);
	    a = (Action)o;
	} catch (Exception e) {
	    Logging.getLogger(getClass()).error("Error copying Action", e);
	}
	ActionsWrapper aw = 
	    new ActionsWrapper(a, source, null, uid);
	a.setWrapper(aw);
	return aw;
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
   * Only update the source side if the permittedValues have changed. 
   **/
  public int updateResponse(MessageAddress t, Object response) {
      if (log.isDebugEnabled())log.debug("updateResponse: r="+response+",a="+action);
      if (!action.getPermittedValues().equals((Set)response)) {
	  newPermittedValues = (Set)response;
	  return Relay.RESPONSE_CHANGE;	  
      }
      return Relay.NO_CHANGE;
  }

  public Set getNewPermittedValues () {
      return newPermittedValues;
  }

  // call after processing by plugin on the source side
  public void clearNewPermittedValues () {
      newPermittedValues = null;
  }

  // Relay.Target implementation -----------------------------------------
  /**
   * Get the address of the Agent holding the Source copy of
   * this Relay.
   **/
  public MessageAddress getSource() {
    return source;
  }

  /**
   * Only the permitted values make the return trip. 
   **/
  public Object getResponse() {
      return action.getPermittedValues();
  }

  /**
   * Update target with new content. 
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. 
   **/
  public int updateContent(Object content, Relay.Token token) {
      Action a = (Action) content;
      
      action.lastAction = a.getValue();
      action.prevAction = a.getPreviousValue();
      action.valuesOffered = a.getValuesOffered();
          
      return Relay.CONTENT_CHANGE;
  }

    public String toString() {
	return "<"+getClass().getName()+'@'+Integer.toHexString(hashCode())+":"+((Object)action).toString()+":"+source+":"+targets+":"+uid+">";
    }

    /**
     * Returns a verbose pretty-printed representation for an Action.
     */
    public String dump() {
	return "\n" +
            "<"+getClass().getName()+'@'+Integer.toHexString(hashCode()) + "\n" +
            "   action = " + action + "\n" +
	    "   newPermittedValues = " + newPermittedValues + "\n" +
	    "   isLocal = " + isLocal + "\n" +
	    "   uid = " + uid + "\n" +
	    "   source = " + source + "\n" +
	    "   targets = " + targets + "\n" +
	    "   content = " + getContent() + "\n" +
	    "   response = " + getResponse() + ">";
    }
  
}
    
    
