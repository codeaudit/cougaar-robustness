/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPAssetDimensionModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/POMDPAssetDimensionModel.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-06-09 17:32:49 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import java.util.Random;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

/**
 * This class represents a particular instance of a POMDP model for a
 * given asset type. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.3 $Date: 2004-06-09 17:32:49 $
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

        logDebug( "\tCreating POMDP model for dimension: " 
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
        
        logDebug( "\tCreating POMDP iinitial belief for dimension: " 
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

        // The event probability needs to factor in all the threats
        // that could affect this state dimension.
        // 
        double event_prob
                =  _asset_dim_model.getEventProbability
                ( prev_belief.getAssetID(),
                  start_time,
                  end_time );

        // If there are no threats, then we assume that the state will
        // not change.  Note that we copy the probability values from
        // prev_belief to next_belief, even though the next_belief
        // likely starts out as a clone of the prev_belief.  We do
        // this because we were afraid of assuming it starts out as a
        // clone, as this would make this more tightly coupled with
        // the specific implmentation that calls this method. Only if
        // this becomes a performance problem should this be
        // revisited.
        // 
        if ( event_prob <= 0.0 )
        {
            next_belief.setProbabilityArray
                    ( prev_belief.getProbabilityArray() );
            return;
        }  // if no threats exist

        double[] prev_belief_prob = prev_belief.getProbabilityArray();
        double[] next_belief_prob = new double[prev_belief_prob.length];
        double[][] event_trans
                = _asset_dim_model.getEventTransitionMatrix
                ( prev_belief.getAssetID() );

        // If we get a non-zero event probability, then we can assume
        // there is at least one threat affecting this asset, and
        // thus, must be an event description defining the state
        // transitions for when the event occurs.  Thus, we check
        // this, but it really should never be null.
        //
        if ( event_trans == null )
            throw new BelievabilityException
                    ( "updateBeliefStateThreatTrans()",
                      "Could not find event transition matrix for: "
                      + prev_belief.getAssetID().getName() );

        // The event transition matrix will model how the asset state
        // transitions occur *if* the event occurs.  Implied in this
        // is that the state does *not* change when the event does not
        // occur.  We have to consider this in our belief update
        // calculation (see below).
        //

        for ( int cur_state = 0; 
              cur_state < prev_belief_prob.length; 
              cur_state++ ) 
        {
            
            for ( int next_state = 0; 
                  next_state < prev_belief_prob.length; 
                  next_state++ ) 
            {
                // If the threat does not result in the event
                // happening, then we assume there is no state change.
                // Thus, most state transitions under the "event did
                // not happen" condition will have probability zero,
                // except...
                //
                double non_event_trans_prob = 0.0;
                
                // ...for the self-transitions, since absence of the
                // event means we assume that the asset remains in its
                // current state.
                //
                if ( cur_state == next_state )
                    non_event_trans_prob = 1.0;

                // If the event does occur, then the state transition
                // is dictated by the state transition matrix
                // associated with this event.
                //
                double trans_prob 
                        = event_prob * event_trans[cur_state][next_state]
                        + ( 1.0 - event_prob) * non_event_trans_prob;

                next_belief_prob[next_state] 
                        += prev_belief_prob[cur_state]  * trans_prob;
                
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

        if( ! Precision.isZero( 1.0 - sum ))
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
        // FIXME: implenent this method
        //
        logError( "updateBeliefStateActionTrans() not implemented." );

        // If the action does not pertain to this state dimension
        // (shouldn't happen), then we assume the state remains
        // unchanged (identiy matrix.  Note that we copy the
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

        if( ! Precision.isZero( 1.0 - sum ))
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
                                        BelievabilityDiagnosis diagnosis,
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
        String diagnosis_value = diagnosis.getDiagnosisValue();

        double[] prev_belief_prob = prev_belief.getProbabilityArray();
        double[] next_belief_prob = new double[prev_belief_prob.length];

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
        int obs_idx = sensor_model.getObsNameIndex( diagnosis_value );

        for ( int state = 0; state < prev_belief_prob.length; state++ ) 
        {

            next_belief_prob[state] 
                    = prev_belief_prob[state] * obs_prob[obs_idx][state];
            
            denom += next_belief_prob[state];
            
        } // for state

        if( Precision.isZero( denom ))
            throw new BelievabilityException
                    ( "updateBeliefStateDiagnosisObs()",
                      "Diagnosis is not possible. i.e., Pr("
                      + diagnosis_value + ") = 0.0.");
        
        for( int i = 0; i < next_belief_prob.length; i++ )
            next_belief_prob[i] /= denom;

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
        
        if ( trigger instanceof BelievabilityDiagnosis)
            updateBeliefStateDiagnosisObs( prev_belief,
                                           (BelievabilityDiagnosis) trigger,
                                           next_belief );
        else if ( trigger instanceof BelievabilityDiagnosis)
            updateBeliefStateActionTrans( prev_belief,
                                          (BelievabilityAction) trigger,
                                          next_belief );
        else
            throw new BelievabilityException
                    ( "updateBeliefStateTrigger()",
                      "Unknown BeliefUpdateTrigger type: "
                      + trigger );
            
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
        
        logDebug( "\tCreating POMDP random belief for dimension: " 
                  + _asset_dim_model.getStateDimensionName( ) );

        int num_vals = _asset_dim_model.getNumStateDimValues( );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getRandomBeliefState()",
                      "Asset dimension model returning zero values: "
                      + _asset_dim_model.getStateDimensionName() );

        double[] belief_prob = new double[num_vals];
        
        double total = 0.0;
        for ( int idx = 0; idx < num_vals; idx++ )
        {
            belief_prob[idx] = _rand.nextDouble();
             total += belief_prob[idx];
        }

        // Normalize
        for ( int idx = 0; idx < num_vals; idx++ )
            belief_prob[idx] /= total;
        
        return new BeliefStateDimension( _asset_dim_model,
                                         belief_prob,
                                         null );

    } // method  getRandomBeliefState

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetTypeDimensionModel _asset_dim_model;

    private BeliefStateDimension _initial_belief;
    
    private static Random _rand = new Random();


} // class POMDPAssetDimensionModel
