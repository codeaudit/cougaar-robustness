/* 
 *@author Steve Ford - OBJS
 *
 * <copyright>
 * Copyright 2003 BBNT Solutions, LLC
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

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

public class RobustnessManagerID implements java.io.Serializable, NotPersistable, UniqueObject {
  MessageAddress addr;
  UID uid;

  public RobustnessManagerID() {
  }
  public RobustnessManagerID(MessageAddress addr, UID uid) {
    this.addr = addr;
    this.uid = uid;
  }
  public MessageAddress getMessageAddress() {
    return addr;
  }
  public UID getUID () { return uid; } 

  public void setUID(UID uid) { throw new RuntimeException("Attempt to change UID"); }

  public String toString() {
    return addr.toString();
  }

}








