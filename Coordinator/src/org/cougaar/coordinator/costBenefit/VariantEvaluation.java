/*
 * VariantEvluation.java
 *
 * Created on May 5, 2004, 10:03 AM
 *
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.coordinator.techspec.ActionDescription;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.believability.StateDimensionEstimation;

import java.util.Enumeration;


public class VariantEvaluation implements Comparable {

    private ActionDescription variantDescription;
    private ActionEvaluation actionEvaluation;
    private double predictedCost;
    private double predictedCostPerTimeUnit;
    private double predictedBenefit;
    private double predictedBenefitPerTimeUnit;
    private boolean chosen = false;
    private boolean active = false;
    private boolean failed = false;
    private long expectedTransitionTime;

    /** Creates new VariantEvaluation containing the cost & benefit for this variant under the current StateEstimation */
    public VariantEvaluation(ActionDescription variantDescription, 
                             ActionEvaluation actionEvaluation,
                             double predictedCost,
                             double predictedCostPerTimeUnit,
                             double predictedBenefit,
                             double predicetdBenefitPerTimeUnit,
                             long expectedTransitionTime) {

        this.variantDescription = variantDescription;
        this.actionEvaluation = actionEvaluation;
        this.predictedCost = predictedCost;
        this.predictedCostPerTimeUnit = predictedCostPerTimeUnit;
        this.predictedBenefit = predictedBenefit;
        this.predictedBenefitPerTimeUnit = predictedBenefitPerTimeUnit;
        this.expectedTransitionTime = expectedTransitionTime;

    }

    public ActionDescription getVariant() { return variantDescription; }
    public String getVariantName() { return variantDescription.name(); }
    public double getPredictedCost() { return predictedCost; }
    public double getPredictedCostPerTimeUnit() { return predictedCostPerTimeUnit; }
    public double getPredictedBenefit() { return predictedBenefit; }
    public double getPredictedBenefitPerTimeUnit() { return predictedBenefitPerTimeUnit; }
    public double getBenefitToCostRatio() {
        if (predictedCost == 0.0) return predictedBenefit;
        else return predictedBenefit/predictedCost;
    }
    public void setFailed() { failed = true; }
    public boolean failedP() { return failed; }
    protected void setFailed(boolean p) { failed = p; }
    public ActionEvaluation getActionEvaluation() { return actionEvaluation; }

    public void setChosen() { chosen = true; }
    public boolean chosenP() { return chosen; }
    protected void setChosen(boolean p) { chosen = p; }

    public void setActive() { active = true; }
    public boolean activeP() { return active; }
    protected void setActive(boolean p) { active = p; }

    public long getExpectedTransitionTime() { return expectedTransitionTime; }

    
    public String toString() {
        return "      "+variantDescription.name().toString()+", Cost="+predictedCost+", Cost/T="+predictedCostPerTimeUnit+", Benefit="+predictedBenefit+", Benefit/T="+predictedBenefitPerTimeUnit+", Time="+expectedTransitionTime+", Benefit/Cost="+getBenefitToCostRatio()+", Chosen?: " + chosenP() +", Active?: " + activeP() +", Failed?: " + failedP() +" \n";
    }

    public int compareTo(java.lang.Object obj) {
        VariantEvaluation otherVE = (VariantEvaluation)obj;
        if (this.failedP()) {
            return otherVE.failedP() ? 0 : 1;
        }
        else if (otherVE.failedP()) return -1;
        //else if (otherVE.getBenefitToCostRatio() < this.getBenefitToCostRatio()) return -1;  dlw - greedy selection based on the ratio doesn't work
        //else if (otherVE.getBenefitToCostRatio() == this.getBenefitToCostRatio()) return 0; 
        else if (otherVE.getPredictedBenefit() < this.getPredictedBenefit()) return -1;
        else if (otherVE.getPredictedBenefit() == this.getPredictedBenefit()) return 0; 
        else return 1;
    }

}
