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
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
//import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.believability.StateEstimation;

import org.cougaar.core.persist.NotPersistable;


/**
 * Temporarily this object will use a hashtable to store the data
 * required by consumers of it.
 * 
 */
public class CostBenefitEvaluation implements NotPersistable {
    
    private AssetID assetID;
    private String calcMethod;
    private long horizon;
    private StateEstimation se;
    private Hashtable actions;
//    private String status = null;
        
    /** Creates a new instance of CostBenefitDiagnosis */
    protected CostBenefitEvaluation(AssetID assetID, String calcMethod, long horizon, StateEstimation se) {
        
        this.assetID = assetID;
        calcMethod = calcMethod;
        this.horizon = horizon;
        this.se = se;
        actions = new Hashtable();
    }
        
    protected void addActionEvaluation(ActionEvaluation ae) { actions.put(ae.getAction(), ae); }    
    public String getAssetName() { return assetID.getName(); }
    public AssetType getAssetType() { return assetID.getType(); }
    public AssetID getAssetID() { return assetID; }
    public String getCalcMethod() { return calcMethod; }
    public double getHorizon() { return horizon; }
    public StateEstimation getStateEstimation() { return se; }


    //
    public Hashtable getActionEvaluations() {
        return actions;
    }
    
    public String toString() {
        String result = assetID.toString()+"\n";
        Iterator iter = getActionEvaluations().values().iterator();
        while (iter.hasNext()) {
            ActionEvaluation thisAction = (ActionEvaluation)iter.next();
            result = result+"    "+thisAction.toString()+"\n";
        }
        return result;
    }
    


    public final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof CostBenefitEvaluation);
            }
        };
}

