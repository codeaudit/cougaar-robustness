/*
 * AgentExistsCondition.java
 *
 * Created on August 20, 2003, 11:03 PM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class AgentExistsCondition extends DefenseApplicabilityBinaryCondition
                                  implements java.io.Serializable {

    /** Creates new AgentExistsCondition */
    public AgentExistsCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME, DefenseConstants.BOOL_FALSE);
    }

}
