/**
 * StateEstimationPublisher.java
 *
 * Created on May 13, 2004
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
 **/

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

import java.util.Hashtable;

public class StateEstimationPublisher 
        extends Loggable implements BeliefConsumer {

    /**
     * Constructor.
     * @param plugin the BelievabilityPlugin that can talk to the blackboard.
     * @param model_manager The model manager with the weight information
     **/
    public StateEstimationPublisher( BelievabilityPlugin plugin,
                                     ModelManagerInterface model_manager ) {
     _plugin = plugin;
     _model_manager = model_manager;
    }

    /**
     * Process a new BeliefState. (this is the BeliefConsumer
     * interface method used by BeliefTriggerHistory. )
     *
     * @param belief_state The belief state to publish
     **/
    public void consumeBeliefState( BeliefState belief_state ) 
    {
        try 
        {
            // Find the AssetStateInformation object, check, and maybe
            // publish
            //
            StateEstimation se = new StateEstimation ( belief_state,
                                                       _model_manager );
            // This doesn't really publish the state estimation to the
            // blackboard, but queues it for later publishing.  This
            // consumeBeliefState() method can be called in the
            // context of an expired alarm, and it is wrong cougaar
            // protocol to publish while processing the alarm's
            // expiration. 
            //
            publish( se );

        }
        catch ( BelievabilityException be ) {
            logWarning( "Cannot publish belief state [" 
                        + belief_state.toString()
                        + "] -- got exception "
                        + be.getMessage() );
        }

    } // method consumeBeliefState

    /**
     * This method publishes the input state estimation to the blackboard
     * @param state_estimation The StateEstimation to publish
     * @throws BelievabilityException when the state estimation cannot 
     *         be published
     **/
    protected void publish ( StateEstimation state_estimation ) 
     throws BelievabilityException {

     try {
         _plugin.queueForPublication( state_estimation );
     }
     catch ( Exception e ) {
         throw new BelievabilityException( "StateEstimationPublisher.publish",
                               "Got exception == " + e.getMessage() );
     }
    }

    /**
     * Set the asset container
     * @param asset_container The new asset container
     **/
    public void setAssetContainer( AssetContainer asset_container ) {
     _asset_container = asset_container;
    }

    /**
     * Get the AssetModel for an asset 
     * @param asset_id the identifier of the asset
     * @return the AssetContainer, null if there is none
     **/
    public AssetModel getAssetModel( AssetID aid ) {
     return _asset_container.getAssetModel( aid );
    }

    //----------------------------------------------------------------------
    // Private interface
    //----------------------------------------------------------------------

    // The publisher that can publish to the blackboard
    private BelievabilityPlugin _plugin = null;

    // The container that holds all the asset models, indexed by asset id
    private AssetContainer _asset_container = null;

    // The interface to the model manager
    private ModelManagerInterface _model_manager = null;

}
