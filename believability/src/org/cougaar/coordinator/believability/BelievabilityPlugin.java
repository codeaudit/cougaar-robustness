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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.leashDefenses.LeashRequestDiagnosis;

import org.cougaar.coordinator.monitoring.SuccessfulAction;

import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.ActionTechSpecService;
import org.cougaar.coordinator.techspec.DefaultThreatModel;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.TechSpecsLoadedCondition;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

import org.cougaar.core.service.AlarmService;

import org.cougaar.core.agent.service.alarm.Alarm;

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
    public static final boolean USE_LEASH_DIAGNOSIS = false;

    // We keep a state variable about rehydration status to know what
    // we need to do.  These are the possible state values.
    //
    public static final int REHYDRATION_NOT_NEEDED    = 0;
    public static final int REHYDRATION_NEEDED        = 1;
    public static final int REHYDRATION_IN_PROGRESS   = 2;
    public static final int REHYDRATION_COMPLETED     = 3;

    // We keep a state variable about whether all the techspecs are
    // loaded or not, since the believability plugin could enter the
    // execute loop before all the techspecs are loaded (I think).
    // There is a special blackboard object posted by the techspec
    // manager when everything has been loaded.
    //
    public static final int TECHSPECS_NOT_LOADED      = 0;
    public static final int TECHSPECS_LOADED          = 1;

    /** 
     * Creates a new instance of BelievabilityPlugin 
     **/
    public BelievabilityPlugin() {

        super(requiredServices);


     // Initialize various classes
     _model_manager = new ModelManager();
     _se_publisher = new StateEstimationPublisher( this, _model_manager );
     _trigger_consumer = new TriggerConsumer( this,
                                _model_manager,
                                _se_publisher );
     _se_publisher.setAssetContainer( _trigger_consumer.getAssetContainer() );
     
    } // constructor BelievabilityPlugin
    

    /**
     * Called from outside. Should contain plugin initialization code.
     **/
    public void load() {

        super.load();
        getPluginParams();
        haveServices();

        // Check to see if we are rehydrating, because if so, we need
        // to set up the internal data from existing blackboard
        // objects. 
        //
        if ( getBlackboardService().didRehydrate() )
        {
            if (logger.isDetailEnabled()) 
                logger.detail("Believability Plugin is rehydrating.");
            _rehydration_state = REHYDRATION_NEEDED;
        }


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
    public synchronized void queueForPublication( Object obj ) {
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
     * Since getAlarmService() is a protected method, we need to wrap
     * it this way to provide other classes access so they can set
     * timers.
     */
    AlarmService getAlarmServiceHandle() { return getAlarmService(); }

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

     // Check for diagnosis leashing
     if ( ! USE_LEASH_DIAGNOSIS) {
      if ( logger.isDebugEnabled() )
          logger.debug( "INITIAL LEASHING OF DIAGNOSES IS DISABLED" );
      _dc_enabled = true;
      _dc_enabled_new = true;
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
     
     _techspecLoadedSub 
      = ( IncrementalSubscription ) 
      getBlackboardService().subscribe
      ( new UnaryPredicate() 
          {
           public boolean execute(Object o) {
               if ( o instanceof TechSpecsLoadedCondition ) {
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

        // We must have all the techspecs in order to handle any
        // belief update triggers, so check for this first if we have
        // not yet seen the special TechSpecsLoadedCondition object.
        //
        if ( _techspec_load_state != TECHSPECS_LOADED )
        {

            if (logger.isDetailEnabled() ) 
                logger.detail ("Checking for techspec loaded object.");
            
            // This may or may not change the techspec load state.
            //
            handleTechSpecLoadedCondition();

            // Bail out if the techspecs are still not fully loaded.
            // Note that we do not want to handle rehydration and/or
            // any belief update trigger ADD or CHANGE unless we have
            // all the techspecs.
            //
            if ( _techspec_load_state != TECHSPECS_LOADED )
                return;

        } // if tech spec loaded condition object not yet seen

        // This will only be true at most one time for the case where
        // this variable is set in the load() method and this is the
        // first cal to execute().  In this case, we need to recreate
        // our internal asset data from the blackboard objects.
        //
        if ( _rehydration_state == REHYDRATION_NEEDED )
        {
            handleRehydration();

            // It is possible that there are ADD/CHANGED objects
            // also. Since rehydration handles *ALL* subscription
            // objects, we have to make sure we do *NOT* call
            // handleUpdateTriggers() as this would result in
            // processing these twice.
            //
            return;
        }

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
              iter.hasNext() ; ) 
     {
         if (logger.isDetailEnabled() ) 
             logger.detail ("UpdateTrigger ADD");

         handleUpdateTriggerObject( iter.next() );

     } // iterator for ADD update trigger

     // ------- CHANGE UpdateTrigger
     for ( iter = _beliefUpdateTriggerSub.getChangedCollection().iterator();
           iter.hasNext() ; ) {

         if (logger.isDetailEnabled() ) 
             logger.detail ("UpdateTrigger CHANGE");

         handleUpdateTriggerObject( iter.next() );

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

    //************************************************************
    /**
     * Handles a single update triggfer object from the update trigger
     * subscrtiption.
     *
     * @param trigger_obj The object from the enumeration of the
     * subscription collection
     */
    private void handleUpdateTriggerObject( Object trigger_obj )
    {
        if (logger.isDetailEnabled() ) 
            logger.detail ("Handling update trigger: "
                           + trigger_obj.toString() );
 
        // For now, we only worry about using the last diagnosis
        // Method implementation comments go here ...
        BeliefUpdateTrigger but = null;

         try 
         {
             // This is for leashing of diagnoses, to be sure that the
             // LeashRequestDiagnosis is propagated both when it enables
             // and when it disables the defense controller
             _dc_enabled_new = _dc_enabled;

             but = constructUpdateTrigger( trigger_obj );
          
             // Check to see whether the defense controller is enabled at
             // the moment
             if (_dc_enabled) 
                 _trigger_consumer.consumeUpdateTrigger( but );
             else
             {
                 if (logger.isDetailEnabled() ) 
                     logger.detail ("Leashing Enabled: tigger ignored.");
             }
             
             _dc_enabled = _dc_enabled_new;
         }
         catch ( BelievabilityException be ) 
         {
             if (logger.isWarnEnabled() ) 
                 logger.warn( "Problem processing added trigger "
                              + be.getMessage() );
         }

    } // method handleUpdateTriggerObject


    //************************************************************
    /**
     * Checks if the special TechSpecsLoadedCondition object has been
     * published, and if so update the tech spec load state
     * accordingly. 
     **/
    private void handleTechSpecLoadedCondition()
    {
        Iterator it = _techspecLoadedSub.getAddedCollection().iterator();

        // Only need to see one thing in this collection to know that
        // the techspec load condition object has been published.
        //
        if (it.hasNext()) 
        { 
            if (logger.isDetailEnabled() ) 
                logger.detail ("Techspec loaded object seen.");
            
            _techspec_load_state = TECHSPECS_LOADED;

            // Now we can load all the information we need to from the
            // techspec service.
            //
            try 
            {
                readInTechSpecInformation();
            }
            catch ( BelievabilityException be ) 
            {
                if ( logger.isDebugEnabled() )
                    logger.debug( "Problem reading techspecs: " 
                                  + be.getMessage() );
            }
            
        }
        else
        {
            if (logger.isDetailEnabled() ) 
                logger.detail ("Techspec loaded object not yet seen.");
        }

    } // method handleTechSpecLoadedCondition

    //************************************************************
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
    private IncrementalSubscription _techspecLoadedSub;

    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };
    private DiagnosisTechSpecService diagnosisTSService = null;
    private ActionTechSpecService actionTSService = null;


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
     * copying out relevant information. If this is a diagnosis leashing
     * related diagnosis, then this also has the side effect of adjusting
     * the propagation of diagnoses.
     **/
    private BeliefUpdateTrigger constructUpdateTrigger( Object o ) 
     throws BelievabilityException {

        if (logger.isDetailEnabled() ) 
            logger.detail ("Constructing update trigger for: "
                           + o.getClass().getName() );

     if (o instanceof DiagnosesWrapper) {
         Diagnosis diag = ((DiagnosesWrapper) o).getDiagnosis();

      // First check for leashing of diagnoses
      if ( o instanceof LeashRequestDiagnosis ) {
          
          if (logger.isDetailEnabled() ) 
              logger.detail ("Handling leashing diagnosis.");

          boolean is_stable =
           (! ( (LeashRequestDiagnosis)diag).areDefensesLeashed() );
          if ( is_stable ) _dc_enabled_new = true;
          else _dc_enabled_new = false;
      }

      // Now pass on diagnosis
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

    //************************************************************
    /**
     * Will query the blackboard for data that was previously posted
     * before the plugin was rehydrated.  This allows us to recreate the
     * state of assets that existed, but which will be lost upon a
     * rehydration. 
     *
     */
    private void handleRehydration( )
    {

        if (logger.isDetailEnabled() ) 
            logger.detail ("Handling Rehydration");

        _rehydration_state = REHYDRATION_IN_PROGRESS;

        // We set this in the model manager, because when we rehydrate
        // we will use the uniform probability distribution as the a
        // rpiori beliewf state, rather than the techspec derived one.
        //
        _model_manager.setRehydrationHappening( true );

//        zzz Handle reading leashing state from believability knob on blackboard;

        // Loop through all the diagnosis and action objects on the
        // blackboard and process them to create the initial
        // belief/state estimations.
        //
        for ( Iterator iter = _beliefUpdateTriggerSub.getCollection().iterator();  
              iter.hasNext() ; ) 
        {
            Object trigger_obj =  iter.next();

            // Ignore SuccessfulAction objects on rehydration
            //
            if ( trigger_obj instanceof SuccessfulAction )
                continue;

            if (logger.isDetailEnabled() ) 
                logger.detail ("UpdateTrigger REHYDRATE");

            handleUpdateTriggerObject( trigger_obj );

        } // iterator for REHYDRATE update trigger

        _rehydration_state = REHYDRATION_COMPLETED;

        // Make sure to set this to false, so that any *new* assets
        // will properly start at the techspec a priori belief state
        // (the uniform initial belief only makes sense for assets
        // that existed prior to rehydration.)
        //
        _model_manager.setRehydrationHappening( false );

        if (logger.isDetailEnabled() ) 
            logger.detail ("Finished Handling Rehydration");

    } // method handleRehydration

    // Boolean saying whether this functionality is enabled or not.
    private boolean _dc_enabled = false;

    // Flag to indicate what the new value of _dc_enabled should be at the
    // end of the execute run
    private boolean _dc_enabled_new = false;

    // The time the system started up.
    private long _system_start_time = System.currentTimeMillis();

    // This is the model manager that has all of the information
    // from the techspecs.
    private ModelManagerInterface _model_manager = null;

    // This is the state estimation publisher that will be making decisions
    // about publishing state estimations.
    private StateEstimationPublisher _se_publisher = null;

    // This is the asset index for the AssetModels. It is indexed by AssetID
    private TriggerConsumer _trigger_consumer = null;

    // Vector of things waiting to be published to the blackboard
    private Vector _publication_list = new Vector();

    // When the plugin rehydrates, we need to specially handle getting
    // older information from blackboard objects.  We use this state
    // variable to maintain the current status of rehydration.
    //
    private int _rehydration_state = REHYDRATION_NOT_NEEDED;

    // We get explicit notification via a blackboard object about
    // when all the techspecs are loaded.  We keep this local state
    // variable to keep track of this state.
    //
    private int _techspec_load_state = TECHSPECS_NOT_LOADED;

}  // class BelievabilityPlugin
