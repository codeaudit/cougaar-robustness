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
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.ActionDescription;


public class ActionEvaluation implements Comparable {

    private Action action;
    private Hashtable variantEvals;
    private SortedSet orderedAvailableVariants =  null;

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

    public SortedSet getOrderedAvaliableVariants() {
        return orderedAvailableVariants;
    }
    
    public void setOrderedAvailableVariants(SortedSet oav) { orderedAvailableVariants = oav; }

//    public void setSelectionScore(double score) { selectionScore = score; }
//    public double getSelectionScore() { return selectionScore; }

    public String toString() {
        String result = "  " + action.toString() + ":\n";
        Iterator iter = variantEvals.values().iterator();
        result = result + "    Known Variants: \n";
        while (iter.hasNext()) {
            result = result + ((VariantEvaluation)iter.next()).toString();
        }
        if (orderedAvailableVariants != null) {
            iter = orderedAvailableVariants.iterator();
            result = result + "    Available Variants: \n";
            while (iter.hasNext()) {
                result = result + ((VariantEvaluation)iter.next()).toString();
            }
        }
        else
            result = result + "    Ordering of Variants not Computed Yet \n";
        return result;
    }

    public String dumpAvailableVariants() {
        String result = "  " + action.toString() + ":\n";
        Iterator iter = variantEvals.values().iterator();
        if (orderedAvailableVariants != null) {
            iter = orderedAvailableVariants.iterator();
            result = result + "    Available Variants: \n";
            while (iter.hasNext()) {
                result = result + ((VariantEvaluation)iter.next()).toString();
            }
        }
        else
            result = result + "    Ordering of Variants not Computed Yet \n";
        return result;
    }


    public int compareTo(Object obj) {
        ActionEvaluation otherAE = (ActionEvaluation)obj;
        double thisBestBenefit = -999999999999999.0;
        double otherBestBenefit = -999999999999999.0;
        if ((this.getOrderedAvaliableVariants() != null) && (!this.getOrderedAvaliableVariants().isEmpty())) {
            VariantEvaluation thisBestVariant = (VariantEvaluation)this.getOrderedAvaliableVariants().first();
            thisBestBenefit = thisBestVariant.getPredictedBenefit();
        }
        if ((otherAE.getOrderedAvaliableVariants() != null) && (!otherAE.getOrderedAvaliableVariants().isEmpty())) {
            VariantEvaluation otherBestVariant = (VariantEvaluation)otherAE.getOrderedAvaliableVariants().first();
            otherBestBenefit = otherBestVariant.getPredictedBenefit();
        }
        if (thisBestBenefit > otherBestBenefit) return -1;
        else if (thisBestBenefit == otherBestBenefit) return 0; 
        else return 1;
    }

    public boolean mustSelectOne() {   
        // determines whether the selection of one variant for this Action is required
        // this should be based on TechSpecs
        if (this.getAction().getClass().getName().equals("org.cougaar.mts.std.LinksEnablingAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.coordinator.security.AgentCompromiseAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.CompressionAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.RMIAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.FuseResetAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.AttackResetAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.core.security.coordinator.SecurityLevelAction")) return true;
        return false;
    }

    public boolean doesNotConflict(VariantEvaluation ve, Collection activeActions, Collection selectedVariants) {   
        // determines whether the selection of this variant for this Action precludes the selection of some other Action
        // this should be based on TechSpecs
        // basic outline is to return TRUE if the action (or variant of the action) cannot conflict with anything
        //   to teturn FALSE if the action variant conflicts with something already active or selected
        //   and to return TRUE if neither of the above is true
        if (activeActions.isEmpty() && selectedVariants.isEmpty()) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.coordinator.security.AgentCompromiseAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.CompressionAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.RMIAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.FuseResetAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.robustness.dos.coordinator.AttackResetAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.core.security.coordinator.SecurityLevelAction")) return true;
        if (this.getAction().getClass().getName().equals("org.cougaar.mts.std.LinksEnablingAction")) {
            if (ve.getVariantName().equals("Disable")) return true;
            else {
                if (conflictWith("org.cougaar.tools.robustness.ma.util.AgentRestartAction", "Yes", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.tools.robustness.disconnection.NodeDisconnectAction", "Allow_Disconnect", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.coordinator.security.AgentCompromiseAction", "Restart", activeActions, selectedVariants)) return false;
            }
        }
        if (this.getAction().getClass().getName().equals("org.cougaar.tools.robustness.ma.util.AgentRestartAction")) {
            if (ve.getVariantName().equals("No")) return true;
            // NOTE - not clear what conflict semantics we want between Restrat & MsgLog
            else {
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "Normal", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "AlternateDirect", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "StoreAndForward", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.tools.robustness.disconnection.NodeDisconnectAction", "Allow_Disconnect", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.coordinator.security.AgentCompromiseAction", "Restart", activeActions, selectedVariants)) return false;
            }
        }
        if (this.getAction().getClass().getName().equals("org.cougaar.coordinator.security.AgentCompromiseAction")) {
            if (ve.getVariantName().equals("DoNothing")) return true;
            else {
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "Normal", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "AlternateDirect", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.mts.std.LinksEnablingAction", "StoreAndForward", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.tools.robustness.ma.util.AgentRestartAction", "Yes", activeActions, selectedVariants)) return false;
                if (conflictWith("org.cougaar.tools.robustness.disconnection.NodeDisconnectAction", "Allow_Disconnect", activeActions, selectedVariants)) return false;
            }
        }
        return true;
    }

    public boolean hasPositiveBenefit() { return hasOfferedVariant() && (((VariantEvaluation)getOrderedAvaliableVariants().first()).getPredictedBenefit() > 0.0); }

    public boolean hasOfferedVariant() { return (getOrderedAvaliableVariants() != null && (!getOrderedAvaliableVariants().isEmpty())); }

    private boolean conflictWith(String actionName, String variantName, Collection activeActions, Collection selectedVariants) {
        if ((alreadyActive(actionName, variantName, activeActions)) || (alreadySelected(actionName, variantName, selectedVariants))) return true;
        else return false;
    }

    private boolean alreadySelected(String actionName, String variantName, Collection selectedVariants) {
        Iterator iter = selectedVariants.iterator();
        while (iter.hasNext()) {
            VariantEvaluation thisVariant = (VariantEvaluation)iter.next();
            if (thisVariant.getActionEvaluation().getAction().getClass().getName().equals(actionName) && thisVariant.getVariantName().equals(variantName)) return true;
        }
        return false;
    }

    private boolean alreadyActive(String actionName, String variantName, Collection activeActions) {
        Iterator iter = activeActions.iterator();
        while (iter.hasNext()) {
            Action thisAction = (Action) iter.next();
            if (thisAction.getClass().getName().equals(actionName) && thisAction.getValue().equals(variantName)) return true;
        }
        return false;
    }

}
