/*
 * StateEstimation.java
 *
 * Created on April 21, 2004
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

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This object is a hashtable that collects together, for an asset, the 
 * StateDimensionEstimations for each state dimension for the asset.
 * A StateDimensionEstimation is a probabilistic distribution, over a
 * specific AssetStateDimension, of the belief that the asset is in each
 * state of the AssetStateDimension.
 **/

public class StateEstimation extends Hashtable implements NotPersistable 
{
    /**
     * Create a new state estimation for an asset. This is done when
     * the state estimation is being made as of some time, without
     * any diagnoses being received explicitly.
     * @param belief_state Some belief state for the asset
     * @throws BelievabilityException if it has a problem
     **/
    public StateEstimation( BeliefState belief_state, 
                   ModelManagerInterface model_manager ) 
     throws BelievabilityException {

     _belief_state = belief_state;
     _model_manager = model_manager;

     Vector _belief_state_dims = _belief_state.getAllBeliefStateDimensions();
     Enumeration bsd_enum = _belief_state_dims.elements();
     while( bsd_enum.hasMoreElements() ) {
         BeliefStateDimension bsd =
          (BeliefStateDimension) bsd_enum.nextElement();

         // Get the utility weights 
         double[] _mau_weighted_utilities = 
          model_manager.getWeightedAssetUtilities( getAssetType(), 
                                    bsd.getAssetStateDimension() );

         // Make a state dimension estimation object for this state dimension
         StateDimensionEstimation sde 
          = new StateDimensionEstimation( bsd, _mau_weighted_utilities );
         setStateDimensionEstimation( bsd.getAssetStateDimension(), sde );
     }
    }


    /**
     * Get the identifier of the asset
     * @return the asset identifier
     **/
    public AssetID getAssetID() { return _belief_state.getAssetID(); }


    /**
     * Get the identifier of the asset
     * @return the asset identifier
     **/
    public AssetType getAssetType() { return getAssetID().getType(); }


    /**
     * Get the timestamp for the diagnosis, which is also the time 
     * as of when the estimation was made.
     * @return The time when the diagnosis was received.
     **/
    public long getTimestamp() { return _belief_state.getTimestamp(); }


    /**
     * Get the StateDimensionEstimation for the named belief state
     * @param state_dimension the AssetStateDimension associated with 
     *                        the belief state
     * @throws BelievabilityException if there is no state estimation
     *                                information for the state.
     * @return the StateDimensionEstimation
     **/
    public StateDimensionEstimation 
     getStateDimensionEstimation( AssetStateDimension state_dimension ) 
     throws BelievabilityException {

     StateDimensionEstimation sde = 
         (StateDimensionEstimation) get( state_dimension );

     if ( sde == null ) {
         throw new BelievabilityException( 
                "StateEstimation.getStateDimensionEstimation",
          "No belief information for state " + 
                state_dimension.getStateName());
     }
     else return sde;
    }


    /**
     * Get all StateDimensionEstimations for this StateEstimation
     * @return an enumeration of the StateDimensionEstimation elements.
     **/
    public Enumeration getStateDimensionEstimations() {
     return this.elements();
    }


    /**
     * Set the StateDimensionEstimation for the named belief state 
     * @param state_dimension the AssetStateDimension associated with 
     *                        the belief state
     * @param estimate the StateDimensionEstimation for the state dimension
     **/
    public void     
     setStateDimensionEstimation( AssetStateDimension state_dimension,
                         StateDimensionEstimation estimate ) {

     put( state_dimension, estimate );
    }


    /**
     * Compute the utility of being in the current state. This takes into 
     * account the relative utility percentages (e.g. from the asset tech
     * specs) and the MAU inputs. If some state dimension has no associated
     * belief state, its utility is assumed to be 0.
     *
     * @throws BelievabilityException if there is no utility information
     *                                available for the state dimension.
     * @return a utility measure.
     **/
    public double getStateUtility() throws BelievabilityException {

        double state_utility = 0.0;

     Enumeration sde_enum = this.getStateDimensionEstimations();
     while ( sde_enum.hasMoreElements() ) {
         StateDimensionEstimation sde = 
          (StateDimensionEstimation) sde_enum.nextElement();
         state_utility += sde.getUtility();
     }

     return state_utility;
    }


    /**
     * Compute the utility along a given state dimension of being in
     * the current state. This takes into account the relative 
     * utility percentages (e.g. from the asset tech specs) and the
     * MAU inputs.
     * @param state_dimension the AssetStateDimension for the state 
     *                         you are interested in.
     * @throws BelievabilityException if there is no state estimation
     *                                information or utility information
     *                                for the input AssetStateDimension
     * @return the utility for that state dimension.
     **/
    public double 
     getStateDimensionUtility( AssetStateDimension state_dimension ) 
     throws BelievabilityException {

     // This may throw a BelievabilityException
     return this.getStateDimensionEstimation(state_dimension).getUtility();
    }


    /**
     * Clone this StateEstimation, so that it can be used to
     * try repair options and compare the utilities in a consistent manner.
     * @throws BelievabilityException but shouldn't
     * @return A clone of all of this StateEstimation
     **/
    public StateEstimation cloneSE() throws BelievabilityException {
     BeliefState bs_clone = (BeliefState) _belief_state.clone();
     return new StateEstimation( bs_clone, _model_manager );
    }


    //************************************************************
    // Static methods 
    
    public static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof StateEstimation ) return true ;
                return false ;
            }
	};




    //************************************************************
    // Belief state related to this StateEstimation
    private BeliefState _belief_state = null;

    // Utility weight vector to use in computing utilities.
    private double[] _mau_weighted_utilities = null;

    // Model manager, to get utility information from.
    private ModelManagerInterface _model_manager = null;
}
