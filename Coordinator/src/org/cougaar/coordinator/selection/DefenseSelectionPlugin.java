/*
 * DefenseSelectionPlugin.java
 *
 * Created on July 8, 2003, 4:09 PM
 *
 * @author David Wells - OBJS
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

package org.cougaar.coordinator.selection;


import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
import org.cougaar.coordinator.DefenseApplicabilityCondition;
import org.cougaar.coordinator.DefenseCondition;
import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.coordinator.DefenseConstants;

import org.cougaar.coordinator.costBenefit.CostBenefitDiagnosis;
import org.cougaar.coordinator.monitoring.DefenseTimeoutCondition;
import org.cougaar.coordinator.test.defense.TestObservationServlet;
import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;


import java.util.Iterator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;


public class DefenseSelectionPlugin extends DeconflictionPluginBase
{  
  private String BOOL_TRUE = DefenseConstants.BOOL_TRUE.toString();
  private String BOOL_FALSE = DefenseConstants.BOOL_FALSE.toString();    
  private String DEF_ENABLED = DefenseConstants.DEF_ENABLED.toString();
  private String MON_ENABLED = DefenseConstants.MON_ENABLED.toString();
  private String DEF_DISABLED = DefenseConstants.DEF_DISABLED.toString();
  private String MON_DISABLED = DefenseConstants.MON_DISABLED.toString();
  
  private IncrementalSubscription costBenefitSubscription;
  private IncrementalSubscription defenseTimeoutSubscription;
  private IncrementalSubscription testServletSubscription;
  
  private Hashtable alarmTable = new Hashtable();
  
  private TestObservationServlet testServlet = null;
  
  long msglogDelay = 10000L; // how long to wait before disabling MsgLog - a default, but can be overridden by a parameter
  long msglogPatience = 30000L; // how long to let MsgLog try before giving up - a default, but can be overridden by a parameter
  long otherPatience = 300000L; // how long to let other defenses try before giving up - a default, but can be overridden by a parameter 
  
  public DefenseSelectionPlugin() {
  }

  
  public void load() {
      super.load();
      cancelTimer();
  }
  
  private void getPluginParams() {
      if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info("Coordinator not provided a MsgLog delay parameter - defaulting to 10 seconds.");

      // These really should be NAMED parameters to avoid confusion
      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
          msglogPatience = Long.parseLong((String)iter.next()) * 1000L;
           logger.debug("Setting msglogPatience = " + msglogPatience);
      }
      if (iter.hasNext()) {      
          otherPatience = Long.parseLong((String)iter.next()) * 1000L;
           logger.debug("Setting otherPatience = " + otherPatience);
      }
  }       
  
  
  public void setupSubscriptions() {
    
      if (logger.isDebugEnabled()) logger.debug("Loading DefenseSelectionPlugin");
     getPluginParams();
     initObjects(); 
      
     //Listen for changes to Conditions
     costBenefitSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof CostBenefitDiagnosis ) {
                return true ;
            }
            return false ;
        }
     });

     //Listen for changes to Conditions
     defenseTimeoutSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseTimeoutCondition ) {
                return true ;
            }
            return false ;
        }
     });

     testServletSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof TestObservationServlet ) {
                return true ;
            }
            return false ;
        }
     });
     
     //Unsure of use...
     if (blackboard.didRehydrate()) { //reset to null so it gets established again after rehydration -- IS THIS RIGHT??
         this.testServlet = null;
     }
     
  }

  
  //Create one condition and one of each type of operating mode
  private void initObjects() {
      // All Conditions & OpModes used by the Coordiator are published by the Defenses
      
  }
  

  public void execute() {

      Iterator iter;
      HashSet affectedAssets = new HashSet();
       
      
      if (testServlet == null) {
          iter = testServletSubscription.getAddedCollection().iterator();
          if (iter.hasNext()) {
              testServlet = (TestObservationServlet)iter.next(); 
              if (logger.isDebugEnabled()) logger.debug("Found TestObservationServlet");
           }
      }
      
      //********* Check for changes in Condition objects ************    

      iter = costBenefitSubscription.getAddedCollection().iterator();
      while (iter.hasNext()) {
          CostBenefitDiagnosis cbd = (CostBenefitDiagnosis)iter.next(); 
          if (logger.isDebugEnabled()) logger.debug("Saw new CBD: \n"+cbd.toString());
          if (testServlet != null) { 
              if (logger.isDebugEnabled()) logger.debug("Adding CBD to observation servlet");
              testServlet.addCostBenefitDiagnosis(cbd); 
              testServlet.updateCostBenefitStatus(cbd,"RUNNING");
          }
          boolean running = handleAsset(cbd);
          if (testServlet != null) { 
              if (!running) testServlet.updateCostBenefitStatus(cbd,"NOTHING TO DO");
          }
      }

      if (logger.isDebugEnabled()) logger.debug("Ready to Process Timeouts");
      iter = defenseTimeoutSubscription.getAddedCollection().iterator();
      // Mark the resolution of all the defenses that just reported back (for now it will just be one defense)
      while (iter.hasNext()) {
          DefenseTimeoutCondition dtc = (DefenseTimeoutCondition)iter.next();
                    
          publishRemove(dtc);    // we have the info we want, so get rid of it       
          DefenseApplicabilityConditionSnapshot dc = dtc.getDefenseCondition();
          if (logger.isDebugEnabled()) logger.debug(dc.getExpandedName());
          CostBenefitDiagnosis cbd = CostBenefitDiagnosis.find(dc.getExpandedName(), blackboard);  
          String outcome = null;
          if (dc.getValue().toString().equals("TRUE")) {
              outcome = "TIMED OUT";
              if (logger.isDebugEnabled()) logger.debug(dc.getDefenseName()+":"+dc.getExpandedName() + " has timed out w/o succeeding");
          }
          else {
              outcome = "SUCCEEDED";
              if (logger.isDebugEnabled()) logger.debug(dc.getDefenseName()+":"+dc.getExpandedName() + " has succeeded");
          }
          
          //mark what the defense did & DISABLE it
          CostBenefitDiagnosis.DefenseBenefit[] dbArray = cbd.getDefenses();
          CostBenefitDiagnosis.DefenseBenefit def;       
          for (int i=0; i<dbArray.length; i++) {
             def = dbArray[i];
             if (def.getDefense() != null && def.getDefense().getName().equalsIgnoreCase(dc.getDefenseName())) {
                 def.setOutcome(outcome);
                 DefenseApplicabilityConditionSnapshot dacs = def.getCondition();
                 dacs.setCompletionTime(dc.getCompletionTime());
                 setAction(dacs, DEF_DISABLED, MON_DISABLED, 0L, null);            
                 if (testServlet != null) { testServlet.updateDefenseStatus(cbd,def.getDefense().getName(),"DISABLED"); }
             } // re-enable MsgLog so messages about the next Defense (if any) can be delivered
             if (def.getDefense() != null && def.getDefense().getName().equalsIgnoreCase("MsgLog")) {
                 DefenseApplicabilityConditionSnapshot dacs = def.getCondition();
                 if (dacs != null) {
                     setAction(dacs, DEF_ENABLED, MON_ENABLED, 0L, null); // check that MsgLog actually exists    
                    if (testServlet != null) { testServlet.updateDefenseStatus(cbd,def.getDefense().getName(),"ENABLED"); }
                 }
             }          
          }

          // fixed the problem, so announce the result, clean up & exit
          if (logger.isDebugEnabled()) logger.debug("***** "+outcome+" *****");
          if (outcome.equals("SUCCEEDED")) {   // whatever we just tried worked (at least its own defense thinks it did)
              if (logger.isDebugEnabled()) logger.debug("Processing Success");
              String resolution = "COMPLETED - PROBLEM SOLVED";
              TimedDefenseDiagnosis tdd = TimedDefenseDiagnosis.find(cbd.getAssetName(), blackboard);
              if (tdd != null) {
                  tdd.setDisposition(resolution, System.currentTimeMillis());
                  publishChange(tdd); // let the TDD Plugin remove it
              }
              publishRemove(cbd);  // no longer need this since the problem is solved
              if (testServlet != null) testServlet.updateCostBenefitStatus(cbd, resolution); 
          }
          else { // the last thing we tried failed
              // see if there is another defense to try & if so, try it
              boolean stillTrying = handleAsset(cbd);

              // no more defenses
              if (!stillTrying) {
                  String resolution = "COMPLETED - NO MORE DEFENSES TO TRY";
                  TimedDefenseDiagnosis tdd = TimedDefenseDiagnosis.find(cbd.getAssetName(), blackboard);
                  if (tdd != null) {
                      tdd.setDisposition(resolution, System.currentTimeMillis());
                      publishChange(tdd); // let the TDD Plugin remove it
                  }
                  publishRemove(cbd);  // no longer need this since the problem can't be solved
                  if (testServlet != null) testServlet.updateCostBenefitStatus(cbd, resolution); 
             }
          }
      }
      if (logger.isDebugEnabled()) logger.debug("Done processing TimeOuts");
  }

 
  
  private boolean handleAsset(final CostBenefitDiagnosis cbd) {
      
      String assetName = cbd.getAssetName();
      if (logger.isDebugEnabled()) logger.debug(cbd.toString());

      CostBenefitDiagnosis.DefenseBenefit[] dbArray = cbd.getDefenses();
      if (logger.isDebugEnabled()) logger.debug(cbd.getAssetName()+" has "+dbArray.length+" diagnoses");
 
      // find the best defense based on expected benefit (> 0.0)
      double maxBenefit = -100000000.0;
      int index = -1;
      for (int i=0; i<dbArray.length; i++) {
          CostBenefitDiagnosis.DefenseBenefit db = (CostBenefitDiagnosis.DefenseBenefit)dbArray[i];
          DefenseApplicabilityConditionSnapshot dc = db.getCondition();
          if ((dc != null) &&                                                                   // the defense exists
              (dc.getValue().toString().equals("TRUE")) &&                                      // the defense wanted to run
              ((DefenseApplicabilityCondition.find(dc.getDefenseName(), dc.getExpandedName(), blackboard).getValue()).toString().equals("TRUE")) && // the defense STILL wants to run
              ((db.getOutcome() == null) || (!(db.getOutcome().equals("TIMED OUT")))) &&        // the defense hasn't already failed
              (db.getBenefit() > maxBenefit)) {                                                 // this defense is more beneficial than any seen so far
                  maxBenefit = db.getBenefit();
                  logger.info(db.getDefense().getName()+"="+db.getBenefit());
                  index = i;
          }
      }
      
     // enable the best defense (if there is one), disable the others - evntually may want to consider enabling more thanone at a time, but not yet
      if (index >= 0) {
          for (int i=0; i<dbArray.length; i++) {
              CostBenefitDiagnosis.DefenseBenefit db = (CostBenefitDiagnosis.DefenseBenefit)dbArray[i];
              DefenseApplicabilityConditionSnapshot dc = db.getCondition();
              if (dc == null) {
                  if (logger.isDebugEnabled()) logger.debug("DefenseCondition is null for CostBenefitDiagnosis.DefenseBenefit where defense="+db.getDefense().getName());
                  continue;
              }
              if (i == index) { // this is the chosen defense
                  String defenseName = db.getDefense().getName();
                  long patience;
                  if (dc.getTimeToBeDone() > 0L) {  // the defense says how long it wants
                      patience = dc.getTimeToBeDone();  
                  }
                  else {  // otherwise use the default value
                      patience = ((defenseName.equals("Msglog")) ? msglogPatience : otherPatience );  // a hack because the TechSpecs do not currently contain how long to allow a defense
                  }
                  setAction(dc, DEF_ENABLED, MON_ENABLED, patience, null);
                  if (testServlet != null) { 
                      testServlet.updateDefenseStatus(cbd,db.getDefense().getName(),"EXECUTING"); 
                      testServlet.setTimeout(cbd,db.getDefense().getName(),patience);
                  }
              }
              else {  // this defense was not chosen, so disable it
                  setAction(dc, DEF_DISABLED, MON_DISABLED, 0L, null);            
                  if (testServlet != null) { testServlet.updateDefenseStatus(cbd,db.getDefense().getName(),"DISABLED"); }
              }
          }
      }
      
      if (index >= 0) {
          return true; // still trying
      }
      else {
        return false;  // ran out of defenses to try
      }
     
  }
      


  
  private void setAction(DefenseApplicabilityConditionSnapshot dc, String defAction, String monAction, long patience, Precondition pred) {
      blackboard.publishAdd(new SelectedAction(dc.getDefenseName(), dc.getExpandedName(), defAction, monAction, pred));
      if (logger.isDebugEnabled()) logger.debug("setAction() created new SelectedAction: d="+dc.getDefenseName()+" expandedName="+dc.getExpandedName()+" "+defAction+" "+patience);
      if (patience > 0L) {
          ActionPatience ap = new ActionPatience(dc.getDefenseName(), dc.getAsset(), dc.getAssetType(), patience);
          ap.setUID(getUIDService().nextUID());
          blackboard.publishAdd(ap);
      }
  }

}
