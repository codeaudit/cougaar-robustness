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

import java.util.Vector;

/**
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface DefenseTechSpecInterface extends TechSpecRootInterface {

    
    /**
     * @return the vector of states that the defense described by this 
     * tech spec can be in
     */
    public Vector getStates();
    
    /**
     * @return a boolean indicating if this defense implements an idempotent action or not.
     *
     */
    public boolean isIdempotent();
    
    /**
     * @return a boolean indicating if this defense implements a reversible action or not.
     *
     */
    public boolean isReversible();

    
    /**
     * @return the asset type that the threat cares about.
     */
    public AssetType getAssetType();
    
    /**
     * @return the vector of monitoring levels that this defense supports
     *
     */
    public Vector getMonitoringLevels();
    
    /**
     * @return the Defense Finite State Machine object for this defense
     *
     */
    public DefenseFSM getDefenseFSM();

    
     /**
      * @return the ThreatTypes associated with this Defense
      */
     public Vector getThreatTypes();
   
     
    /** 
      * @return The asset state descriptor that this defense affects / diagnoses 
      */
    public AssetStateDescriptor getAffectedAssetState();

        /**
      * @return the cost of executing this defense
      */
     public double t_getCost();

     /**
      * @return the benefit of executing this defense
      */
     public double t_getBenefit();

}
