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

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.adaptivity.OMCRangeList;
//import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
//import org.cougaar.core.adaptivity.InterAgentOperatingMode;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

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
  
  private MessageAddress assetAddress;
  private String assetID;
  private MessageAddress managerAddress;
  private String managerID;

  //private IncrementalSubscription myOpModes;
  private IncrementalSubscription reconnectTimeConditionSubscription;
  private IncrementalSubscription applicabilityConditionSubscription;
  private IncrementalSubscription defenseModeSubscription;      
  private IncrementalSubscription monitoringModeSubscription;
  private IncrementalSubscription conditionSubscription;
  

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class
  };
  

  public DisconnectAgentPlugin() {
    super(requiredServices);
  }

  
  public void load() {
      super.load();
      cancelTimer();
  }
  
  public void setupSubscriptions() {
    
     haveServices(); 
     if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");

     initObjects(); //create & publish condition and op mode objects

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
     
  }


  //Create one condition and one of each type of operating mode
  private void initObjects() {
    
      
     assetAddress = agentIdentificationService.getMessageAddress();
     assetID = agentIdentificationService.getName();
     managerAddress = MessageAddress.getMessageAddress(DisconnectConstants.MANAGER_NAME);

     ReconnectTimeCondition rtc =
        new ReconnectTimeCondition("Agent", assetID);
     rtc.setUID(us.nextUID());
     rtc.setSourceAndTarget(assetAddress, managerAddress);

     DisconnectDefenseAgentEnabler dde = 
        new DisconnectDefenseAgentEnabler("Agent", assetID);
     dde.setUID(us.nextUID());
     dde.setSourceAndTarget(assetAddress, managerAddress);

     DisconnectMonitoringAgentEnabler dme = 
        new DisconnectMonitoringAgentEnabler("Agent", assetID);
     dme.setUID(us.nextUID());
     dme.setSourceAndTarget(assetAddress, managerAddress);

     if (logger.isDebugEnabled()) logger.debug(assetID+" "+assetAddress+" "+DisconnectConstants.MANAGER_NAME+" "+managerAddress);
     getBlackboardService().publishAdd(rtc);
     getBlackboardService().publishAdd(dde);
     getBlackboardService().publishAdd(dme);
      
     if (logger.isDebugEnabled()) logger.debug("Published Conditions & OpModes for "+assetID);
   
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
        
      agentIdentificationService = (AgentIdentificationService)
        sb.getService(this, AgentIdentificationService.class, null);
      if (agentIdentificationService == null) {
          throw new RuntimeException(
              "Unable to obtain agent-id service");
      }
      else if (logger.isDebugEnabled()) logger.debug(agentIdentificationService.toString());
      return true;
    }
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  public void execute() {

     Iterator iter;
      
      if (logger.isDebugEnabled()) logger.debug("Agent-level Disconnect Defense in: execute()");
      //********* Check for changes in our modes ************
      
      //We have one defense mode, so we only get the one from iter.next();
      iter = defenseModeSubscription.iterator();
      while (iter.hasNext()) {
          DisconnectDefenseAgentEnabler dmode = (DisconnectDefenseAgentEnabler)iter.next();
          if (dmode != null) {
              if (logger.isDebugEnabled()) logger.debug("Saw: "+
                dmode.getClass()+":"+
                dmode.getName() + " set to " + dmode.getValue());
          }
      }


      //We have one defense mode, so we only get the one from iter.next();
      iter = monitoringModeSubscription.iterator();
      while (iter.hasNext()) {      
          DisconnectMonitoringAgentEnabler mmode = (DisconnectMonitoringAgentEnabler)iter.next();
          if (mmode != null) {
              if (logger.isDebugEnabled()) logger.debug("Saw: "+
                mmode.getClass()+":"+
                mmode.getName() + " set to " + mmode.getValue());
          }
      }
  
  }
  
}