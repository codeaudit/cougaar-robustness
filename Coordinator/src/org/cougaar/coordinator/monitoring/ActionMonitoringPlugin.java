/*
 * DefenseMonitoringPlugin.java
 *
 * Created on July 8, 2003, 4:09 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.monitoring;

import org.cougaar.coordinator.MonitoringPluginBase;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.ActionRecord;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.activation.ActionPatience;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;


import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.util.UID;


/**
 * This Plugin is used to handle the defense monitoring functions for Defense Deconfliction
 * It emits DefenseCondition objects.
 *
 */
public class ActionMonitoringPlugin extends MonitoringPluginBase implements NotPersistable {
    
    
    private IncrementalSubscription monitoredActionsSubscription;
    private IncrementalSubscription actionsWrapperSubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private Hashtable monitoredActions;
    private boolean somethingExpired = false;
    private Hashtable expiredAlarms;
        
 
    /** 
      * Creates a new instance of DefenseMonitoringPlugin 
      */
    public ActionMonitoringPlugin() {
        super();
    }
    

    public void load() {
        super.load();
        monitoredActions = new Hashtable();
        expiredAlarms = new Hashtable();
    }
    
    

    protected void execute() {

        if (somethingExpired) {
            synchronized(expiredAlarms) {
                Iterator iter = expiredAlarms.values().iterator();
                while (iter.hasNext()) {
                    ActionTimeoutAlarm thisAlarm = (ActionTimeoutAlarm)iter.next();
                    if (thisAlarm.hasExpired()) thisAlarm.handleExpiration();
                }
            }
            somethingExpired = false;
       }

        //***************************************************SelectedActions
        //Handle the addition of new ActionPatience for SelectedActions
        for ( Iterator iter = monitoredActionsSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ActionPatience ap = (ActionPatience)iter.next();
            //findDesiredOutcomes(ap);
            startTimerIfNone(ap); 
        }

        for ( Iterator iter = monitoredActionsSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ActionPatience ap = (ActionPatience)iter.next();
            //findDesiredOutcomes(ap);
            cancelTimerIfExists(ap); 
        }

        // Watches for desirable outcomes for Actions
        for ( Iterator iter = actionsWrapperSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ActionsWrapper aw = (ActionsWrapper)iter.next();
            Action action = aw.getAction();
            ActionRecord latestResult = action.getValue();
            ActionTimeoutAlarm alarm = (ActionTimeoutAlarm)monitoredActions.get(action);
            ActionPatience ap = findActionPatience(action);
            if (alarm != null) { // This is an Action change that we are monitoring (it could be an Action change we are not monitoring)
                if (logger.isDebugEnabled()) logger.debug(action.toString());
                if (logger.isDebugEnabled()) if (ap != null) logger.debug(ap.toString()); else logger.debug("No ActionPatience Found");
                if (latestResult != null &&                      // null when action is not started
                    latestResult.getCompletionCode() != null) {  // null when action is not stopped
                    if (((latestResult.getCompletionCode().equals(Action.COMPLETED)) || 
                         (latestResult.getCompletionCode().equals(Action.ACTIVE))) // make sure it completed correctly
                     && (latestResult.getStartTime() >= ap.getStartTime()))        // make sure it's a current action
                        {
                            if (logger.isDebugEnabled()) logger.debug("Latest Result: " + latestResult.toString());
                            if (logger.isDebugEnabled()) logger.debug(monitoredActions.toString());
                            monitoredActions.remove(action);
                            ap.setResult(Action.COMPLETED);
                            SuccessfulAction sa = new SuccessfulAction(action);
                            publishChange(sa);
                            if (logger.isDebugEnabled()) logger.debug("Published a SuccessfulAction object: " + sa.toString());
                            publishChange(ap);
                            alarm.cancel();
                        }
                    }
            }
        }

    }

    /* 
     *  publishes DesiredOutcome objects to the BB for each indicator that the Action worked
     */
    private void findDesiredOutcomes(ActionPatience ap) {
        // find the AssetStateDimension this Action works in
        // find the desired end state for this Action (in the future there may be more than one)
        // emit a DesiredOutcome object for this end state
        return;
    }
        
    

    protected void setupSubscriptions() {

        monitoredActionsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( ActionPatience.pred);

        actionsWrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionsWrapper) {
                    return true ;
                }
                return false ;
            }
        }) ;


        super.setupSubscriptions();
        
    }

    private void startTimerIfNone(ActionPatience ap) {
        ActionTimeoutAlarm alarm = (ActionTimeoutAlarm) monitoredActions.get(ap.getAction());
        if (alarm != null) {
            alarm.cancel(); //changed our minds - no longer want this timeout, may want a different one
        }
        alarm = new ActionTimeoutAlarm(ap);
        monitoredActions.put(ap.getAction(), alarm);
        if (logger.isDetailEnabled()) logger.detail("Index after adding an Alarm:" + monitoredActions.toString());
        getAlarmService().addRealTimeAlarm(alarm);            
        }

    
    private void cancelTimerIfExists(ActionPatience ap) {
        ActionTimeoutAlarm alarm = (ActionTimeoutAlarm) monitoredActions.get(ap.getAction());
        if (alarm != null) {
            alarm.cancel(); //changed our minds - no longer want this timeout
        }        
    }

    
    private class ActionTimeoutAlarm implements Alarm {
        private ActionPatience ap;
        private long detonate;
        private boolean expired;
        private AssetID assetID;
        private Action action;
       // private UID uid;
        
        public ActionTimeoutAlarm (ActionPatience ap) {
            detonate = ap.getDuration() + ap.getStartTime();
            this.action = ap.getAction();
            this.ap = ap;
            if (logger.isDebugEnabled()) logger.debug("ActionTimeoutAlarm created : " + detonate + " " + action);
        }
        
        public long getExpirationTime () {
            return detonate;
        }

        public void expire () {
            expired = true;
            synchronized(expiredAlarms) {
                somethingExpired = true;
                expiredAlarms.put(action, this);
            }
            signalClientActivity();
        }

        public void handleExpiration() {
            ap.setResult(Action.FAILED);
            monitoredActions.remove(action);
            if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + action.toString());
            if (logger.isDetailEnabled()) logger.detail("Index after deleting alarm: " + monitoredActions.toString());
            publishChange(ap);
        }

        public boolean hasExpired () {return expired;
        }

        public boolean cancel () {
            if (!expired)
                return expired = true;
            return false;
        }
        
    }
  
    
}
