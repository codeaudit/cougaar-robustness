package org.cougaar.tools.robustness.ma.util;

/**
 */

public interface CheckServicesListener {

  public void execute(String  communityName,
                      String  serviceCategory,
                      boolean isAvailable,
                      String  message);

}
