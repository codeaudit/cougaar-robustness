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

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;


/**
 *
 * @author  Administrator
 */
public class DisconnectServlet extends BaseServletComponent
                               implements BlackboardClient
 {

  public static final String DISCONNECT = "Disconnect";
  public static final String RECONNECT = "Reconnect";
  public static final String CHANGE = "change";
  public static final String EXPIRE = "expire";

  public static final String CONDITION_NAME = "PlannedDisconnect.UnscheduledDisconnect.Node.";
  
  private ConditionService conditionService;
  private UIDService uidService = null;
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  private MessageAddress agentId = null;
  
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
    
    // get the ConditionService
    this.uidService = (UIDService)
      serviceBroker.getService(
          this,
          UIDService.class,
          null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain UIDService");
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
        System.out.println("Expire = "+ expire);
        double d = 0;
        if (expire != null) {
            try {
                d = Double.parseDouble(expire);
                setConditionValue(DefenseConstants.BOOL_TRUE);
                setReconnectTimeConditionValue(d);
                out.println("<center><h2>Status Changed - Disconnected</h2></center><br>" );
            } catch (NumberFormatException nfe) {
                out.println("<center><h2>Status Not Changed - NumberFormatException!</h2></center><br>" );            
            }
        } else {
            out.println("<center><h2>Status Not Changed - No Reconnect Time Provided (double).</h2></center><br>" );            
        }
      }

 
      /** User is reconnecting the node */
      private void reconnect(HttpServletRequest request, PrintWriter out) {
        setConditionValue(DefenseConstants.BOOL_FALSE);
        out.println("<center><h2>Status Changed - Reconnected</h2></center><br>" );
      }


      /** publish conditional with new status for this agent */ 
      private void setConditionValue(DefenseConstants.OMCStrBoolPoint value) {

            final String condName = CONDITION_NAME+agentId.toString();
            
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {
                return 
                  ((o instanceof DisconnectionApplicabilityCondition) &&
                   (condName.equals(((DisconnectionApplicabilityCondition) o).getName())));
              }
            };
            
            DisconnectionApplicabilityCondition cond = null;
            blackboard.openTransaction();
            Collection c = blackboard.query(pred);
            if (c.iterator().hasNext()) {
               cond = (DisconnectionApplicabilityCondition)c.iterator().next();
            }
            blackboard.closeTransaction();
            
            if (cond == null) { //then create one
                cond = new DisconnectionApplicabilityCondition(condName);
                System.out.print("Created and ");
            }

            System.out.println("Published DisconnectCondition: "+condName+" = "+ value.toString());

            blackboard.openTransaction();
            cond.setValue(value); //true = disconnected
            blackboard.publishChange(cond);    
            blackboard.closeTransaction();
      }      
        
        
      /** Publish reconnect time */
      private void setReconnectTimeConditionValue(double d) {
            final String condName = CONDITION_NAME+agentId.toString();
            
            UnaryPredicate pred = new UnaryPredicate() {
              public boolean execute(Object o) {
                return 
                  ((o instanceof ReconnectTimeCondition) &&
                   (condName.equals(((ReconnectTimeCondition) o).getName())));
              }
            };
            
            ReconnectTimeCondition cond = null;
            blackboard.openTransaction();
            Collection c = blackboard.query(pred);
            if (c.iterator().hasNext()) {
               cond = (ReconnectTimeCondition)c.iterator().next();
            }
            blackboard.closeTransaction();
            
            if (cond == null) { //then create one
                cond = new ReconnectTimeCondition(condName);
                System.out.print("Created and ");
            }


            blackboard.openTransaction();
            cond.setValue(new Double(d)); //true = disconnected
            blackboard.publishChange(cond);    
            blackboard.closeTransaction();

            System.out.println("Published ReconnectTimeCondition: "+condName+" = "+ cond.getValue().toString() + "[was d="+d+"]");
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
        out.print("<h2><center>PlannedDisconnectServlet for ");
        out.print(agentId.toString());
        out.print(
                  "</center></h2>\n"+
                  "<form name=\"myForm\" method=\"get\" >" );
        out.println("<input type=submit name=\"Disconnect\" value=\"Disconnect\"><br>");
        out.println("<input type=submit name=\"Reconnect\" value=\"Reconnect\"><br>");
        out.println("Will Reconnect In <input type=text name="+EXPIRE+"> minutes.");
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
