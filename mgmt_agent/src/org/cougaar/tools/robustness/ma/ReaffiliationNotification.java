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
package org.cougaar.tools.robustness.ma;

import org.cougaar.tools.robustness.threatalert.DefaultThreatAlert;
import org.cougaar.core.mts.MessageAddress;

/**
 * ReaffiliationNotification signifying that one or more enclave members is
 * being added/removed from robustness community.
 */
public class ReaffiliationNotification extends DefaultThreatAlert {

  private String entityType = "Node";
  private String oldCommunity;
  private String newCommunity;
  private long timeout;

  /**
   * Default constructor.
   */
  public ReaffiliationNotification(MessageAddress source,
                                   String         oldCommunity,
                                   String         newCommunity,
                                   long           timeout) {
    super();
    setSource(source);
    this.oldCommunity = oldCommunity;
    this.newCommunity = newCommunity;
    this.timeout = timeout;
  }

  public ReaffiliationNotification(MessageAddress source,
                                   String         oldCommunity,
                                   String         newCommunity,
                                   long           timeout,
                                   String         entityType) {
    this (source, oldCommunity, newCommunity, timeout);
    this.entityType = entityType;
  }

  public String getOldCommunity() {
    return oldCommunity;
  }

  public String getNewCommunity() {
    return newCommunity;
  }

  public String getEntityType() {
    return entityType;
  }

  public long getTimeout() {
    return timeout;
  }

  public String toString() {
    return "ReaffiliationNotification:" +
           " source=" + getSource() +
           " oldCommunity=" + oldCommunity +
           " newCommunity=" + newCommunity +
           " timeout=" + timeout;
  }

}
