/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManager.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManager.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-06-18 00:16:38 $
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisProbability;
import org.cougaar.coordinator.techspec.DiagnosisProbability.DiagnoseAs;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * This is the main container class that handles acces to all local
 * models. It received data input via the TechSpecManagerInterface,
 * and provides information via the ModelManagerInterface. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-06-18 00:16:38 $
 *
 */
public class ModelManager extends Loggable
        implements ModelManagerInterface
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    public ModelManager( )
    {
        super();
    }  // constructor ModelManager

    //************************************************************
    // ModelManagerInterface interface methods
    //************************************************************

    //----------------------------------------
    // Model accessor methods
    //----------------------------------------

    //************************************************************
    /**
     * docs here...
     */
    public double[][] getAssetUtilities( AssetType asset_type,
                                         AssetStateDimension state_dim )
            throws BelievabilityException
    {
        logDebug( "getAssetUtilities() for state dim: " 
                  + state_dim.getStateName() );

        AssetTypeModel at_model 
                = (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model == null )
        {
            throw new BelievabilityException
                    ( "ModelManager.getAssetUtilities()",
                      "Cannot find asset type model." );
        }

        return at_model.getUtilities( state_dim.getStateName() );

     } // method  getAssetUtilities

    //************************************************************
    /**
     * Will get the asset utilities and compute the dot-product with
     * the weights in the MAUWeightModel. 
     */
    public double[] getWeightedAssetUtilities
            ( AssetType asset_type,
              AssetStateDimension state_dim  )
            throws BelievabilityException
    {

        double[][] asset_util = getAssetUtilities( asset_type,
                                                   state_dim );

        double[] mau_weight = _mau_weight_model.getWeights();

        double[] weighted_util 
                = new double[asset_util.length];
        
        for ( int state_idx = 0; 
              state_idx < weighted_util.length; 
              state_idx++ )
        {
            weighted_util[state_idx] = 0.0;

            for ( int weight_idx = 0; 
                  weight_idx < MAUWeightModel.NUM_WEIGHTS;
                  weight_idx++ )
            {
                weighted_util[state_idx] 
                        += asset_util[state_idx][weight_idx] 
                        * mau_weight[weight_idx];

            } // for weight_idx
        } // for state_idx

        return weighted_util;

    } // method  getMAUWeightedAssetUtilities

    //************************************************************
    /**
     * For setting MAU weights to new values.
     *
     * @param mau_weights The new weights to be set.
     */
    public void setMAUWeights( double[] mau_weights )
            throws BelievabilityException
    {
        _mau_weight_model.setWeights( mau_weights );

    } // method  getMAUWeights

     //************************************************************
    /**
     * docs here...
     */
   public double[] getMAUWeights()
    {
        return _mau_weight_model.getWeights();
    } // method  getMAUWeights

    //************************************************************
    /**
     * docs here...
     */
    public long getMaxSensorLatency( AssetType asset_type )
            throws BelievabilityException
    {

        logDebug( "==== getMaxSensorLatency() ====" );

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model == null )
        {
            throw new BelievabilityException
                    ( "ModelManager.getMaxSensorLatency()",
                      "Cannot find asset type model." );
        }

        return at_model.getMaxSensorLatency( );

    } // method getMaxSensorLatency


    //************************************************************
    /**
     * docs here...
     */
    public POMDPModelInterface getPOMDPModel()
    {
        return _pomdp_manager;
    } // method getPOMDPModel

    //----------------------------------------
    // Model mutator methods
    //----------------------------------------

    //--------------------
    // Adding methods
    //--------------------

    //************************************************************
    /**
     * Routine to handle the addition of a new sensor type.  This
     * routine is also responsible for reading the asset models and
     * populating the local models with the necessary information.
     *
     * @param diag_ts The sensor model to be added
     */
    public void addSensorType( DiagnosisTechSpecInterface diag_ts )
    {
        logDebug( "Starting call to: addSensorType()" );

        if ( diag_ts == null )
        {
            logError( "NULL found for DiagnosisTechSpecInterface.");
            return;
        }

        // DO not make the assumption that the asset type model exists
        // for this sensor.  Try to add and/or fetch the model first.
        //
        AssetType asset_type = diag_ts.getAssetType();
        AssetTypeModel asset_type_model = getOrAndAssetType( asset_type );

        if ( asset_type_model == null )
            return;

        logDebug( "==== Add Sensor Type ====" );
        
        try
        {
            
            SensorTypeModel s_model 
                    = asset_type_model.addSensorTypeModel( diag_ts );
            
            logDebug( "Added New SensorTypeModel:\n" 
                      + s_model.toString() );
                
        }
        catch (BelievabilityException be)
        {
            logError( "Cannot add SensorTypeModel "
                      +  diag_ts.getName() + ": " + be.getMessage() );
            return;
        }

    } // method addSensorType

    //************************************************************
    /**
     * Adding a new threat type to the local models
     *
     * @param threat_model Threat type to be added
     */
    public void addThreatType( ThreatModelInterface threat_model )
    {
        logDebug( "addThreatType() called" );

        if ( threat_model == null )
        {
            logError( "addThreatType() sent NULL ThreatModelInterface. " );
            return;
        }

        addThreatVariation( threat_model );

    } // method addThreatModel

    //************************************************************
    /**
     * Adding a new actuator type to the local models.
     *
     * @param actuator_ts Actuator type to be added
     */
    public void addActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDebug( "Starting call to: addActuatorType()" );

        if ( actuator_ts == null )
        {
            logError( "NULL found for ActionTechSpecInterface.");
            return;
        }

        // Do not make the assumption that the asset type model exists
        // for this actuator.  Try to add and/or fetch the model first.
        //
        AssetType asset_type = actuator_ts.getAssetType();
        AssetTypeModel asset_type_model = getOrAndAssetType( asset_type );

        if ( asset_type_model == null )
            return;

        logDebug( "==== Add Actuator Type ====" );
        
        try
        {
            
            ActuatorTypeModel a_model
                    = asset_type_model.addActuatorTypeModel( actuator_ts );
            
            logDebug( "Added New ActuatorTypeModel:\n" 
                      + a_model.toString() );
                
        }
        catch (BelievabilityException be)
        {
            logError( "Cannot add ActuatorTypeModel: " + be.getMessage() );
            return;
        }

    } // method addActuatorType

    //--------------------
    // Updating methods
    //--------------------

    //************************************************************
    /**
     *  Changing the properties of a sensor type in the local models.
     *
     * @param diag_ts The sensor type to be changed/     
     */ 
    public void updateSensorType( DiagnosisTechSpecInterface diag_ts )
    {
        logDebug( "==== Update Sensor Type ====" );

        logDebug( "** NOT IMPLEMENTED ** updateSensorType()" );

    } // method updateSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void updateThreatType( ThreatModelInterface threat_model )
    {
        logDebug( "==== Update Threat Type ====" );

        logDebug( "Ignoring update. This should be handled by "
                  + "call to handleThreatModelChange()" );
    } // method updateThreatType

    //************************************************************
    /**
     * docs here...
     */
    public void updateActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDebug( "==== Update Actuator Type ====" );

        logDebug( "** NOT IMPLEMENTED ** updateActuatorType()" );
    } // method updateActuatorType

    //--------------------
    // Removing methods
    //--------------------

    //************************************************************
    /**
     * Removing a sensor type from the local models.  Logs errors if
     * the type cannot be found in the local model or if there is
     * any other problem with its removal. 
     *
     * @param diag_ts The sensor type to be removed.
     */
    public void removeSensorType( DiagnosisTechSpecInterface diag_ts )
    {
        logDebug( "==== Remove Sensor Type ====" );

        logDebug( "** NOT IMPLEMENTED ** removeSensorType()" );

    } // method removeSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void removeThreatType( ThreatModelInterface threat_model )
    {
        logDebug( "==== Remove Threat Type ====" );

        logDebug( "** NOT IMPLEMENTED ** removeThreatType()" );
    } // method removeThreatModel

    //************************************************************
    /**
     * docs here...
     */
    public void removeActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDebug( "==== Remove Actuator Type ====" );

        logDebug( "** NOT IMPLEMENTED ** removeActuatorType()" );
    } // method removeActuatorType

    //************************************************************
    /**
     * Handle the situation where a threat model has changed the set
     * of assets it pertains to.  This could be the addition and/or
     * removal of assets.
     * @param tm_change The object that defines the nature of the
     * threat change
     */
    public void handleThreatModelChange( ThreatModelChangeEvent tm_change )
    {
        // We just relay the change to the appropriate AssetTypeModel
        // objects for each of the affected assets.
        //

        logDebug( "==== Handle Threat Model Change ====" );

        ThreatModelInterface threat_model = tm_change.getThreatModel();

        // This is a list of AssetTechSpecInterface objects
        Vector asset_list = tm_change.getAddedAssets();
        Enumeration enum = asset_list.elements();
        while ( enum.hasMoreElements() )
        { 
            AssetTechSpecInterface asset_ts
                    = (AssetTechSpecInterface) enum.nextElement();

            AssetID asset_id = asset_ts.getAssetID();

            AssetTypeModel at_model
                    = getAssetTypeModel( asset_ts.getAssetType() );

             try
            {
                at_model.handleThreatModelChange
                    ( threat_model, 
                      asset_id,
                      AssetTypeModel.THREAT_CHANGE_ADD );
            }
            catch (BelievabilityException be)
            {
                logError( "Problem adding threat applicability for: "
                          + asset_id.getName() );
            }

        } // while asset list of added assets

        // Repeat sam logic as above, but this time for removed
        // assets.

        asset_list = tm_change.getRemovedAssets();
        enum = asset_list.elements();
        while ( enum.hasMoreElements() )
        { 
            AssetTechSpecInterface asset_ts
                    = (AssetTechSpecInterface) enum.nextElement();

            AssetID asset_id = asset_ts.getAssetID();

            AssetTypeModel at_model
                    = getAssetTypeModel( asset_ts.getAssetType() );

            try
            {
                at_model.handleThreatModelChange
                    ( threat_model, 
                      asset_id,
                      AssetTypeModel.THREAT_CHANGE_REMOVE );
            }
            catch (BelievabilityException be)
            {
                logError( "Problem removing threat applicability for: "
                          + asset_id.getName() );
            }

        } // while asset list of removed assets

        asset_list = tm_change.getRemovedAssets();

    } // method handleThreatModelChange



    //************************************************************
    /**
     * For retrieving an AssetTypeModel.
     *
     * @param asset_type Source for the asset name
     */
    public AssetTypeModel getAssetTypeModel( AssetType asset_type )
    {
        return  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );

    } // method getAssetTypeModel


    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Adds a new asset type to the local model.  If this asset type
     * already exists, then that model is returns.  To change asset type
     * information use the 'updateAssetType'
     *
     * @param asset_type The asset type to be added.
     */
    protected AssetTypeModel getOrAndAssetType( AssetType asset_type )
    {
        if ( asset_type == null )
        {
            logError( "NULL found for asset_type.");
            return null;
        }

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model != null )
        {
            logDebug( "Asset model found: " + at_model.getName() );
            return at_model;
        }

        logDebug( "==== Add Asset Type ====" );

        try
        {
            at_model = new AssetTypeModel( asset_type );
                
            _asset_type_container.put( asset_type.getName(),
                                       at_model );

            logDebug( "Added New AssetTypeModel:\n"
                      + at_model.toString() );
            
        }
        catch (BelievabilityException be)
        {
            logError( "Cannot add AssetTypeModel "
                      +  asset_type.getName() + ": " + be.getMessage() );
            return null;
        }

        return at_model;

    } // method getOrAndAssetType

    //************************************************************
    /**
     *  Changing the properties of an asset type in the local models.
     *
     * @param asset_type The asset type to be changed/
     */ 
    protected void updateAssetType( AssetType asset_type )
    {
        logDebug( "==== Update Asset Type ====" );

        logDebug( "** NOT IMPLEMENTED ** updateAssetType()" );

    } // method updateAssetType

    //************************************************************
    /**
     * Removing an asset type from the local models.  Logs errors if
     * the asset type cannot be found in the local model or if there is
     * any other problem with its removal. 
     *
     * @param asset_type The asset type to be removed.
     */
    protected void removeAssetType( AssetType asset_type )
    {
        logDebug( "==== Remove Asset Type ====" );

        logDebug( "** NOT IMPLEMENTED ** removeAssetType()" );

    } // method removeAssetStateDimension

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // These are the local models this class manages.
    //
    private Hashtable _asset_type_container = new Hashtable();

    private MAUWeightModel _mau_weight_model = new MAUWeightModel();
    
    private POMDPModelManager _pomdp_manager = new POMDPModelManager(this);

    //************************************************************
    /**
     * Creates a new threat variation model from a threat description,
     * which must have a valid event description contined in it.
     *
     * @param threat_desc The threat description to add.
     */
    private void addThreatVariation( ThreatModelInterface threat_mi )
    {
        // Important note: There may be many ThreatModelInterface
        // objects with the same name published.  Each will have a
        // different filter for the vulerable assets that the threat
        // is defining.  Locally in the believability package, we only
        // keep one ThreatRootModel for a given threat name, but many
        // ThreatVariationModels for each. We also keep the mapping
        // from AssetID to the threat models based on the vulerability
        // filter, but that is handled by the asset type model for the
        // initial creation and elsewhere for the dynamically changing
        // membership of which threats affect which assets.
        //

        logDebug( "Starting call to: addThreatVariation()" );

        // Do not make the assumption that the asset type model
        // exists.  First attempt to add, or simply retrieve the asset
        // model.  
        //
        AssetType asset_type 
                = threat_mi.getThreatDescription().getAffectedAssetType();
        AssetTypeModel asset_type_model = getOrAndAssetType( asset_type );

        if ( asset_type_model == null )
            return;

        logDebug( "==== Add Threat Variation ====" );

        try
        {
            ThreatVariationModel threat_var
                    = asset_type_model.addThreatVariationModel( threat_mi );
            
            logDebug( "Added New ThreatVariationModel:\n"
                      + threat_var.getName() );
            
        }
        catch (BelievabilityException be)
        {
            logError( "Cannot add ThreatModelInterface "
                      + threat_mi.getName() + ": " 
                      + be.getMessage() );
            return;
        }

    } // method addThreatVariation

    //------------------------------------------------------------
    // Test code section
    //------------------------------------------------------------


    // FIXME: This is temporary for testing. Please remove me.
    //
    AssetType _test_asset_type;
    void testInitialBeliefStates()
    {
        logDebug( "\n==== TESTING INITIAL BELIEF STATES ====\n" );

        try
        {
            BeliefState initial_belief
                    = _pomdp_manager.getInitialBeliefState
                    ( _test_asset_type );
            
            logDebug( "Initial Belief:\n"
                           + initial_belief.toString() );
        }
        catch (BelievabilityException be)
        {
            logError( "Initial Belief Exception: "
                           + be.getMessage() );
   
        }
        
    } // testInitialBeliefStates

} // class ModelManager
