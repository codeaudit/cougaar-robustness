/*
 * FailedAction.java
 *
 * Created on April 28, 2004, 2:43 PM
 */

package org.cougaar.coordinator.monitoring;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.Action;

public class FailedActionWrapper {

    private Action action;

    /** Creates new FailedAction */
    public FailedActionWrapper(Action action) {
        this.action = action;
    }

    public Action getAction() { return action; }
    

    public final static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof FailedActionWrapper);
        }
    };

}
