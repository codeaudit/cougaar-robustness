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

  public class DisconnectMonitoringAgentEnabler extends MonitoringEnablingOperatingMode {
      public DisconnectMonitoringAgentEnabler(String assetType, String assetID) {
      super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    } 
  }
  
