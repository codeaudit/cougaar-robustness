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

public class OpModeToConditionPlugin extends ServiceUserPluginBase
{
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;
  
  private static String nodeID = "NodeA";
  
  private IncrementalSubscription defenseModeSubscription; 
  private IncrementalSubscription monitoringModeSubscription;
  private IncrementalSubscription applicabilityConditionSubscription;
  
  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class
  };
    
    
  public OpModeToConditionPlugin() {
        super(requiredServices);
    }
    
  public void load() {
      super.load();
      cancelTimer();
  }
  
  public void setupSubscriptions() {
    
     haveServices(); 
     if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");

     //Listen for changes in out defense mode objects
     defenseModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof PlannedDisconnectDefenseOperatingMode ) {
                return true ;
            }
            return false ;
        }
    }) ;
    
     //Listen for changes in our enabling mode objects
     monitoringModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     });
   
     
     //Listen for changes to Applicability Conditions
     applicabilityConditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof PlannedDisconnectApplicabilityCondition ) {
                return true ;
            }
            return false ;
        }
        
     }) ;         
     
      if (logger.isInfoEnabled()) {
         logger.info ("Published condition and two OpMode ");
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
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }
    
  public void execute() {

      System.out.println();
      System.out.println(new Date() + " in OpModeToCondition");

      Iterator iter;
     
      //********* Check for changes in our modes ************
      
      //We have one defense mode & one defense mode condition, so we only get the one from iterX.next();
      iter = defenseModeSubscription.getAddedCollection().iterator();
      while (iter.hasNext()) {
          PlanedDisconnectDefenseOperatingMode dmode = (PlannedDisconnectDefenseOperatingMode)iter.next();
          getBlackboardService().publishAdd
            (new MyDefenseEnablerCondition(dmode.getName(), dmode.getValue()));
          if (dmode != null) {
            System.out.println(dmode.getName() + " set to " + dmode.getValue());
          }
      }
  }
    
}