package org.cougaar.tools.robustness.disconnection;

/*
 * PersistableState.java
 *
 * Created on July 25, 2004, 11:13 PM
 */

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import org.cougaar.util.UnaryPredicate;
import java.io.Serializable;
import org.cougaar.core.persist.Persistable;

public class NodeStatus extends Hashtable implements Persistable, Serializable {

    /** Creates new PersistableState */
    public NodeStatus() {
    }

    public boolean isPersistable() { return true; }

    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof NodeStatus ) {
                    return true ;
                }
                return false ;
            }
        };

}
