/*
 * Agents.java
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
 * Created on December 17, 2002, 5:43 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

import java.util.BitSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.Hashtable;

/**
 *
 * @author  pazandak@objs.com
 */
public class Agents {
    
    //Store all LogPoints seen but that are NOT declared in the config
    Vector notFoundLogPoints;
    
    //List of all agents seen so far
    Vector agents;

    ConfigData cd;
  
    public Agents(ConfigData _cd) {
        cd = _cd;
        agents = new Vector(200);
        notFoundLogPoints = new Vector(20);        
    }

    /* 
     * This stores the msg in the agent's msg cache.
     * The msgs are processed / anayzed once all the 
     * msgs have been loaded.
     */
    void storeXMLMsg(XMLMessage xmlmsg) throws XMLMessageException {
        
        //Get send / receive level
        int level = cd.GET_LEVEL(xmlmsg.lpName);
        
        if (level==0) {
            if (!notFoundLogPoints.contains(xmlmsg.lpName)) {
                System.out.println("IGNORING ALL occurrences of undeclared LogPoint name => "+xmlmsg.lpName);         
                notFoundLogPoints.add(xmlmsg.lpName);
            }
            return;
        }

        xmlmsg.setLevel(level);
        Agent agent;
        if (level>0) { //Send side - store msg with sending agent
            agent = findAgent(xmlmsg.from);
            if (agent == null)
                throw new XMLMessageException("XMLMessage FROM agent attr is null. Cannot process: " + xmlmsg);
            //Add msg to agent's send cache
            //agent.sentMsgs.add(xmlmsg);
            agent.addSendMsg(level, xmlmsg);
        } 
        else { //Receive side - store msg with receiving agent
            agent = findAgent(xmlmsg.to);
            if (agent == null)
                throw new XMLMessageException("XMLMessage TO agent attr is null. Cannot process: " + xmlmsg);
            //Add msg to agent's recv cache
            //agent.recvMsgs.add(xmlmsg);        
            agent.addRecvMsg(level, xmlmsg);
        }
    }
    
    public Iterator agents() { return agents.iterator(); }

    //Returns a new agent if agent not found
    Agent findAgent(String _name) {
     
        Iterator iter = agents.iterator();
        Agent agent;
        
        while (iter.hasNext()) {
            agent = (Agent)iter.next();
            if (agent.agentName.equals(_name))
                return agent;
        }
        //Agent not found, add it
        agent = new Agent(_name, cd.GET_NUM_SEND_LEVELS(), cd.GET_NUM_RECV_LEVELS());
        agents.add(agent);
        return agent;        
    }

    // Returns null if agent not found 
    Agent lookupAgent(String _name) {
     
        Iterator iter = agents.iterator();
        Agent agent = null;
        
        while (iter.hasNext()) {
            agent = (Agent)iter.next();
            if (agent.agentName.equals(_name))
                return agent;
        }
        return null;        
    }

    // Returns null if agent name not found ... does substring match on agent name only
    Agent lookupByAgentName(String _name) {
     
        Iterator iter = agents.iterator();
        Agent agent = null;
        
        while (iter.hasNext()) {
            agent = (Agent)iter.next();
            if (agent.agentName.indexOf(_name)>=0)
                return agent;
        }
        return null;        
    }
    
    public String printMsgStack(XMLMessage _xmlmsg) {
     
        StringBuffer buf = new StringBuffer();
        MessageStackResult msr = getMsgStack(_xmlmsg);
        
        return msr.toString();
    }

    public MessageStackResult getMsgStack(XMLMessage _xmlmsg) {
     
        Agent fr = lookupAgent(_xmlmsg.from);
        Agent to = lookupAgent(_xmlmsg.to);
 
        if (to==null) //try substring match
            to = lookupByAgentName(_xmlmsg.to);
        
        String fromErr = null;
        Vector fromStack = null;
        if (fr != null) 
            fromStack = fr.getSendStack(_xmlmsg.num);
        else {
            fromErr = "Cannot find sending agent: " + _xmlmsg.from;
        }
        
        Vector toStack = null;
        String toErr   = null;
        if (to != null) 
            toStack = to.getRecvStack(_xmlmsg.from, _xmlmsg.num);
        else { 
            toErr = "Cannot find receiving agent: " + _xmlmsg.to;
        }

        return (new MessageStackResult(_xmlmsg, fromStack, fromErr, toStack, toErr)) ;
    }
}
