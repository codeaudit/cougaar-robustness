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
import org.cougaar.coordinator.Diagnosis;

import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import java.util.Collection;
import java.util.Enumeration;
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
public class BelievabilityPlugin 
        extends ServiceUserPluginBase
        implements NotPersistable 
{
    

    /** 
     * Creates a new instance of BelievabilityPlugin 
     **/
    public BelievabilityPlugin() {

        super(requiredServices);

     // Initialize various classes
     _model_manager = new ModelManager();
     _se_publisher = new StateEstimationPublisher( this, _model_manager );
     _diagnosis_consumer = new DiagnosisConsumer( _model_manager,
                                   _se_publisher );
     
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
      * Called from outside once after initialization, as a
      * "pre-execute()". This method sets up the subscriptions to
      * objects that we are interested in -- models and diagnoses.
      * It also tries to read all of the sensor and asset tech spec
      * information that should have loaded during the load() phase.
      **/
    protected void setupSubscriptions() {

     // First read in sensor type and asset type information. The
     // asset types are found from the sensor types.
     try {
         readInSensorTypeInformation();
     }
     catch ( BelievabilityException be ) {
         if ( logger.isDebugEnabled() )
          logger.debug( " Believability plugin did not find sensor techspecs -- " + be.getMessage() );
     }

     // Now subscribe to the threat models and the diagnoses wrappers.
     _threatModelSub 
	 = ( IncrementalSubscription ) 
	 getBlackboardService().subscribe
	 ( new UnaryPredicate() 
	     {
		 public boolean execute(Object o) {
		     if ( o instanceof ThreatModelInterface ) {
			 return true ;
		     }
		     if ( o instanceof ThreatDescription ) {
			 return true ;
		     }
		     if ( o instanceof EventDescription ) {
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
		     if ( o instanceof Diagnosis ) {
			 return false ;
		     }
		     return false ;
		 }
	     }) ;
     
    } // method setupSubscriptions


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

        handleThreatModel();
	handleDiagnosis();

    } // method execute


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

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
	    Object o = iter.next();
	    if ( o instanceof ThreatModelInterface )
		_model_manager.addThreatType( (ThreatModelInterface) o );
	    if ( o instanceof ThreatDescription )
		_model_manager.addThreatDescription( (ThreatDescription) o );
	    if ( o instanceof EventDescription )
		_model_manager.addEventDescription( (EventDescription) o );
        } // for ADD ThreatModelInterface


        //------- CHANGE ThreatModelInterface
        for ( Iterator iter = _threatModelSub.getChangedCollection().iterator();  
           iter.hasNext() ; ) 
        {
	    Object o = iter.next();
	    if ( o instanceof ThreatModelInterface )
		_model_manager.updateThreatType( (ThreatModelInterface) o );
	    if ( o instanceof ThreatDescription )
		_model_manager.updateThreatDescription( (ThreatDescription) o );
	    if ( o instanceof EventDescription )
		_model_manager.updateEventDescription( (EventDescription) o );
        } // for CHANGE ThreatModelInterface
        
        //------- REMOVE ThreatModelInterface
        for ( Iterator iter = _threatModelSub.getRemovedCollection().iterator();  
           iter.hasNext() ; ) 
        {
	    Object o = iter.next();
	    if ( o instanceof ThreatModelInterface )
		_model_manager.removeThreatType( (ThreatModelInterface) o );
	    if ( o instanceof ThreatDescription )
		_model_manager.removeThreatDescription( (ThreatDescription) o );
	    if ( o instanceof EventDescription )
		_model_manager.removeEventDescription( (EventDescription) o );
        } // for REMOVE ThreatModelInterface
    } // method handleThreatModel


    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for DiagnosesWrapper
     **/
    private void handleDiagnosis()
    {
	Diagnosis diag = null;

        // Diagnoses are published to the blackboard. Each sensor has
	// a Diagnosis object on the blackboard that it either asserts
	// periodically or asserts when some value changes (or both).
	// The Believability plugin receives a wrapped version of this
	// object on the blackboard, when something changes.
	
	// ------- ADD DiagnosesWrapper or Diagnosis
	// Update the belief state for each asset
	for ( Iterator iter = _diagnosisSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) {
	    if (logger.isDebugEnabled() ) logger.debug ("Diagnosis ADD");

	    try {
		Object o = iter.next();
		if ( o instanceof DiagnosesWrapper ) 
		    diag = ((DiagnosesWrapper)o).getDiagnosis();
		else diag = (Diagnosis) o;
	     
		// Check to see whether the defense controller is enabled at
		// the moment
		if ( _dc_enabled ) _diagnosis_consumer.consumeDiagnosis( diag );
	    }
	    catch ( BelievabilityException be ) {
		logger.warn( "Problem processing added diagnosis "
			     + be.getMessage() );
	    }
	} // iterator for ADD Diagnosis

	// ------- CHANGE DiagnosesWrapper or Diagnosis
	// This indicates some new diagnosis information has been asserted.
	for ( Iterator iter = _diagnosisSub.getChangedCollection().iterator();
	      iter.hasNext() ; ) {

	    if (logger.isDebugEnabled() ) logger.debug ("Diagnosis CHANGE");

	    try {
		Object o = iter.next();
		if ( o instanceof DiagnosesWrapper ) 
		    diag = ((DiagnosesWrapper)o).getDiagnosis();
		else diag = (Diagnosis) o;
	     
		// Check to see whether the defense controller is enabled at
		// the moment
		if ( _dc_enabled ) _diagnosis_consumer.consumeDiagnosis( diag );
	    }
	    catch ( BelievabilityException be ) {
		logger.warn( "Problem processing updated diagnosis "
			     + be.getMessage() );
	    }
	} // iterator for CHANGE Diagnosis
        
	// ----- REMOVE DiagnosesWrapper or Diagnosis
	// The DiagnosesWrapper has been removed successfully,
	// we do nothing with this since it is controlled elsewhere, and
	// we have already processed it.
	for ( Iterator iter = _diagnosisSub.getRemovedCollection().iterator(); 
	      iter.hasNext() ; ) {
	    Object o = iter.next(); 
	    // do absolutely nothing
	} // for REMOVE DiagnosesWrapper
	
    } // method handleDiagnosis


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

        if ( (eventService != null) && (diagnosisTSService == null) )
	    return true;

        if (acquireServices()) 
        {
            if (logger.isDebugEnabled()) 
                logger.debug(".haveServices - acquiredServices.");

            ServiceBroker sb = getServiceBroker();
	    if ( sb == null ) 
		throw new RuntimeException ( "No service broker found." );
                
	    // Get the event service
            eventService 
                = (EventService ) sb.getService( this, 
                                                 EventService.class, 
                                                 null ) ;      
            if (eventService == null) 
                throw new RuntimeException("Unable to obtain EventService");

	    // Get the diagnosis tech spec service
	    diagnosisTSService = (DiagnosisTechSpecService)
		sb.getService( this,
			       DiagnosisTechSpecService.class, 
			       null );
	
	    if ( diagnosisTSService == null )
		throw new RuntimeException( "Unable to obtain DiagnosisTechSpecService." );

	    return true;
        }

        else if (logger.isDebugEnabled()) 
            logger.debug(".haveServices - did NOT acquire services.");

        return false;

    } // method haveServices


    // These are the subscriptions we need to monitor the BB activity.

    private IncrementalSubscription _threatModelSub;
    private IncrementalSubscription _diagnosisSub;

    private IncrementalSubscription pluginControlSubscription;
    
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };
    private DiagnosisTechSpecService diagnosisTSService = null;



    /**
     * Set whether or not the defense controller is enabled.
     * This is used to prevent thrashing during startup.
     **/
    private void setDCEnabled( boolean dc_enabled ) {
     _dc_enabled = dc_enabled;
    }


    // Method to read in the sensor types. Used during setup.
    // Throws BelievabilityException if it has a problem finding the techspecs
    private void readInSensorTypeInformation() throws BelievabilityException {
	logger.debug("About to enter sensor tech spec read loop " );

	// Get all of the sensor tech specs from the service.
	Iterator ts_enum = 
	    diagnosisTSService.getAllDiagnosisTechSpecs().iterator();
	while ( ts_enum.hasNext() ) {
	    DiagnosisTechSpecInterface dtsi = 
		(DiagnosisTechSpecInterface) ts_enum.next();

	    logger.debug("plugin adding a sensor tech spec " );
	    _model_manager.addSensorType( dtsi );
	}
    }


    // Boolean saying whether this functionality is enabled or not.
    private boolean _dc_enabled = true;

    // This is the model manager that has all of the information
    // from the techspecs.
    private ModelManagerInterface _model_manager = null;

    // This is the state estimation publisher that will be making decisions
    // about publishing state estimations.
    private StateEstimationPublisher _se_publisher = null;

    // This is the asset index for the AssetModels. It is indexed by AssetID
    private DiagnosisConsumer _diagnosis_consumer = null;

}  // class BelievabilityPlugin
