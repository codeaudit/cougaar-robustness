/*
 * ActiveDisconnect.java
 *
 * Created on July 27, 2004, 1:08 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells
 * @version 
 */

import org.cougaar.coordinator.techspec.AssetID;
import java.io.Serializable;

public class NodeStatusRecord implements Serializable {

    AssetID nodeID;
    AgentVector agents;
    double reconnectTime;

    /** Creates new ActiveDisconnect */
    public NodeStatusRecord(AssetID nodeID, AgentVector agents, double reconnectTime) {
        this.nodeID = nodeID;
        this.agents = agents;
        this.reconnectTime = reconnectTime;
    }

    public AssetID getNodeID() { return nodeID; }
    public AgentVector getAgents() { return agents; }
    public double getReconnectTime() { return reconnectTime; }
    public void setReconnectTime(double reconnectTime) { this.reconnectTime = reconnectTime; }

}
