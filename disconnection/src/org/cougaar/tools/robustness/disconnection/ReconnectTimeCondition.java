/*
 * Class.java
 *
 * Created on August 6, 2003, 8:38 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;
/**
 *
 * @author  administrator
 * @version 
 */
public class ReconnectTimeCondition extends DefenseTimeCondition {
    public ReconnectTimeCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    }

    public void setValue(Double newValue) {
         super.setValue(newValue);
    }
    
  }
