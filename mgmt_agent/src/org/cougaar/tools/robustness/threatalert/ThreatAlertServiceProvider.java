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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.Service;

import org.cougaar.core.mts.MessageAddress;

/**
 * Returns a reference to a ThreatAlertService.
 */

public class ThreatAlertServiceProvider implements ServiceProvider {

  private ThreatAlertService ts;

  public ThreatAlertServiceProvider (BindingSite bs, MessageAddress agentId) {
    ts = ThreatAlertServiceImpl.getInstance(bs, agentId);
  }

  public Object getService(ServiceBroker sb, Object requestor,
      Class serviceClass) {
    if (serviceClass == ThreatAlertService.class)
      return ts;
    else
      throw new IllegalArgumentException(
        "ThreatAlertServiceProvider does not provide a service for: "+
        serviceClass);
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service) {
  }

}
