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
  private IncrementalSubscription actionsWrapperSubscription;

  private Hashtable alarmTable = new Hashtable();
  private ActionSelectionKnob knob;
  
  //private TestObservationServlet testServlet = null;
  
  // Defaults for the Knob - may be overridden by parameters or the Knob may be set by policy
  int maxActions = 1;
  double patienceFactor = 1.5; // how much extra time to give an Action to complete before giving up

  
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

    maxActions = DEFAULT_MAX_ACTIONS;
    patienceFactor = DEFAULT_PATIENCE_FACTOR;
    for (Iterator i = getParameters().iterator(); i.hasNext();) {
      String param = (String) i.next();
      maxActions = parseIntParameter(param, MAX_ACTIONS_PREFIX, maxActions);
      patienceFactor = parseDoubleParameter(param, PATIENCE_FACTOR_PREFIX, patienceFactor);
    }
      /*

*/
  }       
  
 
  public void setupSubscriptions() {

     super.setupSubscriptions();

     //Access to the SelectionKnob
     knobSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ActionSelectionKnob.pred);

     //Listen for new CostBenefitEvaluations
     costBenefitSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitEvaluation.pred);

     //Listen for Failed Actions
     actionPatienceSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(ActionPatience.pred);

     actionsWrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof ActionsWrapper) {
                return true ;
            }
            return false ;
        }
     }) ;

     //Unsure of use...
     if (blackboard.didRehydrate()) { //reset to null so it gets established again after rehydration -- IS THIS RIGHT??
         //this.testServlet = null;
     }
     
  }

  
  //Create a new ActionSelectionKnob for external parameterization
  private void initObjects() {
      // Create the ActionSelectionKnob with default values
      knob = new ActionSelectionKnob(maxActions, patienceFactor);
      if (logger.isDebugEnabled()) logger.debug("Created ActionSelectionKnob with maxActions="+maxActions+", patienceFactor="+patienceFactor);
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

     // Check any changed Actions to see if the change is a change to offeredActions
     //    and there is a currently open CBE for the asset
     //    and there are currently fewer then maxActions actions permitted on he asset
     iter = actionsWrapperSubscription.iterator();
     while (iter.hasNext()) {
        ActionsWrapper aw = (ActionsWrapper) iter.next();
        Action action = aw.getAction();
        CostBenefitEvaluation thisCBE = findCostBenefitEvaluation(action.getAssetID());
        if (thisCBE != null) {
            if (thisCBE.numOpenActions() < knob.getMaxActions()) {
                selectActions(thisCBE, knob);
            }
        }        
     }


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
              if (logger.isDebugEnabled()) logger.debug("Action succeeded - Nothing more to do.  Completed action no longer permitted w/o re-authorization.");
              Object variantAttempted = action.getValue().getAction();
              Set newPermittedValues = action.getPermittedValues();
              boolean removalP = newPermittedValues.remove(variantAttempted);
              if (logger.isDebugEnabled()) logger.debug("Removed: " + variantAttempted.toString() + " successfully: " + removalP);
              try {
                    action.setPermittedValues(newPermittedValues);
                    publishChange(action.getWrapper());
              } catch (IllegalValueException e) { 
                    // can't happen but must be caught
              }
          }
          else {
              if (action.getValue() != null) { // some action was tried & it did not help - mark it as "tried" & remove it from the permittedValues
                  Object variantAttempted = action.getValue().getAction();
                  VariantEvaluation variantAttemptedEvaluation = cbe.getActionEvaluation(action).getVariantEvaluation(variantAttempted);
                  variantAttemptedEvaluation.setTried();
                  Set newPermittedValues = action.getPermittedValues();
                  boolean removalP = newPermittedValues.remove(variantAttempted);
                  if (logger.isDebugEnabled()) logger.debug("Removed: " + variantAttempted.toString() + " successfully: " + removalP);
                  try {
                        action.setPermittedValues(newPermittedValues);
                        publishChange(action.getWrapper());
                  } catch (IllegalValueException e) { 
                        // can't happen but must be caught
                  }
              }      
              else { // the actuator took no action - what should we do here? 
              }
              selectActions(cbe, knob);  // try to find a new action
           }
      }

      if (logger.isDebugEnabled()) logger.debug("Done processing TimeOuts");

  }

  
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
    cbe.setNumOpenActions(index);
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
                  && (!thisVariantEvaluation.triedP())
                  && (thisAction.getValuesOffered().contains(thisVariantEvaluation.getVariantName()))) {
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

  protected static final String MAX_ACTIONS_PREFIX = "maxActions=";

  protected static final String PATIENCE_FACTOR_PREFIX = "patienceFactor=";

  protected static final int DEFAULT_MAX_ACTIONS = 1;

  protected static final double DEFAULT_PATIENCE_FACTOR = 1.5;

  protected int parseIntParameter(String param, String prefix, int dflt) {
    if (param.startsWith(prefix)) {
        return Integer.parseInt(param.substring(prefix.length()));
        }
    else return dflt;
  }
      
  protected double parseDoubleParameter(String param, String prefix, double dflt) {
    if (param.startsWith(prefix)) {
        return Double.parseDouble(param.substring(prefix.length()));
        }
    else return dflt;
  }


}
