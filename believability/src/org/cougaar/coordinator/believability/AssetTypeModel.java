/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetTypeModel.java,v $
 *</NAME>
 *
 *<COPYRIGHT>
 *  Copyright 2004 Telcordia Technologies, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;

/**
 * Believability component's representation for all information it
 * needs concerning an AssetType (from the tech specs).
 *
 * This is an obserable so that things that depend on its contents can
 * be notified is there is a change.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.30 $Date: 2004-12-14 01:41:47 $
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
        setContents( asset_type );
        setValidity( true );

    }  // constructor AssetTypeModel

    //************************************************************
    /**
     * Sets this objects internal values to be those form the given
     * asset state.
     *
     * @param asset_type The source of information for this model
     */
    void setContents( AssetType asset_type )
            throws BelievabilityException
    {
        if ( asset_type == null )
            return;

        this._asset_type = asset_type;
        
        _name = asset_type.getName();
        _societal_util = asset_type.getUtilityValue();
        Vector state_dim_list = asset_type.getCompositeState();

        if ( state_dim_list == null )
            throw new BelievabilityException
                    (  "AssetTypeModel.setContents()",
                      "NULL state dimension vector" );

        if ( state_dim_list.size() == 0 )
            throw new BelievabilityException
                    (  "AssetTypeModel.setContents()",
                       "Empty state dimension vector" );

        _state_dim_name = new String[state_dim_list.size()];
        _dim_model = new AssetTypeDimensionModel[state_dim_list.size()];

        Iterator state_dim_iter = state_dim_list.iterator();

        for ( int dim_idx = 0; state_dim_iter.hasNext(); dim_idx++ )
        {
            AssetStateDimension state_dim 
                    = (AssetStateDimension) state_dim_iter.next();

            _state_dim_name[dim_idx] = state_dim.getStateName();

            _dim_model[dim_idx] 
                    = new AssetTypeDimensionModel( state_dim );

        } // while state_dim_iter
        
    }  // method setContents

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
     * Get the positional index value for the given state dimension
     * name.
     *
     * @param state_dim_name The state dimension name
     * @return the positional index position, or -1 if state dimension
     * name not found
     */
    int getStateDimIndex( String state_dim_name ) 
    {
        if ( state_dim_name == null )
            return -1;
        
        for ( int dim_idx = 0; dim_idx < _state_dim_name.length; dim_idx++ )
        {
            if ( _state_dim_name[dim_idx].equalsIgnoreCase( state_dim_name ))
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
        if (( dim_idx < 0 )
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
     * Get the utilites for the given state dimension of this model
     *
     * @return the utilities for this state dimension as a two
     * dimensional array.   
     */
    double[][] getUtilities( String state_dim_name ) 
            throws BelievabilityException
    {

        int dim_idx = getStateDimIndex( state_dim_name );

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            throw new BelievabilityException
                    ( "AssetTypeModel.getUtilities()",
                      "Unreckognized state dimension name: "
                      + state_dim_name );
        
        return _dim_model[dim_idx].getUtilities();

    } // method getUtilities

    //************************************************************
    /**
     * Retrieve the AssetStateDimension object for the given named
     * asset state dimension.  
     *
     * @param state_dim_name The state dimension name
     * @return the matching state dimension or null if not found
     */
    AssetStateDimension getAssetStateDimension( String state_dim_name )
    {
        return _asset_type.findStateDimension( state_dim_name );
    } // method getAssetStateDimension

    //************************************************************
    /**
     * Retrieve the AssetStateDimension object for the given named
     * asset state dimension.  
     *
     * @param state_dim_name The state dimension name
     * @return the matching state dimension or null if not found
     */
    AssetTypeDimensionModel getAssetTypeDimensionModel( int dim_idx )
    {
        return _dim_model[dim_idx];
    } // method getAssetTypeDimensionModel

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
        if (( dim_idx < 0 )
            || ( dim_idx >= _dim_model.length ))
            return null;

        return _dim_model[dim_idx].getDefaultStateValue();
            
    } // method getDefaultStateValue

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
        if (( dim_idx < 0 )
            || ( dim_idx >= _dim_model.length ))
            return -1;

        return _dim_model[dim_idx].getDefaultStateIndex();

    } // method getDefaultStateIndex

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
        return _dim_model.length;

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

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            return -1;

        return _dim_model[dim_idx].getStateDimValueIndex( state_dim_value );

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
        if (( dim_idx < 0 )
            || ( dim_idx >= _dim_model.length ))
            return -1;

        return _dim_model[dim_idx].getNumStateDimValues();

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

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            return null;

        return _dim_model[dim_idx].getStateDimValueName( val_idx );

    } // method getStateDimValueName

    //************************************************************
    /**
     * Adds a sensor type model to this asset. 
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


        if ( diag_ts.getStateDimension() == null )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "diagnosis tech spec has NULL state dimension." );
        
        int dim_idx = getStateDimIndex
                ( diag_ts.getStateDimension().getStateName() );

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            throw new BelievabilityException
                    ( "AssetTypeModel.addSensorTypeModel()",
                      "Sensor has unknown state dimension : "
                      + diag_ts.getStateDimension().getStateName()
                + "from asset type -- " + toString() );

       return _dim_model[dim_idx].addSensorTypeModel( diag_ts );
        
    } // method addSensorTypeModel

    //************************************************************
    /**
     * Adds an actuator type model to this asset. 
     *
     * @param s_model The actuator type model to be added
     */
    ActuatorTypeModel addActuatorTypeModel( ActionTechSpecInterface action_ts )
            throws BelievabilityException
    {
        if ( action_ts == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addActuatorTypeModel()",
                      "Cannot add null actuator type model" );

        if ( action_ts.getStateDimension() == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addActuatorTypeModel(" 
                      + action_ts + ")",
                      "Actuator has NULL state dimension." );
        
        int dim_idx = getStateDimIndex
                ( action_ts.getStateDimension().getStateName() );

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            throw new BelievabilityException
                    ( "AssetTypeModel.addActuatorTypeModel()",
                      "Actuator has unknown state dimension : "
                      + action_ts.getStateDimension().getStateName()
                      + "from asset type -- " + toString() );

        return _dim_model[dim_idx].addActuatorTypeModel( action_ts );
        
    } // method addActuatorTypeModel

    //************************************************************
    /**
     * Adds a stress instance that is relevant to this asset type.
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
                    ( "AssetTypeModel.addStressInstance()",
                      "Cannot add null stress instance" );

        if ( stress.getStateDimension() == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.addStressInstance()" 
                      + stress + ")",
                      "Stress has NULL state dimension." );
        
        int dim_idx = getStateDimIndex
                ( stress.getStateDimension().getStateName() );

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            throw new BelievabilityException
                    ( "AssetTypeModel.addStressInstance()",
                      "Actuator has unknown state dimension : "
                      + stress.getStateDimension().getStateName() 
                + " from asset type -- " + toString() );

        _dim_model[dim_idx].addStressInstance( stress );

    } // method addStressInstance
 
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
        // We just relay this to the specific state dimension model,
        // as that is where the data structures for this are managed.
        //
        
        if ( tm_change == null )
            throw new BelievabilityException
                    ( "AssetTypeModel.handleThreatModelChange()",
                      "Found NULL for threat change object." );
        
        ThreatModelInterface threat_model = tm_change.getThreatModel();
        
        if ((threat_model == null )
            || ( threat_model.getThreatDescription() == null )
            || ( threat_model.getThreatDescription
                 ().getEventThreatCauses() == null ))
            throw new BelievabilityException
                    ( "AssetTypeModel.handleThreatModelChange()",
                      "Found NULL while trying to retrieve state dimension." );

        ThreatDescription td = threat_model.getThreatDescription();
        
        EventDescription ed
                = threat_model.getThreatDescription().getEventThreatCauses();
        
        String state_dim_name = ed.getAffectedStateDimensionName();
        
        int dim_idx = getStateDimIndex( state_dim_name );

        if (( dim_idx < 0 ) || ( dim_idx >= _dim_model.length ))
            throw new BelievabilityException
                    ( "AssetTypeModel.handleThreatModelChange()",
                      "Threat has unknown state dimension : "
                      + state_dim_name 
                + "from asset type -- " + toString() );
        
        _dim_model[dim_idx].handleThreatModelChange( tm_change );

    } // method handleThreatModelChange
     
    //************************************************************
    /**
     * Look at all the sensors for all state dimensions and get the
     * maximum sensor latency for this asset.
     *
     */
    public long getMaxSensorLatency( )
            throws BelievabilityException
    {
        long max_latency = Long.MIN_VALUE;
        
        for ( int dim_idx = 0; dim_idx < _dim_model.length; dim_idx++ )
        {
            long cur_latency = _dim_model[dim_idx].getMaxSensorLatency();
            
            if ( cur_latency >  max_latency )
                max_latency = cur_latency;
            
        } // for dim_idx

        return max_latency;
    } // method getMaxSensorLatency

    //************************************************************
    /**
     * Gets the sensor latency for a sensor on this asset type.
     *
     * @param sensor_name The name of the sensor
     */
    public long getSensorLatency( String sensor_name )
            throws BelievabilityException
    {
        for ( int dim_idx = 0; dim_idx < _dim_model.length; dim_idx++ )
        {
            if ( _dim_model[dim_idx].hasSensorName( sensor_name ))
                return  _dim_model[dim_idx].getSensorLatency( sensor_name );

        } // for dim_idx

        throw new BelievabilityException
                ( "AssetTypeModel.getSensorLatency()",
                  "Cannot find sensor name '"
                  + sensor_name + "for asset type '"
                  + _name + "'." );

    } // method getMaxSensorLatency

    //************************************************************
    /**
     * Look at all the sensors for all state dimensions and return the
     * total number of sensors across all state dimensions
     *
     */
    public long getNumberOfSensors( )
    {
        int sensor_count = 0;
        
        for ( int dim_idx = 0; dim_idx < _dim_model.length; dim_idx++ )
        {
            sensor_count += _dim_model[dim_idx].getNumberOfSensors();

        } // for dim_idx

        return sensor_count;
    } // method getNumberOfSensors

    //************************************************************
    /**
     * Return whether the given sensor name needs for us to provide
     * implicit diagnoses for this sensor.
     *
     * @param sensor_name The name of the sensor we are querying
     */
    public boolean usesImplicitDiagnoses( String sensor_name )
            throws BelievabilityException
    {
        
        // Given only a sensor name, we cannot know what state
        // dimension it is sensing, so we loop over all the state
        // dimensioons until we one that has a sensor of the given
        // name.
        //

        for ( int dim_idx = 0; dim_idx < _dim_model.length; dim_idx++ )
        {
            SensorTypeModel sensor 
                    = _dim_model[dim_idx].getSensorTypeModel( sensor_name );

            if ( sensor == null )
                continue;

            return sensor.usesImplicitDiagnoses();

        } // for dim_idx

        throw new BelievabilityException
                ( "AssetTypeModel.usesImplicitDiagnoses()",
                  "Did not find sensor name " + sensor_name
                  + " for asset type " + _name );

    } // method usesImplicitDiagnoses

    //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {

        StringBuffer buff = new StringBuffer();

        buff.append( "\tAsset type name: " + _name + "\n" );

        buff.append( "\tSocietal utility: " + _societal_util + "\n" );

        buff.append( "\tState dimensions ["
                     + _state_dim_name.length
                     + "]\n" );
        
        for ( int dim_idx = 0; dim_idx < _dim_model.length; dim_idx++ )
        {
            buff.append( _dim_model[dim_idx].toString() );

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

    private AssetTypeDimensionModel[] _dim_model;

} // class AssetTypeModel
