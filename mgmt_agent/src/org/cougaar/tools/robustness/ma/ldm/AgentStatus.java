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

//import org.cougaar.tools.robustness.ma.Controller;

/**
 **/
public class AgentStatus implements java.io.Serializable {

  protected String name;
  protected String location;
  protected int status;

  public AgentStatus(String name, String location, int status) {
    this.name = name;
    this.location = location;
    this.status = status;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }
  public void setLocation(String location) {
    this.location = location;
  }

  public int getStatus() {
    return status;
  }
  public void setStatus(int status) {
    this.status = status;
  }

  public String toString() {
    return name + ":" + location + ":" + status;
  }

  public String toXML() {
    return toXML("");
  }

  public String toXML(String indent) {
    return indent + "<agent" +
        " name=\"" + name + "\"" +
        " status=\"" + status + "\"" +
        " location=\"" + location + "\"" +
        "/>";
  }
}
