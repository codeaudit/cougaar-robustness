/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  Copyright 2002 Object Services and Consulting, Inc.
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

package org.cougaar.tools.robustness.sensors;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.mts.MessageAddress;

/**
 * Factory for PingRequests & HeartbeatRequests.
 **/
public class SensorFactory implements org.cougaar.core.domain.Factory {
  UIDServer myUIDServer;

  /**
   * Constructor
   **/
  public SensorFactory(LDMServesPlugin ldm) {
    //RootFactory rf = ldm.getFactory();
    myUIDServer = ldm.getUIDServer();
  }

  /** 
   * Creates a new PingRequest.
   *
   * @param source MessageAddress of Agent issuing the PingRequest
   * @param target MessageAddress of Agent to be pinged
   * @param timeout milliseconds before PingRequest will time out
   *
   * @return PingRequest
   */
  public PingRequest newPingRequest(MessageAddress source, 
                                    MessageAddress target, 
                                    long timeout) {
    UID uid = myUIDServer.nextUID();
    PingRequest req = new PingRequest(uid, source, target, timeout);
    return req;
  }

  /** 
   * Creates a new HeartbeatRequest.
   *
   * @param source MessageAddress of agent requesting the Heartbeat
   * @param target MessageAddress of agent providing the Heartbeat
   * @param reqTimeout Request timeout in milliseconds
   * @param hbFrequency Heartbeat frequency in milliseconds
   * @param hbTimeout Heartbeat timeout in milliseconds
   * @param onlyOutOfSpec only report if heartbeat is late,
   * as specified by hbFrequency
   * @param percentOutOfSpec only report when heartbeat is 
   * this much later than specified by hbFrequency
   *
   * @return HeartbeatRequest
   */
  public HeartbeatRequest newHeartbeatRequest(MessageAddress source,  
                                              MessageAddress target, 
                                              long reqTimeout,
                                              long hbFrequency,
                                              long hbTimeout,
                                              boolean onlyOutOfSpec,
                                              float percentOutOfSpec) {
    UID uid = myUIDServer.nextUID();
    HeartbeatRequest req = new HeartbeatRequest(uid, 
                                                source,
                                                target,
                                                reqTimeout,
                                                hbFrequency,
                                                hbTimeout,
                                                onlyOutOfSpec,
                                                percentOutOfSpec);
    return req;
  }

}
  













