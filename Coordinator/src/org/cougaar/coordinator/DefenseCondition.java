/*
 * DefenseCondition.java
 *
 * Created on March 19, 2003, 4:07 PM
 * 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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
 *
 * Modified on 08/01/2003 - to support relay API
 *
 */

package org.cougaar.coordinator;

import org.cougaar.coordinator.techspec.AssetType;

import org.cougaar.core.adaptivity.OMCBase;
import org.cougaar.core.adaptivity.OMCRangeList;

import org.cougaar.core.util.UID;
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

/**
 * Generic superclass to act as supertype for all Defense Condition objects.
 *@deprecated
 *
 */
public abstract class DefenseCondition extends OMCBase 
       implements NotPersistable, Serializable, Relay.Source, Relay.Target, UniqueObject

{

    // FIXME this shouldn't be transient!
    private transient Set targets = Collections.EMPTY_SET;

    protected UID uid;
    protected MessageAddress source = null;
    protected boolean local = true;
    protected Relay.Token owner = null;
    
    protected String assetType;
    protected String assetName;
    protected String defenseName;
    protected long timestamp;
    
    protected String expandedName;
    
    /**
     * Do not use. Only use the constructors of the subclasses.
     */
    public DefenseCondition(String assetType, String assetName, String defenseName, OMCRangeList allowedValues) {
        super(defenseName+":"+assetName, allowedValues);
        this.assetType = assetType;
        this.assetName = assetName;
        this.defenseName = defenseName;
        this.timestamp = System.currentTimeMillis();
        if ((assetType == null) && (assetName == null))
            this.expandedName = "NULL";
//        else
//2004            this.expandedName = AssetID..generateExpandedAssetName(assetName, AssetType.findAssetType(assetType));
    }
   
    /**
     * Do not use. Only use the constructors of the subclasses.
     */
    public DefenseCondition(String assetType, String assetName, String defenseName, OMCRangeList allowedValues, java.lang.Comparable initialValue) {
        super(defenseName+":"+assetName, allowedValues, initialValue);
        this.assetType = assetType;
        this.assetName = assetName;
        this.defenseName = defenseName;
        this.timestamp = System.currentTimeMillis();

        if ((assetType == null) && (assetName == null))
            this.expandedName = "NULL";
//        else
//            this.expandedName = AssetName.generateExpandedAssetName(assetName, AssetType.findAssetType(assetType));
    }    

    
    /**
     * @return the asset type related to this defense condition
     */
    public String getAssetType() { return assetType; }
    
    /**
     * @return the asset related to this defense condition
     */
    public String getAsset() { return assetName; }
    
    /**
     * @return the defense name that generated this defense condition
     */
    public String getDefenseName() { return defenseName; }
    
    /**
     * @return timestamp - "type:long"
     */    
    public long getTimestamp() { return timestamp; }
 
    /**
     * @return expanded name - "type:String"
     */
    public String getExpandedName() { return expandedName; }

    /**
     * Set the value of this defense condition
     */
    protected void setValue(String newValue) {
        super.setValue(newValue);
        this.timestamp = System.currentTimeMillis();
    }
    
 
  // UniqueObject implementation
  public UID getUID() {
    return uid;
  }

  // Initialization methods
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

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID");
    this.uid = uid;
  }

  // Relay.Source implementation
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
    return this;
  }

  /**
   * @return a factory to convert the content to a Relay Target.
   **/
  public Relay.TargetFactory getTargetFactory() {
    return DefenseConditionFactory.INSTANCE;
  }

  /**
   * Set the response that was sent from a target. 
   * This implemenation does nothing because responses are not needed
   * or used.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    // No response expected
    return Relay.NO_CHANGE;
  }

  /**
   * This factory creates a new DefenseCondition.
   **/
  private static class DefenseConditionFactory 
    implements Relay.TargetFactory, java.io.Serializable {
      public static final DefenseConditionFactory INSTANCE = 
        new DefenseConditionFactory();
      private DefenseConditionFactory() { }
      public Relay.Target create(
          UID uid, MessageAddress source, Object content, Relay.Token owner) {
        //The content is the relayed object, just cast back to a DefenseCondition
        DefenseCondition dc = (DefenseCondition) content;
        dc.setLocal(false);

        Logger logger = Logging.getLogger(getClass()); 
        logger.debug("*** Created DefenseCondition Relay of type: "+dc.getClass().getName() + "*********************");
        
        return dc;
      }
      private Object readResolve() { return INSTANCE; }
    }


  // Relay.Target implementation
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
   * Update with new content. 
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. This implementation returns true only
   * if the new value differs from the current value.
   **/
  public int updateContent(Object content, Relay.Token token) {
    /* removed as unnecessary sjf 8/29/2003
    if (token != owner) {
        Logger logger = Logging.getLogger(getClass());
        if (logger.isInfoEnabled()) {
          logger.info(
            "Ignoring \"Not owner\" bug in \"updateContent()\","+
            " possibly a rehydration bug (token="+
            token+", owner="+owner+")");
        }
    }
    */
    DefenseCondition newDC = (DefenseCondition) content;
    if (getValue().compareTo(newDC.getValue()) != 0) {
        setValue(newDC.getValue());
        return Relay.CONTENT_CHANGE;
    }
    return Relay.NO_CHANGE;
  }
  
  
  /** 
   * @param local New value of local. TRUE if this is the local instance, FALSE if
   * this instance is the relay.
   */
  public void setLocal(boolean local) {
      this.local = local;
  }
  
  /** 
   * @return Value of local.
   */
  public boolean isLocal() {
      return local;
  }
  
  public boolean compareSignature(String expandedName, String defenseName) {
      Logger logger = Logging.getLogger(getClass());
      if (logger.isDebugEnabled()) logger.debug("DefOpMode/compareSignature");
      return ((this.getDefenseName().equals(defenseName)) &&
              (this.getExpandedName().equals(expandedName)));
  }
                  
  public boolean compareSignature(String type, String id, String defenseName) {
  return ((this.assetType.equals(type)) &&
          (this.assetName.equals(id)) &&
          (this.defenseName.equals(defenseName)));
  }
  
  public boolean compareSignature(UID uid) {
      return (uid.equals(getUID()));
  }
}
    
    
