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
  
  private static String nodeID;
  
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
  public static final String MY_NODE_DEFENSE_OPMODE_NAME
    = "PlannedDisconnect.ManagerDefense.Node.";
  public static final String MY_NODE_MONITORING_OPMODE_NAME
    = "PlannedDisconnect.ManagerMonitoring.Node.";
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


    //Listen for changes in our monitoring enabling mode object
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
    // **** Future Feature to handle Scheduled Disconnects ****
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
    
     // Find out which Nodes are to be managed
     if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters [must supply AE Node name].");
     
     Iterator iter = getParameters().iterator (); 
     while (iter.hasNext()) {
    
        nodeID = (String)iter.next();
        logger.debug("Creating Conditions & OpModes for: " + nodeID);
        
     // Creating the conditions and modes for the Defense of a Node
    
        MyDefenseApplicabilityCondition dac = 
            new MyDefenseApplicabilityCondition(MY_APPLICABILITY_CONDITION_NAME + nodeID, "myAsset", "myDefense");
        MyDefenseApplicabilityOpMode daom = 
            new MyDefenseApplicabilityOpMode(MY_APPLICABILITY_CONDITION_NAME + nodeID, "myAsset", "myDefense");
        daom.setUID(us.nextUID());

        MyDefenseEnabler deom = 
            new MyDefenseEnabler(MY_DEFENSE_OPMODE_NAME + nodeID, "myAsset", "myDefense");
        deom.setUID(us.nextUID());
        MyDefenseEnablerCondition deomc = 
            new MyDefenseEnablerCondition(MY_DEFENSE_OPMODE_NAME + nodeID, "myAsset", "myDefense");

        MyMonitoringEnabler meom = 
            new MyMonitoringEnabler(MY_MONITORING_OPMODE_NAME + nodeID, "myAsset", "myDefense");
        meom.setUID(us.nextUID());
        MyMonitoringEnablerCondition meomc = 
            new MyMonitoringEnablerCondition(MY_MONITORING_OPMODE_NAME + nodeID, "myAsset", "myDefense");

        // conditions & modes for the Manager Defense instance
         
        MyDefenseApplicabilityCondition ndac = 
            new MyDefenseApplicabilityCondition(MY_NODE_APPLICABILITY_CONDITION_NAME + nodeID, "myAsset", "myDefense");

        MyDefenseEnabler ndeom = 
            new MyDefenseEnabler(MY_NODE_DEFENSE_OPMODE_NAME + nodeID, "myAsset", "myDefense");
        ndeom.setUID(us.nextUID());
        MyDefenseEnablerCondition ndeomc = 
            new MyDefenseEnablerCondition(MY_NODE_DEFENSE_OPMODE_NAME + nodeID, "myAsset", "myDefense");

        MyMonitoringEnabler nmeom = 
            new MyMonitoringEnabler(MY_NODE_MONITORING_OPMODE_NAME + nodeID, "myAsset", "myDefense");
        nmeom.setUID(us.nextUID());
        MyMonitoringEnablerCondition nmeomc = 
            new MyMonitoringEnablerCondition(MY_NODE_MONITORING_OPMODE_NAME + nodeID, "myAsset", "myDefense");
            
        ReconnectTimeCondition rtc =
            new MyReconnectTimeCondition(MY_RECONNECT_TIME_NAME + nodeID, "myAsset", "myDefense");
            
            
        if (logger.isDebugEnabled()) {
            logger.debug("");
            logger.debug("ManagementAgent Published:");
        }
              
        getBlackboardService().publishAdd(dac);
        getBlackboardService().publishAdd(daom);
        if (logger.isDebugEnabled()) {
            logger.debug("Condition: " + dac.getName() + " with value = " + dac.getValue());
            logger.debug(" with allowed values  = " + dac.getAllowedValues());
            logger.debug("TransferOpMode: " + daom.getName() + " with value = " + daom.getValue());
            logger.debug(" with allowed values  = " + daom.getAllowedValues());
        }

        getBlackboardService().publishAdd(deom);
        getBlackboardService().publishAdd(deomc);
        if (logger.isDebugEnabled()) {
            logger.debug("OpMode: " + deom.getName() + " with value = " + deom.getValue());
            logger.debug(" with allowed values  = " + deom.getAllowedValues());
            logger.debug("TransferCondition: " + deomc.getName() + " with value = " + deomc.getValue());
            logger.debug(" with allowed values  = " + deomc.getAllowedValues());
        }
          
        getBlackboardService().publishAdd(meom);
        getBlackboardService().publishAdd(meomc);
        if (logger.isDebugEnabled()) {
            logger.debug("OpMode: " + meom.getName() + " with value = " + meom.getValue());
            logger.debug(" with allowed values  = " + meom.getAllowedValues());
            logger.debug("TransferCondition: " + meomc.getName() + " with value = " + meomc.getValue());
            logger.debug(" with allowed values  = " + meomc.getAllowedValues());
        }

        getBlackboardService().publishAdd(ndac);
        getBlackboardService().publishAdd(rtc);
        if (logger.isDebugEnabled()) {
            logger.debug("Condition: " + ndac.getName() + " with value = " + ndac.getValue());
            logger.debug(" with allowed values  = " + ndac.getAllowedValues());
            logger.debug("Condition: " + rtc.getName() + " with value = " + rtc.getValue());
            logger.debug(" with allowed values  = " + rtc.getAllowedValues());
        }

        getBlackboardService().publishAdd(ndeom);
        getBlackboardService().publishAdd(ndeomc);
        if (logger.isDebugEnabled()) {
            logger.debug("OpMode: " + ndeom.getName() + " with value = " + ndeom.getValue());
            logger.debug(" with allowed values  = " + ndeom.getAllowedValues());
            logger.debug("TransferCondition: " + ndeomc.getName() + " with value = " + ndeomc.getValue());
            logger.debug(" with allowed values  = " + ndeomc.getAllowedValues());
        }
          
        getBlackboardService().publishAdd(nmeom);
        getBlackboardService().publishAdd(nmeomc);
        if (logger.isDebugEnabled()) {
            logger.debug("OpMode: " + nmeom.getName() + " with value = " + nmeom.getValue());
            logger.debug(" with allowed values  = " + nmeom.getAllowedValues());
            logger.debug("TransferCondition: " + nmeomc.getName() + " with value = " + nmeomc.getValue());
            logger.debug(" with allowed values  = " + nmeomc.getAllowedValues());
        }
     }

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
    else if (logger.isWarnEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  public void execute() {

      if (logger.isDebugEnabled()) {
        logger.debug("");
        logger.debug(new Date() + " in Community Defense");
      }

      Iterator iter;
      
      //********* Check for changes in Op Modes & transfer to corresponding Conditions **********
      if (logger.isDebugEnabled()) {
        logger.debug("");
        logger.debug("Transferring OpModes to Conditions");
      }
      
      iter = opModeTransferSubscription.iterator();
      while (iter.hasNext()) {
          DefenseOperatingMode mode = (DefenseOperatingMode)iter.next();
          DefenseCondition cond = (DefenseCondition)conditionService.getConditionByName(mode.getName());
          if (cond instanceof MyDefenseEnablerCondition) {
            MyDefenseEnablerCondition c = (MyDefenseEnablerCondition)cond;
            c.setValue(mode.getValue());
            if (logger.isDebugEnabled()) logger.debug(mode.getName() + " set to " + mode.getValue());
          }
          else if (cond instanceof MyMonitoringEnablerCondition) {
            MyMonitoringEnablerCondition c = (MyMonitoringEnablerCondition)cond;
            c.setValue(mode.getValue());
            if (logger.isDebugEnabled()) logger.debug(mode.getName() + " set to " + mode.getValue());
          }
          else if (cond instanceof MyDefenseApplicabilityCondition) {
            MyDefenseApplicabilityCondition c = (MyDefenseApplicabilityCondition)cond;
            c.setValue(mode.getValue());
            if (logger.isDebugEnabled()) logger.debug(mode.getName() + " set to " + mode.getValue());
          }
      }
          
       
      //********* Check for changes in Condition objects ************
      
      if (logger.isDebugEnabled()) {
        logger.debug("");
        logger.debug("Conditions");
      }
      iter = conditionSubscription.iterator();
      while (iter.hasNext()) {
          Condition sc = (Condition)iter.next();
          if (sc != null) {
            if (logger.isDebugEnabled()) logger.debug(sc.getName() + " set to " + sc.getValue());
          }
      }
      
      
      //********* Check for changes in OpMode objects ************
      
      if (logger.isDebugEnabled()) {
        logger.debug("");
        logger.debug("OpModes");
      }
      iter = opModeSubscription.iterator();
      while (iter.hasNext()) {
          DefenseOperatingMode sc = (DefenseOperatingMode)iter.next();
          if (sc != null) {
            if (logger.isDebugEnabled()) logger.debug(sc.getName() + " set to " + sc.getValue());
          }
      }
            
  }
  

  public class MyDefenseApplicabilityCondition extends DisconnectionApplicabilityCondition {
     private String name;
     public MyDefenseApplicabilityCondition(String a, String b, String c) {
      super(a,b,c);
      } 
      
    public void setValue(Comparable value) {
        super.setValue(value);
      }      
  }
  
  public class MyDefenseApplicabilityOpMode extends DefenseOperatingMode {
    public MyDefenseApplicabilityOpMode(String a, String b, String c) {
        super(a,b,c, DefenseConstants.BOOL_RANGELIST);
    }
    public void setValue(Comparable value) {
        super.setValue(value);
     }
  }
  
  
  
  public class MyDefenseEnabler extends DefenseEnablingOperatingMode {
     public MyDefenseEnabler(String a, String b, String c) {
        super(a,b,c);
     } 
     public void setValue(Comparable value) {
        super.setValue(value);
     }
  }
  
  public class MyDefenseEnablerCondition extends DefenseCondition {
     public MyDefenseEnablerCondition (String a, String b, String c) {
        super(a,b,c, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());
     }
     public void setValue(Comparable value) {
        super.setValue(value);
     }
  }


   
  public class MyMonitoringEnabler extends MonitoringEnablingOperatingMode {
     public MyMonitoringEnabler(String a, String b, String c) {
      super(a,b,c);
    } 
     public void setValue(Comparable value) {
      super.setValue(value);
    }
  }
  
  public class MyMonitoringEnablerCondition extends DefenseCondition {
     public MyMonitoringEnablerCondition (String a, String b, String c) {   
        super(a,b,c, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());
     }
     public void setValue(Comparable value) {
        super.setValue(value);
     }
  }
    
  public class MyReconnectTimeCondition extends ReconnectTimeCondition {
    public MyReconnectTimeCondition(String a, String b, String c) {
      super(a,b,c);
    }
    
    public void setValue(Double newValue) {
      super.setValue(newValue);
    }
    
  }


}