/*
 * StateDimensionEstimation.java
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

import java.util.Hashtable;
import java.util.Enumeration;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetID;


/**
 * This class represents the belief state along a single state dimension
 * of an asset.
 **/
public class StateDimensionEstimation extends Loggable {

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor
     *
     * @param state_dimension the belief state dimension that this 
     *                        estimation concerns
     * @param mau_weighted_utils the utility multipliers for this
     *                               state dimension.
     **/
    public StateDimensionEstimation( BeliefStateDimension state_dimension,
                         double[] mau_weighted_utils )
     throws BelievabilityException {

     _belief_state_dimension = state_dimension;
     _mau_weighted_utils = mau_weighted_utils;
     _asset_id = state_dimension.getAssetID();

    } // end constructor


    /** 
     * Get the name of the state dimension that this estimation concerns
     * @return the name of the state dimension
     **/
    public String getAssetStateDimensionName() { 
     return _belief_state_dimension.getName();
    }

    /** 
     * Get the name of the state dimension that this estimation concerns
     * @return the name of the state dimension
     **/
    public BeliefStateDimension getBeliefStateDimension()
    { 
        return _belief_state_dimension; 
    }

    /**
     * Gets a probability to this belief state using a numeric state.
     *
     * @param state The state to set the probability to as an integer.
     * @exception BelievabilityException if the belief state is not known.
     */
    public double getProbability( String state )
            throws BelievabilityException
    {
        
        return _belief_state_dimension.getProbability( state );
    }  // method getProbability


    /**
     * Gets the probabilities as an array.
     *
     * @exception BelievabilityException if the belief state is not known.
     */
    public double[] getProbabilities( ) throws BelievabilityException
    {
        return _belief_state_dimension.getProbabilityArray(  );
    }  // method getProbabilities


    /**
     * Compute the utility of being in the current state in this state
     * dimension. This takes into account the relative utility
     * percentages (e.g. from the asset tech specs) and the MAU inputs.
     *
     * @throws BelievabilityException if there is no utility information
     *             available for the asset.
     * @return the utility for this state dimension.
     **/
    public double getUtility( ) throws BelievabilityException {

     double retval = 0.0;

     double[] probs = getProbabilities();
     if ( ( probs == null ) || ( _mau_weighted_utils == null ) ||
          ( probs.length != _mau_weighted_utils.length ) ) {
         throw new BelievabilityException( 
               "StateDimensionEstimation.getUtility",
               "belief state does not correlate with utility weights" );
     }

     for ( int i = 0; i < probs.length; i++ ) {
         retval += ( probs[i] * _mau_weighted_utils[i] );
     }

     return retval;
    }


    /**
     * Returns a string represenation of the StateDimensionEstimation
     * @return the string representation.
     **/
    public String toString() {
        StringBuffer buff = new StringBuffer();
        
        buff.append( "StateDimensionEstimation for asset " );
        buff.append( _asset_id.toString() + ":" );
        buff.append( _belief_state_dimension.toString() );

        return buff.toString();
    } // method toString


    /**
     * Convert this belief state to an array.
     * @return A probability distribution over asset stattes as an
     * array 
     */
    public double[] toArray( )
     throws BelievabilityException {

     //**** FIXME mhn
        double[] belief = new double[1];
    //
    //     // This next few lines may throw a BelievabilityException
    //     // copy the probabilities
    //     for ( int i = 0; i < _num_states; i++ ) {
    //         String state_name = _state_dimension.StateIndexToStateName ( i );
    //         
    //         // Set the value in the belief array
    //         if ( state_name != null ) 
    //          belief[i] = getProbability( state_name );
    //         else throw new BelievabilityException( 
    //          "StateDimensionEstimation.toArray",
    //          "No state at index " + i
    //                  + " in state dimension " + _state_dimension_name
    //            + " of asset " + _asset_id.toString() );
    //     }
     return belief;
    } // method toArray


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------
    
    private AssetID _asset_id = null;

    // The associated belief state dimension
    private BeliefStateDimension _belief_state_dimension = null;

    // The utility weights for the StateDimension
    private double[] _mau_weighted_utils = null;

} // class StateDimensionEstimation
