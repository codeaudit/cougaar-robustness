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

package org.cougaar.coordinator.costBenefit;


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
import java.util.Enumeration;
import java.util.Set;


/**
 * This Plugin is used to handle the cost benefit computation for Defense Deconfliction
 * It emits CostBenefitEvaluation objects to the BB and cleans up old ones
 *
 */
public class CostBenefitPlugin extends DeconflictionPluginBase implements NotPersistable {

    private ActionTechSpecService ActionTechSpecService;    
    private IncrementalSubscription stateEstimationSubscription;    
//    private IncrementalSubscription threatModelSubscription;
    private IncrementalSubscription knobSubscription;
    
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

            // Clean up the old SE & CBE
            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            StateEstimation old_se = old_cbe.getStateEstimation();
            publishRemove(old_se);
            publishRemove(old_cbe);

            // Produce and publish a CBE containing the benefits for each offered action on the Asset
            CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);
            publishAdd(cbe);
  
        }           
    }
    
    /** 
      * Sets up local subscriptions - in this case for the control Knob containing the default settings
      */
    protected void setupSubscriptions() { 
        stateEstimationSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(StateEstimation.pred);    
        // threatModelSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(ThreatModel.pred);
        knobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitKnob.pred);
        publishAdd(knob);
    }
    

    private CostBenefitEvaluation createCostBenefitEvaluation(StateEstimation se, CostBenefitKnob knob) {

        AssetID assetID = se.getAssetID();
        CostBenefitEvaluation cbe = 
                new CostBenefitEvaluation(assetID, knob.getHorizon(), se);

        Collection actions = findActionCollection(assetID);
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
                    throw (new TechSpecNotFoundException( "Cannot find Action Tech Spec for "+ this.getClass().getName() ));
                }
                // Which StateDimension does this Action apply to?
                AssetStateDimension asd = atsi.getStateDimension();
                
                try { 
                    // Get the StateDimensionEstimation for that dimension - Throws an exception if SDE not found
                    StateDimensionEstimation currentEstimatedStateDimension = se.getStateDimensionEstimation(asd);

                    // create the Action container that will hold the evaluations of all offered Variants
                    ActionEvaluation thisActionEvaluation = new ActionEvaluation(thisAction);

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
                        thisActionEvaluation.addVariantEvaluation(createVariantEvaluation(assetID, currentEstimatedStateDimension, thisVariantDescription, knob)); 
                    }
                }
                catch (BelievabilityException e) {
                    logger.error("Cannot find StateDimensionEstimate for "+asd.toString());
                } 

            }
            catch (TechSpecNotFoundException e) {
                if (logger.isDebugEnabled()) logger.debug("Cannot find Action Tech Spec for "+ this.getClass().getName() );
            }

        }

        return cbe;
    }



    private double aggregateCost(ActionCost actionCost, double memSize, double bandwidthSize) {
        double memoryComponent = computeCostComponent(actionCost.getMemoryCost(), memSize, bandwidthSize);
        double bandwidthComponent = computeCostComponent(actionCost.getBandwidthCost(), memSize, bandwidthSize);
        double cpuComponent = computeCostComponent(actionCost.getCPUCost(), memSize, bandwidthSize);
        return (0.6*memoryComponent + 0.3*bandwidthComponent+0.1*cpuComponent);
    }


    private double computeCostComponent(ActionCost.Cost thisCost, double memSize, double bandwidthSize) {
        double c = thisCost.getIntensity();
        if (!(thisCost.isAgentSizeAFactor()) || thisCost.isMessageSizeAFactor()) return c;
        double outCost = 0.0;
        if (thisCost.isAgentSizeAFactor()) outCost = outCost + c * memSize;
        if (thisCost.isMessageSizeAFactor()) outCost = outCost + c * bandwidthSize;
        return outCost;
    }

    private double computeStateBenefit(AssetState state, CostBenefitKnob knob) {
        return (knob.getCompletenessWeight()*state.getRelativeMauCompleteness() 
              + knob.getSecurityWeight()*state.getRelativeMauSecurity() 
              + knob.getTimelinessWeight()*1.0);  // FIX - need a way to determine MauTimeliness (the 1.0 factor in the preceeding)
    }

   private VariantEvaluation createVariantEvaluation
            (AssetID assetID, StateDimensionEstimation currentStateDimensionEstimation, ActionDescription thisVariantDescription, CostBenefitKnob knob) {
        double memorySize = 1000.0; // FIX _ needs to be dynamic
        double bandwidthSize = 1000.0; // FIX -needs to be dynamic
        double predictedCost = 0.0;
        double predictedBenefit = 0.0;
        long maxTransitionTime = 0L;
        long horizon = knob.getHorizon();
        Enumeration startStateEnumeration = currentStateDimensionEstimation.getStateNames();
        while (startStateEnumeration.hasMoreElements()) {
            String startStateName = (String) startStateEnumeration.nextElement();
            double startStateProb;
            double startStateBenefit;
            double intermediateStateBenefit;
            double endStateBenefit;
            long transitionTime;
            ActionCost oneTimeCost;
            ActionCost continuingCost;
            try {
                startStateProb = currentStateDimensionEstimation.getProbability(startStateName);
                AssetTransitionWithCost atwc = thisVariantDescription.getTransitionForState(startStateName);
                startStateBenefit = computeStateBenefit(atwc.getStartValue(), knob);
                intermediateStateBenefit = computeStateBenefit(atwc.getIntermediateValue(), knob);
                endStateBenefit = computeStateBenefit(atwc.getEndValue(), knob);
                transitionTime = atwc.getOneTimeCost().getTimeCost(); // assume for now that 1-time costs are all expressed in terms of time (and maybe other factors also)
                oneTimeCost = atwc.getOneTimeCost();
                continuingCost = atwc.getContinuingCost();
                }
            catch (BelievabilityException e) {
                // Should be impossible to get here if the ActionDescription is correctly populated
                throw new RuntimeException("Could not find an AssetState that the ActionDescription said it had");
                }

            if (horizon >= transitionTime)  { // the normal case
                predictedBenefit = predictedBenefit + startStateProb*(transitionTime*intermediateStateBenefit + (horizon-transitionTime)*endStateBenefit - horizon*startStateBenefit);
                predictedCost = predictedCost + startStateProb*(transitionTime
                        * aggregateCost(oneTimeCost, memorySize, bandwidthSize) + (horizon-transitionTime)*aggregateCost(continuingCost, memorySize, bandwidthSize));
            }
            else  { // transition takes longer than the planning horizon, so action transition will not complete
                predictedBenefit = predictedBenefit + startStateProb*(horizon*intermediateStateBenefit - horizon*startStateBenefit);
                predictedCost = predictedCost + startStateProb*(horizon*aggregateCost(oneTimeCost, memorySize, bandwidthSize));
            }
            maxTransitionTime = Math.max(maxTransitionTime, transitionTime);
        }

        return new VariantEvaluation(thisVariantDescription, predictedCost, predictedBenefit, maxTransitionTime);
    }}

