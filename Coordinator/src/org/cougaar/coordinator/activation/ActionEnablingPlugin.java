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


import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.costBenefit.ActionEvaluation;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;
import org.cougaar.coordinator.policy.DefensePolicy;
import org.cougaar.coordinator.selection.SelectedAction;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
//import org.cougaar.util.UnaryPredicate;


/**
 * This Plugin is used to handle the activation of defense functions for Defense Deconfliction
 * It emits Defense & Monitoring Operating Mode objects.
 *
 */
public class ActionEnablingPlugin extends DeconflictionPluginBase implements NotPersistable {
    
    private IncrementalSubscription selectedActionSubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private Hashtable alarmTable = new Hashtable();

    long msglogDelay = 10000L; // how long to wait before disabling MsgLog - a default, but can be overridden by a parameter
 
    
    /** 
      * Creates a new instance of DefenseActivation 
      */
    public ActionEnablingPlugin() {
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
            SelectedAction sa = (SelectedAction)iter.next();
            if (preconditionsMet(sa)) {
                long delay = delayTime(sa);
                if (delay > 0L) 
                    activateWithDelay(sa, delay);
                else 
                    activateNow(sa);
            }
            publishRemove(sa);     
        }
           
    }
    
    protected boolean preconditionsMet(SelectedAction action) {
        return true;  // for now we do not support preconditions
    }
    
    protected long delayTime(SelectedAction sa) {
        if ((sa.getActionEvaluation().getAction().getTechSpec().getStateDimension().equals("Comunications")) && (sa.getActionVariants().contains("Disabled")))
            return msglogDelay;
        else
            return 0L;
    }
 
    protected void activateWithDelay(SelectedAction sa, long delay) {
        getAlarmService().addRealTimeAlarm(new DelayedActionAlarm(sa, delay));
    }
    
    protected void activateNow(SelectedAction sa) {
      Action action = sa.getActionEvaluation().getAction();
      long maxPredictedTime = 0L;
      try {
        Set permittedVariants = new HashSet();
        Iterator iter = sa.getActionVariants().iterator();
        while (iter.hasNext()) {
            VariantEvaluation ve = (VariantEvaluation)iter.next();
            permittedVariants.add(ve.getVariantName());
            maxPredictedTime = Math.max(ve.getExpectedTransitionTime(), maxPredictedTime);
        }
        action.setPermittedValues(permittedVariants);
        }
      catch (IllegalValueException e) {
        logger.error("attempting to enable a non-existent action variant");
        return ; // w/o publishing the bad action, but continue
        }
      if (logger.isInfoEnabled()) logger.info("Setting: "+action);
      publishAdd(new ActionPatience(action, sa.getPatience()));
      publishChange(action);
    }
    
    protected void setupSubscriptions() {

        super.setupSubscriptions();
        selectedActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(SelectedAction.pred) ;        
    }

    private class DelayedActionAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        SelectedAction sa;
        
        public DelayedActionAlarm (SelectedAction sa, long delay) {
            detonate = System.currentTimeMillis() + delay;
            this.sa = sa;
            if (logger.isDebugEnabled()) logger.debug("DelayedPublishAlarm created : "+sa+" in "+detonate+" sec");
        }
        
        public long getExpirationTime () {return detonate;
        }
        
        public void expire () {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("DelayedPublishAlarm expired for: "+sa);
                blackboard.openTransaction();
                activateNow(sa);
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
