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

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;

/**
 * Agent-level component that loads the ThreatAlertService provider and adds
 * initial threat alert relationships for agent to Name Server.
 */
public class ThreatAlertServiceComponent extends ComponentSupport {

  private LoggingService log = null;

  public ThreatAlertServiceComponent() {
    super();
  }

  /**
   * Initializes ThreatAlertService and adds initial threat alert
   * relationships for this agent to Name Server.
   */
  public void load() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    MessageAddress agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);
    ThreatAlertService tas = loadThreatAlertService(agentId);
    super.load();
  }

  /**
   * Creates a ThreatAlert
   * Service instance and adds to agent ServiceBroker.
   * @param agentId
   * @return
   */
  private ThreatAlertService loadThreatAlertService(MessageAddress agentId) {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    ThreatAlertServiceProvider tasp =
        new ThreatAlertServiceProvider(getBindingSite());
    sb.addService(ThreatAlertService.class, tasp);
    return (ThreatAlertService)sb.getService(this, ThreatAlertService.class,
      new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {}
    });
  }

}
