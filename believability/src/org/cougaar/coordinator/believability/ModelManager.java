/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManager.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManager.java,v $
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

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
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * This is the main container class that handles acces to all local
 * models. It received data input via the TechSpecManagerInterface,
 * and provides information via the ModelManagerInterface. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
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
        logDebug( "==== getAssetUtilities() ====" );

        AssetTypeModel at_model 
                = _asset_type_container.get( asset_type.getName() );
        
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
                = _asset_type_container.get( asset_type.getName() );
        
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

        // DO not make the assumption that the asset type model exists
        // for this sensor.  Try to add and/or fetch the model first.
        //
        AssetType asset_type = diag_ts.getAssetType();
        AssetTypeModel asset_type_model = addAssetType( asset_type );

        if ( asset_type_model == null )
            return;

        logDebug( "==== Add Sensor Type ====" );
        
        if ( diag_ts == null )
        {
            logError( "NULL found for DiagnosisTechSpecInterface.");
            return;
        }

        SensorTypeModel s_model 
                = _sensor_type_container.get( diag_ts.getName() );
        
        if ( s_model != null )
        {
            logError( "Trying to add duplicate SensorTypeModel: "
                      +  diag_ts.getName() );
            return;
        }

        try
        {
            s_model = new SensorTypeModel( diag_ts,
                                           asset_type_model );
            
            _sensor_type_container.add( s_model );
            
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
    public void addThreatType( ThreatModelInterface threat_ts )
    {
        logDebug( "addThreatType() called" );

        if ( threat_ts == null )
        {
            logError( "addThreatType() sent NULL ThreatModelInterface. " );
            return;
        }

        createThreatTypeModel( threat_ts.getThreatDescription() );

    } // method addThreatModel

    //************************************************************
    /**
     * Adding a new threat description to the local models
     *
     * @param threat_desc Threat description to be added
     */
    public void addThreatDescription( ThreatDescription threat_desc )
    {
        logDebug( "addThreatDescription() called" );

        if ( threat_desc == null )
        {
            logError( "addThreatDescription() sent NULL ThreatDescription. " );
            return;
        }

        if ( CREATE_THREAT_MODEL_FROM_DESCRIPTIONS )
            createThreatTypeModel( threat_desc );

    } // method addThreatDescription

    //************************************************************
    /**
     * Adding a new event description to the local models
     *
     * @deprecated We can now get at the EventDesciptions directly 
     * from the ThreatDescriptions, so this is no longer needed and
     * actually does nothing. 
     * @param event_desc Event description to be added
     */
    public void addEventDescription( EventDescription event_desc )
    {
        // Deprecated: do nothing now.

    } // method addEventDescription

    //************************************************************
    /**
     * Adding a new actuator type to the local models.
     *
     * @param actuator_ts Actuator type to be added
     */
    public void addActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDebug( "==== Add Actuator Type ====" );

        logDebug( "** NOT IMPLEMENTED ** addActuatorType()" );

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

        logDebug( "** NOT IMPLEMENTED ** updateThreatType()" );
    } // method updateThreatType

    //************************************************************
    /**
     * docs here...
     */
    public void updateThreatDescription( ThreatDescription threat_ts )
    {
        logDebug( "==== Update Threat Description ====" );

        logDebug( "** NOT IMPLEMENTED ** updateThreatDescription()" );
    } // method updateThreatDescription

    //************************************************************
    /**
     * Updating a new event description in the local models
     *
     * @deprecated We can now get at the EventDesciptions directly 
     * from the ThreatDescriptions, so this is no longer needed and
     * actually does nothing. 
     * @param event_desc Event description to be added
     */
    public void updateEventDescription( EventDescription event_desc )
    {
        // Deprecated.
    } // method updateEventDescription

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
    public void removeThreatDescription( ThreatDescription threat_ts )
    {
        logDebug( "==== Remove Threat Description ====" );

        logDebug( "** NOT IMPLEMENTED ** removeThreatDescription()" );
    } // method removeThreatDescription

    //************************************************************
    /**
     * Removing a new event description from the local models
     *
     * @deprecated We can now get at the EventDesciptions directly 
     * from the ThreatDescriptions, so this is no longer needed and
     * actually does nothing. 
     * @param event_desc Event description to be added
     */
    public void removeEventDescription( EventDescription event_desc )
    {
        // Deprecated
    } // method removeEventDescription

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
     * For retrieving an AssetTypeModel.
     *
     * @param asset_type Source for the asset name
     */
    public AssetTypeModel getAssetTypeModel( AssetType asset_type )
    {
        return _asset_type_container.get( asset_type.getName() );

    } // method getAssetTypeModel

    //************************************************************
    /**
     * For retrieving an SensorTypeModel.
     *
     * @param diag_ts Source for the sensor information
     */
    public SensorTypeModel getSensorTypeModel
            ( DiagnosisTechSpecInterface diag_ts )
    {
        return _sensor_type_container.get( diag_ts.getName() );

    } // method getSensorTypeModel

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
    protected AssetTypeModel addAssetType( AssetType asset_type )
    {
        if ( asset_type == null )
        {
            logError( "NULL found for asset_type.");
            return null;
        }

        AssetTypeModel at_model 
                = _asset_type_container.get( asset_type.getName() );
        
        if ( at_model != null )
        {
            logDebug( "Asset model found: " + at_model.getName() );
            return at_model;
        }

        logDebug( "==== Add Asset Type ====" );

        try
        {
            at_model = new AssetTypeModel( asset_type );
                
            _asset_type_container.add( at_model );

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

    } // method addAssetType

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

    // This is a flag to control whether or not we should create the
    // local threat models from threat descriptions.  If true, then
    // the instant we see a matching threat and event desciption pair,
    // we will create the local ThreatTypeModel.  If false, then we
    // will only create the local model when a ThreatModelInterface
    // object is added.
    //
    private static final boolean CREATE_THREAT_MODEL_FROM_DESCRIPTIONS = true;

    // These are the local models this class manages.
    //
    private AssetTypeContainer _asset_type_container = new AssetTypeContainer();
    private SensorTypeContainer _sensor_type_container = new SensorTypeContainer();
    private ThreatTypeContainer _threat_type_container = new ThreatTypeContainer();
    private MAUWeightModel _mau_weight_model = new MAUWeightModel();
    
    private POMDPModelManager _pomdp_manager = new POMDPModelManager(this);

    //************************************************************
    /**
     * Creates a new threat model from a threat description, which
     * must have a valid event description contined in it.
     *
     * @param threat_desc The threat description to add.
     */
    private void createThreatTypeModel( ThreatDescription threat_desc )
    {
        // Method implementation comments go here ...
        logDebug( "Starting call to: createThreatTypeModel()" );

        // Do not make the assumption that the asset type model
        // exists.  First attempt to add, or simply retrieve the asset
        // model.  
        //
        AssetType asset_type = threat_desc.getAffectedAssetType();
        AssetTypeModel asset_type_model = addAssetType( asset_type );

        if ( asset_type_model == null )
            return;

        logDebug( "==== Add Threat Type ====" );

       ThreatTypeModel threat_model
               = _threat_type_container.get( threat_desc.getName() );
        
        if ( threat_model != null )
        {
            logError( "Trying to add duplicate ThreatModel: " 
                      + threat_desc.getName() );
            return;
        }

        try
        {
            threat_model = new ThreatTypeModel( threat_desc, 
                                                asset_type_model );
            
            _threat_type_container.add( threat_model );

            logDebug( "Added New ThreatTypeModel:\n"
                      + threat_model.toString() );
            
        }
        catch (BelievabilityException be)
        {
            logError( "Cannot add ThreatTypeModel "
                      + threat_desc.getName() + ": " + be.getMessage() );
            return;
        }

    } // method createThreatTypeModel

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
