/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: TMIStressInstance.java,v $
 *</NAME>
 *
 *<COPYRIGHT>
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
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.NegativeIntervalException;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

/**
 * This wraps and extends the ThreatModelInterface object.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.21 $Date: 2004-12-14 01:41:47 $
 *
 */
class TMIStressInstance extends StressInstance
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    TMIStressInstance( ThreatModelInterface stress )
    {
        super();

        this._stress_object = stress;
    }  // constructor TMIStressInstance

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Return an identifying label for the stress.
     */
    String getName() { return _stress_object.getThreatDescription
                               ().getName(); }

    //************************************************************
    /**
     * Return the event description object for this stress.
     */
    EventDescription getEventDescription()
    {
        return _stress_object.getThreatDescription().getEventThreatCauses();
    }

    //************************************************************
    /**
     * Return the list of applicable assets (a Vector of
     * AssetTechSpecInterface objects.
     */
    Vector getAssetList()
    {
        return _stress_object.getAssetList();
    } // method getAssetList

    //************************************************************
    /**
     * Return an identifying label for the particular stress instance
     * (concrete class ID)
     */
    String getTypeStr() { return "TMI"; }

    //************************************************************
    /**
     * Return the asset state dimension that this stress has an
     * immediate relationship to.
     */
    AssetStateDimension getStateDimension()
    {
        return _stress_object.getThreatDescription
                ().getEventThreatCauses().getAffectedStateDimension(); 

    } // method getStateDimension

    //************************************************************
    /**
     * Determines if this stress instance affects the given asset.
     * This relationship can change over time, so we must be careful
     * about caching the results of this test.
     */
    boolean affectsAsset( AssetTechSpecInterface asset_ts )
    {
        return _stress_object.containsAsset( asset_ts );
    } // method affectsAsset

    //************************************************************
    /**
     * Returns the stress probability over the given time
     * interval. This immediate stress probability *does not* include
     * the ancestor stress probabilities, so is conditioned on those
     * ancestor events happening. 
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    protected double getImmediateProbability( long start_time, 
                                              long end_time )
    {
        double prob;

        try
        {
            prob = _stress_object.getProbabilityOfEvent( start_time,
                                                                end_time );
        }
        catch (NegativeIntervalException nie)
        {
            if ( _logger.isDebugEnabled() )
                _logger.debug( "Caught NegativeIntervalException exception."
                               + " Using prob = 0.0" );
            prob = 0.0;
        }

        if ( _logger.isDetailEnabled() )
            _logger.detail( "For threat " 
                  + getName()
                  + " from " + start_time + " to " + end_time
                  + " received prob = " + prob );
                  
        return prob;

    } // method getStressProbability

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private ThreatModelInterface _stress_object;

} // class TMIStressInstance
