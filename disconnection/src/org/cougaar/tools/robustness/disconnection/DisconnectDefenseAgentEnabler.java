/*
 * DisconnectDefenseNodeEnabler.java
 *
 * Created on August 8, 2003, 8:15 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
/**
 *
 * @author  administrator
 * @version 
 */

  public class DisconnectDefenseAgentEnabler extends DefenseEnablingOperatingMode {
      public DisconnectDefenseAgentEnabler(String assetType, String assetID) {
      super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    } 
  }
  
