/*
 * DisconnectMonitoringNodeEnabler.java
 *
 * Created on August 8, 2003, 8:14 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
/**
 *
 * @author  administrator
 * @version 
 */

  public class DisconnectMonitoringAgentEnabler extends DefenseOperatingMode {
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
  
