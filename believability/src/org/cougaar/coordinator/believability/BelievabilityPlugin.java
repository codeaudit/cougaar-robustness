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
import org.cougaar.coordinator.monitoring.SuccessfulAction;
import org.cougaar.coordinator.Action;

import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.ActionTechSpecService;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;
import org.cougaar.coordinator.techspec.DefaultThreatModel;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

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
     _diagnosis_consumer = new DiagnosisConsumer( this,
                                _model_manager,
                                _se_publisher );
     _se_publisher.setAssetContainer( _diagnosis_consumer.getAssetContainer() );
     
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
    

    /**
     * Method to queue an object for publication the next time execute() runs
     * @param obj The object to publish
     **/
    public void queueForPublication( Object obj ) {
        _publication_list.add( obj );
        getBlackboardService().signalClientActivity();
    }

    /**
     * Method to publish the items on the queue
     **/
    public synchronized void publishFromQueue() {
        Iterator pli = _publication_list.iterator();
        while ( pli.hasNext() ) {
            Object o = pli.next();
            if ( logger.isDebugEnabled() )
                 logger.debug( " publishFromQueue publishing object " + o.toString() );
            publishAdd(o);
         pli.remove();
        }
    }


    /**
     * Set an alarm
     * @param The Alarm object for the alarm
     **/
    public void setAlarm( Alarm alarm_to_set ) {
     getAlarmService().addRealTimeAlarm( alarm_to_set );
    }
    

    /**
     * Get the system start time
     * @return the start time from when believability is to be
     *         calculated, in milliseconds.
     **/
    public long getSystemStartTime() {
     return _system_start_time;
    }


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
         readInTechSpecInformation();
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
               if ( o instanceof DefaultThreatModel ) {
                return true ;
               }
               return false ;
           }
          }) ;
        
     _beliefUpdateTriggerSub 
      = ( IncrementalSubscription ) 
      getBlackboardService().subscribe
      ( new UnaryPredicate() 
          {
           public boolean execute(Object o) {
               if ( o instanceof DiagnosesWrapper ) {
                return true ;
               }
               if ( o instanceof SuccessfulAction ) {
                return true ;
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

        if (logger.isDetailEnabled()) 
            logger.detail("Believability Plugin in Execute Loop");

        publishFromQueue();
        handleThreatModel();
        handleUpdateTriggers();

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
    private void handleThreatModel() {
        ThreatModelInterface threat_ts = null;
     Iterator iter;
     Iterator i2;

        //------- ADD ThreatModelInterface
        for ( iter = _threatModelSub.getAddedCollection().iterator();
              iter.hasNext() ; ) {
         if ( logger.isDetailEnabled() ) 
          logger.detail( "ThreatModelInterface ADD" );
         threat_ts = (ThreatModelInterface) iter.next();
         _model_manager.addThreatType( threat_ts );
        } // for ADD ThreatModelInterface


        //------- CHANGE ThreatModelInterface
        for ( iter = _threatModelSub.getChangedCollection().iterator();  
           iter.hasNext() ; ) {
         if ( logger.isDetailEnabled() ) 
          logger.detail( "ThreatModelInterface CHANGE" );
         threat_ts = (ThreatModelInterface) iter.next();
         _model_manager.updateThreatType( threat_ts );

         for ( i2 = _threatModelSub.getChangeReports( threat_ts ).iterator(); 
            i2.hasNext() ; ) {
          ThreatModelChangeEvent tmce = 
              (ThreatModelChangeEvent) i2.next();
          _model_manager.handleThreatModelChange( tmce );
         }
        } // for CHANGE ThreatModelInterface
        
        //------- REMOVE ThreatModelInterface
        for ( iter = _threatModelSub.getRemovedCollection().iterator();  
           iter.hasNext() ; ) {
         if ( logger.isDetailEnabled() ) 
          logger.detail( "ThreatModelInterface DELETE" );
         threat_ts = (ThreatModelInterface) iter.next();
         _model_manager.removeThreatType( threat_ts );
        } // for REMOVE ThreatModelInterface
    } // method handleThreatModel


    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for DiagnosesWrappers
     * and SuccessfulActions
     **/
    private void handleUpdateTriggers()
    {
     BeliefUpdateTrigger but = null;

     Iterator iter;

        // Diagnoses are published to the blackboard. Each sensor has
     // a Diagnosis object on the blackboard that it either asserts
     // periodically or asserts when some value changes (or both).
     // The Believability plugin receives a wrapped version of this
     // object on the blackboard, when something changes.
     //
     // Successful actions are published to the blackboard by the 
     // actuators, at the time of success
     
     // ------- ADD UpdateTrigger
     for ( iter = _beliefUpdateTriggerSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) {
         if (logger.isDetailEnabled() ) logger.detail ("UpdateTrigger ADD");

         try {
          but = constructUpdateTrigger( (Object) iter.next() );

          // Check to see whether the defense controller is enabled at
          // the moment
          if (_dc_enabled) 
              _diagnosis_consumer.consumeUpdateTrigger( but );
         }
         catch ( BelievabilityException be ) {
             if (logger.isWarnEnabled() ) 
                 logger.warn( "Problem processing added diagnosis "
                              + be.getMessage() );
         }
     } // iterator for ADD update trigger

     // ------- CHANGE UpdateTrigger
     for ( iter = _beliefUpdateTriggerSub.getChangedCollection().iterator();
           iter.hasNext() ; ) {

         if (logger.isDetailEnabled() ) logger.detail ("UpdateTrigger CHANGE");
         try {
          but = constructUpdateTrigger( (Object) iter.next() );

          // Check to see whether the defense controller is enabled at
          // the moment
          if (_dc_enabled) 
              _diagnosis_consumer.consumeUpdateTrigger( but );

         }
         catch ( BelievabilityException be ) {
             if ( logger.isWarnEnabled()) 
          logger.warn( "Problem processing changed update trigger "
                    + be.getMessage() );
         }
     } // iterator for CHANGE UpdateTrigger
        
     // ----- REMOVE UpdateTrigger
     // The trigger has been removed successfully,
     // we do nothing with this since it is controlled elsewhere, and
     // we have already processed it.
     for ( iter = _beliefUpdateTriggerSub.getRemovedCollection().iterator(); 
           iter.hasNext() ; ) {
         Object o = iter.next();
         // do absolutely nothing
     } // for REMOVE UpdateTrigger
     
    } // method handleUpdateTriggers


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
            logger.info("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        while (iter.hasNext()) {
              if (logger.isDetailEnabled() ) 
           logger.detail("Parameter = " + (String)iter.next());
        }
    } // method getPluginParams


    /**
     * Called to ensure that the services we need get loaded.
     **/
    private boolean haveServices() {

        if ( (eventService != null) 
          && (diagnosisTSService != null)
          && (actionTSService != null) )
         return true;

        if (acquireServices()) {
            if (logger.isDetailEnabled()) 
                logger.detail(".haveServices - acquiredServices.");

            ServiceBroker sb = getServiceBroker();
         if ( sb == null ) 
          throw new RuntimeException ( "No service broker found." );
                
         // Get the event service
            eventService = (EventService)
          sb.getService( this, EventService.class, null ) ;      
            if (eventService == null) 
                throw new RuntimeException("Unable to obtain EventService");

         // Get the diagnosis tech spec service
         diagnosisTSService = (DiagnosisTechSpecService)
          sb.getService( this, DiagnosisTechSpecService.class, null );
     
         if ( diagnosisTSService == null )
          throw new RuntimeException( "Unable to obtain DiagnosisTechSpecService." );

         // Get the action tech spec service
         actionTSService = (ActionTechSpecService)
          sb.getService( this, ActionTechSpecService.class, null );
     
         if ( actionTSService == null )
          throw new RuntimeException( "Unable to obtain ActionTechSpecService." );

         return true;
        }

        else if (logger.isDebugEnabled()) 
            logger.debug(".haveServices - did NOT acquire services.");

        return false;

    } // method haveServices


    // These are the subscriptions we need to monitor the BB activity.

    private IncrementalSubscription _threatModelSub;
    private IncrementalSubscription _beliefUpdateTriggerSub;

    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };
    private DiagnosisTechSpecService diagnosisTSService = null;
    private ActionTechSpecService actionTSService = null;


    /**
     * Set whether or not the defense controller is enabled.
     * This is used to prevent thrashing during startup.
     **/
    private void setDCEnabled( boolean dc_enabled ) {
     _dc_enabled = dc_enabled;
    }


    // Method to read in the sensor types. Used during setup.
    // Throws BelievabilityException if it has a problem finding the techspecs
    private void readInTechSpecInformation() throws BelievabilityException {
     // Get all of the sensor tech specs from the diagnosis tech spec
     // service.
     Iterator ts_enum = 
         diagnosisTSService.getAllDiagnosisTechSpecs().iterator();
     while ( ts_enum.hasNext() ) {
         DiagnosisTechSpecInterface dtsi = 
          (DiagnosisTechSpecInterface) ts_enum.next();

         if ( logger.isDetailEnabled() )
          logger.detail("plugin adding sensor tech spec for sensor "
                    + dtsi.getName()
                    + " on asset type " + dtsi.getAssetType() );
         _model_manager.addSensorType( dtsi );
     }

     // Get all of the actuator tech specs from the action tech spec 
     // service.
     ts_enum = actionTSService.getAllActionTechSpecs().iterator();
     while ( ts_enum.hasNext() ) {
         ActionTechSpecInterface atsi = 
          (ActionTechSpecInterface) ts_enum.next();
     
         if ( logger.isDetailEnabled() ) 
          logger.detail("plugin adding actuator tech spec for action type  "
                    + atsi.getActionType()
                    + " on asset type " + atsi.getAssetType() );
         _model_manager.addActuatorType( atsi );
     }
    }


    /**
     * Make a BeliefUpdateTrigger from the input object on the blackboard,
     * copying out relevant information
     **/
    private BeliefUpdateTrigger constructUpdateTrigger( Object o ) 
     throws BelievabilityException {

     if (o instanceof DiagnosesWrapper) {
         Diagnosis diag = ((DiagnosesWrapper) o).getDiagnosis();
         return new BelievabilityDiagnosis( diag );
     }

     else if (o instanceof SuccessfulAction) {
         Action act = ((SuccessfulAction) o).getAction();
            String techspec_key = act.getClass().getName();
         ActionTechSpecInterface atsi =
          actionTSService.getActionTechSpec( techspec_key );
            BelievabilityAction ba = new BelievabilityAction( act, atsi );
         // BelievabilityAction ba = new BelievabilityAction( act );
         publishRemove ( o );
         return ba;
     }

     else throw new BelievabilityException(
                 "BelievabilityPlugin.constructUpdateTrigger",
                 "Cannot create update trigger"
                 );
    }


    // Boolean saying whether this functionality is enabled or not.
    private boolean _dc_enabled = true;

    // The time the system started up.
    private long _system_start_time = System.currentTimeMillis();

    // This is the model manager that has all of the information
    // from the techspecs.
    private ModelManagerInterface _model_manager = null;

    // This is the state estimation publisher that will be making decisions
    // about publishing state estimations.
    private StateEstimationPublisher _se_publisher = null;

    // This is the asset index for the AssetModels. It is indexed by AssetID
    private DiagnosisConsumer _diagnosis_consumer = null;

    // Vector of things waiting to be published to the blackboard
    private Vector _publication_list = new Vector();

}  // class BelievabilityPlugin
