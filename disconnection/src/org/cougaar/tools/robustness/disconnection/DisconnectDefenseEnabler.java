/*
 * Class.java
 *
 * Created on August 6, 2003, 8:29 AM
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

    
  public class DisconnectDefenseEnabler extends DefenseEnablingOperatingMode {
     public DisconnectDefenseEnabler(String assetType, String assetID) {
      super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    } 
    
    
    // searches the BB for an object of this type with a given signature 
    public static DisconnectDefenseEnabler findOnBlackboard(String assetType, String assetID, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DisconnectDefenseEnabler);
            }
        };

        DisconnectDefenseEnabler rtc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           rtc = (DisconnectDefenseEnabler)iter.next();
           if (rtc.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) {
               return rtc;
           }
        }
        return null;
    }   
    
  }
