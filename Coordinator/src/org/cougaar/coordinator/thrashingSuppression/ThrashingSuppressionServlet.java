/*
 * ThrashingSuppressionServlet.java
 *
 * Created on September 29, 2003, 2:58 PM
 */

package org.cougaar.coordinator.thrashingSuppression;

/*
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


import org.cougaar.coordinator.*;

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
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.planning.servlet.BlackboardServletComponent;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;

import org.cougaar.util.UnaryPredicate;


public class ThrashingSuppressionServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  public static final String SUPPRESS = "SuppressDefenses";
  public static final String ALLOW = "AllowDefenses";
  public static final String CHANGE = "change";
  public static final String EXPIRE = "expire";
  
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  
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
    return "/ThrashingSuppressionDefense";
  }  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String suppress = request.getParameter(SUPPRESS);
        String allow = request.getParameter(ALLOW);
        response.setContentType("text/html");

        try {
          PrintWriter out = response.getWriter();

          if (suppress != null) {
              sendData(out);
              suppress(request, out);
          } else if (allow != null) {
              sendData(out);
              allow(out);
          } else {
            sendData(out);
          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
      }

      /** Asking to Suppress Defenses */
      private void suppress(HttpServletRequest request, PrintWriter out) {

          //get the Condition object
          UnaryPredicate pred = new UnaryPredicate() {
             public boolean execute(Object o) {  
                return 
                    (o instanceof ThrashingSuppressionApplicabilityCondition);
             }
          };

          blackboard.openTransaction();
          ThrashingSuppressionApplicabilityCondition tsac = null;
          Collection c = blackboard.query(pred);
          Iterator iter = c.iterator();
          if (iter.hasNext()) {
             tsac = (ThrashingSuppressionApplicabilityCondition)iter.next();
          }        
          if (tsac != null) {
              tsac.setValue(DefenseConstants.BOOL_TRUE);
              out.println("<center><h2>Status Changed - Defense Suppression Requested</h2></center><br>" );
              blackboard.publishChange(tsac); 
              if (logger.isDebugEnabled()) logger.debug("Status Changed - Defense Suppression Requested");
          }
          blackboard.closeTransaction();

    } 

    
      /** Asking to Allow Defenses */
      private void allow(PrintWriter out) {

          //get the Condition object
          UnaryPredicate pred = new UnaryPredicate() {
             public boolean execute(Object o) {  
                return 
                    (o instanceof ThrashingSuppressionApplicabilityCondition);
             }
          };

          try {
              blackboard.openTransaction();
              ThrashingSuppressionApplicabilityCondition tsac = null;
              Collection c = blackboard.query(pred);
              Iterator iter = c.iterator();
              if (iter.hasNext()) {
                 tsac = (ThrashingSuppressionApplicabilityCondition)iter.next();
              }

              if (tsac != null) {
                  tsac.setValue(DefenseConstants.BOOL_FALSE);
                  out.println("<center><h2>Status Changed - Defenses Allowed Requested</h2></center><br>" );
                  blackboard.publishChange(tsac); 
                  if (logger.isDebugEnabled()) logger.debug("Status Changed - Defenses Allowed Requested");
              blackboard.closeTransaction();
              }
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

        out.print("<h2><center>ThrashingSuppressionServlet");
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"SuppressDefenses\" value=\"Suppress\"><br>");
        out.println("<input type=submit name=\"AllowDefenses\" value=\"Allow\"><br>");
        out.println("\n</form>");

      }
  }
}
