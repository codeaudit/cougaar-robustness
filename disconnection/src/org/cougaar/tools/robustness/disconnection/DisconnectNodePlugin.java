/*
 * DisconnectNodePlugin.java
 *
 * Created on August 20, 2003, 9:03 AM
 */

/**
 *
 * @author  David Wells - OBJS
 * @version 
 *
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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

public class DisconnectNodePlugin extends DisconnectPluginBase {
    
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
  

  public DisconnectNodePlugin() {
    super();
  }

  
  public void load() {
      super.load();
  }
  
  public void setupSubscriptions() {
    
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
      
     }) ;    
  }


  
  private void initObjects() {
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
             createConditionsAndOpModes("Agent", aec.getAsset(), getNodeAddress(), managerAddress);
          }
      }

      iter = managerAddressSubscription.getAddedCollection().iterator();
      if (iter.hasNext()) {
          // find & set the ManagerAgent address
           managerAddress = ((RobustnessManagerID)iter.next()).getMessageAddress();
           if (logger.isDebugEnabled()) logger.debug("ManagerAddress: "+managerAddress.toString());

          // create conditions & opmodes for the NodeAgent
          createLocalCondition("Node", getNodeID(), getNodeAddress(), managerAddress);
          createConditionsAndOpModes("Node", getNodeID(), getNodeAddress(), managerAddress);
     
          //********** Check for new agents on the node **********
          // create conditions for all the agents that reported BEFORE we found the ManagerAgent Address
          Iterator iter2 = localAgentsSubscription.iterator();
          while (iter2.hasNext()) {
             AgentExistsCondition aec = (AgentExistsCondition) iter2.next();
             createConditionsAndOpModes("Agent", aec.getAsset(), getNodeAddress(), managerAddress);
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
              String defenseMode = dmode.getValue().toString();
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
              String monitoringMode = mmode.getValue().toString();
              if (logger.isDebugEnabled()) logger.debug("Saw: "+
                mmode.getClass()+":"+
                mmode.getName() + " set to " + mmode.getValue());
          }
      }

      
      //We have one local time condition, so we only get the one from iter.next();
      // This was set by the DisconnectServlet for the Node
      iter = localReconnectTimeSubscription.getChangedCollection().iterator();
      if (iter.hasNext()) { 
          if (getNodeAddress().equals(managerAddress)) {
              if (eventService.isEventEnabled()) {
                  eventService.event("Not allowed to Disconnect ManagementAgent on "+getNodeAddress().toString());
              }
          }
          else {
              if (logger.isDebugEnabled()) logger.debug("starting to process a changed LocalReconnectTimeCondition");
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
                  if (logger.isDebugEnabled()) logger.debug("Set the Condition for "+getNodeID()+":"+rtc.getAsset());   
              }
          }
      }
      
      // announce changes to the defenseEnabler as Cougaar Events to allow actual disconnection from ACME
      // need to iterate thru the modes to find the one for the Node Agent
      iter = defenseModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          DisconnectDefenseAgentEnabler dmode = (DisconnectDefenseAgentEnabler)iter.next();
          if (dmode != null) {
              if (dmode.getAssetType().equals("Node")) {
                  String defenseMode = dmode.getValue().toString();
                  if (eventService.isEventEnabled()) {
                      if (defenseMode.equals("ENABLED")) {
                          eventService.event(getNodeID()+" plans to Disconnect for "+reconnectInterval+" sec");
                      }
                      else {
                          eventService.event(getNodeID()+" has Reconnected");
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
          lrtc.setUID(getUIDService().nextUID());
          lrtc.setSourceAndTarget(localAddress, remoteAddress);

          getBlackboardService().publishAdd(lrtc);
          
          if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+localAddress+":"+assetID); 
    }
        

    private void createConditionsAndOpModes(String assetType, String assetID, MessageAddress localAddress, MessageAddress remoteAddress) {
        // Make the remote condition & opmodes (used for agents & the node agent)

        ReconnectTimeCondition rtc = new ReconnectTimeCondition(assetType, assetID);
        rtc.setUID(getUIDService().nextUID());
        rtc.setSourceAndTarget(localAddress, remoteAddress);

        DisconnectDefenseAgentEnabler dde = new DisconnectDefenseAgentEnabler(assetType, assetID);
        dde.setUID(getUIDService().nextUID());
        dde.setSourceAndTarget(localAddress, remoteAddress);

        DisconnectMonitoringAgentEnabler dme = new DisconnectMonitoringAgentEnabler(assetType, assetID);
        dme.setUID(getUIDService().nextUID());
        dme.setSourceAndTarget(localAddress, remoteAddress);

        getBlackboardService().publishAdd(rtc);
        getBlackboardService().publishAdd(dde);
        getBlackboardService().publishAdd(dme);

        if (logger.isDebugEnabled()) logger.debug("Created Conditions & OpModes for "+localAddress+":"+assetID); 
    }
    
    
    private void removeConditionsAndOpModes(String assetType, String assetID) {
        // Find and remove the corresponding ReconnectTimeCondition & LocalReconnectTimeCondition
        
        LocalReconnectTimeCondition lrtc = LocalReconnectTimeCondition.findOnBlackboard(assetType, assetID, getBlackboardService());       
        ReconnectTimeCondition rtc = ReconnectTimeCondition.findOnBlackboard(assetType, assetID, getBlackboardService());
        DisconnectMonitoringAgentEnabler dme = DisconnectMonitoringAgentEnabler.findOnBlackboard(assetType, assetID, getBlackboardService());
        DisconnectDefenseAgentEnabler dde = DisconnectDefenseAgentEnabler.findOnBlackboard(assetType, assetID, getBlackboardService());
        
        if (lrtc != null) blackboard.publishRemove(lrtc);
        if (rtc != null) blackboard.publishRemove(rtc);
        if (dme != null) blackboard.publishRemove(dme);
        if (dde != null) blackboard.publishRemove(dde);
        
        if (logger.isDebugEnabled()) logger.debug("Removed Conditions & OpModes for departing agent "+assetID); 
    }
}
