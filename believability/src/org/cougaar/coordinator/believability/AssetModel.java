/**
 * AssetModel.java
 *
 * Created on April 24, 2004
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
 **/

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetID;

import java.util.Enumeration;
import java.util.Vector;


/**
 * The class representing assets, holding the basic information and modeling
 * shortcut information for the asset.
 * There is one AssetModel object for each asset (or each 
 *  AssetTechSpecInterface).
 *
 * @author Misty Nodine
 */
public class AssetModel extends Object
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Each asset will have its own AssetModel, and it makes
     * no sense to have a AssetModel without an asset, so we
     * require one in the constructor.
     *
     * @param asset_ts The handle to the asset's TechSpec
     * @param beleivability_plugin A back pointer to the believability plugin
     * @param pomdp_model The POMDPModelInterface for the believability model
     **/
    public AssetModel( AssetTechSpecInterface asset_ts,
		       BelievabilityPlugin believability_plugin,
		       POMDPModelInterface pomdp_model ) {

	_plugin = believability_plugin;
        setAssetTS( asset_ts );
	try {
	    _asset_state_window = new AssetStateWindow( this );
	    _belief_update = new BeliefUpdate( this, pomdp_model );
	}
	catch( Exception e ) {
	    System.out.println( "***AssetModel Constructor: Need to code what to do with exceptions." );
	}
    } // constructor AssetModel


    /**
     * Return the asset's TechSpec handle.
     * @return the asset TechSpec handle
     **/
    public AssetTechSpecInterface getAssetTS() {
        return _asset_ts;
    } // method getAssetTS


    /**
     * Modify the asset's TechSpec handle and update internal information
     * @param asset_ts the new asset TechSpec handle
     **/
    public void setAssetTS( AssetTechSpecInterface asset_ts ) {

	if (asset_ts != null) {
	    _asset_ts = asset_ts;
	    _asset_type = asset_ts.getAssetType();
	    _asset_id = _asset_ts.getAssetID();
	    _asset_state_dimensions = _asset_ts.getAssetStates();
	    _asset_utility_weights = 
		new UtilityWeights( asset_ts, _plugin.getMAUWeights() );
	}
	else {
	    _asset_ts = null;
            _asset_type = null;
	    _asset_id = null;
	    _asset_state_dimensions = null;
	    _asset_utility_weights = null;
	}
    } // method setAssetTS


    /**
     * Return the asset's type
     *
     * @return the asset type
     **/
    public AssetType getAssetType() {
	return _asset_type;
    } // method getAssetTS


    /**
     * Return the name of the asset's type
     *
     * @return the name of the asset type
     **/
    public String getAssetTypeName() {
        if ( _asset_type == null ) return null;
	else return _asset_type.getName();
    } // method getAssetTS


    /**
     * Return the asset's identifier
     *
     * @return the asset identifier
     **/
    public AssetID getAssetID() {
        return _asset_id;
    } // end getAssetID


    /**
     * Return the asset's list of state dimensions
     *
     * @return a Vector containing the AssetStateDimensions
     **/
    public Vector getAssetStateDimensions() {
	return _asset_state_dimensions;
    } // method getAssetStateDimensions


    /**
     * Return the asset's utility weight information
     *
     * @return the UtilityWeights object for the asset
     **/
    public UtilityWeights getUtilityWeights() {
	return _asset_utility_weights;
    } // method getAssetStateDimensions


    /**
     * Get the asset state window for the asset
     * @return the asset state window
     **/
    public DiagnosisConsumerInterface getAssetStateWindow() {
	return _asset_state_window;
    }
    /**
     * Get the estimation of the current state of the asset
     * @return The current StateEstimation
     **/
    public StateEstimation getCurrentState() {
	//**	return getAssetStateWindow().getCurrentState();
	System.out.println("****AssetModel.getCurrentState: NOT IMPLEMENTED");
	return null;
    }

    /**
     * Determine whether or not the asset's state estimation should be
     * forwarded to the blackboard. If so, forward it. Manage the timer
     * for putting StateEstimations on the blackboard.
     * Returns true if the utility along some state dimension
     * has fallen below a constant threshhold value, or if the timer
     * has expired indicating that there has been an interval of 
     * _max_diagnosis_latency from the arrival of the first diagnosis.
     * @param asset_model The asset model to check.
     **/
    public boolean forwardStateP ( ) {

	System.out.println ("*****forwardStateP: not completely implemented yet ***" );
	
	// Check to see if the state has fallen below some threshhold for
	// some state dimension. If so, return true.
	if ( false ) {
	    _plugin.publishAdd( this.getCurrentState() );

	    // Cancel the alarm
	    if ( _asset_alarm != null ) _asset_alarm.cancel();

	    return true;
	}

	// Check if the timer is running. If not, set the timer for the asset
	if ( _asset_alarm == null ) {
	    _asset_alarm = new AssetAlarm( this, _max_diagnosis_latency );

	    // Start the alarm
	    _plugin.setAlarm( _asset_alarm );
	}

	// Timeouts are handled separately by the timer callback, so just 
	// need to return here.
	return false; 
    }


    /**
     * This method is called when the asset timer expires, is canceled, 
     * or the AssetModel decides that the state should be forwarded
     * immediately.
     * @param expiredP True if the alarm expired validly, false if it was
     *                 canceled.
     **/
    public void timerCallback ( boolean expiredP ) {
	// Publish the state estimation if the timer expired validly.
	_plugin.publishAdd( this.getCurrentState() );

	// Clear the timer information from the local variables
	_asset_alarm = null;
    }


    /**
     * Return a string representation of the asset model
     *
     * @return a string representation of the asset model
     **/
    public String toString() {
        if ( _asset_ts == null ) return null;

	String retstring = "< asset ID:" + getAssetID().toString()
	    + " state_descriptors:[";
	
	
	Enumeration sds = getAssetStateDimensions().elements();
	while ( sds.hasMoreElements() ) {
	    AssetStateDimension sd = (AssetStateDimension) sds.nextElement();
	    retstring += sd.getStateName();
	    retstring += " ";
	}
	retstring += "] >";
        return retstring;
    } // method toString


    /*********************************************************************
     *
     * Return the BeliefUpdate object associated with this asset
     *
     * @return the BeliefUpdate object
     *********************************************************************/
    public BeliefUpdate getBeliefUpdate() {
        return _belief_update;
    } // method getBeliefUpdate


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Asset TechSpec for this asset
    private AssetTechSpecInterface _asset_ts;

    // Type of this asset
    private AssetType _asset_type;

    // ID of this asset;
    private AssetID _asset_id;

    // Vector of AssetStateDimenson objects, one for each 
    // asset state dimension
    private Vector _asset_state_dimensions;

    // Hashtable of asset state dimension utility weights, organized by
    // state dimension name
    private UtilityWeights _asset_utility_weights = null;

    // Asset state window for the asset
    private DiagnosisConsumerInterface _asset_state_window = null;

    // BeliefUpdate object for maintaining our belief states about this 
    // asset
    private BeliefUpdate _belief_update;

    // Length of diagnosis window, in milliseconds
    private long _max_diagnosis_latency = 0;
    
    // Alarm for timing out diagnosis window, if any is set.
    private AssetAlarm _asset_alarm = null;

    // Back pointer to the believability plugin 
    private BelievabilityPlugin _plugin = null;

} // class AssetModel
