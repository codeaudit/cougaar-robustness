/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPModelManager.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/POMDPModelManager.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-06-18 00:16:39 $
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

import java.util.Hashtable;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

/**
 * This is the main class that will manage all the POMDP models.  Each
 * asset type will have its own model, so we need a cotainer class to
 * manage and access them all. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-06-18 00:16:39 $
 */
public class POMDPModelManager 
        extends Loggable implements POMDPModelInterface
{

    // We build individual POMDP models on demand.  When tech
    // spec/system information changes, we do not immendiately
    // recreate the model, but rather jujst mark the model as invalid.
    // On the first attempt to access the model, if it is invalid or
    // non-existent, then it will be created.

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Main constructor that will get all the relevant information
     * from the local model manager.
     *
     * @param model_manager The local model manager containing the
     * local model with the necessary information.
     */
    public POMDPModelManager( ModelManagerInterface model_manger )
    {

        _model_manager = model_manger;

    }  // constructor POMDPModelManager

    //************************************************************
    // POMDPModelInterface methods
    //************************************************************

    //************************************************************
    /**
     * Get the a priori probability that the indicated asset type.
     *
     * @param asset_type The type of the asset
     * @return A new belief states set to default values, or null if
     * something goes wrong.  
     *
     */
    public BeliefState getInitialBeliefState( AssetType asset_type )
            throws BelievabilityException
    {
        if ( asset_type == null )
            throw new BelievabilityException
                    ( "POMDPModelManager.getInitialBeliefState()",
                      "NULL asset type passed in." );

        POMDPAssetModel pomdp_model = getModel( asset_type );
        
        return pomdp_model.getInitialBeliefState();

    } // method getInitialBeliefState

    //************************************************************
    /**
     * Get the a priori probability for the indicated asset id.
     *
     * @param asset_id The ID of the asset
     * @return A new belief states set to default values, or null if
     * something goes wrong.  
     *
     */
    public BeliefState getInitialBeliefState( AssetID asset_id )
           throws BelievabilityException
     {
        // The initial belief is only based on the asset type, so is
        // the same for all assets of a given type.

        if ( asset_id == null )
            return null;

        return getInitialBeliefState( asset_id.getType() );

    } // method getInitialBeliefState

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
    public BeliefState updateBeliefState( BeliefState start_belief,
                                          BeliefUpdateTrigger diagnosis )
            throws BelievabilityException
    {
        if (( start_belief == null )
            || ( diagnosis == null ))
            throw new BelievabilityException
                    ( "POMDPModelManager.updateBeliefState()",
                      "NULL parameters(s) passed in." );

        POMDPAssetModel pomdp_model
                = getModel( start_belief.getAssetType() );


        return pomdp_model.updateBeliefState( start_belief,
                                              diagnosis );

    } // method updateBeliefState

    //************************************************************
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
            throws BelievabilityException
    {
        if ( start_belief == null )
            throw new BelievabilityException
                    ( "POMDPModelManager.updateBeliefState()",
                      "NULL starting belief passed in." );

        POMDPAssetModel pomdp_model
                = getModel( start_belief.getAssetType() );


        return pomdp_model.updateBeliefState( start_belief,
                                              time );

    } // method updateBeliefState

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Get a random belief state consistent with the asset type sent
     * in.
     *
     * @param asset_type The type of the asset
     * @return A new belief states set to random values, or null if
     * something goes wrong.  
     *
     */
    public BeliefState getRandomBeliefState( AssetType asset_type )
            throws BelievabilityException
    {
        if ( asset_type == null )
            throw new BelievabilityException
                    ( "POMDPModelManager.getRandomBeliefState()",
                      "NULL asset type passed in." );

        POMDPAssetModel pomdp_model = getModel( asset_type );
        
        return pomdp_model.getRandomBeliefState();

    } // method getRandomBeliefState

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Retrieves the POMDP model for the given asset type.  It will
     * create the model if it does not exist, or if something has
     * changed in the underlying tech spec models since it was
     * created. 
     *
     * @param asset_type  The asset type ot build the model for
     */
    protected POMDPAssetModel getModel( AssetType asset_type )
            throws BelievabilityException
    {
        // This does an on-demand creation opf POMDP models

        POMDPAssetModel model
                = (POMDPAssetModel) _pomdp_model_set.get
                ( asset_type.getName() );

        // Model has to exist and be valid in order to use it.
        //
        if (( model != null ) && ( model.isValid() ))
            return model;

        // If the model has become invalid, then we remove it before
        // recreating it.
        //
        if (( model != null ) && ( ! model.isValid()))
            _pomdp_model_set.remove( asset_type.getName() );
        
        return createModel( asset_type );

    } // method getModel

    //************************************************************
    /**
     * Creates the model for the given asset type and adds it to the
     * set of models this class is managing.
     *
     * @param asset_type  The asset type ot build the model for
      */
    protected POMDPAssetModel createModel( AssetType asset_type )
            throws BelievabilityException
    {
        // Method implementation comments go here ...

        // First we find the AssetTypeModel to set up the state space
        // and initial belief for the asset type.
        //
        AssetTypeModel at_model
                = _model_manager.getAssetTypeModel( asset_type );

        if ( at_model == null )
            throw new BelievabilityException
                    ( "POMDPModelManager.createModel()",
                      "Asset type does not exist in ModelManager" );
        
        
        POMDPAssetModel pomdp_model
                = new POMDPAssetModel( at_model );

        _pomdp_model_set.put( asset_type.getName(), pomdp_model );

        return pomdp_model;

    } // method createModel

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private ModelManagerInterface _model_manager;

    // This contains all the POMDPModel objects for assets, keyed on
    // the asset type name.  
    //
    private Hashtable _pomdp_model_set = new Hashtable();

} // class POMDPModelManager
