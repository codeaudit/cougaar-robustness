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

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
import org.cougaar.core.adaptivity.InterAgentOperatingMode;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;

import org.cougaar.core.adaptivity.InterAgentCondition;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.persist.NotPersistable;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.GenericStateModelAdapter;


public class PlannedDisconnectNodePlugin extends ServiceUserPluginBase {
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  
  private static String nodeID = "NodeA";

  //private IncrementalSubscription myOpModes;
  private IncrementalSubscription reconnectTimeConditionSubscription;
  private IncrementalSubscription applicabilityConditionSubscription;
  private IncrementalSubscription defenseModeSubscription;      
  private IncrementalSubscription monitoringModeSubscription;
  private IncrementalSubscription conditionSubscription;
  

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class
  };
  
  public static final String MY_APPLICABILITY_CONDITION_NAME 
    = "PlannedDisconnect.UnscheduledDisconnect.Node." + nodeID;
  public static final String MY_RECONNECT_TIME_NAME 
    = "PlannedDisconnect.UnscheduledReconnectTime.Node." + nodeID;
  public static final String MY_DEFENSE_OPMODE_NAME 
    = "PlannedDisconnect.NodeDefense.Node." + nodeID;
  public static final String MY_MONITORING_OPMODE_NAME 
    = "PlannedDisconnect.NodeMonitoring.Node." + nodeID;

  public PlannedDisconnectNodePlugin() {
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
            if ( o instanceof MyDefenseEnabler ) {
                return true ;
            }
            return false ;
        }
    }) ;

    //Listen for changes in our enabling mode object
     monitoringModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     });

     //Listen for changes to Conditions
     conditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseCondition ) {
                return true ;
            }
            return false ;
        }
        
     }) ;
    
    
    /*
    //Listen for changes in TechSpecs for the node & agents on the node
     techSpecSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof TechSpec ) {
                return true ;
            }
            return false ;
        }
        
     }) ;
     */
     
      if (logger.isInfoEnabled()) {
         logger.info ("Published condition and two OpMode ");
      }
      
  }


  //Create one condition and one of each type of operating mode
  private void initObjects() {
     DisconnectionApplicabilityCondition dac = 
        new MyDefenseApplicabilityCondition(MY_APPLICABILITY_CONDITION_NAME);
     ReconnectTimeCondition rtc =
        new MyReconnectTimeCondition(MY_RECONNECT_TIME_NAME);
     DefenseEnablingOperatingMode deom = 
        new MyDefenseEnabler(MY_DEFENSE_OPMODE_NAME);
     MonitoringEnablingOperatingMode meom = 
        new MyMonitoringEnabler(MY_MONITORING_OPMODE_NAME);

     //These InterAgents need UIDs.
     deom.setUID(us.nextUID());
     meom.setUID(us.nextUID());
     
     
      getBlackboardService().publishAdd(dac);
      System.out.println();
      System.out.println("NodeA Published:");
      System.out.println("Condition: " + dac.getName() + " with value = " + dac.getValue());
      System.out.println(" with allowed values  = " + dac.getAllowedValues());
      getBlackboardService().publishAdd(rtc);
      System.out.println("Condition: " + rtc.getName() + " with value = " + rtc.getValue());
      System.out.println(" with allowed values  = " + rtc.getAllowedValues());
      getBlackboardService().publishAdd(deom);
      System.out.println("OpMode: " + deom.getName() + " with value = " + deom.getValue());
      System.out.println(" with allowed values  = " + deom.getAllowedValues());
      getBlackboardService().publishAdd(meom);
      System.out.println("OpMode: " + meom.getName() + " with value = " + meom.getValue());
      System.out.println(" with allowed values  = " + meom.getAllowedValues());

      startTimer(10000);
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
      
      return true;
    }
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  public void execute() {

     System.out.println();
     System.out.println(new Date() + " in NodeDefense");
     
     Iterator iter;
      
      //********* Check for changes in our modes ************
      
      //We have one defense mode, so we only get the one from iter.next();
      iter = defenseModeSubscription.iterator();
      if (iter.hasNext()) {
          DefenseEnablingOperatingMode dmode = (DefenseEnablingOperatingMode)iter.next();
          if (dmode != null) {
            System.out.println(dmode.getName() + " set to " + dmode.getValue());
          }
      }
      //We have one defense mode, so we only get the one from iter.next();
      iter = monitoringModeSubscription.iterator();
      if (iter.hasNext()) {      
          MonitoringEnablingOperatingMode mmode = (MonitoringEnablingOperatingMode)iter.next();
          if (mmode != null) {
            System.out.println(mmode.getName() + " set to " + mmode.getValue());
          }
      }
      
      //********* Check for changes in Condition objects ************
      
      //Many possible condition objects;
      iter = conditionSubscription.iterator();
      while (iter.hasNext()) {
          DefenseCondition sc = (DefenseCondition)iter.next();
          if (sc != null) {
            System.out.println(sc.getName() + " set to " + sc.getValue());
          }
      }
      
      if (timerExpired()) { //then change the value of the condition
        if (haveServices()) {
          cancelTimer();
          setTestCondition();
        }
        else if (logger.isDebugEnabled()) {
          logger.debug(".execute - not all services ready yet.");
        }
      } else {
          logger.debug("** Timer not expired");
      }      
  }

  /* Periodically change the condition value from TRUE to FALSE and back to 
   * allow testing of the plays.
   */
  private void setTestCondition() {
      logger.debug("** In setTestCondition");
      DisconnectionApplicabilityCondition cond = 
        (DisconnectionApplicabilityCondition)conditionService.getConditionByName(MY_APPLICABILITY_CONDITION_NAME);
      ReconnectTimeCondition rtc =
        (ReconnectTimeCondition)conditionService.getConditionByName(MY_RECONNECT_TIME_NAME);
      if (cond != null) {
          
          if (cond.getValue().equals("FALSE")) {
              cond.setValue(DefenseConstants.BOOL_TRUE);
              logger.debug("** setTestCondition - Condition set to "+ cond.getValue());
              System.out.println("Disconnecting Node");
          }
          else {
              cond.setValue(DefenseConstants.BOOL_FALSE);
              logger.debug("** setTestCondition - Condition set to "+ cond.getValue());
              System.out.println("Reconnecting Node");
          }
              
          getBlackboardService().publishChange(cond);
      } else {
	logger.debug("** Cannot find condition object!");
      }    
      if (rtc != null) {
        long reconnectTime = new Date().getTime() + 5000L;
        rtc.setValue(new Double(reconnectTime));
        getBlackboardService().publishChange(rtc);
      }
    startTimer(10000);
  }

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/

  public class MyDefenseApplicabilityCondition extends DisconnectionApplicabilityCondition {
     public MyDefenseApplicabilityCondition(String name) {
      super(name, DefenseConstants.BOOL_FALSE);
    } 
  }

   
  public class MyDefenseEnabler extends DefenseEnablingOperatingMode {
     public MyDefenseEnabler(String name) {
      super(name);
    } 
  }

  public class MyMonitoringEnabler extends MonitoringEnablingOperatingMode {
     public MyMonitoringEnabler(String name) {
      super(name);
    } 
  }
  
  public class MyReconnectTimeCondition extends ReconnectTimeCondition {
    public MyReconnectTimeCondition(String name) {
      super(name);
    }
    
    public void setValue(Double newValue) {
      super.setValue(newValue);
    }
    
  }
  
}