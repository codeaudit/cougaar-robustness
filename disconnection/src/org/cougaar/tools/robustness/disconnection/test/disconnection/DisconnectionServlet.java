/*
 * DisconnectionServlet.java
 *
 * Created on April 10, 2003, 11:19 AM
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

package org.cougaar.tools.robustness.disconnection.test.disconnection;

import org.cougaar.tools.robustness.disconnection.*;
import org.cougaar.tools.robustness.deconfliction.*;

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
import org.cougaar.planning.servlet.BlackboardServletComponent;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.core.adaptivity.Condition;

import org.cougaar.util.log.Logger;


/**
 *
 * Servlet tests the disconnection module (UC7)
 *
 * HTTP Query arguments:
 *
 * reconnectTime=(double >= 0), e.g. reconnectTime=3000
 *
 */
public class DisconnectionServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  private static final String RECONNECT_TIME = "reconnectTime";
  
  private ConditionService conditionService;
  private UIDService uidService = null;
  private EventService eventService = null;
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  private MessageAddress agentId = null;
  
  private String nodeID = null;
  
  private static final String MY_APPLICABILITY_CONDITION_NAME
    = "PlannedDisconnect.UnscheduledDisconnect.Node.";
  private static final String MY_RECONNECT_TIME_NAME
    = "PlannedDisconnect.UnscheduledReconnectTime.Node.";
  
  
  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  /** aquire services */
  public void load() {

    // get the log
    logger = (LoggingService)
      serviceBroker.getService(
          this, LoggingService.class, null);
    if (logger == null) {
      logger = LoggingService.NULL;
    }

    //get the node ID
    NodeIdentificationService node_id_svc = (NodeIdentificationService)
        serviceBroker.getService(this, NodeIdentificationService.class, null);
    nodeID = node_id_svc.getMessageAddress().toString();
    
    
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
    this.agentId = agentIdService.getMessageAddress();
    serviceBroker.releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
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

    // get the ConditionService
    this.conditionService = (ConditionService)
      serviceBroker.getService(
          this,
          ConditionService.class,
          null);
    if (conditionService == null) {
      throw new RuntimeException(
          "Unable to obtain ConditionService");
    }
    
    // get the UIDService
    this.uidService = (UIDService)
      serviceBroker.getService(
          this,
          UIDService.class,
          null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain UIDService");
    }

    // get the EventService
    this.eventService = (EventService)
      serviceBroker.getService(
          this,
          EventService.class,
          null);
    if (eventService == null) {
      throw new RuntimeException(
          "Unable to obtain EventService");
    }
    
    super.load();
  }

  /** release services */
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (conditionService != null) {
      serviceBroker.releaseService(
          this, ConditionService.class, conditionService);
      conditionService = null;
    }
    if (uidService != null) {
      serviceBroker.releaseService(
          this, UIDService.class, uidService);
      uidService = null;
    }
    if (eventService != null) {
      serviceBroker.releaseService(
          this, EventService.class, eventService);
      eventService = null;
    }
    if ((logger != null) && (logger != LoggingService.NULL)) {
      serviceBroker.releaseService(
          this, LoggingService.class, logger);
      logger = LoggingService.NULL;
    }
  }

  /** odd BlackboardClient method */
  public String getBlackboardClientName() {
    return toString();
  }

  /** odd BlackboardClient method */
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }


  /** outputs '"<path>" servlet' */
  public String toString() {
    return "\""+getPath()+"\" servlet";
  }
  
  protected String getPath() {
    return "/Disconnection";
  }  
  
  /**
   * Change the condition value to the specified value
   */
  void setTestCondition(double reconnectTime, PrintWriter out) {
      
      DisconnectionApplicabilityCondition cond = null;
      PlannedDisconnectServletTestNodePlugin.MyReconnectTimeCondition rtc = null;
      
       Condition c1 = 
        conditionService.getConditionByName(MY_APPLICABILITY_CONDITION_NAME+nodeID);
       Condition c2 = 
        conditionService.getConditionByName(MY_RECONNECT_TIME_NAME+nodeID);


      if (c1 != null && c1 instanceof DisconnectionApplicabilityCondition) {
        cond = (DisconnectionApplicabilityCondition)c1;
      }
      if (c2 != null && c2 instanceof PlannedDisconnectServletTestNodePlugin.MyReconnectTimeCondition) {
        rtc = (PlannedDisconnectServletTestNodePlugin.MyReconnectTimeCondition)c2;
      }

      if (cond == null) {
          logger.error("Conditions do not exist!");
          if (eventService.isEventEnabled()) {
              eventService.event("ERROR: Conditions do not exist.");
          }
          out.println("<center><h2>Needed Condition Not Found!</h2></center><br>" );                                        
          return;
      }


      if (reconnectTime > 0) {
          cond.setValue(DefenseConstants.BOOL_TRUE);
          out.println("<center><h2>*"+MY_APPLICABILITY_CONDITION_NAME+nodeID+" set to TRUE</h2></center><br>" );                                        
      }
      else {
          cond.setValue(DefenseConstants.BOOL_FALSE);
          out.println("<center><h2>*"+MY_APPLICABILITY_CONDITION_NAME+nodeID+" set to FALSE</h2></center><br>" );                                        
      }
      rtc.setValue(new Double(reconnectTime));
      out.println("<center><h2>"+MY_RECONNECT_TIME_NAME+nodeID+" set to "+reconnectTime+"</h2></center><br>" );                                        
out.println("***********************************************");
      System.out.println("********************Pushing servlet changes to BB.");
      blackboard.openTransaction();
      blackboard.publishChange(cond);
      blackboard.publishChange(rtc);
      blackboard.closeTransaction();
      System.out.println("********************Pushing servlet changes to BB: Done!");

      if (logger.isDebugEnabled()) {
          System.out.println("********************logging enabled!");
          logger.debug(MY_APPLICABILITY_CONDITION_NAME+nodeID+" set to "+ cond.getValue());
          logger.debug(MY_RECONNECT_TIME_NAME+nodeID+" set to "+ rtc.getValue());
      } else
          System.out.println("********************logging NOT enabled!");
      if (eventService.isEventEnabled()) {
          System.out.println("********************eventService enabled!");
          eventService.event(MY_APPLICABILITY_CONDITION_NAME+nodeID+" set to "+ cond.getValue());
          eventService.event(MY_RECONNECT_TIME_NAME+nodeID+" set to "+ rtc.getValue());
      } else 
          System.out.println("********************eventService NOT enabled!");
          
          
  }
  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String rtc = request.getParameter(RECONNECT_TIME);
        response.setContentType("text/html");

        boolean error = false;
        double d = 0;
        if (rtc != null) {
            try {
                d = Double.parseDouble(rtc);
            } catch (NumberFormatException nfe) {
                error = true;
            }
        }
        try {
          PrintWriter out = response.getWriter();
          sendData(out);
          if (rtc != null) {
              if (!error && d >= 0) {
                  setTestCondition(d, out);
              } else {
                  out.println("<center><h2>Condition Not Changed - Invalid double value entered ["+rtc+"]</h2></center><br>" );            
                  if (eventService.isEventEnabled()) {
                      eventService.event("ERROR: INVALID DOUBLE VALUE ENTERED.");
                  }
              }
          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
      }

      /**
       * Output page with disconnect  / reconnect button & reconnect time slot
       */
      private void sendData(PrintWriter out) {
        out.println("<html><head></head><body>");

        writeButtons(out);

      }

      private void writeButtons(PrintWriter out) {

    //    out.print(
    //	      "<script language=\"JavaScript\">\n"+
    //	      "<!--\n"+
    //	      "function mySubmit() {\n"+
    //	      "  var tidx = document.myForm.formCluster.selectedIndex\n"+
    //	      "  var cluster = document.myForm.formCluster.options[tidx].text\n"+
    //	      "  document.myForm.action=\"/$\"+cluster+\"");
    //    out.print(support.getPath());
    //    out.print("\"\n"+
    //	      "  return true\n"+
    //	      "}\n"+
    //	      "// -->\n"+
    //	      "</script>\n");
        out.print("<h2><center>DisconnectionServlet in Agent ");
        out.print(agentId.toString());
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("Reconnect Time Value: <input type=text name="+RECONNECT_TIME+" value=0 >");
        out.println("<p> [ ** Value can be a double only.]");
        out.println("<input type=submit name=\"Submit\" value=\"Submit\"><br>");
        out.println("\n</form>");

      }
  }
//**End of servlet class  
  
/*    private class CondByNamePredicate implements UnaryPredicate { 
        String name;
        public CondByNamePredicate(String omName) {
          name = omName;
        }

        public boolean execute(Object o) {
          if (o instanceof Condition) {
            Condition c = (Condition) o;
            if (name.equals(c.getName())) {
              return true;
            }
          }
          return false;
        }
    }
*/
  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
   private static class DisconnectDefCon extends DefenseCondition { //implements NotPersistable {
    public DisconnectDefCon(String name) {
      super(name, DefenseConstants.BOOL_RANGELIST);
    }

    public DisconnectDefCon(String name, DefenseConstants.OMCStrBoolPoint pt) {
      super(name, DefenseConstants.BOOL_RANGELIST, pt.toString());
    }

    /* Called by Defense to set current condition. Limited to statically defined values
     * of this class. **This methd should NOT be public as anyone can modify the value.
     * Rather should be subclassed, and super.setValue() called.
     *@param new value
     */
    protected void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
        super.setValue(newValue.toString());
    }
    
   }
  
}
