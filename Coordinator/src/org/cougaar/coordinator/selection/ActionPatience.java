/*
 * ActionPatience.java
 *
 * Created on September 18, 2003, 2:06 PM
 */

package org.cougaar.coordinator.selection;

import org.cougaar.coordinator.*;

import org.cougaar.core.util.UID;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class ActionPatience extends DefenseTimeCondition  implements NotPersistable {
    
    boolean expired = false;

    /** Creates new ActionPatience */
    public ActionPatience(String defenseName, String assetName, String assetType, long time) {
        super (assetType, assetName,defenseName,  time);
    }

    public boolean expired() {
        return expired;
    }
    
    private static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionPatience);
            }
        };
    
    public static ActionPatience find(String defenseName, String expandedName, BlackboardService blackboard) {

        ActionPatience dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (ActionPatience)iter.next();
           if (dc.compareSignature(expandedName, defenseName)) {
               return dc;
           }
        }
        return null;
    }          

    public static ActionPatience find(UID uid, BlackboardService blackboard) {

        ActionPatience dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (ActionPatience)iter.next();
           if (dc.compareSignature(uid)) {
               return dc;
           }
        }
        return null;
    }  

}
