/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPAssetDimensionModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/POMDPAssetDimensionModel.java,v $
 * $Revision: 1.2 $
 * $Date: 2004-05-28 20:01:17 $
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
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
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
    POMDPAssetDimensionModel( AssetTypeModel at_model,
                              int dim_idx )
            throws BelievabilityException
    {
        if ( at_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.POMDPAssetDimensionModel()",
                      "Asset model is NULL" );
        
        this._asset_type_model = at_model;

        logDebug( "\tCreating POMDP model for dimension: " 
                  + at_model.getStateDimName( dim_idx ) );

        this._dim_idx = dim_idx;

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
        if ( _asset_type_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model is NULL" );
        
        logDebug( "\tCreating POMDP iinitial belief for dimension: " 
                  + _asset_type_model.getStateDimName( _dim_idx ) );

        int num_vals = _asset_type_model.getNumStateDimValues( _dim_idx );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model returning zero values" );
        
        double[] belief_prob = new double[num_vals];
        
        int default_idx = _asset_type_model.getDefaultStateIndex( _dim_idx );
        
        if ( default_idx < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.createInitialBeliefState()",
                      "Asset model returning invalid default value" );

        // We assume that each asset state dimension starts off
        // deterministically in a single possible state value.
        //
        belief_prob[default_idx] = 1.0;
 
        _initial_belief = new BeliefStateDimension( _asset_type_model,
                                                    _dim_idx,
                                                    belief_prob );

    } // method  createInitialBeliefState

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
    void updateBeliefStateObs( BeliefStateDimension prev_belief,
                               String diagnosis_value,
                               BeliefStateDimension next_belief )
            throws BelievabilityException
    {
        
        // FIXME: We do nothing for now but log a message
        //
        logError( "updateBeliefStateObs() not implemented: values are bogus.");

    } // method updateBeliefStateObs

    //************************************************************
    /**
     * Used to update the belief state using the given elapsed time.
     * i.e., just factor in the state transitions over this time.
     *
     * @param prev_belief initial belief state
     * @param delta_time Time elapsed since the last belie update
     * @param next_belief The next belief state (we just alter its values)
     * @return the update belief state
     *
     */
    void updateBeliefStateTrans( BeliefStateDimension prev_belief,
                                 long delta_time,
                                 BeliefStateDimension next_belief )
            throws BelievabilityException
    {
        // FIXME: We do nothing for now but log a message
        //
        logError( "updateBeliefStateTrans() not implemented: values are bogus.");

    } // method updateBeliefState

    //************************************************************
    /**
     * Constructs a random belief state for this asset type.
     *
     */
    BeliefStateDimension getRandomBeliefState( )
            throws BelievabilityException
    {
        if ( _asset_type_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getRandomBeliefState()",
                      "Asset model is NULL" );
        
        logDebug( "\tCreating POMDP random belief for dimension: " 
                  + _asset_type_model.getStateDimName( _dim_idx ) );

        int num_vals = _asset_type_model.getNumStateDimValues( _dim_idx );
        
        if ( num_vals < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetDimensionModel.getRandomBeliefState()",
                      "Asset model returning zero values" );

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
        
        return new BeliefStateDimension( _asset_type_model,
                                         _dim_idx,
                                         belief_prob );

    } // method  getRandomBeliefState

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetTypeModel _asset_type_model;

    // The state dimension index into the local models
    private int _dim_idx;

    private BeliefStateDimension _initial_belief;

    
    private static Random _rand = new Random();

} // class POMDPAssetDimensionModel
