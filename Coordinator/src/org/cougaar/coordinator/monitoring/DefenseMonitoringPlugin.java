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

import org.cougaar.coordinator.DefenseApplicabilityCondition;
import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
import org.cougaar.coordinator.DeconflictionPluginBase;

import org.cougaar.coordinator.techspec.AssetName;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.selection.ActionPatience;
import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;
import org.cougaar.coordinator.DefenseConstants;
 

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;


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
public class DefenseMonitoringPlugin extends DeconflictionPluginBase implements NotPersistable {
    
    
    private IncrementalSubscription monitoredActionSubscription;
    private IncrementalSubscription diagnosisSubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private Hashtable monitoredActions;
        
 
    /** 
      * Creates a new instance of DefenseMonitoringPlugin 
      */
    public DefenseMonitoringPlugin() {
        super();
    }
    

    public void load() {
        super.load();
        monitoredActions = new Hashtable();
    }
    
    

    protected void execute() {

        //***************************************************SelectedActions
        //Handle the addition of new ActionPatience for SelectedActions
        for ( Iterator iter = monitoredActionSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ActionPatience ap = (ActionPatience)iter.next();
            startTimerIfNone(ap); 
        }
        
        for ( Iterator iter = diagnosisSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseApplicabilityCondition dc = (DefenseApplicabilityCondition)iter.next();
            if (dc.getValue().toString().equalsIgnoreCase("TRUE")) { continue; } //ignore unless false
            //Make sure this is a defense that we have a timer for!
            DefenseTimeoutAlarm alarm = (DefenseTimeoutAlarm)monitoredActions.get(dc.getExpandedName());
            if (!alarm.defense.equalsIgnoreCase(dc.getDefenseName()) ) {
                if (logger.isDebugEnabled()) logger.debug("DefenseTimeoutAlarm *-* Ignored DefCon we don't care about *-*");
                continue; // not the defense we'return watching for! Ignore it!
            }
            //Remove it... it's the right one...    
            monitoredActions.remove(dc.getExpandedName());

            if (alarm != null) {
                alarm.sendBackResults(dc);
                alarm.cancel();
            }
        }
    }
    

    protected void setupSubscriptions() {

        monitoredActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionPatience) {
                    return true ;
                }
                return false ;
            }
        }) ;

        diagnosisSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseApplicabilityCondition) {
                    return true ;
                }
                return false ;
            }
        }) ;

        
        //Not used at this time - Will be used to provide out-of-band control of this plugin
        pluginControlSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseMonitoringKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
    }

    private void startTimerIfNone(ActionPatience ap) {
        DefenseTimeoutAlarm alarm = (DefenseTimeoutAlarm) monitoredActions.get(ap.getExpandedName());
        if (alarm != null) {
            alarm.cancel(); //changed our minds - no longer want this timeout, may want a different one
        }
        alarm = new DefenseTimeoutAlarm(ap);
        monitoredActions.put(ap.getExpandedName(), alarm);
        getAlarmService().addRealTimeAlarm(alarm);            
        }

    
    private class DefenseTimeoutAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        private String asset;
        private String assetType;
        private String defense;
        private UID uid;
        
        public DefenseTimeoutAlarm (ActionPatience ap) {
            detonate = (long)Double.parseDouble(ap.getValue().toString()) + System.currentTimeMillis();
            this.asset = ap.getAsset();
            this.assetType = ap.getAssetType();
            this.defense = ap.getDefenseName();
            this.uid = ap.getUID();
            if (logger.isDebugEnabled()) logger.debug("DefenseTimeoutAlarm created : " + detonate + " " + asset);
        }
        
        public long getExpirationTime () {
            return detonate;
        }
        
        public void expire () {
            openTransaction();
            sendBackResults(null);
            closeTransaction();
        }
        public boolean hasExpired () {return expired;
        }
        public boolean cancel () {
            if (!expired)
                return expired = true;
            return false;
        }
        
        protected void sendBackResults(DefenseApplicabilityCondition dac) {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("DefenseTimeoutAlarm expired for: " + asset+":"+assetType);
                // Did the Defense succeed?
                try {
                    if (dac == null) {
                        dac = DefenseApplicabilityCondition.find(defense, AssetName.generateExpandedAssetName(asset,AssetType.findAssetType(assetType)), blackboard);
                    }
                    DefenseApplicabilityConditionSnapshot snapshot = new DefenseApplicabilityConditionSnapshot(dac);
                    snapshot.setCompletionTime(System.currentTimeMillis()); //set time that this defense completed (succeed or failure)
                    publishAdd(new DefenseTimeoutCondition(snapshot));
                    // no longer interested in this defense no matter what it did
                    monitoredActions.remove(dac.getExpandedName());
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) logger.warn("Could not find condition for asset: " + asset+ " for defense: "+ defense);
                } 
            }
        }
    }
    
    
}
