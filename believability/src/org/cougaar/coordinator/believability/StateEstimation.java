/*
 * StateEstimation.java
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

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetID;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This object is a hashtable that collects together, for an asset, the 
 * StateDimensionEstimations for each state dimension for the asset.
 * A StateDimensionEstimation is a probabilistic distribution, over a
 * specific AssetStateDimension, of the belief that the asset is in each
 * state of the AssetStateDimension.
 **/

public class StateEstimation extends Hashtable implements NotPersistable 
{
    /** 
     * Create a new instance of StateEstimation. It is empty, with no 
     * diagnoses and a current timestamp.
     **/
    public StateEstimation() { }
    

    /** 
     * Create a new instance of StateEstimation, based on the input 
     * diagnosis. This is meant to be used for times when the state
     * estimation is in response to a new diagnosis being received.
     * @param diagnosis The input diagnosis to the StateEstimation
     **/
    //    public StateEstimation( BelievabilityDiagnosis diagnosis ) {
    //	_diagnosis = diagnosis;
    //	_asset_id = diagnosis.getAssetID();
    //	_timestamp = diagnosis.getTimestamp();
    //    }


    /**
     * Create a new state estimation for an asset. This is done when
     * the state estimation is being made as of some time, without
     * any diagnoses being received explicitly.
     * @param asset_id The identifier of the asset that the 
     *                 state estimation is for.
     * @param timestamp The time when the state estimation was made.
     **/
    public StateEstimation( AssetID asset_id, long timestamp ) {
	_asset_id = asset_id;
	_timestamp = timestamp; 
    }


    /**
     * Get the identifier of the asset
     * @return the asset identifier
     **/
    public AssetID getAssetID() { return _asset_id; }


    /**
     * Get the timestamp for the diagnosis, which is also the time 
     * as of when the estimation was made.
     * @return The time when the diagnosis was received.
     **/
    public long getTimestamp() { return _timestamp; }


    /**
     * Get the StateDimensionEstimation for the named belief state
     * @param state_dimension the AssetStateDimension associated with 
     *                        the belief state
     * @throws BelievabilityException if there is no state estimation
     *                                information for the state.
     * @return the StateDimensionEstimation
     **/
    public StateDimensionEstimation 
	getStateDimensionEstimation( AssetStateDimension state_dimension ) 
	throws BelievabilityException {

	StateDimensionEstimation sde = 
	    (StateDimensionEstimation) get( state_dimension );
	if ( sde == null ) {
	    throw new BelievabilityException( 
                "StateEstimation.getStateDimensionEstimation",
		"No belief information for state " + 
		      state_dimension.getStateName());
	}
	else return sde;
    }


    /**
     * Get all StateDimensionEstimations for this StateEstimation
     * @return an enumeration of the StateDimensionEstimation elements.
     **/
    public Enumeration getStateDimensionEstimations() {
	return this.elements();
    }


    /**
     * Set the StateDimensionEstimation for the named belief state 
     * @param state_dimension the AssetStateDimension associated with 
     *                        the belief state
     * @param estimate the StateDimensionEstimation for the state dimension
     **/
    public void	
	setStateDimensionEstimation( AssetStateDimension state_dimension,
				     StateDimensionEstimation estimate ) {

	put( state_dimension, estimate );
    }


    /**
     * Get the diagnosis that caused the StateEstimation to happen.
     * May be null.
     * @return The diagnosis
     **/
    //    public BelievabilityDiagnosis getDiagnosis() { return _diagnosis; }


    /**
     * Compute the utility of being in the current state. This takes into 
     * account the relative utility percentages (e.g. from the asset tech
     * specs) and the MAU inputs. If some state dimension has no associated
     * belief state, its utility is assumed to be 0.
     *
     * @throws BelievabilityException if there is no utility information
     *                                available for the state dimension.
     * @return a utility measure.
     **/
    public double getStateUtility() throws BelievabilityException {

	double return_utility = 0;

	Enumeration sde_enum = this.getStateDimensionEstimations();
	while ( sde_enum.hasMoreElements() ) {
	    StateDimensionEstimation sde = 
		(StateDimensionEstimation) sde_enum.nextElement();
	    return_utility += sde.getUtility();
	}
	return return_utility;
    }


    /**
     * Compute the utility along a given state dimension of being in
     * the current state. This takes into account the relative 
     * utility percentages (e.g. from the asset tech specs) and the
     * MAU inputs.
     * @param state_dimension the AssetStateDimension for the state 
     *                         you are interested in.
     * @throws BelievabilityException if there is no state estimation
     *                                information or utility information
     *                                for the input AssetStateDimension
     * @return the utility for that state dimension.
     **/
    public double 
	getStateDimensionUtility( AssetStateDimension state_dimension ) 
	throws BelievabilityException {

	// This may throw a BelievabilityException
	return this.getStateDimensionEstimation(state_dimension).getUtility();
    }


    /**
     * Clone the shell of this StateEstimation, so that it can be used to
     * try repair options and compare the utilities in a consistent manner.
     * @param timestamp the timestamp for the new clone.
     * @return A clone of all of this StateEstimation, except for the 
     *         actual StateDimensionEstimations.
     **/
    public StateEstimation cloneSEShell( long timestamp ) 
	throws BelievabilityException {

	return new StateEstimation( getAssetID(), timestamp );
    }


    /**
     * Clone this StateEstimation, so that it can be used to
     * try repair options and compare the utilities in a consistent manner.
     * @throws BelievabilityException but shouldn't
     * @return A clone of all of this StateEstimation
     **/
    public StateEstimation cloneSE() throws BelievabilityException {
	StateEstimation se =
	    new StateEstimation( getAssetID(), getTimestamp() );
	Enumeration sd_enum = this.keys();
	while ( sd_enum.hasMoreElements() ) {
	    AssetStateDimension asd = 
		(AssetStateDimension) sd_enum.nextElement();
	    se.setStateDimensionEstimation( asd,
					    this.getStateDimensionEstimation( asd ) );
	}
	return se;
    }


    /**
     * Accessing whether or not there was an error encountered while
     * trying to create this state estimation object.
     *
     * @return true if an error was logged, and false if everything
     * went ok.
     */
    public boolean hasError() { return this._error; }


    /*
     * Returns the accumulated error messages when an error exists.
     *
     * @return The list of error messages, or an empty String if there
     * were no errors.
     */
    public String getErrorMessage() { return _error_msg_buff.toString(); }


    /**
     * Sets the error condition of the object and adds a message.
     *
     * @param err_msg The message to append to the fulle S.E. error
     * message.  
     */
    public void logError( String err_msg )
    {
        this._error = true;
        _error_msg_buff.append( err_msg );
    }


    //************************************************************
    // Asset that this state estimation concerns
    private AssetID _asset_id = null;

    // Timestamp for the state estimation
    private long _timestamp = System.currentTimeMillis();
 
    // The diagnosis that caused the state estimation to be made
    // private BelievabilityDiagnosis _diagnosis = null;

    // Useful to track errors so we can pass them along.
    private boolean _error = false;
    private StringBuffer _error_msg_buff = new StringBuffer();

}
