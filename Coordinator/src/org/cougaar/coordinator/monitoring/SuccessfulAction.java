/*
 * SuccessfulAction.java
 *
 * Created on June 8, 2004, 1:47 PM
 */

package org.cougaar.coordinator.monitoring;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.coordinator.Action;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class SuccessfulAction implements NotPersistable {

    private Action a;

    /** Creates new SuccessfulAction */
    public SuccessfulAction(Action a) {
        this.a = a;
    }

    public Action getAction() { return a; }

    public String toString() {
        return "<SuccessfulAction> for: \n "
                + a.toString();
    }

}
