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

import org.cougaar.tools.robustness.ma.ReaffiliationNotification;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Properties;

import java.io.IOException;
import java.io.PrintWriter;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;

/**
 * Let user choose all conditions of one threat alert and publish it to the blackboard.
 */
public class ReaffiliationServlet extends BaseServletComponent implements BlackboardClient {

  protected LoggingService log;    //logging service
  protected ThreatAlertService threatAlertService;
  protected MessageAddress agentId;

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/reaffiliation";
  }

  /**
   * Load the servlet and get necessary services.
   */
  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    super.load();
  }

  /**
   * Get Threat Alert service.
   */
  public ThreatAlertService getThreatAlertService() {
    if (threatAlertService == null) {
      threatAlertService =
          (ThreatAlertService) serviceBroker.getService(this, ThreatAlertService.class, null);
    }
    return threatAlertService;
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
    if (log != null) {
      serviceBroker.releaseService(this, LoggingService.class, log);
      log = null;
    }
    if (threatAlertService != null) {
      serviceBroker.releaseService(this, ThreatAlertService.class,
                                   threatAlertService);
      threatAlertService = null;
    }
  }

  private class MyServlet extends HttpServlet {
    protected HttpServletRequest request;
    protected PrintWriter out;

    public void doPut(HttpServletRequest req, HttpServletResponse res) throws
        IOException, ServletException {
      doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws
        IOException, ServletException {
      if (log.isDebugEnabled()) {
        log.debug("doGet: " + req.getQueryString());
      }
      this.request = req;
      out = res.getWriter();
      parseParams();
    }

    public void parseParams() throws IOException {
      final Properties props = new Properties();
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
          new ServletUtil.ParamVisitor() {
        public void setParam(String name, String value) {
          props.setProperty(name, value);
        }
      };
      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      if (props.containsKey("community") && props.containsKey("target")) {
        doReaffiliation(props.getProperty("community"),
                        props.getProperty("target"));
      } else {
        log.warn("Incomplete arguments: args=" + props.keySet());
      }
    }

    public void doReaffiliation(String community, String target) {
      if (log.isDebugEnabled()) {
        log.debug("doReaffiliation:" +
                  " community=" + community +
                  " target=" + target);
      }
      ReaffiliationNotification ra =
          new ReaffiliationNotification(agentId, community);
      ThreatAlertService tas = getThreatAlertService();
      if (tas != null) {
        threatAlertService.sendAlert(ra, target);
      } else {
        if (log.isDebugEnabled()) {
          log.warn("Unable to get ThreatAlertService");
        }
      }
    }
  }

  public String getBlackboardClientName() {
    return agentId.toString();
  }

  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

}
