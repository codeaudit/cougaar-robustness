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

import org.cougaar.tools.robustness.ma.ldm.PersistenceControlRequest;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.service.PersistenceControlService;

import org.cougaar.core.persist.PersistenceNotEnabledException;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Helper class for querying or modifying persistence parameters.
 */

public class PersistenceHelper extends BlackboardClientComponent {

  private ServiceBroker sb;

  // Services used by this component
  private PersistenceControlService pcs;
  private LoggingService logger;
  private UIDService uidService;

  // Queue for PersistenceControlRequest Relays that are waiting for publication
  private List todo = new ArrayList();

  // Flag indicating that a persistence snapshot is required
  private boolean persistNow = false;

  // List of PersistenceControlRequests that are in-process
  private List myUIDs = new ArrayList();

  public PersistenceHelper(BindingSite bs) {
    this.setBindingSite(bs);
    this.sb = bs.getServiceBroker();
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
    uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);
    setPersistenceControlService();
    super.load();
  }

  public void setupSubscriptions() {
    // Subscribe to PersistenceControlRequest responses
    persistenceControlRequests =
        (IncrementalSubscription)blackboard.subscribe(persistenceControlRequestPredicate);
  }

  public void execute() {
    // Send remote requests
    fireAll();
    if (persistNow) {
      try {
        logger.debug("persistNow:");
        blackboard.persistNow();
      } catch (PersistenceNotEnabledException pnee) {
        logger.warn(pnee.getMessage());
      } finally {
        persistNow = false;
      }
    }

    // Get responses to remote requests
    Collection responses = persistenceControlRequests.getChangedCollection();
    for (Iterator it = responses.iterator(); it.hasNext();) {
      RelayAdapter ra = (RelayAdapter)it.next();
      Set responders = ra.getResponders();
      logger.debug("Received PersistenceControlRequest response:" +
                  " responders=" + responders);
      // Remove request after all targets have responded
      if (responders.containsAll(ra.getTargets())) {
        logger.debug("PublishRemove PersistenceControlRequest:" + ra);
        myUIDs.remove(ra.getUID());
      }
    }
  }

  /**
   * Get reference to PersistenceControlService.
   */
  private void setPersistenceControlService() {
    if (sb.hasService(org.cougaar.core.service.PersistenceControlService.class)) {
      pcs =
          (PersistenceControlService)sb.getService(this, PersistenceControlService.class, null);
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(PersistenceControlService.class)) {
            pcs =
                (PersistenceControlService)sb.getService(this, PersistenceControlService.class, null);
          }
        }
      });
    }
  }

  /**
   * Alter persistence parameters of specified agents (local and/or remote).
   * @param agentNames
   * @param persistNow
   * @param controls
   */
  public void controlPersistence(String[] agentNames, boolean persistNow, Properties controls) {
    logger.info("controlPersistence:" +
                " agents=" + arrayToString(agentNames) +
                " persistNow=" + persistNow +
                " controls=" + controls);
    PersistenceControlRequest pcr = new PersistenceControlRequest(agentId, persistNow, controls, uidService.nextUID());
    RelayAdapter ra = new RelayAdapter(agentId, pcr, pcr.getUID());
    for (int i = 0; i < agentNames.length; i++) {
      if (agentNames[i].equals(agentId.toString())) {
        setPersistenceControls(persistNow, controls);
      } else {
        ra.addTarget(MessageAddress.getMessageAddress(agentNames[i]));
      }
    }
    // Queue for remote send
    if (!ra.getTargets().isEmpty()) {
      myUIDs.add(ra.getUID());
      fireLater(ra);
    }
  }

  /**
   * Alter persistence parameters of local agent.
   * @param persistNow  Request immediate persistence snapshot
   * @param controls    Change specified control values
   */
  public void setPersistenceControls(boolean persistNow, Properties controls) {
    this.persistNow = persistNow;
    for (Iterator cit = controls.entrySet().iterator(); cit.hasNext();) {
      Map.Entry me = (Map.Entry)cit.next();
      setPersistenceControl((String)me.getKey(), new Long((String)me.getValue()));
    }
  }

  /**
   * Change persistence control value for local agent.
   * @param name    Control name
   * @param value   New value
   */
  public void setPersistenceControl(String name, Comparable value) {
    String[] mediaNames = pcs.getMediaNames();
    for (int i = 0; i < mediaNames.length; i++) {
      logger.info("setPersistenceControl:" +
                  " media=" + mediaNames[i] +
                  " control=" + name +
                  " value=" + value);
      pcs.setMediaControlValue(mediaNames[i], name, value);
    }
  }

  /**
   * Get persistence parameters names as string.
   * @return  String representing recognized persistence control parameters
   */
  public String getPersistenceParamaters() {
    StringBuffer sb = new StringBuffer("PersistenceParameters: ");
    sb.append(arrayToString(pcs.getControlNames(), "controlNames") + ", ");
    sb.append(arrayToString(pcs.getMediaNames(), "mediaNames"));
    String[] mediaNames = pcs.getMediaNames();
    for (int i = 0; i < mediaNames.length; i++) {
      sb.append(", " + arrayToString(pcs.getMediaControlNames(mediaNames[i]), "mediaControlNames(" + mediaNames[i] + ")"));
    }
    return sb.toString();
  }

  /**
   * Add PersistenceControlRequest Relay to send queue.
   * @param toSend  Relay
   */
  protected void fireLater(Object toSend) {
    synchronized (todo) {
      todo.add(toSend);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  /**
   * Publish all PersistenceControlRequests Relays in queue.
   */
  private void fireAll() {
    int n;
    List l;
    synchronized (todo) {
      n = todo.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(todo);
      todo.clear();
    }
    for (int i = 0; i < n; i++) {
      RelayAdapter ra = (RelayAdapter)l.get(i);
      logger.info("publishAdd: " + ra);
      blackboard.publishAdd(ra);
    }
  }

  /**
   * Utility method for converting array to string.
   * @param sa   Array
   * @param id   Label
   * @return String containing array values
   */
  protected String arrayToString(String[] sa, String id) {
    return id + "=" + arrayToString(sa);
  }

  /**
   * Utility method for converting array to string.
   * @param sa   Array
   * @return String containing array values
   */
  protected String arrayToString(String[] sa) {
    StringBuffer sb = new StringBuffer("[");
    if (sa != null) {
      for (int i = 0; i < sa.length; i++) {
        sb.append(sa[i]);
        if (i < sa.length - 1)
          sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  private IncrementalSubscription persistenceControlRequests;
  private UnaryPredicate persistenceControlRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof RelayAdapter) {
        RelayAdapter ra = (RelayAdapter)o;
        return (myUIDs.contains(ra.getUID()));
      }
      return false;
  }};
}
