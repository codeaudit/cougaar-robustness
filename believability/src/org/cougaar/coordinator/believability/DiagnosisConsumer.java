/**
 * DiagnosisConsumer.java
 *
 * Created on May 6, 2004
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

import org.cougaar.coordinator.Diagnosis;

import java.util.Hashtable;

/**
 * Used to accept new diagnoses from the blackboard
 * and take the appropriate action.
 */
public class DiagnosisConsumer extends Loggable
    implements DiagnosisConsumerInterface {

    /**
     * Constructor
     * @param model_manager The model manager that has all the models
     *                      we need
     * @param se_publisher The publisher that will publish any resulting
     *                     StateEstimations
     **/
    public DiagnosisConsumer( ModelManagerInterface model_manager,
			      StateEstimationPublisher se_publisher ) {
	// Copy off the parameters
	_model_manager = model_manager;
	_se_publisher = se_publisher;

	// Set up the asset containter
	_asset_container = new AssetContainer();
    }


    /**
     * Process a new diagnosis
     * @param diag The diagnosis from the blackboard
     * @throws BelievabilityException if there is a problem processing
     **/
    public void consumeDiagnosis( Diagnosis diag ) 
	throws BelievabilityException {

	// Copy out the diagnosis information and forward it.
	BelievabilityDiagnosis bd = new BelievabilityDiagnosis( diag );
	this.sendToAssetModel( bd );
    } // end consumeDiagnosis


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    /**
     * Pass the diagnosis to the asset model
     * @param bd a BelievabilityDiagnosis
     **/
    private void sendToAssetModel( BelievabilityDiagnosis bd ) {
	if (_logger.isDebugEnabled()) 
	    _logger.debug("Updating BelievabilityDiagnosis " + bd.toString() );
     
	try {
	    // Find the AssetModel and AssetStateWindow that this diagnosis
	    // concerns.
	    AssetModel am = _asset_container.getAssetModel( bd.getAssetID() );
	    if ( am == null ) {
		am = new AssetModel( bd.getAssetID(),
				     _model_manager, 
				     _se_publisher );
		_asset_container.addAssetModel( am );
	    }
	
	    // Update the asset state window with the new diagnosis
	    am.consumeBelievabilityDiagnosis( bd );
	}
	catch( BelievabilityException be ) {
	    if (_logger.isDebugEnabled()) 
		_logger.debug("Failed to update diagnosis -- " +
			     be.getMessage() );
	    
	}
    }


    // The interface to the model manager with all of the modeling
    // information
    private ModelManagerInterface _model_manager;

    // The interface to the publisher of state estimations
    private StateEstimationPublisher _se_publisher;

    // The asset container for all of the asset models.
    private AssetContainer _asset_container;

} // class DiagnosisConsumer



