/*
 * DefenseMonitoringPlugin.java
 *
 * Created on July 8, 2003, 4:09 PM
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
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
                    ap.getAllowedVariants().contains(latestResult.getAction()) && // the last thing that completed is something we permitted (avoids old values)
                    latestResult.getCompletionCode() != null) {  // null when action is not stopped
                    if (((latestResult.getCompletionCode().equals(Action.COMPLETED)) || 
                         (latestResult.getCompletionCode().equals(Action.ACTIVE))) // make sure it completed correctly
                     && (latestResult.getStartTime() >= ap.getStartTime()))        // make sure it's a current action
                        {
                            if (logger.isDebugEnabled()) logger.debug("Latest Result: " + latestResult.toString());
                            if (logger.isDebugEnabled()) logger.debug(monitoredActions.toString());
                            monitoredActions.remove(action);
                            ap.setResult(latestResult.getCompletionCode());
                            SuccessfulAction sa = new SuccessfulAction(action);
                            publishChange(sa);
                            if (logger.isInfoEnabled()) logger.info("Published a SuccessfulAction object: " + sa.toString());
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
