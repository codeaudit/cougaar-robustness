/*
 * DefenseTechSpec.java
 *
 * Created on July 9, 2003, 9:24 AM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
        AssetStateDescriptor affectsAssetState; 
        
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
                                 double tempCost, double tempBenefit, AssetType assetType, AssetStateDescriptor assetState) {
    
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
    public AssetStateDescriptor getAffectedAssetState() {
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
