/*
 * DisconnectNodePlugin.java
 *
 * Created on August 20, 2003, 9:03 AM
 */

/**
 *
 * @author  David Wells
 * @version 
 */
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
import java.util.Set;
import java.util.Collection;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.adaptivity.OMCRangeList;

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
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.node.NodeControlService;

import org.cougaar.core.adaptivity.InterAgentCondition;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.persist.NotPersistable;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.GenericStateModelAdapter;


public class DisconnectNodePlugin extends ServiceUserPluginBase {
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  private NodeIdentificationService nodeIdentificationService;
  private AgentIdentificationService agentIdentificationService;
  private EventService eventService;
  private AgentStatusService agentStatusService;
  private NodeControlService nodeControlService;
  
  private MessageAddress nodeAddress;
  private String nodeID;
  private MessageAddress managerAddress;
  private String managerID;

  //private IncrementalSubscription myOpModes;
  private IncrementalSubscription reconnectTimeSubscription;
  private IncrementalSubscription applicabilityConditionSubscription;
  private IncrementalSubscription defenseModeSubscription;      
  private IncrementalSubscription monitoringModeSubscription;
  private IncrementalSubscription conditionSubscription;
  private IncrementalSubscription localReconnectTimeSubscription;
  private IncrementalSubscription localAgentsSubscription;
  private IncrementalSubscription managerAddressSubscription;
    
  private long reconnectInterval;
  private String defenseMode;
  private String monitoringMode;
 
  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class,
    EventService.class
  };
  

  public DisconnectNodePlugin() {
    super(requiredServices);
  }

  
  public void load() {
      super.load();
      cancelTimer();
  }
  
  public void setupSubscriptions() {
    
     haveServices(); 
     
     initObjects(); //create & publish condition and op mode objects

     //Listen for new agents on this node
     managerAddressSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof RobustnessManagerID ) {
                return true ;
            }
            return false ;
        }
    }) ;

    //Listen for the RobustnessManagerFinderPlugin to find the MessageAddress of the Robustness Manager
     localAgentsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof  AgentExistsCondition) {
                return true ;
            }
            return false ;
        }
    }) ;
    
   
    //Listen for changes in out defense mode object
     defenseModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DisconnectDefenseAgentEnabler ) {
                return true ;
            }
            return false ;
        }
    }) ;

    //Listen for changes in our enabling mode object
     monitoringModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DisconnectMonitoringAgentEnabler ) {
                return true ;
            }
            return false ;
        }
      
     }) ;

     reconnectTimeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof ReconnectTimeCondition ) {
                return true ;
            }
            return false ;
        }
      
     }) ;  
     localReconnectTimeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof LocalReconnectTimeCondition ) {
                return true ;
            }
            return false ;
        }
      
     }) ;    }


  
  private void initObjects() {
    
     nodeAddress = agentIdentificationService.getMessageAddress();
     nodeID = agentIdentificationService.getName();
     if (logger.isDebugEnabled()) logger.debug("NodeAgent Address: "+nodeAddress);
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

      this.nodeControlService = (NodeControlService)
          sb.getService(this, NodeControlService.class, null);
      if (nodeControlService == null) {
          throw new RuntimeException("Unable to obtain EventService");
      }

      this.agentStatusService = (AgentStatusService)
          sb.getService(this, AgentStatusService.class, null);
      if (agentStatusService == null) {
          throw new RuntimeException("Unable to obtain AgentStatusService");
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

      Iterator iter;
      
      // if already know the ManagerAgent Address, then process newly announced Agents as they arrive
      
      if (managerAddress != null) {
          //********** Check for new agents on the node **********
          // create conditions for all the agents that report now
          Iterator iter2 = localAgentsSubscription.getAddedCollection().iterator();
          while (iter2.hasNext()) {
             AgentExistsCondition aec = (AgentExistsCondition) iter2.next();
             createConditionsAndOpModes("Agent", aec.getAsset(), nodeAddress, managerAddress);
          }
      }

      iter = managerAddressSubscription.getAddedCollection().iterator();
      if (iter.hasNext()) {
          // find & set the ManagerAgent address
           managerAddress = ((RobustnessManagerID)iter.next()).getMessageAddress();
           if (logger.isDebugEnabled()) logger.debug("ManagerAddress: "+managerAddress.toString());

          // create conditions & opmodes for the NodeAgent
          createLocalCondition("Node", nodeID, nodeAddress, managerAddress);
          createConditionsAndOpModes("Node", nodeID, nodeAddress, managerAddress);
     
          //********** Check for new agents on the node **********
          // create conditions for all the agents that reported BEFORE we found the ManagerAgent Address
          Iterator iter2 = localAgentsSubscription.iterator();
          while (iter2.hasNext()) {
             AgentExistsCondition aec = (AgentExistsCondition) iter2.next();
             createConditionsAndOpModes("Agent", aec.getAsset(), nodeAddress, managerAddress);
          }
      }
      
      
      //********** Check for departing agents on the node & remove there conditions & opmodes **********
      iter = localAgentsSubscription.getRemovedCollection().iterator();
      while (iter.hasNext()) {
         AgentExistsCondition aec = (AgentExistsCondition) iter.next();
         if (logger.isDebugEnabled()) logger.debug("Removing "+aec.getAsset());
         removeConditionsAndOpModes(aec.getAssetType(), aec.getAsset());
      }

      
      //********* Check for changes in our modes ************
      
      //We have defense modes for all agents
      iter = defenseModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          DisconnectDefenseAgentEnabler dmode = (DisconnectDefenseAgentEnabler)iter.next();
          if (dmode != null) {
              defenseMode = dmode.getValue().toString();
              if (logger.isDebugEnabled()) logger.debug("Saw: "+
                 dmode.getClass()+":"+
                 dmode.getName() + " set to " + dmode.getValue());
          }
      };

      //We have defense modes for all agents
      iter = monitoringModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {      
          DisconnectMonitoringAgentEnabler mmode = (DisconnectMonitoringAgentEnabler)iter.next();
          if (mmode != null) {
              monitoringMode = mmode.getValue().toString();
              if (logger.isDebugEnabled()) logger.debug("Saw: "+
                mmode.getClass()+":"+
                mmode.getName() + " set to " + mmode.getValue());
          }
      }

      
      //We have one local time condition, so we only get the one from iter.next();
      // This was set by the DisconnectServlet for the Node
      iter = localReconnectTimeSubscription.getChangedCollection().iterator();
      if (logger.isDebugEnabled()) logger.debug("starting to process a changed LocalReconnectTimeCondition");
      if (iter.hasNext()) {      
          LocalReconnectTimeCondition lrtc = (LocalReconnectTimeCondition)iter.next();
          if (logger.isDebugEnabled()) logger.debug("Found lrtc "+lrtc.getAsset());
          if (lrtc != null) {
                reconnectInterval = (long) Double.parseDouble(lrtc.getValue().toString()) / 1000L;
          }
          // set the ReconnectTimeCondition for each agent on the node
          Iterator iter2 = reconnectTimeSubscription.iterator();
          while (iter2.hasNext()) {
              ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter2.next();
              rtc.setTime(new Double(Double.parseDouble(lrtc.getValue().toString())));
              getBlackboardService().publishAdd(rtc);
              if (logger.isDebugEnabled()) logger.debug("Set the Condition for "+nodeID+":"+rtc.getAsset());   
          }
      }
      
      // announce changes to the defenseEnabler as Cougaar Events to allow actual disconnection from ACME
      // need to iterate thru the modes to find the one for the Node Agent
      iter = defenseModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          DisconnectDefenseAgentEnabler dmode = (DisconnectDefenseAgentEnabler)iter.next();
          if (dmode != null) {
              if (dmode.getAssetType().equals("Node")) {
                  defenseMode = dmode.getValue().toString();
                  if (eventService.isEventEnabled()) {
                      if (defenseMode.equals("ENABLED")) {
                          eventService.event(nodeID+" plans to Disconnect for "+reconnectInterval+" sec");
                     } 
                  }
              }
          }
      };

  }

    private void createLocalCondition(String assetType, String assetID, MessageAddress localAddress, MessageAddress remoteAddress) {
        // Make the LocalReconnectTimeCondition used for signalling a Disconnect request
        
          LocalReconnectTimeCondition lrtc =
             new LocalReconnectTimeCondition("Node", assetID);
          lrtc.setUID(us.nextUID());
          lrtc.setSourceAndTarget(localAddress, remoteAddress);

          getBlackboardService().publishAdd(lrtc);
          
          if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+localAddress+":"+assetID); 
    }
        

    private void createConditionsAndOpModes(String assetType, String assetID, MessageAddress localAddress, MessageAddress remoteAddress) {
        // Make the remote condition & opmodes (used for agents & the node agent)

        ReconnectTimeCondition rtc = new ReconnectTimeCondition(assetType, assetID);
        rtc.setUID(us.nextUID());
        rtc.setSourceAndTarget(localAddress, remoteAddress);

        DisconnectDefenseAgentEnabler dde = new DisconnectDefenseAgentEnabler(assetType, assetID);
        dde.setUID(us.nextUID());
        dde.setSourceAndTarget(localAddress, remoteAddress);

        DisconnectMonitoringAgentEnabler dme = new DisconnectMonitoringAgentEnabler(assetType, assetID);
        dme.setUID(us.nextUID());
        dme.setSourceAndTarget(localAddress, remoteAddress);

        getBlackboardService().publishAdd(rtc);
        getBlackboardService().publishAdd(dde);
        getBlackboardService().publishAdd(dme);

        if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+localAddress+":"+assetID); 
    }
    
    
    private void removeConditionsAndOpModes(String assetType, String assetID) {
        // Find and remove the corresponding ReconnectTimeCondition & LocalReconnectTimeCondition
        
        LocalReconnectTimeCondition lrtc = LocalReconnectTimeCondition.findOnBlackboard(assetType, assetID, blackboard);       
        ReconnectTimeCondition rtc = ReconnectTimeCondition.findOnBlackboard(assetType, assetID, blackboard);
        DisconnectMonitoringAgentEnabler dme = DisconnectMonitoringAgentEnabler.findOnBlackboard(assetType, assetID, blackboard);
        DisconnectDefenseAgentEnabler dde = DisconnectDefenseAgentEnabler.findOnBlackboard(assetType, assetID, blackboard);
        
        if (lrtc != null) blackboard.publishRemove(lrtc);
        if (rtc != null) blackboard.publishRemove(rtc);
        if (dme != null) blackboard.publishRemove(dme);
        if (dde != null) blackboard.publishRemove(dde);
        
        if (logger.isDebugEnabled()) logger.debug("Removed Conditions & OpModes for departing agent "+assetID); 
    }
}
