/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefUpdateTrigger.java,v $
 *</NAME>
 *
 * <copyright>
 *  Copyright 2004 Telcordia Technologies, Inc.
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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * This is the abstract superclass that defines those objects that
 * trigger a belief update computation.  Catches those things common
 * to all triggers to simplify the API and processing.
 *
 * It is assumed that all triggers act on a single asset state
 * dimension.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2008-07-25 20:47:16 $
 * @see BelievabilityDiagnosis
 * @see BelievabilityAction
 *
 */
abstract class BeliefUpdateTrigger extends Object implements Comparable
{
    // Class implmentation comments go here ...

    // For logging
    protected Logger _logger = Logging.getLogger(this.getClass().getName());

    //------------------------------------------------------------
    // abstract interface
    //------------------------------------------------------------

    /**
     * This routine should return the time (in ms) at which this
     * triggering event happened. More specifically, the time at which
     * we want to determine the new belief update.
     */
    long getTriggerTimestamp() { return _trigger_timestamp; }

    /**
     * This routine should return the time (in ms) at which this
     * triggering event happened. More specifically, the time at which
     * we want to determine the new belief update.
     */
    void setTriggerTimestamp( long value ) 
    { 
        _trigger_timestamp = value;
    }


    /**
     * This routine should return the asset state dimension name that
     * this trigger pertains to.
     */
    abstract String getStateDimensionName();

    /**
     * This routine should return a string representation of the
     * belief update trigger.
     */
    abstract public String toString();

    /*
     * We use comparisons of BeliefUpdateTrigger that are based on the
     * trigger timestamp.  We assume never have a need to compare the
     * contents of belief triggers, so this time based comparison is
     * useful for sorting these objects by time.
     */
    public int compareTo( Object obj )
    {
        if ( ! ( obj instanceof BeliefUpdateTrigger))
            return -1;

        BeliefUpdateTrigger trigger = (BeliefUpdateTrigger) obj;

        if ( this.getTriggerTimestamp() < trigger.getTriggerTimestamp() )
            return -1;

        if ( this.getTriggerTimestamp() > trigger.getTriggerTimestamp() )
            return 1;

        return 0;
    }

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Simple accessor
     *
     * @return the AssetID for the affected asset
     */
    AssetID getAssetID() 
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
    protected BeliefUpdateTrigger( AssetID asset_id ) {
        this._asset_id = asset_id;
    }  // constructor BeliefUpdateTrigger

    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _asset_id;

    private long _trigger_timestamp;

} // class BeliefUpdateTrigger

