/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetTypeDimensionModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/AssetTypeDimensionModel.java,v $
 * $Revision: 1.28 $
 * $Date: 2004-10-20 16:48:21 $
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;

/**
 * Holds model information specific to a given state dimension.  This
 * model will be contained within an AssetTypeModel and roughly
 * corresponds to the tech-spec AssetSatteDimension objects.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.28 $Date: 2004-10-20 16:48:21 $
 * @see AssetTypeModel
 * @see AssetStateDimension
 */
class AssetTypeDimensionModel extends Model
{

    // Turn off this optimization until it proves to be a problem.
    //
    private static final boolean USE_EVENT_COLLECTION_CACHING = false;

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    // Use this when there are no sensors defined to monitor this
    // asset state dimension.
    //
    // FIXME: I have no idea if this value makes any sense.
    //
    public static final long DEFAULT_SENSOR_LATENCY  = 1000;

    //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "\t\t  Dimension name: " 
                     + _state_dim_name 
                     + "\n" );
        buff.append( "\t\t\tDefault value: " 
                     + _default_state_name + "\n" );

        buff.append( "\t\t\tPossible values: [" );
        for ( int val_idx = 0; 
              val_idx < _possible_states.length;
              val_idx++ )
        {
            buff.append( " " + _possible_states[val_idx] );
        } // for val_idx
        buff.append( " ]\n" );
            
        buff.append( "\t\t\tUtilities:\n" );

        for ( int val_idx = 0; 
              val_idx < _possible_states.length;
              val_idx++ )
        {
            for ( int mau_idx = 0 ; 
                  mau_idx < MAUWeightModel.NUM_WEIGHTS;
                  mau_idx++ )
            {
                    
                buff.append( "\t\t\t  V("
                             + _possible_states[val_idx]
                             + "," + MAUWeightModel.WEIGHT_NAME[mau_idx]
                             + ") = "
                             + _utilities[val_idx][mau_idx] 
                             + "\n" );
            } // for val_idx
                
        } // for mau_idx
            
        return buff.toString();

    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor.
     *
     * @param state_dim The tech-spec model which contains much of the
     * information we need
     */
    AssetTypeDimensionModel( AssetStateDimension state_dim )
            throws BelievabilityException
    {

        setContents( state_dim );
        setValidity( true );

    }  // constructor AssetTypeDimensionModel

    //************************************************************
    /**
     * Sets this objects internal values to be those from the given
     * asset state dimension.
     *
     * @param state_dim The tech-spec model which contains much of the
     * information we need
     */
    void setContents( AssetStateDimension state_dim )
            throws BelievabilityException
    {
        if ( state_dim == null )
            return;

        _state_dim = state_dim;
        
        _state_dim_name = state_dim.getStateName();

        if ( state_dim.getDefaultState() == null )
            throw new BelievabilityException
                    (  "AssetTypeDimensionModel.setContents()",
                       "NULL found for default state.");

        _default_state_name = state_dim.getDefaultState().getName();

        if ( _default_state_name == null )
            throw new BelievabilityException
                    (  "AssetTypeDimensionModel.setContents()",
                       "NULL found for default state name.");
        
        // This will allow us to enumerate all the possible states
        // this dimension can be in. 
        //
        Vector asset_state_list = state_dim.getPossibleStates();
        
        if ( asset_state_list == null )
            throw new BelievabilityException
                    (  "AssetTypeDimensionModel.setContents()",
                       "NULL found for possible state vector.");
        
        if ( asset_state_list.size() == 0 )
            throw new BelievabilityException
                    (  "AssetTypeDimensionModel.setContents()",
                       "Empty vector for possible states.");

        _possible_states = new String[asset_state_list.size()];
        _utilities = new double[asset_state_list.size()][];

        Iterator asset_state_iter = asset_state_list.iterator();

        for ( int val_idx = 0; asset_state_iter.hasNext(); val_idx++ )
        {
            AssetState state = (AssetState) asset_state_iter.next();

            _possible_states[val_idx] = state.getName();

            _utilities[val_idx] 
                    = new double[MAUWeightModel.NUM_WEIGHTS];
 
            _utilities[val_idx][MAUWeightModel.COMPLETENESS_IDX]
                    = state.getRelativeMauCompleteness();

            _utilities[val_idx][MAUWeightModel.SECURITY_IDX]
                    = state.getRelativeMauSecurity();

            // This doesn't appear to be implemented at the time
            // of first coding this class.
            //
            _utilities[val_idx][MAUWeightModel.TIMELINESS_IDX]
                    = 0.0f;
            
        } // while asset_state_iter

    }  // method setContents

    //************************************************************
    /**
     * Adds a sensor type model to this asset state dimension. Keep
     * track of them and also add this asset model as an observer of
     * changes to the sensor model.
     *
     * @param s_model The sensor type model to be added and monitored
     */
    SensorTypeModel addSensorTypeModel( DiagnosisTechSpecInterface diag_ts )
            throws BelievabilityException
    {

        
        if ( diag_ts == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addSensorTypeModel()",
                      "Cannot add null diagnosis tech spec" );

        if ( _sensor_model_set.get( diag_ts.getName()) != null )
            if ( _logger.isInfoEnabled() )
                _logger.info( "Replacing sensor type model: "
                        + diag_ts.getName() );

        SensorTypeModel s_model
                = new SensorTypeModel( diag_ts, this );
        
        _sensor_model_set.put( s_model.getName(),
                               s_model );

        return s_model;

    } // method addSensorTypeModel

    //************************************************************
    /**
     * Retrive the sensor model of the given name.
     */
    SensorTypeModel getSensorTypeModel( String sensor_name )
    {
        return (SensorTypeModel) _sensor_model_set.get( sensor_name );

    } // method getSensorTypeModel

    //************************************************************
    /**
     * Adds a sensor type model to this asset state dimension. Keep
     * track of them and also add this asset model as an observer of
     * changes to the sensor model.
     *
     * @param s_model The sensor type model to be added and monitored
     */
    ActuatorTypeModel addActuatorTypeModel( ActionTechSpecInterface action_ts )
            throws BelievabilityException
    {
        
        if ( action_ts == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addActuatorTypeModel()",
                      "Cannot add null action tech spec" );

        ActuatorTypeModel a_model
                = new ActuatorTypeModel( action_ts, this );

        if ( _actuator_model_set.get( a_model.getName()) != null )
            if ( _logger.isInfoEnabled() )
                _logger.info( "Replacing actuator type model: "
                        + a_model.getName() );
        
        _actuator_model_set.put( a_model.getName(),
                                 a_model );
        
        return a_model;

    } // method addActuatorTypeModel

    //************************************************************
    /**
     * Retrive the actuator model of the given name.
     */
    ActuatorTypeModel getActuatorTypeModel( String actuator_name )
    {
        return (ActuatorTypeModel) _actuator_model_set.get( actuator_name );

    } // method getActuatorTypeModel

    //************************************************************
    /**
     * Adds a stress instance that is relevant to this asset state
     * dimension.
     *
     * @param stress The stress to add
     */
    void addStressInstance( StressInstance stress )
            throws BelievabilityException
    {
        // Just relay the stress the the appropriate aset dimension
        // model.

        if ( stress == null )
            throw new BelievabilityException
                    ( "AssetTypeDimensionModel.addStressInstance()",
                      "Cannot add null stress instance" );

        // Just add this to the set of applicable stresses for this
        // state dimension.  We use a lazy evaluation scheme so that
        // only when we need to compute a belief state will we walk
        // this list and build the right structure for a given asset
        // type.
        //
        _stress_set.add( stress );


        if ( ! USE_EVENT_COLLECTION_CACHING )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "NOTE: Event collection caching disabled."
                      + " Ignoring threat asset list." );
            return;
        }

        // We need to also treat this addition as a change.  It is
        // possible that we will create a cache of applicable stresses
        // before we actually see any stresses, or before we have seen
        // the addition of some other stress.  If we have a cache of
        // applicable stresses and a new stress is added that afects
        // that asset, then we need to reconstruct the data structure
        // of applicable threats.
        //
        purgeEventCollectionsCache( stress.getAssetList() );
      
    } // method addStressInstance
 
    //************************************************************
    /**
     * This method will purge the cached set of threat variations for
     * every aset instance in the list sent in.  
     *
     * @param asset_ts_list A vector of AssetTechSpecInterface objects
     * of the assets to remove
     */
    void purgeEventCollectionsCache( Vector asset_ts_list )
    {

        if ( asset_ts_list == null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "No event collections to purge (NULL list)"
                      + " in state dimension " + _state_dim_name );
            return;
       }

        if ( asset_ts_list.size() < 1 )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "No event collections to purge (empty list)"
                      + " in state dimension " + _state_dim_name );
            return;
       }

        Enumeration asset_enum = asset_ts_list.elements();
        while( asset_enum.hasMoreElements() )
        {
            AssetTechSpecInterface asset_ts
                    = (AssetTechSpecInterface) asset_enum.nextElement();

            // We choose to keep the management of the mapping from
            // asset IDs to stresses simple.  In this way, on a stress
            // change (THreatModelChange), we do not attempt the
            // surgical manuevers required to bring the data structure
            // into compliance.  Instead, we simply wipe out the
            // existence of any mapping for this asset ID to the
            // stresses, and rely on a lazy evaluation scheme of
            // building the mapping only at the point we need it and
            // find it does not exist.  Note that this implies that we
            // ignore the 'change_type parameter and do this whether
            // the asset was added or removed from the stress.
            //
            if ( _asset_event_collection_table.contains
                 ( asset_ts.getAssetID() ))
            {
                if ( _logger.isDetailEnabled() )
                    _logger.detail( "Purging event collection from cache for "
                          + asset_ts.getAssetID() 
                          + " in state dimension " + _state_dim_name );
                _asset_event_collection_table.remove( asset_ts.getAssetID() );
            }
            else
            {
                if ( _logger.isDetailEnabled() )
                    _logger.detail( "No event collection to purge from cache for "
                          + asset_ts.getAssetID() 
                          + " in state dimension " + _state_dim_name );
            }

        } // while enum */

    } // method purgeEventCollectionsCache

    //************************************************************
    /**
     * Returns a collection of threat variations for the given asset
     * ID.
     *
     * @param asset_id The aset id whose threat variations we want.
     */
    EventInstanceCollection getEventInstanceCollection( AssetID asset_id )
            throws BelievabilityException
    {
        // We use lazy evaluation for building the data structure, so
        // construct the set if it does not already exist.

        EventInstanceCollection event_collection 
                = (EventInstanceCollection) 
                _asset_event_collection_table.get( asset_id );

        // This should evaluate to true and return in the normal case
        // where we just return the threat variation set.  We only
        // progress past this point if we find that we need to create
        // the set (should be the first time we have seen this asset
        // ID, or the asset membership in a threat has changed.)
        //
        if ( event_collection != null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Found existing event collection for asset '" 
                      + asset_id.getName() 
                      + "' in dimension '" 
                      + _state_dim_name );

            return event_collection;
        }

        // Else, we need to construct the set.

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Event collection not found for asset '" 
                  + asset_id.getName() 
                  + "' in dimension '" 
                  + _state_dim_name
                  + "'. Creating." );

        // The constructor here does a fair bit of work in gathering
        // applicable stresses into groups of events.  It is this
        // EventInstanceCollection data structure that defines the
        // stresses applicable to this asset as well as capturing the
        // relationships between the stresses (in the case where
        // stresses can cause other stresses.)
        //
        event_collection = new EventInstanceCollection( asset_id,
                                                        _stress_set,
                                                        this );
        
        // Optionall turn off caching for testing and debugging, or if
        // the cache managemrnt turns out to be buggy.
        //
        if ( USE_EVENT_COLLECTION_CACHING )
        {
            _asset_event_collection_table.put( asset_id, event_collection );
        }
        else
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "NOTE: Event collection caching disabled." );
        }

        return event_collection;

    } // method getEventInstanceCollection

    //************************************************************
    /**
     * Handle the situation where a threat model has changed the set
     * of assets it pertains to.  This could be the addition and/or
     * removal of assets.
     *
     * @param tm_change the threat model change event that has had the
     * asset membership change.
     */
    public void handleThreatModelChange( ThreatModelChangeEvent tm_change )
            throws BelievabilityException
    {
        if ( tm_change == null )
            throw new BelievabilityException
                    ( "AssetTypeDimensionModel.handleThreatModelChange()",
                      "Found NULL for threat change object." );

        if ( ! USE_EVENT_COLLECTION_CACHING )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "NOTE: Event collection caching disabled."
                      + " Ignoring threat change event." );
            return;
        }

        // FIXME: Do I need to to walk the transitive effects and
        // purge all assets connected with those effects?  If so, it
        // seems like this threatModelChangeEvent is silly, since the
        // getAddedAssets() and getRemovedAssets() isn't telling me
        // everything. 
        //

        ThreatModelInterface threat_model = tm_change.getThreatModel();
        
        // We deal with the removed and added assets the same.  Just
        // purge the cahce to force it to be recalculated.
        //
        purgeEventCollectionsCache( tm_change.getAddedAssets() );

        purgeEventCollectionsCache(  tm_change.getRemovedAssets() );
        
    } // method handleThreatModelChange

    //************************************************************
    /**
     * Simple accessor
     */
    String getAssetTypeName() { return _state_dim.getAssetType().getName(); }

    //************************************************************
    /**
     * Get the name of this model's state dimension.
     *
     * @return the name of the state dimension
     */
    String getStateDimensionName() { return _state_dim_name; }

    //************************************************************
    /**
     * Simple accessor
     *
     * @return the tech spec object for this asset state dimension
     */
    public AssetStateDimension getAssetStateDimension() { return _state_dim; }


    //************************************************************
    /**
     * Retrieve the AssetState object for the given named
     * asset state value.  
     *
     * @param state_val_name The state value name
     * @return the matching state or null if not found
     */
    AssetState getAssetState( String state_val_name )
    {

        return _state_dim.findAssetState( state_val_name );

    } // method getAssetState

    //************************************************************
    /**
     * Retrieve the AssetState object for the given named
     * asset state value.  
     *
     * @param state_val_idx The positional index of a state value
     * @return the matching state or null if not found
     */
    AssetState getAssetState( int state_val_idx )
    {
        return getAssetState( getStateDimValueName( state_val_idx ) );

    } // method getAssetState


    //************************************************************
    /**
     * Get the utilites for the given state dimension of this model
     *
     * @return the utilities for this state dimension as a two
     * dimensional array.   
     */
    double[][] getUtilities( ) 
            throws BelievabilityException
    {
        return _utilities;

    } // method getUtilities

    //************************************************************
    /**
     * Gets the default state value.
     *
     * @return default state name
     */
    String getDefaultStateValue( ) 
    {
        return _default_state_name;
        
    } // method getDefaultStateValue

    //************************************************************
    /**
     * Get the positional index value for the given state value.
     *
     * @param state_dim_value The state value of this dimension
     * @return the positional index position, or -1 if state dimension
     * value is not found
     */
    int getStateDimValueIndex( String state_dim_value ) 
    {
        for ( int val_idx = 0; 
              val_idx < _possible_states.length; 
              val_idx++ )
        {
            if ( _possible_states[val_idx].equalsIgnoreCase
                 ( state_dim_value ))
                return val_idx;
        } // for dim_idx
        
        return -1;

    } // method getStateDimValueIndex

    //************************************************************
    /**
     * Gets the positional index of the default state value for this
     * state dimension.
     *
     * @return default state index
     */
    int getDefaultStateIndex( ) 
    {
        return getStateDimValueIndex( _default_state_name );
        
    } // method getDefaultStateIndex

    //************************************************************
    /**
     * Returns the number of possible different values that this state
     * dimension can have.
     *
     * @return number of state dimension values.
     */
    int getNumStateDimValues( ) 
    {
        return _possible_states.length;

    } // method getNumStateDimValues

    //************************************************************
    /**
     * Get the name of a state value given the positional index value.
     *
     * @param val_idx The state dimension value index
     * @return The string of the state dimension value name, or null
     * if the state value of index is not valid. 
     */
    String getStateDimValueName( int val_idx ) 
    {
        if (( val_idx < 0 )
            || ( val_idx >= _possible_states.length ))
            return null;
        
        return _possible_states[val_idx];

    } // method getStateDimValueName

    //************************************************************
    /**
     * Look at all the sensors and get the maximum sensor latency for
     * all of them.
     *
     */
    long getMaxSensorLatency( )
            throws BelievabilityException
    {
        long max_latency = Long.MIN_VALUE;

        Enumeration sensor_enum = _sensor_model_set.elements();

        // Note that it is possible to have no sensors on a state
        // dimension, yet we may care to track its belief state since
        // it can be affected by threats and actuators.  For this
        // reason, and since the Belieftriggerhistory class needs some
        // latency in order to construct itself, we should return
        // something reasonable here.
        //
        if ( ! sensor_enum.hasMoreElements() )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "getMaxSensorLatency(). "
                      + "No sensor models found for this asset dimension: "
                      + _state_dim_name );
            return DEFAULT_SENSOR_LATENCY;
        }

        while ( sensor_enum.hasMoreElements() )
        {
            SensorTypeModel s_model
                    = (SensorTypeModel) sensor_enum.nextElement();

            if ( s_model.getLatency() >  max_latency )
                max_latency = s_model.getLatency();

        } // while sensor_iter

        return max_latency;

    } // method getMaxSensorLatency

    //************************************************************
    /**
     * Check to see if we have a sensor of the given name
     *
     * @param sensor_name The name of the sensor
     */
    boolean hasSensorName( String sensor_name )
    {
        Enumeration sensor_enum = _sensor_model_set.elements();

        while ( sensor_enum.hasMoreElements() )
        {
            SensorTypeModel s_model
                    = (SensorTypeModel) sensor_enum.nextElement();

            if ( s_model.getName().equalsIgnoreCase( sensor_name ))
                return true;

        } // while sensor_iter

        return false;

    } // method getSensorLatency

    //************************************************************
    /**
     * Return the sensor latency
     *
     * @param sensor_name The name of the sensor
     */
    long getSensorLatency( String sensor_name )
            throws BelievabilityException
    {
        Enumeration sensor_enum = _sensor_model_set.elements();

        while ( sensor_enum.hasMoreElements() )
        {
            SensorTypeModel s_model
                    = (SensorTypeModel) sensor_enum.nextElement();

            if ( s_model.getName().equalsIgnoreCase( sensor_name ))
                return s_model.getLatency();

        } // while sensor_iter

        throw new BelievabilityException
                ( "AssetTypeDimensionModel.getSensorLatency()",
                  "Did not find sensor name " + sensor_name
                  + " for asset dimension " + _state_dim_name );

    } // method getSensorLatency

    //************************************************************
    /**
     * return the number fof potential sesnors looking at this state
     * dimension
     *
     */
    long getNumberOfSensors( )
    {
        return _sensor_model_set.size();

    } // method getNumberOfSensors

    //************************************************************
    /**
     * Returns the action transition probability matix from an
     * actuator model for the given action.
     *
     * @param action Contains the action information.
     */
    double[][] getActionTransitionMatrix( BelievabilityAction action )
           throws BelievabilityException
     {
        
        ActuatorTypeModel actuator_model
                = getActuatorTypeModel( action.getActuatorName() );

        return actuator_model.getTransitionProbabilityMatrix
                ( action.getActionValue() );

    } // method getActionTransitionMatrix

    //************************************************************
    /**
     * Returns the transition probability matix resulting from all
     * threats and all events that could affect this asset state
     * dimension (either directly or through trabnsitive events).
     *
     * @param asset_id The ID of the asset instance
     * @param start_time Start time (transitions are time-dependent)
     * @param end_time End time (transitions are time-dependent)
     */
    double[][] getThreatTransitionMatrix( AssetID asset_id,
                                          long start_time,
                                          long end_time )
            throws BelievabilityException
    {
        // See the extensive comments in EventInstanceCollection's
        // method getTransitionMatrix() for the gorey details of how
        // this is computed.
        //

        // First, get the event collection object for this asset
        // instance so we can see how many *different* events exist.
        //
        EventInstanceCollection event_collection
                = getEventInstanceCollection( asset_id );

        // The nature of the getEventInstanceCollection() code should
        // never result in a NULL being returned, but it never hurts
        // to check (it is supposed to create the set when it finds
        // there is none).  Also, if there are no events, then the
        // probability due to threats should be zero.  However, rather
        // than couting on that, we will assume that the absence of a
        // threat means that the "threat transition" effect is to not
        // change state, so we return the indentity matrix.
        //
        //
        if (( event_collection == null )
            || ( event_collection.getNumEvents() < 1 ))
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Event collection for asset '" 
                                + asset_id.getName() 
                                + "', state dim. '"
                                + _state_dim_name  
                                + " is null or has no events." );

            double[][] probs = new double[_possible_states.length]
                    [_possible_states.length];
            
            for ( int i = 0; i < _possible_states.length; i++ )
                probs[i][i] = 1.0;
            
            return probs;
        } // if no threats acting on this asset state dimension

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Event collection for asset '" 
                            + asset_id.getName() 
                            + "', state dim. '"
                            + _state_dim_name + "'\n"
                            + event_collection.toString() );

        // All the hard work is done in here.
        //
        return event_collection.getTransitionMatrix( start_time,
                                                     end_time );
        
    } // method getThreatTransitionMatrix

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetStateDimension _state_dim;

    private String _state_dim_name;
    private String _default_state_name;
    private String[] _possible_states;
    private double[][] _utilities;

    // Contains all the sensor type models that monitor this state
    // dimension.
    //
    private Hashtable _sensor_model_set = new Hashtable();

    // Contains all the actuator type models that can affect this
    // state dimension.
    //
    private Hashtable _actuator_model_set = new Hashtable();

    // Contains all the stresses that affect this state dimension.
    // Note however that just because a stress is defined to affect a
    // particular state dimension, this does not mean this stress is
    // applicable to all assets of this type. Further, the exact set
    // of stress that affect a particular asset instance can change
    // over time.
    //
    private HashSet _stress_set = new HashSet();

    // This maps asset ID keysinto EventInstanceCollection objects.
    // This uses lazy evaluation, so that we add an event collection
    // at the first time we are asked to cmpute a belief update.
    // Further, on cougaar CHANGE events, we do not try to pluck out
    // the thing that changed, but rather delete the asset ID from
    // this tabel and allow the lazy evaluation to reconstruct this
    // event collection for the asset.
    //
    private Hashtable _asset_event_collection_table = new Hashtable();

} // class AssetTypeDimensionModel
