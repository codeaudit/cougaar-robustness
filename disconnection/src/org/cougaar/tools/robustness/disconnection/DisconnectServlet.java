/*
 * DisconnectServlet.java
 * 
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



/**
 *
 * @author  David Wells - OBJS
 *
 */


package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

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

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;

import org.cougaar.core.servlet.BaseServletComponent;
//import org.cougaar.core.servlet.BlackboardServletSupport;
import org.cougaar.planning.servlet.BlackboardServletComponent;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;


public class DisconnectServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  public static final String DISCONNECT = "Disconnect";
  public static final String RECONNECT = "Reconnect";
  public static final String CHANGE = "change";
  public static final String EXPIRE = "expire";
  
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  private String assetType;
  private String assetID = null;
  
  
  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {

    // get the log
    logger = (LoggingService)
      serviceBroker.getService(
          this, LoggingService.class, null);
    if (logger == null) {
      logger = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      serviceBroker.getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    
    this.assetType = "Agent";  // for now - later may be other kinds of assets
    if (assetType.equals("Agent")) {
        this.assetID = agentIdService.getMessageAddress().toString();
        serviceBroker.releaseService(
            this, AgentIdentificationService.class, agentIdService);
        if (assetID == null) {
          throw new RuntimeException(
              "Unable to obtain agent id");
        }
    }

    // get the blackboard
    this.blackboard = (BlackboardService)
      serviceBroker.getService(
          this,
          BlackboardService.class,
          null);
    if (blackboard == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard service");
    }
    
    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    
    if ((logger != null) && (logger != LoggingService.NULL)) {
      serviceBroker.releaseService(
          this, LoggingService.class, logger);
      logger = LoggingService.NULL;
    }
  }

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
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
              reconnect(request, out);
          } else {
            sendData(out);
          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
      }

      /** User is disconnecting the node */
      private void disconnect(HttpServletRequest request, PrintWriter out) {

          String expire = request.getParameter(EXPIRE);
          //System.out.println("Expire = "+ expire);
          Double d;
          if (expire != null) {
              try {
                  d = new Double(Double.parseDouble(expire));
                  try {
                        blackboard.openTransaction();
                        ReconnectTimeCondition rtc = setReconnectTimeConditionValue(d);
                        if (rtc != null) {
                           out.println("<center><h2>Status Changed - Disconnected</h2></center><br>" );
                           blackboard.publishChange(rtc); 
                           if (logger.isDebugEnabled()) logger.debug("DisconnectServlet published ReconnectTimeCondition: "+rtc.getAsset()+" = "+ rtc.getValue().toString());
                        }
                        else {
                            out.println("<center><h2>Failed to Disconnect - No Condition Objects</h2></center><br>");
                        }
                        blackboard.closeTransaction();
                    } finally {
                        if (blackboard.isTransactionOpen()) blackboard.closeTransactionDontReset();
                    }
              } catch (NumberFormatException nfe) {
                  out.println("<center><h2>Failed to Disconnect - NumberFormatException!</h2></center><br>" );            
              }
          } else {
              out.println("<center><h2>Failed to Disconnect - No Reconnect Time Provided (double).</h2></center><br>" );            
          }
        }

 
      /** User is reconnecting the node */
      private void reconnect(HttpServletRequest request, PrintWriter out) {
          try {
              blackboard.openTransaction();
              ReconnectTimeCondition rtc = setReconnectTimeConditionValue(new Double(0.0));
              if (rtc != null) {
                  out.println("<center><h2>Status Changed - Reconnected</h2></center><br>" );
                  blackboard.publishChange(rtc); 
                  if (logger.isDebugEnabled()) logger.debug("DisconnectServlet published DisconnectCondition: "+rtc.getAsset()+" = "+ rtc.getValue().toString());
                  }
              else {
                  out.println("<center><h2>Failed to Reset</h2></center><br>");
              }
              blackboard.closeTransaction();
          } finally {
                if (blackboard.isTransactionOpen()) blackboard.closeTransactionDontReset();
          }            
      }


      /** publish conditional with new status for this agent */ 
      private DefenseCondition setConditionValue(DefenseConstants.OMCStrBoolPoint value) {
           
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {
                return 
                  (o instanceof DisconnectApplicabilityCondition);
              }
            };
            
            DisconnectApplicabilityCondition cond = null;

            Collection c = blackboard.query(pred);
            if (c.iterator().hasNext()) {
               cond = (DisconnectApplicabilityCondition)c.iterator().next();
               //System.out.println(cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName());
               if (cond.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) 
                    cond.setValue(value); //true = disconnected
            }
            
            return cond;
      }      
        
        
      /** Publish reconnect time */
      private ReconnectTimeCondition setReconnectTimeConditionValue(Double d) {
            
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {  
                return 
                  (o instanceof ReconnectTimeCondition);
              }
            };
            
            ReconnectTimeCondition cond = null;
            Collection c = blackboard.query(pred);
            if (c.iterator().hasNext()) {
               cond = (ReconnectTimeCondition)c.iterator().next();
               //System.out.println(cond.getAssetType()+" "+cond.getAsset()+" " +cond.getDefenseName());
               if (cond.compareSignature(assetType, assetID, DisconnectConstants.DEFENSE_NAME)) {
                    cond.setValue(d); //true = disconnected
                    return cond;
               }
            }

            return null;
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
        out.println("Will Reconnect In <input type=text name="+EXPIRE+"> minutes.");
        out.println("\n</form>");

      }
  }
}
