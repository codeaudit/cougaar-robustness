/*
 * RequestRecord.java
 *
 * Created on July 18, 2004, 12:49 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version
 */

import java.util.Set;
import org.cougaar.coordinator.techspec.AssetID;


public class RequestRecord extends java.util.Hashtable {

    private AssetID nodeID;;
    private Set originalActions;
    private Set originalDiagnoses;
    private AgentVector agentVector;
    private String request;
    private DisconnectManagerPlugin.RequestedAlarm alarm;
    private long reconnectTime;
    private Object originalDiagnosisValue;

    /** Creates new RequestRecord */
    public RequestRecord() {
    }

    public AssetID getNodeID() { return nodeID; }
    public void setNodeID(AssetID nodeID) { this.nodeID = nodeID; }

    public void setAgentVector(AgentVector agentVector) { this.agentVector = agentVector; }
    public AgentVector getAgentVector() { return agentVector; }

    public Set getOriginalActions() { return originalActions; }
    public void setOriginalActions(Set originalActions) {this.originalActions = originalActions; }

    public Set getOriginalDiagnoses() { return originalDiagnoses; }
    public void setOriginalDiagnoses(Set originalDiagnoses) {this.originalDiagnoses = originalDiagnoses; }

//    public Set getRemainingActions() { return remainingActions; }
//    public void setRemainingActions(Set remainingActions) { this.remainingActions = remainingActions; }

    public String getRequest() {return request;}
    public void setRequest(String request) { this.request = request; }

    public void setAlarm(DisconnectManagerPlugin.RequestedAlarm alarm) { this.alarm = alarm; }
    public DisconnectManagerPlugin.RequestedAlarm getAlarm() { return alarm; }

    public void setReconnectTime(long time) { this.reconnectTime = time; }
    public long getReconnectTime() { return this.reconnectTime; }

    public void setOriginalDiagnosisValue(Object value) { this.originalDiagnosisValue = value; }
    public Object getOriginalDiagnosisValue() { return originalDiagnosisValue; }

    public String dump() {
        return "Request: " + request + ":" +nodeID.toString() + "\n"
            + "Original Actions: " + originalActions + "\n";
//            + "RemainingActions: " + remainingActions + "\n";
    }

}
