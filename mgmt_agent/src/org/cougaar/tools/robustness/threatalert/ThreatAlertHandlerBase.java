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

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

/**
 * Base class for handling ThreatAlerts.  A listener is added to the
 * ThreatAlertService.  Classes extending this must implement the "newAlert" and
 * "expiredAlert" abstract methods to receive alert notifications.
 */
public abstract class ThreatAlertHandlerBase implements ThreatAlertListener {

  protected LoggingService logger;
  protected BindingSite bindingSite;
  protected MessageAddress agentId;

  public ThreatAlertHandlerBase(BindingSite    bs,
                                 MessageAddress agentId) {
    this.bindingSite = bs;
    this.agentId = agentId;
    ServiceBroker sb = bindingSite.getServiceBroker();
    logger =
      (LoggingService)sb.getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    if (sb.hasService(ThreatAlertService.class)) {
      initThreatAlertListener();
    }
    else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(ThreatAlertService.class)) {
            initThreatAlertListener();
          }
        }
      });
    }
  }

  private void initThreatAlertListener() {
    logger.debug("Initializing ThreatAlertListener");
    ServiceBroker sb = bindingSite.getServiceBroker();
    ThreatAlertService tas =
        (ThreatAlertService) sb.getService(this, ThreatAlertService.class, null);
    tas.addListener(this);
  }

  public abstract void newAlert(ThreatAlert ta);
  public abstract void changedAlert(ThreatAlert ta);
  public abstract void removedAlert(ThreatAlert ta);

}