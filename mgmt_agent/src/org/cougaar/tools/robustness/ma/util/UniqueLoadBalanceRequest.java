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
package org.cougaar.tools.robustness.ma.util;

import org.cougaar.robustness.exnihilo.plugin.LoadBalanceRequest;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

import java.util.List;

/**
 */

public class UniqueLoadBalanceRequest extends LoadBalanceRequest implements UniqueObject {

  private UID myUID;

  public UniqueLoadBalanceRequest(int annealTime,
                                  int solverMode,
                                  boolean useHamming,
                                  List newNodes,
                                  List killedNodes,
                                  List leaveAsIsNodes,
                                  UID uid) {
    super(annealTime, solverMode, useHamming, newNodes, killedNodes, leaveAsIsNodes);
    myUID = uid;

  }

  public UID getUID() { return myUID; }

   /** Set the UID of a UniqueObject. Will throw a RuntimeException if
    * the UID was already set.
    **/
   public void setUID(UID uid) {
     if (myUID != null) {
       RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
       throw rt;
     }
     myUID = uid;
   }
}