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
  private IncrementalSubscription relays;
  private IncrementalSubscription opModeTransferSubscription ;

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
    = "PlannedDisconnect.Applicable.Node.";

  public static final String MY_NODE_APPLICABILITY_CONDITION_NAME
    = "PlannedDisconnect.ScheduledDisconnect.Node.";
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
     
     //Listen for changes in out op mode objects for transfer to conditions
     opModeTransferSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseOperatingMode )  {
                return true ;
            }
            return false ;
        }
    }) ;
     

     //Listen for changes in out defense mode object
     defenseModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyDefenseEnabler ) {
                return true ;
            }
            return false ;
        }
    }) ;

     /*defenseModeConditionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyDefenseEnablerCondition ) {
                return true ;
            }
            return false ;
        }
    }) ;
*/

    //Listen for changes in our monitoring enabling mode object
     monitoringModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     });

/*     
     monitoringModeConditionSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnablerCondition ) {
                return true ;
            }
            return false ;
        }
    }) ;
*/     
     
     //Listen for changes to Conditions
     conditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof Condition ) {
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
     
          relays = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof InterAgentOperatingMode ) {
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
     MyDefenseApplicabilityCondition dac = 
        new MyDefenseApplicabilityCondition(MY_APPLICABILITY_CONDITION_NAME + nodeID);
     MyDefenseApplicabilityOpMode daom = 
        new MyDefenseApplicabilityOpMode(MY_APPLICABILITY_CONDITION_NAME + nodeID);

     MyDefenseEnabler deom = 
        new MyDefenseEnabler(MY_DEFENSE_OPMODE_NAME + nodeID);
     MyDefenseEnablerCondition deomc = 
        new MyDefenseEnablerCondition(MY_DEFENSE_OPMODE_NAME + nodeID);

     MyMonitoringEnabler meom = 
        new MyMonitoringEnabler(MY_MONITORING_OPMODE_NAME + nodeID);
     MyMonitoringEnablerCondition meomc = 
        new MyMonitoringEnablerCondition(MY_MONITORING_OPMODE_NAME + nodeID);

     MyDefenseApplicabilityCondition ndac = 
        new MyDefenseApplicabilityCondition(MY_NODE_APPLICABILITY_CONDITION_NAME + nodeID);
     MyDefenseApplicabilityOpMode ndaom = 
        new MyDefenseApplicabilityOpMode(MY_NODE_APPLICABILITY_CONDITION_NAME + nodeID);

     ReconnectTimeCondition rtc =
        new MyReconnectTimeCondition(MY_RECONNECT_TIME_NAME + nodeID);
        
        
      System.out.println();
      System.out.println("ManagementAgent Published:");
          
      getBlackboardService().publishAdd(dac);
      getBlackboardService().publishAdd(daom);
      System.out.println("Condition: " + dac.getName() + " with value = " + dac.getValue());
      System.out.println(" with allowed values  = " + dac.getAllowedValues());
      System.out.println("TransferOpMode: " + daom.getName() + " with value = " + daom.getValue());
      System.out.println(" with allowed values  = " + daom.getAllowedValues());

      /*
      getBlackboardService().publishAdd(rtc);
      System.out.println("Condition: " + rtc.getName() + " with value = " + rtc.getValue());
      System.out.println(" with allowed values  = " + rtc.getAllowedValues());
      */
      
      getBlackboardService().publishAdd(deom);
      getBlackboardService().publishAdd(deomc);
      System.out.println("OpMode: " + deom.getName() + " with value = " + deom.getValue());
      System.out.println(" with allowed values  = " + deom.getAllowedValues());
      System.out.println("TransferCondition: " + deomc.getName() + " with value = " + deomc.getValue());
      System.out.println(" with allowed values  = " + deomc.getAllowedValues());
      
      getBlackboardService().publishAdd(meom);
      getBlackboardService().publishAdd(meomc);
      System.out.println("OpMode: " + meom.getName() + " with value = " + meom.getValue());
      System.out.println(" with allowed values  = " + meom.getAllowedValues());
      System.out.println("TransferCondition: " + meomc.getName() + " with value = " + meomc.getValue());
      System.out.println(" with allowed values  = " + meomc.getAllowedValues());

      /*getBlackboardService().publishAdd(ndac);
      getBlackboardService().publishAdd(ndaom);
      System.out.println("Condition: " + ndac.getName() + " with value = " + ndac.getValue());
      System.out.println(" with allowed values  = " + ndac.getAllowedValues());
      System.out.println("TransferOpMode: " + ndaom.getName() + " with value = " + ndaom.getValue());
      System.out.println(" with allowed values  = " + ndaom.getAllowedValues());
      */ 
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

      Iterator iter;
      
      //********* Check for changes in Op Modes & transfer to corresponding Conditions **********
      System.out.println();
      System.out.println("Transferring OpModes to Conditions");
      iter = opModeTransferSubscription.iterator();
      while (iter.hasNext()) {
          DefenseOperatingMode mode = (DefenseOperatingMode)iter.next();
          System.out.println(mode.getName());
          DefenseCondition cond = (DefenseCondition)conditionService.getConditionByName(mode.getName());
          if (cond instanceof MyDefenseEnablerCondition) {
            MyDefenseEnablerCondition c = (MyDefenseEnablerCondition)cond;
            c.setValue(mode.getValue());
          }
          else if (cond instanceof MyMonitoringEnablerCondition) {
            MyMonitoringEnablerCondition c = (MyMonitoringEnablerCondition)cond;
            c.setValue(mode.getValue());
          }
          else if (cond instanceof MyDefenseApplicabilityCondition) {
            MyDefenseApplicabilityCondition c = (MyDefenseApplicabilityCondition)cond;
            c.setValue(mode.getValue());
          }
            
      }
          
       
      //********* Check for changes in Condition objects ************
      
      System.out.println();
      System.out.println("Conditions");
      iter = conditionSubscription.iterator();
      while (iter.hasNext()) {
          Condition sc = (Condition)iter.next();
          if (sc != null) {
            System.out.println(sc.getName() + " set to " + sc.getValue());
          }
      }
      
      
      //********* Check for changes in OpMode objects ************
      
      System.out.println();
      System.out.println("OpModes");
      iter = opModeSubscription.iterator();
      while (iter.hasNext()) {
          DefenseOperatingMode sc = (DefenseOperatingMode)iter.next();
          if (sc != null) {
            System.out.println(sc.getName() + " set to " + sc.getValue());
          }
      }
  }
  

  public class MyDefenseApplicabilityCondition extends DisconnectionApplicabilityCondition {
     private String name;
     public MyDefenseApplicabilityCondition(String name) {
      super(name);
      } 
      
    public void setValue(Comparable value) {
        super.setValue(value);
      }      
  }
  
  public class MyDefenseApplicabilityOpMode extends DefenseOperatingMode {
    public MyDefenseApplicabilityOpMode(String name) {
        super(name, DefenseConstants.BOOL_RANGELIST);
    }
    public void setValue(Comparable value) {
        super.setValue(value);
     }
  }
  
  
  
  public class MyDefenseEnabler extends DefenseEnablingOperatingMode {
     public MyDefenseEnabler(String name) {
        super(name);
     } 
     public void setValue(Comparable value) {
        super.setValue(value);
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
      super.setValue(value);
    }
  }
  
  public class MyMonitoringEnablerCondition extends DefenseCondition {
     public MyMonitoringEnablerCondition (String name) {   
        super(name, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());
     }
     public void setValue(Comparable value) {
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