/*
 * BeliefUpdateTrigger.java
 *
 * Created on June 7, 2004
 * <copyright>
 *  Copyright 2004 Telcordia Technoligies, Inc.
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

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

/**
 * This is the abstract superclass that defines those objects that
 * trigger a belief update computation.  Catches those things common
 * to all triggers to simplify the API and processing.
 *
 * It is assumed that all triggers act on a single asset state
 * dimension.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.10 $Date: 2004-07-02 23:34:03 $
 * @see BelievabilityDiagnosis
 * @see BelievabilityAction
 *
 */
abstract class BeliefUpdateTrigger extends Loggable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // abstract interface
    //------------------------------------------------------------

    /**
     * This routine should return the time (in ms) at which this
     * triggering event happened. More specifically, the time at which
     * we want to determine the new belief update.
     */
    abstract long getTriggerTimestamp();


    /**
     * This routine should return the asset statew dimension name that
     * this trigger pertains to.
     */
    abstract String getStateDimensionName();


    /**
     * This routine should return a string representation of the
     * belief update trigger.
     */
    abstract public String toString();


    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Simple accessor
     *
     * @return the AssetID for the affected asset
     */
    public AssetID getAssetID() 
    { 
        return _asset_id; 

    } // method getAssetID

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    public BeliefUpdateTrigger( AssetID asset_id ) {
        this._asset_id = asset_id;
    }  // constructor BeliefUpdateTrigger

    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _asset_id;

} // class BeliefUpdateTrigger

