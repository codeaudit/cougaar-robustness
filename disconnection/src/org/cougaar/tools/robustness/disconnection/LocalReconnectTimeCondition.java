/*
 * LocalReconnectTimeCondition.java
 *
 * Created on August 20, 2003, 8:31 PM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class LocalReconnectTimeCondition extends DefenseTimeCondition {
    
    // searches the BB for an object of this type with a given signature 
    public static LocalReconnectTimeCondition findOnBlackboard(String assetType, String assetID, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof LocalReconnectTimeCondition);
            }
        };

        LocalReconnectTimeCondition rtc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           rtc = (LocalReconnectTimeCondition)iter.next();
           if (rtc.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) {
               return rtc;
           }
        }
        return null;
    }  
    
    /** Creates new LocalReconnectTimeCondition */
    
    public LocalReconnectTimeCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    }

    public void setTime(Double newValue) {
         super.setValue(newValue);
    }
    
}
