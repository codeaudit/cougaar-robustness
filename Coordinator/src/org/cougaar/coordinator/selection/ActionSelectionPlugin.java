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

import org.cougaar.coordinator.*;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.coordinator.activation.ActionPatience;

import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.costBenefit.ActionEvaluation;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;


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
  private IncrementalSubscription actionPatienceSubscription;
  private IncrementalSubscription knobSubscription;
  private IncrementalSubscription testServletSubscription;

   private IncrementalSubscription diagnosesWrapperSubscription;  // FIX

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
      getPluginParams();
      initObjects(); 
      cancelTimer();
  }
  
  private void getPluginParams() {
      if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info("Coordinator not provided a MsgLog delay parameter - defaulting to 10 seconds.");

      // FIX - These really should come from TEchSpecs
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

     super.setupSubscriptions();

     //Access to the SelectionKnob
     knobSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ActionSelectionKnob.pred);

     //Listen for new CostBenefitEvaluations
     costBenefitSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitEvaluation.pred);

     //Listen for Failed Actions
     actionPatienceSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(ActionPatience.pred);

     //Unsure of use...
     if (blackboard.didRehydrate()) { //reset to null so it gets established again after rehydration -- IS THIS RIGHT??
         //this.testServlet = null;
     }
     
  }

  
  //Create a new ActionSelectionKnob for external parameterization
  private void initObjects() {
      // Create the ActionSelectionKnob with default values
      knob = new ActionSelectionKnob();
      openTransaction();
      publishAdd(knob);
      closeTransaction();
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

          selectActions(cbe, knob);

      }

      //********* Process Actions that have Responded ***********
      if (logger.isDebugEnabled()) logger.debug("Ready to Process Action Responses");
      iter = actionPatienceSubscription.getChangedCollection().iterator();
      // Mark the resolution of all the Actions that just reported back (for now it will just be one Action)
      while (iter.hasNext()) {
          ActionPatience ap = (ActionPatience)iter.next();
          publishRemove(ap);    // we have the info we want, so get rid of it 
          Action action = ap.getAction();

          // get the CBE associated with the Action
          CostBenefitEvaluation cbe = findCostBenefitEvaluation(action.getAssetID());
          if (logger.isDebugEnabled()) logger.debug(action + " has " + ap.getResult());
          if (ap.getResult().equals(Action.COMPLETED)) {
              if (logger.isDebugEnabled()) logger.debug("Nothing more to do");
          }
          else {
              Object variantAttempted = action.getValue().getAction();
              VariantEvaluation variantAttemptedEvaluation = cbe.getActionEvaluation(action).getVariantEvaluation(variantAttempted);
              variantAttemptedEvaluation.setTried();
              selectActions(cbe, knob);  // try to find a new action
         }      }

      if (logger.isDebugEnabled()) logger.debug("Done processing TimeOuts");

  }

/* original implementation of selectActions
  private void selectActions(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {

    boolean done = false;
    int index = 0;
    while (!done) {
        SelectedAction thisAction = selectBest(cbe, knob); 
        if (logger.isDebugEnabled()) 
	    logger.debug("SelectAction: thisAction="+((thisAction != null)?thisAction.toString():null));
        if (thisAction != null) {
            publishAdd(thisAction);
            index++;
            done = true; // done because can't find anything useful to do
            if ((thisAction.getActionEvaluation().getAction().getTechSpec().getActionType() == ActionTechSpecInterface.CORRECTIVE_ACTIONTYPE) // Because we try only one corrective action at a time
		|| (outOfResources())
		|| (index == knob.getMaxActions()))
		done = true; 
        } 
    }
  }
*/
  
  private void selectActions(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {

    boolean done = false;
    int index = 0;
    while (!done) {
        SelectedAction thisAction = selectBest(cbe, knob); 
        if (thisAction != null) {
	    if (logger.isDebugEnabled()) 
		logger.debug("SelectAction: thisAction="+thisAction.toString());
            publishAdd(thisAction);
            index++;
            Action a = thisAction.getActionEvaluation().getAction();
	    ActionTechSpecInterface ats = actionTechSpecService.getActionTechSpec(a.getClass().getName());
	    if (ats == null) {
		if (logger.isErrorEnabled()) 
		    logger.error("Cannot find ActionTechSpec for "+a.getClass().getName());
	    }
	    if (outOfResources() || (index >= knob.getMaxActions())) {
		done = true;
	    } else if (ats != null && 
                      // Because we try only one corrective action at a time
		      ats.getActionType() == ActionTechSpecInterface.CORRECTIVE_ACTIONTYPE) {
		done = true;
	    } else {
		done = false;
	    }
	} else {
	    // exit on first null result from SelectBest
	    done = true; // done because can't find anything useful to do
	}
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
            bestVariantEvaluation.setTried(); //sjf
            permittedVariants.add(bestVariantEvaluation); 
            sa = new SelectedAction(bestActionEvaluation, permittedVariants, Math.round(knob.getPatienceFactor()*bestVariantEvaluation.getExpectedTransitionTime()));
        }
        
        return sa;
    }


    private boolean outOfResources() {
        // tracks resources allocated to Actions so we can tell whether we can choose more actions
        return false;
    }

}
