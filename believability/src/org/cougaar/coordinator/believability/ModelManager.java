/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManager.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManager.java,v $
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
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * This is the main container class that handles acces to all local
 * models. It received data input via the TechSpecManagerInterface,
 * and provides information via the ModelManagerInterface. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-20 21:39:49 $
 *
 */
public class ModelManager 
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
        _logger = Logging.getLogger(this.getClass().getName());
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
         if ( _logger.isDebugEnabled() )
            _logger.debug( "==== getAssetUtilities() ====" );

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

        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== getMaxSensorLatency() ====" );

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
        SensorTypeModel s_model;

        if ( _logger.isDebugEnabled() )
            _logger.debug( "Starting call to: addSensorType()" );

        AssetType asset_type = diag_ts.getAssetType();
        AssetTypeModel asset_type_model = addAssetType( asset_type );

        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Add Sensor Type ====" );
        
        if ( diag_ts == null )
        {
            _logger.error( "NULL found for DiagnosisTechSpecInterface.");
            return;
        }

        // A few cases to be handled:
        //
        //   o Brand new (first time seen) sensor type
        //   o Sensor type already exists, but is not valid
        //   o Sensor type already exists and is valid
        //
        // We handle the two lattrer cases the same way.

        s_model = _sensor_type_container.get( diag_ts.getName() );
        
        if ( s_model == null )
        {
            try
            {
                s_model = new SensorTypeModel( diag_ts,
                                               asset_type_model );

                _sensor_type_container.add( s_model );
                
                if ( _logger.isDebugEnabled() )
                    _logger.debug( "Added New SensorTypeModel:\n" 
                                   + s_model.toString() );
                
            }
            catch (BelievabilityException be)
            {
                _logger.error( "Cannot add SensorTypeModel: "
                               + be.getMessage() );
                return;
            }
        } // if this sensor type does not exist

        else
        {
            try
            {
                s_model.updateContents( diag_ts, asset_type_model );
                
                if ( _logger.isDebugEnabled() )
                    _logger.debug( "Updating invalid SensorTypeModel:\n"
                                   + s_model.toString() );
            }
            catch (BelievabilityException be)
            {
                _logger.error( "Problem updating SensorTypeModel.");
                return;
            }

        } // if model exists, but not valid

        return;

    } // method addSensorType

    //************************************************************
    /**
     * Adding a new threat type to the local models
     *
     * @param threat_model Threat type to be added
     */
    public void addThreatType( ThreatModelInterface threat_model )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Add Threat Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** addThreatType()" );
    } // method addThreatModel

    //************************************************************
    /**
     * Adding a new actuator type to the local models.
     *
     * @param actuator_ts Actuator type to be added
     */
    public void addActuatorType( ActionTechSpecInterface actuator_ts )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Add Actuator Type ====" );

        // FIXME: We will not be using this in the short term, so this
        // does nothing right now.
        //
        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** addActuatorType()" );

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
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Update Sensor Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** updateSensorType()" );

    } // method updateSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void updateThreatType( ThreatModelInterface threat_model )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Update Threat Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** updateThreatType()" );
    } // method updateThreatType

    //************************************************************
    /**
     * docs here...
     */
    public void updateActuatorType( ActionTechSpecInterface actuator_ts )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Update Actuator Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** updateActuatorType()" );
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
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Remove Sensor Type ====" );

        SensorTypeModel model 
                = _sensor_type_container.get( diag_ts.getName() );
        
        if ( model == null )
        {
            _logger.error( "Cannot find SensorTypeModel.");
            return;
        }

        try
        {
            _sensor_type_container.remove( model );
     
        }
        catch (BelievabilityException be)
        {
            _logger.error( "Cannot remove SensorTypeModel.");
            return;
        }

    } // method removeSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void removeThreatType( ThreatModelInterface threat_model )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Remove Threat Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** removeThreatType()" );
    } // method removeThreatModel

    //************************************************************
    /**
     * docs here...
     */
    public void removeActuatorType( ActionTechSpecInterface actuator_ts )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Remove Actuator Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** removeActuatorType()" );
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
     * already exists, then nothing is done.  To change asset type
     * information use the 'updateAssetType'
     *
     * @param asset_type The asset type to be added.
     */
    protected AssetTypeModel addAssetType( AssetType asset_type )
    {
        AssetTypeModel at_model;

        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Add Asset Type ====" );

        if ( asset_type == null )
        {
            _logger.error( "NULL found for asset_type.");
            return null;
        }

        // A few cases to be handled:
        //
        //   o Brand new (first time seen) asset type
        //   o Asset type already exists, but is not valid
        //   o Asset type already exists and is valid

        at_model = _asset_type_container.get( asset_type.getName() );
        
        if ( at_model == null )
        {
            try
            {
                at_model = new AssetTypeModel( asset_type );
                
                _asset_type_container.add( at_model );

                if ( _logger.isDebugEnabled() )
                    _logger.debug( "Added New AssetTypeModel:\n"
                                   + at_model.toString() );
            
            }
            catch (BelievabilityException be)
            {
                _logger.error( "Cannot add AssetTypeModel.");
                return null;
            }

        } // if this asset type does not exist

        else if ( ! at_model.isValid())
        {
            if ( _logger.isDebugEnabled() )
                _logger.debug( "Updating invalid AssetTypeModel" );

            try
            {
                at_model.updateContents( asset_type );
                
                if ( _logger.isDebugEnabled() )
                    _logger.debug( "Updated AssetTypeModel:\n"
                                   + at_model.toString() );
            }
            catch (BelievabilityException be)
            {
                _logger.error( "Problem updating AssetTypeModel.");
                return null;
            }

        } // if model exists, but not valid

        // Else (third case) asset model exists and is valid so we can
        // just return it.
        else
        {

            if ( _logger.isDebugEnabled() )
                _logger.debug( "Using existing valid AssetTypeModel");
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
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Update Asset Type ====" );

        if ( _logger.isDebugEnabled() ) 
            _logger.debug( "** NOT IMPLEMENTED ** updateAssetType()" );

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
        if ( _logger.isDebugEnabled() )
            _logger.debug( "==== Remove Asset Type ====" );

        AssetTypeModel model 
                = _asset_type_container.get( asset_type.getName() );
        
        if ( model == null )
        {
            _logger.error( "Cannot find AssetTypeModel.");
            return;
        }

        try
        {
            _asset_type_container.remove( model );
     
        }
        catch (BelievabilityException be)
        {
            _logger.error( "Cannot remove AssetTypeModel.");
            return;
        }

    } // method removeAssetStateDimension

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // For logging debug, warning and error messages to the cougaar
    // logging system
    //
    Logger _logger;

    // These are the local models this class manages.
    //
    AssetTypeContainer _asset_type_container = new AssetTypeContainer();
    SensorTypeContainer _sensor_type_container = new SensorTypeContainer();
    ThreatTypeContainer _threat_type_container = new ThreatTypeContainer();

    MAUWeightModel _mau_weight_model = new MAUWeightModel();
    
    POMDPModelManager _pomdp_manager = new POMDPModelManager(this);

    //------------------------------------------------------------
    // Test code section
    //------------------------------------------------------------


    // FIXME: This is temporary for testing. Please remove me.
    //
    AssetType _test_asset_type;
    void testInitialBeliefStates()
    {
        _logger.debug( "\n==== TESTING INITIAL BELIEF STATES ====\n" );

        try
        {
            BeliefState initial_belief
                    = _pomdp_manager.getInitialBeliefState
                    ( _test_asset_type );
            
            _logger.debug( "Initial Belief:\n"
                           + initial_belief.toString() );
        }
        catch (BelievabilityException be)
        {
            _logger.error( "Initial Belief Exception: "
                           + be.getMessage() );
   
        }
        
    } // testInitialBeliefStates

} // class ModelManager
