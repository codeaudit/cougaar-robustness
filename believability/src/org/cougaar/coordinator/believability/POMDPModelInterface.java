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

import org.cougaar.coordinator.techspec.AssetType;

/**
 * This is the interface to the POMDP model that the non-POMPD model
 * believability components will use.
 *
 * @author Tony Cassandra
 */
public interface POMDPModelInterface 
{

    /**
     * Get the a priori probability that the indicated asset type.
     *
     * @param asset_type The type of the asset
     *
     */
    public BeliefState getInitialBeliefState( AssetType asset_type )
            throws BelievabilityException;

    /**
     * Used to update the belief state using the given diagnosis.
     *
     * @param start_belief initial belief state
     * @param diagnosis the diagnosis to use to determine new belief
     * state
     * @return the update belief state
     *
     */
    public BeliefState updateBeliefState( BeliefState start_belief,
                                          BeliefUpdateTrigger diagnosis )
            throws BelievabilityException;

    /**
     * Used to update the belief state to the present time.
     *
     * @param start_belief initial belief state
     * @param time the time to compute the new belief state to.
     * @return the update belief state
     *
     */
    public BeliefState updateBeliefState( BeliefState start_belief,
                                          long time )
            throws BelievabilityException;

    /**
     * Get a belief state with arbitrary assigned probabilities.
     *
     * @param asset_type The type of the asset
     *
     */
    public BeliefState getRandomBeliefState( AssetType asset_type )
            throws BelievabilityException;

} // class POMDPModelInterface
