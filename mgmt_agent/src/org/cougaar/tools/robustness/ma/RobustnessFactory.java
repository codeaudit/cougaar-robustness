/*
 * <copyright>
 *  Copyright 2002 Mobile Intelligence Corporation
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

package org.cougaar.tools.robustness.ma;

import org.cougaar.tools.robustness.HealthReport;
import org.cougaar.tools.robustness.HeartbeatRequest;
import org.cougaar.tools.robustness.HealthReportImpl;
import org.cougaar.tools.robustness.HeartbeatRequestImpl;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.domain.LDMServesPlugin;

import org.cougaar.core.service.UIDServer;


/**
 * HealthReport factory implementation for testing.
 **/

public class RobustnessFactory
  implements org.cougaar.core.domain.Factory {

  UIDServer myUIDServer;

  public RobustnessFactory(LDMServesPlugin ldm) {
    RootFactory rf = ldm.getFactory();

    myUIDServer = ldm.getUIDServer();
  }

  /**
   * new HealthReport - returns a new HealthReport
   *
   */
  public HealthReport newHealthReport(MessageAddress source) {
    HealthReportImpl hr = new HealthReportImpl();
    hr.setSource(source);
    hr.setUID(myUIDServer.nextUID());
    return hr;
  }

  /**
   * new HeartbeatRequest - returns a new HeartbeatRequest
   *
   */
  public HeartbeatRequest newHeartbeatRequest(long interval, MessageAddress source) {
    HeartbeatRequestImpl hbr = new HeartbeatRequestImpl(interval);
    hbr.setSource(source);
    hbr.setUID(myUIDServer.nextUID());
    return hbr;
  }
}
