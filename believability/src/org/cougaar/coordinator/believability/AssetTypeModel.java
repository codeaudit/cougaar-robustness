/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetTypeModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/AssetTypeModel.java,v $
 * $Revision: 1.2 $
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetType;

/**
 * Believability component's representation for all information it
 * needs concerning an AssetType (from the tech specs).
 *
 * This is an obserable so that things that depend on its contents can
 * be notified is there is a change.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-20 21:39:49 $
 * 
 *
 */
class AssetTypeModel extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Main constructor for this model.
     *
     * @param asset_type The source of information for this model
     */
    AssetTypeModel( AssetType asset_type )
            throws BelievabilityException
    {
        updateContents( asset_type );

    }  // constructor AssetTypeModel

    //************************************************************
    /**
     * Resets this objects internal values to be those form the given
     * asset state.
     *
     * @param asset_type The source of information for this model
     */
    void updateContents( AssetType asset_type )
            throws BelievabilityException
    {
        try
        {
            // First try to set up all the asset state model things.
            setContents( asset_type );

            // Now we need to verify that any sensors we have
            // monitoring this asset are still consistent with the
            // update asset state contents.
            Enumeration sensor_enum = _sensor_model_set.elements();
            while ( sensor_enum.hasMoreElements() )
            {
                SensorTypeModel s_model
                        = (SensorTypeModel) sensor_enum.nextElement();

                // If any of the sensors are not consistent, then this
                // model is no longer valid.
                //
                if ( ! s_model.isStillConsistent())
                    setValidity( false );

            } // while sensor_iter
        } // try

        // Whether or not an exception is thrown, we want to notify
        // the observers that we have changed.  If we atempt to update
        // the contents and fail, then the model will be in an invalid
        // state so we need to notify the obvservers of this.
        //
        finally
        {
            setChanged();
            notifyObservers();
        }
        
    } // method updateContents

    //************************************************************
    /**
     * Get the name of the asset type
     *
     * @return the name of the asset type
     */
    String getName() { return _name; }

    //************************************************************
    /**
     * Get the tech spec asset type of the asset type model
     *
     * @return the tech spec asset type of this model
     */
    AssetType getAssetType() { return _asset_type; }
    
    //************************************************************
    /**
     * Get the utilites for the given state dimension of this model
     *
     * @return the utilities for this state dimension as a two
     * dimensional array.   
     */
    double[][] getUtilities( String state_dim_name ) 
            throws BelievabilityException
    {
        if ( ! isValid() )
            throw new BelievabilityException
                    ( "AssetTypeModel.getUtilities()",
                      "Model is not in a valid state." );
   
        for ( int dim_idx = 0; dim_idx < _state_dim_name.length; dim_idx++ )
        {
            if ( _state_dim_name[dim_idx].equals( state_dim_name ))
                return _utilities[dim_idx];
        } // for dim_idx

        throw new BelievabilityException
                ( "AssetTypeModel.getUtilities()",
                  "Unreckognized state dimension name." );

    } // method getUtilities

    //************************************************************
    /**
     * Get the positional index value for the given state dimension
     * name.
     *
     * @param state_dim_name The state dimension name
     * @return the positional index position, or -1 if state dimension
     * name not found
     */
    int getStateDimIndex( String state_dim_name ) 
    {
        if (( ! isValid() )
            || ( state_dim_name == null ))
            return -1;
        
        for ( int dim_idx = 0; dim_idx < _state_dim_name.length; dim_idx++ )
        {
            if ( _state_dim_name[dim_idx].equals( state_dim_name ))
                return dim_idx;
        } // for dim_idx

        return -1;

    } // method getStateDimIndex

     //************************************************************
    /**
     * Get the name for the given state dimension given the positional
     * index value.
     *
     * @param dim_idx The positional index of the sttae dimension
     * @return the name of the state dimension or null if there is a
     * problem 
     */
    String getStateDimName( int dim_idx ) 
    {
        if (( ! isValid() )
            || ( dim_idx < 0 )
            || ( dim_idx >= _state_dim_name.length ))
            return null;
        
        return _state_dim_name[dim_idx];

    } // method getStateDimName

    //************************************************************
    /**
     * Checks to see if the parameter corresponds to a valid state
     * dimension name for this asset type model. 
     *
     * @param state_dim_name The state dimension name
     * @return true if name matches one of the stae dimensions, and
     * false if not.
     */
    boolean isValidStateDimName( String state_dim_name ) 
    {
        return getStateDimIndex( state_dim_name ) >= 0;
    } // method isValidStateDimName

    //************************************************************
    /**
     * Gets the default state value for a given sttae dimension
     *
     * @param dim_idx state dimension positional index
     * @return default state name or null if the model is invalid or
     * the positional index is invalid 
     */
    String getDefaultStateValue( int dim_idx ) 
    {
        if (( ! isValid() )
            || ( dim_idx < 0 )
            || ( dim_idx >= _state_dim_default.length ))
            return null;

        return _state_dim_default[dim_idx];
            
    } // method isValidStateDimName

    //************************************************************
    /**
     * Gets the default state value for a given sttae dimension
     *
     * @param state_dim_name The state dimension name
     * @return default state name or null if the model is invalid or
     * the positional index is invalid
     */
    String getDefaultStateValue( String state_dim_name ) 
    {
        int dim_idx = getStateDimIndex( state_dim_name );
        
        if ( dim_idx < 0 )
            return null;

        return getDefaultStateValue( dim_idx );
            
    } // method getDefaultStateValue

    //************************************************************
    /**
     * Gets the positional index of the default state value for the
     * given state dimension.
     *
     * @param state_dim_name The state dimension name
     * @return default state index or -1 if the model is invalid or
     * the positional index is invalid
     */
    int getDefaultStateIndex( int dim_idx ) 
    {
        if (( ! isValid())
            || ( dim_idx < 0 )
            || ( dim_idx >= _possible_states.length ))
            return -1;

        String default_state = getDefaultStateValue( dim_idx );
        
        if ( default_state == null )
            return -1;
        
        for ( int val_idx = 0; 
              val_idx < _possible_states[dim_idx].length;
              val_idx++ )
        {
            if ( _possible_states[dim_idx][val_idx].equals( default_state ))
                return val_idx;
        } // for val_idx
           
        return -1;

    } // method getDefaultStateValue

     //************************************************************
    /**
     * Gets the positional index of the default state value for the
     * given state dimension.
     *
     * @param state_dim_name The state dimension name
     * @return default state index or -1 if the model is invalid or
     * the positional index is invalid
     */
    int getDefaultStateIndex( String state_dim_name ) 
    {
        int dim_idx = getStateDimIndex( state_dim_name );

        return getDefaultStateIndex( dim_idx );

    } // method getDefaultStateValue

    //************************************************************
    /**
     * Returns the number of possible different values that this state
     * dimension can have.
     *
     * @return number of state dimension values, or -1 if the name is
     * invalid. 
     */
    int getNumStateDims( ) 
    {
        if ( ! isValid() )
            return -1;

        return _state_dim_name.length;

    } // method getNumStateDims

    //************************************************************
    /**
     * Get the positional index value for the given state dimension
     * value.
     *
     * @param state_dim_name The state dimension name
     * @param state_dim_value The state dimension value
     * @return the positional index position, or -1 if state dimension
     * name or value is not found
     */
    int getStateDimValueIndex( String state_dim_name,
                                      String state_dim_value ) 
    {
        int dim_idx = getStateDimIndex( state_dim_name );

        if ( dim_idx < 0 )
            return -1;

        for ( int val_idx = 0; 
              val_idx < _possible_states[dim_idx].length; 
              val_idx++ )
        {
            if ( _possible_states[dim_idx][val_idx].equals( state_dim_value ))
                return val_idx;
        } // for dim_idx

        return -1;

    } // method getStateDimValueIndex

    //************************************************************
    /**
     * Checks to see if the parameter corresponds to a valid state
     * dimension value for the given state dimension name. 
     *
     * @param state_dim_name The state dimension name
     * @param state_dim_value The state dimension value
     * @return true if name matches one of the state dimension
     * valuess, and false if not or if the state dimension name is
     * invalid. 
     */
    boolean isValidStateDimValueName( String state_dim_name,
                                             String state_dim_value ) 
    {
        return getStateDimValueIndex( state_dim_name,
                                      state_dim_value ) >= 0;
    } // method isValidStateDimValueName

    //************************************************************
    /**
     * Returns the number of possible different values that this state
     * dimension can have.
     *
      * @param dim_idx The positional index of the state dimension
    * @return number of state dimension values, or -1 if the name is
     * invalid. 
     */
    int getNumStateDimValues( int dim_idx ) 
    {
        if (( ! isValid())
            || ( dim_idx < 0 )
            || ( dim_idx >= _possible_states.length ))
            return -1;

        return _possible_states[dim_idx].length;

    } // method getNumStateDimValues

    //************************************************************
    /**
     * Returns the number of possible different values that this state
     * dimension can have.
     *
     * @param state_dim_name The state dimension name
     * @return number of state dimension values, or -1 if the name is
     * invalid. 
     */
    int getNumStateDimValues( String state_dim_name ) 
    {
        int dim_idx = getStateDimIndex( state_dim_name );

        return getNumStateDimValues( dim_idx );

    } // method getNumStateDimValues

    //************************************************************
    /**
     * Get the name of a state value given the state diumension and a
     * positional index value.
     *
     * @param state_dim_name The state dimension name
     * @param val_idx The state dimension value index
     * @return The string of the state dimension value name, or null
     * if the state dimension of index is not found. 
     */
    String getStateDimValueName( String state_dim_name,
                                        int val_idx ) 
    {
        int dim_idx = getStateDimIndex( state_dim_name );

        if ( dim_idx < 0 )
            return null;

        if (( val_idx < 0 )
            || ( val_idx >= _possible_states[dim_idx].length ))
            return null;

        return _possible_states[dim_idx][val_idx];

    } // method getStateDimValueName

    //************************************************************
    /**
     * Adds a sensor type model to this asset. Keep track of them and
     * also add this asset model as an observer of changes to the
     * sensor model. 
     *
     * @param s_model The sensor type model to be added and monitored
     */
    void addSensorTypeModel( SensorTypeModel s_model )
            throws BelievabilityException
    {
        if ( s_model == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addSensorTypeModel()",
                      "Cannot add null sensor type model" );

        _sensor_model_set.put( s_model.getName(),
                               s_model );

        s_model.addObserver( this );
        
    } // method addSensorTypeModel

    //**************************************************************
    /** 
     * This is the routine that implements the Observer interface.
     * This class cares about changes to existing SensorTypeModel
     * objects.
     * 
     * @param Observable The object that changed.
     * @param Object An argument about how it changed.
     */
    public void update( Observable observable, Object arg ) 
    {
        if ( ! ( observable instanceof SensorTypeModel ))
            return;

        // If sensor type changed, then just check to see if it is
        // still valid.  If not, then this asset type model is no
        // longer valid. 
        //
        if ( ! ((SensorTypeModel) observable).isValid() )
            setValidity( false );

        // Propogate the change to the asset model observers.
        //
        setChanged();
        notifyObservers();

    } // update

    //************************************************************
    /**
     * Look at all the sensors and get the maximum sensor latency for
     * all of them.
     *
     */
    public long getMaxSensorLatency( )
            throws BelievabilityException
    {
        long max_latency = Long.MIN_VALUE;

        Enumeration sensor_enum = _sensor_model_set.elements();

        if ( ! sensor_enum.hasMoreElements() )
            throw new BelievabilityException
                    ( "AssetTypeModel.getMaxSensorLatency",
                      "No sensor models found for this asset type." );
        
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
     * Convert this model to a string.
     *
     */
    public String toString( )
    {

        if ( ! isValid() )
            return "AssetTypeModel is in an invalid state.";

        StringBuffer buff = new StringBuffer();

        buff.append( "\tAsset type name: " + _name + "\n" );

        buff.append( "\tSocietal utility: " + _societal_util + "\n" );

        buff.append( "\tState dimensions ["
                     + _state_dim_name.length
                     + "]\n" );
        
        for ( int dim_idx = 0; dim_idx < _state_dim_name.length; dim_idx++ )
        {

           buff.append( "\t\t  Dimension name: " 
                        + _state_dim_name[dim_idx] 
                        + "\n" );
            buff.append( "\t\t\tDefault value: " 
                         + _state_dim_default[dim_idx] + "\n" );

            buff.append( "\t\t\tPossible values: [" );
            for ( int val_idx = 0; 
                  val_idx < _possible_states[dim_idx].length;
                  val_idx++ )
            {
                buff.append( " " + _possible_states[dim_idx][val_idx] );
            } // for val_idx
            buff.append( " ]\n" );

            buff.append( "\t\t\tUtilities:\n" );

            for ( int val_idx = 0; 
                  val_idx < _possible_states[dim_idx].length;
                  val_idx++ )
            {
                for ( int mau_idx = 0 ; 
                      mau_idx < MAUWeightModel.NUM_WEIGHTS;
                      mau_idx++ )
                {
                    
                    buff.append( "\t\t\t  V("
                                 + _possible_states[dim_idx][val_idx]
                                 + "," + MAUWeightModel.WEIGHT_NAME[mau_idx]
                                 + ") = "
                                 + _utilities[dim_idx][val_idx][mau_idx] 
                                 + "\n" );
                } // for val_idx

            } // for mau_idx

        } // for dim_idx

        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Original handle to source of model information.
    //
    private AssetType _asset_type;

    // These are all the attributes we need for an aset model.
    //
    private String _name;
    private int _societal_util;
    private String[] _state_dim_name;
    private String[] _state_dim_default;
    private String[][] _possible_states;
    private double[][][] _utilities;

    // Contains all the sensor type models that monitor state
    // dimensions of this asset.  
    //
    private Hashtable _sensor_model_set = new Hashtable();

    //************************************************************
    /**
     * Sets this objects internal values to be those form the given
     * asset state.
     *
     * @param asset_type The source of information for this model
     */
    private void setContents( AssetType asset_type )
            throws BelievabilityException
    {
        if ( asset_type == null )
            return;

        setValidity( false );

        this._asset_type = asset_type;
        
        _name = asset_type.getName();
        _societal_util = asset_type.getUtilityValue();
        Vector state_dim_list = asset_type.getCompositeState();

        if ( state_dim_list == null )
            throw new BelievabilityException
                    (  "AssetTypeModel.asetContents()",
                      "NULL state dimension vector" );

        if ( state_dim_list.size() == 0 )
            throw new BelievabilityException
                    (  "AssetTypeModel.asetContents()",
                       "Empty state dimension vector" );

        _state_dim_name = new String[state_dim_list.size()];
        _state_dim_default = new String[state_dim_list.size()];
        _possible_states = new String[state_dim_list.size()][];
        _utilities = new double[state_dim_list.size()][][];

        Iterator state_dim_iter = state_dim_list.iterator();

        for ( int dim_idx = 0; state_dim_iter.hasNext(); dim_idx++ )
        {
            AssetStateDimension state_dim 
                    = (AssetStateDimension) state_dim_iter.next();

            _state_dim_name[dim_idx] = state_dim.getStateName();
            _state_dim_default[dim_idx] 
                    = state_dim.getDefaultState().getName();

            if ( _state_dim_default[dim_idx] == null )
                throw new BelievabilityException
                        (  "AssetTypeModel.asetContents()",
                           "NULL found for default state.");

            Vector asset_state_list = state_dim.getPossibleStates();

            if ( asset_state_list == null )
                throw new BelievabilityException
                        (  "AssetTypeModel.asetContents()",
                           "NULL found for possible state vector.");

            if ( asset_state_list.size() == 0 )
                throw new BelievabilityException
                        (  "AssetTypeModel.asetContents()",
                           "Empty possible state vector.");

            _possible_states[dim_idx] = new String[asset_state_list.size()];
            _utilities[dim_idx] = new double[asset_state_list.size()][];

           Iterator asset_state_iter = asset_state_list.iterator();

           for ( int val_idx = 0; asset_state_iter.hasNext(); val_idx++ )
            {
                AssetState state = (AssetState) asset_state_iter.next();

                 _possible_states[dim_idx][val_idx] = state.getName();

                _utilities[dim_idx][val_idx] 
                        = new double[MAUWeightModel.NUM_WEIGHTS];
 
                _utilities[dim_idx][val_idx][MAUWeightModel.COMPLETENESS_IDX]
                        = state.getRelativeMauCompleteness();

                _utilities[dim_idx][val_idx][MAUWeightModel.SECURITY_IDX]
                        = state.getRelativeMauSecurity();

                // This doesn't appear to be implemented at the time
                // of first coding this class.
                //
                _utilities[dim_idx][val_idx][MAUWeightModel.TIMELINESS_IDX]
                        = 0.0f;
                
            } // while asset_state_iter

        } // while state_dim_iter
        
        // Only at this point do we know that we have successfully set
        // all the values.
        //
        setValidity( true );

    }  // method setContents

} // class AssetTypeModel
