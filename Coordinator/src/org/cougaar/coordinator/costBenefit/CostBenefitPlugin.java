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
    private IncrementalSubscription actionTechSpecsSubscription;
    private IncrementalSubscription threatModelSubscription;
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

            // Produce a CBE containing the benefits for each offered action on the Asset
            CostBenefitEvaluation cbe = createCostBenefitEvaluation(se, knob);

            // Clean up the old SE & CBE
            CostBenefitEvaluation old_cbe = findCostBenefitEvaluation(se.getAssetID());
            StateEstimation old_se = old_cbe.getStateEstimation();
            publishRemove(old_se);
            publishRemove(old_cbe);

            // Publish the new CBE
            publishAdd(cbe);
  
        }

        //***************************************************ThreatModelInterface
        //Handle the addition of new ThreatModels
        /*
        for ( Iterator iter = threatModelSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tma = (ThreatModelInterface)iter.next();
            //Process
        }

        //Handle the modification of ThreatModels
        for ( Iterator iter = threatModelSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tmc = (ThreatModelInterface)iter.next();
            //Process
        }
        
        //Handle the removal of ThreatModels
        for ( Iterator iter = threatModelSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tmr = (ThreatModelInterface)iter.next();
            //Process
        }
        */
           
    }
    
    /** 
      * Sets up local subscriptions - in this case for the control Knob 
      */
    protected void setupSubscriptions() {
        knobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe(CostBenefitKnob.pred);
        publishAdd(knob);
    }
    

    private CostBenefitEvaluation createCostBenefitEvaluation(StateEstimation se, CostBenefitKnob knob) {

        AssetID assetID = se.getAssetID();
        CostBenefitEvaluation cbe = 
                new CostBenefitEvaluation(assetID, knob.getCalcMethod(), knob.getHorizon(), se);

        Collection actions = findActionCollection(se.getAssetID());
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
                // Get the StateDimensionEstimation for that dimension
                try {
                    // Throws an exception if SDE not found
                    StateDimensionEstimation currentEstimatedStateDimension = se.getStateDimensionEstimation(asd);

                    // Find the Variants of this Action currently being offered
                    Set offeredVariants = thisAction.getValuesOffered();
                    // Get the TechSpec data for all the Variants
                    Vector actionDescriptions = atsi.getActions();
                    // create the Action container that will hold the evaluations of all offered Variants
                    ActionEvaluation thisActionEvaluation = new ActionEvaluation(thisAction);
                    // iterate thru all offered Variants of this Action
                    Iterator variantIter = offeredVariants.iterator();                    
                    while (variantIter.hasNext()) {
                        ActionDescription thisVariantDescription = (ActionDescription) actionDescriptions.get(actionDescriptions.indexOf(variantIter.next()));
                        StateDimensionEstimation predictedStateDimensionEstimation = 
                                computePredictedStateDimensionEstimation(assetID, currentEstimatedStateDimension, thisVariantDescription);
                        PredictedCost predictedCost = 
                                computePredictedCost(assetID, currentEstimatedStateDimension, thisVariantDescription, knob);
                        double predictedBenefit = 
                                computePredictedBenefit(assetID, currentEstimatedStateDimension, predictedStateDimensionEstimation, knob);
                        thisActionEvaluation.addVariantEvaluation(
                                new VariantEvaluation(thisVariantDescription, predictedStateDimensionEstimation, predictedCost, predictedBenefit));  
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

    private StateDimensionEstimation computePredictedStateDimensionEstimation 
            (AssetID assetID, StateDimensionEstimation currentStateDimensionEstimation, ActionDescription variantDescription) 
        throws BelievabilityException {

        // set up the data structure for the projected state estimation if the Variant is selected
        StateDimensionEstimation predictedStateDimensionEstimation = 
            new StateDimensionEstimation(currentStateDimensionEstimation.getAssetModel(), currentStateDimensionEstimation.getStateDimension());
        Enumeration stateEnumeration = currentStateDimensionEstimation.getStateNames();
        while (stateEnumeration.hasMoreElements()) {
            predictedStateDimensionEstimation.setProbability((String)stateEnumeration.nextElement(), 0.0);
        }
        
        // compute the probability of each end state (in this dimension) based on estimate of current state & the variant's transitions
        Enumeration startStateEnumeration = currentStateDimensionEstimation.getStateNames();
        while (startStateEnumeration.hasMoreElements()) {
            String startStateName = (String) startStateEnumeration.nextElement();
            double startStateProb;
            double endStateProb;
            try {
                startStateProb = currentStateDimensionEstimation.getProbability(startStateName);
                }
            catch (BelievabilityException e) {
                // Should be impossible to get here if the ActionDescription is correctly populated
                throw new RuntimeException("Could not find an AssetState that the ActionDescription said t had");
                }
            AssetTransitionWithCost atwc = variantDescription.getTransitionForState(startStateName);
            AssetState endState = atwc.getEndValue();
            String endStateName = endState.getName();
            try {
                endStateProb = predictedStateDimensionEstimation.getProbability(endStateName);
                }
            catch (BelievabilityException e) {
                // Should be impossible to get here if the ActionDescription is correctly populated
                throw new RuntimeException("Could not find an AssetState that the ActionDescription said t had");
                }            
            predictedStateDimensionEstimation.setProbability(endStateName, endStateProb+startStateProb);
        }

      return predictedStateDimensionEstimation;
    }
       

    private PredictedCost computePredictedCost 
            (AssetID assetID, StateDimensionEstimation currentStateDimensionEstimation, ActionDescription variantDescription, CostBenefitKnob knob) {
        
        double horizon = knob.getHorizon();
        
        PredictedCost predictedCost = new PredictedCost();
        Enumeration stateEnumeration = currentStateDimensionEstimation.getStateNames();

        // FAKE METHODS TO GET DYNAMIC ASSET STATS - HOW IS THIS ACTUALLY DONE?????
        double memorySize = FOO(assetID);
        double bandwidthSize = BAR(assetID);
        
        while (stateEnumeration.hasMoreElements()) {
            String stateName = (String) stateEnumeration.nextElement();
            AssetTransitionWithCost atwc = variantDescription.getTransitionForState(stateName);
            double transitionProb;
            try {
                transitionProb = currentStateDimensionEstimation.getProbability(stateName);
                }
            catch (BelievabilityException e) {
                // Should be impossible to get here if the ActionDescription is correctly populated
                throw new RuntimeException("Could not find an AssetState that the ActionDescription said t had");
                }  

            double c;  //  a reuseable variable

            ActionCost.Cost oneTimeBandwidthCost = atwc.getOneTimeCost().getBandwidthCost();
            ActionCost.Cost continuingBandwidthCost = atwc.getContinuingCost().getBandwidthCost();
            c = computeIncrementalCost(oneTimeBandwidthCost, memorySize, bandwidthSize) 
                    + horizon * computeIncrementalCost(continuingBandwidthCost, memorySize, bandwidthSize);
            predictedCost.incrementBandwidthCost(transitionProb * c);

            ActionCost.Cost oneTimeMemoryCost = atwc.getOneTimeCost().getMemoryCost();
            ActionCost.Cost continuingMemoryCost = atwc.getContinuingCost().getMemoryCost();
            c = computeIncrementalCost(oneTimeMemoryCost, memorySize, bandwidthSize) 
                    + horizon * computeIncrementalCost(continuingMemoryCost, memorySize, bandwidthSize);
            predictedCost.incrementMemoryCost(transitionProb * c);

            ActionCost.Cost oneTimeCPUCost = atwc.getOneTimeCost().getCPUCost();
            ActionCost.Cost continuingCPUCost = atwc.getContinuingCost().getCPUCost();
            c = computeIncrementalCost(oneTimeCPUCost, memorySize, bandwidthSize) 
                    + horizon * computeIncrementalCost(continuingCPUCost, memorySize, bandwidthSize);
            predictedCost.incrementCPUCost(transitionProb * c);
            
            long oneTimeTimeCost = atwc.getOneTimeCost().getTimeCost();
            predictedCost.incrementTimeCost(transitionProb * oneTimeTimeCost);
        }
        
        return predictedCost;
    }

   private double computePredictedBenefit
            (AssetID assetID, StateDimensionEstimation currentStateDimensionEstimation, StateDimensionEstimation predictedStateDimensionEstimation, CostBenefitKnob knob) {

        return 0.0;
    }

    private double FOO(AssetID assetID) { return 1.0; }
    private double BAR(AssetID assetID) { return 1.0; }


    private double computeIncrementalCost(ActionCost.Cost thisCost, double memSize, double bandwidthSize) {
        double c = thisCost.getIntensity();
        if (!(thisCost.isAgentSizeAFactor()) || thisCost.isMessageSizeAFactor()) return c;
        double outCost = 0.0;
        if (thisCost.isAgentSizeAFactor()) outCost = outCost + c * memSize;
        if (thisCost.isMessageSizeAFactor()) outCost = outCost + c * bandwidthSize;
        return outCost;
    }

      
}

