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
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.adaptivity.Condition;

import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.tools.robustness.deconfliction.DefenseEnablingOperatingMode;
import org.cougaar.tools.robustness.deconfliction.DefenseApplicabilityCondition;
import org.cougaar.tools.robustness.deconfliction.MonitoringEnablingOperatingMode;
import org.cougaar.tools.robustness.deconfliction.DefenseApplicabilityBinaryCondition;
import org.cougaar.tools.robustness.deconfliction.DefenseConstants;
import org.cougaar.tools.robustness.deconfliction.DefenseOperatingMode;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;

import java.util.*;

/**
 * Provides convenience methods for invoking deconflictor. This helper only applies
 * to the robustness manager.
 */
public class DeconflictHelper extends BlackboardClientComponent {
  /*//prefix of applicable condition name for the agents
  public static final String MYCONDITION = "RestartApplicableAgent";
  //prefix of defense op mode name
  public static final String MYDEF_OPMODE = "RestartEnablerAgent";
  //prefix of monitor mode name
  public static final String MYMONITOR_OPMODE = "RestartMonitorAgent";*/
  public static final String assetType = "Agent";
  public static final String defenseName = "Restart";

  public static final long TIMER_INTERVAL = 10 * 1000;

  private LoggingService logger;
  private UIDService uidService;
  private ConditionService conditionService;
  private OperatingModeService operatingModeService;
  private EventService eventService;

  private IncrementalSubscription opModeSubscription;
  private IncrementalSubscription monitorModeSubscription;
  private IncrementalSubscription conditionSubscription;

  private CommunityStatusModel model;

  private ArrayList listeners = new ArrayList(); //store all deconflict listeners
  private ArrayList agentsObjs = new ArrayList(); //store all agents who need to publish deconflict objects
  private ArrayList opModeEnabled = new ArrayList(); //store all agents whose defense opmode is enabled

  private List defenseOperatingModeQueue = new ArrayList(); //store all objects that need to be published into the blackboard.
  private List defenseConditionQueue = new ArrayList(); //store all conditions that need to be changed in the blackboard.

  private WakeAlarm wakeAlarm;

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs
 */
  public DeconflictHelper(BindingSite bs, CommunityStatusModel csm) {
    this.setBindingSite(bs);
    this.model = csm;
    initialize();
    load();
    start();
  }

  //public void setConditionService (ConditionService cs) { this.conditionService = cs; }

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
    if(conditionService == null) {
      logger.warn("No ConditionService?");
    }
    //conditionService = getConditionService();
    //operatingModeService = (OperatingModeService) getServiceBroker().getService(this, OperatingModeService.class, null);
    super.load();
  }

  public void start() {
    super.start();
  }

 /* private ConditionService getConditionService() {
    int counter = 0;
    ConditionService cs = null;
    while (!getServiceBroker().hasService(ConditionService.class)) {
      // Print a message after waiting for 30 seconds
      if (++counter == 60) logger.info("Waiting for ConditionService ... ");
      try { Thread.sleep(500); } catch (Exception ex) {}
    }
    return (ConditionService)getServiceBroker().getService(this, ConditionService.class,
      new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {}
    });
  }*/


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

    //Listen for changes in our enabling mode object
     /*monitorModeSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof RestartMonitoringEnabler ) {
                return true ;
            }
            return false ;
        }
     }) ;*/

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

    /*Iterator iter = monitorModeSubscription.getAddedCollection().iterator();
    while(iter.hasNext()) {
      RestartMonitoringEnabler rme = (RestartMonitoringEnabler)iter.next();
      logger.info("get monitor mode: " + rme.getName() + "=" + rme.getValue());
    }
    iter = conditionSubscription.getAddedCollection().iterator();
    while(iter.hasNext()) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)iter.next();
      logger.info("get condition: " + rdc.getName() + "=" + rdc.getValue());
    }
    iter = opModeSubscription.getAddedCollection().iterator();
    while(iter.hasNext()) {
      RestartDefenseEnabler rme = (RestartDefenseEnabler)iter.next();
      logger.info("get op mode: " + rme.getName() + "=" + rme.getValue());
    }*/


    //check for change in our modes
    Iterator it = opModeSubscription.getChangedCollection().iterator();
    while(it.hasNext()) {
      RestartDefenseEnabler rde = (RestartDefenseEnabler)it.next();
      logger.info("get opmode change: " + rde.getAsset() + " -- " + rde.getValue());
      if(rde.getValue().equals(DefenseConstants.DEF_ENABLED.toString())) {
        if(rde.getAssetType().equals(assetType)) {
          String agent = rde.getAsset();
          opmodeEnabled(agent);
          logger.debug("get " + agent + ": " + rde.getName() + "=" + rde.getValue());
        }
      }
    }
  }

  public void addListener(DeconflictListener dl) {
    if(!listeners.contains(dl))
      listeners.add(dl);
  }

  public void removeListener(DeconflictListener dl) {
    if(listeners.contains(dl))
      listeners.remove(dl);
  }

  /**
   * This method is invoked when we got one defense opmode enabled.
   * @param name the agent to be enabled
   */
  public void opmodeEnabled(String name) {
    for(Iterator it = listeners.iterator(); it.hasNext();) {
      DeconflictListener dl = (DeconflictListener)it.next();
      opModeEnabled.add(name);
      dl.defenseOpModeEnabled(name);
    }
  }

  /**
   * Is the defense opmode of given agent is enabled?
   * @param name Agent name
   * @return
   */
  public boolean isOpEnabaled(String name) {
    return opModeEnabled.contains(name);
  }

  /**
   * Set the defense opmode of given agent to disabled. Normally this method is
   * called when the agent is prove to be active.
   * @param name
   */
  public void opmodeDisabled(String name) {
    if(opModeEnabled.contains(name)) {
      opModeEnabled.remove(name);
      //logger.info("remove " + name + " from opModeEnabled queue");
    }
  }

  /**
   * Publish deconflict objects for every agent. For this defense, we have three
   * deconflict objects for every agent: the RestartDefenseCondition(default to
   * FALSE), the RestartDefenseEnabler(default to DISABLED) and the RestartMonitoringEnabler
   * (default to DISABLED).
   */
  public void initObjs() {
    String[] agents = model.listEntries(CommunityStatusModel.AGENT);
    for(int i=0; i<agents.length; i++) {
      if(!agentsObjs.contains(agents[i])) {
        fireLater(new RestartDefenseCondition(assetType, agents[i], defenseName, DefenseConstants.BOOL_FALSE));
        fireLater(new RestartDefenseEnabler(assetType, agents[i], defenseName));
        fireLater(new RestartMonitoringEnabler(assetType, agents[i], defenseName));
        /*fireLater(new RestartDefenseCondition(defenseName + ":" + agents[i], DefenseConstants.BOOL_FALSE));
        fireLater(new RestartDefenseEnabler(defenseName + ":" + agents[i]));
        fireLater(new RestartMonitoringEnabler(defenseName + ":" + agents[i]));*/
        agentsObjs.add(agents[i]);
      }
    }
  }

  /**
   * Modify the applicable condition value of given agent if current condition value
   * is not equals desired value.
   * @param name the agent name
   * @param desiredValue The desired value.
   */
  public boolean changeApplicabilityCondition(String name) {
    String condition = defenseName + ":" + name;

    Iterator it = conditionSubscription.getCollection().iterator();
    while(it.hasNext()) {
      RestartDefenseCondition rdc = (RestartDefenseCondition)it.next();
      if(rdc != null && rdc.getAssetType().equals(assetType) && rdc.getAsset().equals(name)) {
        if(rdc.getValue().toString().equalsIgnoreCase("true"))
          rdc.setValue(DefenseConstants.BOOL_FALSE);
        else
          rdc.setValue(DefenseConstants.BOOL_TRUE);
        if (logger.isDebugEnabled())
          logger.debug("** setRestartCondition - " + rdc.getName() + "=" + rdc.getValue());
        defenseConditionQueue.add(rdc);
        return true;
      }
    }
    return false;

    /*RestartDefenseCondition rdc = (RestartDefenseCondition)conditionService.getConditionByName(condition);
    if (rdc != null) {
      if(rdc.getValue().toString().equalsIgnoreCase("true"))
        rdc.setValue(DefenseConstants.BOOL_FALSE);
      else
        rdc.setValue(DefenseConstants.BOOL_TRUE);
      if (logger.isDebugEnabled())
        logger.debug("** setRestartCondition - " + rdc.getName() + "=" + rdc.getValue());
      defenseConditionQueue.add(rdc);
    } else {
      if (logger.isDebugEnabled()) logger.debug("** Cannot find condition object!");
    }*/
  }

  public boolean isDefenseApplicable(String agentName) {
    /*String condition = defenseName + ":" + agentName;

    RestartDefenseCondition rdc = (RestartDefenseCondition)conditionService.getConditionByName(condition);
    if(rdc != null) {
      if(rdc.getValue().toString().equalsIgnoreCase("true"))
        return true;
    }*/
    if(conditionSubscription != null && conditionSubscription.getCollection() != null) {
      Iterator it = conditionSubscription.getCollection().iterator();
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
      logger.info("** publish RestartCondition - " + rdc.getName() + "=" + rdc.getValue());
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

  public void test() {
    Iterator iter = opModeSubscription.getAddedCollection().iterator();
    while(iter.hasNext()) {
      RestartDefenseEnabler rme = (RestartDefenseEnabler)iter.next();
      logger.debug("get op mode: " + rme.getName() + "=" + rme.getValue());
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
    /*public RestartDefenseEnabler(String str) {
      super(str);
    }*/
  }

  public class RestartMonitoringEnabler extends MonitoringEnablingOperatingMode {
    public RestartMonitoringEnabler (String assetType, String asset, String defenseName) {
      super (assetType, asset, defenseName);
    }
    /*public RestartMonitoringEnabler(String str) {
      super(str);
    }*/
  }

  public class RestartDefenseCondition extends DefenseApplicabilityBinaryCondition {
    /*public RestartDefenseCondition(String name) {
      super(name);
    }*/
    public RestartDefenseCondition(String assetType, String asset, String defenseName, DefenseConstants.OMCStrBoolPoint pt) {
      super(assetType, asset, defenseName, pt);
    }
    /*public RestartDefenseCondition(String str, DefenseConstants.OMCStrBoolPoint pt) {
      super(str, pt);
    }*/
    protected void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
      super.setValue(newValue);
    }
  }

}
