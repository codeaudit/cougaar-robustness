/*
 * TimedDiagnosisPlugin.java
 *
 * Created on July 8, 2003, 12:05 PM
 * 
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

package org.cougaar.coordinator.timedDiagnosis;

import org.cougaar.coordinator.thrashingSuppression.ThrashingSuppressionApplicabilityCondition;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.*;

import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.service.alarm.Alarm;


/**
 * This Plugin is used to handle the timed collection of defense diagnoses.
 * It emits TimedDefenseDiagnosis objects.
 *
 */
public class TimedDiagnosisPlugin extends DeconflictionPluginBase implements NotPersistable {
   
   // private BlackboardService bb;
    
    private IncrementalSubscription defenseTechSpecsSubscription;
    private IncrementalSubscription defenseConditionsSubscription;
    private IncrementalSubscription pluginControlSubscription;
    private IncrementalSubscription searchCommunitySubscription;
    private IncrementalSubscription communityResponseSubscription;
    private IncrementalSubscription defenseSubscription;
    private IncrementalSubscription timedDefenseDiagnosisSubscription;
    private IncrementalSubscription initDoneSubscription;
    private IncrementalSubscription thrashingSuppressionSubscription;
    
    private ThrashingSuppressionApplicabilityCondition tsac = null;
    private TimedDiagnosisKnob timedDiagnosisKnob;
    private HashSet pendingTimedDiagnoses;
    private boolean initDone = false;
    private boolean thrashingSuppressed = true;

    
    /** 
      * Creates a new instance of TimedDiagnosisPlugin 
      */
    public TimedDiagnosisPlugin() {
    }
         

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        timedDiagnosisKnob = new TimedDiagnosisKnob();
        timedDiagnosisKnob.setWaitTime(5000L); //set default
        pendingTimedDiagnoses = new HashSet();
        //Join Community
        //Request community membership - block until we have that if possible
        //Populate the hash table with empty TimedDefenseDiagnosis objects for each asset in the community
    }

      //Create one condition and one of each type of operating mode
    private void initObjects() {
        // All Conditions & OpModes used by the Coordiator are published by the Defenses
        ThrashingSuppressionApplicabilityCondition tsac = new ThrashingSuppressionApplicabilityCondition();
        tsac.setUID(us.nextUID());
        
        blackboard.publishAdd(tsac);
        
        if (logger.isDebugEnabled()) logger.debug("Created Condition for ThrashingSuppression");
        
        // enable all defenses after a restart of Manager Agent  - sjf - 10/24/03
        if (blackboard.didRehydrate()) {
            tsac.setValue(DefenseConstants.BOOL_FALSE);
            blackboard.publishChange(tsac);
            if (logger.isDebugEnabled())
                logger.debug("rehydrate detected - disabling ThrashingSuppression");
        }
    }


    protected void execute() {
        

        //Get the timedDiagnosisKnob - it contains the max time to wait
        //----------------------------------------------------------------
        /* Uncomment should we ever need this on the BB
        for ( Iterator iter = pluginControlSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            timedDiagnosisKnob = (TimedDiagnosisKnob) iter.next();
        }
        */

        Iterator iterInitDone = initDoneSubscription.getAddedCollection().iterator();
        if (iterInitDone.hasNext()) {
            initDone = true;
            thrashingSuppressed = false;
        }
            
      //handle changes to thrashing suppression to ENABLE or DISABLE
      Iterator iterThrash = thrashingSuppressionSubscription.getChangedCollection().iterator();
      if (iterThrash.hasNext()) {
          if (!applicable(tsac)) {//thrashing suppression is diasbled
              thrashingSuppressed = false;
              if (eventService.isEventEnabled()) eventService.event("Defenses Unleashed");
              
              // toggle DoS Defense before PROCEEDING - specific to this one defense
              Collection c = DefenseEnablingOperatingMode.findDefenseCollection("AttackReset", blackboard);
              Iterator iter2 = c.iterator();
              while (iter2.hasNext()) {
                  DefenseEnablingOperatingMode deom = (DefenseEnablingOperatingMode)iter2.next();
                  if (deom.getValue().equals(DefenseConstants.DEF_ENABLED.toString()))
                      deom.setValue(DefenseConstants.DEF_DISABLED.toString());
                  else
                      deom.setValue(DefenseConstants.DEF_ENABLED.toString());
                  blackboard.publishChange(deom);
                  if (logger.isDebugEnabled()) logger.debug(deom.getClass()+" for "+deom.getExpandedName()+" toggled");
              }
          }
          else {//trashing suppression is renabled
              thrashingSuppressed = true;
              if (eventService.isEventEnabled()) eventService.event("Defenses Suppressed");
              
              //DISABLE all Defenses (execpt MsgLog) before PROCEEDING
              Collection c = DefenseEnablingOperatingMode.findDefenseCollection("AttackReset", blackboard);
              Iterator iter2 = c.iterator();
              while (iter2.hasNext()) {
                  DefenseEnablingOperatingMode deom = (DefenseEnablingOperatingMode)iter2.next();
                  if (!deom.getDefenseName().equals("Msglog")) {
                      deom.setValue(DefenseConstants.DEF_DISABLED.toString());
                      blackboard.publishChange(deom);
                      if (logger.isDebugEnabled()) logger.debug(deom.getClass()+" for "+deom.getExpandedName()+" suppressed");
                  }
              } 
          } 
      }       

        
        //Get the defenses - to compute the max latency time IF any defenses were added or changed.
        //----------------------------------------------------------------------------------------
        long defenseLatency = -1L;
        if (defenseSubscription.getChangedCollection().size() > 0 ) { 
            //then at least one defense value changed, so look at all defenses to recalculate
            defenseLatency = calculateDefenseLatency(defenseSubscription.getCollection().iterator(), defenseLatency);
        } else {
            Iterator defIter = defenseSubscription.getAddedCollection().iterator();  
            if (defIter.hasNext()) { //then at least one new defense has been added
                defenseLatency = calculateDefenseLatency(defIter, timedDiagnosisKnob.getWaitTime());
            }
        }
        //Update the defenseLatency value
        if (defenseLatency >= 0) { //it has been changed, so update the value
            timedDiagnosisKnob.setWaitTime(defenseLatency);
            //getBlackboardService().publishChange(timedDiagnosisKnob); //if this goes on the BB
            if (logger.isDebugEnabled()) logger.debug("** Updated Defense Latency time in control knob. New Value = " +defenseLatency);
        }
        
        
        
        //Handle the chnages to previously published TDDs - remove them & keep a record if desired
        for ( Iterator iter = timedDefenseDiagnosisSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            TimedDefenseDiagnosis tdd = (TimedDefenseDiagnosis) iter.next();
            publishRemove(tdd);
        }
        
        
        if (thrashingSuppressed) return;  // don't start creating TDDs if thrashing suppression is TRUE (the system is not stable)
        
        //Handle the modification of DefenseConditions
        for ( Iterator iter = defenseConditionsSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseApplicabilityCondition dac = (DefenseApplicabilityCondition) iter.next();
            if (logger.isDebugEnabled()) logger.debug(dac.getExpandedName()+":"+dac.getValue().toString());
            if ((dac.getValue().toString().equals("TRUE")) &&                             //only start working a TDD if a defense is reporting a problem
                (TimedDefenseDiagnosis.find(dac.getExpandedName(), blackboard) == null)) {// avoid (at least for now) having multiple active TDDs on the BB (Confuses Believability)
                    startTimerIfNone(dac.getExpandedName());
            }
        }
        
      
    }

    
    private void startTimerIfNone(String asset) {
        if (!pendingTimedDiagnoses.contains(asset)) {
            if (logger.isDebugEnabled()) logger.debug("Adding an alarm");
            pendingTimedDiagnoses.add(asset);
            getAlarmService().addRealTimeAlarm(new DiagnosisReadyAlarm(asset));            
        }
        else
            if (logger.isDebugEnabled()) logger.debug("Already found an entry");
    }

    private class DiagnosisReadyAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        private String asset;
        private long startTime;
        
        public DiagnosisReadyAlarm (String asset) {
            detonate = timedDiagnosisKnob.getWaitTime() + System.currentTimeMillis();
            this.asset = asset;
            this.startTime = System.currentTimeMillis();
            if (logger.isInfoEnabled()) logger.info("DiagnosisAlarmCreated : " + detonate + " " + asset);
        }
        
        public long getExpirationTime () {
            return detonate;
        }
        
        public void expire () {
            if (!expired) {
                expired = true;
                if (logger.isInfoEnabled()) logger.info("TDD alarm expired for: " + asset);
                pendingTimedDiagnoses.remove(asset);
                openTransaction();
                Collection c1 = DefenseApplicabilityCondition.findCollection(asset, defenseConditionsSubscription);
                Collection c2 = new HashSet();
                Iterator iter = c1.iterator();
                TimedDefenseDiagnosis tdd;
                boolean hasPositiveDiagnosis = false;
                while (iter.hasNext()) {
                    DefenseApplicabilityCondition dac = (DefenseApplicabilityCondition) iter.next();
                    DefenseApplicabilityConditionSnapshot dacSnapshot = new DefenseApplicabilityConditionSnapshot(dac);
                    c2.add(dacSnapshot);
                    hasPositiveDiagnosis = hasPositiveDiagnosis || (dac.getValue().toString().equalsIgnoreCase("TRUE"));
                }
                if (hasPositiveDiagnosis) {
                    tdd = new TimedDefenseDiagnosis(asset, startTime, System.currentTimeMillis(), c2);
                    if (logger.isInfoEnabled()) logger.info(tdd.toString());
                    publishAdd(tdd);
                }
                closeTransaction();
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
    
    
    protected void setupSubscriptions() {
        
        initObjects();

        initDoneSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
           public boolean execute(Object o) {
               if ( o instanceof AllAssetsSeenCondition ) {
                   return true ;
               }
               return false ;
           }
        });
        
        thrashingSuppressionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
           public boolean execute(Object o) {
               if ( o instanceof ThrashingSuppressionApplicabilityCondition ) {
                   return true ;
               }
               return false ;
           }
        });
        
        timedDefenseDiagnosisSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof TimedDefenseDiagnosis) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        defenseConditionsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseApplicabilityCondition) {
                    return true ;
                }
                return false ;
            }
        }) ;

        pluginControlSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof TimedDiagnosisKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;

        
        defenseSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefaultDefenseTechSpec) {
                    return true ;
                }
                return false ;
            }
        }) ;

        //Publish my controller knob to the BB
        //getBlackboardService().publishAdd(timedDiagnosisKnob); //if this ever goes on the BB
        
    }


    /** Calculates the defense latency ASSUMES only one monitoring level per Defense */
    private long calculateDefenseLatency(Iterator i, long currentValue) {
        
        DefaultDefenseTechSpec def;
        long maxDefenseLatency = currentValue;
        long defLat;
        while (i.hasNext()) {
            
            def = (DefaultDefenseTechSpec)i.next();
            //Get the latency for the ONLY monitoring level. 
            //***** Will need to modify once there are multiple monitoring levels /
            Vector v = def.getMonitoringLevels();
            if (v.size() >= 1) {
                defLat = ((MonitoringLevel)v.get(0)).getDiagnosisLatency();
                maxDefenseLatency = ( maxDefenseLatency < defLat ) ? defLat : maxDefenseLatency;
            }        
        }        
        return maxDefenseLatency;
    }

 
  
    // THese should become static methods of DefenseCondition & be used throughout
    private boolean applicable(DefenseCondition dc) {
        return ((exists(dc)) && ((dc.getValue().equals(DefenseConstants.BOOL_TRUE.toString()))));
    }
    
    private boolean exists(DefenseCondition dc) {
        return (dc != null) ? true : false;
    }
    
}
