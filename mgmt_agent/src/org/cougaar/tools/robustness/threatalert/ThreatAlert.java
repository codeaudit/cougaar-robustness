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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UniqueObject;

import java.util.Date;

/**
 * Basic ThreatAlert defining alert level and affected assets.
 */
public interface ThreatAlert extends UniqueObject, Relay.Target {

  // ThreatAlert severity levels
  public static final int MAXIMUM_SERVERITY  = 4;
  public static final int HIGH_SEVERITY      = 3;
  public static final int MEDIUM_SEVERITY    = 2;
  public static final int LOW_SEVERITY       = 1;
  public static final int MINIMUM_SEVERITY   = 0;
  public static final int UNDEFINED_SEVERITY = -1;

  /**
   * Get alert source.
   */
  public void setSource(MessageAddress source);

  /**
   * Get severity level associated with this alert.
   * @return SeverityLevel
   */
  public int getSeverityLevel();

  /**
   * Get severity level associated with this alert.
   * @return SeverityLevel as String
   */
  public String getSeverityLevelAsString();

  /**
   * Get time alert was created.
   * @return ThreatAlert creation time.
   */
  public Date getCreationTime();

 /**
   * Get time that alert becomes active.
   * @return ThreatAlert activation time.
   */
  public Date getStartTime();

  /**
   * Get time that alert expires.
   * @return ThreatAlert expiration time.
   */
  public Date getExpirationTime();

  /**
   * Returns true if the alert is currently active.  An alert is active
   * if startTime < currentTime < expirationTime;
   * @return True if alert is active
   */
  public boolean isActive();

  /**
   * Returns true if the alert period has expired.  An alert is expired
   * if currentTime > expirationTime;
   * @return True if alert period has expired
   */
  public boolean isExpired();

  /**
   * Get Assets that are affected by the alert.
   * @return Array of Assets affected by alert.
   */
  public Asset[] getAffectedAssets();

}