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

//    private ActionTechSpecService ActionTechSpecService;
    private IncrementalSubscription stateEstimationSubscription;
//    private IncrementalSubscription threatModelSubscription;
    private IncrementalSubscription knobSubscription;
    private IncrementalSubscription diagnosesWrapperSubscription;


    private Hashtable actions;

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

/*  removed 9/22/04 dlw
            //  this was the old way of doing thngs - it caused Selction to see changes that were part of processing an old situation rather than new information
            // Clean up the old SE & CBE if they exist

            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            if (old_cbe != null) {
                StateEstimation old_se = old_cbe.getStateEstimation();
                publishRemove(old_se);
                publishRemove(old_cbe);
                }
*/
			// If there is an existing CBE, ignore this SE, because otherwise we might oveerrun the selection process
            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            if (old_cbe == null) {
            	CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);
            	if (logger.isInfoEnabled()) logger.info("CostBenefitEvaluation created: "+cbe.toString());
            	publishAdd(cbe);
			}
			else if (logger.isInfoEnabled()) logger.info("Discarding SE for: " + se.getAssetID() + " still processng last one.");


/*
            // ignore new SE's if still processing an old one
            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            if (old_cbe == null) {
                // Produce and publish a CBE containing the benefits for each offered action on the Asset
                CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);
                if (logger.isInfoEnabled()) logger.info("CostBenefitEvaluation created: "+cbe.toString());
                publishAdd(cbe);
                }
            else {
                // this SE represents a transititional state - ignore this SE but keep it on the BB so it is visible to the monitor servletst
                if (logger.isInfoEnabled()) logger.info("Ignoring Transitional SE");
             //   publishRemove(se);
            }
*/

        }
    }

    /**
      * Sets up local subscriptions - in this case for the control Knob containing the default settings
      */
    protected void setupSubscriptions() {
        super.setupSubscriptions();

        diagnosesWrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosesWrapper) {
                    return true ;
                }
                return false ;
            }
        }) ;

        stateEstimationSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(StateEstimation.pred);
        // threatModelSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(ThreatModel.pred);
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

        Iterator actionsIter = actions.iterator();

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
                        //if (logger.isDebugEnabled()) logger.debug("Current ESD: "+((currentEstimatedStateDimension==null)?"null":"cesd="+currentEstimatedStateDimension.toString()));
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
                            // Add an entry to the current Action containing the information about this Variant
                            thisActionEvaluation.addVariantEvaluation(createCorrectiveVariantEvaluation(assetID, asd, currentEstimatedStateDimension, thisVariantDescription, knob));
                        }
                    }
                    catch (BelievabilityException e) {
                        logger.error("Cannot find StateDimensionEstimate for "+asd.toString());
                    }
                }
                else if (atsi.getActionType() == ActionTechSpecInterface.COMPENSATORY_ACTIONTYPE) {
                    // Which StateDimension does this Action apply to?
                    AssetStateDimension actionStateDimension = atsi.getStateDimension();
                    AssetStateDimension baseStateDimension = diagnosisTechSpecService.getDiagnosisTechSpec("org.cougaar.mts.std.RMILinksStatusDiagnosis").getStateDimension();
                    AssetStateDimension compensatedStateDimension = diagnosisTechSpecService.getDiagnosisTechSpec("org.cougaar.mts.std.AllLinksStatusDiagnosis").getStateDimension();
                    if (logger.isDebugEnabled()) logger.debug("This COMPENSATORY Action: "+thisAction+":"+atsi+":"+actionStateDimension+":"+baseStateDimension+":"+compensatedStateDimension);

                    try {
                        // Get the StateDimensionEstimation for the base dimension the Action applies to - Throws an exception if SDE not found
                        StateDimensionEstimation currentEstimatedBaseStateDimension = se.getStateDimensionEstimation(baseStateDimension);
                        // create the Action container that will hold the evaluations of all offered Variants
                        ActionEvaluation thisActionEvaluation = new ActionEvaluation(thisAction);
                        cbe.addActionEvaluation(thisActionEvaluation);

                        /* Evaluate ALL variants of this Action, not just the ones currently offered
                         * Selection will down-select to only the offered variants
                         * We compute for all variants because the set of offered variants may change during the process of addressing a problem
                         */

                        double benefitOfStayingTheSame;
                        if (thisAction.getValue() != null) { // Compute the benefit of what we are doing NOW, so we an compute the differential benefit of doing soemthing different
                            ActionDescription currentVariant = atsi.getActionDescriptionForValue(thisAction, thisAction.getValue().getAction());
                            if (logger.isDebugEnabled()) logger.debug("currentVariant: " + currentVariant.toString());
                            benefitOfStayingTheSame = createCompensatoryVariantEvaluation
                                                                   (currentVariant,
                                                                    baseStateDimension,
                                                                    currentEstimatedBaseStateDimension,
                                                                    compensatedStateDimension,
                                                                    0.0,
                                                                    knob).getPredictedBenefit();
                        }
                        else { // nothing has been started yet (initialization)
                            benefitOfStayingTheSame = 0.0;
                        }

                        // Get the TechSpec data for all the Variants
                        Collection variantDescriptions = atsi.getActions();
                        // iterate thru all offered Variants of this Action
                        Iterator variantIter = variantDescriptions.iterator();
                        while (variantIter.hasNext()) {
                            // Get TechSpec for this Variant
                            ActionDescription thisVariantDescription = (ActionDescription) variantIter.next();
                            // Add an entry to the current Action containing the information about this Variant
                            thisActionEvaluation.addVariantEvaluation(createCompensatoryVariantEvaluation
                                       (thisVariantDescription,
                                        baseStateDimension,
                                        currentEstimatedBaseStateDimension,
                                        compensatedStateDimension,
                                        benefitOfStayingTheSame,
                                        knob)
                            );
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

   private VariantEvaluation createCorrectiveVariantEvaluation(AssetID assetID, AssetStateDimension asd, StateDimensionEstimation currentStateDimensionEstimation, ActionDescription thisVariantDescription, CostBenefitKnob knob) {
        double memorySize = 1000.0; // FIX _ needs to be dynamic
        double bandwidthSize = 1000.0; // FIX -needs to be dynamic
        double predictedCost = 0.0;
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
                    transitionTime = (atwc.getOneTimeCost()!=null)?atwc.getOneTimeCost().getTimeCost():0L; // assume for now that 1-time costs are all expressed in terms of time (and maybe other factors also)
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

        return new VariantEvaluation(thisVariantDescription, predictedCost, predictedBenefit, maxTransitionTime);
    }

    private VariantEvaluation createCompensatoryVariantEvaluation(ActionDescription thisVariantDescription,
                                                                  AssetStateDimension baseStateDimension,
                                                                  StateDimensionEstimation currentEstimatedBaseStateDimension,
                                                                  AssetStateDimension compensatedStateDimension,
                                                                  double benefitOfStayingTheSame,
                                                                  CostBenefitKnob knob) {

        double predictedCost = 0.0;
        double predictedBenefit = 0.0;

        long maxTransitionTime = thisVariantDescription.getTransitionForState("*").getOneTimeCost().getTimeCost();

        Iterator baseStateIterator = baseStateDimension.getPossibleStates().iterator();
        Vector cumulativeProbVector = new Vector();
            cumulativeProbVector.add(new StateProb("Excellent", 0.0));
            cumulativeProbVector.add(new StateProb("Good", 0.0));
            cumulativeProbVector.add(new StateProb("Fair", 0.0));
            cumulativeProbVector.add(new StateProb("Poor", 0.0));
            cumulativeProbVector.add(new StateProb("None", 0.0));
        while (baseStateIterator.hasNext()) {
            String startStateName = ((AssetState)baseStateIterator.next()).getName();
            double startStateProb = 0.0;
            try {
               startStateProb = currentEstimatedBaseStateDimension.getProbability(startStateName);
            }
            catch (BelievabilityException e) {
                logger.error(e.toString());
            }

            Vector v = mapCompensation(startStateName, thisVariantDescription.name());
            if (logger.isDebugEnabled()) logger.debug(thisVariantDescription.name() +" for " + startStateName + "----"+v.toString());
            accumulateProbs(cumulativeProbVector, v, startStateProb);
        }
        if (logger.isDebugEnabled()) logger.debug("Cumulative Compensated Prob Vector: " + cumulativeProbVector.toString());

        predictedBenefit = knob.horizon * computeBenefit(cumulativeProbVector, compensatedStateDimension, knob) - benefitOfStayingTheSame;

        return new VariantEvaluation(thisVariantDescription, predictedCost, predictedBenefit, maxTransitionTime);
    }

    private Vector mapCompensation(String baseStateName, String actionSettingName) {
        Vector v = new Vector(5);
        if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("Normal")) {
            v.add(new StateProb("Excellent", 0.8));
            v.add(new StateProb("Good", 0.1));
            v.add(new StateProb("Fair", 0.1));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 0.0));
            return v;
        }
        else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("AlternateDirect")) {
            v.add(new StateProb("Excellent", 0.7));
            v.add(new StateProb("Good", 0.1));
            v.add(new StateProb("Fair", 0.1));
            v.add(new StateProb("Poor", 0.1));
            v.add(new StateProb("None", 0.0));
            return v;
        }
        else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("StoreAndForward")) {
            v.add(new StateProb("Excellent", 0.1));
            v.add(new StateProb("Good", 0.6));
            v.add(new StateProb("Fair", 0.2));
            v.add(new StateProb("Poor", 0.1));
            v.add(new StateProb("None", 0.0));
            return v;
        }
        else if (baseStateName.equals("DirectPathExists") && actionSettingName.equals("Disable")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("Normal")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("AlternateDirect")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.2));
            v.add(new StateProb("Poor", 0.3));
            v.add(new StateProb("None", 0.5));
            return v;
        }
        else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("StoreAndForward")) {
            v.add(new StateProb("Excellent", 0.1));
            v.add(new StateProb("Good", 0.7));
            v.add(new StateProb("Fair", 0.1));
            v.add(new StateProb("Poor", 0.1));
            v.add(new StateProb("None", 0.0));
            return v;
        }
        else if (baseStateName.equals("OnlyIndirectPathExists") && actionSettingName.equals("Disable")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("Normal")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("AlternateDirect")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("StoreAndForward")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        else if (baseStateName.equals("NoPathExists") && actionSettingName.equals("Disable")) {
            v.add(new StateProb("Excellent", 0.0));
            v.add(new StateProb("Good", 0.0));
            v.add(new StateProb("Fair", 0.0));
            v.add(new StateProb("Poor", 0.0));
            v.add(new StateProb("None", 1.0));
            return v;
        }
        return null;
    }

    protected void accumulateProbs(Vector cumulative, Vector startStateVector, double startStateProb) {
        Iterator iterA = cumulative.iterator();
        Iterator iterB = startStateVector.iterator();
        while (iterA.hasNext()) {
            StateProb cumulativeState = (StateProb)iterA.next();
            StateProb incrementalState = (StateProb)iterB.next();
            cumulativeState.setProb(cumulativeState.getProb() + startStateProb*incrementalState.getProb());
        }
    }

    protected double computeBenefit(Vector cumulative, AssetStateDimension compensatedStateDimension, CostBenefitKnob knob) {
        double benefit = 0.0;
        Iterator iter = cumulative.iterator();
        while (iter.hasNext()) {
            StateProb thisState = (StateProb)iter.next();
            benefit = benefit + thisState.getProb() * computeStateBenefit(compensatedStateDimension.findAssetState(thisState.getStateName()), knob);
        }
        return benefit;
    }

    private class StateProb {

        private String stateName;
        private double prob;

        public StateProb(String stateName, double prob) {
            this.stateName = stateName;
            this.prob = prob;
        }

        public String getStateName() { return stateName; }
        public double getProb() { return prob; }
        protected void setProb(double prob) { this.prob=prob; }

        public String toString() { return stateName + ":" + prob; }

    }




}

