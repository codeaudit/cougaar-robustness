/*
 * AgentVector.java
 *
 * Created on June 29, 2004, 4:31 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  administrator
 * @version 
 */
public class AgentVector extends java.util.Vector implements java.io.Serializable {

    /** Creates new AgentVector */
    public AgentVector() {
    }

    public void addAgent(String agentName) { super.add(agentName); }
    public void removeAgent(String agentName) { super.remove(agentName); }

}
