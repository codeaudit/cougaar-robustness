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
  
  private MessageAddress assetAddress;
  private String assetID;
  private MessageAddress managerAddress;
  private String managerID;
  private String MANAGER_NAME;

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
     if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");

     //getPluginParams();
     
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

/*
    // Takes the Name of the Manager Agent as a required String parameter
    private void getPluginParams() {
      if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters [must supply the name of the Manager Agent].");

      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           MANAGER_NAME = (String)iter.next();
           logger.debug("Setting Manager Agent Name = " + MANAGER_NAME);
      }
  }       
*/
  
  //Create one condition and one of each type of operating mode
  private void initObjects() {
    
     assetAddress = agentIdentificationService.getMessageAddress();
     assetID = agentIdentificationService.getName();
     //managerAddress = MessageAddress.getMessageAddress(MANAGER_NAME);
     if (logger.isDebugEnabled()) logger.debug("NodeAgent Address: "+assetAddress);
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
             String agentID = aec.getAsset();
              ReconnectTimeCondition rtc =
                  new ReconnectTimeCondition("Agent", agentID);
              rtc.setUID(us.nextUID());
              rtc.setSourceAndTarget(assetAddress, managerAddress);

              DisconnectDefenseAgentEnabler dde = 
                  new DisconnectDefenseAgentEnabler("Agent", agentID);
              dde.setUID(us.nextUID());
              dde.setSourceAndTarget(assetAddress, managerAddress);

              DisconnectMonitoringAgentEnabler dme = 
                  new DisconnectMonitoringAgentEnabler("Agent", agentID);
              dme.setUID(us.nextUID());
              dme.setSourceAndTarget(assetAddress, managerAddress);

              getBlackboardService().publishAdd(rtc);
              getBlackboardService().publishAdd(dde);
              getBlackboardService().publishAdd(dme);

              if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+assetID+":"+agentID); 
          }
      }

      iter = managerAddressSubscription.getAddedCollection().iterator();
      if (iter.hasNext()) {
          // find & set the ManagerAgent address
           managerAddress = ((RobustnessManagerID)iter.next()).getMessageAddress();
           if (logger.isDebugEnabled()) logger.debug("ManagerAddress: "+managerAddress.toString());

           // create the conditions & opmodes for the NodeAgent
          LocalReconnectTimeCondition lrtc =
             new LocalReconnectTimeCondition("Node", assetID);
          lrtc.setUID(us.nextUID());
          lrtc.setSourceAndTarget(assetAddress, managerAddress);

          ReconnectTimeCondition rtc =
              new ReconnectTimeCondition("Node", assetID);
          rtc.setUID(us.nextUID());
          rtc.setSourceAndTarget(assetAddress, managerAddress);

          DisconnectDefenseAgentEnabler dde = 
              new DisconnectDefenseAgentEnabler("Node", assetID);
          dde.setUID(us.nextUID());
          dde.setSourceAndTarget(assetAddress, managerAddress);

          DisconnectMonitoringAgentEnabler dme = 
              new DisconnectMonitoringAgentEnabler("Node", assetID);
          dme.setUID(us.nextUID());
          dme.setSourceAndTarget(assetAddress, managerAddress);

          getBlackboardService().publishAdd(lrtc);
          getBlackboardService().publishAdd(rtc);
          getBlackboardService().publishAdd(dde);
          getBlackboardService().publishAdd(dme);
     
          //********** Check for new agents on the node **********
          // create conditions for all the agents that reported BEFORE we found the ManagerAgent Address
          Iterator iter2 = localAgentsSubscription.iterator();
          while (iter2.hasNext()) {
             AgentExistsCondition aec = (AgentExistsCondition) iter2.next();
             String agentID = aec.getAsset();
              rtc = new ReconnectTimeCondition("Agent", agentID);
              rtc.setUID(us.nextUID());
              rtc.setSourceAndTarget(assetAddress, managerAddress);

              dde = new DisconnectDefenseAgentEnabler("Agent", agentID);
              dde.setUID(us.nextUID());
              dde.setSourceAndTarget(assetAddress, managerAddress);

              dme = new DisconnectMonitoringAgentEnabler("Agent", agentID);
              dme.setUID(us.nextUID());
              dme.setSourceAndTarget(assetAddress, managerAddress);

              getBlackboardService().publishAdd(rtc);
              getBlackboardService().publishAdd(dde);
              getBlackboardService().publishAdd(dme);

              if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+assetID+":"+agentID); 
          }
      }
      
      
      //********** Check for departing agents on the node & remove there conditions & opmodes **********
      iter = localAgentsSubscription.getRemovedCollection().iterator();
      while (iter.hasNext()) {
         AgentExistsCondition aec = (AgentExistsCondition) iter.next();
         if (logger.isDebugEnabled()) logger.debug("Removing "+aec.getAsset());
         removeReconnectTimeCondition(aec);
         removeDefenseEnabler(aec);
         removeMonitoringEnabler(aec);
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

      /*
      //We have one time condition, so we only get the one from iter.next();
      // This was set by the DisconnectServlet for the Node
      iter = reconnectTimeSubscription.iterator();
      if (iter.hasNext()) {      
          ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
          if (rtc != null) {
                reconnectInterval = (long) Double.parseDouble(rtc.getValue().toString()) / 1000L;
          }
          if (logger.isDebugEnabled()) {
              Iterator iter2 = nodeControlService.getRootContainer().getAgentAddresses().iterator();
              while (iter2.hasNext()) {
                logger.debug("Node contains Agent: "+iter2.next());
              }
          }
      }
     */
      
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
              if (logger.isDebugEnabled()) logger.debug("Set the Condition for "+assetID+":"+rtc.getAsset());   
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
                          eventService.event(assetID+" plans to Disconnect for "+reconnectInterval+" sec");
                     } 
                  }
              }
          }
      };

  }
  
  private boolean removeDefenseEnabler(AgentExistsCondition agentID) {
    // Find the corresponding DisconnectDefenseAgentEnabler
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {  
        return 
          (o instanceof DisconnectDefenseAgentEnabler);
      }
    };

    DisconnectDefenseAgentEnabler cond = null;
    Collection c = blackboard.query(pred);
    Iterator iter = c.iterator();
    //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
    while (iter.hasNext()) {
       cond = (DisconnectDefenseAgentEnabler)iter.next();
       if (cond.compareSignature(agentID.getAssetType(), agentID.getAsset(), agentID.getDefenseName())) {
            if (logger.isDebugEnabled()) logger.debug("Removing "
                    +cond.getClass()+" "
                    +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue());
           blackboard.publishRemove(cond);
           return true;
       }
    }

    return false;
  }

    private boolean removeMonitoringEnabler(AgentExistsCondition agentID) {
    // Find the corresponding DisconnectMonitoringAgentEnabler
        UnaryPredicate pred = new UnaryPredicate() {
          public boolean execute(Object o) {  
            return 
              (o instanceof DisconnectMonitoringAgentEnabler);
          }
        };

        DisconnectMonitoringAgentEnabler cond = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
           cond = (DisconnectMonitoringAgentEnabler)iter.next();
           //if (logger.isDebugEnabled()) logger.debug(cond.getAssetType()+" "+cond.getAsset());
           if (cond.compareSignature(agentID.getAssetType(), agentID.getAsset(), agentID.getDefenseName())) {
                if (logger.isDebugEnabled()) logger.debug("Removing "
                        +cond.getClass()+" "
                        +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue());
               blackboard.publishRemove(cond);
               return true;
           }
        }

        return false;
      }
      
    private boolean removeReconnectTimeCondition(AgentExistsCondition agentID) {
    // Find the corresponding ReconnectTimeCondition
        UnaryPredicate pred = new UnaryPredicate() {
          public boolean execute(Object o) {  
            return 
              (o instanceof ReconnectTimeCondition);
          }
        };

        ReconnectTimeCondition cond = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
           cond = (ReconnectTimeCondition)iter.next();
           //if (logger.isDebugEnabled()) logger.debug(cond.getAssetType()+" "+cond.getAsset());
           if (cond.compareSignature(agentID.getAssetType(), agentID.getAsset(), agentID.getDefenseName())) {
                if (logger.isDebugEnabled()) logger.debug("Removing "
                        +cond.getClass()+" "
                        +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue());
               blackboard.publishRemove(cond);
               return true;
           }
        }

        return false;
      }      
}
