/*
 * AgentStatus.java
 *
 * Created on July 29, 2004, 11:27 AM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import org.cougaar.util.UnaryPredicate;
import java.io.Serializable;
import org.cougaar.core.persist.Persistable;

public class AgentStatus extends Hashtable implements Persistable, Serializable {

    /** Creates new PersistableState */
    public AgentStatus() {
    }

    public boolean isPersistable() { return true; }

    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof AgentStatus ) {
                    return true ;
                }
                return false ;
            }
        };

}