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
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Provides convenience methods used to interface Sledgehammer defense to
 * defense Coordinator infrastructure.
 */
public class CoordinatorHelperImpl
    extends BlackboardClientComponent
    implements CoordinatorHelper {

  public static final String INITIAL_DIAGNOSIS = LIVE;
  public static final String RESTART_ACTION = "Yes";

  // Blackboard publication modes
  protected static final int ADD = 0;
  protected static final int CHANGE = 1;
  protected static final int REMOVE = 2;

  private LoggingService logger;

  private IncrementalSubscription actionSubscription;
  private IncrementalSubscription diagnosisSubscription;

  private List listeners = new ArrayList();
  private Map agents = Collections.synchronizedMap(new HashMap());
  private List actionsInProcess = Collections.synchronizedList(new ArrayList());

  private List toPublish = new ArrayList(); //queue for objects to be published to blackboard.

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs The agents binding site
 */
  public CoordinatorHelperImpl(BindingSite bs) {
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
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    super.load();
  }

  public void setupSubscriptions() {

    diagnosisSubscription = (IncrementalSubscription)
        getBlackboardService().subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof AgentLivenessDiagnosis;
      }
    });

    actionSubscription = (IncrementalSubscription)
        getBlackboardService().subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof AgentRestartAction;
      }
    });

  }

  public void execute() {
    if (!toPublish.isEmpty()) { fireAll(); }

    for (Iterator it = diagnosisSubscription.getChangedCollection().iterator(); it.hasNext();) {
      AgentLivenessDiagnosis diagnosis = (AgentLivenessDiagnosis)it.next();
      if (logger.isDetailEnabled()) {
        logger.detail("Changed diagnosis: " + diagnosis);
      }
    }

    for (Iterator it = actionSubscription.getChangedCollection().iterator(); it.hasNext();) {
      AgentRestartAction action = (AgentRestartAction)it.next();
      Set newPV = action.getNewPermittedValues();
      action.clearNewPermittedValues();
      if (logger.isDetailEnabled()) {
        logger.detail("Changed action: " + action);
      }
      if (newPV != null && newPV.contains(RESTART_ACTION)) {
        opmodeEnabled(action.getAssetName());
      }
    }
  }

  public void addListener(CoordinatorListener cl) {
    if (!listeners.contains(cl)) {
      listeners.add(cl);
    }
  }

  public void removeListener(CoordinatorListener cl) {
    if (listeners.contains(cl)) {
      listeners.remove(cl);
    }
  }

  /**
   * Returns true if defense is currently being applied to specified agent
   * (i.e., agent is currently being restarted).
   * @param name Agent name
   * @return A boolean value
   */
  public boolean isOpEnabled(String agentName) {
    if (logger.isDetailEnabled()) {
      logger.detail("isOpEnabled:" +
                   " agent=" + agentName +
                   " enabled=" + actionsInProcess.contains(agentName));
    }
    return (actionsInProcess.contains(agentName));
  }

  /**
   * This method should be invoked when the defense is activated (i.e., restart is
   * initiated).
   * @param name the agent to be restarted
   */
  public void opmodeEnabled(String agentName) {
    if (logger.isDetailEnabled()) {
      logger.detail("opmodeEnabled: agent=" + agentName);
    }
    if (!actionsInProcess.contains(agentName)) {
      actionsInProcess.add(agentName);
      Coordination coordObj = (Coordination)agents.get(agentName);
      if (coordObj != null) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
          CoordinatorListener cl = (CoordinatorListener)it.next();
          cl.actionEnabled(agentName);
        }
        try {
          coordObj.action.start(RESTART_ACTION);
        } catch (Exception ex) {
          logger.error("Exception calling Coordinator start(): " + ex);
        }
      }
    }
  }

  /**
   * This method should be invoked when the defense action has completed (i.e.,
   * agent has been restarted).
   * @param name the agent restarted
   */
  public void opmodeDisabled(String agentName) {
    if (logger.isDetailEnabled()) {
      logger.detail("opmodeDisabled: agent=" + agentName);
    }
    if (actionsInProcess.contains(agentName)) {
      actionsInProcess.remove(agentName);
      Coordination coordObj = (Coordination)agents.get(agentName);
      if (coordObj != null) {
        try {
          coordObj.action.stop();
        } catch (Exception ex) {
          logger.error("Exception calling Coordinator stop(): " + ex);
        }
      }
      //changeApplicabilityCondition(agentName);
    }
  }

  /**
   * Publish Coordinators objects for  agent.
   */
  public void addAgent(String agentName) {
    if (logger.isDebugEnabled()) {
      logger.debug("addAgent: " + agentName);
    }
    if (!agents.containsKey(agentName)) {
      AgentLivenessDiagnosis diagnosis = null;
      AgentRestartAction action = null;
      try {
        diagnosis =
            new AgentLivenessDiagnosis(agentName,
                                       INITIAL_DIAGNOSIS,
                                       getServiceBroker());
        fireLater(ADD, diagnosis);
        if (logger.isDetailEnabled()) {
          logger.detail("Adding Diagnosis object: " + diagnosis);
        }
      } catch (Exception ex) {
        logger.error("Exception adding Diagnosis: " + ex);
      }
      try {
        action =
            new AgentRestartAction(agentName,
                                   Collections.singleton(RESTART_ACTION),
                                   getServiceBroker());
        fireLater(ADD, action);
        if (logger.isDetailEnabled()) {
          logger.detail("Adding Action object: " + action);
        }
      } catch (Exception ex) {
        logger.error("Exception adding Action: " + ex);
      }
      agents.put(agentName, new Coordination(diagnosis, action));
    }
  }

  /**
   * Remove Coordinators objects for  agent.
   */
  public void removeAgent(String agentName) {
    if (logger.isDebugEnabled()) {
      logger.debug("removeAgent: " + agentName);
    }
    if (agents.containsKey(agentName)) {
      agents.remove(agentName);
    }
  }

  /**
   * Toggles diagnosis value between LIVE and DEAD state.
   * @param name the agent name
   */
  public void changeApplicabilityCondition(String agentName) {
    Coordination coordObj = (Coordination)agents.get(agentName);
    if (coordObj != null && coordObj.diagnosis != null) {
      Object priorState = coordObj.diagnosis.getValue();
      Object newState =
          coordObj.diagnosis.getValue().equals(DEAD) ? LIVE : DEAD;
      try {
        coordObj.diagnosis.setValue(newState);
        fireLater(CHANGE, coordObj.diagnosis);
      } catch (Exception ex) {
        logger.error("Exception in changeApplicabilityCondition, " + ex);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("changeApplicabilityCondition:" +
                     " agent=" + agentName +
                     " priorState=" + priorState +
                     " newState=" + newState);
      }
    }
  }

  public void setDiagnosis(String agentName, String newState) {
    Coordination coordObj = (Coordination)agents.get(agentName);
    if (coordObj != null && coordObj.diagnosis != null) {
      Object priorState = coordObj.diagnosis.getValue();
      if (!newState.equals(priorState)) {
        try {
          coordObj.diagnosis.setValue(newState);
          fireLater(CHANGE, coordObj.diagnosis);
        } catch (Exception ex) {
          logger.error("Exception in setDiagnosis, " + ex);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("setDiagnosis:" +
                       " agent=" + agentName +
                       " priorState=" + priorState +
                       " newState=" + newState);
        }
      }
    }
  }

  /**
   * Determines whether the defense is applicable (i.e., is the agent currently
   * diagnosed as DEAD).
   * @param agentName String
   * @return boolean  True if agent diagnosis is DEAD
   */
  public boolean isDefenseApplicable(String agentName) {
    Coordination coordObj = (Coordination)agents.get(agentName);
    boolean applicable = coordObj != null &&
                      coordObj.diagnosis != null &&
                      coordObj.diagnosis.getValue() != null &&
                      coordObj.diagnosis.getValue().equals(DEAD);
    if (logger.isDetailEnabled()) {
      logger.detail("isDefenseApplicable:" +
                   " agent=" + agentName +
                   " applicable=" + applicable +
                   " diagnosis.value=" + coordObj.diagnosis.getValue());
    }
    return applicable;
  }

  private void fireLater(int action, Object obj) {
    synchronized (toPublish) {
      toPublish.add(new ObjectToPublish(action, obj));
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    } else {

    }
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (toPublish) {
      n = toPublish.size();
      if (n <= 0) { return; }
      l = new ArrayList(toPublish);
      toPublish.clear();
    }
    for (int i = 0; i < n; i++) {
      ObjectToPublish otp = (ObjectToPublish)l.get(i);
      switch (otp.action) {
        case ADD:
          if (logger.isDetailEnabled()) { logger.detail("publishAdd " + otp.obj); }
          blackboard.publishAdd(otp.obj);
          break;
        case CHANGE:
          if (logger.isDetailEnabled()) { logger.detail("publishChange " + otp.obj); }
          blackboard.publishChange(otp.obj);
          break;
        case REMOVE:
          if (logger.isDetailEnabled()) { logger.detail("publishRemove " + otp.obj); }
          blackboard.publishRemove(otp.obj);
          break;
      }
    }
  }

  // Holds Coordinator objects
  class Coordination {
    private AgentLivenessDiagnosis diagnosis;
    private AgentRestartAction action;
    Coordination (AgentLivenessDiagnosis d,
                  AgentRestartAction a) {
      diagnosis = d;
      action = a;
    }
    public String toString() {
      return "diagnosis=" + diagnosis + " action=" + action;
    }
  }

  class ObjectToPublish {
    int action = ADD;
    Object obj;
    ObjectToPublish (int a, Object o) {
      action = a;
      obj = o;
    }
  }

}
