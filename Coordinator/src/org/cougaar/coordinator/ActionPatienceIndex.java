/*
 * ActionPatienceIndex.java
 *
 * Created on June 7, 2004, 2:04 PM
 */

/**
 *
 * @author  Administrator
 * @version 
 */
package org.cougaar.coordinator;
/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.activation.ActionPatience;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.persist.NotPersistable;

public class ActionPatienceIndex implements NotPersistable {

    private Hashtable entries = new Hashtable();

    /** Creates new Class */
    public ActionPatienceIndex() {
    }
    
    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionPatienceIndex);
            }
        };

    protected ActionPatience indexActionPatience(ActionPatience ap) {
        return (ActionPatience) entries.put(ap.getAction(), ap);
    }
    
    protected ActionPatience findActionPatience(Action a) {
        return (ActionPatience) entries.get(a);
    }  

    protected ActionPatience removeActionPatience(ActionPatience ap) {
        return (ActionPatience) entries.remove(ap);
    }  

}
