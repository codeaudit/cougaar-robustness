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
    private double predictedCost;
    private double predictedBenefit;
    private boolean chosen = false;
    private boolean active = false;
    private boolean failed = false;
    private long expectedTransitionTime;
    private double selectionScore = -100000000.0;

    /** Creates new VariantEvaluation containing the cost & benefit for this variant under the current StateEstimation */
    public VariantEvaluation(ActionDescription variantDescription, 
                             double predictedCost, 
                             double predictedBenefit,
                             long expectedTransitionTime) {

        this.variantDescription = variantDescription;
        this.predictedCost = predictedCost;
        this.predictedBenefit = predictedBenefit;
        this.expectedTransitionTime = expectedTransitionTime;

    }

    public ActionDescription getVariant() { return variantDescription; }
    public String getVariantName() { return variantDescription.name(); }
    public double getPredictedCost() { return predictedCost; }
    public double getPredictedBenefit() { return predictedBenefit; }
    public void setFailed() { failed = true; }
    public boolean failedP() { return failed; }
    protected void setFailed(boolean p) { failed = p; }

    public void setChosen() { chosen = true; }
    public boolean chosenP() { return chosen; }
    protected void setChosen(boolean p) { chosen = p; }

    public void setActive() { active = true; }
    public boolean activeP() { return active; }
    protected void setActive(boolean p) { active = p; }

    public void setSelectionScore(double score) { selectionScore = score; }
    public double getSelectionScore() { return selectionScore; }

    public long getExpectedTransitionTime() { return expectedTransitionTime; }

    
    public String toString() {
        return "      "+variantDescription.name().toString()+", Cost="+predictedCost+", Benefit="+predictedBenefit+", Time="+expectedTransitionTime+", Chosen?: " + chosenP() +", Active?: " + activeP() +", Failed?: " + failedP() +" \n";
    }

    public int compareTo(java.lang.Object obj) {
        VariantEvaluation otherVE = (VariantEvaluation)obj;
        if (this.failedP()) {
            return otherVE.failedP() ? 0 : 1;
        }
        else if (otherVE.failedP()) return -1;
        else if (otherVE.getPredictedBenefit() < this.getPredictedBenefit()) return -1;
        else if (otherVE.getPredictedBenefit() == this.getPredictedBenefit()) return 0; 
        else return 1;
    }

}
