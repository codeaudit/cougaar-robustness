/*
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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



package org.cougaar.coordinator.policy;


import org.cougaar.coordinator.*;

import java.util.Iterator;
import java.util.Collection;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.EventService;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.believability.BelievabilityKnob;
import org.cougaar.core.agent.service.alarm.Alarm;


/**
 * This Plugin is used to handle the defense policy functions for Defense Deconfliction
 * It emits CostBenefitDiagnosis objects.
 *
 */
public class DefensePolicyPlugin extends DeconflictionPluginBase implements NotPersistable {
    
    
    private IncrementalSubscription believabilityKnobSubscription;
    
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };

    private static final boolean leashOnRestart;
    private static final long leashTimeAfterCoordinatorRestart;
    private boolean somethingExpired = false;

    static
    {
         String s = "org.cougaar.coordinator.leashOnRestart";
         leashOnRestart = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
    }

    static
    {
         String s = "org.cougaar.coordinator.leashTimeAfterCoordinatorRestart";
         leashTimeAfterCoordinatorRestart = Long.valueOf(System.getProperty(s, "240")).longValue();
    }
    
    /** 
      * Creates a new instance of DefensePolicyPlugin 
      */
    public DefensePolicyPlugin() {
    }
    
     

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
    }
    
    
    /** Called every time this component is scheduled to run. Any time objects that belong to its
     *  subscription sets change (add/modify/remove), this will be called. This method is executed
     *  within the context of a blackboard transaction (so do NOT use transaction syntax inside it).
     *  You may only need to monitor one or two type of actions (e.g. additions), in which case you
     *  can safely remove the sections of code dealing with collection changes you are not interested
     *  in.
     */
    protected void execute() {

        if (blackboard.didRehydrate()) {
            Iterator iter = believabilityKnobSubscription.iterator();    
            BelievabilityKnob knob;
            if (iter.hasNext()) {
                knob = (BelievabilityKnob) iter.next();
            }
            else {
                knob = new BelievabilityKnob();
                publishAdd(knob);
            }
            if(leashOnRestart) {
                knob.setIsLeashed(true);
                blackboard.publishChange(knob);
                if (logger.isDebugEnabled()) logger.debug("rehydrate detected  && leashOnRestart==true - Leashing Defenses");       
            }
            else if (leashTimeAfterCoordinatorRestart > 0) { // leash the defenses on an MA restart, dlw - 9/29/04
                getAlarmService().addRealTimeAlarm(new LeashAlarm(leashTimeAfterCoordinatorRestart));
                knob.setIsLeashed(true);
                blackboard.publishChange(knob);
                if (logger.isDebugEnabled()) logger.debug("rehydrate detected  && leashTimeAfterCoordinatorRestart==" + leashTimeAfterCoordinatorRestart  + " - Leashing Defenses");       
            }
        }

        if (somethingExpired) {
            Iterator iter = believabilityKnobSubscription.iterator();    
            if (iter.hasNext()) {
                BelievabilityKnob knob = (BelievabilityKnob) iter.next();
                knob.setIsLeashed(false);
                blackboard.publishChange(knob);
                somethingExpired = false;
                if (logger.isDebugEnabled()) logger.debug("Unleashing Defenses");       
            }
        }
    }
    
    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

        //Not used at this time - Will be used to provide out-of-band control of this plugin
        believabilityKnobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof BelievabilityKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;
    }

    private class LeashAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        
        public LeashAlarm (long delay) {
            detonate = System.currentTimeMillis() + 1000*delay;
            if (logger.isDebugEnabled()) logger.debug("LeashAlarm created, will expire in "+delay+" sec");
        }
        
        public long getExpirationTime () { return detonate; }
        
        public void expire () {
            if (!expired) {
                expired = true;
                somethingExpired = true;
                signalClientActivity();
                if (logger.isDebugEnabled()) logger.debug("LeashAlarm expired");
            }
        }
        public boolean hasExpired () { return expired; }

        public boolean cancel () {
            if (!expired)
                return expired = true;
            return false;
        }
 
    }    
    
}
