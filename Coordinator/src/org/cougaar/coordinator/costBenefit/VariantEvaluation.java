/*
 * VariantEvluation.java
 *
 * Created on May 5, 2004, 10:03 AM
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
                             long expectedTransitionTime) {

        this.variantDescription = variantDescription;
        this.actionEvaluation = actionEvaluation;
        this.predictedCost = predictedCost;
        this.predictedCostPerTimeUnit = predictedCostPerTimeUnit;
        this.predictedBenefit = predictedBenefit;
        this.expectedTransitionTime = expectedTransitionTime;

    }

    public ActionDescription getVariant() { return variantDescription; }
    public String getVariantName() { return variantDescription.name(); }
    public double getPredictedCost() { return predictedCost; }
    public double getPredictedCostPerTimeUnit() { return predictedCostPerTimeUnit; }
    public double getPredictedBenefit() { return predictedBenefit; }
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
        return "      "+variantDescription.name().toString()+", Cost="+predictedCost+", Benefit="+predictedBenefit+", Time="+expectedTransitionTime+", Cost/Benefit="+getCostBenefitRatio()+", Chosen?: " + chosenP() +", Active?: " + activeP() +", Failed?: " + failedP() +" \n";
    }

    public int compareTo(java.lang.Object obj) {
        VariantEvaluation otherVE = (VariantEvaluation)obj;
        if (this.failedP()) {
            return otherVE.failedP() ? 0 : 1;
        }
        else if (otherVE.failedP()) return -1;
        else if (otherVE.getBenefitToCostRatio() < this.getBenefitToCostRatio()) return -1;
        else if (otherVE.getBenefitToCostRatio() == this.getBenefitToCostRatio()) return 0; 
        else return 1;
    }

}
