/*
 * TimeUpdateTrigger.java
 *
 * Created on June 8, 2004
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
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.ActionDescription;

/**
 * This class serves for concrete instances of belief update triggers
 * that are based solely on time (no diagnosis or action.) 
 * 
 * @author Tony Cassandra
 */
abstract class TimeUpdateTrigger extends BeliefUpdateTrigger 
{

    //---------------------------------------------------------------
    // public interface
    //---------------------------------------------------------------

    //************************************************************
    /**
     * Return the object as a string
     */
    public String toString() {

        return this.getClass().getName() + " - AssetID: "
                + getAssetID() + ", Time: "
                + this._time;
     
    } // method toString

    //---------------------------------------------------------------
    // package interface
    //---------------------------------------------------------------

    /**
     * Main constructor
     *
     * @param asset_id The asset ID
     * @param time The time that the update happened or was triggered
     */
    TimeUpdateTrigger( AssetID asset_id, long time ) 
    {
        super( asset_id );

        this._time = time;

    } // constructor TimeUpdateTrigger

    //************************************************************
    /**
     * Return the time that this trigger was asserted.  This should be
     * the time for which the belief update calculation should take
     * place.  The exact semantics and source for this time will be
     * dependent on the particular trigger.
     * @return timestamp
     **/
    long getTriggerTimestamp() 
    {
        return this._time;
    } // method getTriggerTimestamp

    //************************************************************
    /**
     * When updating based on time only, we update all state
     * dimensions. Thus, instances of this class do *not* have a state
     * dimension. 
     *
     * @return Always returns null to indicate that there is no
     * particular state dimensioon that this trigger pertains to.
     */
    String getStateDimensionName()
    {
        return null;
    } // method getStateDimensionName
    
    //---------------------------------------------------------------
    // private interface
    //---------------------------------------------------------------

    private long _time;

} // class TimeUpdateTrigger

