/* 
 * <copyright>
 * Copyright 2002-2003 Object Services and Consulting, Inc.
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.coordinator.test.coordination;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.adaptivity.*;

public class MetricsTestPlugin extends ServiceUserPlugin {

  private MetricsService metricsService;

  private static final Class[] requiredServices = {
    MetricsService.class
  };

  public MetricsTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    if (haveServices()) setMetricsConditions();
  }

  private boolean haveServices() {
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      metricsService = (MetricsService)
        sb.getService(this, MetricsService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setMetricsConditions();
      }
    }
  }

  private void setMetricsConditions() {
    logger.info("setMetricsConditions");
    Metric metricIn =  metricsService.getValue("Agent([TestAgent]):BytesIn1000SecAvg");
    Metric metricOut =  metricsService.getValue("Agent([TestAgent]):BytesOut1000SecAvg");
    if (metricIn != null) {
        long valueL = metricIn.longValue();
        if (logger.isInfoEnabled()) logger.info(valueL + " bytes average coming into TestAgent");
    } else {
      logger.warn("metricIn is null");
    }
    if (metricOut != null) {
        long valueL = metricOut.longValue();
        if (logger.isInfoEnabled()) logger.info(valueL + " bytes average leaving TestAgent");
    } else {
      logger.warn("metricOut is null");
    }
    resetTimer(1000); //102B
  }
}
