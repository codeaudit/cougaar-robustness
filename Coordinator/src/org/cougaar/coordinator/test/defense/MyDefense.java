 
/* 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
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
 * involving that asset. Then, it would act only if the DefenseEnablingOperatingMode
 * object was properly set (as described in the Defense Deconfliction API &
 * Architecture paper).
 *@deprecated
 */
public class MyDefense extends ServiceUserPluginBase {
    
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private UIDService us = null;

  private IncrementalSubscription myOpModes;
  private IncrementalSubscription defenseModeSubscription;      
  private IncrementalSubscription enablingModeSubscription;

  private static final Class[] requiredServices = {
    ConditionService.class,
    OperatingModeService.class,
    UIDService.class
  };
  
  /** Name of condition object */
 // public static final String MYCONDITION_NAME = "MyDefense.MyCondition";
  /** Name of Defense Enabling Operating Mode object */
 // public static final String MYDEF_OPMODE_NAME = "MyDefense.MyEnabler";
  /** Name of Monitor Enabling Operating Mode object */
//  public static final String MYMONITORING_OPMODE_NAME = "MyDefense.MyMonitor";

  /** Create a new MyDefense instance */
  public MyDefense() {
    super(requiredServices);
  }

  /** load method */
  public void load() {
      super.load();
      cancelTimer();
  }

  /** Set up needed subscriptions */
  public void setupSubscriptions() {
             
     if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");

     if (!haveServices()) {
        logger.error(".haveServices - did NOT acquire services.");
        return;
     } 

     initObjects(); //create & publish condition and op mode objects
/*
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
     enablingModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof MyMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     }) ;
     
      if (logger.isInfoEnabled()) {
         logger.info ("Published condition and two OpMode ");
      }
 */
  }

  //Create one condition and one of each type of operating mode
  private void initObjects() {
/*     MyCondition dabc = 
        new MyCondition(MYCONDITION_NAME, "assetType", "defenseName", DefenseConstants.BOOL_FALSE);
     MyDefenseEnabler deom = 
        new MyDefenseEnabler(MYDEF_OPMODE_NAME,"assetType", "defenseName");
     MyMonitoringEnabler meom = 
        new MyMonitoringEnabler(MYMONITORING_OPMODE_NAME,"assetType", "defenseName");

     //These InterAgents need UIDs.
     deom.setUID(us.nextUID());
     meom.setUID(us.nextUID());
     
     
      getBlackboardService().publishAdd(dabc);
      getBlackboardService().publishAdd(deom);
      getBlackboardService().publishAdd(meom);
*/
      setTestCondition();
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
    
    logger.error(".haveServices - did NOT acquire services.");    
    return false;
    
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
      if (iter.hasNext()) {
          DefenseEnablingOperatingMode dmode = (DefenseEnablingOperatingMode)iter.next();
          if (dmode != null) {
            if (logger.isDebugEnabled()) logger.debug("**** Added DefenseEnablingOperatingMode with value = " + dmode.getValue());
          }
      }
      //We have one defense mode, so we only get the one from iter.next();
      iter = enablingModeSubscription.getAddedCollection().iterator();
      if (iter.hasNext()) {      
          MonitoringEnablingOperatingMode mmode = (MonitoringEnablingOperatingMode)iter.next();
          if (mmode != null) {
            if (logger.isDebugEnabled()) logger.debug("**** Added MonitoringEnablingOperatingMode with value = " + mmode.getValue());
          }
      }
      
      
      
      
      //********* Check for changes in our modes ************
      
      //We have one defense mode, so we only get the one from iter.next();
      iter = defenseModeSubscription.getChangedCollection().iterator();
      if (iter.hasNext()) {
          DefenseEnablingOperatingMode dmode = (DefenseEnablingOperatingMode)iter.next();
          if (dmode != null) {
              if (logger.isDebugEnabled()) logger.debug("****DefenseEnablingOperatingMode set to " + dmode.getValue());
          }
      }
      //We have one defense mode, so we only get the one from iter.next();
      iter = enablingModeSubscription.getChangedCollection().iterator();
      if (iter.hasNext()) {      
          MonitoringEnablingOperatingMode mmode = (MonitoringEnablingOperatingMode)iter.next();
          if (mmode != null) {
              if (logger.isDebugEnabled()) logger.debug("****MonitoringEnablingOperatingMode set to " + mmode.getValue());
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
          if (logger.isDebugEnabled()) logger.debug("** Timer not expired");
      }      
 */
  }

  /* Periodically change the condition value from TRUE to FALSE and back to 
   * allow testing of the plays.
   */
  private void setTestCondition() {
/*      
      if (logger.isDebugEnabled()) logger.debug("** In setTestCondition");
      MyCondition cond = 
        (MyCondition)conditionService.getConditionByName(MYCONDITION_NAME);
      if (cond != null) {
          
          if (cond.getValue().equals("FALSE")) {
              cond.setValue(DefenseConstants.BOOL_TRUE);
              if (logger.isDebugEnabled()) logger.debug("** setTestCondition - Condition set to "+ cond.getValue());
          }
          else {
              cond.setValue(DefenseConstants.BOOL_FALSE);
              if (logger.isDebugEnabled()) logger.debug("** setTestCondition - Condition set to "+ cond.getValue());
          }
              
          getBlackboardService().publishChange(cond);
      } else {
	if (logger.isDebugEnabled()) logger.debug("** Cannot find condition object!");
      }          
    startTimer(10000);
 */
  }

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  /*
   private static class MyCondition extends  DefenseApplicabilityBinaryCondition implements NotPersistable {
    public MyCondition(String a, String b, String c) {
      super(a,b,c);
    }

    public MyCondition(String a, String b, String c, DefenseConstants.OMCStrBoolPoint pt) {
      super(a,b,c, pt);
    }
    
    public void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
      super.setValue(newValue);
    }
  }
   
  //  DefenseEnablingOperatingMode subtype for the Defense Enabling Operating Mode 
  public class MyDefenseEnabler extends DefenseEnablingOperatingMode {
     public MyDefenseEnabler(String a, String b, String c) {
      super(a,b,c);
    } 
  }

  //  MonitoringEnablingOperatingMode subtype for the Monitoring Enabling Operating Mode 
  public class MyMonitoringEnabler extends MonitoringEnablingOperatingMode {
     public MyMonitoringEnabler(String a, String b, String c) {
      super(a,b,c);
    } 
  }
  */
}

  
