/*
 * DisconnectAgentPlugin.java
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

import org.cougaar.tools.robustness.deconfliction.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Collection;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.adaptivity.OMCRangeList;
//import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
//import org.cougaar.core.adaptivity.InterAgentOperatingMode;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.EventService;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;

import org.cougaar.core.adaptivity.InterAgentCondition;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.persist.NotPersistable;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.GenericStateModelAdapter;


public class DisconnectAgentPlugin extends ServiceUserPluginBase {
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  private NodeIdentificationService nodeIdentificationService;
  private AgentIdentificationService agentIdentificationService;
  private EventService eventService;
  
  private MessageAddress assetAddress;
  private String assetID;
  private MessageAddress nodeAddress;

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class,
    EventService.class
  };
  

  public DisconnectAgentPlugin() {
    super(requiredServices);
  }

  
  public void load() {
      super.load();
      haveServices(); 
        if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");
     
      initObjects(); //create & publish condition and op mode objectscancelTimer();
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

  
  //Create one condition and one of each type of operating mode
  private void initObjects() {
           
     assetAddress = agentIdentificationService.getMessageAddress();
     assetID = agentIdentificationService.getName();
     nodeAddress = nodeIdentificationService.getMessageAddress();
    
     AgentExistsCondition aec =
        new AgentExistsCondition("Agent", assetID);
     aec.setUID(us.nextUID());
     aec.setSourceAndTarget(assetAddress, nodeAddress);
     if (logger.isDebugEnabled()) logger.debug("Source: "+assetAddress+", Target: "+nodeAddress);

     getBlackboardService().openTransaction();
     getBlackboardService().publishAdd(new Dummy(assetID));  // weird hack so the agent doesnt get lost on rehydration - not entirely clear this is the problem
     getBlackboardService().publishAdd(aec);
     getBlackboardService().closeTransaction();

     if (logger.isDebugEnabled()) logger.debug("Announced existence of "+assetID);
   
  }      
  
  private boolean haveServices() {
    //if (conditionService != null && operatingModeService != null && us != null) return true;
    if (acquireServices()) {
      if (logger.isDebugEnabled()) logger.debug(".haveServices - acquiredServices.");
      ServiceBroker sb = getServiceBroker();
      conditionService = (ConditionService)
        sb.getService(this, ConditionService.class, null);
      
      operatingModeService = (OperatingModeService)
        sb.getService(this, OperatingModeService.class, null);
      
      us = (UIDService ) 
        sb.getService( this, UIDService.class, null ) ;
        
      // get the EventService
      this.eventService = (EventService)
          sb.getService(this, EventService.class, null);
      if (eventService == null) {
          throw new RuntimeException("Unable to obtain EventService");
      }

      this.nodeIdentificationService = (NodeIdentificationService)
          sb.getService(this, NodeIdentificationService.class, null);
      if (nodeIdentificationService == null) {
          throw new RuntimeException("Unable to obtain EventService");
      }
      
      agentIdentificationService = (AgentIdentificationService)
        sb.getService(this, AgentIdentificationService.class, null);
      if (agentIdentificationService == null) {
          throw new RuntimeException("Unable to obtain agent-id service");
      }
      else if (logger.isDebugEnabled()) logger.debug(agentIdentificationService.toString());
      return true;
    }
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  public void execute() {
  }
  
}