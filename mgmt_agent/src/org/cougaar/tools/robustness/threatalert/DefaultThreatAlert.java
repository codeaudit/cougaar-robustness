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
import org.cougaar.core.util.UID;
import org.cougaar.core.relay.Relay;

import java.util.*;

/**
 * Default implementation of ThreatAlert.
 */
public class DefaultThreatAlert implements ThreatAlert, java.io.Serializable {

  public static final long NEVER = -1;

  private MessageAddress source;
  private int severityLevel;
  private Date creationTime;
  private long startTime;
  private long duration;
  private UID uid;
  private List assets;

  /**
   * Create a new ThreatAlert.
   * @param source         ThreatAlert source
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param expiration     Time at which alert expires
   * @param uid            Unique identifier
   */
  public DefaultThreatAlert(MessageAddress    source,
                            int               severityLevel,
                            Date              start,
                            Date              expiration,
                            UID               uid) {
    this.source = source;
    this.severityLevel = severityLevel;
    this.creationTime = new Date();
    this.startTime = start.getTime();
    this.duration = expiration.getTime() - start.getTime();
    this.uid = uid;
    this.assets = new ArrayList();
  }

  /**
   * Create a new ThreatAlert.
   * @param source         ThreatAlert source
   * @param severityLevel  Severity level of alert
   * @param start          Time at which alert becomes active
   * @param duration       Duration of threat period (-1 == never expires)
   * @param uid            Unique identifier
   */
  public DefaultThreatAlert(MessageAddress  source,
                            int             severityLevel,
                            Date            start,
                            long            duration,
                            UID             uid) {
    this.source = source;
    this.severityLevel = severityLevel;
    this.startTime = start.getTime();
    this.duration = duration;
    this.uid = uid;
    this.assets = new ArrayList();
    this.creationTime = new Date();
  }

  public int getSeverityLevel() {
    return this.severityLevel;
  }
  public void setSeverityLevel(int severityLevel) {
    this.severityLevel = severityLevel;;
  }

  public void setCreationTime(Date creation) {
    this.creationTime = creation;
  }

  public Date getCreationTime() {
    return this.creationTime;
  }

  public Date getStartTime() {
    return new Date(startTime);
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime.getTime();
  }

  public Date getExpirationTime() {
    return new Date(startTime + duration);
  }

  public void setExpirationTime(Date expirationTime) {
    this.duration = expirationTime.getTime() - startTime;
  }

  public Asset[] getAffectedAssets() {
    Asset[] as = new Asset[assets.size()];
    for(int i=0; i<assets.size(); i++) {
      as[i] = (Asset)assets.get(i);
    }
    return as;
  }

  /**
   * Returns true if the alert is currently active.  An alert is active
   * if startTime < currentTime < expirationTime;
   * @return True if alert is active
   */
  public boolean isActive() {
    long now = now();
    return now > startTime && !isExpired();
  }

  /**
   * Returns true if the alert period has expired.  An alert is expired
   * if currentTime > expirationTime;
   * @return True if alert period has expired
   */
  public boolean isExpired() {
    long now = now();
    return duration == NEVER || now > (startTime + duration);
  }

  public void addAsset(Asset asset) {
    assets.add(asset);
  }

  public String getSeverityLevelAsString() {
    switch (severityLevel) {
      case ThreatAlert.HIGH_SEVERITY: return "HIGH";
      case ThreatAlert.LOW_SEVERITY: return "LOW";
      case ThreatAlert.MAXIMUM_SERVERITY: return "MAXIMUM";
      case ThreatAlert.MEDIUM_SEVERITY: return "MEDIUM";
      case ThreatAlert.MINIMUM_SEVERITY: return "MINIMUM";
      case ThreatAlert.UNDEFINED_SEVERITY: return "UNDEFINED";
    }
    return "Invalid Value";
  }

  /**
   * Returns current time as a long.
   * @return current time
   */
  protected long now() {
    return System.currentTimeMillis();
  }

  public String toString() {
    return "ThreatAlert:" +
        " severityLevel=" + getSeverityLevelAsString() +
        " start=" + startTime +
        " expired=" + getExpirationTime() +
        " asset=" + assets;
  }

  //Relay.Target methods
  public Object getResponse() {
    return null;
  }

  public MessageAddress getSource() {
    return source;
  }

  public int updateContent(Object content, Relay.Token token) {
    ThreatAlert alert = (ThreatAlert)content;
    this.severityLevel = alert.getSeverityLevel();
    this.creationTime = alert.getCreationTime();
    this.startTime = alert.getStartTime().getTime();
    this.duration = alert.getExpirationTime().getTime() - startTime;
    this.assets.clear();
    Asset[] as = alert.getAffectedAssets();
    for(int i=0; i<as.length; i++) {
      addAsset(as[i]);
    }
    return Relay.CONTENT_CHANGE;
  }

  public void setUID(UID uid) {
    if (uid != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }
    this.uid = uid;
  }
  public UID getUID() {
    return this.uid;
  }



}