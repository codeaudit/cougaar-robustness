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
import org.cougaar.core.service.DomainService;
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

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.community.RelayAdapter;

import java.util.*;

/** A ThreatAlert is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to threat alert management capabilities.
 **/

public class ThreatAlertServiceImpl extends BlackboardClientComponent implements ThreatAlertService{

  private LoggingService log; //logging service
  private UIDService uidService; //uid service
  private CommunityService commSvc;
  private static Map instances = Collections.synchronizedMap(new HashMap()); //

  public void setCommunityService(CommunityService cs) {
    this.commSvc = cs;
  }

  //save active current threat alerts
  private static Map threatAlerts = Collections.synchronizedMap(new HashMap());

  //save all alerts and their alarms. The alarm is used to remove the alert when it is expired.
  private static List alertAlarms = Collections.synchronizedList(new ArrayList());

  private IncrementalSubscription taSub; //subscirptions to threat alert

  private List listeners = new ArrayList();

  protected ThreatAlertServiceImpl(BindingSite bs, MessageAddress addr) {
    setBindingSite(bs);
    ServiceBroker sb = getServiceBroker();
    setAgentIdentificationService(
        (AgentIdentificationService)sb.getService(this, AgentIdentificationService.class, null));
    log = (LoggingService)sb.getService(this, LoggingService.class, null);
    log = org.cougaar.core.logging.LoggingServiceWithPrefix.add(log, agentId + ": ");
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
   * Fetch all necessary services.
   */
  public void init() {
    setBlackboardService((BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    DomainService ds =
      (DomainService) getServiceBroker().getService(this, DomainService.class, null);
    uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);

    initialize();
    load();
    start();
  }

  /**
   * Returns ThreatAlertService instance.
   */
  public static ThreatAlertService getInstance(BindingSite    bs,
                                             MessageAddress addr) {
    ThreatAlertService tas = (ThreatAlertService)instances.get(addr);
    if (tas == null) {
      tas = new ThreatAlertServiceImpl(bs, addr);
      instances.put(addr, tas);
    }
    return tas;
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
   * Remove a threat alert listener.
   * @param tal
   */
  public void removeListener(ThreatAlertListener tal) {
    if(listeners.contains(tal))
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

  /**
   * Publish the threat alert to all agents in given community with the given role.
   * @param ta
   * @param community
   * @param role
   */
  public void sendAlert(ThreatAlert ta, String community, String role) {
    log.debug("sendAlert:" +
              " alert=" + ta +
              " community=" + community +
              " role=" + role);
    CommunityService cs = getCommunityService();
    if (cs != null) {
      Collection members =
          commSvc.searchCommunity(community, "(Role=" + role + ")", false,
                                  Community.AGENTS_ONLY, null);
      log.debug("members=" + members);
      if (members != null && !members.isEmpty()) {
        for (Iterator it = members.iterator(); it.hasNext(); ) {
          Entity entity = (Entity) it.next();
          if (entity.getName().equals(agentId.toString())) {
            addThreatAlert(ta); // add locally and notify local listeners
            log.debug("add ThreatAlert locally");
          }
        }
      }
    }
    /*if(target.equals(agentId)) {
      if(log.isInfoEnabled()) {
        log.info("publish ThreatAlert " + ta.toString());
      }
      try {
        blackboard.openTransaction();
        blackboard.publishAdd(ta);
      }finally {
        blackboard.closeTransactionDontReset();
      }
    } else {*/

      // Send to remote listeners
      RelayAdapter taiRelay = new RelayAdapter(agentId, ta, ta.getUID());
      AttributeBasedAddress target =
          AttributeBasedAddress.getAttributeBasedAddress(community, "Role", role);
      taiRelay.addTarget(target);
      if (log.isDebugEnabled()) {
        log.debug("publish ThreatAlert, remote agent is " + target.toString() +
                 " " + ta.toString());
      }
      try {
        blackboard.openTransaction();
        blackboard.publishAdd(taiRelay);
      } catch (Exception ex) {
        log.error(ex.getMessage(), ex);
      } finally {
        blackboard.closeTransactionDontReset();
      }
    //}
  }

  public ThreatAlert[] getCurrentThreats() {
    ThreatAlert[] alerts = new ThreatAlert[threatAlerts.size()];
    int i=0;
    Map temp = new HashMap();
    synchronized(threatAlerts) {
      temp.putAll(threatAlerts);
    }
    for(Iterator it = temp.values().iterator(); it.hasNext();) {
      alerts[i] = (ThreatAlert)it.next();
      i++;
    }
    return alerts;
  }

  public void setupSubscriptions() {
    taSub = (IncrementalSubscription)blackboard.subscribe(threatAlertPredicate);
  }

  public void execute() {
    for(Iterator it = taSub.getAddedCollection().iterator(); it.hasNext();) {
      addThreatAlert((ThreatAlert)it.next());
    }

    if(!alertAlarms.isEmpty()) {
      List tempalarms = new ArrayList();
      synchronized(alertAlarms) {
        tempalarms.addAll(alertAlarms);
      }
      for (Iterator it = tempalarms.iterator(); it.hasNext(); ) {
        ThreatAlertAlarm alarm = (ThreatAlertAlarm) it.next();
        if (alarm.hasExpired()) {
          ThreatAlert alert = alarm.getThreatAlert();
          log.info("publish remove alert: " + alert.toString());
          blackboard.publishRemove(alert);
          alertAlarms.remove(alarm);
          for (Iterator iter = threatAlerts.keySet().iterator(); iter.hasNext(); ) {
            UID uid = (UID) iter.next();
            if (uid.toString().equals(alert.getUID().toString())) {
              threatAlerts.remove(uid);
            }
          }
        }
      }
    }
  }

  private void addThreatAlert(ThreatAlert ta) {
    threatAlerts.put(ta.getUID(), ta);
    ThreatAlertAlarm alarm = new ThreatAlertAlarm(ta);
    alarmService.addRealTimeAlarm(alarm);
    alertAlarms.add(alarm);
    fireListeners(ta);
  }

  private void fireListeners(ThreatAlert ta) {
    for(Iterator iter = listeners.iterator(); iter.hasNext();) {
      ThreatAlertListener listener = (ThreatAlertListener)iter.next();
      listener.newAlert(ta);
    }
  }

  private UnaryPredicate threatAlertPredicate = new UnaryPredicate() {
  public boolean execute (Object o) {
    return (o instanceof ThreatAlert);
  }};



  private class ThreatAlertAlarm implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    private ThreatAlert ta;

    public ThreatAlertAlarm(ThreatAlert ta) {
      expirationTime = ta.getExpirationTime().getTime();
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