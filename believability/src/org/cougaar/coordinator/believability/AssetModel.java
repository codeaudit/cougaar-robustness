/*
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
 */

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
     * @param pomdp_model The POMDPModelInterface for the believability model
     * @param mau_weights The MAU weight object carrying the current weights
     **/
    public AssetModel( AssetTechSpecInterface asset_ts,
		       POMDPModelInterface pomdp_model,
		       MAUWeights mau_weights ) {

        setAssetTS( asset_ts, mau_weights );
	_belief_update = new BeliefUpdate( this, pomdp_model );
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
    public void setAssetTS( AssetTechSpecInterface asset_ts,
			    MAUWeights mau_weights ) {
	if (asset_ts != null) {
	    _asset_ts = asset_ts;
	    _asset_type = asset_ts.getAssetType();
	    _asset_id = _asset_ts.getAssetID();
	    _asset_state_dimensions = _asset_ts.getAssetStates();
	    _asset_utility_weights = 
		new UtilityWeights( asset_ts, mau_weights );
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

    // BeliefUpdate object for maintaining our belief states about this 
    // asset
    private BeliefUpdate _belief_update;

} // class AssetModel
