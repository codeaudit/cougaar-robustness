/*
 * AgentMgmt.java
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 *
 * Created on February 18, 2003, 1:35 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;
import org.cougaar.tools.robustness.audit.msgAudit.gui.*;

import java.util.Vector;
import java.util.Iterator;

/**
 *
 * @author  Administrator
 */
public class AgentMgmt {
    
    private LogPointVectorMgmt logPointMgmt;

    //List of all agents seen so far
    Vector agents;
    AgentSummaryGUI gui;
  
    public AgentMgmt(LogPointVectorMgmt _logpointMgmt, AgentSummaryGUI _gui) {
        logPointMgmt = _logpointMgmt;
        logPointMgmt.registerAgentMgmt(this);
        gui = _gui;
        agents = new Vector(200, 50);
    }

    /**
     * Iterator of all agents
     */
    public Iterator agents() { return agents.iterator(); }

    
    /** 
     * @return Returns a new agent if agent not found
     */
    public AgentData createAgent(String _name) {
     
        synchronized(agents) {
            Iterator iter = agents.iterator();
            AgentData agent;

            while (iter.hasNext()) {
                agent = (AgentData)iter.next();
                if (agent.name().equals(_name))
                    return agent;
            }
            //Agent not found, add it
            agent = new AgentData(this, logPointMgmt, _name);
            agents.add(agent);
            gui.addAgent(agent);
            return agent;        
        }
    }

    /**
     * @return Returns null if agent not found 
     */
    public AgentData lookupAgent(String _name) {
     
        Iterator iter = agents.iterator();
        AgentData agent = null;

        while (iter.hasNext()) {
            agent = (AgentData)iter.next();
            if (agent.name().equals(_name))
                return agent;
        }
        return null;        
    }

    /*
     * @return null if agent name not found ... does substring match on agent name only
     * Returns the first agent found
     */
    public AgentData lookupAgentNameSubstring(String _name) {
     
        Iterator iter = agents();
        AgentData agent = null;
        
        while (iter.hasNext()) {
            agent = (AgentData)iter.next();
            if (agent.name().indexOf(_name)>=0)
                return agent;
        }
        return null;        
    }

    /*
     * @return Vector of agents that match (on agent name only) the provided substring.
     * Returns ALL agents found
     */
    public Vector lookupAllAgentNameSubstring(String _name) {
     
        Iterator iter = agents();
        AgentData agent = null;
        Vector found = new Vector();
        
        while (iter.hasNext()) {
            agent = (AgentData)iter.next();
            if (agent.name().indexOf(_name)>=0)
                found.add(agent);
        }
        return found;        
    }
    
    /* 
     * Rechecks ALL agent data in case changes have been made via GUIs
     * E.g. the final log point is changed.
     */
    public void recheckAgentData() {
        
        Iterator iter = agents();
        AgentData agent = null;
        
        while (iter.hasNext()) {
            agent = (AgentData)iter.next();
            agent.recheckMessagesArrivedStatus();
        }
        
    }
    
}

