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

import org.cougaar.core.component.Service;

/**
 * Service providing the ability to send and receive ThreatAlert relays.
 *
 */
public interface ThreatAlertService extends Service {

  /**
   * Add a listener to receive new ThreatAlerts.
   * @param tal  ThreatAlertListener to receive ThreatAlert notifications
   */
  public void addListener(ThreatAlertListener tal);

 /**
  * Remove ThreatAlert listener.
  * @param tal  ThreatAlertListener
  */
  public void removeListener(ThreatAlertListener tal);

  /**
   * Broadcast a new ThreatAlert message.  The ThreatAlert is sent to all
   * agents in the specified community with the specified role.
   * @param ta  ThreatAlert to broadcast
   * @param community  Destination community to receive alert
   * @param role       Agents roles to receive alert
   */
  public void sendAlert(ThreatAlert ta, String community, String role);

  /**
   * Used by ThreatAlert originator to update alert contents.
   * @param ta  ThreatAlert to update
   * @param community  Destination community to receive alert
   * @param role       Agents roles to receive alert
   */
  public void updateAlert(ThreatAlert ta);

  /**
   * Get all current ThreatAlerts.
   * @return Array of current ThreatAlerts
   */
  public ThreatAlert[] getCurrentAlerts();
}