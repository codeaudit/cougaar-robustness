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

public class StateEstimationPublisher extends Loggable {

    public static double PUBLICATION_THRESHHOLD = .5;

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
     try {
         // Find the AssetStateInformation object, check, and maybe publish
         StateEstimation se = new StateEstimation ( belief_state,
                                  _model_manager );
      AssetID aid = se.getAssetID();
         AssetStateInformation asi = 
          (AssetStateInformation) _state_information.get( aid );
      AssetType at = belief_state.getAssetType();
      long max_latency = _model_manager.getMaxSensorLatency( at );
         if ( asi == null ) {
          logDebug( "Making new AssetStateInformation for asset " + aid
                 + " with latency " + max_latency );
             asi = new AssetStateInformation( aid, this, max_latency );
             _state_information.put( se.getAssetID(), asi );
         }
         asi.checkPublish( se );
     }
     catch ( BelievabilityException be ) {
      logError( "Cannot publish belief state [" 
             + belief_state.toString()
             + "] -- got exception "
             + be.getMessage() );
     }
    }


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
     * This method sets an alarm via the plugin
     * @param alarm The AssetAlarm to set
     * @throws BelievabilityException if it cannot find the plugin
     **/
    protected void setAlarm ( AssetAlarm alarm ) 
     throws BelievabilityException {
        
     try {
         _plugin.setAlarm( alarm );
     }
     catch ( Exception e ) {
         throw new BelievabilityException( "StateEstimationPublisher.setAlarm",
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
    // Private Classes
    //----------------------------------------------------------------------

    private class AssetStateInformation extends Loggable 
                                     implements CallbackInterface {

     AssetID _asset_id = null;
     AssetModel _asset_model = null;
     StateEstimation _last_se_published = null;
     StateEstimation _last_se_created = null;
     AssetAlarm _asset_alarm = null;
     StateEstimationPublisher _publisher = null;
     long _timer_length = 0;
     double _utility_threshhold = PUBLICATION_THRESHHOLD;


     /**
      * Constructor
      * @param asset_id the identifier of the asset
      * @param publisher the publisher to the blackboard
      * @param timer_length the length of time to wait before publishing
      **/
     public AssetStateInformation( AssetID asset_id,
                          StateEstimationPublisher publisher,
                          long timer_length ) {
         _asset_id = asset_id;
         _publisher = publisher;
         _timer_length = timer_length;
     }


     /**
      * Determine whether or not the asset's state estimation should be
      * forwarded to the blackboard. If so, forward it. Manage the timer
      * for putting StateEstimations on the blackboard.
      * Returns true if the utility along some state dimension
      * has fallen below a constant threshhold value, or if the timer
      * has expired indicating that there has been an interval of 
      * _max_diagnosis_latency from the arrival of the first diagnosis.
      **/

     public synchronized void checkPublish( StateEstimation state_estimation ) 
         throws BelievabilityException { 

         _last_se_created = state_estimation;

         if ( checkUtilityChange( _last_se_published, 
                         state_estimation ) ) {
          publish( state_estimation );
         }
         else {
          if ( ! isAlarmRunning() ) setAssetAlarm();
          logDebug("Delaying StateEstimation publication for asset "
                + state_estimation.getAssetID().toString() );
         }
     }


     /**
      * Check to see whether or not there has been a significant utility
      * change that would merit immediate publication of the new state.
      * @param old_se The last StateEstimation published
      * @param new_se The current StateEstimation
      * @throws BelievabilityException if there is trouble with the utility
      *         calculations
      * @return true if there is a significant change in the utility,
      *         false otherwise
      **/
     protected boolean checkUtilityChange( StateEstimation old_se, 
                               StateEstimation new_se ) 
         throws BelievabilityException {

         // May want to make this more sophisticated later
         if ( old_se == null ) return false;
         if ( new_se == null ) return false;
         logDebug( "Checking Utility for asset " 
                + _last_se_published.getAssetID().toString()
                + " -- old utility is  " 
                + old_se.getStateUtility()
                + " :: new utility is "
                + new_se.getStateUtility() );
         double utility_change = 
          old_se.getStateUtility() - new_se.getStateUtility();
         if ( ( utility_change > _utility_threshhold ) || 
           ( utility_change < ( 0 - _utility_threshhold ) ) )
          return true;
         return false;
     }


     /**
      * Publish the state estimation to the blackboard
      * @param state_estimation the StateEstimation to publish
      * @throws BelievabilityException if it cannot find the plugin
      **/
     protected synchronized void publish( StateEstimation state_estimation )
         throws BelievabilityException {
     
         clearAssetAlarm();
         _last_se_published = state_estimation;
         // _last_se_published = getRandomStateEstimation( state_estimation );
         logDebug( "Publishing StateEstimation for asset " 
                + _last_se_published.getAssetID().toString()
                + " with utility " 
                + _last_se_published.getStateUtility() );
         _publisher.publish( _last_se_published );
     }


     /**
      * Set an alarm for the asset
      * @throws BelievabilityException if it cannot find the plugin
      **/
     protected void setAssetAlarm() throws BelievabilityException {

         _asset_alarm = new AssetAlarm( _timer_length, 
                            (CallbackInterface) this );
         _publisher.setAlarm( _asset_alarm );
         logDebug( "Asset alarm set for asset " + _asset_id.toString() 
               + " for time " + _timer_length );

     }
   
     protected boolean isAlarmRunning() {
         if ( _asset_alarm == null ) return false;
         else return (! (_asset_alarm.isCanceled() || 
                   _asset_alarm.hasExpired() ) );
     }

     protected void clearAssetAlarm() {
         if (_asset_alarm != null) _asset_alarm.cancel();
         _asset_alarm = null;
     }
   

     /**
      * This method is called when the asset timer expires, is canceled, 
      * or the AssetModel decides that the state should be forwarded
      * immediately.
      * @param expiredP True if the alarm expired validly, false if it was
      *                 canceled.
      **/
     public void timerCallback ( boolean expiredP ) {

         // Don't do anything if the timer was canceled
         if (! expiredP) {
          logDebug( "Alarm canceled for: " + _asset_id.toString() );
          return; //**** check this
         }

         // Alarm has expired
         logDebug( "Alarm expired for: " + _asset_id.toString());

         // Get the current state and publish it.
         try {
          if ( _asset_model == null ) 
              _asset_model = _publisher.getAssetModel( _asset_id );
          BeliefState curr_belief = _asset_model.getCurrentBeliefState();
          publish( new StateEstimation( curr_belief, _model_manager ) );
         }
         catch( Exception e ) {
          logError( "Failed to publish belief state for assetID " 
                 + _asset_id 
                 + " when its timer expired -- "
                 + e.getMessage() );
         }
     }
    } // end AssetStateInformation internal class.


    //----------------------------------------------------------------------
    // Private interface
    //----------------------------------------------------------------------

    // Generate a random belief state
    private StateEstimation getRandomStateEstimation( StateEstimation se ) 
     throws BelievabilityException {

        POMDPModelInterface pomdp_model = _model_manager.getPOMDPModel();
           
        BeliefState random_belief 
            = pomdp_model.getRandomBeliefState ( se.getAssetType() );

        random_belief.setUpdateTrigger( null );
        random_belief.setAssetID( se.getAssetID() );
        random_belief.setTimestamp( se.getTimestamp() );

        logDebug( "Random state estimation returned for asset " 
            + se.getAssetID().toString() );

     return new StateEstimation( random_belief, _model_manager );
    }

    // The publisher that can publish to the blackboard
    private BelievabilityPlugin _plugin = null;

    // The container that holds all the asset models, indexed by asset id
    private AssetContainer _asset_container = null;

    // The interface to the model manager
    private ModelManagerInterface _model_manager = null;

    // A Hashtable of belief state information per asset, to aid in
    // determining when to publish. The key is the AssetID
    private Hashtable _state_information = new Hashtable();

}
