/*
 * <copyright>
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
 * Created on September 12, 2001, 10:55 AM
 */
package org.cougaar.tools.robustness.ma.util;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityChangeEvent;

import java.util.Collection;
import java.util.Observable;

abstract public class CommunityFinder extends Observable implements
    CommunityResponseListener,
    CommunityChangeListener {

  public static final String MANAGER_ATTRIBUTE = "RobustnessManager";
  public static final String COMMUNITY_TYPE = "Robustness";

  private String community;

  protected CommunityService svc;
  protected String agentID;
  protected Logger logger;

  public CommunityFinder(CommunityService svc,
                         MessageAddress agentID) {
    this.svc = svc;
    this.agentID = agentID.getAddress();
    logger =
        Logging.getLogger("org.cougaar.robustness.dos.manager.CommunityFinder");
    logger.debug("CommunityFinder for " + agentID);
    svc.addListener(this);
  }

  abstract public void postQuery();

  public void communityChanged(CommunityChangeEvent e) {
    if (logger.isDebugEnabled())
      logger.debug("CommunityChangeEvent " + e);
    if (e.getType() == CommunityChangeEvent.ADD_COMMUNITY) {
      postQuery();
    }
  }

  private void foundCommunity(String community_found) {
    if (logger.isDebugEnabled())
      logger.debug("Community = " + community_found);
    this.community = community_found;
    setChanged();
    notifyObservers(community);
    clearChanged();
  }

  protected void handleResponse(Object candidate) {
    if (logger.isDebugEnabled())
      logger.debug("Response was candidate" + candidate);
    svc.removeListener(this);
    if (candidate instanceof Community) {
      foundCommunity( ( (Community) candidate).getName());
    } else if (candidate instanceof String) {
      foundCommunity( (String) candidate);
    } else {
      if (logger.isErrorEnabled())
        logger.error("Response was " + candidate +
                     " of class " + candidate.getClass());
    }
  }

  // This is only called in response to a match of the exact
  // communuty type and manager attribute, so there better not be
  // more than one entry.
  protected void handleCollectionResponse(Collection result) {
    int count = result.size();
    if (count > 0) {
      handleResponse(result.iterator().next());
      if (count > 1 && logger.isWarnEnabled())
        logger.warn("say something here");
    } else {
      if (logger.isDebugEnabled())
        logger.debug("CommunityResponse is empty");
    }
  }

  public void getResponse(CommunityResponse response) {
    if (logger.isDebugEnabled())
      logger.debug("CommunityResponse " + response.getStatus());
    if (response.getStatus() == CommunityResponse.SUCCESS) {
      Collection result = (Collection) response.getContent();
      handleCollectionResponse(result);
    }
  }

  public String getCommunityName() {
    if (logger.isDebugEnabled())
      logger.debug("getCommunityName() -> " + community);
    return community;
  }

  public static class ForManager extends CommunityFinder {
    public ForManager(CommunityService svc, MessageAddress agentID) {
      super(svc, agentID);
    }

    public void postQuery() {
      String filter = "(&(CommunityType=" + COMMUNITY_TYPE + ")("
          + MANAGER_ATTRIBUTE + "=" + agentID + "))";
      if (logger.isDebugEnabled())
        logger.debug("Posted Manger Subscription filter=" +
                     filter);
      Collection results = svc.searchCommunity(null, filter, true,
                                               Community.COMMUNITIES_ONLY,
                                               this);
      if (results != null) {
        handleCollectionResponse(results);
      }
    }
  }

  public static class ForMember extends CommunityFinder {
    public ForMember(CommunityService svc, MessageAddress agentID) {
      super(svc, agentID);
    }

    public void postQuery() {
      if (logger.isDebugEnabled())
        logger.debug("Posted Member Subscription");
        // only look in Community Cache and not use the callback
      String[] names = svc.getParentCommunities(true);
      if (names != null && names.length > 0) {
        // If more than one, how do we know which one is the
        // robustness community?
        if (logger.isErrorEnabled() && names.length > 1)
          logger.error("More than one community for " + agentID);
        handleResponse(names[0]);
      } else {
        if (logger.isDebugEnabled())
          logger.debug("getParentCommunities return empty array.");
      }
    }
  }

}
