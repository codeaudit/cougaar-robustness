/*
 * Host.java
 *
 * Created on August 28, 2003, 10:59 AM
 */

package org.cougaar.coordinator.tools;

import java.util.Vector;

/**
 * @author Administrator
 */
public class Host {
    
    public String name;
    public Vector nodes;
    
    /** Creates a new instance of Host */
    public Host(String _name) {
        name = _name;
        nodes = new Vector();
    }
    
    public Vector getNodes() { return nodes; }
    public void addNode(Node _node) { nodes.addElement(_node); }
}
