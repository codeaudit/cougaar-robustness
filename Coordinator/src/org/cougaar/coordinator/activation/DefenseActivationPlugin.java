/*
 * DefenseActivationPlugin.java
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

package org.cougaar.coordinator.activation;


import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.coordinator.policy.DefensePolicy;
import org.cougaar.coordinator.selection.SelectedAction;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;


/**
 * This Plugin is used to handle the activation of defense functions for Defense Deconfliction
 * It emits Defense & Monitoring Operating Mode objects.
 *
 */
public class DefenseActivationPlugin extends DeconflictionPluginBase implements NotPersistable {
    
    private IncrementalSubscription selectedActionSubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private String DEF_ENABLED = DefenseConstants.DEF_ENABLED.toString();
    private String MON_ENABLED = DefenseConstants.MON_ENABLED.toString();
    private String DEF_DISABLED = DefenseConstants.DEF_DISABLED.toString();
    private String MON_DISABLED = DefenseConstants.MON_DISABLED.toString();

    private Hashtable alarmTable = new Hashtable();

    long msglogDelay = 10000L; // how long to wait before disabling MsgLog - a default, but can be overridden by a parameter
 
    
    /** 
      * Creates a new instance of DefenseActivation 
      */
    public DefenseActivationPlugin() {
        super();
    }
    

    /**
      * Demonstrates how to read in parameters passed in via configuration files. Use/remove as needed. 
      */
    private void getPluginParams() {
      if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info("Coordinator not provided a MsgLog delay parameter - defaulting to 10 seconds.");

      // These really should be NAMED parameters to avoid confusion
      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           msglogDelay = Long.parseLong((String)iter.next()) * 1000L;
           logger.debug("Setting msglogDelay = " + msglogDelay);
      }

    }       

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams();
    }
    
    
    /** Called every time this component is scheduled to run. Any time objects that belong to its
     *  subscription sets change (add/modify/remove), this will be called. This method is executed
     *  within the context of a blackboard transaction (so do NOT use transaction syntax inside it).
     *  You may only need to monitor one or two type of actions (e.g. additions), in which case you
     *  can safely remove the sections of code dealing with collection changes you are not interested
     *  in.
     */
    protected void execute() {

        //***************************************************SelectedActions
        //Handle the addition of new SelectedActions
        for ( Iterator iter = selectedActionSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            SelectedAction tma = (SelectedAction)iter.next();
            if (preconditionsMet(tma)) {
                long delay = delayTime(tma);
                if (delay > 0L) 
                    activateWithDelay(tma, delay);
                else 
                    activateNow(tma);
            }
            publishRemove(tma);     
        }
           
    }
    
    protected boolean preconditionsMet(SelectedAction action) {
        return true;  // for now we do not support preconditions
    }
    
    protected long delayTime(SelectedAction action) {
        if ((action.getDefenseName().equals("Msglog") && (action.getDefenseAction().equals(DEF_ENABLED))))
            return msglogDelay;
        else
            return 0L;
    }
 
    protected void activateWithDelay(SelectedAction action, long delay) {
        getAlarmService().addRealTimeAlarm(new DelayedActionAlarm(action, delay));
    }
    
    protected void activateNow(SelectedAction action) {
      DefenseEnablingOperatingMode dm = DefenseEnablingOperatingMode.find(action.getDefenseName(), action.getExpandedAssetName(), blackboard);
      MonitoringEnablingOperatingMode mm = MonitoringEnablingOperatingMode.find(action.getDefenseName(), action.getExpandedAssetName(), blackboard);
      dm.setValue(action.getDefenseAction());
      mm.setValue(action.getMonitoringAction());
      if (logger.isInfoEnabled()) logger.info(dm.getDefenseName()+":"+dm.getExpandedName()+":"+dm.getValue());
      if (logger.isInfoEnabled()) logger.info(mm.getDefenseName()+":"+mm.getExpandedName()+":"+mm.getValue());
      publishChange(dm);
      publishChange(mm);
    }
    
    protected void setupSubscriptions() {


        selectedActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof SelectedAction) {
                    return true ;
                }
                return false ;
            }
        }) ;        
    }

    private class DelayedActionAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        SelectedAction action;
        
        public DelayedActionAlarm (SelectedAction action, long delay) {
            detonate = System.currentTimeMillis() + delay;
            this.action = action;
            if (logger.isDebugEnabled()) logger.debug("DelayedPublishAlarm created : "+action.getDefenseName()+":"+action.getExpandedAssetName()+" in "+detonate+" sec");
        }
        
        public long getExpirationTime () {return detonate;
        }
        
        public void expire () {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("DelayedPublishAlarm expired for: "+action.getDefenseName()+":"+action.getExpandedAssetName());
                blackboard.openTransaction();
                activateNow(action);
                blackboard.closeTransaction();
            }
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
