/**
 * BelievabilityPlugin.java
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

import org.cougaar.coordinator.DiagnosesWrapper;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.adaptivity.ServiceUserPluginBase;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.EventService;
import org.cougaar.util.UnaryPredicate;


/**
 * This Plugin is used to handle the believability computation for
 * the defense coordinator. It uses the models and tech specs to set
 * up a model of the agent system. It reads DiagnosesWrappers, updates
 * diagnosis state, and emits StateEstimation objects when it notices
 * significant problems.
 *
 */
public class BelievabilityPlugin extends ServiceUserPluginBase 
                                 implements NotPersistable {
    

    /** 
     * Creates a new instance of BelievabilityPlugin 
     **/
    public BelievabilityPlugin() {

        super(requiredServices);

    } // constructor BelievabilityPlugin
    

    /**
     * Called from outside. Should contain plugin initialization code.
     **/
    public void load() {
        super.load();
        getPluginParams();
        haveServices();
    } // method load
    

    // Helper methods to publish objects to the Blackboard
    /**
     * Publish a new object to the Blackboard
     * @param o The object to be published
     * @return true always
     **/
    public boolean publishAdd( Object o ) {
        getBlackboardService().publishAdd( o );
        return true;
    } // method publishAdd


    /**
     * Publish a change to an object to the Blackboard. Must have the 
     * same OID
     * @param o The object to be changed
     * @return true always
     **/
    public boolean publishChange( Object o ) {
	getBlackboardService().publishChange( o );
        return true;
    } // method publishChange


    /**
     * Remove an object from the Blackboard.
     * @param o The object to be removed
     * @return true always
     **/
    public boolean publishRemove( Object o ) {
	getBlackboardService().publishRemove( o );
        return true;
    } // method publishRemove
    

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    /** 
     *  Execute method.
     *  Called every time this component is scheduled to run. Any time
     *  objects that belong to its subscription sets change
     *  (add/modify/remove), this will be called. This method is
     *  executed within the context of a blackboard transaction (so do
     *  NOT use transaction syntax inside it).  You may only need to
     *  monitor one or two type of actions (e.g. additions), in which
     *  case you can safely remove the sections of code dealing with
     *  collection changes you are not interested in.
     *
     *  This method checks everything that may have happened, cleaning
     *  out every new notification
     **/
    protected void execute() {

        if (logger.isDebugEnabled()) 
            logger.debug("Believability Plugin in Execute Loop");

        handleAssetTS();
        handleSensorTS();
        handleThreatModel();
        handleDiagnosisWrapper();

    } // method execute


    /** 
      * Called from outside once after initialization, as a
      * "pre-execute()". This method sets up the subscriptions to
      * objects that we are interested in -- tech specs and models
      * and diagnoses
      **/
    protected void setupSubscriptions() {

        _assetSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof AssetTechSpecInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _sensorSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof DiagnosisTechSpecInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _threatModelSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof ThreatModelInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _diagnosisSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof DiagnosesWrapper ) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
    } // method setupSubscriptions


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * AssetTechSpecInterface objects.  
     *
     */
    private void handleAssetTS()
    {
        AssetTechSpecInterface asset_ts = null;

        // ADD AssetTechSpecInterface
        for ( Iterator iter = _assetSub.getAddedCollection().iterator();
              iter.hasNext() ; ) 
        {
            // Add the asset to the intermediate model
            asset_ts = (AssetTechSpecInterface) iter.next(); 
//***            _intermediate_model.addAsset( asset_ts );

        } // for ADD AssetTechSpecInterface

        //--------------------
        //      CHANGE AssetTechSpecInterface
        //--------------------

        for ( Iterator iter = _assetSub.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            asset_ts = (AssetTechSpecInterface) iter.next(); 
//***         _intermediate_model.changeAsset( asset_ts );
            
        } // for CHANGE AssetTechSpecInterface
        
        //--------------------
        //      REMOVE AssetTechSpecInterface
        //--------------------

        for ( Iterator iter = _assetSub.getRemovedCollection
                  ().iterator();  
          iter.hasNext() ; ) 
        {
            asset_ts = (AssetTechSpecInterface) iter.next(); 
//***            _intermediate_model.removeAsset( asset_ts );

        } // for REMOVE AssetTechSpecInterface

    } // method handleAsset


    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * DiagnosisTechSpecInterface objects.  A diagnosis is related to one or
     * more threat types that may affect the same asset. 
     *
     */
    private void handleSensorTS()
    {
        DiagnosisTechSpecInterface sensor_ts = null;

        //------- ADD DiagnosisTechSpecInterface
        for ( Iterator iter = _diagnosisSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) 
        {
            sensor_ts = (DiagnosisTechSpecInterface) iter.next(); 
	    _intermediate_model.consumeSensorType( sensor_ts );
        } // for ADD DiagnosisTechSpecInterface


        //------- CHANGE DiagnosisTechSpecInterface
        for ( Iterator iter = _diagnosisSub.getChangedCollection().iterator(); 
              iter.hasNext() ; ) 
        {
            sensor_ts = (DiagnosisTechSpecInterface) iter.next(); 
	    _intermediate_model.consumeSensorType( sensor_ts );
        } // for CHANGE DiagnosisTechSpecInterface
        

        //------- REMOVE DiagnosisTechSpecInterface
        for ( Iterator iter = _diagnosisSub.getRemovedCollection().iterator();
              iter.hasNext() ; ) 
        {
            sensor_ts = (DiagnosisTechSpecInterface) iter.next(); 
	    _intermediate_model.removeSensorType( sensor_ts );
        } // for REMOVE DiagnosisTechSpecInterface

    } // method handleDiagnosis


    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * ThreatModelInterface objects.  A threat model models a specific
     * type of threat.
     *
     */
    private void handleThreatModel()
    {
        ThreatModelInterface threat_ts = null;

        //------- ADD ThreatModelInterface
        for ( Iterator iter = _threatModelSub.getAddedCollection().iterator();
              iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
	    _intermediate_model.consumeThreatModel( threat_ts );
        } // for ADD ThreatModelInterface


        //------- CHANGE ThreatModelInterface
        for ( Iterator iter = _threatModelSub.getChangedCollection().iterator();  
	      iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
	    _intermediate_model.consumeThreatModel( threat_ts );
        } // for CHANGE ThreatModelInterface
        
        //------- REMOVE ThreatModelInterface
        for ( Iterator iter = _threatModelSub.getRemovedCollection().iterator();  
	      iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
	    _intermediate_model.removeThreatModel( threat_ts );
        } // for REMOVE ThreatModelInterface
    } // method handleThreatModel


    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for DiagnosesWrapper
     **/
    private void handleDiagnosisWrapper()
    {
        DiagnosesWrapper dw = null;

        // Diagnoses are published to the blackboard. Each sensor has
	// a Diagnosis object on the blackboard that it either asserts
	// periodically or asserts when some value changes (or both).
	// The Believability plugin receives a wrapped version of this
	// object on the blackboard, when something changes.

	// ------- ADD DiagnosesWrapper
	// Update the belief state for each asset
	for ( Iterator iter = _diagnosisSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) 
        {
	    if (logger.isDebugEnabled() ) logger.debug ("Diagnosis ADD");

	    try {
		dw = (DiagnosesWrapper) iter.next(); 
		processAssertedDiagnosis( dw );
	    }
	    catch ( BelievabilityException be ) {
		logger.warn( "Problem processing added diagnosis "
			     + be.getMessage() );
	    }
        } // iterator for ADD DiagnosesWrapper

        // ------- CHANGE DiagnosesWrapper
	// DiagnosesWrapper objects have changed.
	// This indicates some new diagnosis information has been asserted.
        for ( Iterator iter = _diagnosisSub.getChangedCollection().iterator();
	      iter.hasNext() ; ) 
        {
	    if (logger.isDebugEnabled() ) logger.debug ("Diagnosis CHANGE");

	    try {
		dw = (DiagnosesWrapper) iter.next(); 
		processAssertedDiagnosis( dw );
	    }
	    catch ( BelievabilityException be ) {
		logger.warn( "Problem processing updated diagnosis "
			     + be.getMessage() );
	    }
	} // iterator for CHANGE DiagnosesWrapper
        
        // ----- REMOVE DiagnosesWrapper
	// The DiagnosesWrapper has been removed successfully,
	// we do nothing with this since it is controlled elsewhere, and
	// we have already processed it.
        for ( Iterator iter = _diagnosisSub.getRemovedCollection().iterator(); 
	      iter.hasNext() ; ) 
        {
            dw = (DiagnosesWrapper) iter.next(); 
	    // do absolutely nothing
        } // for REMOVE DiagnosesWrapper

    } // method handleDiagnosisWrapper


    /**
     * Accessor method for the MAU weight information
     * @return The current MAU weights
     **/
    public MAUWeights getMAUWeights() { return _mau_weights; }

    
    /**
     * Set an alarm
     * @param The Alarm object for the alarm
     **/
    public void setAlarm( Alarm alarm_to_set ) {
	getAlarmService().addRealTimeAlarm( alarm_to_set );
    }
    

    //-------------------------------------------------------------------
    // Private Methods
    //-------------------------------------------------------------------

    /**
      * Demonstrates how to read in parameters passed in via
      * configuration files. Use/remove as needed.
      **/
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for
        //debugging
        if (logger.isInfoEnabled() && getParameters().isEmpty()) 
            logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        while (iter.hasNext()) {
             logger.debug("Parameter = " + (String)iter.next());
        }
    } // method getPluginParams


    /**
     * Called to ensure that the services we need get loaded.
     **/
    private boolean haveServices() {

        if (eventService != null) return true;

        if (acquireServices()) 
        {
            if (logger.isDebugEnabled()) 
                logger.debug(".haveServices - acquiredServices.");

            ServiceBroker sb = getServiceBroker();
                
            eventService 
                = (EventService ) sb.getService( this, 
                                                 EventService.class, 
                                                 null ) ;      

            if (eventService == null) 
            {
                throw new RuntimeException("Unable to obtain EventService");
            }

            return true;
        }
        else if (logger.isDebugEnabled()) 
            logger.debug(".haveServices - did NOT acquire services.");

        return false;

    } // method haveServices


    // These are the subscriptions we need to monitor the BB activity.

    private IncrementalSubscription _threatModelSub;
    private IncrementalSubscription _assetSub;
    private IncrementalSubscription _sensorSub;
    private IncrementalSubscription _diagnosisSub;

    private IncrementalSubscription pluginControlSubscription;
    
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };



    /**
     * Process a diagnosis that has been asserted as either an add or a change.
     * @param dw The diagnosis wrapper from the blackboard
     * @throws BelievabilityException if there is a problem processing
     **/
    private void processAssertedDiagnosis( DiagnosesWrapper dw ) 
	throws BelievabilityException {

	// First check to see whether the defense controller is enabled at
	// the moment
	if ( ! _dc_enabled ) return;

	// Copy out the diagnosis information
	BelievabilityDiagnosis bd = new BelievabilityDiagnosis( dw );
	
	if (logger.isDebugEnabled()) 
	    logger.debug("Updating BelievabilityDiagnosis " + bd.toString() );
	
	// Find the AssetModel and AssetStateWindow that this diagnosis
	// concerns.
	AssetModel am = _asset_container.getAssetModel( bd.getAssetID() );
	DiagnosisConsumerInterface dci = am.getAssetStateWindow();

	// Update the asset state window with the new diagnosis
	try {
	    dci.consumeDiagnosis( bd );
	}
	catch( Exception e ) {
	    System.out.println( "***Need to code what to do with exceptions in BelievabilityPlugin.processAssertedDiagnosis");
	}
	
	// Check to see whether or not the new state should be forwarded
	// to the blackboard. 
	if ( am.forwardStateP( ) ) publishAdd( am.getCurrentState() );
    }


    /**
     * Set whether or not the defense controller is enabled.
     * This is used to prevent thrashing during startup.
     **/
    private void setDCEnabled( boolean dc_enabled ) {
	_dc_enabled = dc_enabled;
    }


    // Boolean saying whether this functionality is enabled or not.
    private boolean _dc_enabled = true;

    // This is the intermediate model that has all of the information
    // from the techspecs.
    private TechSpecConsumerInterface _intermediate_model = null; //****new IntermediateModel();

    // This is the asset index for the AssetModels. It is indexed by AssetID
    private AssetContainer _asset_container = new AssetContainer();

    // This is the pointer to the MAU Weights object.
    private MAUWeights _mau_weights = new MAUWeights();

}  // class BelievabilityPlugin
