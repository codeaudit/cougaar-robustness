/*
 * DeconflictionServlet.java
 *
 * Created on April 10, 2003, 11:19 AM
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


package org.cougaar.coordinator.test.defense;

import org.cougaar.coordinator.*;

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
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.core.adaptivity.Condition;

import org.cougaar.util.log.Logger;


/**
 *
 * Servlet tests the deconfliction module (UC9)
 *
 * HTTP Query arguments:
 *
 *
 *
 */
public class DeconflictionServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  private static final String CONDITION_NAME = "condName";
  private static final String CONDITION_VALUE = "condValue";
  
  private ConditionService conditionService;
  private UIDService uidService = null;
  private EventService eventService = null;
  private BlackboardService blackboard = null;
  private LoggingService logger = null;
  
  private MessageAddress agentId = null;
  
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
    return "/Deconfliction";
  }  
  
  /**
   * Change the condition value to the specified value
   */
  private void setTestCondition(String condName, boolean bool) {
/*
      Condition condX = conditionService.getConditionByName(condName);
      
      DisconnectDefCon cond = null;
      if (cond != null) {
          if (cond instanceof DisconnectDefCon) {
            cond = (DisconnectDefCon)condX;
          } else {
              logger.error("Condition ["+condName+"] of another type (not DisconnectDefCon) already exists.");
              if (eventService.isEventEnabled()) {
                  eventService.event("ERROR: Condition ["+condName+"] of another type (not DisconnectDefCon) already exists.");
              }
          }
      }
          
      boolean newCond = false;
      if (cond == null) { // create new condition
          cond = new DisconnectDefCon(condName, condName, condName);
          newCond = true;
      }
          
      if (bool) {
          cond.setValue(DefenseConstants.BOOL_TRUE);
      }
      else {
          cond.setValue(DefenseConstants.BOOL_FALSE);
      }

      blackboard.openTransaction();
      if (newCond) {
          blackboard.publishAdd(cond);
      } else {
          blackboard.publishChange(cond);
      }
      blackboard.closeTransaction();
      if (logger.isDebugEnabled()) logger.debug("Condition "+condName+" set to "+ cond.getValue());
      if (eventService.isEventEnabled()) {
          eventService.event("Condition "+condName+" set to "+ cond.getValue());
      }
 */
  }
  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
/*        String condName = request.getParameter(CONDITION_NAME);
        String condValue = request.getParameter(CONDITION_VALUE);
        response.setContentType("text/html");

        
        try {
          PrintWriter out = response.getWriter();
          if (condName != null && condValue != null && (condValue.equalsIgnoreCase("TRUE") || condValue.equalsIgnoreCase("FALSE")) ) {
              sendData(out);
              boolean condBool = Boolean.valueOf(condValue).booleanValue();
              setTestCondition(condName, condBool);
              out.println("<center><h2>Condition "+condName+" set to "+condBool+".</h2></center><br>" );            
          } else {
              sendData(out);
              out.println("<center><h2>Condition Not Changed - Both condition name & value (TRUE|FALSE) required.</h2></center><br>" );            
              if (eventService.isEventEnabled()) {
                  eventService.event("ERROR: Condition Name or Value not set properly: "+condName+"="+condValue);
              }
          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
 */
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
        out.print("<h2><center>DeconflictionServlet in Agent ");
  //      out.print(agentId.toString());
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
    //    out.println("ConditionName: <input type=text name="+CONDITION_NAME+" value=\""+
      //                  MyServletTestDefense.MYCONDITION_NAME+"\">");
        out.println("<p>");
   //     out.println("        Value: <input type=text name="+CONDITION_VALUE+" value=TRUE >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]");
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
/*   private static class DisconnectDefCon extends DefenseCondition { //implements NotPersistable {
    public DisconnectDefCon(String a, String b, String c) {
      super(a,b,c, DefenseConstants.BOOL_RANGELIST);
    }

    public DisconnectDefCon(String a, String b, String c, DefenseConstants.OMCStrBoolPoint pt) {
      super(a,b,c, DefenseConstants.BOOL_RANGELIST, pt.toString());
    }
*/
    /* Called by Defense to set current condition. Limited to statically defined values
     * of this class. **This methd should NOT be public as anyone can modify the value.
     * Rather should be subclassed, and super.setValue() called.
     *@param new value
     */
/*    protected void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
        super.setValue(newValue.toString());
    }
*/    
  
}
