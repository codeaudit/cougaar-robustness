/*
 * DiagnosisMonitorServlet.java
 *
 * Created on February 11, 2004, 2:12 PM
 * <copyright>
 *  Copyright 2003 Object Services & Consulting, Inc.
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

package org.cougaar.coordinator.test.coordination;

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
import java.util.Set;

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


/**
 *
 * Servlet allows monitoring diagnosis objects on the node & DiagnosesWrapper objects on the enclave coordinator.
 *
 *
 */
public class DiagnosisMonitorServlet extends BaseServletComponent implements BlackboardClient, NotPersistable {
    
    //private String LONG_NUM_FORMAT = "###0.####";
    //private DecimalFormat nf = new DecimalFormat(LONG_NUM_FORMAT);
    private EventService eventService = null;
    private BlackboardService blackboard = null;
    private Logger logger = null;
    
    private MessageAddress agentId = null;

    private final static int NEITHER = 0;
    private final static int CHANGED = 1;
    private final static int ADDED = 2;
    
    private static final String CHANGEDFONT = "<font color=\"#00ff00\">"; //green            
    private static final String ADDEDFONT = "<font color=\"#0000ff\">"; //blue            

    
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
        
        //Publish this servlet to the BB so the object monitors can talk to it.
        blackboard.openTransaction();
        blackboard.publishAdd(this);
        blackboard.closeTransaction();
        
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
        return "/DiagnosisMonitorServlet";
    }
    

    Object changes = null; // used to synchronize vector changes
    Vector diagnoses = new Vector();

/*    
    Vector wrappers = new Vector();

    Vector changedDiagnoses = new Vector();
    Vector changedWrappers = new Vector();

    Vector newDiagnoses = new Vector();
    Vector newWrappers = new Vector();    

    Vector tempChangedDiagnoses = null;
    Vector tempChangedWrappers = null;
    Vector tempNewDiagnoses = null;
    Vector tempNewWrappers = null;
    Vector tempDiagnoses = null;
    Vector tempWrappers = null;
 */
    
    /////////////////////////////////////////////////////////////Diagnoses
    
    void handleDiagnosis(Diagnosis d, int state, boolean isWrapper) {
     
        Diagnosis current;
        DiagRecord rec;
        
        //First look for rec
        Iterator i = diagnoses.iterator();
        //synchronized(changes) { //so 
            while (i.hasNext() ) {      
                rec = (DiagRecord) i.next();
                if ( rec.contains(d) ) { //found it
                    rec.setDiagnosis(d, state, isWrapper);
                    return;
                }
            }
            //Not found so create it
            rec = new DiagRecord(d, state, isWrapper);
            diagnoses.add(rec);
        //}        
    }
    
    //These methods are called by the DiagnosisMonitorPlugin
    public void addDiagnosesWrapper(DiagnosesWrapper dw) {
        handleDiagnosis((Diagnosis)dw.getContent(), ADDED, true);
    }
    
    public void changedDiagnosesWrapper(DiagnosesWrapper dw) {
        handleDiagnosis((Diagnosis)dw.getContent(), CHANGED, true);
    }

    public void addDiagnosis(Diagnosis d) {
        handleDiagnosis(d, ADDED, false);
    }
    
    public void changedDiagnosis(Diagnosis d) {
        handleDiagnosis(d, CHANGED, false);
    }

    
    ///////////////////////////////////////////////////////////////////////

    
        
    private class DiagRecord {
     
        String clsname;
        String shortClsname;
        AssetType type;
        String asset;
        Set pValues;
        
        Diagnosis d;
        Diagnosis dw; //wrapped originally
        int dState = 0;
        int dwState = 0;
        
        public DiagRecord(Diagnosis t, int state, boolean isWrapper) {

            setDiagnosis(t, state, isWrapper);
            
            //Get the identifying properties of this Diagnosis
            clsname = t.getClass().getName();
            type = DiagnosisUtils.getAssetType(t);
            asset= t.getAssetName();
            shortClsname = setShortName(clsname);
            pValues = t.getPossibleValues();
        }
        
        /**
         * @return the class name of the Diagnosis without the pkg name
         */
        String setShortName(String s) {
         
            return s.substring(s.lastIndexOf('.')+1);
            
        }

        /**
         * @return the class name of the Diagnosis without the pkg name. If shortNm = TRUE, return the 
         * class name without the pkg prepended.
         */
        String getName(boolean shortNm) {
         
            return shortNm ? this.shortClsname : clsname;
            
        }
        
        Diagnosis getDiagnosis() { return d; }
        Diagnosis getWrapper() { return dw; }

        void setDiagnosis(Diagnosis t, int state, boolean isWrapper) { 
            
            if (isWrapper) {
                dw = t;
                dwState = state;
            } else {
                d = t;
                dState = state;
            }
        }
        
        int  getWrapperState() { return dwState; }        
        int  getDiagnosisState() { return dState; }

        /**
         * Colorize the diagnosis value based upon whether it has changed, or was recently added.
         */
        String colorizeTDValue(Object val, int state) {
        
            if (state == CHANGED)  {
                return CHANGEDFONT + val + "</font>";
            }
            else if (state == ADDED) {
                return ADDEDFONT + val + "</font>";
            }
            else { //no font annotation
                return val.toString(); 
            }
            
        }
        
        /**
         * @return the possible values for this diagnosis
         */
        Set getPossibleValues() { return pValues; }
        
        /**
         * @return TRUE if the DiagRec is specific to this Diagnosis class, asset type and asset name.
         */
        boolean contains(Diagnosis dd) {
            
           return (dd.getClass().getName().equals(clsname) && 
                   DiagnosisUtils.getAssetType(dd).equals(type) && 
                   dd.getAssetName().equals(asset) );
        }
        
    }
    
    //////////////////////////////////////////////////////////////////////////////////
    
    private class MyServlet extends HttpServlet {
        
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
            String refresh = null;
            String error = null;
            boolean useShortName = true;
            
            if (request != null) {
                
                String ln = request.getParameter("LONGNAME");
                if (ln != null) {useShortName = false;}
                
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
/*                
                String lnf = request.getParameter("LNF");
                if (lnf != null) {
                    LONG_NUM_FORMAT = lnf;
                    nf = new DecimalFormat(LONG_NUM_FORMAT);
                }
*/                
            }
 
            
            response.setContentType("text/html");
            
            try {
                PrintWriter out = response.getWriter();
/*          if (assetName != null && assetType != null && state != null && (state.equalsIgnoreCase("TRUE") || state.equalsIgnoreCase("FALSE")) ) {
              sendData(out);
              boolean stateBool = Boolean.valueOf(state).booleanValue();
              String i = emitDACs(assetName, assetType, stateBool, setMsgLog);
              out.println("<center><h2>Emitted DefenseApplicabilityConditions for the specified asset.</h2></center><br>" );
              out.println("<center><h2>"+i+"</h2></center><br>" );
          } else {
 */
                emitHeader(out);
                if (error != null) { // then emit the error
                    out.print("<font color=\"#0C15FE\">"+ error + "</h2></font>");
                }
                
                boolean e1 = emitData(out, useShortName); //emit diagnoses
                //boolean e2 = emitData(out, true); //emit wrappers
                
                if (!e1) {
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
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitHeader(PrintWriter out) {
            out.println("<html><head></head><body onload=\"setTimeout('location.reload()',"+refreshRate+");\">");
            out.println("<center><h1>Coordinator Diagnosis Monitoring Servlet</h1>");
            out.println("<p>Will refresh every " + (refreshRate/1000) + " seconds. ");
            out.println("You can change this rate here: (in milliseconds)");
            out.print("<form clsname=\"myForm\" method=\"get\" >" );
            out.println("Refresh Rate: <input type=text name=REFRESH value=\""+refreshRate+"\" size=7 >");
            out.println("<input type=submit name=\"Submit\" value=\"Submit\" size=10 ><br>");
            out.println("\n</form>");
            out.println("</center><hr>");
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
        private boolean emitData(PrintWriter out, boolean useShortName) {
            
            boolean emittedData = false;
            Diagnosis d; //diagnosis
            Diagnosis dw; //wrapper
            DiagRecord dr;
            
            Object dv; //store value of diagnosis
            Object dwv; //store value of wrapped diagnosis

            
            Iterator i = diagnoses.iterator();
            if (i.hasNext()) {
                emittedData = true;
            } else { return emittedData; }

            
            tableHeader(out);
            
            //Print out each diagnosis
            while (i.hasNext()) {

                out.print("<TR>\n");

                //Get diagnosis record
                dr = (DiagRecord)i.next();
                
                //Output the asset name
                out.print("   <TD>"+ dr.getDiagnosis().getAssetName() +"</TD>\n");

                //Output the diagnosis name
                out.print("   <TD>"+ dr.getName(useShortName) +"</TD>\n");

                // Possible Values List -----------------------
                Set pvalues = dr.getPossibleValues();

                Iterator pv = pvalues.iterator();
                if (pvalues.size() >0 ) {
                    out.print("   <TD><SELECT size=\"5\" >\n");            
                    while (pv.hasNext()) {
                        out.print("        <OPTION>"+ pv.next() +"</OPTION>\n");
                    }
                    out.print("   </SELECT></TD>\n");            
                } else { //no values
                    out.print("   <TD>Values Unknown</TD>\n");    //per unit time
                }
                
                //Get the diagnosis & wrapped diagnosis
                d  = dr.getDiagnosis();
                dw = dr.getWrapper();
                
                //Get the diagnosis values
                dv =  (d  != null) ?  d.getValue() : "null" ;
                dwv = (dw != null) ? dw.getValue() : "null" ;
                
                //Output their values in living color
                out.print("   <TD>"+ dr.colorizeTDValue(dv, dr.getDiagnosisState() ) +"</TD>\n");    //Diagnosis Value
                out.print("   <TD>"+ dr.colorizeTDValue(dwv, dr.getWrapperState() ) +"</TD>\n");    //Wrapper Value

                //End of row
                out.print("</TR>");
            }
            

        
            return emittedData;
        }
        
        
        private void tableHeader(PrintWriter out) {
            
            out.print("<p><p><TABLE cellspacing=\"20\">");
            out.print("<CAPTION align=left ><font color=\"#891728\">Diagnoses</font></CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>AssetName <sp> </TH>");
            out.print("   <TH>Diagnosis <sp> </TH>");
            out.print("   <TH>PossValues</TH>");
            out.print("   <TH>D-Value</TH>");
            out.print("   <TH>DW-Value</TH>");
            //out.print("   <TH>Time</TH>");
            out.print("</TR>");
        }

        private void tableFooter(PrintWriter out) {
            
            out.print("</TABLE>");
            out.print("D-Value = The value of the Diagnosis object<p>");
            out.print("DW-Value = The value of the wrapped Diagnosis object<p>");
            out.print(CHANGEDFONT + "Denotes changed values</font><p>");
            out.print(ADDEDFONT + "Denotes newly added values</font><p>");
            out.print("<hr>");
            out.print("This debug tool intentionally displays both the value of any Diagnosis objects and");
            out.print("any DiagnosisWrapper objects so that it is broadly applicable to all situations.");
            out.print("That is, it can be used in the agent, or the node/enclave-level coordinator.");
            out.print("In general, if you see both the d-value and dw-value, they will be the same.");
            out.print("In the agent you should only see the D-value, since the objects that wrap the");
            out.print("Diagnosis objects only exist on the node and enclave.");
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
            out.print("<h2><center>DiagnosisMonitorServlet in Agent ");
            out.print(agentId.toString() + "</center></h2><p><p>\n");
            
        }
    }
    //**End of servlet class


    
}