/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPAssetDimensionModel.java,v $
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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

/**
 * This class represents a particular instance of a POMDP model for a
 * given asset type. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.32 $Date: 2004-12-14 01:41:47 $
 *
 */
class POMDPAssetDimensionModel extends Model
{
    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    POMDPAssetDimensionModel( AssetTypeDimensionModel dim_model )
            throws BelievabilityException
    {
        if ( dim_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.POMDPAssetDimensionModel()",
                      "Asset model is NULL" );
        
        this._asset_dim_model = dim_model;

        if ( _logger.isDetailEnabled() )
            _logger.detail( "\tCreating POMDP model for dimension: " 
                  + _asset_dim_model.getStateDimensionName( ) );

        createInitialBeliefState();

    }  // constructor POMDPAssetDimensionModel

    //************************************************************
    // Simple accessors
    //

    BeliefStateDimension getInitialBeliefState() 
    {
        return _initial_belief; 
    }

    //************************************************************
    /**
     * Constructs the initial belief state for this asset type.
     *
     */
    void createInitialBeliefState( )
            throws BelievabilityException
    {
        if ( _asset_dim_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model is NULL" );
        
        if ( _logger.isDetailEnabled() )
            _logger.detail( "\tCreating POMDP iinitial belief for dimension: " 
                  + _asset_dim_model.getStateDimensionName( ) );

        int num_vals = _asset_dim_model.getNumStateDimValues( );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model returning zero values: "
                      + _asset_dim_model.getStateDimensionName( ) );
        
        double[] belief_prob = new double[num_vals];
        
        int default_idx = _asset_dim_model.getDefaultStateIndex( );
        
        if ( default_idx < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model returning invalid default value: "
                      + _asset_dim_model.getStateDimensionName( ) );

        // We assume that each asset state dimension starts off
        // deterministically in a single possible state value.
        //
        belief_prob[default_idx] = 1.0;
 
        _initial_belief = new BeliefStateDimension( _asset_dim_model,
                                                    belief_prob,
                                                    null );

    } // method  createInitialBeliefState

    //************************************************************
    /**
     * Used to update the belief state using the given elapsed time.
     * i.e., just factor in the state transitions due to threats over
     * this time.  This returns exact results if there are less than 4
     * threats and approximate results otherwise.
     *
     * @param prev_belief initial belief state
     * @param start_time time of last belief update
     * @param end_time time desired for new belief
     * @param next_belief The next belief state (we just alter its
     * values).
     * @return the update belief state
     *
     */
    void updateBeliefStateThreatTrans( BeliefStateDimension prev_belief,
                                       long start_time,
                                       long end_time,
                                       BeliefStateDimension next_belief )
            throws BelievabilityException
    {

        // The updating of the belief state due to threats is a
        // relatively complicated process.  The complication comes
        // from the fact that we must allow multiple threats to cause
        // the same event, and even allow multiple events to affect
        // the state dimension of an asset.  We have models for how
        // the threats and events act individually, but no models
        // about how they act in combination.  Our job here is trying
        // to estimate what effect these threats and events could have
        // on the asset state dimension. (Note that threats cause
        // events in the techspec model.
        //
        // Note that making a simplifying asumption that only one
        // event will occur at a time does not help us in this
        // computation: we are not trying to adjust after the fact
        // when we *know* an event occurred; we are trying to deduce
        // which of any number of events might have occurred.  Thus,
        // we really do need to reason about combinations of events.
        //
        // The prospect of having the techspec encode the full joint
        // probability distributions for a events is not something
        // that will be managable, so we must live with the individual
        // models and make some assumptions.  At the heart of the
        // assumption sare that threats that can generate a given
        // event will do so independently, and among multiple events,
        // they too act independently.
        // 
        // Details of the calculations and assumptions are found in
        // the parts of the code where the calculations occur.
        //

        // All the complications of handling multiple threats happens
        // in this method call.
        //
        double[][] trans_matrix
                = _asset_dim_model.getThreatTransitionMatrix
                ( prev_belief.getAssetID(),
                  start_time,
                  end_time );

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Threat transition matrix: " 
                  + _asset_dim_model.getStateDimensionName() + "\n" 
                  + ProbabilityUtils.arrayToString( trans_matrix ));

        double[] prev_belief_prob = prev_belief.getProbabilityArray();
        double[] next_belief_prob = new double[prev_belief_prob.length];

        // The event transition matrix will model how the asset state
        // transitions occur.  We now need to fold this into the
        // current belief sate to produce the next belief state.
        //

        for ( int cur_state = 0; 
              cur_state < prev_belief_prob.length; 
              cur_state++ ) 
        {
            
            for ( int next_state = 0; 
                  next_state < prev_belief_prob.length; 
                  next_state++ ) 
            {

                next_belief_prob[next_state] 
                        += prev_belief_prob[cur_state]  
                        * trans_matrix[cur_state][next_state];
                
            } // for next_state
        }  // for cur_state

        // We do this before the sanity check, but maybe this isn't
        // the right thing to do. For now, it allows me to more easily
        // convert it to a string in the case where there is a
        // problem. 
        //
        next_belief.setProbabilityArray( next_belief_prob );

        // Add a sanity check to prevent bogus belief from being
        // propogated to other computations. 
        //
        double sum = 0.0;
        for ( int i = 0; i < next_belief_prob.length; i++ )
            sum += next_belief_prob[i];

        if( ! Precision.isZeroComputation( 1.0 - sum ))
            throw new BelievabilityException
                    ( "updateBeliefStateThreatTrans()",
                      "Resulting belief doesn't sum to 1.0 : "
                      + next_belief.toString() );

    } // method updateBeliefStateThreatTrans

    //************************************************************
    /**
     * Used to update the belief state using the given elapsed time.
     * i.e., just factor in the state transitions over this time.
     * Will use action and threat effects to determine new belief
     * state. 
     *
     * @param prev_belief initial belief state
     * @param start_time time of last belief update
     * @param end_time time desired for new belief
     * @param next_belief The next belief state (we just alter its
     * values).
     * @return the update belief state
     *
     */
    void updateBeliefStateActionTrans( BeliefStateDimension prev_belief,
                                       BelievabilityAction action,
                                       BeliefStateDimension next_belief )
            throws BelievabilityException
    {
        if (( prev_belief == null )
            || ( action == null )
            || ( next_belief == null ))
            throw new BelievabilityException
                    ( "updateBeliefStateActionTrans()",
                      "NULL parameter(s) sent in." );

        // If the action does not pertain to this state dimension
        // (shouldn't happen), then we assume the state remains
        // unchanged (identiy matrix).  Note that we copy the
        // probability values from prev_belief to next_belief, even
        // though the next_belief likely starts out as a clone of the
        // prev_belief.  We do this because we were afraid of assuming
        // it starts out as a clone, as this would make this more
        // tightly coupled with the specific implmentation that calls
        // this method. Only if this becomes a performance problem
        // should this be revisited.
        // 

        double[] prev_belief_prob = prev_belief.getProbabilityArray();
        double[] next_belief_prob = new double[prev_belief_prob.length];

        // The action transition matrix will model how the asset state
        // change will happen when the action is taken..
        //
        double[][] action_trans
                = _asset_dim_model.getActionTransitionMatrix( action );

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Action transition matrix: " 
                  + _asset_dim_model.getStateDimensionName() + "\n" 
                  + ProbabilityUtils.arrayToString( action_trans ));

        // We check this, but it really should never be null.
        //
        if ( action_trans == null )
            throw new BelievabilityException
                    ( "updateBeliefStateActionTrans()",
                      "Could not find action transition matrix for: "
                      + prev_belief.getAssetID().getName() );
        
        // Start the probability calculation
        //
        for ( int cur_state = 0; 
              cur_state < prev_belief_prob.length; 
              cur_state++ ) 
        {
            
            for ( int next_state = 0; 
                  next_state < prev_belief_prob.length; 
                  next_state++ ) 
            {
                next_belief_prob[next_state] 
                        += prev_belief_prob[cur_state]  
                        * action_trans[cur_state][next_state];
                
            } // for next_state
        }  // for cur_state

        // We do this before the sanity check, but maybe this isn't
        // the right thing to do. For now, it allows me to more easily
        // convert it to a string in the case where there is a
        // problem. 
        //
        next_belief.setProbabilityArray( next_belief_prob );

        // Add a sanity check to prevent bogus belief from being
        // propogated to other computations. 
        //
        double sum = 0.0;
        for ( int i = 0; i < next_belief_prob.length; i++ )
            sum += next_belief_prob[i];

        if( ! Precision.isZeroComputation( 1.0 - sum ))
            throw new BelievabilityException
                    ( "updateBeliefStateActionTrans()",
                      "Resulting belief doesn't sum to 1.0 : "
                      + next_belief.toString() );

    } // method updateBeliefStateActionTrans

    //************************************************************
    /**
     * Used to update the belief state using the given diagnosis value.
     * i.e., just factor in the observation made.
     *
     * @param prev_belief initial belief state
     * @param diagnosis_value the diagnosis to use to determine new belief
     * state
     * @param next_belief The next belief state (we just alter its values)
     * @return the update belief state
     *
     */
    void updateBeliefStateDiagnosisObs( BeliefStateDimension prev_belief,
                                        DiagnosisTrigger diagnosis,
                                        BeliefStateDimension next_belief )
            throws BelievabilityException
    {
        if (( prev_belief == null )
            || ( diagnosis == null )
            || ( next_belief == null ))
            throw new BelievabilityException
                    ( "updateBeliefStateDiagnosisObs()",
                      "NULL parameter(s) sent in." );

        double denom = 0.0;
        String diagnosis_value = diagnosis.getSensorValue();

        double[] prev_belief_prob = prev_belief.getProbabilityArray();
        double[] next_belief_prob = new double[prev_belief_prob.length];

        if ( _logger.isDebugEnabled() )
            _logger.debug( "Updating belief given sensor '"
                  + diagnosis.getSensorName()
                  + "' has sensed '" + diagnosis_value + "'");

        SensorTypeModel sensor_model 
                = _asset_dim_model.getSensorTypeModel 
                ( diagnosis.getSensorName() );
        
        if ( sensor_model == null )
            throw new BelievabilityException
                    ( "updateBeliefStateDiagnosisObs()",
                      "Cannot find sensor model for: "
                      + diagnosis.getSensorName() );
        
        // This 'obs_prob' is a matrix of conditional probabilities of
        // an observation (i.e., diagnosis) given an asset state.  The
        // first index is the observation and the second is the state.
        //
        double[][] obs_prob = sensor_model.getObservationProbabilityArray();

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Observation probabilities: " 
                  + _asset_dim_model.getStateDimensionName() + "\n" 
                  + ProbabilityUtils.arrayToString( obs_prob ));


        int obs_idx = sensor_model.getObsNameIndex( diagnosis_value );

        if ( obs_idx < 0 )
            throw new BelievabilityException
                    ( "updateBeliefStateDiagnosisObs()",
                      "Diagnosis value '" 
                      + diagnosis_value + "' not found. "
                      + diagnosis.toString() );
 
        if ( _logger.isDetailEnabled() )
            _logger.detail( "Pre-update: " 
                  + ProbabilityUtils.arrayToString( prev_belief_prob ));

        for ( int state = 0; state < prev_belief_prob.length; state++ ) 
        {

            next_belief_prob[state] 
                    = prev_belief_prob[state] * obs_prob[state][obs_idx];
            
            denom += next_belief_prob[state];
            
        } // for state

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Pre-normalization: " 
                  + ProbabilityUtils.arrayToString( next_belief_prob ));
   
        // Here we choose to ignore impossible observations, though we
        // will give a warning.  We do not want to completely abort
        // this operation, since the previous belief state will more
        // than likely have some threat transition information in it.
        // Thus, we choose to leave the belief state as is, which is
        // why we simply copy the arrays over. 
        //
        if( Precision.isZeroComputation( denom ))
        {
            if ( _logger.isWarnEnabled() )
                _logger.warn( "updateBeliefStateDiagnosisObs(): "
                        + "Diagnosis is not possible. i.e., Pr("
                        + diagnosis_value + ") = 0.0. Ignoring diagnosis.");
            next_belief.setProbabilityArray
                    ( prev_belief.getProbabilityArray());

            return;
        } // if found an impossible observation

        for( int i = 0; i < next_belief_prob.length; i++ )
            next_belief_prob[i] /= denom;

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Post-normalization: " 
                  + ProbabilityUtils.arrayToString( next_belief_prob ));

        next_belief.setProbabilityArray( next_belief_prob );

    } // method updateBeliefStateDiagnosisObs

   //************************************************************
    /**
     * Used to update the belief state using the given diagnosis value.
     * i.e., just factor in the observation made.
     *
     * @param prev_belief initial belief state
     * @param diagnosis_value the diagnosis to use to determine new belief
     * state
     * @param next_belief The next belief state (we just alter its values)
     * @return the update belief state
     *
     */
    void updateBeliefStateTrigger( BeliefStateDimension prev_belief,
                                   BeliefUpdateTrigger trigger,
                                   BeliefStateDimension next_belief )
            throws BelievabilityException
    {
        if (( prev_belief == null )
            || ( trigger == null )
            || ( next_belief == null ))
            throw new BelievabilityException
                    ( "updateBeliefStateTrigger()",
                      "NULL parameter(s) sent in." );
        
        if ( trigger instanceof DiagnosisTrigger )
            updateBeliefStateDiagnosisObs( prev_belief,
                                           (DiagnosisTrigger) trigger,
                                           next_belief );
        else if ( trigger instanceof BelievabilityAction)
            updateBeliefStateActionTrans( prev_belief,
                                          (BelievabilityAction) trigger,
                                          next_belief );
        else
            throw new BelievabilityException
                    ( "updateBeliefStateTrigger()",
                      "Unknown BeliefUpdateTrigger subclass: "
                      + trigger.getClass().getName() );
            
    } // method updateBeliefStateTrigger

    //************************************************************
    /**
     * Constructs a random belief state for this asset type.
     *
     */
    BeliefStateDimension getRandomBeliefState( )
            throws BelievabilityException
    {
        if ( _asset_dim_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getRandomBeliefState()",
                      "Asset type dimension model is NULL" );
        
        if ( _logger.isDetailEnabled() )
            _logger.detail( "\tCreating POMDP random belief for dimension: " 
                  + _asset_dim_model.getStateDimensionName( ) );

        int num_vals = _asset_dim_model.getNumStateDimValues( );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getRandomBeliefState()",
                      "Asset dimension model returning zero values: "
                      + _asset_dim_model.getStateDimensionName() );

        double[] belief_prob = new double[num_vals];
        
        ProbabilityUtils.setRandomDistribution( belief_prob );
        
        return new BeliefStateDimension( _asset_dim_model,
                                         belief_prob,
                                         null );

    } // method  getRandomBeliefState

    //************************************************************
    /**
     * Constructs a unifmrm probability s\distribution belief state
     * for this asset type state dimension.
     *
     */
    BeliefStateDimension getUniformBeliefState( )
            throws BelievabilityException
    {
        if ( _asset_dim_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getUniformBeliefState()",
                      "Asset type dimension model is NULL" );
        
        if ( _logger.isDetailEnabled() )
            _logger.detail( "\tCreating POMDP uniform belief for dimension: " 
                  + _asset_dim_model.getStateDimensionName( ) );

        int num_vals = _asset_dim_model.getNumStateDimValues( );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getUniformBeliefState()",
                      "Asset dimension model returning zero values: "
                      + _asset_dim_model.getStateDimensionName() );

        double[] belief_prob = new double[num_vals];
        
        ProbabilityUtils.setUniformDistribution( belief_prob );
        
        return new BeliefStateDimension( _asset_dim_model,
                                         belief_prob,
                                         null );

    } // method getUniformBeliefState

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetTypeDimensionModel _asset_dim_model;

    private BeliefStateDimension _initial_belief;
    
} // class POMDPAssetDimensionModel
