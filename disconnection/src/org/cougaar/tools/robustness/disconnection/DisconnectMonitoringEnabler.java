/*
 * Class.java
 *
 * Created on August 6, 2003, 10:25 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
/**
 *
 * @author  administrator
 * @version 
 */

  public class DisconnectMonitoringEnabler extends MonitoringEnablingOperatingMode {
     public DisconnectMonitoringEnabler(String assetType, String assetID) {
      super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    } 
  }
  
