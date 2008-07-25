/*
 * DisconnectServlet.java
 * 
 * @author David Wells - OBJS
 * 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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

 
package org.cougaar.tools.robustness.disconnection;

import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes.LocalReconnectTimeCondition;


public class DisconnectServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  public static final String DISCONNECT = "Disconnect";
  public static final String RECONNECT = "Reconnect";
  public static final String CHANGE = "change";
  public static final String EXPIRE = "expire";
  
  private BlackboardService blackboard = null;
  private LoggingService logger = null;
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
                            out.println("<center><h2>Failed to Disconnect - Manager not Ready</h2></center><br>");
                            if (logger.isErrorEnabled()) logger.error("Failed to Disconnect - Manager not Ready");
                            if (eventService.isEventEnabled()) {
                                eventService.event("Failed to Disconnect - Manager not Ready");
                            }
                        }
                        blackboard.closeTransaction();
                    } finally {
                        if (blackboard.isTransactionOpen()) blackboard.closeTransactionDontReset();
                    }
              } catch (NumberFormatException nfe) {
                  out.println("<center><h2>Failed to Disconnect - NumberFormatException!</h2></center><br>" );            
                  if (logger.isErrorEnabled()) logger.error("Failed to Disconnect - Invalid Disconnect Interval = " + expire);
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
