/*
 * LocalReconnectTimeCondition.java
 *
 * Created on August 20, 2003, 8:31 PM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class LocalReconnectTimeCondition extends DefenseTimeCondition {

    /** Creates new LocalReconnectTimeCondition */
    
    public LocalReconnectTimeCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    }

    public void setTime(Double newValue) {
         super.setValue(newValue);
    }
    
}
