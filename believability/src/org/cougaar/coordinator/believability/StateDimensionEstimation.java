/*
 * StateDimensionEstimation.java
 *
 * Created on April 21, 2004
 * <copyright>
 *  Copyright 2004 Telcordia Technoligies, Inc.
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
 * </copyright>
 */

package org.cougaar.coordinator.believability;

import java.util.Hashtable;
import java.util.Enumeration;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetID;


/**
 * This class represents the belief state along a single state dimension
 * of an asset.
 **/
public class StateDimensionEstimation extends Object
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor
     *
     * @param asset_model the asset model for this asset that we have
     *                    an estimation for
     * @param state_dimension the asset state dimension that this 
     *                        estimation concerns
     **/
    public StateDimensionEstimation( AssetModel asset_model,
				     AssetStateDimension state_dimension )
	throws BelievabilityException {

	_state_dimension = state_dimension;
	_asset_model = asset_model;
        _asset_id = _asset_model.getAssetID();
	_state_dimension_name = state_dimension.getStateName();
	_num_states = state_dimension.getNumStates();
    } // end constructor


    /**
     * Get the identifier of the asset that this estimation concerns
     * @return the identifier of the asset
     **/
    public AssetID getAssetID() { return _asset_id; }


    /** 
     * Get the name of the state dimension that this estimation concerns
     * @return the name of the state dimension
     **/
    public String getAssetStateDimensionName() { 
	return _state_dimension_name;
    }


    /**
     * Set a probability in this belief state using a numeric state.
     *
     * @param state The state to set the probability to as an integer.
     * @param prob The probability to set this state to, opverwriting
     * any previous value.  
     */
    public void setProbability( String state, double prob )
    {
        _belief.put( state, new Double( prob ));
    }




    /**
     * Set this belief state to be the same as the array values passed
     * in. 
     *
     * @param belief The belief state as an array
     * @return A probability distribution over asset stattes as an
     * array 
     */
    public void setProbabilities ( double[] belief )
	throws BelievabilityException {

        for ( int i = 0; i < _num_states; i++ ) {
	    String state_name = 
		_state_dimension.StateIndexToStateName( i );
	    if ( state_name != null )
		setProbability( state_name, belief[i] );
	    else throw new BelievabilityException( 
                "StateDimensionEstimation.setProbabilities",
		"No state corresponding to index " + i 
                  + " in state dimension " + _state_dimension_name
		  + " of asset " + _asset_id.toString() );
	}
    } // setProbabilities


    /**
     * Gets a probability to this belief state using a numeric state.
     *
     * @param state The state to set the probability to as an integer.
     * @exception BelievabilityException if the belief state is not known.
     */
    public double getProbability( String state )
            throws BelievabilityException
    {
        
        Double d = (Double) _belief.get( state );
        if ( d != null ) 
            return d.doubleValue();
	else throw new BelievabilityException( 
		"StateDimensionEstimation.getProbability",
		"No state named " + state
                  + " in state dimension " + _state_dimension_name
		  + " of asset " + _asset_id.toString() );
    }  // method get


    /**
     * Compute the utility of being in the current state in this state
     * dimension. This takes into account the relative utility
     * percentages (e.g. from the asset tech specs) and the MAU inputs.
     *
     * @throws BelievabilityException if there is no utility information
     *             available for the asset.
     * @return the utility for this state dimension.
     **/
    public double getUtility( ) throws BelievabilityException {
	UtilityWeights uw = _asset_model.getUtilityWeights();
	if ( uw != null ) return uw.computeSDUtility( this );
	else throw new BelievabilityException(
	      "StateDimensionEstimation.getUtility",
	      "No utility weights available for asset " + _asset_id.toString() );
    }


    /**
     * Return an enumeration of the names of all of the states in this
     * StateDimensionEstimation. This is the list of known state names
     * that currently have associated probabilities, as opposed to the
     * list of known state names from the AssetStateDimension.
     *
     * @return an Enumeration of known states with associated probabilities
     **/

    public Enumeration getStateNames() {
	return _belief.keys();
    }
    

    /**
     * Clone this StateDimensionEstimation
     * @throws BelievabilityException but really shouldn't
     * @return the cloned StateDimensionEstimation
     **/
    public StateDimensionEstimation cloneSDE() 
	throws BelievabilityException {

	StateDimensionEstimation sde = 
	    new StateDimensionEstimation( _asset_model,
					  _state_dimension );
	Enumeration state_names = getStateNames();
	while ( state_names.hasMoreElements() ) {
	    String sn = (String) state_names.nextElement();
	    sde.setProbability( sn, this.getProbability( sn ) );
	}
	return sde;
    }


    /**
     * Returns a string represenation of the StateDimensionEstimation
     * @return the string representation.
     **/
    public String toString() {
        StringBuffer buff = new StringBuffer();
        
        buff.append( _asset_id.toString() 
                     + " : " + _state_dimension_name
                     + " : [" );

        Enumeration key_enum = getStateNames();

	while ( key_enum.hasMoreElements()) {
	    String state = (String) key_enum.nextElement();
	    buff.append( " " + state + ":" + _belief.get( state ) );
	}

        buff.append( " ]" );
        
        return buff.toString();

    } // method toString


    /**
     * Convert this belief state to an array.
     * @return A probability distribution over asset stattes as an
     * array 
     */
    public double[] toArray( )
	throws BelievabilityException {
    
	double[] belief = new double[_num_states];

	// This next few lines may throw a BelievabilityException
	// copy the probabilities
	for ( int i = 0; i < _num_states; i++ ) {
	    String state_name = _state_dimension.StateIndexToStateName ( i );
	    
	    // Set the value in the belief array
	    if ( state_name != null ) 
		belief[i] = getProbability( state_name );
	    else throw new BelievabilityException( 
		"StateDimensionEstimation.toArray",
		"No state at index " + i
                  + " in state dimension " + _state_dimension_name
		  + " of asset " + _asset_id.toString() );
	}
	return belief;
    } // method toArray


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------
    
    // The ID and model of the asset
    private AssetModel _asset_model = null;
    private AssetID _asset_id = null;

    // The name of the state dimension
    private String _state_dimension_name = null;

    // The actual state dimension
    private AssetStateDimension _state_dimension = null;

    // The number of states in the state dimension
    private int _num_states = 0;

    // A hashtable of states and probabilities
    private Hashtable _belief = new Hashtable();

} // class StateDimensionEstimation
