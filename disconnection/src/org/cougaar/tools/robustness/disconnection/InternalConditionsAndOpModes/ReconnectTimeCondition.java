/*
 * ReconnectTimeCondition.java
 *
 * Created on August 6, 2003, 8:38 AM
 *
 * @author David Wells - OBJS
 *
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
 */
 
package org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes;

import org.cougaar.core.relay.Relay;
import java.io.Serializable;
//import org.cougaar.core.persist.Persistable;

import org.cougaar.core.util.UniqueObject;

import org.cougaar.tools.robustness.disconnection.DisconnectConstants;
import org.cougaar.tools.robustness.disconnection.AgentVector;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

public class ReconnectTimeCondition extends DefenseTimeCondition
        implements Serializable, Relay.Source, Relay.Target, UniqueObject
    {

    public boolean isPersistable() { return true; }

    private AgentVector agents;
    
    public static ReconnectTimeCondition findOnBlackboard(String assetType, String assetID, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ReconnectTimeCondition);
            }
        };

        ReconnectTimeCondition rtc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           rtc = (ReconnectTimeCondition)iter.next();
           if (rtc.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) {
               return rtc;
           }
        }
        return null;
    }
    
    public ReconnectTimeCondition(String assetType, String assetID) {
        super(assetType, assetID, DisconnectConstants.DEFENSE_NAME);
    }

    public void setTime(Double newValue) {
         super.setValue(newValue);
    }

    public void setAgents(AgentVector agents) {
        this.agents = agents;
    }

    public AgentVector getAgents() {
        return agents;
    }

    public String toString() {
        return this.getAsset()+ " with agents: " + this.getAgents() + " and value " + getValue();
    }
    
     public int updateContent(Object content, Relay.Token token) {
        ReconnectTimeCondition newRTC = (ReconnectTimeCondition) content;
        if (getValue().compareTo(newRTC.getValue()) != 0 ||
            !newRTC.getAgents().equals(agents)) {
          setValue(newRTC.getValue());
          agents = newRTC.getAgents();
          return Relay.CONTENT_CHANGE;
        }
        return Relay.NO_CHANGE;
    }

}
