/*
 * MAUPolicyServlet.java
 *
 * Created on September 30, 2004, 10:12 AM
 */

/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.policy;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;

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
import org.cougaar.coordinator.costBenefit.CostBenefitKnob;
import org.cougaar.coordinator.costBenefit.BadWeightsException;

public class MAUPolicyServlet extends BaseServletComponent implements BlackboardClient
 {

    // Predefined Policies
    public static final String HIGH_SECURITY = "HighSecurity";
    public static final String NORMAL = "Normal";
    public static final String HIGH_COMPLETENESS = "HighCompleteness";

    private BlackboardService blackboard = null;
    private Logger logger = null;
  
  
  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {

    super.load();

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
    
    if(logger.isDebugEnabled()) logger.debug("MAUPolicyServlet Loaded");
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


  public String toString() {
    return "\""+getPath()+"\" servlet";
  }
  
  protected String getPath() {
    return "/MAUPolicy";
  }  

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }


  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String highSecurity = request.getParameter(HIGH_SECURITY);
        String normal = request.getParameter(NORMAL);
        String highCompleteness = request.getParameter(HIGH_COMPLETENESS);
        response.setContentType("text/html");
        CostBenefitKnob knob = null;

        blackboard.openTransaction();
        Collection c = blackboard.query(CostBenefitKnob.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
            knob = (CostBenefitKnob)iter.next();
        }

        try {
            PrintWriter out = response.getWriter();
            sendData(out);
            if (knob != null) {
                try {
                    if (highSecurity != null) {
                        knob.setWeights(0.9, 0.1, 0.0);
                        out.println("<center><h2>Status Changed - MAU set to High Security</h2></center><br>" );
                        blackboard.publishChange(knob); 
                        if (logger.isInfoEnabled()) logger.info("Status Changed - MAU set to High Security");
                    }
                    else if (normal != null) {
                        knob.setWeights(0.4, 0.6, 0.0);
                        out.println("<center><h2>Status Changed - MAU set to Normal</h2></center><br>" );
                        blackboard.publishChange(knob); 
                        if (logger.isInfoEnabled()) logger.info("Status Changed - MAU set to Normal");
                    }
                    else if (highCompleteness != null) {
                        knob.setWeights(0.1, 0.9, 0.0);
                        out.println("<center><h2>Status Changed - MAU set to HighCompleteness</h2></center><br>" );
                        blackboard.publishChange(knob); 
                        if (logger.isInfoEnabled()) logger.info("Status Changed - MAU set to HighCompleteness");
                    }
                }
                catch (BadWeightsException e) {
                    logger.error(e.toString());
                }
            }
            else {
                out.println("<center><h2>Status Unchanged - CostBenefitKnob Not Found</h2></center><br>" );
            }
            out.close();
        }
        catch (java.io.IOException e) { 
            e.printStackTrace(); 
        }
        blackboard.closeTransaction();
    }



      /**
       * Output page with MAU Preference choices
       */
      private void sendData(PrintWriter out) {
        out.println("<html><head></head><body>");

        writeButtons(out);

      }

      private void writeButtons(PrintWriter out) {

        out.print("<h2><center>MAUPolicyServlet");
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"" + HIGH_SECURITY + "\" value=\"" + HIGH_SECURITY + "\"><br>");
        out.println("<input type=submit name=\"" + NORMAL + "\" value=\"" + NORMAL + "\"><br>");
        out.println("<input type=submit name=\"" + HIGH_COMPLETENESS + "\" value=\"" + HIGH_COMPLETENESS + "\"><br>");
        out.println("\n</form>");

      }
  }
}