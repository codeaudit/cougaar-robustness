/*
 * TheAllNewTheAllNewPublishDefenseConditionsServlet.java
 *
 * Created on October 29, 2003, 2:17 PM
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

package org.cougaar.coordinator.test.defense;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.*;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;

import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;

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
public class TheAllNewPublishDefenseConditionsServlet extends BaseServletComponent
                               implements BlackboardClient
 {
  
  private ConditionService conditionService;
  private UIDService uidService = null;
  private EventService eventService = null;
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  private MessageAddress agentId = null;

  private Hashtable agentConditions = new Hashtable(10);
  
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
    return "/NEW__SimulatedStressInjectionServlet";
  }  
  
  
  /**
   * Change the condition value to the specified value
   */
  private void sendDefenseConditions(String assetName, String assetType, PrintWriter out) {
      
      String expandedName = AssetName.generateExpandedAssetName( assetName, AssetType.findAssetType(assetType) );
      Collection conditions = getDefenseConditions(assetName, assetType, expandedName);
            
      //Now update defense conditions list
      agentConditions.remove(expandedName);
      agentConditions.put(expandedName, conditions);
      
      //OK, now we have all the defenses available.
      DefenseApplicabilityCondition dac;
      
      out.println("<h2><font color=\"#891728\"> Defense Conditions for "+expandedName+" </font></h2>");
      out.println("<form name=\"myForm\" method=\"get\" >" );
      out.println("            <input type=\"hidden\" name=\"ASSETNAME\" value=\""+assetName+"\" >");
      out.println("            <input type=\"hidden\" name=\"ASSETTYPE\" value=\""+assetType+"\" >");

      out.println("<p><TABLE cellspacing=\"20\">");
            out.println("<TR align=left>");
            out.println("   <TH>Defense</TH>");            
            out.println("   <TH>Current Value</TH>");            
            out.println("   <TH>Change to:</TH>");            
            out.println("</TR>");
            
            for (Iterator i = conditions.iterator(); i.hasNext();) {
                dac = (DefenseApplicabilityCondition)i.next();
                
                out.println("<TR>");
                out.println("   <TD>"+dac.getDefenseName()+"</TD>");            
                
                String cVal = (dac.getValue().toString().equalsIgnoreCase("TRUE")) ? "APPLICABLE" : "NOT APPLICABLE";
                out.println("   <TD>"+cVal+"</TD>");            
                out.println("   <TD>");
                out.println("<SELECT NAME=\""+dac.getDefenseName()+"\" SIZE=\"1\">");
                out.println("<OPTION VALUE=\"NOCHANGE\" SELECTED>No Change");
                out.println("<OPTION VALUE=\"APPLICABLE\">Applicable");
                out.println("<OPTION VALUE=\"NOTAPPLICABLE\">Not Applicable");
                out.println("</SELECT>");
                out.println("</TD>");            
                out.println("</TR>");
            }            
            out.println("</TABLE>");

        out.println("<input type=submit name=\"OPERATION\" value=\"UPDATE\"><br>");
        out.println("\n</form>");

        out.println("<form name=\"myForm\" method=\"get\" >" );
            out.println("<input type=submit name=\"Main_Page\" value=\"MAIN\"><br>");
        out.println("\n</form>");
  }
  

  private Collection getDefenseConditions(String assetName, String assetType, String expandedName) {
                  
      blackboard.openTransaction();
      Collection conditions = DefenseApplicabilityCondition.findCollection(expandedName, blackboard);
      blackboard.closeTransaction();
      
      DefenseApplicabilityCondition dac;
      boolean found = false;
      //Search to see if FOO & BAR & DemandEstimation exist, if not create them
      
      for (Iterator i = conditions.iterator(); i.hasNext();) {
      
          dac = (DefenseApplicabilityCondition)i.next();
          if (dac.getDefenseName().equals("FOO") || dac.getDefenseName().equals("BAR") ) {
              found = true; //we known that Foo & Bar have already been added...
              break;
          }
      }
      if (!found) { //add FOO & BAR & DemandEstimation defense conditions
          blackboard.openTransaction();                
            Vector v = addFooBarDefenseConditions(assetName, assetType); 
            conditions.addAll(v);
          blackboard.closeTransaction();
      }  
      
      return conditions;
  }
  
  
  private void updateDefenseConditions(String assetName, String assetType, HttpServletRequest request, PrintWriter out) {
  
      String expandedName = AssetName.generateExpandedAssetName( assetName, AssetType.findAssetType(assetType) );
      
      //Get the defense conditions we know about, if they don't exist in the hashtable, get them now.
      Collection defs = (Collection)agentConditions.get(expandedName);
      if (defs == null) {
          defs = getDefenseConditions(assetName, assetType, expandedName);
          agentConditions.put(expandedName, defs);
      }
      
      DefenseApplicabilityCondition dac;
      String value;
      blackboard.openTransaction();      
      //Now iterate thru the defense conditions & see if any are in the http request. If so, set their value accordingly.
      for (Iterator i = defs.iterator(); i.hasNext(); ) {
        
          dac = (DefenseApplicabilityCondition)i.next();
          value = request.getParameter(dac.getDefenseName());
          if (value != null && !value.equals("NOCHANGE") ) { //change the value & publish it
          
              String b = (value.equals("APPLICABLE")) ? "TRUE" : "FALSE";
              dac.setValue(b);
              blackboard.publishChange(dac);
              logger.debug("Changed defense condition for "+dac.getDefenseName()+" to "+value);
          }
      }
      blackboard.closeTransaction();
      
  }
  

 
  /** Emits the list of assets */
  private void sendDefaultContent(PrintWriter out) {
  
    AssetTechSpecInterface asset;
    
    
    blackboard.openTransaction();
    Collection assets = findAllAssets();
    blackboard.closeTransaction();
    
    out.println("<h2>Select an agent from the list to see its defense conditions.</h2>");
    out.println("<form name=\"myForm\" method=\"get\" >" );


            if (assets.size() == 0) { //then let user know
                out.println("<SELECT NAME=\"AssetList\" SIZE=\"1\">");
                out.println("<OPTION VALUE=\"NONE\">NO ASSETS FOUND");
                out.println("</SELECT>");                
            } else { //list all assets with chance to select one
                out.println("<SELECT NAME=\"EXPANDEDNAME\" SIZE=\"1\">");
                if (assets.size() == 0) {
                    out.println("<OPTION VALUE=\"NONE\">NO ASSETS FOUND");
                } else {
                    for (Iterator i = assets.iterator(); i.hasNext();) {
                        asset = (AssetTechSpecInterface)i.next();
                        out.println("<OPTION VALUE=\""+asset.getExpandedName()+"\">"+asset.getExpandedName());
                    } 
                }
                out.println("</SELECT>");
                out.println("<input type=submit name=\"OPERATION\" value=\"Get_Defense_Conditions\"><br>");
            }
        out.println("\n</form>");
      
            
  }
  
  private Vector addFooBarDefenseConditions(String assetName, String assetType) {
      Vector v = new Vector(2);
      v.addElement( addDefenseConditions(assetName, assetType, "FOO") );
      v.addElement( addDefenseConditions(assetName, assetType, "BAR") );            
      v.addElement( addDefenseConditions(assetName, assetType, "DemandEstimation") );            
      
      return v;
  }
  
  
    private final static UnaryPredicate assetPred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof AssetTechSpecInterface);
            }
        };
        
    public Collection findAllAssets() {
        return blackboard.query(assetPred);
    } 
  
  
  
  private ServletApplicabilityCondition addDefenseConditions(String assetName, String assetType, String defName) {
        ServletApplicabilityCondition cond1 = new ServletApplicabilityCondition(assetType, assetName, defName, DefenseConstants.BOOL_FALSE);
        blackboard.publishAdd(cond1);
        cond1.setUID(uidService.nextUID());
        ServletMonitoringEnabler sme = new ServletMonitoringEnabler(assetType, assetName, defName);
        sme.setUID(uidService.nextUID());
        blackboard.publishAdd(sme);
        ServletDefenseEnabler sde = new ServletDefenseEnabler(assetType, assetName, defName);
        sde.setUID(uidService.nextUID());
        blackboard.publishAdd(sde);    
        
        return cond1;
  }
  

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
          
        String assetName = null;
        String assetType = null; 
        String operation = null;
        String expandedName = null;
        
        if (request != null) {
            assetName = request.getParameter("ASSETNAME");
            assetType = request.getParameter("ASSETTYPE");
            if (assetType != null) { assetType = assetType.toLowerCase(); } //types are lower case 
            operation = request.getParameter("OPERATION");
            expandedName = request.getParameter("EXPANDEDNAME");
        }
        
        logger.debug("operation = "+operation);
        
        response.setContentType("text/html");
        
        if (assetName == null && assetType == null && expandedName != null) { //generate asset/type names
        
            assetName = expandedName.substring(0,expandedName.indexOf(':'));
            assetType = expandedName.substring(expandedName.indexOf(':')+1, expandedName.length());
            logger.debug("assetName = "+assetName+", assetType = "+assetType);
        }
        
        try {
          PrintWriter out = response.getWriter();
          sendHeader(out);
          if (assetName != null && assetType != null && operation != null ) {
              if (operation != null && operation.equals("Get_Defense_Conditions") ) { 
                  sendDefenseConditions(assetName, assetType, out); 
              }
              if (operation != null && operation.equals("UPDATE") ) { 
                  updateDefenseConditions(assetName, assetType, request, out); 
                  sendDefenseConditions(assetName, assetType, out);                   
              }
          } else { //   
              sendDefaultContent(out);              
          }
          
/*              
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
 */         
          
          
          out.close();
        } catch (java.io.IOException ie) { 
            logger.debug("IOException in servlet.", ie);
        }
      }

      /**
       * Output page with disconnect  / reconnect button & reconnect time slot
       */
      private void sendData(PrintWriter out) {
        sendHeader(out);
        writeButtons(out);
      }

      /**
       * Output header
       */
      private void sendHeader(PrintWriter out) {      
              out.println("<html><head></head><body>");
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
        out.print("<h2><center>TheAllNewPublishDefenseConditionsServlet in Agent ");
        out.print(agentId.toString() + "</center></h2><p><p>\n");
        out.print("<h3><center>Publishes DefenseApplicabilityConditions </center></h3>\n");
        out.print("<form name=\"myForm\" method=\"get\" >" );
        out.println("AssetName: <input type=text name=ASSETNAME value=\"\"><p>");
        out.println("AssetType: <input type=text name=ASSETTYPE value=\"\" ><p>");
        out.println("Condition: <input type=text name=T_OR_F value=\"TRUE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]<p>");
        out.println("Include Fake MsgLog?: <input type=text name=SET_MSGLOG value=\"FALSE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]");
        out.println("Include Fake Restart?: <input type=text name=SET_RESTART value=\"FALSE\" >");
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

}
