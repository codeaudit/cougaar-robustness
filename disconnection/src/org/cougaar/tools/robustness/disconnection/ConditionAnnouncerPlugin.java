
 
/* 
 * <copyright>
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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

package test;
//package org.cougaar.logistics.plugin.trans.tools;
//org.cougaar.logistics.plugin.trans.tools.TestCondition 
import java.util.Iterator;

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
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.AgentIdentificationService;



public class ConditionAnnouncerPlugin extends ServiceUserPluginBase {
  public static final String CONDITION_NAME = "SensorTestPlugin.TestCondition";

  private ConditionService conditionService;
  private OperatingModeService operatingModeService;

  private InterAgentOperatingMode testOpMode = null;

  private IncrementalSubscription relays;
  private AgentIdentificationService ais;
  
  private static final Class[] requiredServices = {
  };

  public ConditionAnnouncerPlugin() {
    super(requiredServices);
  }

  private void getParams() {
/*    if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info ("plugin saw 0 parameters [must supply AE Node name].");

    Iterator iter = getParameters().iterator (); 
    if (iter.hasNext()) {
         AEAgent = (String)iter.next();
         logger.info ("AEAgent = " + AEAgent);
    }
 */
  }      
  
  public void load() {
      super.load();
      getParams();
  }
  
  public void setupSubscriptions() {
    
     relays = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof InterAgentCondition || o instanceof InterAgentOperatingMode ||
                    o instanceof InterAgentOperatingModePolicy) {
                    return true ;
                }
                return false ;
            }
        }) ;
  }

  public void execute() {
        for ( Iterator iter = relays.getAddedCollection().iterator() ;  iter.hasNext() ; ) {
            Object o = iter.next();
            if (o instanceof InterAgentCondition)
               logger.info("=====> New InterAgentCondition: " + ((InterAgentCondition)o).getName());
            if (o instanceof InterAgentOperatingMode)
               logger.info("=====> New InterAgentOperatingMode: " + ((InterAgentOperatingMode)o).getName());            
            if (o instanceof InterAgentOperatingModePolicy)
               logger.info("=====> New InterAgentOperatingModePolicy: " + ((InterAgentOperatingModePolicy)o).getName());            
        }        
  }
}
