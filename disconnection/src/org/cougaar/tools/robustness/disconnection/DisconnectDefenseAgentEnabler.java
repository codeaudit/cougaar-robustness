/*
 * DisconnectDefenseNodeEnabler.java
 *
 * Created on August 8, 2003, 8:15 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;


/**
 *
 * @author  administrator
 * @version 
 */

import org.cougaar.core.adaptivity.OMCPoint;
import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;

/**
 * The Defense publishes a DefenseEnablingOperatingMode for each asset it may defend.
 * The DefenseEnablingOperatingMode is used to control the actions of the Defense as
 * described in the Defense Deconfliction API & Architecture paper. The Defense does
 * <b>NOT</b> change the values of this instance. It should only call getState() to find
 * what the value is & then act accordingly.
 */
public class DisconnectDefenseAgentEnabler extends DefenseOperatingMode {
    
    // searches the BB for an object of this type with a given signature 
    public static DisconnectDefenseAgentEnabler findOnBlackboard(String assetType, String assetID, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DisconnectDefenseAgentEnabler);
            }
        };

        DisconnectDefenseAgentEnabler rtc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           rtc = (DisconnectDefenseAgentEnabler)iter.next();
           if (rtc.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) {
               return rtc;
           }
        }
        return null;
    }   
    
    
    /** Creates a new instance of DisconnectDefenseEnabler. This mode 
     *  supports two values: ENABLED and DISABLED. The default is set to DISABLED.
     *@param name - the name of the OperatingMode
     */
    public DisconnectDefenseAgentEnabler(String assetType, String asset) {
        
        super(assetType, asset, DisconnectConstants.DEFENSE_NAME, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());

    }
    
    /*
     * @return the String value of the state of this mode.
     */
    public String getState() { 
        return getValue().toString();
    }
    

}
  
