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