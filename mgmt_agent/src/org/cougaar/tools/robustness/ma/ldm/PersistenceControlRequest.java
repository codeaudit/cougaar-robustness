/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

package org.cougaar.tools.robustness.ma.ldm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UniqueObject;

import java.util.Properties;

/**
 * Request for change in agents persistence control.
 **/
public class PersistenceControlRequest
  implements Relay.Target, UniqueObject {

  private MessageAddress source;
  private UID uid;
  private boolean persistNow;
  private Properties controls;
  private MessageAddress target;

  /**
   * Constructor.
   */
  public PersistenceControlRequest(MessageAddress     source,
                                   boolean            persistNow,
                                   Properties         controls,
                                   UID                uid) {
    this.source = source;
    this.persistNow = persistNow;
    this.controls = controls;
    this.uid = uid;
  }

  public boolean persistNow() {
    return this.persistNow;
  }

  public Properties getControls() {
    return this.controls;
  }

  public void setResponder(MessageAddress target) {
    this.target = target;
  }

  //
  // Relay.Target Interface methods
  //
  public Object getResponse() {
    return target;
  }

  public MessageAddress getSource() {
    return source;
  }

  public int updateContent(Object content, Relay.Token token) {
    PersistenceControlRequest pcr = (PersistenceControlRequest)content;
    this.persistNow = pcr.persistNow();
    this.controls = (Properties)pcr.getControls().clone();
    return Relay.CONTENT_CHANGE;
  }

  //
  // UniqueObject Interface methods
  //
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

  /**
   * Returns a string representation of the request
   *
   * @return String - a string representation of the request.
   **/
  public String toString() {
    return "PersistenceControlRequest:" +
           " persistNow=" + persistNow +
           " controls=" + controls;
  }

}
