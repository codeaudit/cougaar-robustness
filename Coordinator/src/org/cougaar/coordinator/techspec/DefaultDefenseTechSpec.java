/*
 * DefenseTechSpec.java
 *
 * Created on July 9, 2003, 9:24 AM
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
 */

package org.cougaar.coordinator.techspec;

import org.cougaar.core.util.UID;
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;
import org.cougaar.core.util.UniqueObject;

import java.util.Vector;


/**
 * A default implementation of the DefenseTechSpec
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DefaultDefenseTechSpec implements DefenseTechSpecInterface, NotPersistable, UniqueObject {
    
        String name;
        UID uid;
        private static final String revision = "1.0";
        boolean idempotent;
        boolean reversible;
        boolean abortable;
        double tempCost;
        double tempBenefit;
        AssetType assetType;
        AssetStateDimension affectsAssetState; 
        
        //Unused Attributes
        Vector states = null;
        Vector monitoringLevels = null;
        DefenseFSM fsm = null;
    
        
        
        /** Holds the threat types that this defense is applicable to */
        Vector threatTypes;
        
    
    /** Creates a new instance of DefenseTechSpec */
    public DefaultDefenseTechSpec(String name, UID uid, Vector threatTypes, boolean idempotent, boolean reversible)  {
    
        this.name = name;
        this.uid = uid;
        this.idempotent = idempotent;
        this.reversible = reversible;
        this.threatTypes = threatTypes;

        monitoringLevels = new Vector();
    }

    /** Creates a new instance of DefenseTechSpec */
    public DefaultDefenseTechSpec(String name, UID uid, boolean idempotent, boolean reversible, boolean abortable, 
                                 double tempCost, double tempBenefit, AssetType assetType, AssetStateDimension assetState) {
    
        this.name = name;
        this.uid = uid;
        this.idempotent = idempotent;
        this.reversible = reversible;
        this.abortable = abortable;
        this.tempCost = tempCost;
        this.tempBenefit = tempBenefit;
        this.assetType = assetType;
        this.affectsAssetState = assetState;
        
        monitoringLevels = new Vector();
    }
    
    
    /** @return the threat types that this defense is applicable to */
    public Vector getThreatTypes() { return threatTypes; }
    
    
    /** @return the FSM for this defense - NOT IN USE AT THIS TIME */
    public DefenseFSM getDefenseFSM() {
        return fsm;
    }
    
    /** Set the FSM for this defense */
    public void setDefenseFSM(DefenseFSM fsm) {
        this.fsm = fsm;
    }   
    
    /** @return the monitoring levels of this defense */
    public Vector getMonitoringLevels() {
        return monitoringLevels;
    }
    
    /** Set the monitoring levels of this defense */
    public void setMonitoringLevels(Vector monitoringLevels) {
        this.monitoringLevels = monitoringLevels;
    }

    /** Set the monitoring levels of this defense */
    public void addMonitoringLevel(MonitoringLevel monitoringLevel) {
        this.monitoringLevels.addElement(monitoringLevel);
    }

    /** @return the name of this defense */
    public String getName() {
        return name;
    }
    
    /** @return the revision of this interface */
    public String getRevision() {
        return revision;
    }
    
    /** @return the fsm states of this defense */
    public Vector getStates() {
        return states;
    }
    
    /** Set the fsm states of this defense */
    public void setStates(Vector states) {
        this.states = states;
    }

    /** @return the UID of this object */
    public org.cougaar.core.util.UID getUID() {
        return uid;
    }

    /** set the UID of this object*/
    public void setUID(UID uid) {
        this.uid = uid;
    }
    
    
    /** @return TRUE if this defense is idempotent */
    public boolean isIdempotent() {
        return idempotent;
    }

    /** @return TRUE if this defense is reversible */
    public boolean isReversible() {
        return reversible;
    }
    
    /** @return TRUE if this defense is reversible */
    public boolean isAbortable() {
        return abortable;
    }
   
    /** @return the AssetType that this defense monitors */
    public AssetType getAssetType() {
        return assetType;
    }

    /** @return The asset state descriptor that this defense affects / diagnoses */
    public AssetStateDimension getAffectedAssetState() {
        return affectsAssetState;
    }
    

    /**
      * @return the cost of executing this defense
      */
     public double t_getCost() {
         
        return (double)tempCost;
     }

     /**
      * @return the benefit of executing this defense
      */
     public double t_getBenefit() {
         
        return (double)tempBenefit;         
     }
    
    
}
