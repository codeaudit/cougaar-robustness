/*
 * ActionPatience.java
 *
 * Created on September 18, 2003, 2:06 PM
 */

package org.cougaar.coordinator.activation;

import org.cougaar.core.util.UID;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class ActionPatience implements NotPersistable {

    Action action;
    Set allowedVariants;
    Action.CompletionCode result = null;
    long timeoutTime;
    long startTime;
    CostBenefitEvaluation cbe;

    /** Creates new ActionPatience */
    public ActionPatience(Action action, Set allowedVariants, long timeoutTime, CostBenefitEvaluation cbe) {
        this.action = action;
        this.allowedVariants = allowedVariants;
        this.timeoutTime = timeoutTime;
        startTime = System.currentTimeMillis();
        this.cbe = cbe;
    }

    public Action getAction() { return action; }

    public Set getAllowedVariants() { return allowedVariants; }

    public long getDuration() { return timeoutTime; }

    public long getStartTime() { return startTime; }

    public void setResult(Action.CompletionCode result) { this.result = result; }

    public Action.CompletionCode getResult() { return result; }

    public CostBenefitEvaluation getCBE() { return cbe; }

    public String toString() {
        String buff = "<ActionPatience: \n";
        buff = buff + action.toString() + "\n";
        buff = buff + "waiting for: " + allowedVariants.toString() + "\n";
        buff = buff + "startTime=" + getStartTime() + ", duration=" + getDuration() + "\n";
        return buff;
    }
    
    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionPatience);
            }
        };

}
