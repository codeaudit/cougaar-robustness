/* 
 * <copyright>
 * Copyright 2002-2004 Object Services and Consulting, Inc.
 * Copyright 2002 BBNT Solutions, LLC

 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
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
