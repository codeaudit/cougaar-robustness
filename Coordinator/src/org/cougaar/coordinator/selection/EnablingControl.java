/*
 * Class.java
 *
 * Created on July 6, 2004, 5:36 PM
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
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;


public class EnablingControl {

    private Action action;
    private CostBenefitEvaluation cbe;

    /** Creates new EnablingControl */
    public EnablingControl(Action action, CostBenefitEvaluation cbe) {
        this.action = action;
        this.cbe = cbe;
    }

    public Action getAction() { return action; };

    public CostBenefitEvaluation getCBE() { return cbe; }
}
