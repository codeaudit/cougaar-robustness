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


public class RequestRecord extends java.util.Hashtable {

    private Set originalActions;
    private Set remainingActions;
    private String request;

    /** Creates new RequestRecord */
    public RequestRecord() {
    }

    public Set getOriginalAction() { return originalActions; }
    public void setOriginalActions(Set originalActions) {this.originalActions = originalActions; }

    public Set getRemainingActions() { return remainingActions; }
    public void setRemainingActions(Set remainingActions) { this.remainingActions = remainingActions; }

    public String getRequest() {return request;}
    public void setRequest(String request) { this.request = request; }

    public String dump() {
        return "Request: " + request + "\n"
            + "Original Actions: " + originalActions + "\n"
            + "RemainingActions: " + remainingActions + "\n";
    }

}
