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
import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.sensors.load.OutsideLoadDiagnosis;


public class ActionSelectionPlugin extends DeconflictionPluginBase
{  
  private IncrementalSubscription costBenefitSubscription;
  private IncrementalSubscription actionPatienceSubscription;
  private IncrementalSubscription knobSubscription;
  private IncrementalSubscription testServletSubscription;
  private IncrementalSubscription actionsWrapperSubscription;

  private Hashtable alarmTable = new Hashtable();
  private ActionSelectionKnob knob;

  private static final boolean leashOnRestart;
  
  static
  {
     String s = "org.cougaar.coordinator.leashOnRestart";
     leashOnRestart = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }
  
  // Defaults for the Knob - may be overridden by parameters or the Knob may be set by policy
  int maxActions = 1;
  double patienceFactor = 1.5; // how much extra time to give an Action to complete before giving up

  
  public ActionSelectionPlugin() {
  }

  
  public void load() {
      super.load();
      getPluginParams();
      if (!blackboard.didRehydrate()) {
        initObjects(); 
      }
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
  
        // leash the defenses on rehydration if leashOnRestart is TRUE, otherwise leave as-is - dlw - 9/7/04
        if (blackboard.didRehydrate() && leashOnRestart) {
            iter = knobSubscription.iterator();    
            if (iter.hasNext()) {
                ActionSelectionKnob knob = (ActionSelectionKnob) iter.next();
                knob.leash();
                blackboard.publishChange(knob);
                if (logger.isDebugEnabled()) 
                    logger.debug("rehydrate detected  && leashOnRestart==true - enabling ThrashingSuppression");       
            }
        }
      
        // Get the ActionSelectionKnob for current settings
        iter = knobSubscription.iterator();
        if (iter.hasNext()) 
        {        
            knob = (ActionSelectionKnob) iter.next();
        }

      // Use the Control Knob to determine if Defenses are Leashed & if so, do nothing more
        if (knob.isEnclaveLeashed()) {
            if (logger.isInfoEnabled()) logger.info("Enclave Defenses are LEASHED - no actions will be selected");
            return;
        }

      //********* Process Actions that have Responded ***********
      if (logger.isDetailEnabled()) logger.detail("Ready to Process Action Responses");
      iter = actionPatienceSubscription.getChangedCollection().iterator();
      // Mark the resolution of all the Actions that just reported back (for now it will just be one Action)
      while (iter.hasNext()) {
          ActionPatience ap = (ActionPatience)iter.next();
          publishRemove(ap);    // we have the info we want, so get rid of it 
          removeActionPatience(ap);
          Action action = ap.getAction();
          if (logger.isDebugEnabled()) logger.debug("Removed this AP: " + ap.toString());
          // get the CBE associated with the Action
          CostBenefitEvaluation cbe = ap.getCBE();   ///findCostBenefitEvaluation(action.getAssetID());
          ActionEvaluation ae = cbe.getActionEvaluation(action);
          if (logger.isDebugEnabled()) logger.debug(action.dump() + " has " + ap.getResult());
          if (action.getValue() == null) {
              Iterator iter2 = action.getPermittedValues().iterator();
              while (iter2.hasNext()) {
                  Object variantNotTried = iter2.next();
                  if (logger.isInfoEnabled()) logger.info("For: " + action.getAssetID().toString() + ", " + variantNotTried.toString() + " timed out w/o being tried - Try something else.  Timed out action no longer permitted w/o re-authorization.");
                  VariantEvaluation ve = ae.getVariantEvaluation(variantNotTried);
                  ve.setFailed();
              }
              selectActions(cbe, knob);  // try to find a new action
          }
          else {
              Object variantAttempted = action.getValue().getAction();
              VariantEvaluation ve = ae.getVariantEvaluation(variantAttempted);
              if (ap.getResult().equals(Action.FAILED)) {
                  if (logger.isInfoEnabled()) logger.info("For: " + action.getAssetID().toString() + ", " + variantAttempted.toString() + " failed - Try something else.  Failed action no longer permitted w/o re-authorization.");
                  ve.setFailed();
                  selectActions(cbe, knob);  // try to find a new action
                  publishAdd(new RetractedActions(action, cbe));
              }
              else if (ap.getResult().equals(Action.COMPLETED)) {
                  if (logger.isInfoEnabled()) logger.info("For: " + action.getAssetID().toString() + ", " + variantAttempted.toString() + " succeeded - Nothing more to do.  Completed action no longer permitted w/o re-authorization.");
                  publishAdd(new RetractedActions(action, cbe));
                  if (logger.isInfoEnabled()) logger.info("Sucess in processing: " + cbe.dumpAvailableVariants());
                  publishRemove(cbe);
              }
              else if (ap.getResult().equals(Action.ACTIVE)) {
                  if (logger.isInfoEnabled()) logger.info("For: " + action.getAssetID().toString() + ", " + variantAttempted.toString() + " is active - Other actions may be pending.");
                  //publishAdd(new RetractedActions(action));
                  if (logger.isInfoEnabled()) logger.info("Success in processing: " + cbe.dumpAvailableVariants());
                  publishRemove(cbe);
              }
              else logger.error("Unhandled Action result: " + ap.getResult().toString());
          }
      }

      if (logger.isDetailEnabled()) logger.detail("Done processing Action Responses");
      
      //********* Process new CostBenefit objects ************    

      iter = costBenefitSubscription.getAddedCollection().iterator();
      while (iter.hasNext()) {
          CostBenefitEvaluation cbe = (CostBenefitEvaluation)iter.next(); 
          if (logger.isDebugEnabled()) logger.debug("Saw new CBD: \n"+cbe.toString());

          selectActions(cbe, knob);

      }
  }


  private void rankAvailableActions(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {
    
    Iterator iter = cbe.getActionEvaluations().values().iterator();
    SortedSet orderedActions = new TreeSet();
    while (iter.hasNext()) {
        ActionEvaluation ae = (ActionEvaluation)iter.next();
        Action action = ae.getAction();
        SortedSet availableActions = new TreeSet();
        Iterator iter2 = action.getValuesOffered().iterator();
        while (iter2.hasNext()) {
            String variantName = (String) iter2.next();
            availableActions.add(ae.getVariantEvaluation(variantName));
        }
        ae.setOrderedAvailableVariants(availableActions);
        if (!availableActions.isEmpty()) orderedActions.add(cbe.getActionEvaluation(action));
    }
    cbe.setOrderedEvaluations(orderedActions);
    if (logger.isDebugEnabled()) logger.debug(cbe.toString());
  }

  private double getCurrentEnclaveResources() {
    return 100.0;  // for now
  }

  private double getCostForAction(Action thisAction) { 
    return 1.0; // for now
  }
  
  private void selectActions(CostBenefitEvaluation cbe, ActionSelectionKnob knob) {

    rankAvailableActions(cbe, knob);
    
    Iterator iter;

    boolean done = false;
    double resourcePercentageRemaining = getCurrentEnclaveResources();
    Set alreadyActiveActions = findActiveActions(cbe);

    // deduct the cost of actions that we can't terminate
    iter = alreadyActiveActions.iterator();
    while (iter.hasNext()) {
        Action thisAction = (Action)iter.next();
        double thisCost = getCostForAction(thisAction);
        resourcePercentageRemaining = resourcePercentageRemaining - thisCost;
    }

    Set alreadySelectedVariants = new HashSet();

    iter = cbe.getOrderedEvaluations().iterator();
    if (logger.isInfoEnabled()) logger.info(cbe.dumpAvailableVariants());
    while (iter.hasNext()) {
        ActionEvaluation thisActionEvaluation = (ActionEvaluation) iter.next();
        Action thisAction = thisActionEvaluation.getAction();
        Set permittedVariants = new HashSet();  
        Iterator iter3 = thisActionEvaluation.getOrderedAvaliableVariants().iterator();
        boolean pickedSomeVariant = false;
        while (iter3.hasNext() && !pickedSomeVariant) {
            VariantEvaluation proposedVariant = (VariantEvaluation)iter3.next();
            if (logger.isDebugEnabled()) logger.debug("Considering conflict for Action: " +thisActionEvaluation.getAction().getClass().getName() + "Variant: " + proposedVariant.toString());
            if (logger.isDebugEnabled()) logger.debug(proposedVariant + " doesNotConflict: " + thisActionEvaluation.doesNotConflict(proposedVariant, alreadyActiveActions, alreadySelectedVariants));
            if (thisActionEvaluation.doesNotConflict(proposedVariant, alreadyActiveActions, alreadySelectedVariants)) {
                if ((proposedVariant.getPredictedBenefit() > 0.0) || (thisActionEvaluation.mustSelectOne())) {
                    pickedSomeVariant = true;
                    alreadySelectedVariants.add(proposedVariant);
                    proposedVariant.setChosen();
                    if (logger.isInfoEnabled()) logger.info("Selected: " + proposedVariant.toString() + "for: " + thisAction.getAssetID().toString());
                    if (eventService.isEventEnabled()) eventService.event(agentId + " selected " + thisAction.getClass().getName() + ":" + proposedVariant + " for " + thisAction.getAssetID().toString());
                    if ((!thisAction.getPermittedValues().contains(proposedVariant.getVariantName()))
                            && (thisAction.getValue() == null  
                                || !thisAction.getValue().getAction().equals(proposedVariant.getVariantName())
                                || !thisAction.getValue().isActive())) {
                        permittedVariants.add(proposedVariant);
                        publishAdd(new SelectedAction(thisAction, permittedVariants, Math.round(knob.getPatienceFactor()*proposedVariant.getExpectedTransitionTime()), cbe));
                        if (logger.isInfoEnabled()) logger.info("Enabling: " + proposedVariant.toString() + "for: " + thisAction.getAssetID().toString());
                    }


                    // special code to activate/deactivate RMIAction in tandem with SecurityDefenseAction - a special case for ACUC #2B
                    if (thisAction.getClass().getName().equals("org.cougaar.core.security.coordinator.Security_Defense_Setting")) {
                        Action rmiAction = findAction(thisAction.getAssetID(), "org.cougaar.robustness.dos.coordinator.RMIAction").getAction();
                        if (proposedVariant.getVariantName().equals("High")) {
                            String rmiVariantName = "Disabled";
                            VariantEvaluation rmiVariant = getRmiVariantEvaluation(cbe, rmiAction, rmiVariantName);
                            if (rmiVariant != null && rmiAction != null && rmiAction.getValuesOffered().contains(rmiVariantName)) {
                                if (logger.isInfoEnabled()) logger.info("Selected: " + rmiVariant.toString() + "for: " + rmiAction.getAssetID().toString());
                                if (eventService.isEventEnabled()) eventService.event(agentId + " selected " + rmiAction.getClass().getName() + ":" + rmiVariant.getVariantName() + " for " + rmiAction.getAssetID());
                                Set rmiPermittedVariants = new HashSet();  
                                if ((!rmiAction.getPermittedValues().contains(rmiVariant.getVariantName()))
                                        && (thisAction.getValue() == null  
                                            || !thisAction.getValue().getAction().equals(proposedVariant.getVariantName())
                                            || !thisAction.getValue().isActive())) {
                                    permittedVariants.add(proposedVariant);
                                    publishAdd(new SelectedAction(thisAction, permittedVariants, Math.round(knob.getPatienceFactor()*proposedVariant.getExpectedTransitionTime()), cbe));
                                    if (logger.isInfoEnabled()) logger.info("Enabling: " + proposedVariant.toString() + "for: " + thisAction.getAssetID().toString());
                                }
                             }
                             else if (eventService.isEventEnabled()) eventService.event(rmiVariantName + " was not offered");
                        }
                        if(proposedVariant.getVariantName().equals("Low")) {
                            String rmiVariantName = "Enabled";
                            VariantEvaluation rmiVariant = getRmiVariantEvaluation(cbe, rmiAction, rmiVariantName);
                            if (rmiVariant != null && rmiAction != null && rmiAction.getValuesOffered().contains(rmiVariantName)) {
                                if (logger.isInfoEnabled()) logger.info("Selected: " + rmiVariant.toString() + "for: " + rmiAction.getAssetID().toString());
                                if (eventService.isEventEnabled()) eventService.event(agentId + " selected " + rmiAction.getClass().getName() + ":" + rmiVariant.getVariantName() + " for " + rmiAction.getAssetID().toString());
                                Set rmiPermittedVariants = new HashSet();  
                                if ((!rmiAction.getPermittedValues().contains(rmiVariant.getVariantName()))
                                        && (thisAction.getValue() == null  
                                            || !thisAction.getValue().getAction().equals(proposedVariant.getVariantName())
                                            || !thisAction.getValue().isActive())) {
                                    permittedVariants.add(proposedVariant);
                                    publishAdd(new SelectedAction(thisAction, permittedVariants, Math.round(knob.getPatienceFactor()*proposedVariant.getExpectedTransitionTime()), cbe));
                                    if (logger.isInfoEnabled()) logger.info("Enabling: " + proposedVariant.toString() + "for: " + thisAction.getAssetID().toString());
                                }
                             }
                             else if (eventService.isEventEnabled()) eventService.event(rmiVariantName + " was not offered");
                        }
                    }
                    
                }
            }
        }
    }
    if (alreadySelectedVariants.isEmpty()) { // could not find anything useful to do
        if (logger.isInfoEnabled()) logger.info("Done processing: " + cbe.dumpAvailableVariants());
        publishRemove(cbe);
    }
  }

  private Set findActiveActions(CostBenefitEvaluation cbe) {
    Set activeActions = new HashSet();
    Collection allActions = findActionCollection(cbe.getAssetID());
    if (allActions != null) {
	Iterator iter = allActions.iterator();
	while (iter.hasNext()) {
	    ActionsWrapper thisWrapper = (ActionsWrapper) iter.next();
	    Action thisAction = thisWrapper.getAction();
	    if ((thisAction.getValue() != null && thisAction.getValue().isActive())) {
		activeActions.add(thisAction);
	    }
	}
    }
    if (logger.isInfoEnabled()) logger.info("Active Actions: " + activeActions.toString());
    return activeActions;
  }

    private boolean outOfResources() {
        // tracks resources allocated to Actions so we can tell whether we can choose more actions
        return false;
    }


    // a special method in support of controlling RMIAction in tandem with Security_Defense_Setting - for ACUC 2B
    // returns the VariantEValuation or null if not found
    private VariantEvaluation getRmiVariantEvaluation(CostBenefitEvaluation cbe, ACtion rmiAction, String rmiVariantName) {
        if (rmiAction == null) return null;
        ActionEvaluation ae = cbe.getActionEvaluation(rmiAction);
        if (ae != null) return ae.getVariantEvaluation(rmiVariantName);
        else return null;
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
