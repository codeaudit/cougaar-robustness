/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetTypeDimensionModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/AssetTypeDimensionModel.java,v $
 * $Revision: 1.6 $
 * $Date: 2004-06-22 04:02:12 $
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

/**
 * Holds model information specific to a given state dimension.  This
 * model will be contained within an AssetTypeModel and roughly
 * corresponds to the tech-spec AssetSatteDimension objects.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.6 $Date: 2004-06-22 04:02:12 $
 * @see AssetTypeModel
 * @see AssetStateDimension
 */
class AssetTypeDimensionModel extends Model
{

    // Class implmentation comments go here ...

    // This is for testing to ignore any event probability that comes
    // from the tech-specs.  If 'true' this will force it to have a
    // small non-zero probility to help debug the belief update
    // calculations.  The tech spec derived probabilities are
    // time-dependent making testing difficult.
    //
    private static final boolean OVERRIDE_EVENT_PROBABILITIES = false;
    
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
            logWarning( "Replacing sensor type model: "
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
            logWarning( "Replacing actuator type model: "
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
     * Adds a threat variation model to this asset dimension. 
     *
     * @param threat_var The threat variation model to be added
     */
    ThreatVariationModel addThreatVariationModel
            ( ThreatModelInterface threat_mi )
            throws BelievabilityException
    {
        if ( threat_mi == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addThreatVariationModel()",
                      "Cannot add null ThreatModelInterface model" );

        
        // Need to first check if common threat information
        // (ThreatRootModel) exists.  If not, we create it.
        //
        ThreatRootModel threat_root_model 
                = (ThreatRootModel) _threat_model_set.get( threat_mi.getName());
        
        if ( threat_root_model == null )
        {
            threat_root_model = new ThreatRootModel
                    ( threat_mi.getThreatDescription(), 
                      this );

            _threat_model_set.put( threat_mi.getName(),
                                   threat_root_model );

            logDebug( "Created new ThreatRootModel: " 
                      + threat_root_model.toString() );

        } // if didn't have threat root model

        // Now we can add this variation safely to the root.
        //
        ThreatVariationModel threat_var
                =  threat_root_model.addThreatVariation( threat_mi );

        logDebug( "Created new ThreatVariationModel: " 
                  + threat_var.toString() );

        // Finally, if this Threat involves assets we already have
        // built up a set of threat variations for, then we will need
        // to invalidate (by deleting) that set.  Note that we could
        // alternatively add this to the existing set, but prefer to
        // allow our lazy reconstruction to rebuild this set when it
        // odes not exist.  The performance impact of this will likely
        // not mater given the infrequency of adding threat models.
        //

        Vector asset_list = threat_var.getAssetList();
        if ( asset_list == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addThreatVariationModel()",
                      "Found null asset list." );
            
        Enumeration asset_enum = asset_list.elements();
        while( asset_enum.hasMoreElements() )
        {
            AssetTechSpecInterface asset_ts
                    = (AssetTechSpecInterface) asset_enum.nextElement();

            // Because we build the threat variation set on the fly, it is
            // very possible that we may try to remove things that were
            // never created. In this case, we will do nothing silently.
            //
            _asset_threat_var_table.remove( asset_ts.getAssetID() );

        } // while enum */

        return threat_var;

    } // method addThreatVariationModel

    //************************************************************
    /**
     * Returns a collection of threat variations for the given asset
     * ID.
     *
     * @param asset_id The aset id whose threat variations we want.
     */
    ThreatVariationCollection getThreatVariations( AssetID asset_id )
    {
        // We use lazy evaluation for building the data structure, so
        // construct the set if it does not already exist.

        ThreatVariationCollection threat_var_set 
                = (ThreatVariationCollection) 
                _asset_threat_var_table.get( asset_id );

        // This should evaluate to true and return in the normal case
        // where we just return the threat variation set.  We only
        // progress past this point if we find that we need to create
        // the set (should be the first time we have seen this asset
        // ID, or the asset membership in a threat has changed.)
        //
        if ( threat_var_set != null )
            return threat_var_set;

        // Else, we need to construct the set.

        logDebug( "Threat variation collection not found for asset '" 
                  + asset_id.getName() 
                  + "' in dimension '" 
                  + _state_dim_name
                  + "'. Creating." );
        
        threat_var_set = new ThreatVariationCollection();
        _asset_threat_var_table.put( asset_id, threat_var_set );

        // Loop over all the threat root models and all their threat
        // variations, finding the ones that are applicable for this
        // state dimension.
        //
        Enumeration tm_enum = _threat_model_set.elements();
        while ( tm_enum.hasMoreElements() )
        {
            ThreatRootModel tm = (ThreatRootModel) tm_enum.nextElement();

            Iterator tv_iter = tm.getThreatVariationSet().iterator();

            while ( tv_iter.hasNext() )
            {
                ThreatVariationModel tv
                        = (ThreatVariationModel) tv_iter.next();

                if ( ! tv.isApplicable( asset_id ))
                    continue;

                threat_var_set.add( tv );

            } // while tv_iter
            
        } // while tm_enum
        
        logDebug( "Found " + threat_var_set.getNumThreatVariations() 
                  + " applicable threat variations for "
                  + asset_id.getName() );

        return threat_var_set;

    } // method getThreatVariations

    //************************************************************
    /**
     * Handle the situation where a threat model has changed the set
     * of assets it pertains to.  This could be the addition and/or
     * removal of assets.
     *
     * @param threat_model the threat model that has had the asset
     * membership change.
     * @param asset_id the ID of the asset affected by the threat
     * model change
     * @param change_type Whether this asset has been added or removed
     * from the threat
     */
    public void handleThreatModelChange( ThreatModelInterface threat_model,
                                         AssetID asset_id,
                                         int change_type )
            throws BelievabilityException
    {
        // We choose to keep the management of the mapping from asset
        // IDs to applicable threats simple.  In this way, on a threat
        // model change, we do not attempt the surgical manuevers
        // required to bring the data structure into compliance.
        // Instead, we simply wipe out the existence of any mapping
        // for this asset ID to the threats, and rely on a lazy
        // evaluation scheme of building the mapping only at the point
        // we need it and find it does not exist.  Note that this
        // implies that we ignore the 'change_type parameter.
        //
        logDebug( "Removing applicable threat variations for "
                  + asset_id.getName() );
 
        // Because we build the threat variation set on the fly, it is
        // very possible that we may try to remove things that were
        // never created. In this case, we will do nothing silently
        // (aside from the debug statement.
        //
        if ( _asset_threat_var_table.remove( asset_id ) == null )
            logDebug( "There were no applicable threat variations for "
                      + asset_id.getName() );

        
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
        // reason, and since the BeliefStateWindow class needs some
        // latency in order to construct itself, we should return
        // something reasonable here. 
        //
        if ( ! sensor_enum.hasMoreElements() )
        {
            logDebug( "getMaxSensorLatency(). "
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
     * Computes the threat probability for a given instance of an
     * asset.  This needs to account for all threats acting on this
     * asset's state dimension.
     *
     * @param threat_var_set The set of ThreatVariationModel objects
     * that define the threats that can lead to an event (assumes all
     * ThreatVariationModel in this set cause the same event.)
     * @param start_time time of last belief update
     * @param end_time time desired for new belief
     */
    double getEventProbability( Set threat_var_set,
                                long start_time,
                                long end_time )
            throws BelievabilityException
    {

        // Some assumptions:
        //
        //  o For a given coordinator event, there may be multiple
        //  threats defined for the asset that can cause this
        //  coordinator event.
        //
        //  o If multiple threats exist, then they occur independently
        //  from one another.
        //
        //  o If there are more than three threats causing the same
        //  event for this given asset state dimension, then this
        //  routine will only estimate the transition probability.
        //  This results from the computeEventUnionProbability()
        //  routine, which contains a comment explaining the
        //  rationale.
        // 

        // Having no threats implies no threat-induced event can
        // happen.
        //
        if (( threat_var_set == null )
            || ( threat_var_set.size() < 1 ))
            return 0.0;

        int num_threats = threat_var_set.size();

        double[] prob = new double[num_threats];
        
        // This loop will get the individual event probabilities for
        // each threat.
        //
        Iterator threat_var_iter = threat_var_set.iterator();
        for ( int idx = 0; threat_var_iter.hasNext(); idx++ )
        {
            ThreatVariationModel threat_var 
                    = (ThreatVariationModel) threat_var_iter.next();
        
            prob[idx] = threat_var.getEventProbability( start_time, end_time );

            logDebug( "Event prob. due to threat "
                      + threat_var.getName() + " is "
                      + prob[idx] );

            
        } // for idx

        return ProbabilityUtils.computeEventUnionProbability( prob );

    } // method getEventProbability

    //************************************************************
    /**
     * Returns the transition probability matix resulting from all
     * threats and all events that could affect this asset state
     * dimension.
     *
     * @param asset_id The ID of the asset instance
     */
    double[][] getThreatTransitionMatrix( AssetID asset_id,
                                          long start_time,
                                          long end_time )
            throws BelievabilityException
    {
        // Note that because of the parameterize-by-time nature of
        // the threat probabilities and the dynamic asset
        // membership of the threats, we *cannopt* precompute this
        // transition matrix. 
        //

        // Getting the transition probability matrix at the most
        // abstract is a two step process.  We have to find all the
        // unique events that are acting on this asset state
        // dimension, and blend their individual transition matrices
        // according to the probability of each event.  But to do
        // this, requires us to blend together all the possible
        // threats that can cause each event event.
        //
        
        // General algorithm: Find each event, and compute the
        // probability of each event (possibly factoring in multiple
        // threats.)  Blending the threats together is done in
        // ProbabilityUtils.computeEventUnionProbability(). Then,
        // consider all possible combinations of the events occurring
        // and not occurring, building up the transition matrix from
        // the individual event components.  This blending of events
        // occurs in the ProbabilityUtils.mergeMultiEventTransitions()
        // method.
        //

        // First, get the threat variation set so we can see how many
        // *different* events exist.
        //
        ThreatVariationCollection threat_var_collection
                = getThreatVariations( asset_id );

        logDebug( "Threat collection for asset '" + asset_id.getName() 
                  + "', state dim. '"
                  + _state_dim_name + "'\n"
                  + threat_var_collection.toString() );

        // The nature of the getThreatVariations() code should never
        // result in a NULL being returned, but it never hurts to
        // check (it is supposed to create the set when it finds there
        // is none).  Also, if there are no threats, then the
        // probability of the threat should be zero.  However, rather
        // than couting on that, we will assume that the absence of a
        // threat means that the "threat transition" effect is to not
        // change state, so we return the indentity matrix.
        //
        //
        if (( threat_var_collection == null )
            || ( threat_var_collection.getNumEvents() < 1 ))
        {
            double[][] probs = new double[_possible_states.length]
                    [_possible_states.length];
            
            for ( int i = 0; i < _possible_states.length; i++ )
                probs[i][i] = 1.0;
            
            return probs;
        } // if no threats acting on this asset state dimension

        // This array will hold (for each event) the transition matrix
        // *given* the event occurs. (We will assume the transitions
        // are the identity matrix for the case when the event does
        // not occur.) First index: the event, second index: the 'from'
        // asset state, third index: the 'to' asset state.
        //
        double event_trans[][][]
                = new double[threat_var_collection.getNumEvents()][][];

        // This will hold (for each event) the probability that the
        // event occurs (independent of all others).  This may need to
        // factor in one or more threats by computing the probability
        // of the union of all the threats that can cause a given
        // event.
        //
        double event_probs[]
                = new double[threat_var_collection.getNumEvents()];

        // Fill out the arrays by looping over each event.
        //
        Enumeration event_name_enum 
                = threat_var_collection.eventNameEnumeration();
        for ( int idx = 0; event_name_enum.hasMoreElements(); idx++ )
        {
            String event_name = (String) event_name_enum.nextElement();
            
            HashSet threat_var_set 
                    = (HashSet) threat_var_collection.getThreatVariationSet
                    ( event_name );

            //  The nature of this threat variation set is such that
            //  it should not be empty (we would not consider an event
            //  unless some threat could cause it), and every threat
            //  variation in there should refer to the same event.
            //  Because of that, we can get the first item and get the
            //  event transition from that.
            //
            ThreatVariationModel threat_var 
                    = (ThreatVariationModel) threat_var_set.iterator().next();
            
            event_trans[idx] = threat_var.getEventTransitionMatrix();

            event_probs[idx] = getEventProbability( threat_var_set,
                                                    start_time,
                                                    end_time );
            logDebug( "Event prob for  " + event_name 
                      + " is " + event_probs[idx] );
            
            if ( OVERRIDE_EVENT_PROBABILITIES )
            {
                logError( "OVERRIDE EVENT PROBS.  THIS IS FOR TESTING ONLY!");
                
                if ( event_probs[idx] < 0.0001 )
                    event_probs[idx] = 0.1;
                else if ( event_probs[idx] > 0.9999 )
                    event_probs[idx] = 0.9;
                else
                    event_probs[idx] = 0.5;

                logDebug( "New event prob for  " + event_name 
                          + " is " + event_probs[idx] );
            }

        } // for idx

        logDebug( "Event probs: " 
                  + ProbabilityUtils.arrayToString( event_probs ));
        
        logDebug( "Event transitions: " 
                  + ProbabilityUtils.arrayToString( event_trans ));
        
        // Ok, so now we have the individual state transition
        // probabilities for each event and the individual
        // probabilities that each event will occur.  Next up is
        // merging these into a single transition matrix. Note the
        // assumption that if an event does not occur, then the state
        // remains unchanged. Or more accurately, if no events occurs,
        // there is no state change.
        //
        return ProbabilityUtils.mergeMultiEventTransitions( event_probs,
                                                            event_trans );

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

    // Contains all the threat type models that affect this state
    // dimension.  Note however that just because a threat is defined
    // to affect a particular state dimension, this does not mean this
    // threat is applicable to all assets of this type. Further, the
    // exact set of threats that affect a particular asset instance
    // can change over time.
    //
    private Hashtable _threat_model_set = new Hashtable();

    // This maps asset IDs into a hashtable of threat variation
    // collections.  This uses lazy evaluation, so that we add a
    // threat variation at the first time we are asked for the
    // probability of an event.  Further, on threat model change
    // events, we do not try to pluck out the one changed threat, but
    // rather delete the asset ID from this tabel and allow the lazy
    // evaluation to reconstruct the set for the asset.
    //
    private Hashtable _asset_threat_var_table = new Hashtable();

} // class AssetTypeDimensionModel
