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
    private StateDimensionEstimation predictedStateDimensionEstimation;
    private PredictedCost predictedCost;
    private double predictedBenefit;
    private boolean alreadyTried = false;
    private double selectionScore;

    /** Creates new VariantEvaluation containing the cost & benefit for this variant under the current StateEstimation */
    public VariantEvaluation(ActionDescription variantDescription, 
                             StateDimensionEstimation predictedStateDimensionEstimation, 
                             PredictedCost predictedCost, 
                             double predictedBenefit) {

        this.variantDescription = variantDescription;
        this.predictedStateDimensionEstimation = predictedStateDimensionEstimation;
        this.predictedCost = predictedCost;
        this.predictedBenefit = predictedBenefit;

    }


    public ActionDescription getVariant() { return variantDescription; }
    public String getVariantName() { return variantDescription.name(); }
    public PredictedCost getPredictedCost() { return predictedCost; }
    public double getPredictedBenefit() { return predictedBenefit; }
    public boolean triedP() { return alreadyTried; }
    protected void setAlreadyTried(boolean p) { alreadyTried = p; }
    
    public void setSelectionScore(double score) { selectionScore = score; }
    public double getSelectionScore() { return selectionScore; }

    
    public String toString() {
        return "Variant: "+variantDescription.name().toString()+", Cost="+predictedCost.toString()+", Benefit="+predictedBenefit+"\n";
    }

}
