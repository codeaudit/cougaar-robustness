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
    String diagnosis;

    /** Creates new ActiveDisconnect */
    public NodeStatusRecord(AssetID nodeID, AgentVector agents, double reconnectTime, String diagnosis) {
        this.nodeID = nodeID;
        this.agents = agents;
        this.reconnectTime = reconnectTime;
        this.diagnosis = diagnosis;
    }

    public AssetID getNodeID() { return nodeID; }
    public AgentVector getAgents() { return agents; }
    public double getReconnectTime() { return reconnectTime; }
    public void setReconnectTime(double reconnectTime) { this.reconnectTime = reconnectTime; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String toString() {
        String buff = "NodeID: " + getNodeID() + "\n";
        buff = buff + "Agents: " + getAgents().toString() + "\n";
        buff = buff + "Diagnosis: " + getDiagnosis() + "\n";
        buff = buff + "ReconnectTime: " + getReconnectTime();
        return buff;
    }

}
