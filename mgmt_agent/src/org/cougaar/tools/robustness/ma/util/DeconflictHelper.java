/*
 * <copyright>
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

package org.cougaar.tools.robustness.ma.util;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.tools.robustness.deconfliction.DefenseEnablingOperatingMode;
import org.cougaar.tools.robustness.deconfliction.MonitoringEnablingOperatingMode;
import org.cougaar.tools.robustness.deconfliction.DefenseApplicabilityBinaryCondition;
import org.cougaar.tools.robustness.deconfliction.DefenseConstants;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

/**
 * Provides convenience methods for invoking deconflictor. This helper only applies
 * to the robustness manager.
 */
public class DeconflictHelper extends BlackboardClientComponent
    implements CoordinatorHelper {

  public static final String assetType = "Agent";
  public static final String defenseName = "Restart";

  public static final long TIMER_INTERVAL = 10 * 1000;

  private LoggingService logger;
  private UIDService uidService;
  private ConditionService conditionService;
  private EventService eventService;

  private IncrementalSubscription opModeSubscription;
  private IncrementalSubscription conditionSubscription;

  private ArrayList listeners = new ArrayList(); //store all deconflict listeners
  private ArrayList agents = new ArrayList(); //store all agents who need to publish deconflict objects
  private ArrayList opModeEnabled = new ArrayList(); //store all agents whose defense opmode is enabled
  private List conditionEnabled = Collections.synchronizedList(new ArrayList()); //store all agents whose condition is true

  private List defenseOperatingModeQueue = new ArrayList(); //store all objects that need to be published into the blackboard.
  private List defenseConditionQueue = new ArrayList(); //store all conditions that need to be changed in the blackboard.

  private WakeAlarm wakeAlarm;

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs The binding site
 * @param csm The community status model
 */
  public DeconflictHelper(BindingSite bs) {
    this.setBindingSite(bs);
    initialize();
    load();
    start();
  }

  public void load() {
    setAgentIdentificationService(
      (AgentIdentificationService)getServiceBroker().getService(this, AgentIdentificationService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    eventService = (EventService) getServiceBroker().getService(this, EventService.class, null);
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);
    conditionService = (ConditionService) getServiceBroker().getService(this, ConditionService.class, null);
    if(conditionService == null && logger.isWarnEnabled()) {
      logger.warn("No ConditionService?");
    }
    super.load();
  }

  public void start() {
    super.start();
  }

  public void setupSubscriptions() {

    //listen for changes in our defense mode object
    opModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof RestartDefenseEnabler ) {
                return true ;
            }
            return false ;
        }
    });

     conditionSubscription = (IncrementalSubscription) getBlackboardService().subscribe(new UnaryPredicate() {
       public boolean execute(Object o) {
         if(o instanceof RestartDefenseCondition) {
           return true;
         }
         return false;
       }
     });

     // Start timer to periodically check all queues who needs to publish to the blackboard
    wakeAlarm = new WakeAlarm((new Date()).getTime() + TIMER_INTERVAL);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  public void execute() {
    if(!defenseOperatingModeQueue.isEmpty()) {
      fireAll();
    }

    if(!defenseConditionQueue.isEmpty()) {
      publishChangeCondition();
    }

    List temp = new ArrayList();
    Iterator it = conditionSubscription.getCollection().iterator();
    while(it.hasNext()) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)it.next();
      temp.add(rdc);
    }
    synchronized(conditionEnabled) {
      conditionEnabled.clear();
      conditionEnabled.addAll(temp);
    }

    //check for change in our modes
    it = opModeSubscription.getChangedCollection().iterator();
    while(it.hasNext()) {
      RestartDefenseEnabler rde = (RestartDefenseEnabler)it.next();
      if (logger.isDebugEnabled()) {
        logger.debug("get opmode change: " + rde.getAsset() + " -- " +
                     rde.getValue());
      }
      if(rde.getValue().equals(DefenseConstants.DEF_ENABLED.toString())) {
        if(rde.getAssetType().equals(assetType)) {
          String agent = rde.getAsset();
          opmodeEnabled(agent);
          if (logger.isDebugEnabled()) {
            logger.debug("get " + agent + ": " + rde.getName() + "=" +
                         rde.getValue());
          }
        }
      }
    }
  }

  public boolean isCoordinatorEnabled() {
    return true;
  }

  public void addListener(CoordinatorListener dl) {
    if(!listeners.contains(dl))
      listeners.add(dl);
  }

  public void removeListener(CoordinatorListener dl) {
    if(listeners.contains(dl))
      listeners.remove(dl);
  }

  /**
   * This method is invoked when we got one defense opmode enabled.
   * @param name the agent to be enabled
   */
  public void opmodeEnabled(String name) {
    for(Iterator it = listeners.iterator(); it.hasNext();) {
      CoordinatorListener cl = (CoordinatorListener)it.next();
      opModeEnabled.add(name);
      cl.actionEnabled(name);
    }
  }

  /**
   * Is the defense opmode of given agent is enabled?
   * @param name Agent name
   * @return A boolean value
   */
  public boolean isOpEnabled(String name) {
    return opModeEnabled.contains(name);
  }

  /**
   * Set the defense opmode of given agent to disabled. Normally this method is
   * called when the agent is prove to be active.
   * @param name the agent name
   */
  public void opmodeDisabled(String name) {
    if(opModeEnabled.contains(name)) {
      opModeEnabled.remove(name);
      if (logger.isDebugEnabled()) {
        logger.debug("remove " + name + " from opModeEnabled queue");
      }
    }
  }

  /**
   * Publish Coordinators objects for  agent.
   */
  public void addAgent(String agentName) {
    if (!agents.contains(agentName)) {
      fireLater(new RestartDefenseCondition(assetType, agentName, defenseName,
                                            DefenseConstants.BOOL_FALSE));
      fireLater(new RestartDefenseEnabler(assetType, agentName, defenseName));
      fireLater(new RestartMonitoringEnabler(assetType, agentName, defenseName));
      agents.add(agentName);
    }
  }

  /**
   * Publish Coordinators objects for  agent.
   */
  public void removeAgent(String agentName) {
    // Nothing for now.
  }

  /**
   * Modify the applicable condition value of given agent if current condition value
   * is not equals desired value.
   * @param name the agent name
   */
  public void changeApplicabilityCondition(String name) {

    List l;
    synchronized(conditionEnabled) {
      l = new ArrayList(conditionEnabled);
    }
    Iterator it = l.iterator();
    while (it.hasNext()) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)it.next();
      if (rdc != null && rdc.getAssetType().equals(assetType) && rdc.getAsset().equals(name)) {
        setDiagnosis(name, rdc.getValue().toString().equalsIgnoreCase("true")
                     ? LIVE
                     : DEAD);
      }
    }
  }

  public void setDiagnosis(String agentName, String state) {
    List l;
    synchronized(conditionEnabled) {
      l = new ArrayList(conditionEnabled);
    }
    Iterator it = l.iterator();
    while (it.hasNext()) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)it.next();
      if (rdc != null && rdc.getAssetType().equals(assetType) && rdc.getAsset().equals(agentName)) {
        rdc.setValue(state.equalsIgnoreCase(LIVE)
                     ? DefenseConstants.BOOL_FALSE
                     : DefenseConstants.BOOL_TRUE);
        if (logger.isDebugEnabled())
          logger.debug("** setRestartCondition - " + agentName + "=" + rdc.getValue());
        defenseConditionQueue.add(rdc);
      }
    }
  }

  public boolean isDefenseApplicable(String agentName) {
    List l;
    synchronized(conditionEnabled) {
      l = new ArrayList(conditionEnabled);
    }

    if(l != null) {
      Iterator it = l.iterator();
      while (it.hasNext()) {
        RestartDefenseCondition rdc = (RestartDefenseCondition) it.next();
        if (rdc != null && rdc.getAssetType().equals(assetType) &&
            rdc.getAsset().equals(agentName)) {
          if (rdc.getValue().toString().equalsIgnoreCase("true"))
            return true;
        }

      }
    }

    return false;
  }

  private void publishChangeCondition() {
    for(int i=0; i<defenseConditionQueue.size(); i++) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)defenseConditionQueue.get(i);
      blackboard.publishChange(rdc);
      if (logger.isDebugEnabled()) {
        logger.debug("** publish RestartCondition - " + rdc.getName() + "=" +
                     rdc.getValue());
      }
    }
    defenseConditionQueue.clear();
  }

  private void fireLater(Object obj) {
    synchronized (defenseOperatingModeQueue) {
      defenseOperatingModeQueue.add(obj);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (defenseOperatingModeQueue) {
      n = defenseOperatingModeQueue.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(defenseOperatingModeQueue);
      defenseOperatingModeQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      Object obj = l.get(i);
      if(obj instanceof RestartDefenseEnabler) {
        RestartDefenseEnabler rde = (RestartDefenseEnabler) obj;
        rde.setUID(uidService.nextUID());
        blackboard.publishAdd(rde);
        if(logger.isDebugEnabled())
          logger.debug("publish add " + rde.getName() + "=" + rde.getValue());
      }
      else if(obj instanceof RestartMonitoringEnabler) {
        RestartMonitoringEnabler rme = (RestartMonitoringEnabler) obj;
        rme.setUID(uidService.nextUID());
        blackboard.publishAdd(rme);
        if(logger.isDebugEnabled())
          logger.debug("publish add " + rme.getName() + "=" + rme.getValue());
      }
      else if(obj instanceof RestartDefenseCondition) {
        RestartDefenseCondition rdc = (RestartDefenseCondition) obj;
        rdc.setUID(uidService.nextUID());
        blackboard.publishAdd(rdc);
        if(logger.isDebugEnabled())
          logger.debug("publish add " + rdc.getName() + "=" + rdc.getValue());
      }
    }
  }

  // Timer for periodically stimulating execute() method to check/process
  // deconflict object queue
  private class WakeAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public WakeAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() {
      return expiresAt;
    }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        if (blackboard != null) blackboard.signalClientActivity();
        wakeAlarm = new WakeAlarm((new Date()).getTime() + TIMER_INTERVAL);
        alarmService.addRealTimeAlarm(wakeAlarm);
      }
    }
    public boolean hasExpired() {
      return expired;
    }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }

  public class RestartDefenseEnabler extends DefenseEnablingOperatingMode {
    public RestartDefenseEnabler(String assetType, String asset, String defenseName) {
      super (assetType, asset, defenseName);
    }
  }

  public class RestartMonitoringEnabler extends MonitoringEnablingOperatingMode {
    public RestartMonitoringEnabler (String assetType, String asset, String defenseName) {
      super (assetType, asset, defenseName);
    }
  }

  public class RestartDefenseCondition extends DefenseApplicabilityBinaryCondition {
    public RestartDefenseCondition(String assetType, String asset, String defenseName, DefenseConstants.OMCStrBoolPoint pt) {
      super(assetType, asset, defenseName, pt);
    }
    protected void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
      super.setValue(newValue);
    }
  }

}
