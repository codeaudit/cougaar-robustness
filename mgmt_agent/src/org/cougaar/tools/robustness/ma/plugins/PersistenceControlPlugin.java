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
package org.cougaar.tools.robustness.ma.plugins;

import org.cougaar.tools.robustness.ma.util.PersistenceHelper;
import org.cougaar.tools.robustness.ma.ldm.PersistenceControlRequest;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Provides remote control of agent persistence.  Used by robustness management
 * agent to change persistence paramaters based on anticipated threats and
 * changes to current operating conditions.
 */
public class PersistenceControlPlugin extends ComponentPlugin {

  private LoggingService logger;
  private PersistenceHelper persistenceHelper;

  public void setupSubscriptions() {
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");

    // Subscribe to external requests
    persistenceControlRequests =
        (IncrementalSubscription)blackboard.subscribe(persistenceControlRequestPredicate);

    persistenceHelper = new PersistenceHelper(getBindingSite());
  }

  public void execute() {
    Collection requests = persistenceControlRequests.getAddedCollection();
    for (Iterator it = requests.iterator(); it.hasNext();) {
      PersistenceControlRequest pcr = (PersistenceControlRequest)it.next();
      logger.debug("Received PersistenceControlRequest: " + pcr);
      persistenceHelper.setPersistenceControls(pcr.persistNow(), pcr.getControls());
      pcr.setResponder(agentId);
      blackboard.publishChange(pcr);
    }
  }

  private IncrementalSubscription persistenceControlRequests;
  private UnaryPredicate persistenceControlRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof PersistenceControlRequest);
  }};
}
