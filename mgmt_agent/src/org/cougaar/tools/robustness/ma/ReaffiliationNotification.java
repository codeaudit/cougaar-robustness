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

import java.util.Date;

/**
 * ReaffiliationNotification signifying that one or more enclave members is
 * being added/removed from robustness community.
 */
public class ReaffiliationNotification extends DefaultThreatAlert {

  protected String community;

  /**
   * Default constructor.
   */
  public ReaffiliationNotification() {
    super();
  }

  /**
   * Create a new ReaffiliationNotification.
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param duration       Duration of threat period (ThreatAlert.FOREVER == never expires)
   */
  public ReaffiliationNotification(int    severityLevel,
                                   Date   start,
                                   long   duration,
                                   String newCommunity) {
    super(severityLevel, start, duration);
    community = newCommunity;
  }

  /**
   * Create a new ReaffiliationNotification.
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param expiration     Time at which alert expires
   */
  public ReaffiliationNotification(int    severityLevel,
                                   Date   start,
                                   Date   expiration,
                                   String newCommunity) {
    super(severityLevel, start, expiration);
    community = newCommunity;
  }

  public String getCommunity() {
    return community;
  }

}
