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
package org.cougaar.tools.robustness.ma;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;

import org.cougaar.tools.robustness.ma.RestartManagerConstants;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

/**
 * Handler to respond to requests to add/remove members to/from robustness
 * community.
 */
public class ReaffiliationNotificationHandler extends RobustnessThreatAlertHandlerBase
  implements RestartManagerConstants {

  private ThreatAlertService threatAlertService;

  public ReaffiliationNotificationHandler(BindingSite          bs,
                                          MessageAddress       agentId,
                                          CommunityStatusModel model) {
    super(bs, agentId, null, model);
  }

  /**
   * Add Threat Alert service.
   */
  public void setThreatAlertService(ThreatAlertService tas) {
    this.threatAlertService = tas;
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      ReaffiliationNotification rn = (ReaffiliationNotification)ta;
      if (logger.isInfoEnabled()) {
        logger.info("Received new ReaffiliationNotification: " + ta);
      }
      if (model != null && agentId.toString().equals(preferredLeader()) && ta.isActive()) {
        sendToMembers(rn, getAffectedNodes(ta.getAffectedAssets()));
      } else {
        // Leave current robustness community
        Collection communities =
            commSvc.searchCommunity(null,
                                    "(CommunityType=Robustness)",
                                    false,
                                    Community.COMMUNITIES_ONLY,
                                    null);
        if (communities != null && !communities.isEmpty()) {
          String communityName = ((Community)communities.iterator().next()).getName();
          commSvc.leaveCommunity(communityName,
                                 agentId.toString(),
                                 new CommunityResponseListener(){
                                   public void getResponse(CommunityResponse resp) {}
          });
          if (logger.isInfoEnabled()) {
            logger.info("Leaving community: " + communityName);
          }
        }
        // Join new robustness community
        String newCommunity = (String)rn.getContent();
        Attributes attrs = new BasicAttributes();
        attrs.put("EntityType", "Agent");
        attrs.put("Role", "Agent");
        if (newCommunity != null && newCommunity.length() > 0) {
          commSvc.joinCommunity(newCommunity,
          agentId.toString(),
          commSvc.AGENT,
          attrs,
          true,
          null,
          new CommunityResponseListener(){
            public void getResponse(CommunityResponse resp) {}
          });
          if (logger.isInfoEnabled()) {
            logger.info("Joining community: " + newCommunity);
          }
        }
      }
    }
  }

  public void changedAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      if (logger.isInfoEnabled()) {
        logger.info("Received changed ReaffiliationNotification: " + ta);
      }
    }
  }

  public void removedAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      if (logger.isInfoEnabled()) {
        logger.info("Received removed ReaffiliationNotification: " + ta);
      }
    }
  }

  /**
   * Propagate notification to members
   * @param nodesToRemove
   */
  private void sendToMembers (ReaffiliationNotification rn, final Set nodesToRemove) {
    final Set membersToRemove = affectedAgents(nodesToRemove);
    membersToRemove.addAll(nodesToRemove);
    Set toRemove = new HashSet(membersToRemove);
    if (logger.isInfoEnabled()) {
      logger.info("Removing members from community: " + membersToRemove);
    }
    model.addChangeListener(new StatusChangeListener() {
      public void statusChanged(CommunityStatusChangeEvent[] csce) {
        for (int i = 0; i < csce.length; i++) {
          if (csce[i].membersRemoved() &&
              membersToRemove.contains(csce[i].getName())) {
            membersToRemove.remove(csce[i].getName());
            if (membersToRemove.isEmpty()) {
              if (logger.isInfoEnabled()) {
                logger.info("Remove members from community complete: nodes=" + membersToRemove);
              }
              model.removeChangeListener(this);
            }
          }
        }
      }
    });
    if (threatAlertService == null) {
      setThreatAlertService(
          (ThreatAlertService) bindingSite.getServiceBroker().getService(this,
          ThreatAlertService.class, null));
    }
    if (threatAlertService != null) {
      for (Iterator it = toRemove.iterator(); it.hasNext(); ) {
        String memberName = (String) it.next();
        //commSvc.leaveCommunity(model.getCommunityName(), memberName, null);
        if (logger.isInfoEnabled()) {
          logger.info("SendAlert: agent=" + memberName);
        }
        ReaffiliationNotification copyForMember =
            new ReaffiliationNotification(rn.getSeverityLevel(),
                                          rn.getStartTime(),
                                          rn.getExpirationTime(),
                                          rn.getCommunity());
        copyForMember.setContent(rn.getContent());
        threatAlertService.sendAlert(copyForMember, memberName);
      }
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("ThreatAlertService is null");
      }
    }
  }

  protected Set getAffectedNodes(Asset[] affectedAssets) {
    Set nodesAndHosts = new HashSet();
    for (int i = 0; i < affectedAssets.length; i++) {
      String type = affectedAssets[i].getAssetType();
      String id = affectedAssets[i].getAssetIdentifier();
      if (type != null &&
          (type.equalsIgnoreCase("Node") || type.equalsIgnoreCase("Host"))) {
        nodesAndHosts.add(id);
      }
    }
    if (!nodesAndHosts.isEmpty()) {
      return resolveNodes(nodesAndHosts);
    } else {
      return Collections.EMPTY_SET;
    }
  }

  protected Set getExcludedNodes() {
    Set excludedNodes = new HashSet();
    String allNodes[] = model.listEntries(model.NODE);
    for (int i = 0; i < allNodes.length; i++) {
      if (model.hasAttribute(model.getAttributes(allNodes[i]), "UseForRestarts", "False")) {
        excludedNodes.add(allNodes[i]);
      }
    }
    return excludedNodes;
  }

}
