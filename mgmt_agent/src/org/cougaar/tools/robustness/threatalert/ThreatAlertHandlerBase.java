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
package org.cougaar.tools.robustness.threatalert;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import java.util.List;
import java.util.ArrayList;

/**
 * Base class for handling ThreatAlerts.  A listener is added to the
 * ThreatAlertService.  Classes extending this must implement the "newAlert" and
 * "expiredAlert" abstract methods to receive alert notifications.
 */
public abstract class ThreatAlertHandlerBase implements ThreatAlertListener {

  protected LoggingService logger;
  protected BindingSite bindingSite;
  protected MessageAddress agentId;
  protected CommunityService commSvc;

  public ThreatAlertHandlerBase(BindingSite    bs,
                                 MessageAddress agentId) {
    this.bindingSite = bs;
    this.agentId = agentId;
    final ServiceBroker sb = bindingSite.getServiceBroker();
    logger =
      (LoggingService)sb.getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    if (sb.hasService(ThreatAlertService.class)) {
      initThreatAlertListener();
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(ThreatAlertService.class)) {
            initThreatAlertListener();
          }
        }
      });
    }
    if (sb.hasService(CommunityService.class)) {
      commSvc =
          (CommunityService) sb.getService(this, CommunityService.class, null);
    } else {
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

  private void initThreatAlertListener() {
    if (logger.isDebugEnabled()) {
      logger.debug("Initializing ThreatAlertListener");
    }
    ServiceBroker sb = bindingSite.getServiceBroker();
    ThreatAlertService tas =
        (ThreatAlertService) sb.getService(this, ThreatAlertService.class, null);
    tas.addListener(this);
  }

  public abstract void newAlert(ThreatAlert ta);
  public abstract void changedAlert(ThreatAlert ta);
  public abstract void removedAlert(ThreatAlert ta);

  /**
   * Modify one or more attributes of a community or entity.
   * @param communityName  Target community
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

  /**
   * Modify one or more attributes of a community or entity.
   * @param community      Target community
   * @param entityName     Name of entity or null to modify community attributes
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(final Community community, final String entityName, Attribute[] newAttrs) {
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
          if (logger.isErrorEnabled()) {
            logger.error("Error setting community attribute:" +
                         " community=" + community.getName() +
                         " attribute=" + newAttrs[i]);
          }
        }
      }
      if (!mods.isEmpty()) {
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              if (logger.isWarnEnabled()) {
                logger.warn(
                    "Unexpected status from CommunityService modifyAttributes request:" +
                    " status=" + resp.getStatusAsString() +
                    " community=" + community.getName());
              }
            }
          }
      };
        commSvc.modifyAttributes(community.getName(),
                            entityName,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            crl);
      }
    }
  }

  public void unload() {
  if (logger != null) {
    bindingSite.getServiceBroker().releaseService(this, LoggingService.class,
        logger);
    logger = null;
  }
  if (commSvc != null) {
    bindingSite.getServiceBroker().releaseService(this, CommunityService.class,
        commSvc);
    commSvc = null;
  }
}

}
