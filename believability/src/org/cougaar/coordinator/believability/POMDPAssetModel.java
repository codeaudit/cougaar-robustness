/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPAssetModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/POMDPAssetModel.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-20 21:39:49 $
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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;

/**
 * This class represents a particular instance of a POMDP model for a
 * given asset type. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-20 21:39:49 $
 *
 */
class POMDPAssetModel extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor which extracts all the needed information out
     * of the AssetTypeModel to build the POMDP model for it.
     *
     * @param at_model The asset type model for which this is the
     * POMDP model for
     */
    POMDPAssetModel( AssetTypeModel at_model )
            throws BelievabilityException
    {
        
        if ( at_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetModel.POMDPAssetModel()",
                      "AssetType model is NULL" );

        logDebug( "Creating POMDP model for asset type: " 
                  + at_model.getName() );
        
        this._asset_type_model = at_model;

        int num_dims = at_model.getNumStateDims();

        if ( num_dims < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetModel.POMDPAssetModel()",
                      "AssetType model reporting zero state dimensions" );

        _dimension_pomdp_model 
                = new POMDPAssetDimensionModel[num_dims];

        // A POMDPAssetModel is nothing more than an array of simpler
        // POMDP models, one for each asset state dimension.
        //
        for ( int dim_idx = 0; dim_idx < num_dims; dim_idx++ )
        {

            // We assume that these individual models will handle
            // accessing the sensor models as needed.
            //
            _dimension_pomdp_model[dim_idx]
                    = new POMDPAssetDimensionModel( at_model,
                                                    dim_idx );
        } // for dim_idx

        // This just gathers all the individual state dimension
        // initial beliefs into a single package for this model.
        //
        createInitialBeliefState();

        logDebug( "Finished creating POMDP model for asset type: " 
                  + _asset_type_model.getName() );
        
        setValidity( true );

    }  // constructor POMDPAssetModel

    //************************************************************
    // Simple accessors
    //

    BeliefState getInitialBeliefState() { return _initial_belief; }

    //************************************************************
    /**
     * Retrieve the component model for the state dimension named.
     *
     * @param state_dim_name The name of the state dimension whose
     * POMDP model should be returned.
     */
    POMDPAssetDimensionModel getPOMDPAssetDimensionModel
            ( String state_dim_name )
            throws BelievabilityException
    {
        // Method implementation comments go here ...
        int dim_idx = _asset_type_model.getStateDimIndex( state_dim_name );

        if ( dim_idx < 0 )
            throw new BelievabilityException
                    ( "POMDPAssetModel.getPOMDPAssetDimensionModel()",
                      "State dimension invalid" );

        return _dimension_pomdp_model[dim_idx];

    } // method getPOMDPAssetDimensionModel
            
    //************************************************************
    /**
     * Constructs the initial belief state for this asset type.
     *
     */
    void createInitialBeliefState( )
            throws BelievabilityException
    {
        // A belief state is a composite of a number of belief state
        // sfor each state dimension  of the asset, so here we do the
        // loop over state dimensions, while the
        // POMDPAssetDimensionModel handles the individual belief
        // states.
        //
        if ( _asset_type_model == null )
            throw new BelievabilityException
                    ( "POMDPAssetModel.createInitialBeliefState()",
                      "AssetType model is NULL" );
        
        logDebug( "Creating POMDP initial belief for asset type: " 
                  + _asset_type_model.getName() );
        
        _initial_belief = new BeliefState( _asset_type_model );
        
        for ( int dim_idx = 0; 
              dim_idx < _dimension_pomdp_model.length; 
              dim_idx++ )
        {
            
            _initial_belief.addBeliefStateDimension
                    ( _dimension_pomdp_model[dim_idx].getInitialBeliefState());

        }  // for dim_idx

    } // method createInitialBeliefState

    //************************************************************
    /**
     * Used to update the belief state to the present time.
     *
     * @param start_belief initial belief state
     * @param time the time to compute the new belief state to.
     * @return the update belief state
     *
     */
    BeliefState updateBeliefState( BeliefState start_belief, 
                                   long time )
            throws BelievabilityException
    {
        if ( start_belief == null )
            throw new BelievabilityException
                    ( "POMDPModelManager.updateBeliefState()",
                      "NULL starting belief passed in." );

        BeliefState next_belief = (BeliefState) start_belief.clone();
        
        // Set this to null to indicate that no diagnosis was
        // responsible for this update.
        //
        next_belief.setDiagnosis( null );

        // FIXME: I do not think this is the right value to set here.
        // 
        next_belief.setTimestamp( time );

        // FIXME: verify that this is the corect changhe in time.  In
        // particular, make sure it does not need to be computed on a
        // state dimension by state dimnesion basis.
        //
        long delta_time = time - start_belief.getTimestamp();

        // Here we will be updating all the state dimensions.
        //

        for ( int dim_idx = 0;
              dim_idx < _dimension_pomdp_model.length;
              dim_idx++ )
        {
            POMDPAssetDimensionModel pomdp_model_dim
                    = _dimension_pomdp_model[dim_idx];

            String state_dim_name = _asset_type_model.getStateDimName(dim_idx);

            // ..and then the appropriate start belief state dimension...
            BeliefStateDimension start_belief_dim
                    = start_belief.getBeliefStateDimension( state_dim_name );
            
            // ...and then the approrpiate next belief state dimension...
            BeliefStateDimension next_belief_dim
                    = next_belief.getBeliefStateDimension( state_dim_name );

            // ...then finally we do the belief update proper.
            pomdp_model_dim.updateBeliefStateTrans( start_belief_dim,
                                                    delta_time,
                                                    next_belief_dim );

        } // for dim_idx

        return next_belief;

    } // method updateBeliefState

    //************************************************************
    /**
     * Used to update the belief state using the given diagnosis.
     *
     * @param start_belief initial belief state
     * @param diagnosis the diagnosis to use to determine new belief
     * state
     * @return the update belief state
     *
     */
    BeliefState updateBeliefState( BeliefState start_belief,
                                   BelievabilityDiagnosis diagnosis )
            throws BelievabilityException
    {
        if (( start_belief == null )
            || ( diagnosis == null ))
            throw new BelievabilityException
                    ( "POMDPModelManager.updateBeliefState()",
                      "NULL parameters(s) passed in." );
        
        // All state dimensions will be updated to the currrent
        // diagnosis time, factopring in the state transitions due to
        // threats.  We do all this first, and then we will go back
        // and factor in the observation from the diagfnosis for the
        // lone state dimension that the diagnosis pertains to.
        //
        // FIXME: Is the above staement true?  Or should we keep
        // separate timestamps for each state dimension?  Should we be
        // able to handle simultaneous diagnoses?
        //

        // This will create a new belief state with all state
        // dimensions belief adjusted for the passage of time (state
        // transitions).
        //
        // FIXME: Is this the right timestamp to use?
        //
        BeliefState next_belief 
                = updateBeliefState( start_belief,
                                     diagnosis.getLastAssertedTimestamp() );

        // Now we go and do the single state dimension updtae to
        // factro in the observation/diagnosis.
        //
        next_belief.setDiagnosis( diagnosis );

        // Next we need to find the state dimension that this diagnosis
        // is relevanmt to.
        DiagnosisTechSpecInterface diag_ts = diagnosis.getDiagnosisTechSpec();
        String state_dim_name = diag_ts.getStateDimension();

        // First, fetch the appropriate POMDP model for this dimension...
        POMDPAssetDimensionModel pomdp_model_dim
                = getPOMDPAssetDimensionModel( state_dim_name );
        
        // ..and then the appropriate start belief state dimension...
        BeliefStateDimension start_belief_dim
                = start_belief.getBeliefStateDimension( state_dim_name );

        // ...and then the appropriate next belief state dimension...
        BeliefStateDimension next_belief_dim
                = next_belief.getBeliefStateDimension( state_dim_name );

        // ...then finally we do the belief update proper.
        pomdp_model_dim.updateBeliefStateObs( start_belief_dim,
                                              diagnosis.getDiagnosisValue(),
                                              next_belief_dim );

        return next_belief;

    } // method updateBeliefState

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetTypeModel _asset_type_model;

    private POMDPAssetDimensionModel[] _dimension_pomdp_model;

    private BeliefState _initial_belief;

} // class POMDPAssetModel
