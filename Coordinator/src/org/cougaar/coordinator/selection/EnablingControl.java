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


public class EnablingControl {

    private Action action;

    /** Creates new EnablingControl */
    public EnablingControl(Action action) {
        this.action = action;
    }

    public Action getAction() { return action; };
}
