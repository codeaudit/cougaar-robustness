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

package org.cougaar.tools.robustness.ma;

import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Monitors CommunityStatusModel activity to detect duplicate agents.
 */

public class DuplicateAgentDetector implements StatusChangeListener {

  protected CommunityStatusModel model;
  protected RestartHelper restartHelper;

  protected static Logger logger =
      LoggerFactory.getInstance().createLogger(DuplicateAgentDetector.class);

  protected Map suspectedDuplicates = new HashMap();

  public DuplicateAgentDetector(CommunityStatusModel csm, RestartHelper rh) {
    model = csm;
    model.addChangeListener(this);
    restartHelper = rh;
    if (logger.isDebugEnabled()) {
      logger.debug("DuplicateAgentDetector started");
    }
  }

  public void statusChanged(CommunityStatusChangeEvent[] csce) {
    for(int i = 0; i < csce.length; i++) {
      if(csce[i].locationChanged()) {
        locationChanged(csce[i].getName(),
                        csce[i].getPriorLocation(),
                        csce[i].getCurrentLocation());
      }
    }
  }

  protected void locationChanged(String agent, String priorLoc, String newLoc) {
    if (priorLoc != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("locationChanged:" +
                     " agent=" + agent +
                     " prior=" + priorLoc +
                     " new=" + newLoc);
      }
      if (suspectedDuplicates.containsKey(agent)) {
        Map priorLocations = (Map)suspectedDuplicates.get(agent);
        if (!priorLocations.containsKey(priorLoc)) {
          priorLocations.put(priorLoc, new Long(model.getVersion(agent)));
        }
        if (priorLocations.containsKey(newLoc)) {
          String agentToRemove = selectAgentToRemove(priorLocations);
          if (logger.isWarnEnabled()) {
            logger.warn("Duplicate agent detected, removing duplicate:" +
                        " agent=" + agent +
                        " locations=" + priorLocations.keySet() +
                        " removingFrom=" + agentToRemove);
          }
          suspectedDuplicates.remove(agent);
          restartHelper.killAgent(agent, agentToRemove, model.getCommunityName());
        }
      } else {
        Map priorLocations = new HashMap();
        priorLocations.put(priorLoc, new Long(model.getVersion(agent)));
        suspectedDuplicates.put(agent, priorLocations);
      }
    }
  }

  protected String selectAgentToRemove(Map locationMap) {
    String location = null;
    long timestamp = 0;
    for (Iterator it = locationMap.entrySet().iterator(); it.hasNext();) {
      Map.Entry me = (Map.Entry)it.next();
      if (location == null || ((Long)me.getValue()).longValue() > timestamp) {
        location = (String)me.getKey();
        timestamp = ((Long)me.getValue()).longValue();
      }
    }
    return location;
  }

}
