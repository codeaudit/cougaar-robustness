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


public class PlannedDisconnectManagerPlugin extends ServiceUserPluginBase
{
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  
  private static String nodeID = "NodeA";
  
  private IncrementalSubscription myOpModes;
  private IncrementalSubscription defenseModeSubscription; 
  private IncrementalSubscription defenseModeConditionSubscription;
  private IncrementalSubscription monitoringModeSubscription;
  private IncrementalSubscription monitoringModeConditionSubscription;
  private IncrementalSubscription techSpecSubscription;
  private IncrementalSubscription conditionSubscription;
  private IncrementalSubscription opModeSubscription;

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class
  };
  
  public static final String MY_DEFENSE_OPMODE_NAME 
    = "PlannedDisconnect.Defense.Node.";
  public static final String MY_MONITORING_OPMODE_NAME 
    = "PlannedDisconnect.Monitoring.Node.";
  public static final String MY_APPLICABILITY_CONDITION_NAME
    = "PlannedDisconnect.ScheduledDisconnect.Node.";

  public static final String MY_NODE_APPLICABILITY_CONDITION_NAME
    = "PlannedDisconnect.Applicable.Node.";
  public static final String MY_RECONNECT_TIME_NAME 
    = "PlannedDisconnect.ScheduledReconnectTime.Node.";


  public PlannedDisconnectManagerPlugin() {
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
    
     defenseModeConditionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyDefenseEnablerCondition ) {
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

     monitoringModeConditionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnablerCondition ) {
                return true ;
            }
            return false ;
        }
    }) ;
     
     
     //Listen for changes to Conditions
     conditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseCondition ) {
                return true ;
            }
            return false ;
        }
        
     }) ;
     
     
     //Listen for changes to OpModes
     opModeSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseOperatingMode ) {
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
     DefenseApplicabilityCondition dac = 
        new MyDefenseApplicabilityCondition(MY_APPLICABILITY_CONDITION_NAME + nodeID);

     DefenseEnablingOperatingMode deom = 
        new MyDefenseEnabler(MY_DEFENSE_OPMODE_NAME + nodeID);
     DefenseCondition deomc =
        new MyDefenseEnablerCondition(MY_DEFENSE_OPMODE_NAME + nodeID);

     MonitoringEnablingOperatingMode meom = 
        new MyMonitoringEnabler(MY_MONITORING_OPMODE_NAME + nodeID);
     DefenseCondition meomc =
        new MyMonitoringEnablerCondition(MY_MONITORING_OPMODE_NAME + nodeID);

     DefenseApplicabilityCondition ndac = 
        new MyDefenseApplicabilityCondition(MY_NODE_APPLICABILITY_CONDITION_NAME + nodeID);
     ReconnectTimeCondition rtc =
        new MyReconnectTimeCondition(MY_RECONNECT_TIME_NAME + nodeID);
          
     //These InterAgents need UIDs.
     deom.setUID(us.nextUID());
     meom.setUID(us.nextUID());
     
     
      getBlackboardService().publishAdd(dac);
      System.out.println();
      System.out.println("ManagementAgent Published:");
      System.out.println("Condition: " + dac.getName() + " with value = " + dac.getValue());
      System.out.println(" with allowed values  = " + dac.getAllowedValues());
      getBlackboardService().publishAdd(rtc);
      System.out.println("Condition: " + rtc.getName() + " with value = " + rtc.getValue());
      System.out.println(" with allowed values  = " + rtc.getAllowedValues());
      getBlackboardService().publishAdd(deom);
      System.out.println("OpMode: " + deom.getName() + " with value = " + deom.getValue());
      System.out.println(" with allowed values  = " + deom.getAllowedValues());
      getBlackboardService().publishAdd(deomc);
      System.out.println("TransferCondition: " + deomc.getName() + " with value = " + deomc.getValue());
      System.out.println(" with allowed values  = " + deomc.getAllowedValues());
      getBlackboardService().publishAdd(meom);
      System.out.println("OpMode: " + meom.getName() + " with value = " + meom.getValue());
      System.out.println(" with allowed values  = " + meom.getAllowedValues());
      getBlackboardService().publishAdd(meomc);
      System.out.println("TransferCondition: " + meomc.getName() + " with value = " + meomc.getValue());
      System.out.println(" with allowed values  = " + meomc.getAllowedValues());
      getBlackboardService().publishAdd(ndac);
      System.out.println("Condition: " + ndac.getName() + " with value = " + ndac.getValue());
      System.out.println(" with allowed values  = " + ndac.getAllowedValues());
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
      System.out.println(new Date() + " in Community Defense");

      Iterator iter1;
      Iterator iter2;
     
      //********* Check for changes in our modes ************
      
      //We have one defense mode & one defense mode condition, so we only get the one from iterX.next();
      iter1 = defenseModeSubscription.getChangedCollection().iterator();
      if (iter1.hasNext()) {
          MyDefenseEnabler dmode = (MyDefenseEnabler)iter1.next();
          iter2 = defenseModeConditionSubscription.iterator();
          if (iter2.hasNext()) {
              MyDefenseEnablerCondition deomc = (MyDefenseEnablerCondition)iter2.next();
              deomc.setValue(dmode.getValue());
              getBlackboardService().publishChange(deomc);
          }
          if (dmode != null) {
            System.out.println(dmode.getName() + " set to " + dmode.getValue());
          }
       }
       else {
          System.out.println("no defense mode chnages");
       }



      //We have one defense mode, so we only get the one from iter.next();
      iter1 = monitoringModeSubscription.getChangedCollection().iterator();
      if (iter1.hasNext()) {      
          MyMonitoringEnabler mmode = (MyMonitoringEnabler)iter1.next();
          iter2 = monitoringModeConditionSubscription.iterator();
          if (iter2.hasNext()) {
              MyMonitoringEnablerCondition meomc = (MyMonitoringEnablerCondition)iter2.next();
              meomc.setValue(mmode.getValue());
              getBlackboardService().publishChange(meomc);
          }
          if (mmode != null) {
            System.out.println(mmode.getName() + " set to " + mmode.getValue());
          }
      }
      else {
        System.out.println("no monitoring mode chnages");
      }

      
      //********* Check for changes in Condition objects ************
      
      //Many possible condition objects;
      iter1 = conditionSubscription.iterator();
      while (iter1.hasNext()) {
          DefenseCondition sc = (DefenseCondition)iter1.next();
          if (sc != null) {
            System.out.println(sc.getName() + " set to " + sc.getValue());
          }
      }
      
      
      //********* Check for changes in OpMode objects ************
      
      //Many possible condition objects;
      iter1 = opModeSubscription.iterator();
      while (iter1.hasNext()) {
          DefenseOperatingMode sc = (DefenseOperatingMode)iter1.next();
          if (sc != null) {
            System.out.println(sc.getName() + " set to " + sc.getValue());
          }
      }
      
  }

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
  
  public class MyDefenseEnablerCondition extends DefenseCondition {
     public MyDefenseEnablerCondition (String name) {
        super(name, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());
     }
     
     public void setValue(Comparable value) {
        super.setValue(value);
     }
  }

  public class MyMonitoringEnabler extends MonitoringEnablingOperatingMode {
     public MyMonitoringEnabler(String name) {
      super(name);
    } 
    
     public void setValue(Comparable value) {
        super.setValue(value.toString());
     }
  }
  
  public class MyMonitoringEnablerCondition extends DefenseCondition {
     public MyMonitoringEnablerCondition (String name) {   
        super(name, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());
     }
     
     protected void setValue(Comparable value) {
        super.setValue(value);
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