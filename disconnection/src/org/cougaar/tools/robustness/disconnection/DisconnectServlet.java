/*
 * DisconnectServlet.java
 * 
 * @author David Wells - OBJS
 *
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
 */

 
package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes.*;

import java.lang.reflect.Constructor;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;

import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.ComponentServlet;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.planning.servlet.BlackboardServletComponent;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.core.service.EventService;


public class DisconnectServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  public static final String DISCONNECT = "Disconnect";
  public static final String RECONNECT = "Reconnect";
  public static final String CHANGE = "change";
  public static final String EXPIRE = "expire";
  
  private BlackboardService blackboard = null;
  private Logger logger = null;
  private EventService eventService = null;
  
  private String assetType = "Node";
  private String assetID = null;
  private long lastRequestTime = 0;
  
  
  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {

    // get the log
    logger = 
        (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    if (logger == null) {
      logger = LoggingService.NULL;
    }
    
    // get the blackboard
    blackboard = 
        (BlackboardService) serviceBroker.getService(this, BlackboardService.class, null);
    if (blackboard == null) {
      throw new RuntimeException("Unable to obtain blackboard service");
    }
    
    // get the agentId
    if (assetType.equals("Node")) {
      NodeIdentificationService nodeIdService = 
          (NodeIdentificationService) serviceBroker.getService(this, NodeIdentificationService.class, null);
      if (nodeIdService == null) {
          throw new RuntimeException("Unable to obtain node-id service");
      } 
      assetID = nodeIdService.getMessageAddress().toString();
      serviceBroker.releaseService(this, NodeIdentificationService.class, nodeIdService);
      if (assetID == null) {
          throw new RuntimeException("Unable to obtain node id");
      }
      
      // get the EventService
      this.eventService = (EventService)
          serviceBroker.getService(this, EventService.class, null);
      if (eventService == null) {
          throw new RuntimeException("Unable to obtain EventService");
      }
    }
    
    if (eventService.isEventEnabled()) eventService.event("Loaded DisconnectServlet");

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    
    if ((logger != null) && (logger != LoggingService.NULL)) {
      serviceBroker.releaseService(this, LoggingService.class, logger);
      logger = LoggingService.NULL;
    }
  }

  // odd BlackboardClient method:
  public java.lang.String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }


  public String toString() {
    return "\""+getPath()+"\" servlet";
  }
  
  protected String getPath() {
    return "/Disconnect";
  }  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String discon = request.getParameter(DISCONNECT);
        String recon = request.getParameter(RECONNECT);
        response.setContentType("text/html");

        try {
          PrintWriter out = response.getWriter();

          if (discon != null) {
              sendData(out);
              disconnect(request, out);
          } else if (recon != null) {
              sendData(out);
              reconnect(out);
          } else {
            sendData(out);
          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
      }

      /** User is disconnecting the node */
      private void disconnect(HttpServletRequest request, PrintWriter out) {
      
          if ( (System.currentTimeMillis() - lastRequestTime) < 120000)  {
                out.println("<center><h2>Failed to Disconnect - Requests must be at least 2 minutes apart. </h2></center><br>");
                if (logger.isErrorEnabled()) logger.error("Failed to Disconnect - Requests must be at least 2 minutes apart.");
                if (eventService.isEventEnabled()) {
                    eventService.event("Failed to Disconnect - Requests must be at least 2 minutes apart.");
                }
                return;
          }
          lastRequestTime = System.currentTimeMillis();

          String expire = request.getParameter(EXPIRE);
          Double d;
          if (expire != null) {
              try {
                  double t = Double.parseDouble(expire);
                  t = t>120L ? t : 120.0;
                  d = new Double(t*1000.0);                  
                  try {
                        blackboard.openTransaction();
                        LocalReconnectTimeCondition lrtc = LocalReconnectTimeCondition.findOnBlackboard(assetType, assetID, blackboard);
                        if (lrtc != null) {
                           lrtc.setTime(d);
                           out.println("<center><h2>Status Changed - Disconnect Requested</h2></center><br>" );
                           blackboard.publishChange(lrtc); 
                           if (logger.isDebugEnabled()) logger.debug("Disconnect Requested: "+lrtc.getAsset()+" = "+lrtc.getValue().toString());
                        }
                        else {
                            out.println("<center><h2>Failed to Disconnect - Defense not initialized: can't find ManagementAgent address</h2></center><br>");
                            if (logger.isErrorEnabled()) logger.error("Failed to Disconnect - Defense not initialized: can't find ManagementAgent address");
                            if (eventService.isEventEnabled()) {
                                eventService.event("Defense not initialized");
                            }
                        }
                        blackboard.closeTransaction();
                    } finally {
                        if (blackboard.isTransactionOpen()) blackboard.closeTransactionDontReset();
                    }
              } catch (NumberFormatException nfe) {
                  out.println("<center><h2>Failed to Disconnect - NumberFormatException!</h2></center><br>" );            
                  if (logger.isErrorEnabled()) logger.error("Failed to Disconnect - NumberFormatException!");
              }
          } 
        }

 
      /** User is reconnecting the node */
      private void reconnect(PrintWriter out) {

           if ( (System.currentTimeMillis() - lastRequestTime) < 120000)  {
                out.println("<center><h2>Failed to Connect - Requests must be at least 2 minutes apart. </h2></center><br>");
                if (logger.isErrorEnabled()) logger.error("Failed to Connect - Requests must be at least 2 minutes apart.");
                if (eventService.isEventEnabled()) {
                    eventService.event("Failed to Connect - Requests must be at least 2 minutes apart.");
                }
                return;
          }
          lastRequestTime = System.currentTimeMillis();

          try {
              blackboard.openTransaction();
              LocalReconnectTimeCondition lrtc = LocalReconnectTimeCondition.findOnBlackboard(assetType, assetID, blackboard);
              if (lrtc != null) {
                  lrtc.setTime(new Double(0.0));
                  out.println("<center><h2>Status Changed - Reconnect Requested</h2></center><br>" );
                  blackboard.publishChange(lrtc); 
                  if (logger.isDebugEnabled()) logger.debug("Reconnect Request: "+lrtc.getAsset()+" = "+lrtc.getValue().toString());
                  }
              else {
                  out.println("<center><h2>Failed to Reset</h2></center><br>");
              }
              blackboard.closeTransaction();
          } finally {
                if (blackboard.isTransactionOpen()) blackboard.closeTransactionDontReset();
          }            
      }   
        

      /**
       * Output page with disconnect  / reconnect button & reconnect time slot
       */
      private void sendData(PrintWriter out) {
        out.println("<html><head></head><body>");

        writeButtons(out);

      }

      private void writeButtons(PrintWriter out) {

        out.print("<h2><center>PlannedDisconnectServlet for ");
        out.print(assetID.toString());
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"Disconnect\" value=\"Disconnect\"><br>");
        out.println("<input type=submit name=\"Reconnect\" value=\"Reconnect\"><br>");
        out.println("Will Reconnect In <input type=text name="+EXPIRE+"> seconds.");
        out.println("\n</form>");

      }
  }
}
