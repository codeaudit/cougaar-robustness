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


public class ActionEvaluation {

    private Action action;
    private Hashtable variants;
    private double selectionScore;

    /** Creates new ActionEvaluation */
    public ActionEvaluation(Action action) {
        this.action = action;
        variants = new Hashtable();
    }

    public Action getAction() { return action; }

    
    protected void addVariantEvaluation(VariantEvaluation ve) { 
        variants.put(ve.getVariantName(), ve);
    }


    public Collection getVariants() {
        return variants.values();
    }

    public void setSelectionScore(double score) { selectionScore = score; }
    public double getSelectionScore() { return selectionScore; }

}
