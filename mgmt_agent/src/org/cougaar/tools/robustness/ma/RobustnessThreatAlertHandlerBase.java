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

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;

import java.util.*;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

/**
 * Base class for robustness ThreatAlert handlers.
 */
public abstract class RobustnessThreatAlertHandlerBase extends ThreatAlertHandlerBase {

  protected CommunityStatusModel model;
  protected RobustnessController controller;
  protected MoveHelper moveHelper;
  protected CommunityService commSvc;

  public RobustnessThreatAlertHandlerBase(BindingSite          bs,
                                          MessageAddress       agentId,
                                          RobustnessController controller,
                                          CommunityStatusModel model) {
    super(bs, agentId);
    this.model = model;
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
    final ServiceBroker sb = bs.getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      commSvc =
          (CommunityService) sb.getService(this, CommunityService.class, null);
    }
    else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(CommunityService.class)) {
            commSvc =
                (CommunityService) sb.getService(this, CommunityService.class, null);
          }
        }
      });
    }
  }

  /**
   * Returns a Set of agent names residing on specified nodes.
   * @param locations  Node names
   * @return           Set of agent names
   */
  protected Set affectedAgents(Set nodes) {
    Set affectedAgents = new HashSet();
    for (Iterator it = nodes.iterator(); it.hasNext();) {
      String node = (String)it.next();
      affectedAgents.addAll(agentsOnNode(node));
    }
    return affectedAgents;
  }

  /**
   * Returns a Set of node names from a set of node and/or host names.
   * @param locations  Set of host and/or node names
   * @return           Node names
   */
  protected Set resolveNodes(Set locations) {
    Set nodes = new HashSet();
    for (Iterator it = locations.iterator(); it.hasNext();) {
      String location = (String)it.next();
      if (model.getType(location) == model.NODE) {
        nodes.add(location);
      } else {  // Host
        String nodeNames[] = model.entitiesAtLocation(location);
        for (int i = 0; i < nodeNames.length; i++) {
          nodes.add(nodeNames[i]);
        }
      }
    }
    return nodes;
  }

  /**
   * Returns a Set of all agents on specified node.
   * @param nodeName
   * @return   Set of agent names
   */
  protected Set agentsOnNode(String nodeName) {
    Set agents = new HashSet();
    String[] agentsOnNode = model.entitiesAtLocation(nodeName);
    for (int i = 0; i < agentsOnNode.length; i++) {
      agents.add(agentsOnNode[i]);
    }
    return agents;
  }

  protected String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

  /**
   * Modify one or more attributes of a community or entity.
   * @param communityName  Affected community
   * @param entityName     Name of entity or null to modify community attributes
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(String communityName, final String entityName, final Attribute[] newAttrs) {
    Community community =
      commSvc.getCommunity(communityName, new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          changeAttributes((Community) resp.getContent(), entityName, newAttrs);
        }
      }
    );
    if (community != null) {
      changeAttributes(community, entityName, newAttrs);
    }
  }

  protected void changeAttributes(Community community, final String entityName, Attribute[] newAttrs) {
    if (community != null) {
      List mods = new ArrayList();
      for (int i = 0; i < newAttrs.length; i++) {
        try {
          Attributes attrs = community.getAttributes();
          Attribute attr = attrs.get(newAttrs[i].getID());
          if (attr == null || !attr.contains(newAttrs[i].get())) {
            int type = attr == null
                ? DirContext.ADD_ATTRIBUTE
                : DirContext.REPLACE_ATTRIBUTE;
            mods.add(new ModificationItem(type, newAttrs[i]));
          }
        } catch (NamingException ne) {
          logger.error("Error setting community attribute:" +
                       " community=" + community.getName() +
                       " attribute=" + newAttrs[i]);
        }
      }
      if (!mods.isEmpty()) {
        commSvc.modifyAttributes(community.getName(),
                            entityName,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            null);
      }
    }
  }

}
