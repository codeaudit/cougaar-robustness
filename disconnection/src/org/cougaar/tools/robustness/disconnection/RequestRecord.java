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

    private AssetID assetID;;
    private Set originalActions;
    private Set originalDiagnoses;
    private String request;
    private DisconnectManagerPlugin.RequestedAlarm alarm;

    /** Creates new RequestRecord */
    public RequestRecord() {
    }

    public AssetID getAssetID() { return assetID; }
    public void setAssetID(AssetID assetID) { this.assetID = assetID; }

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

    public String dump() {
        return "Request: " + request + ":" +assetID.toString() + "\n"
            + "Original Actions: " + originalActions + "\n";
//            + "RemainingActions: " + remainingActions + "\n";
    }

}
