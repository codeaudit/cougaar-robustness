/*
 * DefenseOperatingMode.java
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
import java.util.Hashtable;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
import org.cougaar.core.adaptivity.InterAgentOperatingMode;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.community.CommunityService;


import org.cougaar.core.adaptivity.InterAgentCondition;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.persist.NotPersistable;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;

import java.util.Collection;

public class DisconnectManagerPlugin extends ServiceUserPluginBase
{
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  private NodeIdentificationService nodeIdentificationService;
  private AgentIdentificationService agentIdentificationService;
  private EventService eventService;
  private CommunityService communityService;
  
  private MessageAddress assetAddress;
  private String assetID;
  private MessageAddress managerAddress;
  private String managerID;
  private static String nodeID;
  private String MANAGER_NAME;
  
  private IncrementalSubscription reconnectTimeConditionSubscription;
  private IncrementalSubscription defenseOpModeSubscription;
  private IncrementalSubscription monitoringOpModeSubscription;
  
  private Hashtable activeDisconnects = new Hashtable();

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class,
    EventService.class,
    CommunityService.class
  };
  

  public DisconnectManagerPlugin() {
    super(requiredServices);
  }

  
  public void load() {
      super.load();
      cancelTimer();
  }
  
  public void setupSubscriptions() {
    
     haveServices(); 
     if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");

     getPluginParams();

     initObjects(); //create & publish condition and op mode objects - none yet
      
     //Listen for changes to Conditions
     reconnectTimeConditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof ReconnectTimeCondition ) {
                return true ;
            }
            return false ;
        }
     });

     //Listen for changes to DefenseOpModes
     defenseOpModeSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DisconnectDefenseEnabler ) {
                return true ;
            }
            return false ;
        }
     });

     //Listen for changes to MonitoringOpModes
     monitoringOpModeSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DisconnectMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     });
         
  }

  
  //Create one condition and one of each type of operating mode
  private void initObjects() {
      // Find the ManagerAgent
      //communityService.searchCommunity(
  }
  
  private boolean haveServices() {
    if (conditionService != null && operatingModeService != null && us != null) return true;
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
      
      this.communityService = (CommunityService)
          sb.getService(this, CommunityService.class, null);
      if (communityService == null) {
          throw new RuntimeException("Unable to obtain CommunityService");
      }
          
      agentIdentificationService = (AgentIdentificationService)
        sb.getService(this, AgentIdentificationService.class, null);
      if (agentIdentificationService == null) {
          throw new RuntimeException(
              "Unable to obtain agent-id service");
      }
      else if (logger.isDebugEnabled()) logger.debug(agentIdentificationService.toString());
     
      return true;
    }
    else if (logger.isWarnEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  
  // Takes the Name of the Manager Agent as a required String parameter
  private void getPluginParams() {
      if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters [must supply the name of the Manager Agent].");

      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           MANAGER_NAME = (String)iter.next();
           logger.debug("Setting Manager Agent Name = " + MANAGER_NAME);
      }
  }  
  
  
  public void execute() {

      Iterator iter;
    
      // A new Disconnect remote has appeared, so create the Defense Conditions & OpModes for it     
      assetAddress = agentIdentificationService.getMessageAddress();
      assetID = agentIdentificationService.getName();
      managerAddress = MessageAddress.getMessageAddress(MANAGER_NAME);
      iter = reconnectTimeConditionSubscription.getAddedCollection().iterator();
      while (iter.hasNext()) {
          ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
          if (rtc != null) {
            
            DisconnectApplicabilityCondition dac = new DisconnectApplicabilityCondition(rtc.getAssetType(), rtc.getAsset());
            double t = ((Double)rtc.getValue()).doubleValue();
            if (t > 0.0) {
                dac.setValue(DefenseConstants.BOOL_TRUE); // disconnected
                OverdueAlarm overdueAlarm = new OverdueAlarm(dac, t > 10000.0 ? t : 10000.0);  // Don't monitor for less than 5 sec
                activeDisconnects.put(dac, overdueAlarm);
                getAlarmService().addRealTimeAlarm(overdueAlarm);
            }
            else {
                dac.setValue(DefenseConstants.BOOL_FALSE); // not disconnected
            }          
            dac.setUID(us.nextUID());
            dac.setSourceAndTarget(assetAddress, managerAddress);
            blackboard.publishAdd(dac);
            
            DisconnectDefenseEnabler dde = new DisconnectDefenseEnabler(rtc.getAssetType(), rtc.getAsset());
            dde.setUID(us.nextUID());
            dde.setSourceAndTarget(assetAddress, managerAddress);
            blackboard.publishAdd(dde);
            
            DisconnectMonitoringEnabler dme = new DisconnectMonitoringEnabler(rtc.getAssetType(), rtc.getAsset());
            dme.setUID(us.nextUID());
            dme.setSourceAndTarget(assetAddress, managerAddress);
            blackboard.publishAdd(dme);
            
            if (logger.isDebugEnabled()) logger.debug("Added "+rtc.getAsset()+" Conditions & OpModes for Coordinator");
            
          }
      }

      iter = reconnectTimeConditionSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          ReconnectTimeCondition sc = (ReconnectTimeCondition)iter.next();
          if (sc != null) {
            if (logger.isDebugEnabled()) logger.debug("Changed "+sc.getAsset() + " set to " + sc.getValue());
            handleDisconnectChange(sc);
          }
      }

      iter = defenseOpModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          DisconnectDefenseEnabler sc = (DisconnectDefenseEnabler)iter.next();
          if (sc != null) {
            if (logger.isDebugEnabled()) logger.debug(sc.getClass()+":"+sc.getAsset() + " set to " + sc.getValue());
          }
          propagateDefenseEnablerChange(sc); 
      }    
      
      
      iter = monitoringOpModeSubscription.getChangedCollection().iterator();
      while (iter.hasNext()) {
          DisconnectMonitoringEnabler sc = (DisconnectMonitoringEnabler)iter.next();
          if (sc != null) {
            if (logger.isDebugEnabled()) logger.debug(sc.getClass()+":"+sc.getAsset() + " set to " + sc.getValue());
          }
          propagateMonitoringEnablerChange(sc); 
      }    
      
  }
  

      private boolean handleDisconnectChange(ReconnectTimeCondition rtc) {
          
          // Find the corresponding DisconnectApplicabilityCondition
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {  
                return 
                  (o instanceof DisconnectApplicabilityCondition);
              }
            };
            
            double t = ((Double)rtc.getValue()).doubleValue();
            DisconnectApplicabilityCondition cond = null;
            Collection c = blackboard.query(pred);
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
               cond = (DisconnectApplicabilityCondition)iter.next();
               if (cond.compareSignature(rtc.getAssetType(), rtc.getAsset(), rtc.getDefenseName())) {
                    if (t > 0.0) {
                        cond.setValue(DefenseConstants.BOOL_TRUE); // disconnected
                        OverdueAlarm overdueAlarm = new OverdueAlarm(cond, t > 10000.0 ? t : 10000.0);  // Don't monitor for less than 5 sec
                        activeDisconnects.put(cond, overdueAlarm);
                        getAlarmService().addRealTimeAlarm(overdueAlarm);
                    }
                    else {
                        cond.setValue(DefenseConstants.BOOL_FALSE); // not disconnected
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(cond);
                        if (overdueAlarm != null) overdueAlarm.cancel();
                    }
                    if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                            +cond.getClass()+" "
                            +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue()+ " "+t
                            +" for the Coordinator");
                    blackboard.publishChange(cond);
                    return true;
               }
            }

            return false;
      }

      private boolean propagateMonitoringEnablerChange(DisconnectMonitoringEnabler dme) {
          
          // Find the corresponding DisconnectMonitoringAgentEnabler
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {  
                return 
                  (o instanceof DisconnectMonitoringAgentEnabler);
              }
            };
            
            //if (logger.isDebugEnabled()) logger.debug("Starting to PROGPAGATE MonitoringEnabler");
            DisconnectMonitoringAgentEnabler cond = null;
            Collection c = blackboard.query(pred);
            Iterator iter = c.iterator();
            //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
            while (iter.hasNext()) {
               cond = (DisconnectMonitoringAgentEnabler)iter.next();
               //if (logger.isDebugEnabled()) logger.debug(cond.getAssetType()+" "+cond.getAsset());
               if (cond.compareSignature(dme.getAssetType(), dme.getAsset(), dme.getDefenseName())) {
                   cond.setValue(dme.getValue());
                    if (logger.isDebugEnabled()) logger.debug("Propagating "
                            +cond.getClass()+" "
                            +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue());
                   blackboard.publishChange(cond);
                   return true;
               }
            }

            return false;
      }
      
      private boolean propagateDefenseEnablerChange(DisconnectDefenseEnabler dme) {
          
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
            while (iter.hasNext()) {
               cond = (DisconnectDefenseAgentEnabler)iter.next();
               if (cond.compareSignature(dme.getAssetType(), dme.getAsset(), dme.getDefenseName())){ 
                   cond.setValue(dme.getValue());
                   if (logger.isDebugEnabled()) logger.debug("Propagating "
                            +cond.getClass()+" "
                            +cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName()+" " +cond.getValue());
                   blackboard.publishChange(cond);
                   return true;
               }
            }

            return false;
      }  
      
    private class OverdueAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        DisconnectApplicabilityCondition cond;
        
        public OverdueAlarm (DisconnectApplicabilityCondition cond, double t) {
            detonate = System.currentTimeMillis() + (long) t;
            this.cond = cond;
            if (logger.isDebugEnabled()) logger.debug("OverdueAlarm created : "+cond.getAsset()+ " at time "+detonate);
        }
        
        public long getExpirationTime () {return detonate;
        }
        
        public void expire () {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + cond.getAsset()+" no longer legitimately Disconnected");
                if (eventService.isEventEnabled()) eventService.event(cond.getAsset()+" is no longer legitimately Disconnected");
                blackboard.openTransaction();
                cond.setValue(DefenseConstants.BOOL_FALSE);
                blackboard.publishChange(cond);
                blackboard.closeTransaction();
            }
        }
        public boolean hasExpired () {return expired;
        }
        public boolean cancel () {
            if (!expired)
                return expired = true;
            return false;
        }
 
    }


}