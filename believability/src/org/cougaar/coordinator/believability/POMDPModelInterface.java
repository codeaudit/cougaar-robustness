/*
 * POMDPModelInterface.java
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

/**
 * This is the interface to the POMDP model.
 * @author Misty Nodine
 **/

import org.cougaar.coordinator.techspec.AssetID;


public interface POMDPModelInterface {

    /**
     * Get the a priori probability that the indicated asset and the
     * indicated state descriptor take a specific given state (value).
     * This probability will be 1 for the default state of the asset
     * along the dimension described by the state descriptor, and 
     * zero otherwise.
     *
     * @param asset_id The AssetID of the asset
     * @param state_descriptor_name The name of the state descriptor
     * @param state_name The name of the stae in the state descriptor
     * @return The a priori probability that the asset is in the named state 
     *         with respect to the dimension of the state descriptor.
     * @exception BelievabilityException if the asset does not exist or the
     *                                   state descriptor does not exist
     **/
    public double getAprioriProbability( AssetID asset_id,
                                         String state_descriptor_name,
                                         String state_name )
            throws BelievabilityException;


    /**
     * Get the state transition probability for the given state 
     * descriptor for the asset, given a particular threat. This is 
     * computed as the product
     *  P(state transition) = 
     *        P(threat is realized) * P(state transition | threat is realized)
     *
     * @param asset_id The AssetID of the asset
     * @param state_descriptor_name The name of the state descriptor
     * @param from_state_name The name of the state in the state 
     *                        descriptor that the transition is from
     * @param to_state_name The name of the state in the state 
     *                      descriptor that the transition is to
     * @param start_time The time of the start of the time window this concerns
     * @param end_time The time that this window ends
     * @return The probability that the threat has caused a transition from
     *         state from_state_name to state to_state_name;
     *         zero if the probability cannot be computed.
     * @exception BelievabilityException if the asset does not exist,
     *                           the state descriptor does not exist,
     *                           or the probability cannot be computed
     **/

    public double getTransProbability( String asset_expanded_name,
                                       String state_descriptor_name,
                                       String from_descriptor_value,
                                       String to_descriptor_value,
                                       long start_time,
                                       long end_time )
     throws BelievabilityException;


    /**
     * Get the observation probability -- that is, the probability of
     * the observation given the actual state of the asset.
     *
     * @param asset_ID The AssetID of the asset
     * @param diagnosis The BeleivabilityDiagnosis for the asset
     * @param monitoring_level The monitoring level at the current 
     *                             time (currently ignored)
     * @param state_descriptor_name The name of the state descriptor
     * @param state_name The name of the state in the state descriptor
     * @return The probability of the observed diagnosis given the actual
     *         state of the asset; zero if no probability can be computed.
     * @exception BelievabilityException if the asset does not exist,
     *                           the state descriptor does not exist
     *                           or the defense does not exist
     **/

    public double getObsProbability( AssetID asset_id,
                                     String monitoring_level,
                                     BelievabilityDiagnosis diagnosis,
                                     String state_descriptor_name,
                                     String state_descriptor_value )
     throws BelievabilityException;
   
} // class POMDPModelInterface
