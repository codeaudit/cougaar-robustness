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

public class StateEstimationPublisher extends Loggable {


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
     * Process a new BeliefState
     * @param BeliefState belief_state
     **/
    public void consumeBeliefState( BeliefState belief_state ) {


        //*** FIXME: Ignore passed in belief sttae and just generates a
        // random belief state for now to allow David to continue action
        // selection testing until we get full belief update in place. 
        //
        try {
            POMDPModelInterface pomdp_model = _model_manager.getPOMDPModel();
            
            BeliefState random_belief 
                    = pomdp_model.getRandomBeliefState
                    ( belief_state.getAssetType() );

            random_belief.setDiagnosis( belief_state.getDiagnosis() );
            random_belief.setAssetID( belief_state.getAssetID() );
            random_belief.setTimestamp( belief_state.getTimestamp() );

            logDebug( "Ramdom belief state: " + random_belief.toString() );
            
            StateEstimation se =
                    new StateEstimation ( random_belief, _model_manager );
            if ( forwardStateP() ) _plugin.publishAdd( se );
            logDebug( "StateEstimationPublisher: published StateEstimation" );
        }
        catch ( BelievabilityException be ) {
            System.out.println(" ***** StateEstimationPublisher -- fix !!!");
        }
    }

    /**
     * Determine whether or not the asset's state estimation should be
     * forwarded to the blackboard. If so, forward it. Manage the timer
     * for putting StateEstimations on the blackboard.
     * Returns true if the utility along some state dimension
     * has fallen below a constant threshhold value, or if the timer
     * has expired indicating that there has been an interval of 
     * _max_diagnosis_latency from the arrival of the first diagnosis.
     * @param asset_model The asset model to check.
     **/
    private boolean forwardStateP ( ) {
     return true;
     //     System.out.println ("*****forwardStateP: not completely implemented yet ***" );
     
     // Check to see if the state has fallen below some threshhold for
     // some state dimension. If so, return true.
     //     if ( false ) {
     //         _plugin.publishAdd( this.getCurrentState() );
     //
     //         // Cancel the alarm
     //         if ( _asset_alarm != null ) _asset_alarm.cancel();
     //
     //         return true;
     //     }
     //
     //     // Check if the timer is running. If not, set the timer for the asset
     //     if ( _asset_alarm == null ) {
     //
     //         _asset_alarm = new AssetAlarm( this, _max_diagnosis_latency );

         // Start the alarm
     //         _plugin.setAlarm( _asset_alarm );
     //    }

     // Timeouts are handled separately by the timer callback, so just 
     // need to return here.
    //     return false; 
    }


    /**
     * This method is called when the asset timer expires, is canceled, 
     * or the AssetModel decides that the state should be forwarded
     * immediately.
     * @param asset_id The ID of the timed-out asset.
     * @param expiredP True if the alarm expired validly, false if it was
     *                 canceled.
     **/
    public void timerCallback ( AssetID asset_id, boolean expiredP ) {
     //   *** Need to get the current state to publish
     // Publish the state estimation if the timer expired validly.
     //     _plugin.publishAdd( this.getCurrentState() );

     //     // Clear the timer information from the local variables
     //     _asset_alarm = null;
    }


    //----------------------------------------------------------------------
    // Private interface
    //----------------------------------------------------------------------

    // The publisher that can publish to the blackboard
    private BelievabilityPlugin _plugin = null;

    // The interface to the model manager
    private ModelManagerInterface _model_manager = null;

}

