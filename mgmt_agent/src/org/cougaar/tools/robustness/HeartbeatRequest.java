/*
 * <copyright>
 * Copyright 2002 Mobile Intelligence Corporation
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
package org.cougaar.tools.robustness;

import java.util.Date;

import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;

/**
 * Request from ManagementAgent to monitored agents to begin transmitting
 * Heartbeat messages.
 * Extends Relay so that messages can be sent to a role based
 * AttributeBasedAddress.
 **/
public interface HeartbeatRequest extends Relay.Source, Relay.Target, UniqueObject {

  /**
   * Add a target message address.
   * @param target the address of the target agent.
   **/
  public void addTarget(MessageAddress target);

  /**
   * Remove a target message address.
   * @param target the address of the target agent to be removed.
   **/
  public void removeTarget(MessageAddress target);

  /**
   *
   * @return HeartbeatInterval
   */
  public long getInterval();

  /**
   *
   * @param interval  Defines frequency at which Heartbeats are generated.
   */
  public void setInterval(long interval);

}