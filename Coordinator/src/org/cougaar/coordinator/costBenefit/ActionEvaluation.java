/*
 * ActionEvaluation.java
 *
 * Created on May 5, 2004, 9:42 AM
 */

package org.cougaar.coordinator.costBenefit;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.ActionDescription;


public class ActionEvaluation {

    private Action action;
    private Hashtable variantEvals;
//    private double selectionScore = -100000000.0;
    private VariantEvaluation bestAvailableVariant = null;

    /** Creates new ActionEvaluation */
    public ActionEvaluation(Action action) {
        this.action = action;
        variantEvals = new Hashtable();
    }

    public Action getAction() { return action; }

    
    protected void addVariantEvaluation(VariantEvaluation ve) { 
        variantEvals.put(ve.getVariantName(), ve);
    }
    
    public VariantEvaluation getVariantEvaluation(Object variant) {
        return (VariantEvaluation)variantEvals.get(variant);
    }

    public Hashtable getVariantEvaluations() {
        return variantEvals;
    }

//    public void setSelectionScore(double score) { selectionScore = score; }
//    public double getSelectionScore() { return selectionScore; }

    public String toString() {
        return variantEvals.toString();
    }
}
