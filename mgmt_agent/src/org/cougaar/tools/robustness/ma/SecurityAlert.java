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
 * Alert signifying a change in security posture.
 */
public class SecurityAlert extends DefaultThreatAlert {

  /**
   * Default constructor.
   */
  public SecurityAlert() {
    super();
  }

  /**
   * Create a new SecurityAlert.
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param duration       Duration of threat period (-1 == never expires)
   */
  public SecurityAlert(int severityLevel,
                       Date start,
                       long duration) {
    super(severityLevel, start, duration);
  }

  /**
   * Create a new SecurityAlert.
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param expiration     Time at which alert expires
   */
  public SecurityAlert(int  severityLevel,
                       Date start,
                       Date expiration) {
   super(severityLevel, start, expiration);
  }

  /**
   * Create a new SecurityAlert using the current time as the threat start time.
   * @param severityLevel  Severity level of alert
   * @param duration       Duration of threat period (FOREVER == never expires)
   */
  public SecurityAlert(int  severityLevel,
                       long duration) {
    super(severityLevel, duration);
  }

  /**
   * Create a new SecurityAlert using the current time as the threat start time
   * and a duration of FOREVER.
   * @param severityLevel  Severity level of alert
   */
  public SecurityAlert(int  severityLevel) {
    super(severityLevel);
  }
}
