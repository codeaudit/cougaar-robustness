/*
 * PublishDefenseConditionsServlet.java
 *
 * Created on October 23, 2003, 1:54 PM
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
public class PublishDefenseConditionsServlet extends BaseServletComponent
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
    return "/SimulatedStressInjectionServlet";
  }  
  
  /**
   * Change the condition value to the specified value
   */
  private String emitDACs(String assetName, String assetType, boolean state, String setMsgLog, String setRestart) {

/*

      DefenseConstants.OMCStrBoolPoint omcState = ( state ? DefenseConstants.BOOL_TRUE : DefenseConstants.BOOL_FALSE );

      
      String msg = "Published Applicability conditions on asset ["+assetName+":"+assetType+"] for Foo";

      try {
          
          blackboard.openTransaction();
          ServletApplicabilityCondition cond1 = (ServletApplicabilityCondition)DefenseApplicabilityCondition.find("Foo", assetName+":"+assetType, blackboard);
          if (cond1 == null) { // don't have a condition for this asset, so create a new one along with its opModes            
                cond1 = new ServletApplicabilityCondition(assetType, assetName, "Foo", omcState);
                blackboard.publishAdd(cond1);
                cond1.setUID(uidService.nextUID());
                ServletMonitoringEnabler sme = new ServletMonitoringEnabler(assetType, assetName, "Foo");
                sme.setUID(uidService.nextUID());
                blackboard.publishAdd(sme);
                ServletDefenseEnabler sde = new ServletDefenseEnabler(assetType, assetName, "Foo");
                sde.setUID(uidService.nextUID());
                blackboard.publishAdd(sde);
          }
          blackboard.closeTransaction();

          blackboard.openTransaction();
            cond1.setValue(omcState.toString());
            blackboard.publishChange(cond1);     
          blackboard.closeTransaction();
          

          blackboard.openTransaction();
          DefenseApplicabilityCondition cond2 = DefenseApplicabilityCondition.find("Msglog", assetName+":"+assetType, blackboard);
          if (cond2 != null && setMsgLog != null && setMsgLog.equalsIgnoreCase("TRUE")) { //let's create it!
            cond2.setValue(omcState.toString());
            blackboard.publishChange(cond2);
            msg = msg+", Msglog";
          } else { // see if we should publish MsgLog
            if (setMsgLog != null && setMsgLog.equalsIgnoreCase("TRUE") ) {
                 
                cond2 = new ServletApplicabilityCondition(assetType, assetName, "Msglog", omcState);
                blackboard.publishAdd(cond2);
                cond2.setUID(uidService.nextUID());
                ServletMonitoringEnabler sme2 = new ServletMonitoringEnabler(assetType, assetName, "Msglog");
                sme2.setUID(uidService.nextUID());
                blackboard.publishAdd(sme2);
                ServletDefenseEnabler sde2 = new ServletDefenseEnabler(assetType, assetName, "Msglog");
                sde2.setUID(uidService.nextUID());
                blackboard.publishAdd(sde2);

                msg = msg+", New Msglog";

                blackboard.closeTransaction();
                blackboard.openTransaction();                
            }
          }

//          DefenseApplicabilityCondition cond3 = DefenseApplicabilityCondition.find("Restart", assetName+":"+assetType, blackboard);
//          if (cond3 != null) { //let's create it!
//            cond3.setValue(omcState.toString());
//            blackboard.publishChange(cond3);
//            msg = msg+", Restart";
//          }

          
          DefenseApplicabilityCondition cond3 = DefenseApplicabilityCondition.find("Restart", assetName+":"+assetType, blackboard);
          if (cond3 != null && setRestart != null && setRestart.equalsIgnoreCase("TRUE")) { //let's create it!
            cond3.setValue(omcState.toString());
            blackboard.publishChange(cond3);
            msg = msg+", Restart";
          } else { // see if we should publish MsgLog
            if (setRestart != null && setRestart.equalsIgnoreCase("TRUE") ) {
                 
                cond3 = new ServletApplicabilityCondition(assetType, assetName, "Restart", omcState);
                blackboard.publishAdd(cond3);
                cond3.setUID(uidService.nextUID());
                ServletMonitoringEnabler sme2 = new ServletMonitoringEnabler(assetType, assetName, "Restart");
                sme2.setUID(uidService.nextUID());
                blackboard.publishAdd(sme2);
                ServletDefenseEnabler sde2 = new ServletDefenseEnabler(assetType, assetName, "Restart");
                sde2.setUID(uidService.nextUID());
                blackboard.publishAdd(sde2);

                msg = msg+", New Restart";

                blackboard.closeTransaction();
                blackboard.openTransaction();                
            }
          }          
          
          msg = msg+". All = "+state;
          
          blackboard.closeTransaction();
          if (logger.isDebugEnabled()) logger.debug(msg+" All = "+state);
          if (eventService.isEventEnabled()) {
              eventService.event(msg);
          }
      } catch (Exception e) {
          logger.warn("Got exception trying to publish conditions", e);
          if ( blackboard.isTransactionOpen() ) { blackboard.closeTransaction(); }
      }           

      return msg;
 */
      return null;
  }
  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
/*          
        String assetName = null;
        String assetType = null; 
        String state = null;
        String setMsgLog = null;
        String setRestart = null;

        if (request != null) {
            assetName = request.getParameter("ASSETNAME");
            assetType = request.getParameter("ASSETTYPE");
            if (assetType != null) { assetType = assetType.toLowerCase(); } //types are lower case 
            state = request.getParameter("T_OR_F");
            setMsgLog = request.getParameter("SET_MSGLOG");
            setRestart = request.getParameter("SET_RESTART");
        }
        
        response.setContentType("text/html");
        
        try {
          PrintWriter out = response.getWriter();
          if (assetName != null && assetType != null && state != null && (state.equalsIgnoreCase("TRUE") || state.equalsIgnoreCase("FALSE")) ) {
              sendData(out);
              boolean stateBool = Boolean.valueOf(state).booleanValue();
              String i = emitDACs(assetName, assetType, stateBool, setMsgLog, setRestart);
              out.println("<center><h2>Emitted DefenseApplicabilityConditions for the specified asset.</h2></center><br>" );            
              out.println("<center><h2>"+i+"</h2></center><br>" );            
          } else {
              sendData(out);
              out.println("<center><h2>DefenseApplicabilityConditions not emitted - All three values required.</h2></center><br>" );            
              //if (eventService.isEventEnabled()) {
               //   eventService.event("ERROR: Condition Name or Value not set properly: "+condName+"="+condValue);
              //}
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
/*          
        out.print("<h2><center>PublishDefenseConditionsServlet in Agent ");
        out.print(agentId.toString() + "</center></h2><p><p>\n");
        out.print("<h3><center>Publishes DefenseApplicabilityConditions </center></h3>\n");
        out.print("<form name=\"myForm\" method=\"get\" >" );
        out.println("AssetName: <input type=text name=ASSETNAME value=\"\"><p>");
        out.println("AssetType: <input type=text name=ASSETTYPE value=\"\" ><p>");
        out.println("Condition: <input type=text name=T_OR_F value=\"TRUE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]<p>");
        out.println("Include Fake Msglog?: <input type=text name=SET_MSGLOG value=\"FALSE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]");
        out.println("Include Fake Restart?: <input type=text name=SET_RESTART value=\"FALSE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]");
        out.println("<input type=submit name=\"Submit\" value=\"Submit\"><br>");
        out.println("\n</form>");
*/
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
  /*
   private static class ServletApplicabilityCondition extends DefenseApplicabilityCondition { //implements NotPersistable {
    public ServletApplicabilityCondition(String a, String b, String c) {
      super(a,b,c, DefenseConstants.BOOL_RANGELIST);
    }

    public ServletApplicabilityCondition(String a, String b, String c, DefenseConstants.OMCStrBoolPoint pt) {
      super(a,b,c, DefenseConstants.BOOL_RANGELIST, pt.toString());
    }
    
   }

   
    private class ServletMonitoringEnabler extends MonitoringEnablingOperatingMode {
        public ServletMonitoringEnabler(String assetType, String assetID, String defense) {
           super(assetType, assetID, defense);
        } 
    }
     
   private class ServletDefenseEnabler extends DefenseEnablingOperatingMode {
      public ServletDefenseEnabler(String assetType, String assetID, String defense) {
         super(assetType, assetID, defense);
      } 
  }
*/
}
