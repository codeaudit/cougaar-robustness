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


import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.coordinator.monitoring.FailedAction;

import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.costBenefit.ActionEvaluation;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;

import org.cougaar.coordinator.monitoring.ActionTimeoutCondition;
import org.cougaar.coordinator.techspec.AssetID;


//import org.cougaar.coordinator.test.defense.TestObservationServlet;

import java.util.Iterator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;


public class ActionSelectionPlugin extends DeconflictionPluginBase
{  
/*
  private String BOOL_TRUE = DefenseConstants.BOOL_TRUE.toString();
  private String BOOL_FALSE = DefenseConstants.BOOL_FALSE.toString();    
  private String DEF_ENABLED = DefenseConstants.DEF_ENABLED.toString();
  private String MON_ENABLED = DefenseConstants.MON_ENABLED.toString();
  private String DEF_DISABLED = DefenseConstants.DEF_DISABLED.toString();
  private String MON_DISABLED = DefenseConstants.MON_DISABLED.toString();

  */
  private IncrementalSubscription costBenefitSubscription;
  private IncrementalSubscription failedActionSubscription;
  private IncrementalSubscription selectionKnobSubscription;
  private IncrementalSubscription testServletSubscription;
 
  private Hashtable alarmTable = new Hashtable();
  
  //private TestObservationServlet testServlet = null;
  
  long msglogDelay = 10000L; // how long to wait before disabling MsgLog - a default, but can be overridden by a parameter
  long msglogPatience = 30000L; // how long to let MsgLog try before giving up - a default, but can be overridden by a parameter
  long otherPatience = 300000L; // how long to let other defenses try before giving up - a default, but can be overridden by a parameter 
  
  public ActionSelectionPlugin() {
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
     costBenefitSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitEvaluation.pred);

     //Listen for Failed Actions
     failedActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(FailedAction.pred);

     //Access to the SelectionKnob
     selectionKnobSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ActionSelectionKnob.pred);

/*     // Does this really belong here?  Shouldn't the Servlet subscribe to the BB and pick up whatever it needs itself?
     testServletSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof TestObservationServlet ) {
                return true ;
            }
            return false ;
        }
     });
*/     
     //Unsure of use...
     if (blackboard.didRehydrate()) { //reset to null so it gets established again after rehydration -- IS THIS RIGHT??
         //this.testServlet = null;
     }
     
  }

  
  //Create one condition and one of each type of operating mode
  private void initObjects() {
      // All Conditions & OpModes used by the Coordiator are published by the Defenses
      
  }
 

  public void execute() {

      Iterator iter;    
  
      // Get the ActionSelectionKnob for current settings
      ActionSelectionKnob knob = (ActionSelectionKnob) selectionKnobSubscription.iterator().next();

/*      
      if (testServlet == null) {
          iter = testServletSubscription.getAddedCollection().iterator();
          if (iter.hasNext()) {
              testServlet = (TestObservationServlet)iter.next(); 
              if (logger.isDebugEnabled()) logger.debug("Found TestObservationServlet");
           }
      }
*/      
      //********* Process new CostBenefit objects ************    

      iter = costBenefitSubscription.getAddedCollection().iterator();
      while (iter.hasNext()) {
          CostBenefitEvaluation cbe = (CostBenefitEvaluation)iter.next(); 
          if (logger.isDebugEnabled()) logger.debug("Saw new CBD: \n"+cbe.toString());
/*          if (testServlet != null) { 
              if (logger.isDebugEnabled()) logger.debug("Adding CBD to observation servlet");
              testServlet.addCostBenefitDiagnosis(cbd); 
              testServlet.updateCostBenefitStatus(cbd,"RUNNING");
          }
*/
          boolean running = selectAction(cbe, knob.getRankingPolicy(), knob.getMultiplicityPolicy());
/*          if (testServlet != null) { 
              if (!running) testServlet.updateCostBenefitStatus(cbd,"NOTHING TO DO");
          }
*/
      }

/*  NOT CONVERTED YET
      //********* Process Actions that have Timed Out ***********
      if (logger.isDebugEnabled()) logger.debug("Ready to Process Timeouts");
      iter = defenseTimeoutSubscription.getAddedCollection().iterator();
      // Mark the resolution of all the defenses that just reported back (for now it will just be one defense)
      while (iter.hasNext()) {
          ActionTimeoutCondition atc = (ActionTimeoutCondition)iter.next();
                    
          publishRemove(atc);    // we have the info we want, so get rid of it 
          String assetName = atc.getExpandedName();
          String actionName = atc.getActionName();
          CostBenefitDiagnosis cbd = findCostBenefitDiagnosis(assetName);  
          if (logger.isDebugEnabled()) logger.debug(assetName);
          String outcome = null;
          if (atc.getResult().toString().equals("TRUE")) {
              outcome = "TIMED OUT";
              if (logger.isDebugEnabled()) logger.debug(actionName+":"+assetName + " has timed out w/o succeeding");
          }
          else {
              outcome = "SUCCEEDED";
              if (logger.isDebugEnabled()) logger.debug(actionName+":"+assetName + " has succeeded");
          }
          
          //mark what the defense did & DISABLE it
          CostBenefitDiagnosis.ActionBenefit[] dbArray = cbd.getActions();
          CostBenefitDiagnosis.ActionBenefit actionBenefit;       
          for (int i=0; i<dbArray.length; i++) {
             actionBenefit = dbArray[i];
             if (actionBenefit.getAction() != null && actionBenefit.getAction().getName().equalsIgnoreCase(actionName)) {
                 actionBenefit.setOutcome(outcome);
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

*/
  }

  private void rankVariants(ActionEvaluation actionEval, String rankingPolicy) {
    return;
  }

  private void rankActions(CostBenefitEvaluation cbe, String rankingPolicy) {
    Collection actionEvals = cbe.getActionEvaluations().values();
    Iterator iter = actionEvals.iterator();
    while (iter.hasNext()) {
        ActionEvaluation thisActionEval = (ActionEvaluation) iter.next();
        rankVariants(thisActionEval, rankingPolicy);
    }
  }
 
  
  private boolean selectAction(CostBenefitEvaluation cbe, String rankingPolicy, String multiplicityPolicy) {

    rankActions(cbe, rankingPolicy);
      
    AssetID assetID = cbe.getAssetID(); 
      if (logger.isDebugEnabled()) logger.debug(cbe.toString());

      Hashtable actions = cbe.getActionEvaluations();
//      CostBenefitEvaluation.ActionBenefit[] dbArray = new ActionBenefit[actions.size()];
      if (logger.isDebugEnabled()) logger.debug(cbe.getAssetName()+" has "+actions.size()+" Possible Actions, using policy "+rankingPolicy+":"+multiplicityPolicy);
 
      // find the best defense based on expected benefit (> 0.0)
      double maxBenefit = -100000000.0;
      Iterator iter = actions.values().iterator();

  return true;
  }
   //   while (iter.hasNext()) {
        
/*      int index = -1;
      for (int i=0; i<dbArray.length; i++) {
          CostBenefitEvaluation.ActionBenefit db = (CostBenefitEvaluation.ActionBenefit)dbArray[i];
          if (db.getBenefit() > maxBenefit) {    // this defense is more beneficial than any seen so far
                  maxBenefit = db.getBenefit();
                  logger.info(db.getAction().getName()+"="+db.getBenefit());
                  index = i;
          }
      }
      
     // enable the best defense (if there is one), disable the others - evntually may want to consider enabling more thanone at a time, but not yet
      if (index >= 0) {
          for (int i=0; i<dbArray.length; i++) {
              CostBenefitEvaluation.ActionBenefit db = (CostBenefitDiagnosis.ActionBenefit)dbArray[i];
              String thisAction = db.getAction().getName();
              if (i == index) { // this is the chosen defense
                  long patience = ((thisAction.equals("Msglog")) ? msglogPatience : otherPatience );  // a hack because the TechSpecs do not currently contain how long to allow a defense
                  setAction(thisAction, assetName, assetType, DEF_ENABLED, MON_ENABLED, patience, null);
                  if (testServlet != null) { 
                      testServlet.updateDefenseStatus(cbd, thisAction, "EXECUTING"); 
                      testServlet.setTimeout(cbd, thisAction, patience);
                  }

              }
              else {  // this defense was not chosen, so disable it
                  setAction(thisAction, assetName, assetType, DEF_DISABLED, MON_DISABLED, 0L, null);            
                  //if (testServlet != null) { testServlet.updateDefenseStatus(cbd, thisAction, "DISABLED"); }
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
      


  
  private void setAction(String actionName, String assetName, String assetType, String defAction, String monAction, long patience, Precondition pred) {
      blackboard.publishAdd(new SelectedAction(actionName, assetName, defAction, monAction, pred));
      if (logger.isDebugEnabled()) logger.debug("setAction() created new SelectedAction: d="+actionName+" expandedName="+assetName+" "+defAction+" "+patience);
      if (patience > 0L) {
          ActionPatience ap = new ActionPatience(actionName, assetName, assetType, patience);
          ap.setUID(getUIDService().nextUID());
          blackboard.publishAdd(ap);
      }
  }
*/
}
