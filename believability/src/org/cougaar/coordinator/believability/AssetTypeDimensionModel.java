/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetTypeDimensionModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/AssetTypeDimensionModel.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-09 18:00:22 $
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
 * @version $Revision: 1.1 $Date: 2004-06-09 18:00:22 $
 * @see AssetTypeModel
 * @see AssetStateDimension
 */
class AssetTypeDimensionModel extends Model
{

    // Class implmentation comments go here ...
    
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
            logDebug( "Creating new ThreatRootModel: " 
                      + threat_mi.getName());

            threat_root_model = new ThreatRootModel
                    ( threat_mi.getThreatDescription(), 
                      this );

            _threat_model_set.put( threat_mi.getName(),
                                   threat_root_model );

        } // if didn't have threat root model

        // Now we can add this variation safely to the root.
        //
        ThreatVariationModel threat_var
                = new ThreatVariationModel( threat_root_model,
                                            threat_mi );

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
            AssetTechSpecInterface aset_ts
                    = (AssetTechSpecInterface) asset_enum.nextElement();

            // Because we build the threat variation set on the fly, it is
            // very possible that we may try to remove things that were
            // never created. In this case, we will do nothing silently.
            //
            _asset_threat_var_table.remove( aset_ts.getAssetID() );

        } // while enum */

        return threat_var;

    } // method addThreatVariationModel

    //************************************************************
    /**
     * returns a set of threat variations for the given asset ID.
     *
     * @param asset_id
     */
    Set getThreatVariationSet( AssetID asset_id )
    {
        // We use lazy evaluation for building the data structure, so
        // construct the set if it does not already exist.

        HashSet threat_var_set 
                = (HashSet) _asset_threat_var_table.get( asset_id );

        // This should evaluate to true and return in the normal case
        // where we just return the threat variation set.  We only
        // progress past this point if we find that we need to create
        // the set (should be the first time we have seen this asset
        // ID, or the asset membership in a threat has changed.)
        //
        if ( threat_var_set != null )
            return threat_var_set;

        // Else, we need to construct the set.

        logDebug( "Threat variation set not found for asset '" 
                  + asset_id.getName() 
                  + "' in dimension '" 
                  + _state_dim_name
                  + "'. Creating." );
        
        threat_var_set = new HashSet();

        // We make a simplifying assumption (for now) that all threats
        // affecting a particular sttae dimension, of a particular
        // asset, will all result in the same event happening. (See
        // comments elsewhere in the code.  Because of this, we
        // definitely want to check to see if this assumption is
        // violated.  We will find the event name of the first
        // matching threat, and then compare all others to this one.
        // 
        String event_name = null;

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

                if ( event_name == null )
                    event_name = tm.getEventName();
                else
                    if ( ! event_name.equalsIgnoreCase( tm.getEventName()))
                        logError( "ASSUMPTION VIOLATED! "
                                  + "There are different events that can "
                                  + "affect a single asset state dimension."
                                  + " (asset=" + asset_id.getName()
                                  + ", threat1=" + event_name
                                  + ", threat2=" + tm.getEventName() );
                
            } // while tv_iter
            
        } // while tm_enum
        
        logDebug( "Found " + threat_var_set.size() 
                  + " applicable threat variations for "
                  + asset_id.getName() );

        return threat_var_set;

    } // method getThreatVariationSet

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
            logDebug( "AssetTypeDimensionModel.getMaxSensorLatency(). "
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
     * Returns the event transition probability matix from an event
     * description.  
     *
     * @param asset_id The ID of the asset instance
     */
    double[][] getActionTransitionMatrix( BelievabilityAction action )
    {
        logError( "getActionTransitionMatrix() is not implements." );

        // FIXME: Need to find out info from OBJS to know how to
        // construct and access this info.
        return null;

    } // method getActionTransitionMatrix

    //************************************************************
    /**
     * Returns the event transition probability matix from an event
     * description.  
     *
     * @param asset_id The ID of the asset instance
     */
    double[][] getEventTransitionMatrix( AssetID asset_id )
    {
        // This uses the asset ID to find the applicable threat
        // variations, and from there we can determine the event
        // transitions for this asset instance (which are stored in
        // the ThreatRootModel).
        //

        // Note that we have a many-to-one relationship between
        // AssetID and ThreatVariationModels.  Each of these threat
        // variations will (likely) have different ThreatRootModels.
        // In general, there is nothing to stop each of these
        // ThreatRootModels having their threat generate different
        // events. However, as per conversations with OBJS, we will
        // not (in the near term) handle the case where multiple
        // events can simultaneously act on the same asset instance.
        // The main reason is that one has to either specify the full
        // joint probability distributions for combinations of events,
        // or make simplifying assumptions about what the semantics
        // are when more than one event occurs simultaneously.  For
        // now, we were not ready to do either of these, so make the
        // deep assumption that all ThreatRootModels for an given
        // AssetID have exactky the same Event.  Thus, we can grab any
        // ThreatRootModel from the set and use it to get at the event
        // transition probabilities.
        //

        Set threat_var_set = getThreatVariationSet( asset_id );

        // The nature of the getThreatVariationSet() code should never
        // result in a NULL being returned, but it never hurts to
        // check.  Also, if there are no threats, then the probability
        // of the threat should be zero.  However, rather than couting
        // on that, we will assume that the absence of a threat means
        // that the "event transition" is to not change state, so we
        // return the indentity matrix.
        //
        //
        if (( threat_var_set == null )
            || ( threat_var_set.size() < 1 ))
        {
            double[][] probs = new double[_possible_states.length]
                    [_possible_states.length];
            
            for ( int i = 0; i < _possible_states.length; i++ )
                probs[i][i] = 1.0;
            
            return probs;
        } // if no threats acting on this asset state dimension
        
        // Should be at least
        ThreatVariationModel threat_var 
                = (ThreatVariationModel) threat_var_set.iterator().next();
        
        return threat_var.getEventTransitionMatrix();

    } // method getEventTransitionMatrix

    //************************************************************
    /**
     * Computes the threat probability for a given instance of an
     * asset.  This needs to account for all threats acting on this
     * asset's state dimension.
     *
     * @param asset_id The ID of the asset instance
     * @param start_time time of last belief update
     * @param end_time time desired for new belief
     */
    double getEventProbability( AssetID asset_id,
                                long start_time,
                                long end_time )
            throws BelievabilityException
    {
        // Some assumptions:
        //
        //  o For a given asset instance, there is only one possible
        //  "coordinator event" that can cause this state dimension to
        //  change value.  (If there are multiple coordinator events
        //  that can cause a transition of this state dimension, then
        //  we would have to consider the joint probability
        //  distributions over all combinations of "coordinator
        //  events" when trying to predict the state transition, see
        //  comments in getEventTransitionMatrix() for more rational
        //  about this assumption.)
        //
        //  o For a given coordinator event, there may be multiple
        //  threats defined for the asset that can cause this
        //  coordinator event.
        //
        //  o If multiple threats exist, then they occur independently
        //  from one another.
        //
        //  o If there are more than three threats causing the same
        //  event for this given asset sate dimension, then this
        //  routine will only estimate the transition probability.
        //  The reason is that the general inclusion-exclusion rule
        //  for determining the probability of a disjunction of events
        //  (each individual threat) will result in computation time
        //  exponential in the number of threats. If an exact answer is
        //  needed for >=4 threats, then one will need to code the
        //  exponential time algorithm here, or if a less
        //  computationally intensive better approximation is needed
        //  then possibly implementing something similar to this might
        //  help: 
        //
        //   "Inclusion-Exclusion: Exact and Approximate" by Jeff
        //    Kahn, Nathan Linial, Alex Samorodnitsky.
        //    Combinatorica. 1993

        // 
        Set threat_var_set = getThreatVariationSet( asset_id );

        // No threats -> no event possible
        if ( threat_var_set != null )
            return 0.0;

        int num_threats = threat_var_set.size();

        // No threats -> no event possible
        if ( num_threats < 1 )
            return 0.0;

        double[] prob = new double[num_threats];
        
        double disjunction_prob = 0.0;
        double conjunction_prob = 1.0;

        // This loop will get the individual event probabilities for
        // each threat, and also accumulate the sum andproduct of al
        // these probabilities, since these are used in the
        // probability calculations for most cases.
        //
        Iterator threat_var_iter = threat_var_set.iterator();
        for ( int idx = 0; threat_var_iter.hasNext(); idx++ )
        {
            ThreatVariationModel threat_var 
                    = (ThreatVariationModel) threat_var_iter.next();
        
            prob[idx] = threat_var.getEventProbability( start_time, end_time );

            disjunction_prob += prob[idx];
            conjunction_prob *= prob[idx];
        } // for idx

        switch ( num_threats )
        {

        case 1:
            //--------------------
            // CASE: One threat:  
            //
            //        P( Event ) = Pr( T1 )
            //
            return prob[0];
            
        case 2:
            //--------------------
            // CASE: Two threats:  
            //
            //        P( Event ) = Pr( T1 or T2 )
            //                   = Pr( T1) + Pr( T2 ) - Pr( T1 and T2 )
            //
            // Then using the indpendence assumption:
            //
            //                   = Pr( T1) + Pr( T2 ) - Pr( T1 ) * Pr( T2 )
            //
            return disjunction_prob - conjunction_prob;

        case 3:
            //--------------------
            // CASE: Three threats:  
            //
            //  P(Event) = Pr( T1 or T2 or T3 )
            //           = Pr(T1) + Pr(T2) + Pr(T3) 
            //             - Pr(T1 and T2 ) - Pr(T2 and T3 ) - Pr(T1 and T3 )
            //             + Pr( T1 and T2 and T3 )
            //
            // Then using the indpendence assumption:
            //
            //      = Pr(T1) + Pr(T2) + Pr(T3) 
            //        - Pr(T1) * Pr(T2) - Pr(T2) * Pr(T3) - Pr(T1) * Pr(T3)
            //        + Pr(T1) * Pr(T2) * Pr(T3)
            //
            return disjunction_prob
                    - ( prob[0] * prob[1] ) 
                    - ( prob[1] * prob[2] ) 
                    - ( prob[0] * prob[2] )
                    + conjunction_prob;

        default:
            //--------------------
            // CASE: More than three threats:  
            //
            // Here we approximate (crudely) the probability with:
            //
            //   P( Event ) = \sum_i [ Pr( T_i ) - \prod_i Pr( T_i ) ]
            //
            // this approximation will over-estimate the probability.

            logError( "More than 3 threats. Probabilities only approximate.");

            return disjunction_prob - ( num_threats * conjunction_prob );
        
        } // switch num_threats

    } // method getEventProbability

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

    // This maps asset IDs into a set of threat variations.  This uses
    // lazy evaluation, so that we add a threat variation at the first
    // time we are asked for the probability of an event.  Further, on
    // threat model change events, we do not try to pluck out the one
    // changed threat, but rather delete the asset ID from this tabel
    // and allow the lazy evaluation to reconstruct the set for the
    // asset.
    //
    private Hashtable _asset_threat_var_table = new Hashtable();

} // class AssetTypeDimensionModel
