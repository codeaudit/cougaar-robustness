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
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.FindCommunityCallback;

import org.cougaar.tools.robustness.ma.RestartManagerConstants;

import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

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
      oldCommunity = rn.getOldCommunity();
      newCommunity = rn.getNewCommunity();
      String entityType = rn.getEntityType();
      if (oldCommunity != null) {
        leaveCommunity(oldCommunity, rn.getTimeout());
      } else {
        if (newCommunity != null) {
          joinCommunity(newCommunity, entityType, rn.getTimeout());
        }
      }
      // If this is a node agent, send Notification to member agents
      if (entityType != null && entityType.equals("Node")) {
        Set agentsToNotify = (oldCommunity != null)
            ? findLocalAgentsInCommunity(oldCommunity)
            : findLocalAgents();
        // Set counters to enable completion message when all member
        // agents have finished required leave and/or join operations
        leaveCtr = oldCommunity != null ? agentsToNotify.size() : 0;
        joinCtr = newCommunity != null ? agentsToNotify.size() : 0;
        sendToMembers(rn, agentsToNotify);
      }
    }
  }

  // Leave current Robustness community
  protected void leaveCommunity(String community, final long timeout) {
    if (community != null && community.trim().length() > 0) {
      Community comm = commSvc.getCommunity(community, null);
      if (comm != null && comm.hasEntity(agentId.toString())) {
        final Attributes attrs =
            comm.getEntity(agentId.toString()).getAttributes();
        Attribute attr = attrs.get("EntityType");
        final String entityType =
            (attr != null && attr.contains("Node")) ? "Node" : "Agent";
        commSvc.leaveCommunity(community, agentId.toString(),
                               new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            checkCompletion(LEAVE, resp);
            if (newCommunity != null && resp.getStatus() == CommunityResponse.SUCCESS) {
              joinCommunity(newCommunity, entityType, attrs, timeout);
            }
          }
        });
        /* Use the following to perform a LEAVE with a timeout, requires Cougaar 11.4
        commSvc.leaveCommunity(community, agentId.toString(),
                               timeout,
                               new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            checkCompletion(LEAVE, resp);
            if (newCommunity != null && resp.getStatus() == CommunityResponse.SUCCESS) {
              joinCommunity(newCommunity, entityType, attrs, timeout);
            }
          }
        });*/
        if (logger.isDetailEnabled()) {
          logger.detail("Requesting to leave community: " + community);
        }
      } else {
        community = null; // Specified current community not found
      }
    } else {
      community = null; // Current community not specified
    }
  }

  // Join new Robustness community
  protected void joinCommunity(String community, String entityType, long timeout) {
    Attributes attrs = new BasicAttributes();
    joinCommunity(community, entityType, attrs, timeout);
  }

  // Join new Robustness community
  protected void joinCommunity(final String community,
                               final String entityType,
                               final Attributes attrs,
                               long timeout) {
    if (logger.isDebugEnabled()) {
      logger.debug("joinCommunity:" +
                   " community=" + community +
                   " entityType=" + entityType +
                   //" attrs=" + attrs +
                   " timeout=" + timeout);
    }
    if (community != null && community.length() > 0) {
      if (attrs.size() == 0) {
        attrs.put("EntityType", entityType);
        Attribute roleAttr = new BasicAttribute("Role");
        roleAttr.add("Member");
        if (entityType.equals("Node")) {
          roleAttr.add("HealthMonitor"); // For Agent liveness defense
          roleAttr.add("DosNode"); // For DOS defense
        }
        attrs.put(roleAttr);
      }
      commSvc.findCommunity(community, new FindCommunityCallback() {
        public void execute(String name) {
          if (name == null) { //Timed out
            if (logger.isWarnEnabled()) {
              logger.warn("Join not successful, re-joining original community," +
                          " community=" + oldCommunity);
            }
            joinCommunity(oldCommunity, entityType, attrs, -1);
          } else {
            commSvc.joinCommunity(community,
                                  agentId.toString(),
                                  CommunityService.AGENT,
                                  attrs,
                                  false,
                                  null,
                                  new CommunityResponseListener() {
                public void getResponse(CommunityResponse resp) {
                  checkCompletion(JOIN, resp);
                }
              });
        }
      }}, timeout);
      /* Alternate mechanism for handling timeout, relies on new community
         service methods in Cougaar 11.4
      commSvc.joinCommunity(community,
                            agentId.toString(),
                            commSvc.AGENT,
                            attrs,
                            false,
                            null,
                            timeout,
                            new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          checkCompletion(JOIN, resp);
          if (resp.getStatus() != CommunityResponse.SUCCESS) {
            if (logger.isWarnEnabled()) {
              logger.warn("Join not successful, re-joining original community," +
                          " community=" + oldCommunity);
            }
            joinCommunity(oldCommunity, entityType, attrs, -1);
          }
        }
      });*/
      if (logger.isDetailEnabled()) {
        logger.detail("Requesting to join community: " + community);
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
                                            rn.getTimeout(),
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

  protected void checkCompletion(int action,
                                 CommunityResponse resp) {
    if (resp.getStatus() == CommunityResponse.SUCCESS) {
      int remaining = actionsComplete(action);
      if (logger.isDebugEnabled()) {
        if (action == JOIN) {
          logger.debug("Joining community:" +
                       " community=" + newCommunity +
                       " resp=" + resp +
                       " remaining=" + remaining);
        } else {
          logger.debug("Leaving community:" +
                       " community=" + oldCommunity +
                       " resp=" + resp +
                       " remaining=" + remaining);
        }
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
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Community request not completed," +
                    " status=" + resp.getStatusAsString() +
                    " action=" + (action == JOIN ? "JOIN" : "LEAVE") +
                    " oldCommunity=" + oldCommunity +
                    " newCommunity=" + newCommunity);
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
