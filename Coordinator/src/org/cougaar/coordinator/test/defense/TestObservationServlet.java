/*
 * TestObservationServlet.java
 *
 * Created on October 24, 2003, 12:31 PM
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
//org.cougaar.coordinator.test.defense.TestObservationServlet
//import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
//import org.cougaar.coordinator.costBenefit.CostBenefitDiagnosis;
//import org.cougaar.coordinator.techspec.DefaultDefenseTechSpec;

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

import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.core.adaptivity.Condition;

import org.cougaar.util.log.Logger;
import org.cougaar.core.persist.NotPersistable;
import java.text.DecimalFormat;


/**
 *
 * Servlet tests the deconfliction module (UC9)
 *
 * HTTP Query arguments:
 *
 *
 *
 */
public class TestObservationServlet extends BaseServletComponent
                               implements BlackboardClient, NotPersistable
 {

  private String LONG_NUM_FORMAT = "###0.####";
  private DecimalFormat nf = new DecimalFormat(LONG_NUM_FORMAT);     
  private EventService eventService = null;
  private BlackboardService blackboard = null;
  private Logger logger = null;
  
  private MessageAddress agentId = null;
  
  //Default refresh rate
  private int refreshRate = 10000;
  
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
    
    //Publish this servlet to the BB so the DefenseSelectionPlugin can talk to it.
    blackboard.openTransaction();
    blackboard.publishAdd(this);
    blackboard.closeTransaction();

    nf.setMinimumFractionDigits(4);
    
  }

  /** release services */
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
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
    return "/TestObservationServlet";
  }  
  
    Vector activeCostBenefits = new Vector();
    Vector oldCostBenefits = new Vector();
    
    public void addCostBenefitDiagnosis(CostBenefitDiagnosis cbd) {

   //     activeCostBenefits.addElement(cbd);
    }

    
    public void updateDefenseStatus(CostBenefitDiagnosis cbd, String defenseName, String status) {
/*
        CostBenefitDiagnosis.DefenseBenefit defenses[] = cbd.getDefenses();
        CostBenefitDiagnosis.DefenseBenefit def;
        
        boolean found = false;
        for (int i=0; i<defenses.length; i++) {
            def = defenses[i];
            if (def.getDefense() != null && def.getDefense().getName().equalsIgnoreCase(defenseName)) {
                def.setStatus(status);
                found = true;
                break;
            }
        } 
        
        if (!found) {
           logger.debug("No defense "+defenseName+" found to update status to "+status);   
        }
 */
    }

    public void updateCostBenefitStatus(CostBenefitDiagnosis cbd, String status) {
/*
        //If done, move to old CBD vector
        if (status.equalsIgnoreCase("SUCCEEDED") || status.equalsIgnoreCase("FAILED") || status.startsWith("COMPLETED")) {
            activeCostBenefits.removeElement(cbd);
            oldCostBenefits.addElement(cbd);
        }
        
        cbd.setStatus(status);
 */
    }
    
    public void setTimeout(CostBenefitDiagnosis cbd, String defenseName, long l) {
        /*
        CostBenefitDiagnosis.DefenseBenefit defenses[] = cbd.getDefenses();
        CostBenefitDiagnosis.DefenseBenefit def;
        
        for (int i=0; i<defenses.length; i++) {
            def = defenses[i];
            if (def.getDefense() != null && def.getDefense().getName().equalsIgnoreCase(defenseName)) {
                def.setTimeout(l);
                break;
            }
        }
        */
    }
    
    public void updateDefenseOutcome(CostBenefitDiagnosis cbd, String defenseName, String outcome) {
/*
        CostBenefitDiagnosis.DefenseBenefit defenses[] = cbd.getDefenses();
        CostBenefitDiagnosis.DefenseBenefit def;
        
        for (int i=0; i<defenses.length; i++) {
            def = defenses[i];
            if (def.getDefense() != null && def.getDefense().getName().equalsIgnoreCase(defenseName)) {
                def.setOutcome(outcome);
                break;
            }
        }
        */
    }
    

  private class MyServlet extends HttpServlet {
      
      public void doGet(HttpServletRequest request, HttpServletResponse response) {
/*          
        String refresh = null;
        String error = null;
        
        if (request != null) {
            refresh = request.getParameter("REFRESH");
            if (refresh != null) {
                try {
                    int r = Integer.parseInt(refresh);
                    if (r < 1000) {
                        error = "Could not set refresh rate to "+refresh+". Number must be greater than 999."; 
                    } else {
                        refreshRate = r;
                    }
                } catch (NumberFormatException nfe) {
                    error = "Could not set refresh rate to "+refresh+". NumberFormatException occurred."; 
                }
            }
            
            String lnf = request.getParameter("LNF");
            if (lnf != null) {
                LONG_NUM_FORMAT = lnf;
                nf = new DecimalFormat(LONG_NUM_FORMAT);
            }
            
        }
        
        response.setContentType("text/html");
        
        try {
          PrintWriter out = response.getWriter();
 */
/*          if (assetName != null && assetType != null && state != null && (state.equalsIgnoreCase("TRUE") || state.equalsIgnoreCase("FALSE")) ) {
              sendData(out);
              boolean stateBool = Boolean.valueOf(state).booleanValue();
              String i = emitDACs(assetName, assetType, stateBool, setMsgLog);
              out.println("<center><h2>Emitted DefenseApplicabilityConditions for the specified asset.</h2></center><br>" );            
              out.println("<center><h2>"+i+"</h2></center><br>" );            
          } else {
*/
/*          
          emitHeader(out);
          if (error != null) { // then emit the error
             out.print("<font color=\"#0C15FE\">"+ error + "</h2></font>");
          }

          boolean e1 = emitData(out, activeCostBenefits, false);
          boolean e2 = emitData(out, oldCostBenefits, true);
          
          if (!(e1 || e2)) {
              out.println("<p><p><p><h2><center>No Data is Available.</center></h2>");
          }
          emitFooter(out);
          //out.println("<center><h2>DefenseApplicabilityConditions not emitted - All three values required.</h2></center><br>" );            
              //if (eventService.isEventEnabled()) {
               //   eventService.event("ERROR: Condition Name or Value not set properly: "+condName+"="+condValue);
              //}
//          }
          out.close();
        } catch (java.io.IOException ie) { ie.printStackTrace(); }
        */
      }

      /**
       * Output page with disconnect  / reconnect button & reconnect time slot
       */
      private void emitHeader(PrintWriter out) {
/*          
        out.println("<html><head></head><body onload=\"setTimeout('location.reload()',"+refreshRate+");\">");
        out.println("<center><h1>Deconfliction Test Observation Servlet</h1>");
        out.println("<p>Will refresh every " + (refreshRate/1000) + " seconds. ");
        out.println("You can change this rate here: (in milliseconds)");
        out.print("<form name=\"myForm\" method=\"get\" >" );
        out.println("Refresh Rate: <input type=text name=REFRESH value=\""+refreshRate+"\" size=7 >");
        out.println("<sp>Long# Format: <input type=text name=LNF value=\""+LONG_NUM_FORMAT+"\" >");
        out.println("<input type=submit name=\"Submit\" value=\"Submit\" size=10 ><br>");
        out.println("\n</form>");
        out.println("</center><hr>");
 */
      }

      /**
       * Output page with disconnect  / reconnect button & reconnect time slot
       */
      private void emitFooter(PrintWriter out) {
        out.println("</html>");
      }
      
      /** Emit data for the given CostBenefitDiagnosis vector
       *
       */      
      private boolean emitData(PrintWriter out, Vector vector, boolean isOld) {
/*
         CostBenefitDiagnosis cbd = null;
         boolean emittedData = false;
         Iterator i = vector.iterator(); 
         if (i.hasNext()) {
              if (isOld) { out.println("<h2>Completed Observations</h2><hr>"); }
              emittedData = true;             
         }
         
         while (i.hasNext()) {          

              cbd = (CostBenefitDiagnosis)i.next();
              String stat = (cbd.getStatus() == null) ? "NONE" : cbd.getStatus();
              out.println("<h2><font color=\"#0C15FE\">CostBenefit Diagnosis for "+cbd.getAssetName()+"</font></h2>");
              out.println("<p>Status = "+stat);
              
              
                out.print("<p><p><TABLE cellspacing=\"20\">");
                out.print("<CAPTION align=left ><font color=\"#891728\">Defenses</font></CAPTION>");
                out.print("<TR align=left>");
                out.print("   <TH>Defense <sp> </TH>");            
                out.print("   <TH>Applicable </TH>");            
                out.print("   <TH>Belief </TH>");            
                out.print("   <TH>Benefit/Time   </TH>");            
                out.print("   <TH>Horizon   </TH>");            
                out.print("   <TH>Cost   </TH>");            
                out.print("   <TH>Exp_Benefit   </TH>");            
                out.print("   <TH>Timeout   </TH>");            
                out.print("   <TH>Status   </TH>");            
                out.print("   <TH>Outcome [t(a->c)]  </TH>");            
                out.print("</TR>");

              CostBenefitDiagnosis.DefenseBenefit defenses[] = cbd.getDefenses();
              CostBenefitDiagnosis.DefenseBenefit def;


              if (defenses != null && defenses.length > 0) {
                    for (int j=0; j<defenses.length; j++) {
                        def = defenses[j];
                        out.print("<TR>");
                        String defense = "null";                        
                        DefaultDefenseTechSpec dts = (DefaultDefenseTechSpec)def.getDefense();
                        if (dts != null) { 
                            defense = def.getDefense().getName(); 
                        }                        
                        out.print("   <TD>"+defense +"</TD>");

                        String appl="unknown";
                        if (def.getCondition() != null) { //then assign either applicable or not applicable
                            if (def.getCondition().getValue().toString().equalsIgnoreCase("FALSE")) { appl = "No"; }
                            else { appl = "Yes"; }
                        }
                        out.print("   <TD>"+appl+"</TD>");        
                        
                        String believe;
                        try {
                            believe = "" + nf.parse(nf.format(def.getBelievability())); 
                        } catch(Exception e) {
                            believe = "error";
                            logger.warn("Error parsing number: "+def.getBelievability());
                        }
                        out.print("   <TD>"+believe+"</TD>");            
                        
                        out.print("   <TD>"+dts.t_getBenefit()+"</TD>");    //per unit time        
                        out.print("   <TD>"+def.getHorizon()+"</TD>");            //horizon
                        out.print("   <TD>"+dts.t_getCost()+"</TD>");   //cost         
                        
                        String expBenefit; 

                        try {
                            expBenefit = "" + nf.parse(nf.format(def.getOrigBenefit())); 
                        } catch(Exception e) {
                            expBenefit = "error";
                            logger.warn("Error parsing number: "+def.getOrigBenefit());
                        }
                        
                        out.print("   <TD>"+expBenefit+"</TD>");  //expected overall benefit          
                        out.print("   <TD>"+def.getTimeout()+"</TD>");  //timeout          
                        String stat2 = def.getStatus();
                        if (stat2 == null) { stat2 = "unknown"; }
                        out.print("   <TD>"+stat2+"</TD>");        
                        String outcome = (def.getOutcome() == null) ? "---" : def.getOutcome();
                        long ct = (def.getCondition() != null) ? def.getCondition().getCompletionTime() : 0;
                        if (ct > 0) { //then this defense ran & completed, so show its time.
                            outcome = outcome + " ["+( ((double)(ct-def.getCondition().getTimestamp()))/1000 )+"]";
                        }
                        out.print("   <TD>"+outcome+"</TD>");        
                        out.print("</TR>");
                    }            
              }
              out.print("</TABLE>");
              out.print("<hr>");
         }
*/          
         return false; //emittedData;
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
        out.print("<h2><center>TestObservationServlet in Agent ");
   //     out.print(agentId.toString() + "</center></h2><p><p>\n");
        out.print("<h3><center>Publishes DefenseApplicabilityConditions </center></h3>\n");
        out.print("<form name=\"myForm\" method=\"get\" >" );
        out.println("AssetName: <input type=text name=ASSETNAME value=\"\"><p>");
        out.println("AssetType: <input type=text name=ASSETTYPE value=\"\" ><p>");
        out.println("Condition: <input type=text name=T_OR_F value=\"TRUE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]<p>");
        out.println("Include Fake MsgLog?: <input type=text name=SET_MSGLOG value=\"FALSE\" >");
        out.println("<p> [ ** Value can be TRUE or FALSE only.]");
        out.println("<input type=submit name=\"Submit\" value=\"Submit\"><br>");
        out.println("\n</form>");

      }
  }
    //**End of servlet class  
    
    
}
