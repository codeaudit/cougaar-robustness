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


import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
import org.cougaar.coordinator.DeconflictionPluginBase;

import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.coordinator.policy.DefensePolicy;
import org.cougaar.coordinator.selection.SelectedAction;
import org.cougaar.coordinator.believability.StateEstimation;
import org.cougaar.coordinator.believability.AssetBeliefState;
import org.cougaar.coordinator.believability.NoSuchBeliefStateException;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

import java.util.Enumeration;



import org.cougaar.core.persist.NotPersistable;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;
 


/**
 * This Plugin is used to handle the cost benefit functions for Defense Deconfliction
 * It emits CostBenefitDiagnosis objects.
 *
 */
public class CostBenefitPlugin extends DeconflictionPluginBase implements NotPersistable {

    
    private IncrementalSubscription stateEstimationSubscription;    
    private IncrementalSubscription defenseTechSpecsSubscription;
    private IncrementalSubscription threatModelSubscription;
    private IncrementalSubscription defensePolicySubscription;
    private IncrementalSubscription selectedActionSubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private Hashtable defenses;
    
    public static final String CALC_METHOD = "SIMPLE";
    public CostBenefitKnob knob;
    
    /** 
      * Creates a new instance of CostBenefitPlugin 
      */
    public CostBenefitPlugin() {
        super();
        
    }
    

    /**
      * Demonstrates how to read in parameters passed in via configuration files. Use/remove as needed. 
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
             logger.debug("Parameter = " + (String)iter.next());
        }
    }       

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams();
        
        defenses = new Hashtable(10);
        
        knob = new CostBenefitKnob();
    }
    


    protected void execute() {

        //***************************************************DefenseTechSpecs
        //Handle the addition of new DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsa = (DefenseTechSpecInterface)iter.next();            
            
            //Process - by adding this defense to our hashtable            
            defenses.put(dtsa.getName(), dtsa);
        }

        //Handle the modification of DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsc = (DefenseTechSpecInterface)iter.next();
            //Remove old one & add new one
            defenses.remove(dtsc.getName());
            defenses.put(dtsc.getName(), dtsc);
        }
        
        //Handle the removal of DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsr = (DefenseTechSpecInterface)iter.next();
            //Process - remove from our hashtable
            defenses.remove(dtsr.getName());
        }

        //***************************************************StateEstimation
        
        //Handle the addition of new StateEstimations
        for ( Iterator iter = stateEstimationSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            StateEstimation sea = (StateEstimation)iter.next();
            //Process - a new SE object - compute the benefits for each defense & publish a CostBenefitDiagnosis

            String err = sea.hasError() ? sea.getErrorMessage() : "none";
            String out = "New StateEstimation with "+sea.size()+" AssetBeliefStates -- Error:"+err+"\n";
            
            if (logger.isInfoEnabled()) logger.info(sea.toString());
            
            CostBenefitDiagnosis cbd = null;
            
            Enumeration states = sea.elements();
            while (states.hasMoreElements() ) {
                
                AssetBeliefState abs = (AssetBeliefState)states.nextElement();                
                //We cannot create this until we have info from one of the asset belief states. Only create 
                //one per state estimation
                
                if (cbd == null) { cbd = new CostBenefitDiagnosis(abs.getAssetName(), CALC_METHOD); }
                
                //Now, look at the AssetBeliefState, get the defense condition, the defense
                                
                String assetStateName = abs.getAssetStateDescName();
                out += "-- asset="+ abs.getAssetName()+ ", state descriptor="+abs.getAssetStateDescName()+"\n";

                
                //DefenseApplicabilityCondition dac = (DefenseApplicabilityCondition)abs.getDefenseCondition(); 
                //DefaultDefenseTechSpec 
                                
                Vector defenses = findDefenseByAssetStateName(assetStateName);
                if (defenses == null | defenses.size() == 0) { //ouch!
                    logger.warn("Could not find any defense tech specs for AssetStateName. Name = " + assetStateName );
                    continue;
                }                                
            
                DefenseTechSpecInterface dtsi;
                for (Iterator i = defenses.iterator(); i.hasNext();) {
                    
                    dtsi  = (DefenseTechSpecInterface)i.next();
                    
                    //Get believability from asset belief state
                    double beliefProb = getBelievability(abs);
                    
                    //Compute the expected total benefit
                    double d = computeBenefit(beliefProb, dtsi);

                    out += "---- defense="+ dtsi.getName()+ ", beliefProb="+beliefProb+", benefit="+d+"\n";
                    
                    DefenseApplicabilityConditionSnapshot dac = DefenseApplicabilityConditionSnapshot.find( dtsi.getName(), cbd.getAssetName(), sea.getDefenseConditions());
                    if (dac == null) {
                        logger.warn("Could not find Applicability condition for defense="+dtsi.getName()+", asset="+cbd.getAssetName());
                    }
                    cbd.addDefense(dtsi, dac, d, beliefProb, knob.getHorizon() );
                    //cbd.addDefense(dtsi, d, beliefProb);
                }            
            } 
            logger.debug(out);
            
            //******************************************************Publishing to the BB
            //Adding a CostBenefitDiagnosis to the BB
            logger.warn("Publishing CostBenefitDiagnosis: \n" + cbd);
            publishAdd(cbd);
            
        }


        //***************************************************DefensePolicy
        //Handle the addition of new DefensePolicies
        /*
        for ( Iterator iter = defensePolicySubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpa = (DefensePolicy)iter.next();
            //Process
        }

        //Handle the modification of DefensePolicies
        for ( Iterator iter = defensePolicySubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpc = (DefensePolicy)iter.next();
            //Process
        }
        
        //Handle the removal of DefensePolicies
        for ( Iterator iter = defensePolicySubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpr = (DefensePolicy)iter.next();
            //Process
        }
        */
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
        //***************************************************SelectedActions
        /*
        //Handle the addition of new SelectedActions
        for ( Iterator iter = selectedActionSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            SelectedAction tma = (SelectedAction)iter.next();
            //Process
        }

        //Handle the modification of SelectedActions
        for ( Iterator iter = selectedActionSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            SelectedAction tmc = (SelectedAction)iter.next();
            //Process
        }
        
        //Handle the removal of SelectedActions
        for ( Iterator iter = selectedActionSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            SelectedAction tmr = (SelectedAction)iter.next();
            //Process
        }
        */
        




           
    }
    
    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

        stateEstimationSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof StateEstimation) {
                    return true ;
                }
                return false ;
            }
        }) ;
        

        defenseTechSpecsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseTechSpecInterface) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
/*        
        threatModelSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ThreatModelInterface) {
                    return true ;
                }
                return false ;
            }
        }) ;

        defensePolicySubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefensePolicy) {
                    return true ;
                }
                return false ;
            }
        }) ;

        //Currently not referenced in execute() method. Use as required.
        selectedActionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof SelectedAction) {
                    return true ;
                }
                return false ;
            }
        }) ;
*/
        
        //*********************
        //Not used at this time - Will be used to provide out-of-band control of this plugin
        //*********************
        pluginControlSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof CostBenefitKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        publishAdd(knob);





    }
    
 
    private final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof CostBenefitDiagnosis);
            }
        };
    
    public static CostBenefitDiagnosis find(String expandedName, BlackboardService blackboard) {
 
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        CostBenefitDiagnosis cbd = null;
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof CostBenefitDiagnosis) {
               cbd = (CostBenefitDiagnosis) o;
               if (cbd.getAssetName().equals(expandedName)) {
                return cbd;
               }
           }
        }
        return null;
    }     

    /**
     * Extracts the believability for the AssetBeliefState 
     *
     * @return the believability for the AssetBeliefState 
     */
    private double getBelievability(AssetBeliefState abs) { 
    
        //extract prob from AssetBeliefState
        Enumeration e = abs.getStateNames();
        int count = 0;
        double  okProb = 0;
        double badProb = 0;
        boolean hasBadProb = false;
        boolean hasOKProb = false;
        while (e.hasMoreElements()) {
            count++;
            String s = (String)e.nextElement();
            if (s.equals("OK")) {
                try {
                    okProb = abs.get("OK");
                    hasOKProb = true;
                } catch (NoSuchBeliefStateException ne) {
                    logger.warn("***AssetBeliefState had no OK probability: " + abs.toString() );                    
                }
            } else {
                try {
                    badProb = abs.get(s);
                    hasBadProb = true;
                } catch (NoSuchBeliefStateException ne) {
                    if (hasOKProb) { badProb = 1 - okProb; } // then just compute it from okProb
                    else { badProb = 0; }
                    logger.warn("***AssetBeliefState had no bad probability: " + abs.toString() );                    
                }
            }
        }

        if (count > 2) { //we had more than two states!
            logger.warn("***AssetBeliefState had more than two states: " + abs.toString() );
        } else if (count == 0) { //we had NO states!
            logger.error("***AssetBeliefState had NO states. Ignoring." );
            return 0;
        }

        return badProb;
    }    
    
    private double computeBenefit(double badProb, DefenseTechSpecInterface dtsi) { 
    
        try {            
            
            double expectedBenefitPerUnitTime = dtsi.t_getBenefit() * badProb;
            double expectedTotalBenefit = expectedBenefitPerUnitTime * knob.getHorizon() - dtsi.t_getCost();

            return expectedTotalBenefit;
            
        } catch (Exception e) {
            logger.warn("Request to compute benefit caused exception (returning 0 benefit). Defense name = " + dtsi.getName(), e );
            return 0;
        }
        
    }
    
    
    /** 
      * Locate the DefenseTechSpecInterface(s) matching the supplied string 
      */
    private Vector findDefenseByAssetStateName(String assetStateName) {
        
        Vector results = new Vector();
        
        for (Enumeration e = defenses.elements(); e.hasMoreElements(); ) {
            DefenseTechSpecInterface dtsi = (DefenseTechSpecInterface)e.nextElement();
            if (assetStateName.equals(dtsi.getAffectedAssetState().getStateName()) ) {
                results.addElement(dtsi);
            }
        }
        return results;
        
    }


    /** 
      * Locate the DefenseTechSpecInterface matching the supplied string 
      */
    private DefenseTechSpecInterface findDefense(String defenseName) {
        
        for (Enumeration e = defenses.elements(); e.hasMoreElements(); ) {
            DefenseTechSpecInterface dtsi = (DefenseTechSpecInterface)e.nextElement();
            if (defenseName.equals(dtsi.getName()) ) {
                return dtsi;
            }
        }
        return null;
        
    }
    

}

