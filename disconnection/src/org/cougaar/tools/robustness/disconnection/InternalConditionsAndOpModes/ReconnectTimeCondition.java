/*
 * ReconnectTimeCondition.java
 *
 * Created on August 6, 2003, 8:38 AM
 *
 * @author David Wells - OBJS
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
 
package org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes;

import org.cougaar.core.relay.Relay;
import java.io.Serializable;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
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
        boolean somethingChanged = false;
        Logger logger = Logging.getLogger(ReconnectTimeCondition.class);
        if (logger.isDebugEnabled()) logger.debug("thisRTC = " + this + "\n" + "newRTC = " + newRTC);

        if ((getValue()==null && newRTC.getValue()!=null) ||
            (getValue()!=null && getValue().compareTo(newRTC.getValue()) != 0 )) {
                setValue(newRTC.getValue());
                somethingChanged = true;
        }
        if ((getAgents()==null && newRTC.getAgents()!=null) ||
            (getAgents()!=null && !getAgents().equals(newRTC.getAgents()))) {
                agents = newRTC.getAgents();
                somethingChanged = true;
        }
        if (somethingChanged) {
            return Relay.CONTENT_CHANGE;
        }
        else {
            return Relay.NO_CHANGE;
        }
    }

}
