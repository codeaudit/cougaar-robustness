/*
 * DisconnectAgentPlugin.java
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

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Collection;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;


public class DisconnectAgentPlugin extends DisconnectPluginBase {


  public DisconnectAgentPlugin() {
    super();
  }

  
  public void load() {
      super.load();
      initObjects(); 
  }
  
  
    public void suspend() {
        // Remove the AgentExistsCondition so that the DisconnectNodePlugin will know the Agent has left the Node
        UnaryPredicate pred = new UnaryPredicate() {
          public boolean execute(Object o) {
            return 
              (o instanceof AgentExistsCondition);
          }
        };

        AgentExistsCondition cond = null;

        getBlackboardService().openTransaction();
        Collection c = getBlackboardService().query(pred);
        if (c.iterator().hasNext()) {
           cond = (AgentExistsCondition)c.iterator().next();
           if (logger.isDebugEnabled()) logger.debug("UNLOADING "+cond.getAsset());
           getBlackboardService().publishRemove(cond); //lets the NodeAgent learn that the Agent has unloaded
        }    
        getBlackboardService().closeTransaction();
   
        super.suspend();
    }
  
  public void setupSubscriptions() {
  }

  
  private void initObjects() {
     // create an AgentExistsCondition object to inform the NodeAgent that the agent is here
     AgentExistsCondition aec =
        new AgentExistsCondition("Agent", getAgentID());
     aec.setUID(getUIDService().nextUID());
     aec.setSourceAndTarget(getAgentAddress(), getNodeAddress());
     if (logger.isDebugEnabled()) logger.debug("Source: "+getAgentAddress()+", Target: "+getNodeAddress());

     getBlackboardService().openTransaction();
     //getBlackboardService().publishAdd(new Dummy(assetID));  // weird hack so the agent doesnt get lost on rehydration - not entirely clear this is the problem
     getBlackboardService().publishAdd(aec);
     getBlackboardService().closeTransaction();

     if (logger.isDebugEnabled()) logger.debug("Announced existence of "+getAgentID());   
  }      


  public void execute() {
  }
  
}