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

import org.cougaar.tools.robustness.ma.ReaffiliationNotificationHandler;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;

/**
 * Loads reaffiliation notification handler to join or leave communities.
 */
public class ReaffiliationPlugin extends ComponentPlugin {

  private LoggingService logger;
  private ReaffiliationNotificationHandler reaffiliationHandler;

  public void load() {
    super.load();
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    if (logger.isDebugEnabled()) {
      logger.debug("load ReaffiliationNotificationHandler: agentId=" + agentId);
    }
    reaffiliationHandler = new ReaffiliationNotificationHandler(getBindingSite(), agentId);
  }

  public void unload() {
    if (logger != null) {
      getBindingSite().getServiceBroker().releaseService(this, LoggingService.class, logger);
      logger = null;
    }
    if (reaffiliationHandler != null) {
      reaffiliationHandler.unload();
    }
  }

  public void setupSubscriptions() {
  }

  public void execute() {
  }

}
