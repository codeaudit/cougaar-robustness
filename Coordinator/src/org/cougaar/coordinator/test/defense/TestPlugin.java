/*
 * TestPlugin.java
 *
 * Created on March 20, 2003, 3:36 PM
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

package org.cougaar.coordinator.test.defense;


import org.cougaar.coordinator.*;

import java.util.Iterator;
import java.util.Collection;

import org.cougaar.core.adaptivity.*;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import org.cougaar.core.persist.NotPersistable;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;

import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.adaptivity.InterAgentCondition;
import org.cougaar.core.adaptivity.InterAgentOperatingMode;
import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.AgentIdentificationService;


/**
 * This Plugin is used when the AdaptivityEngine (AE) is located in a different
 * agent. This plugin must be colocated with the Defense. It handles creating 
 * a surrogate object (relay) in the AE's agent blackboard so that values of 
 * the condition can be propagated to the AE.
 *
 * The plugin requires one argument (in the ini file), which is the name of the 
 * agent containing the AdaptivityEngine plugin. Do not instantiate this class
 * directly.
 */
public class TestPlugin extends ServiceUserPluginBase {
    
   private int count = 0;
    
    /** Name of condition object */
    public static final String MYCONDITION_NAME = "MyDefense.MyCondition";
    
    private MyCondition dabc;
    private ConditionService conditionService;
    private OperatingModeService operatingModeService;
    private IncrementalSubscription defenseConditionsSubscription;
    private IncrementalSubscription defenseOpModeSubscription;
        
    private UIDService us = null;
    
    private AgentIdentificationService ais;

    private String RemoteAgent = null;
    
    private Collection defenseOpModes = null;

    private static final Class[] requiredServices = {
        UIDService.class
    };

    
    
    /** Creates a new instance of TestPlugin */
    public TestPlugin() {
        super(requiredServices);
    }
    
    
    private void getPluginParams() {
        if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters [must supply Remote Agent name].");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
             RemoteAgent = (String)iter.next();
             logger.debug("Setting Remote Agent to = " + RemoteAgent);
        }
    }       

    public void load() {
        super.load();
        cancelTimer();
        getPluginParams();
        haveServices();
        initObjects();
    }
    

  //Create one condition and one of each type of operating mode
  private void initObjects() {
     dabc = 
        new MyCondition("MyAssetType", "MyAsset", "MyDefense", DefenseConstants.BOOL_FALSE);

     UID uid = null;
     //These InterAgents need UIDs.
     if (us == null) {
          logger.debug("us is NULL!");
          uid = new UID("me", 1234567L);
     } else {
          uid = us.nextUID();
     }    
     
     dabc.setUID(uid);
     dabc.setSourceAndTarget(agentId, MessageAddress.getMessageAddress(RemoteAgent));
     getBlackboardService().openTransaction();     
     getBlackboardService().publishAdd(dabc);
     getBlackboardService().closeTransaction();
     logger.debug("Published new Defense Condition, sending to "+ RemoteAgent);
     
     startTimer(10000);
  }      
  
  public void execute() { //do nothing 
      logger.debug("execute() called");
    
      if (timerExpired()) { //then change the value of the condition
          cancelTimer();
          setTestCondition();
      } else {
          logger.debug("** Timer not expired");
      }      
  } 
    
  public void setupSubscriptions() { //do nothing
  }
    
    private boolean haveServices() {
    if (us != null) return true;
    if (acquireServices()) {
        if (logger.isDebugEnabled()) logger.debug(".haveServices - acquiredServices.");
            ServiceBroker sb = getServiceBroker();
        //conditionService = (ConditionService)
        //sb.getService(this, ConditionService.class, null);

        //operatingModeService = (OperatingModeService)
        //sb.getService(this, OperatingModeService.class, null);
        
        // get UID service
        us = (UIDService) 
          getServiceBroker().getService(
              this, UIDService.class, null);
        if (us == null) {
          throw new RuntimeException(
              "Unable to obtain uid service");
        }

        // get agent id
        AgentIdentificationService agentIdService = 
          (AgentIdentificationService) 
          getServiceBroker().getService(
              this, AgentIdentificationService.class, null);
        if (agentIdService == null) {
          throw new RuntimeException(
              "Unable to obtain agent-id service");
        }
        agentId = agentIdService.getMessageAddress();
        getServiceBroker().releaseService(
            this, AgentIdentificationService.class, agentIdService);
        if (agentId == null) {
          throw new RuntimeException(
              "Agent id is null");
        }

        

        return true;
      }
      else if (logger.isDebugEnabled()) 
          logger.debug(".haveServices - did NOT acquire services.");
      
      return false;
    }
    
    
    public boolean publishAdd(Object o) {
        getBlackboardService().publishAdd(o);
        return true;
      }


    public boolean publishChange(Object o) {
	getBlackboardService().publishChange(o);
        return true;
    }

    public boolean publishRemove(Object o) {
	getBlackboardService().publishRemove(o);
        return true;
    }
    
    
    /* Periodically change the condition value from TRUE to FALSE and back to 
    * allow testing of the plays.
    */
    private void setTestCondition() {

              dabc.testSlot = "Foo"+(count++);
/*        
        logger.debug("** In setTestCondition -- dabc.getValue()="+dabc.getValue());
          if ("FALSE".equals(dabc.getValue())) {
              logger.debug("** setTestCondition - Condition is FALSE.");
              dabc.setValue(DefenseConstants.BOOL_TRUE);
              logger.debug("** setTestCondition - Condition set to "+ dabc.getValue());
          }
          else if ("TRUE".equals(dabc.getValue())) {
              logger.debug("** setTestCondition - Condition is TRUE.");
              dabc.setValue(DefenseConstants.BOOL_FALSE);
              logger.debug("** setTestCondition - Condition set to "+ dabc.getValue());
          } else
              logger.debug("** setTestCondition - Condition is UNKNOWN.");
             
*/
          getBlackboardService().publishChange(dabc);
          startTimer(10000);
    }

    
    

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
   static class MyCondition extends DefenseApplicabilityBinaryCondition implements NotPersistable {
    public MyCondition(String at, String a, String defenseName) {
      super(at, a, defenseName);
    }

    public MyCondition(String at, String a, String defenseName, DefenseConstants.OMCStrBoolPoint pt) {
      super(at, a, defenseName, pt);
    }
    
    public void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
      super.setValue(newValue);
    }
  }
   
  /**  DefenseEnablingOperatingMode subtype for the Defense Enabling Operating Mode */
//  public class MyDefenseEnabler extends DefenseEnablingOperatingMode {
//     public MyDefenseEnabler(String name) {
//      super(name);
//    } 
//  }

  /**  MonitoringEnablingOperatingMode subtype for the Monitoring Enabling Operating Mode */
//  public class MyMonitoringEnabler extends MonitoringEnablingOperatingMode {
//     public MyMonitoringEnabler(String name) {
//      super(name);
//    } 
//  }
    
    
}
