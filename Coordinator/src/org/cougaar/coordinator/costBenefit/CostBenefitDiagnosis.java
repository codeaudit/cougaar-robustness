/*
 * CostBenefitDiagnosis.java
 *
 * Created on July 8, 2003, 4:17 PM
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

import java.util.Hashtable;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.persist.NotPersistable;


/**
 * Temporarily this object will use a hashtable to store the data
 * required by consumers of it.
 * 
 */
public class CostBenefitDiagnosis implements NotPersistable {
    
    private String asset;
    private String calculationMethod;
    private Vector list;
    private String status = null;
    
//    private DefenseApplicabilityCondition dac;
    
    /** Creates a new instance of CostBenefitDiagnosis */
    protected CostBenefitDiagnosis(String assetName, String calcMethod) {
        
        asset = assetName;
        calculationMethod = calcMethod;
        list = new Vector(5,5);
    }
        
    protected void addDefense(DefenseTechSpecInterface d, DefenseApplicabilityConditionSnapshot dac, double b, double believability, long horizon) {
//    protected void addDefense(DefenseTechSpecInterface d, double b, double believability) {
        
//        list.addElement(new DefenseBenefit( d, b, believability) );
        list.addElement(new DefenseBenefit( d, dac, b, believability, horizon) );
    }
    
    public String getAssetName() { return asset; }
    public String getCalculationMethod() { return calculationMethod; }
    
    //
    public DefenseBenefit[] getDefenses() {
        DefenseBenefit[] db = new DefenseBenefit[list.size()];
        list.copyInto(db); 
        return db;
    }
    
    public String toString() {
        String result = getAssetName()+"\n";
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            DefenseBenefit db = (DefenseBenefit)iter.next();
            result = result+"Defense="+db.getDefense().getName()+" - benefit="+db.getBenefit()+"\n";
        }
        return result;
    }
    
    public void   setStatus(String s) { status = s; }
    public String getStatus() { return status; }
    
    public class DefenseBenefit implements NotPersistable {
     
        private DefenseTechSpecInterface dtsi;
        private double benefit;
        private double orig_benefit;
        private DefenseApplicabilityConditionSnapshot dac;
        private double believability;
        private String myStatus = null;
        private String outcome = null;
        private double horizon = 0.0;
        private long timeout = 0;
        
        public DefenseBenefit(DefenseTechSpecInterface d, DefenseApplicabilityConditionSnapshot dac, double b, double believability, double horizon) {
        //public DefenseBenefit(DefenseTechSpecInterface d, double b, double believability) {
            dtsi = d;
            benefit = b;
            orig_benefit = b; //Since selection sets this to 0 after trying it, we want to preserve the original #.
            this.dac = dac;
            this.believability = believability;
            this.horizon = horizon;
        }
        
        public DefenseTechSpecInterface getDefense() { return dtsi; }
        public double getBenefit() { return benefit; }      
        public void setBenefit(double newBenefit) { benefit = newBenefit; }

        public double getOrigBenefit() { return orig_benefit; }      
        
        public double getBelievability() { return believability; }      
        
        public DefenseApplicabilityConditionSnapshot getCondition() { return dac; }
        
        /** Set the status - enabled / disabled - of this defense, by the DefenseSelectionPlugin */
        public void   setStatus(String s) { myStatus = s; }
        public String getStatus() { return myStatus; }

        /** Set the outcome of running this defense */
        public void   setOutcome(String s) { outcome = s; }
        public String getOutcome() { return outcome; }

        /** Set the horizon that was used for this defense */
        public void   setHorizon(double d) { horizon = d; }
        public double getHorizon() { return horizon; }

        /** Set the timeout that was used for this defense */
        public void setTimeout(long l) { timeout = l; }
        public long getTimeout() { return timeout; }
    }

    public final static UnaryPredicate pred = new UnaryPredicate() {
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
}

