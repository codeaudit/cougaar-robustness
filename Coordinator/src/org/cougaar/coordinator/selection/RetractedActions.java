/*
 * RetractedActions.java
 *
 * Created on July 6, 2004, 4:41 PM
 */

package org.cougaar.coordinator.selection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.core.persist.NotPersistable;
import java.util.Set;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;
import org.cougaar.util.UnaryPredicate;

public class RetractedActions extends EnablingControl implements NotPersistable {

    private Set actionVariants;

    /** Creates new RetractedActions */
    public RetractedActions(Action action, Set retractedVariants) {
        super(action);
        this.actionVariants = retractedVariants;
    }

    public RetractedActions(Action action) {
        super(action);
        this.actionVariants = null;
    }
    
    public Set getActionVariants() { return actionVariants; }

    public String toString() {
        String buff = "RetractedActions: ";
        buff = buff + super.getAction().toString() + "\n";
        buff = buff + "retracting: ";
        buff = buff + actionVariants == null?"ALL":actionVariants.toString() + "\n";
        return buff;
    }

    
    public static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof RetractedActions ) {
                return true ;
            }
            return false ;
        }
     };
     }
