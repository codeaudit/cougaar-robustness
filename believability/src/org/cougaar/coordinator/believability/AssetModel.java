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
public class AssetModel extends Loggable
        implements TriggerConsumerInterface 
{

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
     * @param bel_plugin A handle to the believability plugin
     * @param initial_time Timestamp to use to mark the start of the
     * asset's existence.
     **/
    public AssetModel( AssetID asset_id,
                       BelievabilityPlugin bel_plugin,
                       ModelManagerInterface model_manager,
                       StateEstimationPublisher se_publisher,
                       long initial_time ) 
    {
        
     // Save construction parameters and information
     _asset_id = asset_id;
     _model_manager = model_manager;
     _se_publisher = se_publisher;

     _trigger_history 
             = new BeliefTriggerHistory( asset_id,
                                         model_manager,
                                         se_publisher,
                                         bel_plugin.getAlarmServiceHandle());

    } // constructor AssetModel


    /**
     * Return the asset's identifier
     * @return the asset identifier
     **/
    public AssetID getAssetID() {
        return _asset_id;
    } // end getAssetID


    /**
     * Forward a BeliefUpdateTrigger to the belief state window
     * @param BeliefUpdateTrigger but
     * @throws BelievabilityException if there is a problem dealing
     *                                with the belief update trigger
     **/
    public void consumeUpdateTrigger( BeliefUpdateTrigger but )
            throws BelievabilityException 
    {

        if ( _trigger_history == null )
            throw new BelievabilityException
                    ( "AssetModel.consumeUpdateTrigger()",
                      "No trigger history found." );
        
        _trigger_history.handleBeliefTrigger( but );

    }


    /**
     * Return a string representation of the asset model
     *
     * @return a string representation of the asset model
     **/
    public String toString() 
    {
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

    // This does most of the work handling triggers and deciding when
    // to publish.
    //
    private BeliefTriggerHistory _trigger_history = null;

} // class AssetModel
