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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

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
public class AssetModel extends Object {

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Each asset will have its own AssetModel, and it makes
     * no sense to have a AssetModel without an asset, so we
     * require one in the constructor.
     *
     * @param asset_id The ID of the asset this is a model of
     * @param model_manager The manager of the different models used
     *                      in estimation
     * @param se_publisher The publisher that will create and publish
     *                     StateEstimations to the blackboard as needed.
     **/
    public AssetModel( AssetID asset_id,
		       ModelManagerInterface model_manager,
		       StateEstimationPublisher se_publisher ) {

	// Save construction parameters and information
	_asset_id = asset_id;
	_model_manager = model_manager;
	_se_publisher = se_publisher;

	// Create a logger to send things to.
	_logger = Logging.getLogger(this.getClass().getName());

	POMDPModelInterface pmif = model_manager.getPOMDPModel();
	
	try {
	    long window_length = 
		_model_manager.getMaxSensorLatency( _asset_id.getType() );
	    _belief_state_window = 
		new BeliefStateWindow( asset_id, 
				       window_length,
				       pmif,
				       se_publisher );
	}
	catch( BelievabilityException be ) {
	    if ( _logger.isDebugEnabled() ) _logger.debug( be.getMessage() );
	}
    } // constructor AssetModel


    /**
     * Return the asset's identifier
     * @return the asset identifier
     **/
    public AssetID getAssetID() {
        return _asset_id;
    } // end getAssetID


    /**
     * Get the asset state window for the asset
     * @return the asset state window
     **/
    public BeliefStateWindow getBeliefStateWindow() {
	return _belief_state_window;
    }


    /**
     * Forward a BelievabilityDiagnosis to the belief state window
     * @param BelievabilityDiagnosis diagnosis
     * @throws BelievabilityException if there is a problem dealing
     *                                with the diagnosis
     **/
    public void consumeBelievabilityDiagnosis( BelievabilityDiagnosis diagnosis )
	throws BelievabilityException {

	// May throw BelievabilityException
	getBeliefStateWindow().consumeBelievabilityDiagnosis( diagnosis );
    }


    /**
     * Return a string representation of the asset model
     *
     * @return a string representation of the asset model
     **/
    public String toString() {
	return ( "< Asset model for asset ID:"
		 + getAssetID().toString()
		 + " >" );
    } // method toString


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // ID of this asset;
    private AssetID _asset_id = null;

    // Model manager that you may need to interact with
    private ModelManagerInterface _model_manager = null;

    // Place that is publishing the StateEstimations
    private StateEstimationPublisher _se_publisher = null;

    // Asset state window for the asset
    private BeliefStateWindow _belief_state_window = null;

    // Length of diagnosis window, in milliseconds
    private long _max_diagnosis_latency = 0;

    // Logger interface
    private Logger _logger;
} // class AssetModel
