  
/* 
 * <copyright>
 *  Copyright 2003 Object Services & Consulting, Inc.
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


/**
 * This implements a trivial Defense to illustrate how the initial
 * Deconfliction API is used. It creates two operating mode objects 
 * - one for monitoring and one for enabling - a specific asset. It 
 * also creates a condition object that it uses to signal that it 
 * has detected a problem & believes it can solve it. The playbook used 
 * by the AdaptivityEngine determines what will happen when the
 * condition's value changes.
 * <p>
 * This class watches the operating mode objects, and reports when the 
 * values change. The Condition object's value is changed every 10 seconds 
 * to enable testing of the plays in the playbook.
 * <p>
 * In an actual implementation the Defense would only monitor an asset if the
 * MonitorEnablingOperatingMode object is set to ENABLED. At this point, it
 * could change the DefenseApplicabilityCondition to TRUE IF it saw a problem 
 * involving that asset. Then, it would act only if the DefenseCondition
 * object was properly set (as described in the Defense Deconfliction API &
 * Architecture paper).
 */
public class TestResponderPlugin extends ServiceUserPluginBase {
    
  private UIDService us = null;

  private IncrementalSubscription defenseModeSubscription;      
  private IncrementalSubscription enablingModeSubscription;

  private static final Class[] requiredServices = {
    UIDService.class
  };
  
  /** Create a new TestResponderPlugin instance */
  public TestResponderPlugin() {
    super(requiredServices);
  }

  /** load method */
  public void load() {
      super.load();
  }

  /** Set up needed subscriptions */
  public void setupSubscriptions() {
  /*           
     logger.debug("setupSubscriptions called.");

     if (!haveServices()) {
        logger.warn(".haveServices - did NOT acquire services.");
        return;
     } 

     //Listen for changes in out defense mode object
     defenseModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DefenseCondition ) {
                return true ;
            }
            return false ;
        }
    }) ;

    logger.debug("Listening for DefenseConditions");
   */
  }

  
  private boolean haveServices() {
      return true;
  }

  /** 
   * Watch for changes in the operating mode & periodically change the condition value to
   * test the firing of the plays.
   */
  public void execute() {
/*
      Iterator iter;
      //********* Check for our modes being added ***********
      //We have one defense mode, so we only get the one from iter.next();
      iter = defenseModeSubscription.getAddedCollection().iterator();
      if (iter == null) logger.debug("****nothing added to the collection...");
      if (iter.hasNext()) {
          DefenseCondition dmode = (DefenseCondition)iter.next();
          if (dmode != null) {
                logger.debug("**** Saw new DefenseCondition with value = " + dmode.getValue());
                logger.debug("********* and type = " + dmode.getClass().getName() );
                logger.debug("********* Source = " + dmode.getSource() );
                if (dmode instanceof DefenseApplicabilityBinaryCondition)
                    logger.debug("********* It's a DefenseApplicabilityBinaryCondition");                    
                else
                    logger.debug("********* It's NOT a DefenseApplicabilityBinaryCondition");                    
            }
      }
      
      //********* Check for changes in our modes ************
      
      //We have one defense mode, so we only get the one from iter.next();
      iter = defenseModeSubscription.getChangedCollection().iterator();
      if (iter == null) logger.debug("****nothing changed in the collection...");
      if (iter.hasNext()) {
          DefenseCondition dmode = (DefenseCondition)iter.next();
          if (dmode != null) {
              logger.debug("****DefenseCondition set to " + dmode.getValue());
              if (dmode instanceof DefenseApplicabilityBinaryCondition) {
                    DefenseApplicabilityBinaryCondition bc = (DefenseApplicabilityBinaryCondition)dmode;
                    logger.debug("****DefenseCondition testSlot = " + bc.testSlot);
              }
          }
      }
*/      
  }


  
}

  
