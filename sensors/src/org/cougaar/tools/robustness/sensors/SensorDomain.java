/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  Copyright 2002-2003// Object Services and Consulting, Inc.
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

package org.cougaar.tools.robustness.sensors;

import java.util.*;
//100 import org.cougaar.core.agent.ClusterServesLogicProvider;
//100 import org.cougaar.core.blackboard.LogPlan;
//100 import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker; //100
import org.cougaar.core.domain.DomainAdapter;
//100 import org.cougaar.core.domain.DomainBindingSite;
import org.cougaar.tools.manager.Constants;
import org.cougaar.core.service.UIDService; //100

/**
 * Domain for Sensors, named "sensors".
 **/
public class SensorDomain extends DomainAdapter {
  private static final String SENSORS_NAME = "sensors".intern();

  private UIDService uidService; //100

  /**
   * getDomainName - returns the Domain name. Used as domain identifier in 
   * DomainService.getFactory(String domainName)
   *
   * @return String domain name
   */
  public String getDomainName() {
    return SENSORS_NAME;
  }

  /**
   * default constructor 
   */
  public SensorDomain() {
    super();
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  /**
   * initialize method from GenericStateModelAdapter
   **/
  public void initialize() {
    super.initialize();
    Constants.Role.init();    // Insure that our Role constants are initted
  }

  public void unload() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (uidService != null) {
      sb.releaseService(
          this, UIDService.class, uidService);
      uidService = null;
    }
    super.unload();
  }

  /**
   * load method from GenericStateModelAdapter
   **/
  public void load() {
    super.load();
  }

  protected void loadFactory() {
/* //100
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                                 "Unable to initialize domain Factory without a binding site.");
    } 
*/ //100
    getLoggingService().debug("Sensor domain:: loadfactory");


    //100 setFactory(new SensorFactory(bindingSite.getClusterServesLogicProvider().getLDM()));
    setFactory(new SensorFactory(uidService)); //100
  }

  protected void loadXPlan() {
/* //100
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain XPlan without a binding site.");
    } 

    Collection xPlans = bindingSite.getXPlans();
    LogPlan logPlan = null;
    
    for (Iterator iterator = xPlans.iterator(); iterator.hasNext();) {
      XPlanServesBlackboard  xPlan = (XPlanServesBlackboard) iterator.next();
      if (xPlan instanceof LogPlan) {
        // Note that this means there are 2 paths to the plan.
        // Is this okay?
        logPlan = (LogPlan) xPlan;
        break;
      }
    }
    
    if (logPlan == null) {
      logPlan = new LogPlan();
    }
    
    setXPlan(logPlan);
*/ //100
  }

  protected void loadLPs() {
  }

}








