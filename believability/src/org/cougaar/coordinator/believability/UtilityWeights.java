/*
 * UtilityWeights.java
 *
 * Created on April 24, 2004
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetState;

/**
 * This is the class that encapsulates the utility information and
 * computation for the state dimensions of the assets.
 **/

public class UtilityWeights extends Hashtable {

    /** 
     * Regular constructor. This reads the tech specs and extracts out
     * the information needed for the utility calculations.
     * @param asset_ts The tech spec for the asset type
     * @param mau_weights The MAU weights from the user to be used in
     *                    the computation. 
     **/
    public UtilityWeights( AssetTechSpecInterface asset_ts,
                           double[] mau_weights ) {

     // Iterate through the state dimensions, computing the utility
     // information for each dimension.
        Enumeration asset_sd_enum = asset_ts.getAssetStates().elements();
        while ( asset_sd_enum.hasMoreElements() ) {

            // Compute the utility weights for the state dimension and add
         // them to the hashtable
            AssetStateDimension asd =
          (AssetStateDimension) asset_sd_enum.nextElement();
            String asd_name = asd.getStateName();
            double[] multipliers = computeSDUtilityMultipliers( asd,
                                        mau_weights );
            put( asd_name, multipliers );
        }
    }


    /**
     * Get the multipliers for the given AssetStateDimension
     * @param asd_name The name of the AssetStateDimension
     * @throws BelievabilityException if there are no multipliers
     *                                associated with the state
     * @return the array of multipliers, as a double[]
     **/
    public double [] getSDUtilityMultipliers( String asd_name )
     throws BelievabilityException {

        Object retval = get( asd_name );
     if ( retval != null ) return (double[]) retval;
        else 
         throw new BelievabilityException(
                  "UtilityWeights.getSDUtilityMultipliers",
                  "No utility multipliers for state " + asd_name );
    }


    /**
     * Compute a utility value from a StateDimensionEstimation
     * @param sde The input StateDimensionEstimation
     * @throws BelievabilityException if the sde is for a state
     *                                dimension we have no knowledge of.
     * @return The utility of the StateDimension, as a double
     **/
    public double computeSDUtility( StateDimensionEstimation sde ) 
     throws BelievabilityException {

        String sd_name = sde.getAssetStateDimensionName();

        // Get the utility multipliers, as an array of size num_states
        // May throw BelievabilityException
        double[] multipliers = getSDUtilityMultipliers( sd_name );

        // Get the belief state for the state dimension, as an array of
     // size num_states
        double[] sd_estimation = sde.toArray();

        // Compute the utility
     double utility = 0.0;
        for ( int i = 0; i < multipliers.length; i++ ) {
            utility += multipliers[i] * sd_estimation[i];
        }

        return utility;
    }


    //-----------------------------------------------------------------------
    // private interface
    //-----------------------------------------------------------------------

    /**
     * Compute the utility multipliers related to one StateDimension
     **/
    private double[] computeSDUtilityMultipliers( AssetStateDimension asd,
                                double[] mau_weight_array ) {

        // Initialize the return array
     int num_states = asd.getNumStates( );
     double[] utility_multipliers = new double[num_states];
        for (int i = 0; i < utility_multipliers.length; i++ ) 
         utility_multipliers[i] = 0.0;
 
     // Now set the multipliers from tthe mau weights, tech specs, etc.
        for ( Iterator i = asd.getPossibleStates().iterator(); i.hasNext(); ) {
            AssetState as = (AssetState) i.next();
            int state_index = asd.StateNameToStateIndex( as.getName() );

            double utility_weight = 0.0;
            utility_weight += ( mau_weight_array[MAUWeightModel.COMPLETENESS_IDX]
                    * as.getRelativeMauCompleteness() );
            utility_weight += ( mau_weight_array[MAUWeightModel.SECURITY_IDX]
                    * as.getRelativeMauSecurity() );
            utility_weight += ( mau_weight_array[MAUWeightModel.TIMELINESS_IDX]
                    * this.getRelativeMauTimeliness( as ) );

            utility_multipliers[state_index] = utility_weight;
        }
        return utility_multipliers;
    }


    /**
     * Gets the timeliness relative MAU weight. Currently a stub returning 0.0.
     * @param asset_state The AssetState for which the timeliness is needed
     * @return the computed relativeMAUTimeliness
     **/
    private double getRelativeMauTimeliness( AssetState asset_state ) {
        return 0.0;
    }
}
