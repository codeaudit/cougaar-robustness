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

/**
 * Response to request performed by HealthMonitor component.
 **/
public class HealthMonitorResponse implements java.io.Serializable {

  public static final int UNDEFINED = -1;
  public static final int SUCCESS   =  0;
  public static final int FAIL      =  1;
  public static final int TIMEOUT   =  2;

  private int statusCode = UNDEFINED;
  private Object content = null;

  public HealthMonitorResponse(int code, Object content) {
    this.statusCode = code;
    this.content = content;
  }

  public int getStatus() {
    return statusCode;
  }

  public Object getContent() {
    return content;
  }

  public String getStatusAsString() {
    switch (statusCode) {
      case UNDEFINED: return "UNDEFINED";
      case FAIL: return "FAIL";
      case SUCCESS: return "SUCCESS";
      case TIMEOUT: return "TIMEOUT";
    }
    return "INVALID_VALUE";
  }

  public String toString() {
    return "(" + getStatusAsString() + ":" +
        (content == null ? "null" : content.getClass().getName()) + ")";
  }

}
