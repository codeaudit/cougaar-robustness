/*
 * ActionMonitorServlet.java
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
 * Servlet allows monitoring action objects on the node & ActionsWrapper objects on the enclave coordinator.
 *
 *
 */
public class ActionMonitorServlet extends BaseServletComponent implements BlackboardClient, NotPersistable {
    
    //private String LONG_NUM_FORMAT = "###0.####";
    //private DecimalFormat nf = new DecimalFormat(LONG_NUM_FORMAT);
    private EventService eventService = null;
    private BlackboardService blackboard = null;
    private Logger logger = null;
    
    private MessageAddress agentId = null;

    
    private static final String CHANGEDFONT = "<font color=\"#00ff00\">"; //green            
    private static final String ADDEDFONT = "<font color=\"#0000ff\">"; //blue            

    
    
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
        return "/ActionMonitorServlet";
    }
    

    Object changes = null; // used to synchronize vector changes
    Vector actions = new Vector();

    /////////////////////////////////////////////////////////////Actions
    
    void handleAction(Action a, int state, boolean isWrapper) {
     
        Action current;
        ActionData actionData;
        
        //First look for actionData
        Iterator i = actions.iterator();
        //synchronized(changes) { //so 
            while (i.hasNext() ) {      
                actionData = (ActionData) i.next();
                if ( actionData.contains(a) ) { //found it
System.out.println("Changed Action Data Found. Asset ="+a.getAssetName());                    
                    actionData.setAction(a, state, isWrapper);
                    return;
                } else {
System.out.println("Added Action Data Found. Asset = "+a.getAssetName());                    
                }                    
            }
            //Not found so create it
            actionData = new ActionData(a, state, isWrapper);
            actions.add(actionData);
        //}        
    }
    
    //These methods are called by the ActionMonitorPlugin
    public void addActionsWrapper(ActionsWrapper aw) {
        handleAction((Action)aw.getContent(), ADDED, true);
    }
    
    public void changedActionsWrapper(ActionsWrapper aw) {
        handleAction((Action)aw.getContent(), CHANGED, true);
    }

    public void addAction(Action a) {
        handleAction(a, ADDED, false);
    }
    
    public void changedAction(Action a) {
        handleAction(a, CHANGED, false);
    }

    
    ///////////////////////////////////////////////////////////////////////

    
        
    private final static int NEITHER = 0;
    private final static int CHANGED = 1;
    private final static int ADDED = 2;

    private class ActionData {
     
        String clsname;
        String shortClsname;
        AssetType type;
        String asset;
        Set pValues;
        
        Action a;
        Action aw; //wrapped originally
        int aState = 0;
        int awState = 0;
        
        public ActionData(Action t, int state, boolean isWrapper) {

            setAction(t, state, isWrapper);
            
            //Get the identifying properties of this Action
            clsname = t.getClass().getName();
            type = ActionUtils.getAssetType(t);
            asset= t.getAssetName();
            shortClsname = setShortName(clsname);
            pValues = t.getPossibleValues();
        }
        
        /**
         * @return the class name of the Action without the pkg name
         */
        String setShortName(String s) {
         
            return s.substring(s.lastIndexOf('.')+1);
            
        }

        /**
         * @return the class name of the Action without the pkg name. If shortNm = TRUE, return the 
         * class name without the pkg prepended.
         */
        String getName(boolean shortNm) {
         
            return shortNm ? this.shortClsname : clsname;
            
        }
        
        Action getAction() { return a; }
        Action getWrapper() { return aw; }

        void setAction(Action t, int state, boolean isWrapper) { 
            
            if (isWrapper) {
                aw = t;
                awState = state;
            } else {
                a = t;
                aState = state;
            }
        }
        
        int  getWrapperState() { return awState; }        
        int  getActionState() { return aState; }


        
        /**
         * @return the possible values for this action
         */
        Set getPossibleValues() { return pValues; }
        
        /**
         * @return TRUE if the ActionData is specific to this Action class, asset type and asset name.
         */
        boolean contains(Action aa) {
            
           return (aa.getClass().getName().equals(clsname) && 
                   ActionUtils.getAssetType(aa).equals(type) && 
                   aa.getAssetName().equals(asset) );
        }
        
    }
    
    //////////////////////////////////////////////////////////////////////////////////
    
    private class MyServlet extends HttpServlet {
        
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
            String refresh = null;
            String error = null;
            boolean useShortName = false;
            String actiondataFilter = "ACTIONS";
            //Default refresh rate
            int refreshRate = 50000;
            String ln="SHORTNAME";
            
            String updateResult = null;
            boolean wasUpdated = false;
            
            if (request != null) {
                
                ln = request.getParameter("NAMEFORMAT");
                if (ln != null && ln.length()>0) { 
                    if (ln.equals("SHORTNAME")) {useShortName = true;} 
                } else { //if not specified use default
                    ln = "SHORTNAME";
                }

                actiondataFilter = request.getParameter("FILTER");
                if (actiondataFilter == null || actiondataFilter.length()==0) {
                    actiondataFilter = "ACTIONS"; //set up value to enable default data output.
                }
                
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
                
                String[] newvalues = request.getParameterValues("NEWVALUE");
                if (newvalues != null && newvalues.length >0 ) { //the user wants to update a value
                    
                    System.out.println("*************** newvalue = " + newvalues);
                    
                    String type = request.getParameter("TYPE");
                    String uid  = request.getParameter("UID");
                    
                    java.util.HashSet hs = new java.util.HashSet();
                    for (int i=0; i<newvalues.length; i++) {
                        hs.add(newvalues[i]);
                    }
                    updateResult = updateAction(type, uid, hs);
                    wasUpdated = true;
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
                emitHeader(out, refreshRate, useShortName, actiondataFilter, updateResult, ln, updateResult, wasUpdated );
                if (error != null) { // then emit the error
                    out.print("<font color=\"#0C15FE\">"+ error + "</h2></font>");
                }
                
                boolean e1 = emitData(out, useShortName, actiondataFilter, refreshRate, ln); //emit actions
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

        
        private String updateAction(String type, String uid, Set newvalues) {

            ActionData ad;
            Action a;
            
            String result = null;
            //find the action
            Iterator i = actions.iterator();
            while (i.hasNext()) {
                ad = (ActionData)i.next();
                
                if (type.equals("ACTIONS")) {
                    a = (Action)ad.getAction();
                    if (a.getUID().toString().equals(uid)) {
                        result = ActionUtils.setPermittedValues(a, newvalues);
                        break;
                    }
                } else { //wrapper change
                    a = (Action)ad.getWrapper();
                    if (a.getUID().toString().equals(uid)) {
                        result = ActionUtils.setPermittedValues(a, newvalues);
                        break;
                    }
                }
            }
            
            
            return result;
        }
        
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitHeader(PrintWriter out, int refresh, boolean useShortName, 
                                String actiondataFilter, String updateResult, String nameformat,
                                String updateResponse, boolean wasUpdated ) {
            
            out.println("<html>");
            out.println("<script type=\"text/javascript\">");
            out.println("function refreshPage(){");   
            out.println("     var szURL = \"?REFRESH="+refresh+"&NAMEFORMAT="+nameformat+"&FILTER="+actiondataFilter+" \";");
            out.println("     self.location.replace(szURL);");
            out.println("}</script>");
            out.println("<head></head><body onload=\"setTimeout('refreshPage()', " +refresh+");\">");
            out.println("<center><h1>Coordinator Action Monitoring Servlet</h1>");
            out.println("<p>Will refresh every " + (refresh/1000) + " seconds. ");
            out.println("You can change this rate here: (in milliseconds)");
            out.print("<form clsname=\"myForm\" method=\"get\" >" );
            out.println("Refresh Rate: <input type=text name=REFRESH value=\""+refresh+"\" size=7 >");

            out.println("<br><b>Action Name - use:</b><SELECT NAME=\"NAMEFORMAT\" SIZE=\"1\">");
            out.println("<OPTION VALUE=\"LONGNAME\" "+(!useShortName?"SELECTED":"")+" />Pkg Name");
            out.println("<OPTION VALUE=\"SHORTNAME\" "+(useShortName?"SELECTED":"")+"/>Class name");
            out.println("</SELECT>");

            //String assets = if (filter.equals(ASSETS))SELECTED 
            out.println("<br><b>Include:</b><SELECT NAME=\"FILTER\" SIZE=\"1\">");
            out.println("<OPTION VALUE=\"ACTIONS\" "+((actiondataFilter!=null&&actiondataFilter.equals("ACTIONS"))?"SELECTED":"")+"/>Only Actions");
            out.println("<OPTION VALUE=\"WRAPPERS\" "+((actiondataFilter!=null&&actiondataFilter.equals("WRAPPERS"))?"SELECTED":"")+"/>Only Wrappers");
            out.println("<OPTION VALUE=\"BOTH\" "+((actiondataFilter==null ||actiondataFilter.equals("BOTH"))?"SELECTED":"")+" />Both");
            out.println("</SELECT>");
            
            out.println("<input type=submit name=\"Submit\" value=\"Submit\" size=10 ><br>");
            out.println("\n</form>");
            out.println("<a href=\"PublishServlet\">Publish Actions</a>");
            out.println("<a href=\"ActionMonitorServlet\">Actions</a>");
            out.println("<a href=\"DiagnosisMonitorServlet\">Diagnoses</a>");
            if (wasUpdated) {
                if (updateResult == null) {
                    out.println("<p><b>Object Updated.</b><p>");
                } else {
                    out.println("<p><b>Update Error: "+updateResult+"</b><p>");
                }
            }
            out.println("</center><hr>");
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitFooter(PrintWriter out) {
            out.println("</html>");
        }
        
        /** Emit data for the given CostBenefitAction vector
         *
         */
        private boolean emitData(PrintWriter out, boolean useShortName, String actiondataFilter, int refresh, String nameformat) {
            
            boolean emittedData = false;
            Action a; //action
            Action aw; //wrapper
            ActionData ad;
            
            Object av; //store value of action
            Object awv; //store value of wrapped action

            Iterator i = actions.iterator();
            if (i.hasNext()) {
                emittedData = true;
            } else { return emittedData; }

            
            tableHeader(out);
            
            boolean twoRows = actiondataFilter.equals("BOTH");
            //Print out each action
            while (i.hasNext()) {

                out.print("<TR>\n");

                //Get action record
                ad = (ActionData)i.next();
                
                //Output the asset name
                out.print("   <TD " + (twoRows ? "ROWSPAN=2":"") + ">"+ ad.getAction().getAssetName() +"</TD>\n");
                
                //Output the action class name
                out.print("   <TD " + (twoRows ? "ROWSPAN=2":"") + ">"+ ad.getName(useShortName) +"</TD>\n");

                
                emitActionData(out, ad, actiondataFilter, refresh, nameformat);
                
                //End of row
                if (!twoRows) {
                    out.print("</TR>");
                }
            }
            
            tableFooter(out);

        
            return emittedData;
        }
        
        
        private void emitActionData(PrintWriter out, ActionData ad, String what, int refresh, String nameformat) {

            boolean twoRows = what.equals("BOTH");  
            
            //emit the action object data
            if (what.equals("BOTH") || what.equals("ACTIONS")) {
                
                emitActionDataItem(out, ad, true, twoRows, what, refresh, nameformat);
                
            }

            if (twoRows) { //then end the first row, and start the next
                out.print("</TR>");    
                out.print("<TR>");    
            }
            
            //now emit the wrapper data
            if (what.equals("BOTH") || what.equals("WRAPPERS")) {
                
                emitActionDataItem(out, ad, false, twoRows, what, refresh, nameformat);
                
            }
            out.print("</TD>");    
        }
        
        private void emitActionDataItem(PrintWriter out, ActionData ad, boolean isActionObject, boolean twoRows, 
                                        String actiondataFilter, int refresh, String nameformat) {

            Object av;
            Object awv;
            Action a, aw;

            Action action;
            Object lastActionValue;
            Object prevActionValue;
            
            //Emit object type
            if (isActionObject) {
                out.print("   <TD>Action</TD>\n");    
                action = ad.getAction();
            } else {
                out.print("   <TD>Wrapper</TD>\n");    
                action = ad.getWrapper();
            }                

            if (action == null) { 
                out.print("   <TD>NULL</TD>");
                return; 
            }
            
            emitModifiableSelect(out, action.getPossibleValues(), ad, isActionObject, actiondataFilter, refresh, nameformat);
            emitSelect(out, action.getPermittedValues());
            emitSelect(out, action.getValuesOffered());
            
            if ( isActionObject ) { //emit value indicating if data has changed or was just added.
                out.print("   <TD><TABLE>");
                out.print("   "+ emitActionValue(action.getValue(), "<b>last:</b>"));    //last Action Value
                out.print("   "+ emitActionValue(action.getPreviousValue(), "<b>prev:</b>"));    //prev Action Value
                out.print("   </TABLE></TD>");
                out.print("   <TD>"+getStatusString(ad.getActionState())+"</TD>");
            } else { //emit wrapper data
                out.print("   <TD><TABLE>");
                out.print("   "+ emitActionValue(action.getValue(), "<b>last:</b>"));    //last Wrapper Value
                out.print("   "+ emitActionValue(action.getPreviousValue(), "<b>prev:</b>"));    //prev Wrapper Value
                out.print("   </TABLE></TD>");
                out.print("   <TD>"+getStatusString(ad.getWrapperState())+"</TD>");
            }
        }

        private void emitSelect(PrintWriter out, Set s) {
                       
            String str;
            if (s != null && s.size() >0 ) {
                Iterator iter = s.iterator();
                out.print("   <TD><SELECT size=\"3\" ");            
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: red; font-size: 8pt;\">\n");            
                while (iter.hasNext()) {
                    str = iter.next().toString();
                    out.print("        <OPTION value=\""+ str +"\" />" + str + "\n");
                }
                out.print("   </SELECT></TD>\n");            
            } else { //no values
                out.print("   <TD>Null</TD>\n");
            }
        }         


        private void emitModifiableSelect(PrintWriter out, Set s, ActionData ad, boolean isActionObject, 
                                          String actiondataFilter, int refresh, String nameformat) {
                       
            String str;
            if (s != null && s.size() >0 ) {
                Iterator iter = s.iterator();
                out.print("    <TD><form clsname=\"UPDATEVALUE\" method=\"get\" ><br><br>" );
                out.print("    <SELECT MULTIPLE NAME=\"NEWVALUE\" size=\"3\" ");
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: red; font-size: 8pt;\">\n");            
                while (iter.hasNext()) {
                    str = iter.next().toString();
                    out.print("        <OPTION value=\""+ str +"\" UNSELECTED />" + str + "\n");
                }
                out.print("   </SELECT>\n");            
                out.println("   <input type=hidden name=\"TYPE\" value=\""+(isActionObject?"ACTIONS":"WRAPPERS")+"\" >");
                out.println("   <input type=hidden name=\"UID\" value=\""+(isActionObject?ad.getAction().getUID():ad.getWrapper().getUID())+"\" >");
                out.println("   <input type=hidden name=\"REFRESH\" value=\""+refresh+"\" >");
                out.println("   <input type=hidden name=\"NAMEFORMAT\" value=\""+nameformat+"\" >");
                out.println("   <input type=hidden name=\"FILTER\" value=\""+actiondataFilter+"\" ><br>");
                out.print("   <input type=submit name=\"Submit\" value=\"Set Permitted\" size=15 ");
                out.println("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
                out.println("   \n</form></TD>");
            } else { //no values
                out.print("   <TD>Null</TD>\n");
            }
        }         
        
        /**
         * Emit the action value & completion code 
         */
        private String emitActionValue(ActionRecord ar, String s) {
            if (ar != null) {
                return "<TR><TD>" + s + ar.getAction() + "</TD><TD>" + ar.getCompletionCodeString() + "</TD></TR>";
            } else {
                return "<TR><TD>"+s+"null</TD><TD>N/A</TD></TR>";
            }
        }
        

        /**
         * Emit the action value as a table item and colorize the action value based upon whether it has changed, 
         * or was recently added.
         */
        private String getStatusString(int state) {
        
            if (state == CHANGED)  {
                return CHANGEDFONT + "Changed</font>";
            }
            else if (state == ADDED) {
                return ADDEDFONT + "Added</font>";
            }
            else { //no font annotation
                return ""; 
            }            
        }
        
        private void tableHeader(PrintWriter out) {
            
            out.print("<p><p><TABLE cellspacing=\"20\">");
            //out.print("<CAPTION align=left ><font color=\"#891728\">Actions</font></CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>AssetName <sp> </TH>");
            out.print("   <TH>Action <sp> </TH>");
            out.print("   <TH>Type</TH>");
            out.print("   <TH>Poss. Values</TH>");
            out.print("   <TH>Permitted</TH>");
            out.print("   <TH>Offered</TH>");
            out.print("   <TH>Value / Completed?</TH>");
            out.print("   <TH>Status</TH>");
            //out.print("   <TH>Time</TH>");
            out.print("</TR>");
        }

        private void tableFooter(PrintWriter out) {
            
            out.print("</TABLE><hr>");
            out.print("Value = The value of the Action object<p>");
            out.print(CHANGEDFONT + "Denotes changed values</font><p>");
            out.print(ADDEDFONT + "Denotes newly added values</font><p>");
            out.print("<hr>");
            out.print("This debug tool intentionally displays both the value of any Action objects and");
            out.print("any ActionWrapper objects so that it is broadly applicable to all situations.");
            out.print("That is, it can be used in the agent, or the node/enclave-level coordinator.");
            out.print("In general, if you see both the A-value and AW-value, they will be the same.");
            out.print("In the agent you should only see the A-value, since the objects that wrap the");
            out.print("Action objects only exist on the node and enclave.");
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
            out.print("<h2><center>ActionMonitorServlet in Agent ");
            out.print(agentId.toString() + "</center></h2><p><p>\n");
            
        }
    }
    //**End of servlet class


    
}
