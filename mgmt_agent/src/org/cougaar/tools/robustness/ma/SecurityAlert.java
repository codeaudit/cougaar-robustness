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

import org.cougaar.tools.robustness.threatalert.ThreatAlert;
import org.cougaar.tools.robustness.threatalert.DefaultThreatAlert;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

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
   * @param source         Alert source
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param duration       Duration of threat period (-1 == never expires)
   * @param uid            Unique identifier
   */
  public SecurityAlert(MessageAddress source,
                             int severityLevel,
                             Date start,
                             long duration,
                             UID uid) {
    super(source, severityLevel, start, duration, uid);
  }

  /**
   * Create a new SecurityAlert.
   * @param source         Alert source
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param expiration     Time at which alert expires
   * @param uid            Unique identifier
   */
  public SecurityAlert(MessageAddress    source,
                            int               severityLevel,
                            Date              start,
                            Date              expiration,
                            UID               uid) {
   super(source, severityLevel, start, expiration, uid);
  }
}
