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

/*
 * AvailableBandwidthServlet.java
 *
 * Created on October 4, 2004, 8:43 AM
 */

package org.cougaar.coordinator.sensors.load;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;
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
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.coordinator.RobustnessManagerID;
import org.cougaar.util.UnaryPredicate;


public class AvailableBandwidthServlet extends BaseServletComponent implements BlackboardClient {

    // Note - this servlet should only be loaded into an AR Manager agent

    // Predefined Load Ranges
    public static final String LOW = "Low";
    public static final String MODERATE = "Moderate";
    public static final String HIGH = "High";

    private BlackboardService blackboard = null;
    private Logger logger = null;
  
    private String arManager;

  
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
    
    if(logger.isDebugEnabled()) logger.debug("AvailableBandwidthServlet Loaded");
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
    return "/AvailableBandwidthServlet";
  }  

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }


  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String none = request.getParameter(LOW);
        String moderate = request.getParameter(MODERATE);
        String high = request.getParameter(HIGH);
        response.setContentType("text/html");
        AvailableBandwidthDiagnosis diag = null;

        blackboard.openTransaction();
        Collection c = blackboard.query(AvailableBandwidthDiagnosis.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
            diag = (AvailableBandwidthDiagnosis)iter.next();
        }

        try {
            PrintWriter out = response.getWriter();
            sendData(out);
            if (diag == null) {
                try { 
                    Collection c2 = blackboard.query(arManagerPred);
                    Iterator iter2 = c2.iterator();
                    if (iter2.hasNext()) { 
                        arManager = ((RobustnessManagerID)iter2.next()).getMessageAddress().toString();
                        diag = new AvailableBandwidthDiagnosis(arManager , serviceBroker);
                    }
                    else {
                        if (logger.isInfoEnabled()) logger.info("Cannot find ARManager name - so do not know Enclave name");
                    }
                }
                catch (TechSpecNotFoundException e) {
                    logger.error(e.toString());
                }
                blackboard.publishAdd(diag);
            }
            try {
                if (none != null) {
                    diag.setValue(LOW);
                    out.println("<center><h2>Available Bandwidth Changed to Low </h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Available Bandwidth Changed to Low for enclave " + getBlackboardClientName());
                }
                else if (moderate != null) {
                    diag.setValue(MODERATE);
                    out.println("<center><h2>Available Bandwidth Changed to Moderate</h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Available Bandwidth Changed to Moderate for enclave " + getBlackboardClientName());
                }
                else if (high != null) {
                    diag.setValue(HIGH);
                    out.println("<center><h2>Available Bandwidth Changed to High</h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Available Bandwidth Changed to High for enclave " + getBlackboardClientName());
                }
            }
            catch (IllegalValueException e) {
                logger.error(e.toString());
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

        out.print("<h2><center>LeashDefensesServlet");
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"" + LOW + "\" value=\"" + LOW + "\"><br>");
        out.println("<input type=submit name=\"" + MODERATE + "\" value=\"" + MODERATE + "\"><br>");
        out.println("<input type=submit name=\"" + HIGH + "\" value=\"" + HIGH + "\"><br>");
        out.println("\n</form>");

      }
  }

    private static final UnaryPredicate arManagerPred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof org.cougaar.coordinator.RobustnessManagerID ) {
                    return true ;
                }
                return false ;
            }
        };
}
