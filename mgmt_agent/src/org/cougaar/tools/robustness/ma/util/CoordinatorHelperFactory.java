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

import org.cougaar.tools.robustness.ma.RestartManagerConstants;
import org.cougaar.core.component.BindingSite;
import java.lang.reflect.Constructor;

/**
 */

public class CoordinatorHelperFactory implements RestartManagerConstants {

  public static CoordinatorHelper getCoordinatorHelper(BindingSite bs) {
    String coordinatorClassname =
      System.getProperty(COORDINATOR_CLASS_PROPERTY, DEFAULT_COORDINATOR_CLASSNAME);
    try {
      Class cls = Class.forName(coordinatorClassname);
      Constructor cons[] = cls.getConstructors();
      return (CoordinatorHelper)cons[0].newInstance(new Object[]{bs});
    } catch (Exception ex) {
      System.err.println("Exception creating Coordinator helper, " + ex);
      return null;
    }
  }
}
