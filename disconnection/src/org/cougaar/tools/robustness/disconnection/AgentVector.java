/*
 * AgentVector.java
 *
 * Created on June 29, 2004, 4:31 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.coordinator.techspec.AssetID;

public class AgentVector extends java.util.Vector implements java.io.Serializable {

    /** Creates new AgentVector */
    public AgentVector() {
    }

    public void addAgent(AssetID agentID) { super.add(agentID); }
    public void removeAgent(AssetID agentID) { super.remove(agentID); }

}
