package org.cougaar.coordinator.costBenefit;

/*
 * CostBenefitPlugin.java
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

import org.cougaar.coordinator.*;  //FIX

import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.policy.DefensePolicy;
import org.cougaar.coordinator.believability.StateEstimation;
import org.cougaar.coordinator.believability.StateDimensionEstimation;
//import org.cougaar.coordinator.believability.AssetBeliefState;
import org.cougaar.coordinator.believability.BelievabilityException;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.techspec.ActionDescription;
import org.cougaar.coordinator.techspec.ActionCost;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTransitionWithCost;
import org.cougaar.coordinator.techspec.ActionTechSpecService;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.coordinator.selection.SelectionCompleted;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Set;


/**
 * This Plugin is used to handle the cost benefit computation for Defense Deconfliction
 * It emits CostBenefitEvaluation objects to the BB and cleans up old ones
 *
 */
public class CostBenefitPlugin extends DeconflictionPluginBase implements NotPersistable {

    private IncrementalSubscription stateEstimationSubscription;
    private IncrementalSubscription selectionCompletedSubscription;
    private IncrementalSubscription knobSubscription;

    private Hashtable actions;
    private Hashtable pendingSEs;

    private MediatedTechSpecMap mediatedTechSpecMap = new MediatedTechSpecMap(); // this should come from real TechSpecs - no time

    public static final String CALC_METHOD = "SIMPLE";
    public CostBenefitKnob knob;

    /**
      * Creates a new instance of CostBenefitPlugin
      */
    public CostBenefitPlugin() {
        super();

    }


    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        actions = new Hashtable(10);
        pendingSEs = new Hashtable();
        knob = new CostBenefitKnob();
    }


    protected void execute() {

        Iterator iter;

        // If the control Knob has been changed (by the PolicyPlugin), grab the new settings for use in the CB computations
        iter = knobSubscription.getChangedCollection().iterator();
        if (iter.hasNext())
        {
            knob = (CostBenefitKnob) iter.next();
        }

        //Handle the addition of new StateEstimations
        iter = stateEstimationSubscription.getAddedCollection().iterator();
        while(iter.hasNext())
        {
            StateEstimation se = (StateEstimation)iter.next();
            if (logger.isInfoEnabled()) logger.info(se.toString());

            // If there is an existing CBE, ignore this SE, because otherwise we might oveerrun the selection process
            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            if (old_cbe == null) {
            	CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);
            	if (logger.isInfoEnabled()) logger.info("CostBenefitEvaluation created: "+cbe.toString());
            	publishAdd(cbe);
	      }
		else {
	            pendingSEs.put(se.getAssetID(), se);
                  if (logger.isInfoEnabled()) logger.info("Caching SE for: " + se.getAssetID() + " for possible future use.  Still processing last one.");
            }
		publishRemove(se);
        }

        //Handle CompletedSelections - if FAILED & have an existing SE, try again with the new SE
        iter = selectionCompletedSubscription.getAddedCollection().iterator();
        while(iter.hasNext())
        {
            SelectionCompleted sc = (SelectionCompleted)iter.next();
            if (logger.isInfoEnabled()) logger.info(sc.toString());
   
            AssetID assetID = sc.geAssetID();
            StateEstimation se = (StateEstimation) pendingSEs.remove(assetID);

		if ((se != null) && !sc.successfulP()) {
          		CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);
           		if (logger.isInfoEnabled()) logger.info("CostBenefitEvaluation created: "+cbe.toString());
           		publishAdd(cbe);
	      }
		publishRemove(sc);
        }
    }

    /**
      * Sets up local subscriptions - in this case for the control Knob containing the default settings
      */
    protected void setupSubscriptions() {
        super.setupSubscriptions();
        stateEstimationSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(StateEstimation.pred);
        selectionCompletedSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(SelectionCompleted.pred);
        knobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitKnob.pred);
        publishAdd(knob);
    }


    private CostBenefitEvaluation createCostBenefitEvaluation(StateEstimation se, CostBenefitKnob knob) {

        AssetID assetID = se.getAssetID();
        if (logger.isDebugEnabled()) logger.debug("Looking for actions for "+assetID);
        CostBenefitEvaluation cbe =
                new CostBenefitEvaluation(assetID, knob.getHorizon(), se);

        Collection actions = findActionCollection(assetID);
        if (logger.isDebugEnabled()) logger.debug("Have "+((actions != null)?actions.size():0)+" actions");
        if (logger.isDebugEnabled()) logger.debug("StateEstimation: "+se.toString());

        if (actions == null) return cbe; // No actions exist that even possibly apply - usually an initialization situation

        Iterator actionsIter;

	if (logger.isDebugEnabled()) {
	    actionsIter = actions.iterator();
   	    logger.debug("CB knows following Actions for: " + assetID);
	    while (actionsIter.hasNext()) {
                ActionsWrapper thisActionWrapper = (ActionsWrapper) actionsIter.next();
                Action thisAction = thisActionWrapper.getAction();
		logger.debug(thisAction.toString());
	    }
	}

	actionsIter = actions.iterator();
        // iterate over all applicable Actions for this asset
        // compute the expected c-b for each based on StateEstimation
        while (actionsIter.hasNext()) {
            ActionsWrapper thisActionWrapper = (ActionsWrapper) actionsIter.next();
            Action thisAction = thisActionWrapper.getAction();

            try {
                //call tech spec service & get action tech spec
                ActionTechSpecInterface atsi = (ActionTechSpecInterface) actionTechSpecService.getActionTechSpec(thisAction.getClass().getName());
                if (atsi == null) {
                    throw (new TechSpecNotFoundException( "Cannot find Action Tech Spec for "+ thisAction.getClass().getName() ));
                }
               if (atsi.getActionType() == ActionTechSpecInterface.CORRECTIVE_ACTIONTYPE) {
                    // Which StateDimension does this Action apply to?
                    AssetStateDimension asd = atsi.getStateDimension();
                    if (logger.isDebugEnabled()) logger.debug("This CORRECTIVE Action: "+thisAction+":"+atsi+":"+asd);

                    try {
                        // Get the StateDimensionEstimation for the dimension the Action applies to - Throws an exception if SDE not found
                        StateDimensionEstimation currentEstimatedStateDimension = se.getStateDimensionEstimation(asd);
                        // create the Action container that will hold the evaluations of all offered Variants
                        ActionEvaluation thisActionEvaluation = new ActionEvaluation(thisAction);
                        cbe.addActionEvaluation(thisActionEvaluation);

                        /* Evaluate ALL variants of this Action, not just the ones currently offered
                         * Selection will down-select to only the offered variants
                         * We compute for all variants because the set of offered variants may change during the process of addressing a problem
                         */

                        // Get the TechSpec data for all the Variants
                        Collection variantDescriptions = atsi.getActions();
                        // iterate thru all offered Variants of this Action
                        Iterator variantIter = variantDescriptions.iterator();
                        while (variantIter.hasNext()) {
                            // Get TechSpec for this Variant
                            ActionDescription thisVariantDescription = (ActionDescription) variantIter.next();
                            // Is this variant active?
                            boolean thisVariantActiveP = (thisAction.getValue() != null) && thisAction.getValue().isActive() && thisAction.getValue().getAction().equals(thisVariantDescription.name());
                            // Add an entry to the current Action containing the information about this Variant
                            thisActionEvaluation.addVariantEvaluation(createCorrectiveVariantEvaluation(assetID, asd, currentEstimatedStateDimension, thisVariantDescription, thisActionEvaluation, thisVariantActiveP, knob)); 

                        }
                    }
                    catch (BelievabilityException e) {
                        logger.error("Cannot find StateDimensionEstimate for "+asd.toString());
                    }
                }
                else if (atsi.getActionType() == ActionTechSpecInterface.COMPENSATORY_ACTIONTYPE) {
                    // Which StateDimension does this Action apply to?
                    AssetStateDimension actionStateDimension = atsi.getStateDimension();
                    AssetStateDimension baseStateDimension = mediatedTechSpecMap.getBaseDimension(actionStateDimension.getStateName(), diagnosisTechSpecService);
                    AssetStateDimension compensatedStateDimension = mediatedTechSpecMap.getCompensatedDimension(actionStateDimension.getStateName(), diagnosisTechSpecService);
                    if (logger.isDebugEnabled()) logger.debug("This COMPENSATORY Action: "+thisAction+":"+atsi+":"+actionStateDimension+":"+baseStateDimension+":"+compensatedStateDimension);

                    try {
                        // Get the StateDimensionEstimation for the base dimension the Action applies to - Throws an exception if SDE not found
                        StateDimensionEstimation currentEstimatedBaseStateDimension = se.getStateDimensionEstimation(baseStateDimension);
                        // create the Action container that will hold the evaluations of all offered Variants
                        ActionEvaluation thisActionEvaluation = new ActionEvaluation(thisAction);
                        cbe.addActionEvaluation(thisActionEvaluation);

                        // Get the TechSpec data for all the Variants
                        Collection variantDescriptions = atsi.getActions();
                        // iterate thru all offered Variants of this Action
                        Iterator variantIter = variantDescriptions.iterator();
                        while (variantIter.hasNext()) {
                            // Get TechSpec for this Variant
                            ActionDescription thisVariantDescription = (ActionDescription) variantIter.next();
                            boolean thisVariantActiveP = thisAction.getValue().isActive() && thisAction.getValue().getAction().equals(thisVariantDescription.name());
                            // Add an entry to the current Action containing the information about this Variant
                            thisActionEvaluation.addVariantEvaluation(createCompensatoryVariantEvaluation
                                       (thisVariantDescription, thisActionEvaluation, currentEstimatedBaseStateDimension, thisAction, 
                                        baseStateDimension, compensatedStateDimension, thisVariantActiveP, knob));
                        }
                    }
                    catch (BelievabilityException e) {
                        logger.error("Cannot find StateDimensionEstimate"+e.toString());
                    }
                }

            }
            catch (TechSpecNotFoundException e) {
                if (logger.isErrorEnabled()) logger.error( e.toString() );
            }

        }

        return cbe;
    }

    private double aggregateCost(ActionCost actionCost, double memSize, double bandwidthSize) {
        if (actionCost == null) return 0.0; // there is no cost component (often true of continuing costs)
        double memoryComponent = computeCostComponent(actionCost.getMemoryCost(), memSize, bandwidthSize);
        double bandwidthComponent = computeCostComponent(actionCost.getBandwidthCost(), memSize, bandwidthSize);
        double cpuComponent = computeCostComponent(actionCost.getCPUCost(), memSize, bandwidthSize);
        return (0.6*memoryComponent + 0.3*bandwidthComponent+0.1*cpuComponent);
    }


    private double computeCostComponent(ActionCost.Cost thisCost, double memSize, double bandwidthSize) {
        if (thisCost == null) return 0.0;  // no cost in this dimension
        double c = thisCost.getIntensity();
        if (!(thisCost.isAgentSizeAFactor()) || thisCost.isMessageSizeAFactor()) {
            return c;
            }
        double outCost = 0.0;
        if (thisCost.isAgentSizeAFactor()) outCost = outCost + c * memSize;
        if (thisCost.isMessageSizeAFactor()) outCost = outCost + c * bandwidthSize;
        return outCost;
    }

    private double computeStateBenefit(AssetState state, CostBenefitKnob knob) {
        if (state == null) return 0.0; // no such state - should only be for Intermediate state
        double stateBenefit =
                knob.getCompletenessWeight()*state.getRelativeMauCompleteness()
              + knob.getSecurityWeight()*state.getRelativeMauSecurity()
              + knob.getTimelinessWeight()*1.0;  // FIX - need a way to determine MauTimeliness (the 1.0 factor in the preceeding)
        if (logger.isDetailEnabled()) logger.detail("Benefit of state: " + state.toString() + " is " + stateBenefit);
        return stateBenefit;
    }

   private VariantEvaluation createCorrectiveVariantEvaluation(AssetID assetID, AssetStateDimension asd, StateDimensionEstimation currentStateDimensionEstimation, ActionDescription thisVariantDescription, ActionEvaluation thisActionEvaluation, boolean isActive, CostBenefitKnob knob) {
        double memorySize = 1000.0; // FIX _ needs to be dynamic
        double bandwidthSize = 1000.0; // FIX -needs to be dynamic
        double predictedCost = 0.0; // total predicted cost
        double predictedBenefit = 0.0;
        long maxTransitionTime = 0L;
        long horizon = knob.getHorizon();
        Vector startStateVector = asd.getPossibleStates();
        Iterator startStateIterator = startStateVector.iterator();
        while (startStateIterator.hasNext()) {
            AssetState startState = (AssetState) startStateIterator.next();
            String startStateName = startState.getName();
            double startStateProb;
            double startStateBenefit;
            double intermediateStateBenefit;
            double endStateBenefit;
            long transitionTime;
            ActionCost oneTimeCost;
            ActionCost continuingCost;
            try {
                startStateProb = currentStateDimensionEstimation.getProbability(startStateName);
                AssetTransitionWithCost atwc = thisVariantDescription.getTransitionForState(startState);
                if (atwc != null) { // the TechSpecs define a transition by this Variant for this state
                    if (logger.isDebugEnabled()) logger.debug("Found a transition for: "+ thisVariantDescription.name() + " for start state " + startStateName+" is "+atwc.toString());
                    startStateBenefit = computeStateBenefit(startState, knob);
                    intermediateStateBenefit = computeStateBenefit(atwc.getIntermediateValue(), knob);
                    endStateBenefit = computeStateBenefit(atwc.getEndValue(), knob);
                    if (!isActive) { // dlw 10/7/04 
                        // this is the case where the action has to startup
                        transitionTime = (atwc.getOneTimeCost()!=null)?atwc.getOneTimeCost().getTimeCost():0L; // assume for now that 1-time costs are all expressed in terms of time (and maybe other factors also)
                    }
                    else {
                        // this is the case where the action is already active, so transition doesn't happen (handled as an instantaneous transition)
                        transitionTime = 0;
                    }
                    oneTimeCost = atwc.getOneTimeCost();
                    continuingCost = atwc.getContinuingCost();

                    if (horizon >= transitionTime)  { // the normal case
                        double stateBenefit = 0.5 * endStateBenefit * (horizon - transitionTime) // time-depreciated benefit of being in the end state
                                            + 0.5 * transitionTime * intermediateStateBenefit * (1 + (horizon-transitionTime)/horizon) // time-depreciated benefoit of being in the intermediate (transition) state
                                            - 0.5 * horizon * startStateBenefit; // the benefit of doing nothing


                        predictedBenefit = predictedBenefit + startStateProb * stateBenefit;
                        if (logger.isDebugEnabled()) logger.debug("**** stateBenefit for: " + startStateName + " is: " + stateBenefit + " with prob: " + startStateProb);
                        predictedCost = predictedCost + startStateProb*(transitionTime
                                * aggregateCost(oneTimeCost, memorySize, bandwidthSize) + (horizon-transitionTime)*aggregateCost(continuingCost, memorySize, bandwidthSize));
                        }
                    else  { // transition takes longer than the planning horizon, so action transition will not complete
                        double stateBenefit = horizon*intermediateStateBenefit;
                        predictedBenefit = predictedBenefit + startStateProb * stateBenefit;
                        if (logger.isDebugEnabled()) logger.debug("**** stateBenefit for: " + startStateName + " is: " + stateBenefit + " with prob: " + startStateProb);
                        predictedCost = predictedCost + startStateProb*(horizon*aggregateCost(oneTimeCost, memorySize, bandwidthSize));
                        }
                    if (logger.isDebugEnabled()) logger.debug("**** Cumlative Benefit: " + predictedBenefit);
                    maxTransitionTime = Math.max(maxTransitionTime, transitionTime);

                    }
               }
            catch (BelievabilityException e) {
                // Should be impossible to get here if the ActionDescription is correctly populated
                throw new RuntimeException("Could not find an AssetState that the ActionDescription said it had");
                }

        }

        return new VariantEvaluation(thisVariantDescription, thisActionEvaluation, predictedCost, predictedCost/horizon, predictedBenefit, maxTransitionTime);
    }

    private VariantEvaluation createCompensatoryVariantEvaluation(ActionDescription proposedVariantDescription,
                                                                  ActionEvaluation thisActionEvaluation,
                                                                  StateDimensionEstimation baseStateEstimation, 
                                                                  Action action, // so we can find out what we are currently doing
                                                                  AssetStateDimension baseStateDimension, 
                                                                  AssetStateDimension compensatedStateDimension,
                                                                  boolean isVariantActive,
                                                                  CostBenefitKnob knob) {

        double memorySize = 1000.0; // FIX _ needs to be dynamic
        double bandwidthSize = 1000.0; // FIX -needs to be dynamic
        double predictedCost = 0.0;
        double predictedBenefit = 0.0;

        AssetTransitionWithCost atwc = proposedVariantDescription.getTransitionForState("*");
        long transitionTime = atwc.getOneTimeCost().getTimeCost();
        ActionCost oneTimeCost = atwc.getOneTimeCost();
        ActionCost continuingCost = atwc.getContinuingCost();

        String actionName = thisActionEvaluation.getActionName();
        String compensatedDimensionName = compensatedStateDimension.getStateName();

        String currentVariantName;
        if ((action.getValue() != null) && (action.getValue().getAction() != null) && action.getValue().isActive()) {
            currentVariantName = (String)action.getValue().getAction();
        }
        else {
            currentVariantName = null;
        }

        String proposedVariantName = proposedVariantDescription.name();

        Vector endStateCompensatedProbs = computeCompensatedStateProbs(actionName, proposedVariantName, baseStateDimension, compensatedStateDimension, baseStateEstimation);
        Vector intermediateStateCompensatedProbs = computeCompensatedStateProbs(actionName, currentVariantName, baseStateDimension, compensatedStateDimension, baseStateEstimation); // assume the intermediate state is the current state (need TS model chnages to accomodate this)
        
        if (logger.isDebugEnabled()) {
            logger.debug("BASE state: " + baseStateEstimation.toString());
            logger.debug("predicted Compensated END state: " + compensatedDimensionName + " " + endStateCompensatedProbs.toString());
            logger.debug("predicted Compensated INTERMEDIATE state: " + compensatedDimensionName + " " + intermediateStateCompensatedProbs.toString());
        }

        if (isVariantActive) {
            predictedBenefit = knob.horizon * computeBenefit(endStateCompensatedProbs, compensatedStateDimension, knob);
            predictedCost = knob.horizon * aggregateCost(continuingCost, memorySize, bandwidthSize);
        }
        else {
            predictedBenefit = knob.horizon * computeBenefit(endStateCompensatedProbs, compensatedStateDimension, knob);
            predictedCost = transitionTime * aggregateCost(oneTimeCost, memorySize, bandwidthSize) 
                         + (knob.horizon-transitionTime)*aggregateCost(continuingCost, memorySize, bandwidthSize);
        }

        return new VariantEvaluation(proposedVariantDescription, thisActionEvaluation, predictedCost, predictedCost/knob.getHorizon(), predictedBenefit, transitionTime);
    }


    protected Vector computeCompensatedStateProbs(String actionName, String variantSetting, AssetStateDimension baseStateDimension, AssetStateDimension compensatedDimension, StateDimensionEstimation estimatedBaseStateDimension) {
        Iterator baseStateIterator = baseStateDimension.getPossibleStates().iterator();
        Vector cumulativeProbVector = mediatedTechSpecMap.getEmptyCompensationVector(compensatedDimension);
        while (baseStateIterator.hasNext()) {
            String baseStateName = ((AssetState)baseStateIterator.next()).getName();
            double baseStateProb = 0.0;
            try {
               baseStateProb = estimatedBaseStateDimension.getProbability(baseStateName);
            }
            catch (BelievabilityException e) {
                logger.error(e.toString());
            }

            Vector mappingVector = mediatedTechSpecMap.mapCompensation(actionName, baseStateName, variantSetting);
            if (cumulativeProbVector.size() != mappingVector.size()) {
                logger.error("Bad Compensatory TechSpecs");
            }
            for (int i=0; i < mappingVector.size()-1; i++) {
                ((StateProb)cumulativeProbVector.elementAt(i)).setProb(((StateProb)cumulativeProbVector.elementAt(i)).getProb() + baseStateProb * ((StateProb)mappingVector.elementAt(i)).getProb());
            }
        }
        return cumulativeProbVector;
    }

    protected double computeBenefit(Vector cumulativeProbVector, AssetStateDimension compensatedStateDimension, CostBenefitKnob knob) {
        double benefit = 0.0;
        Iterator iter = cumulativeProbVector.iterator();
        while (iter.hasNext()) {
            StateProb thisState = (StateProb)iter.next();
            benefit = benefit + thisState.getProb() * computeStateBenefit(compensatedStateDimension.findAssetState(thisState.getStateName()), knob);
        }
        return benefit;
    }

}

