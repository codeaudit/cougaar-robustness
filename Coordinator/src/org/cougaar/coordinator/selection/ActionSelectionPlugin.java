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
import org.cougaar.coordinator.monitoring.FailedActionWrapper;

import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.costBenefit.ActionEvaluation;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;

import org.cougaar.coordinator.monitoring.ActionTimeoutCondition;
import org.cougaar.coordinator.techspec.AssetID;


//import org.cougaar.coordinator.test.defense.TestObservationServlet;

import java.util.Iterator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.Action;


public class ActionSelectionPlugin extends DeconflictionPluginBase
{  
  private IncrementalSubscription costBenefitSubscription;
  private IncrementalSubscription failedActionSubscription;
  private IncrementalSubscription knobSubscription;
  private IncrementalSubscription testServletSubscription;
 
  private Hashtable alarmTable = new Hashtable();
  private ActionSelectionKnob knob;
  
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
      
     //Listen for new CostBenefitEvaluations
     costBenefitSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitEvaluation.pred);

     //Listen for Failed Actions
     failedActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(FailedActionWrapper.pred);

     //Access to the SelectionKnob
     knobSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ActionSelectionKnob.pred);

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

  
  //Create a new ActionSelectionKnob for external parameterization
  private void initObjects() {
      // Create the ActionSelectionKnob with default values
      knob = new ActionSelectionKnob();
      publishAdd(knob);
  }
 

  public void execute() {

      Iterator iter;    
  
      // Get the ActionSelectionKnob for current settings
        iter = knobSubscription.iterator();
        if (iter.hasNext()) 
        {        
            knob = (ActionSelectionKnob) iter.next();
        }      

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
          selectActions(cbe, knob);

/*          if (testServlet != null) { 
              if (!running) testServlet.updateCostBenefitStatus(cbd,"NOTHING TO DO");
          }
*/
      }

      //********* Process Actions that have Timed Out ***********
      if (logger.isDebugEnabled()) logger.debug("Ready to Process Timeouts");
      iter = failedActionSubscription.getAddedCollection().iterator();
      // Mark the resolution of all the Actions that just reported back (for now it will just be one Action)
      while (iter.hasNext()) {
          FailedActionWrapper faw = (FailedActionWrapper)iter.next();
          publishRemove(faw);    // we have the info we want, so get rid of it 
          Action action = faw.getAction();

          // get the CBE associated with the failed Action
          CostBenefitEvaluation cbe = findCostBenefitEvaluation(action.getAssetID());
          if (logger.isDebugEnabled()) logger.debug(action + " has timed out w/o succeeding");
          Object variantAttempted = action.getValue().getAction();
          VariantEvaluation variantAttemptedEvaluation = cbe.getActionEvaluation(action).getVariantEvaluation(variantAttempted);
          variantAttemptedEvaluation.setTried();
          selectActions(cbe, knob);  // try to find a new action
      }

/*          
          //mark what the defense did & DISABLE it
          CostBenefitDiagnosis.ActionBenefit[] dbArray = cbe.getActions();
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
/*
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
*/
      if (logger.isDebugEnabled()) logger.debug("Done processing TimeOuts");


  }


  
  private void selectActions(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {
    // compute the score for each Action/Variant given that it may force some other action to be changed
    // for now, this just copies the original CB values
       //scoreActionsInCombination(cbe, knob);
    // at this point, we may want to make a choice between several selection policies based on the Knob
    // for now, we just select the best score
    boolean done = false;
    int index = 0;
    while (!done) {
        SelectedAction thisAction = selectBest(cbe, knob); 
        publishAdd(thisAction);
        index++;
        if ((thisAction.getActionEvaluation().actionType().equals("Corrective"))
         || (outOfResources())
         || (index == knob.getMaxActions()))
                done = true;
    }
  }


  private SelectedAction selectBest(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {
        ActionEvaluation bestActionEvaluation = null;
        VariantEvaluation bestVariantEvaluation = null;
        double bestBenefit = -1000000.0;
        SelectedAction sa =  null;
        // iterate thru all Actions & Variants
        Collection actionEvaluations = cbe.getActionEvaluations().values();
        Iterator actionIter = actionEvaluations.iterator();
        while (actionIter.hasNext()) {
            ActionEvaluation thisActionEvaluation = (ActionEvaluation) actionIter.next();
            Action thisAction = thisActionEvaluation.getAction();
            Collection variantEvaluations = thisActionEvaluation.getVariantEvaluations().values();
            Iterator variantIter = variantEvaluations.iterator();
            while (variantIter.hasNext()) {
                VariantEvaluation thisVariantEvaluation = (VariantEvaluation) variantIter.next();
                if ((thisVariantEvaluation.getPredictedBenefit() > bestBenefit)
                  && (!thisVariantEvaluation.triedP())) {
                    bestBenefit = thisVariantEvaluation.getPredictedBenefit();
                    bestActionEvaluation = thisActionEvaluation;    
                    bestVariantEvaluation = thisVariantEvaluation;
                }
            }
        }
        if (bestBenefit > 0) {
            HashSet permittedVariants = new HashSet();
            permittedVariants.add(bestVariantEvaluation);
            sa = new SelectedAction(cbe.getAssetID(), bestActionEvaluation, permittedVariants);
        }
        
        return sa;
    }


    private boolean outOfResources() {
        // tracks resources allocated to Actions so we can tell whether we can choose more actions
        return false;
    }


/*  private void scoreActionsInCombination(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {
        // For now, we only consider the direct effect of an action when choosing
        // eventually, we need to also consider what it precludes   \
        // we still enforce deconfliction, just don't consider it when choosing
        Collection actionEvaluations = cbe.getActionEvaluations().values();
        Iterator iter = actionEvaluations.iterator();
        return;
  }*/


/*  NOT CURRENTLY BEING USED - MAYBE NEVER WILL BE
  private VariantEvaluation scoreVariants(ActionEvaluation actionEval) {
    Collection variantEvals = actionEval.getVariantEvaluations().values();
    Iterator iter = variantEvals.iterator();
    Collection offeredVariants = actionEval.getAction().getValuesOffered();
    double bestScore = -100000000.0;
    VariantEvaluation bestVariant = null;
    while (iter.hasNext()) {
        VariantEvaluation thisVariantEval = (VariantEvaluation) iter.next();
        double score = 1.0; //FIX
        // double score = thisVariantEval.getPredictedBenefit()/thisVariantEval.getPredictedCost().getAggregateCost();
        thisVariantEval.setSelectionScore(score);
        if ((offeredVariants.contains(thisVariantEval)) && (thisVariantEval.triedP() == false)) { // the variant is avialbe & has not yet been tried
            if (score > bestScore) {
                bestScore = score;
                bestVariant = thisVariantEval;
            }
        }
    }    
    return bestVariant;
  }

  private void scoreActions(CostBenefitEvaluation cbe) {
    Collection actionEvals = cbe.getActionEvaluations().values();
    Iterator iter = actionEvals.iterator();
    while (iter.hasNext()) {
        ActionEvaluation thisActionEval = (ActionEvaluation) iter.next();
        thisActionEval.setBestAvailableVariant(scoreVariants(thisActionEval));
    }
  }
 */ 
}
