/*
 * AgentExistsCondition.java
 *
 * Created on August 20, 2003, 11:03 PM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

/**
 *
 * @author  administrator
 * @version 
 */
public class AgentExistsCondition extends DefenseApplicabilityBinaryCondition {

    /** Creates new AgentExistsCondition */
    public AgentExistsCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME, DefenseConstants.BOOL_FALSE);
    }

}