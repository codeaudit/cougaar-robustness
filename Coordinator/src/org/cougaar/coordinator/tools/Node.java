/*
 * Node.java
 *
 * Created on August 28, 2003, 10:59 AM
 */

package org.cougaar.coordinator.tools;

import java.util.Vector;

/**
 * @author Administrator
 */
public class Node {
    
    public String name;
    public Vector agents;
    
    /** Creates a new instance of Node */
    public Node(String _name) {
        name = _name;
        agents = new Vector();
    }
    
    public Vector getAgents() { return agents; }
    public void addAgent(Agent _agent) { agents.addElement(_agent); }
}
