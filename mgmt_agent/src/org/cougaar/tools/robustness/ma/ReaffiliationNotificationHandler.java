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

import org.cougaar.core.node.NodeControlService;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * Handler to respond to requests to add/remove members to/from robustness
 * community.
 */
public class ReaffiliationNotificationHandler extends RobustnessThreatAlertHandlerBase
  implements RestartManagerConstants {

  private ThreatAlertService threatAlertService;
  private NodeControlService nodeControlService;

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

  public NodeControlService getNodeControlService() {
    if (nodeControlService == null) {
      nodeControlService =
          (NodeControlService) bindingSite.getServiceBroker().getService(this,
          NodeControlService.class, null);
    }
    return nodeControlService;
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      ReaffiliationNotification rn = (ReaffiliationNotification) ta;
      if (logger.isDebugEnabled()) {
        logger.debug("Received new ReaffiliationNotification: " + ta);
      }
      String communityName = null;
      // Leave current robustness community
      Collection communities =
          commSvc.searchCommunity(null,
                                  "(CommunityType=Robustness)",
                                  false,
                                  Community.COMMUNITIES_ONLY,
                                  null);
      final Attributes attrs = new BasicAttributes();
      // Leave current Robustness community
      if (communities != null && !communities.isEmpty()) {
        communityName =
            ((Community) communities.iterator().next()).getName();
        Community comm = commSvc.getCommunity(communityName, null);
        if (comm != null && comm.hasEntity(agentId.toString())) {
          Attributes entityAttrs =
              comm.getEntity(agentId.toString()).getAttributes();
          NamingEnumeration enum = entityAttrs.getAll();
          try {
            while (enum.hasMore()) {
              attrs.put((Attribute)enum.next());
            }
          } catch (NamingException ne) {}
          commSvc.leaveCommunity(communityName, agentId.toString(),
                                 new CommunityResponseListener() {
            public void getResponse(CommunityResponse resp) {
              if (logger.isDebugEnabled()) {
                logger.debug("Leaving community: community=" +
                             resp.getContent() + " resp=" + resp);
              }
            }
          });
          if (logger.isDebugEnabled()) {
            logger.debug("Requesting to leave community: " + communityName);
          }
        }
      }
        // Join new Robustness community
        final String newCommunity = (String) rn.getContent();
        if (newCommunity != null && newCommunity.length() > 0) {
          if (attrs.size() == 0) { // No attributes extracted from prior comm
            attrs.put("EntityType", rn.getEntityType());
            Attribute roleAttr = new BasicAttribute("Role");
            roleAttr.add("Member");
            if (rn.getEntityType().equals("Node")) {
              roleAttr.add("HealthMonitor"); // For Agent liveness defense
              roleAttr.add("DosNode"); // For DOS defense
            }
            attrs.put(roleAttr);
          }
          commSvc.joinCommunity(newCommunity,
                                agentId.toString(),
                                commSvc.AGENT,
                                attrs,
                                true,
                                null,
                                new CommunityResponseListener() {
            public void getResponse(CommunityResponse resp) {
              if (logger.isDebugEnabled()) {
                logger.debug("Joining community: community=" +
                             newCommunity + " resp=" + resp);
              }
            }
          });
          if (logger.isDebugEnabled()) {
            logger.debug("Requesting to join community: " + newCommunity);
          }
        }
        // Send Notification to members
        Attribute entityType = attrs.get("EntityType");
        if (entityType != null && entityType.contains("Node")) {
          Set agents = communityName != null
                        ? findLocalAgentsInCommunity(communityName)
                        : findLocalAgents();
          sendToMembers(rn, agents);
        }
      }
  }

  public void changedAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      if (logger.isDebugEnabled()) {
        logger.debug("Received changed ReaffiliationNotification: " + ta);
      }
    }
  }

  public void removedAlert(ThreatAlert ta) {
    if (ta instanceof ReaffiliationNotification) {
      if (logger.isDebugEnabled()) {
        logger.debug("Received removed ReaffiliationNotification: " + ta);
      }
    }
  }

  /**
   * Propagate notification to members
   * @param nodesToRemove
   */
  private void sendToMembers (ReaffiliationNotification rn, final Set agentsToRemove) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing members from community: " + agentsToRemove);
    }
    final Set toRemove = new HashSet(agentsToRemove);
    if (model != null) {
      model.addChangeListener(new StatusChangeListener() {
        public void statusChanged(CommunityStatusChangeEvent[] csce) {
          for (int i = 0; i < csce.length; i++) {
            if (csce[i].membersRemoved() &&
                toRemove.contains(csce[i].getName())) {
              toRemove.remove(csce[i].getName());
              if (toRemove.isEmpty()) {
                if (logger.isInfoEnabled()) {
                  logger.info("Remove members from community complete: node=" +
                              agentId.toString());
                }
                model.removeChangeListener(this);
              }
            }
          }
        }
      });
    }
    if (threatAlertService == null) {
      setThreatAlertService(
          (ThreatAlertService) bindingSite.getServiceBroker().getService(this,
          ThreatAlertService.class, null));
    }
    if (threatAlertService != null) {
      String thisAgent = agentId.toString();
      for (Iterator it = agentsToRemove.iterator(); it.hasNext();) {
        String memberName = (String) it.next();
        if (!memberName.equals(thisAgent)) {
          if (logger.isDebugEnabled()) {
            logger.debug("SendAlert: agent=" + memberName);
          }
          ReaffiliationNotification copyForMember =
              new ReaffiliationNotification(agentId, rn.getCommunity(), "Agent");
          copyForMember.setContent(rn.getContent());
          threatAlertService.sendAlert(copyForMember, memberName);
        }
      }
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn("ThreatAlertService is null");
      }
    }
  }

  private Set findLocalAgentsInCommunity(String communityName) {
    Set localAgentsInCommunity = new HashSet();
    Community community = commSvc.getCommunity(communityName, null);
    if (community != null) {
      // Agents found using NodeControlService
      Set agents = findLocalAgents();
      for (Iterator it = agents.iterator(); it.hasNext();) {
        String agentName = (String)it.next();
        if (community.hasEntity(agentName)) {
          localAgentsInCommunity.add(agentName);
        }
      }
    }
    return localAgentsInCommunity;
  }

  private Set findLocalAgents() {
    Set names = new HashSet();
    NodeControlService ncs = getNodeControlService();
    if (ncs != null) {
      Set agentAddresses = ncs.getRootContainer().getAgentAddresses();
      for (Iterator it = agentAddresses.iterator(); it.hasNext(); ) {
        names.add(it.next().toString());
      }
    }
    return names;
  }

}
