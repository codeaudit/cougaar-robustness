/*
 * Class.java
 *
 * Created on August 6, 2003, 8:29 AM
 */


package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
/**
 *
 * @author  administrator
 * @version 
 */
   
  public class DisconnectDefenseEnabler extends DefenseEnablingOperatingMode {
     public DisconnectDefenseEnabler(String assetType, String assetID) {
      super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    } 
  }
