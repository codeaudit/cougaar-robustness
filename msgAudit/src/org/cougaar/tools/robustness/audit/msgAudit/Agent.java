/*
 * Agent.java
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
public class Agent {
    
    Vector[] sendArray; //one bitset for each send level
    //BitSet arrivedOKArray; 
    Vector[] recvArray;
    String agentName;
    //Vector sentMsgs;
    //Vector recvMsgs;
    
    /** Creates a new instance of Agent */
    public Agent(String _name, int _ns, int _nr) {
        
        agentName = _name;
        
        //Init message arrays
        int numSend = _ns;
        int numRecv = _nr;

        sendArray = new Vector[numSend];
        for (int i=0; i< numSend; i++)
            sendArray[i] = new Vector(100);

        recvArray = new Vector[numRecv];
        for (int i=0; i< numRecv; i++)
            recvArray[i] = new Vector(100);
        
        //arrivedOKArray = new BitSet();
        
        //sentMsgs = new Vector(100);
        //recvMsgs = new Vector(100);
    }

      
    public boolean markMsgReceived(XMLMessage _xml) {
        
        XMLMessage xml;
        boolean found = false;
        Iterator iter = sendArray[0].iterator();
        String seqnum = _xml.num;
        
        while (iter.hasNext()) {
            xml = (XMLMessage) iter.next();
            if (xml.num.equals(seqnum)) { //we found the pair
                found = true;
                xml.receiveCount++;
                break;
            }
        }
        return found;
    }
    
    public void addSendMsg(int _level, XMLMessage _xmlmsg) {
        sendArray[_level-1].add(_xmlmsg);
    }
    
    public void addRecvMsg(int _level, XMLMessage _xmlmsg) {
        recvArray[((-_level)-1)].add(_xmlmsg);
    }
    
    //Returns all msgs from the send array that have the specified seq number
    public Vector getSendStack(String _msgnum) {
        
        Vector out = new Vector();
        Iterator it;
        XMLMessage msg;
        
        for (int i =0; i<sendArray.length; i++) {
            it = sendArray[i].iterator();
            //System.out.println("sendArray "+i+" size="+sendArray[i].size());
            while(it.hasNext()) {
                msg = (XMLMessage) it.next();
                if (msg.num.equals(_msgnum)) {
                    out.add(msg); //keep looking, there may be dups
                    //System.out.println("Added sendArray Msg... level="+i);
                }
            }
        }   
        return out;
    }
    
    //Returns all msgs from the receive array that have the specified sender & seq number
    public Vector getRecvStack(String _agent, String _num) {
        Vector out = new Vector();
        Iterator it;
        XMLMessage msg;

        for (int i =0; i<recvArray.length; i++) {
            it = recvArray[i].iterator();
            //System.out.println("recvArray "+i+" size="+recvArray[i].size());
            while(it.hasNext()) {
                msg = (XMLMessage) it.next();
                if (msg.num.equals(_num) && msg.from.equals(_agent)) {
                    out.add(msg); //keep looking, there may be dups
                    //System.out.println("Added sendArray Msg... level="+i);
                }
            } 
        }                            
        return out;
    }
    
}
