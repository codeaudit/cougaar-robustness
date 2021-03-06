/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: TEDStressInstance.java,v $
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
import org.cougaar.coordinator.techspec.TransitiveEffectDescription;
import org.cougaar.coordinator.techspec.TransitiveEffectModel;

/**
 * This wraps and extends the TransitveEffectDescription (TED)
 * objects.  This is only used when a TransitveEffectDescription has
 * no corresponding instantiation of a TransitveEffectModel.  The
 * absence of the model means we cannot compute any probabilities
 * (we'll assume it is zero), but there can be further transitive
 * effects, so we must treat this like a stress.
 *
 *
 * The other complication that happens, is that though we may not have
 * amodel when this is first created, it is possible that the model
 * might be created down the road.  In this case, this object then
 * needs to start behaving as a TEMStressInstance.  thus, we have to
 * check for the model whenever we attempt to use it.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.21 $Date: 2004-12-14 01:41:47 $
 *
 */
class TEDStressInstance extends StressInstance
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
    TEDStressInstance( TransitiveEffectDescription stress )
    {
        super();

        this._stress_object = stress;

    }  // constructor TEDStressInstance

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Return an identifying label for the stress.
     */
    String getName() 
    {
        // TransitiveEffectDescription (TED) and
        // TransitiveEffectModel's (TEM) do not have proper names.
        // TED's have no name and TEM's use the event name (which is
        // really confusing).  Thus, we manufacture a name based on
        // the parent event (which should be unique).
        //
        return "CausedBy-" + getParentStressCollection().getEventName();
    }

    //************************************************************
    /**
     * Return the event description object for this stress.
     */
    EventDescription getEventDescription()
    {
        return _stress_object.getTransitiveEvent();
    } 

    //************************************************************
    /**
     * Return the list of applicable assets (a Vector of
     * AssetTechSpecInterface objects.  No assets are applicable to
     * this stress instance, so always return null.
     */
    Vector getAssetList()
    {
        // Here is where we attempt to convert this object if a model
        // has become available.
        //
        checkForInstantiation();

        if ( _model == null )
            return null;
        else
            return _model.getAssetList();
    } // method getAssetList

    //************************************************************
    /**
     * Return an identifying label for the particular stress instance
     * (concrete class ID)
     */
    String getTypeStr() 
    {
        if ( _model == null )
            return "TED"; 
        else
            return "TED/TEM"; 
    }

    //************************************************************
    /**
     * Return the asset state dimension that this stress has an
     * immediate relationship to.
     */
    AssetStateDimension getStateDimension()
    {
        return getEventDescription().getAffectedStateDimension();

    } // method getStateDimension

    //************************************************************
    /**
     * Determines if this stress instance affects the given asset.
     * This relationship can change over time, so we must be careful
     * about caching the results of this test.
     *
     * This treats instance affects no assets, so this always returns
     * false.
     */
    boolean affectsAsset( AssetTechSpecInterface asset_ts )
    {
        // Here is where we attempt to convert this object if a model
        // has become available.
        //
        checkForInstantiation();

        if ( _model == null )
            return false;
        
        return _model.containsAsset( asset_ts );
        
    } // method affectsAsset

     //************************************************************
    /**
     * Returns the stress probability over the given time
     * interval. Note that this stress is not dependent on the time
     * interval. This immediate stress probability *does not* include
     * the ancestor stress probabilities, so is conditioned on those
     * ancestor events happening. 
     *
     * This stress has no associated model, so we cannot access the
     * probability.  We assume it to be zero in that case, so this
     * always returns zero.
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    protected double getImmediateProbability( long start_time, 
                                              long end_time )
    {
        // Here is where we attempt to convert this object if a model
        // has become available.
        //
        checkForInstantiation();

        if ( _model == null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Attempting to compute the probability of a "
                        + "stress while there is no model instance: "
                        + getName() + ". Assuming prob=0.0" );
            
            return 0.0;
        }

        double prob = _model.getTransitiveEffectLikelihood();
        
        if ( _logger.isDetailEnabled() )
            _logger.detail( "For trans effect " + getName()
                  + " received prob = " + prob );
        
        return prob;
    
    } // method getStressProbability

    //************************************************************
    /**
     * there may be no TransitiveEffectModel for the
     * TransitiveEffectDescription when this object is first created.
     * However, because it may become available later, we should always
     * check to see if it is available.
     *
     * @return true only in the case where we go from having no model
     * to having one. False is already instantiated or if canot
     * instantiate. 
     */
    boolean checkForInstantiation()
    {
        if ( _model != null )
            return false;

        _model = _stress_object.getInstantiation();
        
        if ( _model != null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "New TransitiveEffectModel: "
                      + "TED Stress " + getName() 
                      + " converted to TEM stress.");

        }

        return _model != null;

    } // method checkForInstantiation

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private TransitiveEffectDescription _stress_object;

    // This will initially be null, since the whole point of creating
    // this type of instance is precisely because we do not have a
    // model.  However, in the future, a model instantiation of the
    // TransitiveEffectDescription may be created, in which case it is
    // easier to make this object behave like a TEMStressInstance than
    // it is to surgically retrofit the StressGraph structure.
    //
    private TransitiveEffectModel _model = null;

} // class TEDStressInstance
