/**
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: TriggerConsumer.java,v $
 *</NAME>
 *
 * <copyright>
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
 * </copyright>
 **/

package org.cougaar.coordinator.believability;

import java.util.Enumeration;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * Used to accept new diagnoses or action successes from the blackboard
 * and take the appropriate action.
 *
 * @author Tony Cassandra, Misty Nodine
 */
public class TriggerConsumer extends Object
        implements TriggerConsumerInterface 
{

    // For logging
    protected Logger _logger = Logging.getLogger(this.getClass().getName());

    //************************************************************
    /**
     * Main constructor
     *
     * @param bel_plugin The believability plugin
     * @param model_manager The model manager that has all the models
     *                      we need
     * @param se_publisher The publisher that will publish any resulting
     *                     StateEstimations
     **/
    public TriggerConsumer( BelievabilityPlugin bel_plugin,
                            ModelManagerInterface model_manager,
                            StateEstimationPublisher se_publisher ) 
    {
        // Copy off the parameters
        _plugin = bel_plugin;
        _model_manager = model_manager;
        _se_publisher = se_publisher;
        
        // Set up the asset containter
        _asset_container = new AssetContainer();

    }

    //************************************************************
    /**
     * Process a new update trigger (diagnosis or successful action)
     * @param but The belief update trigger to prcess
     * @throws BelievabilityException if there is a problem processing
     **/
    public void consumeUpdateTrigger( BeliefUpdateTrigger but ) 
            throws BelievabilityException 
    {
        
        // Forward the information
        this.sendToAssetModel( but );

    } // end consumeUpdateTrigger
    
    //************************************************************
    /**
     * Provide an access to the AssetContainer
     * @return the Asset Container
     **/
    public AssetContainer getAssetContainer() {
        return _asset_container;
    }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------
    
    /**
     * Pass the diagnosis or successful action to the asset model
     * @param but A BeliefUpdateTrigger (Believability diagnosis or action)
     **/
    private void sendToAssetModel( BeliefUpdateTrigger but ) 
    {

        try 
        {
            // Find the AssetModel and AssetStateWindow that this trigger
            // concerns.
            AssetModel am = _asset_container.getAssetModel( but.getAssetID() );
            if ( am == null ) {
                am = new AssetModel( but.getAssetID(),
                                     _plugin.getIntervalAlarmHandler(),
                                     _model_manager, 
                                     _se_publisher,
                                     but.getTriggerTimestamp() );
                _asset_container.addAssetModel( am );
            }
     
            // Update the asset state window with the new trigger
            am.consumeUpdateTrigger( but );
        }
        catch( BelievabilityException be ) 
        {
            if ( _logger.isWarnEnabled())
                _logger.warn( "Failed handling trigger: " +
                            be.getMessage() );
         
        }
    }

    //************************************************************
    /**
     * Forces the trigger history in each asset model to update the
     * belief set based on their current set of triggers.  This may
     * also lead to those updated belief states being published.
     *
     */
    void forceAllBeliefUpdates( )
            throws BelievabilityException
    {
        // Method implementation comments go here ...
        Enumeration asset_model_enum = _asset_container.elements();
        while (asset_model_enum.hasMoreElements() )
        {
            AssetModel asset_model 
                    = (AssetModel) asset_model_enum.nextElement();

            asset_model.forceBeliefUpdate();

        } // while asset_model_enum

    } // method forceUpdates

    // A handle to the believability plugin
    private BelievabilityPlugin _plugin;

    // The interface to the model manager with all of the modeling
    // information
    private ModelManagerInterface _model_manager = null;

    // The interface to the publisher of state estimations
    private StateEstimationPublisher _se_publisher = null;

    // The asset container for all of the asset models.
    private AssetContainer _asset_container = null;

} // class TriggerConsumer



