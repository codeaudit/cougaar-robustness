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

package org.cougaar.tools.robustness.threatalert;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** A ThreatAlert is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to threat alert management capabilities.
 **/

public class ThreatAlertServiceImpl extends BlackboardClientComponent implements ThreatAlertService{

  private LoggingService log;
  private CommunityService commSvc;
  private UIDService uidSvc;
  private final List sendQueue = new ArrayList(5);
  private final List updateQueue = new ArrayList(5);
  private final List cancelQueue = new ArrayList(5);

  public void setCommunityService(CommunityService cs) {
    this.commSvc = cs;
  }

  //Un-expired threat alerts
  private Map currentThreatAlerts = Collections.synchronizedMap(new HashMap());

  private Map myRelays = Collections.synchronizedMap(new HashMap());

  //List of alarms for alerts that are waiting to expire
  private List expirationAlarms = Collections.synchronizedList(new ArrayList());

  //List of alarms for alerts that are waiting to to be activated
  private List activationAlarms = Collections.synchronizedList(new ArrayList());

  private IncrementalSubscription taSub; //subscirptions to threat alert

  private List listeners = Collections.synchronizedList(new ArrayList());

  /**
   * ThreatAlertService Implementation.
   * @param bs
   */
  public ThreatAlertServiceImpl(BindingSite bs) {
    setBindingSite(bs);
    ServiceBroker sb = getServiceBroker();
    setAgentIdentificationService(
        (AgentIdentificationService)sb.getService(this, AgentIdentificationService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    uidSvc = (UIDService)getServiceBroker().getService(this, UIDService.class, null);
    log = (LoggingService)sb.getService(this, LoggingService.class, null);
    log = org.cougaar.core.logging.LoggingServiceWithPrefix.add(log, agentId + ": ");
    if (log.isDebugEnabled()) {
      log.debug("init:");
    }
    if (sb.hasService(org.cougaar.core.service.BlackboardService.class)) {
      init();
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(BlackboardService.class)) {
            init();
          }
        }
      });
    }
  }

  /**
   * Set blackboard service and complete initialization.
   */
  public void init() {
    setBlackboardService((BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    initialize();
    load();
    start();
  }

  /**
   * Remove a threat alert listener.
   * @param tal
   */
  public void removeListener(ThreatAlertListener tal) {
    if (listeners.contains(tal))
      listeners.remove(tal);
  }

  private CommunityService getCommunityService() {
    if (commSvc == null) {
      commSvc =
          (CommunityService) getServiceBroker().getService(this,
          CommunityService.class, null);
    }
    return commSvc;
  }

  private boolean sendToLocalAgent(String community, String role) {
    boolean result = false;
    CommunityService cs = getCommunityService();
    if (cs != null) {
      Collection members =
          commSvc.searchCommunity(community, "(Role=" + role + ")", false,
                                  Community.AGENTS_ONLY, null);
      if (members != null && !members.isEmpty()) {
        for (Iterator it = members.iterator(); it.hasNext(); ) {
          Entity entity = (Entity) it.next();
          if (entity.getName().equals(agentId.toString())) {
            result = true;
            break;
          }
        }
      }
    }
    return result;
  }

  /**
   * Publish the threat alert to all agents in given community with the given role.
   * @param ta
   * @param community
   * @param role
   */
  public void sendAlert(ThreatAlert ta, String community, String role) {
    ta.setSource(agentId);
    ta.setUID(uidSvc.nextUID());
    // Send to remote listeners via ABA/Relay
    RelayAdapter taRelay = new RelayAdapter(agentId, ta, ta.getUID());
    AttributeBasedAddress target =
        AttributeBasedAddress.getAttributeBasedAddress(community, "Role", role);
    taRelay.addTarget(target);
    // Add local agent to target set if community/role matches
    if (sendToLocalAgent(community, role)) {
      taRelay.addTarget(agentId);
    }
    if (log.isDebugEnabled()) {
      log.debug("sendAlert:" +
                " alert=" + ta +
                " community=" + community +
                " role=" + role +
                " targets=" + taRelay.targetsToString(taRelay));
    }
    queueForSend(taRelay);
  }

  /**
   * Send a new ThreatAlert message to a single agent.
   * @param ta  ThreatAlert to send
   * @param agent  Agent to receive alert
   */
  public void sendAlert(ThreatAlert ta, String agent) {
    ta.setSource(agentId);
    ta.setUID(uidSvc.nextUID());
    // Send to remote listeners via Relay
    RelayAdapter taRelay = new RelayAdapter(agentId, ta, ta.getUID());
    taRelay.addTarget(MessageAddress.getMessageAddress(agent));
    if (log.isDebugEnabled()) {
      log.debug("sendAlert:" +
                " alert=" + ta +
                " target=" + taRelay.targetsToString(taRelay));
    }
    queueForSend(taRelay);
  }

  /**
   * Used by ThreatAlert originator to update alert contents.
   * @param ta  ThreatAlert to update
   */
  public void updateAlert(ThreatAlert ta) {
    if (log.isDebugEnabled()) {
      log.debug("updateAlert:" + ta);
    }
    RelayAdapter taRelay = (RelayAdapter)myRelays.get(ta.getUID());
    if (taRelay != null) {
      queueForUpdate(taRelay);
    }
  }

  /**
   * Used by ThreatAlert originator to cancel a current alert.
   * @param ta  ThreatAlert to cancel
   */
  public void cancelAlert(ThreatAlert ta) {
    if (log.isDebugEnabled()) {
      log.debug("cancelAlert:" + ta);
    }
    queueForCancel(ta);
  }

  /**
   * Check for availability of essential services
   * @return True if needed services available
   */
  private boolean servicesReady() {
    return blackboard != null;
  }

  /**
   * Queue new ThreatAlerts.
   */
  protected void queueForSend(Object o) {
    synchronized (sendQueue) {
      sendQueue.add(o);
    }
    if (servicesReady()) {
      blackboard.signalClientActivity();
    }
  }

  /**
   * Queue updated ThreatAlerts.
   */
  protected void queueForUpdate(Object o) {
    synchronized (updateQueue) {
      updateQueue.add(o);
    }
    if (servicesReady()) {
      blackboard.signalClientActivity();
    }
  }

  /**
   * Queue canceled ThreatAlerts.
   */
  protected void queueForCancel(Object o) {
    synchronized (cancelQueue) {
      cancelQueue.add(o);
    }
    if (servicesReady()) {
      blackboard.signalClientActivity();
    }
  }

  /**
   * Process queued ThreatAlerts.
   */
  private void sendQueuedAlerts() {
    int n;
    List l;
    // Publish new ThreatAlerts
    synchronized (sendQueue) {
      n = sendQueue.size();
      l = new ArrayList(sendQueue);
      sendQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      RelayAdapter ra = (RelayAdapter)l.get(i);
      blackboard.publishAdd(l.get(i));
      myRelays.put(ra.getUID(), ra);
      if (log.isDebugEnabled()) {
        log.debug("publishAdd ThreatAlert Relay: " + ra);
      }
      if (ra.getTargets().contains(agentId)) {
        addThreatAlert((ThreatAlert)ra.getContent());
        if (log.isDebugEnabled()) {
          log.debug("add ThreatAlert locally: " + ra);
        }
      }
    }
    // Publish updated ThreatAlerts
    synchronized (updateQueue) {
      n = updateQueue.size();
      l = new ArrayList(updateQueue);
      updateQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      RelayAdapter ra = (RelayAdapter)l.get(i);
      blackboard.publishChange(l.get(i));
      if (log.isDebugEnabled()) {
        log.debug("publishChange ThreatAlert: " + ra);
      }
      if (ra.getTargets().contains(agentId)) {
        fireListenersForChangedAlert((ThreatAlert)ra.getContent());
      }
    }
    // Process canceled ThreatAlerts
    synchronized (cancelQueue) {
      n = cancelQueue.size();
      l = new ArrayList(cancelQueue);
      cancelQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      cancelThreatAlert((ThreatAlert)l.get(i));
    }
  }

  /**
   * Returns an array of all un-expired ThreatAlerts.
   * @return  Array of ThreatAlerts
   */
  public ThreatAlert[] getCurrentAlerts() {
    return (ThreatAlert[])currentThreatAlerts.values().toArray(new ThreatAlert[0]);
  }

  public void setupSubscriptions() {
    taSub = (IncrementalSubscription)blackboard.subscribe(threatAlertPredicate);
  }

  public void execute() {

    // Publish ThreatAlerts queued for send to remote agents (via Relay)
    sendQueuedAlerts();

    // Get ThreatAlerts sent from remote agents
    for(Iterator it = taSub.getAddedCollection().iterator(); it.hasNext();) {
      addThreatAlert((ThreatAlert)it.next());
    }

    // Get changed ThreatAlerts and notify listeners
    for(Iterator it = taSub.getChangedCollection().iterator(); it.hasNext();) {
      fireListenersForChangedAlert((ThreatAlert)it.next());
    }

    // Get removed ThreatAlerts and notify listeners
    for(Iterator it = taSub.getRemovedCollection().iterator(); it.hasNext();) {
      cancelThreatAlert((ThreatAlert)it.next());
    }

    // Get newly activated alerts.  Notify listeners by firing changedAlert callback
    if(!activationAlarms.isEmpty()) {
      List tempAlarms = new ArrayList();
      synchronized(activationAlarms) {
        tempAlarms.addAll(activationAlarms);
      }
      for (Iterator it = tempAlarms.iterator(); it.hasNext(); ) {
        ThreatAlertAlarm alarm = (ThreatAlertAlarm) it.next();
        if (alarm.hasExpired()) {
          activationAlarms.remove(alarm);
          fireListenersForChangedAlert(alarm.getThreatAlert());
        }
      }
    }

    // Get Expired alerts.  Notify listeners and remove artifacts
    if(!expirationAlarms.isEmpty()) {
      List tempAlarms = new ArrayList();
      synchronized(expirationAlarms) {
        tempAlarms.addAll(expirationAlarms);
      }
      for (Iterator it = tempAlarms.iterator(); it.hasNext(); ) {
        ThreatAlertAlarm alarm = (ThreatAlertAlarm) it.next();
        if (alarm.hasExpired()) {
          expirationAlarms.remove(alarm);
          cancelThreatAlert(alarm.getThreatAlert());
        }
      }
    }
  }

  // Add new alert
  private void addThreatAlert(ThreatAlert ta) {
    long now = System.currentTimeMillis();
    currentThreatAlerts.put(ta.getUID(), ta);
    // Add activation alarm
    if (now < ta.getStartTime().getTime()) {
      ThreatAlertAlarm alarm = new ThreatAlertAlarm(ta, ta.getStartTime().getTime());
      alarmService.addRealTimeAlarm(alarm);
      activationAlarms.add(alarm);
    }
    // Add expiration alarm
    if (now < ta.getExpirationTime().getTime()) {
      ThreatAlertAlarm alarm = new ThreatAlertAlarm(ta, ta.getExpirationTime().getTime());
      alarmService.addRealTimeAlarm(alarm);
      expirationAlarms.add(alarm);
    }
    fireListenersForNewAlert(ta);
  }

  private void cancelThreatAlert(ThreatAlert alert) {
    if (log.isDebugEnabled()) {
      log.debug("cancelThreatAlert: listeners=" + listeners.size());
    }
    if (currentThreatAlerts.containsKey(alert.getUID())) {
      // Remove from current alert list
      currentThreatAlerts.remove(alert.getUID());
      fireListenersForRemovedAlert(alert);
    }
    // Remove Relay.Source if Alert originated from this agent
    if (myRelays.containsKey(alert.getUID())) {
      RelayAdapter ra = (RelayAdapter)myRelays.remove(alert.getUID());
      blackboard.publishRemove(ra);
      if (log.isDebugEnabled()) {
        log.debug("publishRemove ThreatAlert: " + ra);
      }
    }
  }

  /**
   * Notify listeners of new alert.
   * @param ta  New ThreatAlert
   */
  private void fireListenersForNewAlert(ThreatAlert ta) {
    List l = new ArrayList();
    synchronized (listeners) {
      l.addAll(listeners);
    }
    for(Iterator iter = l.iterator(); iter.hasNext();) {
      ThreatAlertListener listener = (ThreatAlertListener)iter.next();
      listener.newAlert(ta);
    }
  }

  /**
   * Add a threat alert listener.
   * @param tal
   */
  public void addListener(ThreatAlertListener tal) {
    if(!listeners.contains(tal))
      listeners.add(tal);
  }

  /**
   * Notify listeners of changed alert.
   * @param ta  Changed ThreatAlert
   */
  private void fireListenersForChangedAlert(ThreatAlert ta) {
    List l = new ArrayList();
    synchronized (listeners) {
      l.addAll(listeners);
    }
    for(Iterator iter = l.iterator(); iter.hasNext();) {
      ThreatAlertListener listener = (ThreatAlertListener)iter.next();
      listener.changedAlert(ta);
    }
  }

  /**
   * Notify listeners of expired alert.
   * @param ta  Expired ThreatAlert
   */
  private void fireListenersForRemovedAlert(ThreatAlert ta) {
    List l = new ArrayList();
    synchronized (listeners) {
      l.addAll(listeners);
    }
    for(Iterator iter = l.iterator(); iter.hasNext();) {
      ThreatAlertListener listener = (ThreatAlertListener)iter.next();
      listener.removedAlert(ta);
    }
  }

  private UnaryPredicate threatAlertPredicate = new UnaryPredicate() {
  public boolean execute (Object o) {
    return (o instanceof ThreatAlert);
  }};


  /**
   * Alert expiration alarm.
   */
  private class ThreatAlertAlarm implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    private ThreatAlert ta;

    public ThreatAlertAlarm(ThreatAlert ta, long alarmExpiration) {
      expirationTime = alarmExpiration;
      this.ta = ta;
    }

    /** @return absolute time (in milliseconds) that the Alarm should go off.
     **/
    public long getExpirationTime () {
      return expirationTime;
    }

    /**
     * Called by the cluster clock when clock-time >= getExpirationTime().
     **/
    public void expire () {
      expired = true;
      blackboard.signalClientActivity();
    }

    /** @return true IFF the alarm has expired or was canceled. **/
    public boolean hasExpired () {
      return expired;
    }

    public synchronized boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }

    public ThreatAlert getThreatAlert() {
      return ta;
    }
  }

}
