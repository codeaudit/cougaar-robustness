/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManager.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManager.java,v $
 * $Revision: 1.29 $
 * $Date: 2004-09-10 17:25:31 $
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

/**
 * This is the main container class that handles acces to all local
 * models. It received data input via the TechSpecManagerInterface,
 * and provides information via the ModelManagerInterface. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.29 $Date: 2004-09-10 17:25:31 $
 *
 */
public class ModelManager extends Loggable
        implements ModelManagerInterface
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    // These constants for testing purposes only.  Make sure the
    // bolean are set to false for a running system.
    //
    public static final boolean USE_FAKE_SENSOR_LATENCY = false;
    public static final long FAKE_SENSOR_LATENCY_MS = 15000;

    public static final boolean USE_FAKE_PUBLISH_INTERVAL = false;
    public static final long FAKE_PUBLISH_INTERVAL_MS = 120000;

    public static final boolean USE_FAKE_IMPLICT_INTERVAL = false;
    public static final long FAKE_IMPLICIT_INTERVAL_MS = 30000;

    public static final boolean USE_FAKE_PUBLISH_DELAY_INTERVAL = false;
    public static final long FAKE_PUBLISH_DELAY_INTERVAL_MS = 3000;

    /**
     * Main constructor
     *
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
        logDetail( "getAssetUtilities() for state dim: " 
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

        logDetail( "==== getMaxSensorLatency() ====" );

        if ( USE_FAKE_SENSOR_LATENCY )
        {
            logWarning( "USING FAKE LATENCY FOR TESTING ! (FIXME)" );
            return FAKE_SENSOR_LATENCY_MS;
        }

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
     * Retrieves the sensor latency for the given sensor of the given
     * asset type.
     *
     * @param asset_type The asset type
     * @param sensor_name The name of the sensor
     */
    public long getSensorLatency( AssetType asset_type,
                                  String sensor_name )
            throws BelievabilityException
    {

        logDetail( "==== getSensorLatency() ====" );

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model == null )
        {
            throw new BelievabilityException
                    ( "ModelManager.getSensorLatency()",
                      "Cannot find asset type model." );
        }

        return at_model.getSensorLatency( sensor_name );

    } // method getSensorLatency


    //************************************************************
    /**
     * Returns to number of sensors applicable for the given asset
     * type.
     */
    public long getNumberOfSensors( AssetType asset_type )
            throws BelievabilityException
    {

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model == null )
        {
            throw new BelievabilityException
                    ( "ModelManager.getNumberOfSensors()",
                      "Cannot find asset type model." );
        }

        return at_model.getNumberOfSensors( );

    } // method getNumberOfSensors

    //************************************************************
    /**
     * Returns whether or not the given sensor should have implicit
     * diagnoses generated for it.  Somce sensors only report on a
     * diagnsis value change, even though they are continually monitoring
     * an asset.
     */
    public boolean usesImplicitDiagnoses( AssetType asset_type,
                                          String sensor_name )
            throws BelievabilityException
    {
        // Just relay this to the appropriate asset type model.
        //

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model == null )
        {
            throw new BelievabilityException
                    ( "ModelManager.usesImplicitDiagnoses()",
                      "Cannot find asset type model." );
        }

        return at_model.usesImplicitDiagnoses( sensor_name );

    } // method usesImplicitDiagnoses

    //************************************************************
    /**
     * Returns the maximal amount of time we should allow between
     * consecutive belief state (state estimation) publications.
     */
    public long getMaxPublishInterval( AssetType asset_type )
            throws BelievabilityException
    {

        if ( USE_FAKE_PUBLISH_INTERVAL )
        {
            logWarning( "USING FAKE PUBLISH INTERVAL FOR TESTING ! (FIXME)" );
            return FAKE_PUBLISH_INTERVAL_MS;

        }

        return _believability_knob.getMaxPublishInterval();
    }

    //************************************************************
    /**
     * Returns the delay time (millisecs) we should allow between
     * consecutive when a trigger is processed and when it triggers a
     * new belief state computation.
     */
    public long getPublishDelayInterval( )
            throws BelievabilityException
    {

        if ( USE_FAKE_PUBLISH_DELAY_INTERVAL )
        {
            logWarning( "USING FAKE PUBLISH DELAY INTERVAL FOR TESTING ! (FIXME)" );
            return FAKE_PUBLISH_DELAY_INTERVAL_MS;

        }

        return _believability_knob.getPublishDelayInterval();
    }

    //************************************************************
    /**
     * Returns the amount of utility chnage that must be seen in a
     * belief state for us to go through the trouble of publishing it.
     * This threshold is only applied in some cases, as others require
     * the immediate publication, regardless of the utility change.
     */
    public double getBeliefUtilityChangeThreshold( )
    {
        return _believability_knob.getBeliefReleaseUtilityThreshold();
    }

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
        if ( diag_ts == null )
        {
            logDebug( "NULL found for DiagnosisTechSpecInterface.");
            return;
        }

        // DO not make the assumption that the asset type model exists
        // for this sensor.  Try to add and/or fetch the model first.
        //
        AssetType asset_type = diag_ts.getAssetType();
        AssetTypeModel asset_type_model = getOrCreateAssetTypeModel( asset_type );

        if ( asset_type_model == null )
            return;

        logDetail( "==== Start: Add Sensor Type ====" );
        
        SensorTypeModel s_model;

        try
        {
            
            s_model= asset_type_model.addSensorTypeModel( diag_ts );
            
            logDetail( "Added New SensorTypeModel:\n" 
                      + s_model.toString() );
                
        }
        catch (BelievabilityException be)
        {
            logDebug( "Cannot add SensorTypeModel "
                      +  diag_ts.getName() + ": " + be.getMessage() );
            return;
        }

        logDebug( "Added sensor: " + s_model.getName() );

    } // method addSensorType

    //************************************************************
    /**
     * Adding a new threat type to the local models
     *
     * @param threat_mi Threat type to be added
     */
    public void addThreatType( ThreatModelInterface threat_mi )
    {
        logDetail( "==== Start: Add Threat Model ====" );

        if ( threat_mi == null )
        {
            logDebug( "addThreatType() sent NULL ThreatModelInterface. " );
            return;
        }

        // Here we need to walk the threat model to find all the
        // transitive effects that this threat will have.  We build a
        // graph of all these stresses, connecting them where there is
        // a causal relationship.  The causality can span state
        // dimensions as well as asset types, so the complete graph
        // needs to be built up here, above all the specific
        // AssetTypeModel and AssetTypeDimensionModels.
        //
        Vector new_stress_set = new Vector();

        _stress_graph.add( threat_mi, new_stress_set );

        logDetail( "Added " + new_stress_set.size()
                  + " new stresses. Current Stress Graph: " 
                  + _stress_graph.toString() );

        // However, when constructing the stress model graph for a
        // particular asset state dimension, the
        // AssetStateDimensionModel only needs to look at those
        // stresses on that particular state dimension.  It will
        // build a set of direct effects on that asset but uses
        // the linking structure in the larger stress graph to
        // compute the probbailities.  For this reason, we need to
        // make sure the asset models have a cache of the stresses
        // for their asset type and state dimension.
        //

        Enumeration stress_enum = new_stress_set.elements();
        while ( stress_enum.hasMoreElements() )
        {

            StressInstance stress = (StressInstance) stress_enum.nextElement();

            // Do not make the assumption that the asset type model
            // exists.  First attempt to add, or simply retrieve the asset
            // model.  
            //
            AssetType asset_type = stress.getAffectedAssetType();
            AssetTypeModel asset_type_model 
                    = getOrCreateAssetTypeModel( asset_type );

            if ( asset_type_model == null )
            {
                logDebug( "Cannot find AssetTypeModel for asset type: "
                          + asset_type.getName() );
                continue;
            }

            try
            {
                logDetail( "Adding new stress to asset model: "
                          + stress.getName() );
            
                asset_type_model.addStressInstance( stress );

            }
            catch (BelievabilityException be)
            {
                logDebug( "Cannot add stress "
                          + stress.getName() + " to asset model : " 
                          + be.getMessage() );
                return;
            }
        } // while stress_enum

        logDebug( "Added threat: " + threat_mi.getName() );

    } // method addThreatModel

    //************************************************************
    /**
     * Adding a new actuator type to the local models.
     *
     * @param actuator_ts Actuator type to be added
     */
    public void addActuatorType( ActionTechSpecInterface actuator_ts )
    {
        if ( actuator_ts == null )
        {
            logDebug( "NULL found for ActionTechSpecInterface.");
            return;
        }

        // Do not make the assumption that the asset type model exists
        // for this actuator.  Try to add and/or fetch the model first.
        //
        AssetType asset_type = actuator_ts.getAssetType();
        AssetTypeModel asset_type_model 
                = getOrCreateAssetTypeModel( asset_type );

        if ( asset_type_model == null )
            return;

        logDetail( "==== Start: Add Actuator Type ====" );
        
        ActuatorTypeModel a_model;
        try
        {
            
            a_model = asset_type_model.addActuatorTypeModel( actuator_ts );
            
            logDetail( "Added New ActuatorTypeModel:\n" 
                      + a_model.toString() );
                
        }
        catch (BelievabilityException be)
        {
            logDebug( "Cannot add ActuatorTypeModel: " + be.getMessage() );
            return;
        }

        logDebug( "Added actuator: " + a_model.getName() );

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
        logDetail( "** NOT IMPLEMENTED ** updateSensorType()" );
    } // method updateSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void updateThreatType( ThreatModelInterface threat_model )
    {
        logDetail( "Ignoring updateThreatType(). This should be handled by "
                  + "call to handleThreatModelChange()" );
    } // method updateThreatType

    //************************************************************
    /**
     * docs here...
     */
    public void updateActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDetail( "** NOT IMPLEMENTED ** updateActuatorType()" );
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
        logDetail( "** NOT IMPLEMENTED ** removeSensorType()" );

    } // method removeSensorType


    //************************************************************
    /**
     * docs here...
     */
    public void removeThreatType( ThreatModelInterface threat_model )
    {
        logDetail( "** NOT IMPLEMENTED ** removeThreatType()" );
    } // method removeThreatModel

    //************************************************************
    /**
     * docs here...
     */
    public void removeActuatorType( ActionTechSpecInterface actuator_ts )
    {
        logDetail( "** NOT IMPLEMENTED ** removeActuatorType()" );
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

        logDetail( "==== Start: Handle Threat Model Change ====" );

        ThreatModelInterface threat_model = tm_change.getThreatModel();

        // Do not assume the asset type exists.  First attempt to add,
        // or simply retrieve the asset model. 
        //
        // Note that a given threat is defined on only one state
        // dimension of only one asset type.  Thus, we do not have to
        // check all the individual asset types for the asset
        // instances included in this change event: they should all be
        // the same.
        //
        AssetType asset_type 
                = threat_model.getThreatDescription().getAffectedAssetType();
        AssetTypeModel asset_type_model 
                = getOrCreateAssetTypeModel( asset_type );

        if ( asset_type_model == null )
            return;

        try
        {
            asset_type_model.handleThreatModelChange( tm_change );
        }
        catch (BelievabilityException be)
        {
            logDebug( "Problem handling threat model change: "
                      + be.getMessage() );
        }

        logDebug( "Handled threat change for : "
                  + tm_change.getThreatModel().getName() );

    } // method handleThreatModelChangxe

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

    //************************************************************
    /**
     * Sets the current flag for whether or not the rehydration
     * processis in progress.  This should be set true only when the
     * plugin is in the process of restoring the local data from the
     * blackboard. 
     *
     * @param value The value to set
     */
    public void setRehydrationHappening( boolean value )
    {
        _rehydration_happening = value;

        // Whether we are beginning or ending the rehydration, we know
        // for sure that we must be in the rehydrated state.
        //
        _is_in_rehydrated_state = true;

    } // method setRehydrationHappening

    //************************************************************
    /**
     * Checks whether or not the rehydration processis in progress.
     * This should be true only when the plugin is in the process
     * of restoring the local data from the blackboard.
     *
     * @return True if the plugin is rehydrating
     */
    public boolean isRehydrationHappening( )
    {
        return _rehydration_happening;

    } // method isRehydrationHappening

    //************************************************************
    /**
     * Checks whether or not the rehydration processis is in progress
     * or has happened in the past.  We can move out of the rehydrated
     * state if the system is leashed and unleashed after the
     * rehydration.
     *
     * @return True if the plugin is rehydrating
     */
    public boolean isInRehydratedState( )
    {
        return _is_in_rehydrated_state;

    } // method isInRehydratedState



    //************************************************************
    /**
     * Checks whether or not we are operating in a leashed state.
     *
     * @return True if the believability plugin is leashed.
     */
    public boolean isLeashed( )
    {
        return _believability_knob.isLeashed();

    } // method isLeashed

    //************************************************************
    /**
     * Sets the current flag for whether or not the unleashing
     * processis in progress.  This should be set true only when the
     * plugin is in the process of restoring the local data from the
     * blackboard. 
     *
     * @param value The value to set
     */
    public void setUnleashingHappening( boolean value )
    {

        _unleashing_happening = value;

    } // method setUnleashingHappening

    //************************************************************
    /**
     * Checks whether or not the unleasing processis in progress.
     * This should be true only when the plugin is in the process of
     * restoring the local data from the blackboard.
     *
     * @return True if the plugin is rehydrating
     */
    public boolean isUnleashingHappening( )
    {
        return _unleashing_happening;

    } // method isUnleashingHappening

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
    protected AssetTypeModel getOrCreateAssetTypeModel( AssetType asset_type )
    {
        if ( asset_type == null )
        {
            logDebug( "NULL found for asset_type.");
            return null;
        }

        AssetTypeModel at_model 
                =  (AssetTypeModel) _asset_type_container.get
                ( asset_type.getName() );
        
        if ( at_model != null )
        {
            logDetail( "Asset model found: " + at_model.getName() );
            return at_model;
        }

        logDetail( "==== Start: Add Asset Type ====" );

        try
        {
            at_model = new AssetTypeModel( asset_type );
                
            _asset_type_container.put( asset_type.getName(),
                                       at_model );

            logDetail( "Added New AssetTypeModel:\n"
                      + at_model.toString() );
            
        }
        catch (BelievabilityException be)
        {
            logDebug( "Cannot add AssetTypeModel "
                      +  asset_type.getName() + ": " + be.getMessage() );
            return null;
        }

        logDebug( "Added asset type: " + at_model.getName() );

        return at_model;

    } // method getOrCreateAssetTypeModel

    //************************************************************
    /**
     *  Changing the properties of an asset type in the local models.
     *
     * @param asset_type The asset type to be changed/
     */ 
    protected void updateAssetType( AssetType asset_type )
    {
        logDetail( "** NOT IMPLEMENTED ** updateAssetType()" );

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
        logDetail( "** NOT IMPLEMENTED ** removeAssetType()" );

    } // method removeAssetStateDimension

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // These are the local models this class manages.
    //
    private Hashtable _asset_type_container = new Hashtable();

    private MAUWeightModel _mau_weight_model = new MAUWeightModel();

    private StressGraph _stress_graph = new StressGraph();

    // Whether or not rehydration is happening (in progress recreation
    // of state).
    //
    private boolean _rehydration_happening = false;

    // Whether or not rehydration has hapened.
    //
    private boolean _is_in_rehydrated_state = false;

    // Whether or not rehydration is happening (in progress recreation
    // of state).
    //
    private boolean _unleashing_happening = false;

    // This is the place to put externally controllable policy-derived
    // parameters/settings. 
    //
    private BelievabilityKnob _believability_knob = new BelievabilityKnob();

    //------------------------------------------------------------
    // POMDP model section
    //------------------------------------------------------------

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
        // The initial belief is only based on the asset type, so is
        // the same for all assets of a given type.

        if ( asset_type == null )
            throw new BelievabilityException
                    ( "ModelManager.getInitialBeliefState()",
                      "NULL asset type passed in." );

        POMDPAssetModel pomdp_model = getPOMDPModel( asset_type );

        // If no rehydrating, then we get the techspec based initial
        // belief.
        //
        return pomdp_model.getInitialBeliefState();

    } // method getInitialBeliefState

    //************************************************************
    /**
     * Get the a priori probability for the indicated asset id and
     * makes sure the bleif state has that ID set in it.
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
         //
         
         // This call will handle the case of asset_id == null by
         // throwing an exception.
         //
         BeliefState belief = getInitialBeliefState( asset_id.getType() );
         
         belief.setAssetID( asset_id );

         // The techspecs are currently set up to define a single
         // initial state, which transaltes into an initial belief of
         // certainty of that state.  Starting off believing that
         // every other state has zero probability can cause trouble,
         // because there is indeed some chance that these states
         // might happen, though it is relkatively small.
         //
         belief.blurProbabilities
                 ( _believability_knob.getInitialBeliefBlurAmount() );
         
         
         return belief;
         
     } // method getInitialBeliefState

    //************************************************************
    /**
     * Gets a belief state with each state dimension set to the
     * value representing the initial belief when there is no
     * information available (e,g. during a rehydration).
     *
     * @param asset_id The ID of the asset
     * @return A new belief states set to the distributions to use
     * with no information.
     *
     */
    public BeliefState getNoInformationBeliefState( AssetID asset_id )
           throws BelievabilityException
     {
         // Here we first get the initial belief state which is close
         // to what we want.  However, for each state dimension, we
         // want to make surew we spinkle some amount of probability
         // evenly across all state dimensions so that no dimension
         // has zero probability.  This has the effect of blurring the
         // belief state.
         //

         if (( asset_id == null )
             || ( asset_id.getType() == null ))
             throw new BelievabilityException
                     ( "ModelManager.getNoInformationBeliefState()",
                       "NULL asset or asset type passed in." );
         
         // The uniform belief is only based on the asset type, so is
         // the same for all assets of a given type.
         //
         POMDPAssetModel pomdp_model = getPOMDPModel( asset_id.getType() );
         
         BeliefState belief = pomdp_model.getInitialBeliefState();
         belief.setAssetID( asset_id );

         logDetail( "Blurring initial belief probabilities." );

         // This is the routine that does the main work here.
         //
         belief.blurProbabilities
                 ( _believability_knob.getNoInformationBeliefBlurAmount() );
         
         return belief;
         
     } // method getNoInformationBeliefState

    //************************************************************
    /**
     * Gets a belief state with each state dimension set to the
     * unformation distribution. Used to express complete uncertainty
     * about the state of an asset.
     *
     * @param asset_id The ID of the asset
     * @return A new belief states set to uniform distributions.
     *
     */
    public BeliefState getUniformBeliefState( AssetID asset_id )
           throws BelievabilityException
     {
         
         if (( asset_id == null )
             || ( asset_id.getType() == null ))
             throw new BelievabilityException
                     ( "ModelManager.getUniformBeliefState()",
                       "NULL asset or asset type passed in." );
         
         // The uniform belief is only based on the asset type, so is
         // the same for all assets of a given type.
         //
         POMDPAssetModel pomdp_model = getPOMDPModel( asset_id.getType() );
         
         BeliefState belief = pomdp_model.getUniformBeliefState();
         
         belief.setAssetID( asset_id );
         
         return belief;
         
     } // method getUniformBeliefState

    //************************************************************
    /**
     * Used to update the belief state using the given trigger.
     *
     * @param start_belief initial belief state
     * @param trigger the trigger to use to determine new belief
     * state
     * @return the update belief state
     *
     */
    public BeliefState updateBeliefState( BeliefState start_belief,
                                          BeliefUpdateTrigger trigger )
            throws BelievabilityException
    {
        if (( start_belief == null )
            || ( trigger == null ))
            throw new BelievabilityException
                    ( "ModelManager.updateBeliefState()",
                      "NULL parameters(s) passed in." );

        POMDPAssetModel pomdp_model
                = getPOMDPModel( start_belief.getAssetType() );

        logDebug( "Belief update for :" + start_belief.getAssetID() );

        return pomdp_model.updateBeliefState( start_belief,
                                              trigger );

    } // method updateBeliefState

     //************************************************************
    /**
     * Retrieves the POMDP model for the given asset type.  It will
     * create the model if it does not exist, or if something has
     * changed in the underlying tech spec models since it was
     * created. 
     *
     * @param asset_type  The asset type ot build the model for
     */
    protected POMDPAssetModel getPOMDPModel( AssetType asset_type )
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
        
        return createPOMDPModel( asset_type );

    } // method getPOMDPModel

    //************************************************************
    /**
     * Creates the model for the given asset type and adds it to the
     * set of models this class is managing.
     *
     * @param asset_type  The asset type ot build the model for
      */
    protected POMDPAssetModel createPOMDPModel( AssetType asset_type )
            throws BelievabilityException
    {
        // Method implementation comments go here ...

        // First we find the AssetTypeModel to set up the state space
        // and initial belief for the asset type.
        //
        AssetTypeModel at_model = getAssetTypeModel( asset_type );

        if ( at_model == null )
            throw new BelievabilityException
                    ( "ModelManager.createModel()",
                      "Asset type "
                      + asset_type.getName() 
                      + " does not exist in ModelManager" );
        
        
        POMDPAssetModel pomdp_model
                = new POMDPAssetModel( at_model );

        _pomdp_model_set.put( asset_type.getName(), pomdp_model );

        return pomdp_model;

    } // method createPOMDPModel

    // This contains all the POMDPModel objects for assets, keyed on
    // the asset type name.  
    //
    private Hashtable _pomdp_model_set = new Hashtable();

    //------------------------------------------------------------
    // Test code section
    //------------------------------------------------------------


    // FIXME: This is temporary for testing. Please remove me.
    //
    AssetType _test_asset_type;
    void testInitialBeliefStates()
    {
        logDetail( "\n==== TESTING INITIAL BELIEF STATES ====\n" );

        try
        {
            BeliefState initial_belief
                    = getInitialBeliefState( _test_asset_type );
            
            logDetail( "Initial Belief:\n"
                           + initial_belief.toString() );
        }
        catch (BelievabilityException be)
        {
            logDebug( "Initial Belief Exception: "
                           + be.getMessage() );
   
        }
        
    } // testInitialBeliefStates

} // class ModelManager
