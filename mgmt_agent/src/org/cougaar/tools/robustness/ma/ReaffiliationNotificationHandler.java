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
import org.cougaar.core.service.EventService;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;

import org.cougaar.tools.robustness.ma.RestartManagerConstants;

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

  public static final int LEAVE = 0;
  public static final int JOIN = 1;

  private ThreatAlertService threatAlertService;
  private NodeControlService nodeControlService;
  private EventService eventService;

  private static int leaveCtr;
  private static int joinCtr;
  private static String oldCommunity;
  private static String newCommunity;

  public ReaffiliationNotificationHandler(BindingSite          bs,
                                          MessageAddress       agentId) {
    super(bs, agentId, null, null);
  }

  /**
   * Get Threat Alert service.
   */
  public ThreatAlertService getThreatAlertService() {
    if (threatAlertService == null) {
      threatAlertService =
      (ThreatAlertService)bindingSite.getServiceBroker().getService(this, ThreatAlertService.class, null);
    }
    return threatAlertService;
  }

  public EventService getEventService() {
    if (eventService == null) {
      eventService =
      (EventService)bindingSite.getServiceBroker().getService(this, EventService.class, null);
    }
    return eventService;
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
      final Attributes attrs = new BasicAttributes();

      // Leave current Robustness community
      String currentCommunity = rn.getOldCommunity();
      if (currentCommunity != null && currentCommunity.trim().length() > 0) {
        Community comm = commSvc.getCommunity(currentCommunity, null);
        if (comm != null && comm.hasEntity(agentId.toString())) {
          Attributes entityAttrs =
              comm.getEntity(agentId.toString()).getAttributes();
          NamingEnumeration enum = entityAttrs.getAll();
          try {
            while (enum.hasMore()) {
              attrs.put( (Attribute) enum.next());
            }
          } catch (NamingException ne) {}
          commSvc.leaveCommunity(currentCommunity, agentId.toString(),
                                 new CommunityResponseListener() {
            public void getResponse(CommunityResponse resp) {
              int remaining = actionsComplete(LEAVE);
              if (logger.isDebugEnabled()) {
                logger.debug("Leaving community:" +
                             " community=" + resp.getContent() +
                             " resp=" + resp +
                             " remaining=" + remaining);
              }
              if (remaining == 0) {
                String message = "Reaffiliation action complete:" +
                                 " oldCommunity=" + oldCommunity +
                                 " newCommunity=" + newCommunity;
                EventService es = getEventService();
                if (es != null && es.isEventEnabled()) {
                  es.event(message);
                } else if (logger.isInfoEnabled()) {
                  logger.info(message);
                }
              }
            }
          });
          if (logger.isDebugEnabled()) {
            logger.debug("Requesting to leave community: " + currentCommunity);
          }
        } else {
          currentCommunity = null; // Specified current community not found
        }
      } else {
        currentCommunity = null; // Current community not specified
      }
      // Join new Robustness community
      final String communityToJoin = rn.getNewCommunity();
      if (communityToJoin != null && communityToJoin.length() > 0) {
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
        commSvc.joinCommunity(communityToJoin,
                              agentId.toString(),
                              commSvc.AGENT,
                              attrs,
                              true,
                              null,
                              new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            int remaining = actionsComplete(JOIN);
            if (logger.isDebugEnabled()) {
              logger.debug("Joining community:" +
                           " community=" + communityToJoin +
                           " resp=" + resp +
                           " remaining=" + remaining);
            }
            if (remaining == 0) {
              String message = "Reaffiliation action complete:" +
                               " oldCommunity=" + oldCommunity +
                               " newCommunity=" + newCommunity;
              EventService es = getEventService();
              if (es != null && es.isEventEnabled()) {
                es.event(message);
              } else if (logger.isInfoEnabled()) {
                logger.info(message);
              }
            }
          }
        });
        if (logger.isDebugEnabled()) {
          logger.debug("Requesting to join community: " + communityToJoin);
        }
      }
      // If this is a node agent, send Notification to member agents
      Attribute entityType = attrs.get("EntityType");
      if (entityType != null && entityType.contains("Node")) {
        Set agentsToNotify = (currentCommunity != null)
            ? findLocalAgentsInCommunity(currentCommunity)
            : findLocalAgents();
        // Set counters to enable completion message when all member
        // agents have finished required leave and/or join operations
        if (currentCommunity != null) {
          oldCommunity = currentCommunity;
          leaveCtr = agentsToNotify.size();
        }
        if (communityToJoin != null && communityToJoin.trim().length() > 0) {
          newCommunity = communityToJoin;
          joinCtr = agentsToNotify.size();
        }
        sendToMembers(rn, agentsToNotify);
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
   * Propagate notification to member agents on node.
   * @param rn Original notification
   * @param agentsToRemove
   */
  private void sendToMembers (ReaffiliationNotification rn, final Set agents) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing members from community: " + agents);
    }
    ThreatAlertService tas = getThreatAlertService();
    if (tas != null) {
      String thisAgent = agentId.toString();
      for (Iterator it = agents.iterator(); it.hasNext();) {
        String memberName = (String) it.next();
        if (!memberName.equals(thisAgent)) {
          if (logger.isDebugEnabled()) {
            logger.debug("SendAlert: agent=" + memberName);
          }
          ReaffiliationNotification copyForMember =
              new ReaffiliationNotification(agentId,
                                            rn.getOldCommunity(),
                                            rn.getNewCommunity(),
                                            "Agent");
          tas.sendAlert(copyForMember, memberName);
        }
      }
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn("ThreatAlertService is null");
      }
    }
  }

  /**
   * Find agents on local node that are members of specified community.
   * @param communityName String
   * @return Set Name of member agents
   */
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

  /**
   * Find all agents on local node.
   * @return Set  Agent names.
   */
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

  /**
   * Decrement counter for completed leave/join operation.
   * @param operationType int
   * @return remaining number of pending operations
   */
  private synchronized static int actionsComplete(int operationType) {
    if (operationType == LEAVE) {
      --leaveCtr;
    } else {
      --joinCtr;
    }
    return leaveCtr + joinCtr;
  }

  public void unload() {
    super.unload();
    if (nodeControlService != null) {
      bindingSite.getServiceBroker().releaseService(this, NodeControlService.class,
          nodeControlService);
      nodeControlService = null;
    }
    if (threatAlertService != null) {
      bindingSite.getServiceBroker().releaseService(this, ThreatAlertService.class,
          threatAlertService);
      threatAlertService = null;
    }
    if (eventService != null) {
      bindingSite.getServiceBroker().releaseService(this, EventService.class,
          eventService);
      eventService = null;
    }
  }

}
