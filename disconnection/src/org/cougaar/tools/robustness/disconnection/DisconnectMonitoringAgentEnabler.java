/*
 * DisconnectMonitoringNodeEnabler.java
 *
 * Created on August 8, 2003, 8:14 AM
 *
 * @author David Wells - OBJS 
 *
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 *
 * </copyright> 
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

public class DisconnectMonitoringAgentEnabler extends DefenseOperatingMode {
      
    public static final UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof DisconnectMonitoringAgentEnabler);
        }
    };

        
        // searches the BB for an object of this type with a given signature 
    public static DisconnectMonitoringAgentEnabler find(String defenseName, String expandedName, BlackboardService blackboard) {

        DisconnectMonitoringAgentEnabler dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (DisconnectMonitoringAgentEnabler)iter.next();
           if (dc.compareSignature(expandedName, defenseName)) {
               return dc;
           }
        }
        return null;
    }      
    
      /** Creates a new instance of DisconnectMonitoringEnabler. This mode 
     *  supports two values: ENABLED and DISABLED. The default is set to DISABLED.
     *@param name - the name of the OperatingMode
     */
    public DisconnectMonitoringAgentEnabler(String assetType, String asset) {
        
        super(assetType, asset, DisconnectConstants.DEFENSE_NAME, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());

    }
    
    /*
     * @return the String value of the state of this mode.
     */
    public String getState() { 
        return getValue().toString();
    }
    


}
  
