/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: TEMStressInstance.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/TEMStressInstance.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-29 22:43:18 $
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

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.TransitiveEffectModel;

/**
 * This wraps and extends the TransitveEffectModel objects.  
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-06-29 22:43:18 $
 *
 */
class TEMStressInstance extends StressInstance
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
      // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    TEMStressInstance( TransitiveEffectModel stress )
    {
        this._stress_object = stress;
    }  // constructor TEMStressInstance

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Return an identifying label for the stress.
     */
    String getName() { return _stress_object.getName(); }

    //************************************************************
    /**
     * Return the event description object for this stress.
     */
    EventDescription getEventDescription()
    {
        return _stress_object.getTransitiveEffectDescription
                ().getTransitiveEvent();
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
    String getTypeStr() { return "TEM"; }

    //************************************************************
    /**
     * Return the asset state dimension that this stress has an
     * immediate relationship to.
     */
    AssetStateDimension getStateDimension()
    {
        return _stress_object.getTransitiveEffectDescription
                ().getTransitiveEvent().getAffectedStateDimension();

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
     * interval. Note that this stress is not dependent on the time
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
        double prob = _stress_object.getTransitiveEffectLikelihood();
        
        logDebug( "For trans effect " + getName()
                  + " received prob = " + prob );
                  
        return prob;

    } // method getStressProbability

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private TransitiveEffectModel _stress_object;

} // class TEMStressInstance
