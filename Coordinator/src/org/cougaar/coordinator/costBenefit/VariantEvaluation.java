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


public class VariantEvaluation {

    private ActionDescription variantDescription;
    private double predictedCost;
    private double predictedBenefit;
    private boolean alreadyTried = false;
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
    public void setTried() { alreadyTried = true; }
    public boolean triedP() { return alreadyTried; }
    protected void setAlreadyTried(boolean p) { alreadyTried = p; }
    
    public void setSelectionScore(double score) { selectionScore = score; }
    public double getSelectionScore() { return selectionScore; }

    public long getExpectedTransitionTime() { return expectedTransitionTime; }

    
    public String toString() {
        return "Variant: "+variantDescription.name().toString()+", Predicted Aggregate Cost="+predictedCost+", Benefit="+predictedBenefit+", Predicted Max Time="+expectedTransitionTime+"\n";
    }

}
