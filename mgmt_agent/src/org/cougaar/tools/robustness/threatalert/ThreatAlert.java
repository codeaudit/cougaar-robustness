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

import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UniqueObject;

import java.util.Date;

public interface ThreatAlert extends UniqueObject, Relay.Target {

  public static final int MAX_SERVERITY      = 4;
  public static final int HIGH_SEVERITY      = 3;
  public static final int MEDIUM_SEVERITY    = 2;
  public static final int LOW_SEVERITY       = 1;
  public static final int MIN_SEVERITY       = 0;
  public static final int UNDEFINED_SEVERITY = -1;

  public int getSeverityLevel();
  public Date getCreationTime();
  public Date getStartTime();
  public Date getExpirationTime();
  public Asset[] getAffectedAssets();
  public String getSeverityLevelAsString();
  public String toString();

}