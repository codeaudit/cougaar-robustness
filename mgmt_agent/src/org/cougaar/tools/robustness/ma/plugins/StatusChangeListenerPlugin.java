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
package org.cougaar.tools.robustness.ma.plugins;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Sample plugin demonstrating the use of the CommunityStatusMode to receive
 * notification of changes in a robustness community.
 */
public class StatusChangeListenerPlugin extends ComponentPlugin {

  private LoggingService logger;

  private StatusChangeListener myChangeListener = new StatusChangeListener() {
    public void statusChanged(CommunityStatusChangeEvent[] csce) {
      for (int i = 0; i < csce.length; i++) {
        if (csce[i].locationChanged() && csce[i].getPriorLocation() != null) {
          if (logger.isInfoEnabled()) {
            logger.info("Agent location changed: agent=" + csce[i].getName() +
                        " priorLocation=" + csce[i].getPriorLocation() +
                        " newLocation=" + csce[i].getCurrentLocation());
          }
        }
      }
    }
  };

  public void setupSubscriptions() {
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");

    // Subscribe to CommunityStatusModel
    communityStatusModelSub =
        (IncrementalSubscription)blackboard.subscribe(communityStatusModelPredicate);

  }

  public void execute() {
    Collection models = communityStatusModelSub.getAddedCollection();
    for (Iterator it = models.iterator(); it.hasNext();) {
      CommunityStatusModel csm = (CommunityStatusModel)it.next();
      if (logger.isInfoEnabled()) {
        logger.info("Found CommunityStatusModel: community=" +
                    csm.getCommunityName());
      }
      csm.addChangeListener(myChangeListener);
    }
  }

  private IncrementalSubscription communityStatusModelSub;
  private UnaryPredicate communityStatusModelPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof CommunityStatusModel);
  }};
}
