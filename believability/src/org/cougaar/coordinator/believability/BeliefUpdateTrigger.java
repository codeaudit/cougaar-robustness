/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefUpdateTrigger.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefUpdateTrigger.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-09 17:58:33 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
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
 * @version $Revision: 1.1 $Date: 2004-06-09 17:58:33 $
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

    /**
     * Simple accessor
     *
     * @return the AssetType for the affected asset
     */
    public AssetType getAssetType() 
    { 
        return _asset_type; 

    } // method getAssetType

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    protected BeliefUpdateTrigger( AssetID asset_id,
                                   AssetType asset_type )
    {
        this._asset_id = asset_id;
        this._asset_type = asset_type;
    }  // constructor BeliefUpdateTrigger

    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _asset_id;
    private AssetType _asset_type;

} // class BeliefUpdateTrigger
