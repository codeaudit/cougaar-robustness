/*
 * SocietyTree.java
 *
 * Created on August 28, 2003, 10:50 AM
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */


package org.cougaar.coordinator.tools;

import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DefaultAssetTechSpec;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;

import java.util.Vector;
import java.util.Iterator;

/**
 * @author Administrator
 */
public class SocietyTree extends Vector {

    private Vector society;
    private Host currentHost = null;
    private Node currentNode = null;
    private int count = 0;
    
    private UIDService us = null;
    
    /** Creates a new instance of SocietyTree */
    public SocietyTree()  {
        society = new Vector(); //of hosts
    }
        
    void addHost(String name) {
        currentHost = new Host(name);
        society.addElement(currentHost);
        count++;
    }
    
    void addNode(String name) {
        currentNode = new Node(name);
        currentHost.addNode(currentNode);
        count++;
    }
    
    void addAgent(String name) {
        currentNode.addAgent(new Agent(name));
        count++;
    }

    /** Create TechSpec assets for each host, node, and agent in the tree */
    public Vector toAssets(UIDService us) {

        this.us = us;
        
        AssetType hostType = AssetType.findAssetType("host");
        AssetType nodeType = AssetType.findAssetType("node");
        AssetType agentType = AssetType.findAssetType("agent");
        
        Vector assets = new Vector(count);

        DefaultAssetTechSpec hostAsset;
        DefaultAssetTechSpec nodeAsset;
        
        Iterator hosts = society.iterator();
        while (hosts.hasNext()) {
            Host host = (Host) hosts.next();
            hostAsset = new DefaultAssetTechSpec(null, null, host.name, hostType, nextUID());
            assets.addElement(hostAsset);
            Iterator nodes = host.getNodes().iterator();
            while (nodes.hasNext()) {
                Node node = (Node) nodes.next();
                nodeAsset = new DefaultAssetTechSpec(hostAsset, null, node.name, nodeType, nextUID());
                assets.addElement(nodeAsset); 
                Iterator agents = node.getAgents().iterator();
                while (agents.hasNext()) {
                    Agent agent = (Agent)agents.next();
                    assets.addElement(new DefaultAssetTechSpec(hostAsset, nodeAsset, agent.name, agentType, nextUID())); //could add relationship to node
                }
            }
        }
        return assets;
    }
    

    /** Get a UID. Return 0 if the UIDService is null */
    private UID nextUID() {
        if (us !=null) return us.nextUID();
        return null;
    }
    
    
    public void printTree() {
        
        Iterator hosts = society.iterator();
        while (hosts.hasNext()) {
            Host host = (Host) hosts.next();
            System.out.println(host.name);
            Iterator nodes = host.getNodes().iterator();
            while (nodes.hasNext()) {
                Node node = (Node) nodes.next();
                System.out.println("    "+node.name);
                Iterator agents = node.getAgents().iterator();
                while (agents.hasNext()) {
                    System.out.println("        "+((Agent)(agents.next())).name);
                }
            }
        }
    }
    
    
}
