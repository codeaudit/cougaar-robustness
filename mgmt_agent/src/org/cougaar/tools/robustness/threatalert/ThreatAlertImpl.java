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

public class ThreatAlertImpl implements ThreatAlert, java.io.Serializable {

  private MessageAddress source;
  private int severityLevel;
  private Date create, start, expired;
  private UID uid;
  private List assets;

  public ThreatAlertImpl(MessageAddress    source,
                         int               severityLevel,
                         Date              start,
                         Date              expiration,
                         UID               uid) {
    this.source = source;
    this.severityLevel = severityLevel;
    this.start = start;
    this.expired = expiration;
    this.uid = uid;
    this.assets = new ArrayList();
  }

  public int getSeverityLevel() {
    return this.severityLevel;
  }

  public void setCreationTime(Date creation) {
    this.create = creation;
  }

  public Date getCreationTime() {
    return this.create;
  }

  public Date getStartTime() {
    return this.start;
  }

  public Date getExpirationTime() {
    return this.expired;
  }

  public Asset[] getAffectedAssets() {
    Asset[] as = new Asset[assets.size()];
    for(int i=0; i<assets.size(); i++) {
      as[i] = (Asset)assets.get(i);
    }
    return as;
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

  public String toString() {
    return "ThreatAlert:" +
        " severityLevel=" + getSeverityLevelAsString() +
        " start=" + start +
        " expired=" + expired +
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
    this.create = alert.getCreationTime();
    this.start = alert.getStartTime();
    this.expired = alert.getExpirationTime();
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