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

/**
 * ThreatAlert listener that is notified when a ThreatAlert is received
 * changed, or removed (expired).
 */
public interface ThreatAlertListener {

  /**
   * Callback method invoked when a new ThreatAlert is received by
   * ThreatAlertService.  Note that receipt of the alert does not necessarily mean
   * the alert is active as the alerts start time could be for some time in
   * the future.
   * @param ta  New ThreatAlert
   */
  public void newAlert(ThreatAlert ta);

  /**
   * Callback method invoked when an existing alert is modified by sender.
   */
  public void changedAlert(ThreatAlert ta);

  /**
   * Callback method invoked when an existing alert is removed by sender.  An
   * alert is removed when it has reached its expiration time.
   */
  public void removedAlert(ThreatAlert ta);

}