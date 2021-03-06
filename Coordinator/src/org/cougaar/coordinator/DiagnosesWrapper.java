/*
 * DiagnosesManager.java
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
 * This class wraps a Diagnosis so that the Diagnosis can be relayed to the coordinator
 * in a controlled fashion. The DiagnosisManager regulates if and when a given Diagnosis 
 * is relayed.
 *
 */
public class DiagnosesWrapper 
        implements NotPersistable, Serializable, Relay.Source, Relay.Target, UniqueObject
{
    
    private transient Set targets = Collections.EMPTY_SET;    
    private Diagnosis diagnosis;
    private UID uid = null;
    MessageAddress source = null;
    
    /** Creates a new instance of DiagnosesWrapper */
    public DiagnosesWrapper(Diagnosis d, MessageAddress source, MessageAddress target, UID uid) {
        diagnosis = d;
        setSourceAndTarget(source, target);
        this.setUID(uid);
    }

    //protected boolean local = true;
    //protected Relay.Token owner = null;
    
  /**
   * Helper method to return content as a Diagnosis
   **/
  public Diagnosis getDiagnosis() {
    return diagnosis;
  }
    
  
  // UniqueObject implementation
  public UID getUID() {
    return uid;
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
  
  // Relay.Source implementation -----------------------------------------------
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
    return diagnosis;
  }

  /**
   * @return a factory to convert the content to a Relay Target.
   **/
  //sjf public Relay.TargetFactory getTargetFactory() {
  //sjf   return null;
  //sjf }

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
      return new DiagnosesWrapper(
          (Diagnosis)content, source, null, uid);
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
   * Set the response that was sent from a target. 
   * This implementation does nothing because responses are not needed,
   * used, or permitted.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    // No response expected
    return Relay.NO_CHANGE;
  }


  // Relay.Target implementation -----------------------------------------------
  /**
   * Get the address of the Agent holding the Source copy of
   * this Relay.
   **/
  public MessageAddress getSource() {
    return source;
  }

  /**
   * Get the current response for this target. Null indicates that
   * this target has no response. This implementation never has a
   * response so it always returns null.
   **/
  public Object getResponse() {
    return null;
  }

  /**
   * Send new content to the target. Called on the target side.
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. 
   **/
  public int updateContent(Object content, Relay.Token token) {
      diagnosis = (Diagnosis) content;
      return Relay.CONTENT_CHANGE;
  }

 
  
}
    
    
