/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.tools.manager.ldm.report;


/**
 * HeartbeatHealthReport: HealthReport with an associated list of 
 * monitored agents, time last msg was received, and whether that
 * was in-spec or not, according to current HeartbeatRequests.
 **/
public class HeartbeatHealthReport extends HealthReportAdapter {
  
  private String myText = null;
  private String myToString = null;

  /**
   * HeartbeatHealthReport - no arg Constructor
   **/
  public HeartbeatHealthReport() {
  }

  /**
   * HeartbeatHealthReport - Constructor
   * 
   * @param reportText String specifying the text associated with the 
   * HealthReport
   **/
  public HeartbeatHealthReport(String reportText) {
    super();
    setText(reportText);
  }

  /**
   * getText - get the text associated with the HealthReport
   *
   * @return String - text associated with the HealthReport
   **/
  public String getText() {
    return myText;
  }

  /**
   * setText - set the text associated with the HealthReport
   *
   * @param reportText String text to be associated with the HealthReport
   **/
  public void setText(String reportText) {
    myText = reportText;
    myToString = null;
  }

  /**
   * toString -  Returns a string representation of the HeartbeatHealthReport
   *
   * @return String - a string representation of the HeartbeatHealthReport.
   **/
  public String toString() {
    if (myToString == null) {
      myToString = "HeartbeatHealthReport: " + getText();
      myToString = myToString.intern();
    }

    return myToString;
  }
}








