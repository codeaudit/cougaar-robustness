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
package org.cougaar.tools.robustness.ma.util;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.*;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

/**
 * Utility servlet to modify a community attribute.
 */
public class CommunityAttributeServlet extends BaseServletComponent implements BlackboardClient{

  private LoggingService logger;
  private CommunityService cs;
  private MessageAddress agentId;

  protected String getPath() {
    return "/modcommattr";
  }

  /**
   * Load the servlet and get necessary services.
   */
  public void load() {
    // get the logging service
    logger =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    super.load();
  }

  public void setCommunityService(CommunityService cs) {
    this.cs = cs;
  }

  /**
   * Create the servlet.
   */
  protected Servlet createServlet() {
    AgentIdentificationService ais = (AgentIdentificationService)serviceBroker.getService(
        this, AgentIdentificationService.class, null);
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
      serviceBroker.releaseService(this, AgentIdentificationService.class, ais);
    }

    return new MyServlet();
  }

  /**
   * Release the serlvet.
   */
  public void unload() {
    super.unload();
    if (cs != null) {
      serviceBroker.releaseService(this, CommunityService.class, cs);
      cs = null;
    }
  }

  private class MyServlet extends HttpServlet {
    PrintWriter out;
    String communityName = null;
    String attrId = null;
    String attrValue = null;

    public void doPut(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
      parseParams(req);
      modifyCommunityAttribute(communityName, attrId, attrValue);
    }

    private void parseParams(HttpServletRequest req) throws IOException {
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
         public void setParam(String name, String value) {
           if(name.equals("community")) {
             communityName = value;
           }
           if(name.equals("id")) {
             attrId = value;
           }
           if(name.equals("value")) {
             attrValue = value;
           }
         }
      };
      // visit the URL parameters
      ServletUtil.parseParams(vis, req);
    }
  }

  /**
   * Modify specified community attribute.  Attribute is created if it doesn't
   * exist.
   * @param communityName Name of affected community
   * @param attrId        ID of attribute to modify
   * @param attrValue     New value
   */
  protected void modifyCommunityAttribute(String communityName,
                                          String attrId,
                                          String attrValue) {
    String cname = communityName;
    if (cname == null) {
      // If community name isn't supplied use Robustness Community
      cname = getRobustnessCommunityName();
    }
    logger.info("modifyCommunityAttributes:" +
                 " communityName=" + cname +
                 " attrId=" + attrId +
                 " attrValue=" + attrValue);
    if (cname != null && attrId != null && attrValue != null) {
      changeAttributes(cname,
                       new Attribute[] {new BasicAttribute(attrId, attrValue)});
    }

  }

  /**
   * Find name of this agents robustness community.
   */
  protected String getRobustnessCommunityName() {
    String communityName = null;
    Collection parentNames = cs.listParentCommunities(agentId.toString());
    for (Iterator it = parentNames.iterator(); it.hasNext();) {
      String parentName = (String)it.next();
      Community community = cs.getCommunity(parentName, null);
      if (community != null) {
        Attributes allAttrs = community.getAttributes();
        Attribute commTypeAttr = allAttrs.get("CommunityType");
        if (commTypeAttr != null && commTypeAttr.contains("Robustness")) {
          communityName = parentName;
          break;
        }
      }
    }
    return communityName;
  }


  /**
   * Modify one or more attributes of a community or entity.
   * @param community      Target community
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(final String communityName, Attribute[] newAttrs) {
    Community community = cs.getCommunity(communityName, null);
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
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              logger.warn("Unexpected status from CommunityService modifyAttributes request:" +
                          " status=" + resp.getStatusAsString() +
                          " community=" + communityName);
            }
          }
      };
        cs.modifyAttributes(communityName,
                            null,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            crl);
      }
    }
  }

  public String getBlackboardClientName() { return toString(); }

  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  public boolean triggerEvent(Object event) {
    throw new UnsupportedOperationException(
      this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

}
