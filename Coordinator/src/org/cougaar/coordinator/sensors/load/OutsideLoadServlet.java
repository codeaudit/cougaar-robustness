/*
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */


/*
 * LoadServlet.java
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

public class OutsideLoadServlet extends BaseServletComponent implements BlackboardClient {

    // Note - This servlet should only be loaded into the AR Manager agent

    // Predefined Load Ranges
    public static final String NONE = "None";
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
    
    if(logger.isDebugEnabled()) logger.debug("LoadServlet Loaded");
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
    return "/OutsideLoadServlet";
  }  

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }


  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String none = request.getParameter(NONE);
        String moderate = request.getParameter(MODERATE);
        String high = request.getParameter(HIGH);
        response.setContentType("text/html");
        OutsideLoadDiagnosis diag = null;

        blackboard.openTransaction();
        Collection c = blackboard.query(OutsideLoadDiagnosis.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
            diag = (OutsideLoadDiagnosis)iter.next();
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
                        diag = new OutsideLoadDiagnosis(arManager , serviceBroker);
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
                    diag.setValue(NONE);
                    out.println("<center><h2>Outside Load Changed to None </h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Outside Load Changed to None for enclave " + arManager);
                }
                else if (moderate != null) {
                    diag.setValue(MODERATE);
                    out.println("<center><h2>Outside Load Changed to Moderate</h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Outside Load Changed to Moderate for enclave " + arManager);
                }
                else if (high != null) {
                    diag.setValue(HIGH);
                    out.println("<center><h2>Outside Load Changed to High</h2></center><br>" );
                    blackboard.publishChange(diag); 
                    if (logger.isInfoEnabled()) logger.info("Outside Load Changed to High for enclave " + arManager);
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
       * Output page with Stipulated Outside Load choices
       */
      private void sendData(PrintWriter out) {
        out.println("<html><head></head><body>");

        writeButtons(out);

      }

      private void writeButtons(PrintWriter out) {

        out.print("<h2><center>OutsideLoadServlet");
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"" + NONE + "\" value=\"" + NONE + "\"><br>");
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
