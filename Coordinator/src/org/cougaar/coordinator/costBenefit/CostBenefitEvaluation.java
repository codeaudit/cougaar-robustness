/*
 * CostBenefitDiagnosis.java
 *
 * Created on July 8, 2003, 4:17 PM
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */


package org.cougaar.coordinator.costBenefit;

import java.util.Hashtable;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.believability.StateEstimation;
import org.cougaar.coordinator.Action;

import org.cougaar.core.persist.NotPersistable;


/**
 * Temporarily this object will use a hashtable to store the data
 * required by consumers of it.
 * 
 */
public class CostBenefitEvaluation implements NotPersistable {
    
    private AssetID assetID;
    private long horizon;
    private StateEstimation se;
    private Hashtable actionEvaluations;
    private SortedSet orderedEvaluations = null;
    private int openActions = 0;
    private int numSelectedActions = 0;
    private int numCompletedActions = 0;
        
    /** Creates a new instance of CostBenefitDiagnosis */
    protected CostBenefitEvaluation(AssetID assetID, long horizon, StateEstimation se) {
        
        this.assetID = assetID;
        this.horizon = horizon;
        this.se = se;
        actionEvaluations = new Hashtable();
    }
        
    protected void addActionEvaluation(ActionEvaluation ae) { actionEvaluations.put(ae.getAction(), ae); }    
    public AssetID getAssetID() { return assetID; }
    public double getHorizon() { return horizon; }
    public StateEstimation getStateEstimation() { return se; }
    public Hashtable getActionEvaluations() { return actionEvaluations; }
    public ActionEvaluation getActionEvaluation(Action action) { return (ActionEvaluation)actionEvaluations.get(action); }
    public int numOpenActions() { return openActions; }
    public void setNumOpenActions(int n) { openActions = n; }

    public void actionSelected() { numSelectedActions++; }
    public void actionRetracted() { numSelectedActions--; }
    public void actionCompleted() { numCompletedActions++; }
    public boolean noOutstandingSelectionsP() {
        return (numSelectedActions == numCompletedActions)?true:false; 
    }

    public void setOrderedEvaluations(SortedSet orderedEvaluations) { this.orderedEvaluations = orderedEvaluations; }
    public SortedSet getOrderedEvaluations() { return orderedEvaluations; }
    
    public String toString() {
        String result = "CBE for: " + assetID.toString()+"\n";
        result = result + "Horizon: " + horizon + "\n";
        Iterator iter = getActionEvaluations().values().iterator();
        result = result + "    All Actions & Variants: \n:";
        while (iter.hasNext()) {
            ActionEvaluation thisAction = (ActionEvaluation)iter.next();
            result = result + thisAction.toString()+"\n";
        }
        if (orderedEvaluations != null) {
            iter = orderedEvaluations.iterator();
            result = result + "    Available Actions: \n";
            while (iter.hasNext()) {
                result = result + ((ActionEvaluation)iter.next()).toString();
            }
        }
        else
            result = result + "    Ordering of Variants not Computed Yet \n";        return result;
    }

    public String dumpAvailableVariants() {
        String result = "CBE for: " + assetID.toString()+"\n";
        Iterator iter = getActionEvaluations().values().iterator();
        if (orderedEvaluations != null) {
            iter = orderedEvaluations.iterator();
            result = result + "    Available Actions: \n";
            while (iter.hasNext()) {
                result = result + ((ActionEvaluation)iter.next()).dumpAvailableVariants();
            }
        }
        else
            result = result + "    Ordering of Variants not Computed Yet \n";        return result;
    }

    public final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof CostBenefitEvaluation);
            }
        };
}

