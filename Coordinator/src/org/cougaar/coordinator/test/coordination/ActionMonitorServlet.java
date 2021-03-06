/*
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
import java.util.SortedSet;
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

import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingServiceWithPrefix;

import org.cougaar.core.adaptivity.Condition;

import org.cougaar.core.persist.NotPersistable;


/**
 *
 * Servlet provides monitoring and updating of Actions in agents & ActionsWrappers in the Coordinator.
 *
 *
 */
public class ActionMonitorServlet extends BaseServletComponent implements BlackboardClient, NotPersistable {
    
    private EventService eventService = null;
    private BlackboardService blackboard = null;
    private LoggingService log;
    
    private MessageAddress agentId = null;

    private static final String CHANGEDFONT = "<font color=\"#00ff00\">"; //green            
    private static final String ADDEDFONT = "<font color=\"#0000ff\">"; //blue            
    
    
    protected Servlet createServlet() {
        // create inner class
        return new MyServlet();
    }
    
    /** aquire services */
    public void load() {
        
        super.load();
        
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

        log = (LoggingService)
	    serviceBroker.getService(this, LoggingService.class, null);
	log = LoggingServiceWithPrefix.add(log, agentId + ": ");
        
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
        if (log != null) {
            serviceBroker.releaseService(this, LoggingService.class, log);
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
    SortedSet assetNames = Collections.synchronizedSortedSet(new TreeSet());

    /////////////////////////////////////////////////////////////Actions
    
    void handleAction(Action a, int state, boolean isWrapper) {
     
        Action current;
        ActionData actionData;
        
        //First look for actionData
        synchronized(actions) { //so 
            Iterator i = actions.iterator();
            while (i.hasNext() ) {      
                actionData = (ActionData) i.next();
                if ( actionData.contains(a) ) { //found it
		    if (log.isDebugEnabled())
			log.debug("Changed Action Data Found. Asset ="+a.getAssetName());
                    actionData.setAction(a, state, isWrapper);
                    return;
                } else {
		    if (log.isDebugEnabled())
			log.debug("Added Action Data Found. Asset = "+a.getAssetName());                    
                }                    
            }
            //Not found so create it
            actionData = new ActionData(a, state, isWrapper);
            actions.add(actionData);
            assetNames.add(a.getAssetName());
        }        
    }
    
    //These methods are called by the ActionMonitorPlugin
    public void addActionsWrapper(ActionsWrapper aw) {
        if (log.isDebugEnabled())
	    log.debug("Received added "+aw);
	handleAction((Action)aw.getContent(), ADDED, true);
	//handleAction(aw, ADDED, true);
    }
    
    public void changedActionsWrapper(ActionsWrapper aw) {
        if (log.isDebugEnabled())
	    log.debug("Received changed "+aw);
        handleAction((Action)aw.getContent(), CHANGED, true);
        //handleAction(aw, CHANGED, true);
    }
    
    public void addAction(Action a) {
        if (log.isDebugEnabled())
	    log.debug("Received added "+a);
        handleAction(a, ADDED, false);
    }
    
    public void changedAction(Action a) {
        if (log.isDebugEnabled())
	    log.debug("Received changed "+a);
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

	public String toString() {
            return
		"<ActionData"+
		" clsname="+clsname+
		" shortClsname="+shortClsname+
		" type="+type+
		" asset="+asset+
		" pValues="+pValues+
		" a="+a+
		" aw="+aw+
		" aState="+aState+
		" awState="+awState+
		">";
        }
        
        /**
         * @return asset name
         */
        String getAssetName() { return asset; }
        
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
		//a = ((ActionsWrapper)aw).getAction(); //sjf
                //aState = state;     //sjf
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
            boolean useShortName = true;
            String actiondataFilter = "WRAPPERS";
            //Default refresh rate
            int refreshRate = 30000;
            String ln="SHORTNAME";
    
            String assetFilter = "ALL";
            
            String updateResult = null;
            boolean wasUpdated = false;
            
            if (request != null) {
                
                ln = request.getParameter("NAMEFORMAT");
                if (ln != null && ln.length()>0) { 
                    if (!ln.equals("SHORTNAME")) {useShortName = false;} 
                } else { //if not specified use default
                    ln = "SHORTNAME";
                }

                actiondataFilter = request.getParameter("FILTER");
                if (actiondataFilter == null || actiondataFilter.length()==0) {
                    actiondataFilter = "WRAPPERS"; //set up value to enable default data output.
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
                

                String[] newvalues = request.getParameterValues("SETPERMITTEDVALUES");
                if (newvalues != null && newvalues.length >0 ) { //the user wants to update the permitted values
                    
                    String type = request.getParameter("TYPE");
                    String uid  = request.getParameter("UID");
                    
                    java.util.HashSet hs = new java.util.HashSet();
                    for (int i=0; i<newvalues.length; i++) {
                        hs.add(newvalues[i]);
                    }
                    updateResult = updateAction(type, uid, hs);
                    wasUpdated = true;
                }
        
                //See if the user hit "Start Action" or "Stop Action"
                String startaction = request.getParameter("STARTACTION");
                
                String af = request.getParameter("ASSETFILTER");
                if (af != null && af.length() >0 ) { 
                    assetFilter = af;                    
                }
                
                //Start an action
                if (startaction != null ) {
                
                    String actionVal = request.getParameter("ACTIONCONTROL");
                    if (actionVal != null && actionVal.length() >0 ) { //the user wants to update the value of an action

                        String type = request.getParameter("TYPE");
                        String uid  = request.getParameter("UID");

                        startAction(type, uid, actionVal, error);
                        wasUpdated = true;
                    }
                } else { 

                    String stopaction = request.getParameter("STOPACTION");
                    String failaction = request.getParameter("FAILACTION");
                    String abortaction = request.getParameter("ABORTACTION");
                    
                    //stop an action
                    if (stopaction != null ) {
                        
                        Action.CompletionCode cc = null;
                        
                        if (stopaction.equalsIgnoreCase("Stop")) {
                            cc = Action.COMPLETED; }
                        else if (stopaction.equalsIgnoreCase("Abort")) {
                            cc = Action.ABORTED; }
                        else if (stopaction.equalsIgnoreCase("Fail")) {
                            cc = Action.FAILED; }

                        if (cc != null) {
                            String type = request.getParameter("TYPE");
                            String uid  = request.getParameter("UID");

                            stopAction(type, uid, cc, error);
                            wasUpdated = true;
                        } 
                    }
                }
            } 
 
            
            response.setContentType("text/html");
            
            try {
                PrintWriter out = response.getWriter();
                emitHeader(out, refreshRate, useShortName, actiondataFilter, updateResult, ln, updateResult, wasUpdated, assetFilter );
                if (error != null) { // then emit the error
                    out.print("<font color=\"#0C15FE\">"+ error + "</h2></font>");
                }
                
                boolean e1 = emitData(out, useShortName, actiondataFilter, refreshRate, ln, assetFilter); //emit actions
                //boolean e2 = emitData(out, true); //emit wrappers
                
                if (!e1) {
                    out.println("<p><p><p><h2><center>No Data is Available.</center></h2>");
                }
                emitFooter(out, refreshRate, useShortName, actiondataFilter, updateResult, ln, updateResult, wasUpdated, assetFilter );
                out.close();
            } catch (java.io.IOException ie) { ie.printStackTrace(); }
            
        
        }

        //Set the permitted values for an object
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
			blackboard.openTransaction();
			blackboard.publishChange(a);
			blackboard.closeTransaction();
                        break;
                    }
                } else { //wrapper change
                    a = (Action)ad.getWrapper();
                    if (a.getUID().toString().equals(uid)) {
                        result = ActionUtils.setPermittedValues(a, newvalues);
			blackboard.openTransaction();
			blackboard.publishChange(a.getWrapper());
			blackboard.closeTransaction();
                        break;
                    }
                }
            }
            
            
            return result;
        }

        private void startAction(String type, String uid, String newvalue, String error) {

            ActionData ad;
            Action a;
            
            //find the action
            Iterator i = actions.iterator();
            while (i.hasNext()) {
                ad = (ActionData)i.next();
                
                if (type.equals("ACTIONS")) {
                    a = (Action)ad.getAction();
                    if (a.getUID().toString().equals(uid)) {
			try {
			    a.start(newvalue);
			    blackboard.openTransaction();
			    blackboard.publishChange(a);
			    blackboard.closeTransaction();
			} catch (IllegalValueException ive) {
			    error = "Illegal Value "+newvalue+" passed to "+a+".start()";
			}
		    }
                } else { //wrapper change
                    a = (Action)ad.getWrapper();
                    if (a.getUID().toString().equals(uid)) {
			try {
			    a.start(newvalue);
			    blackboard.openTransaction();
			    blackboard.publishChange(a.getWrapper());
			    blackboard.closeTransaction();
			} catch (IllegalValueException ive) {
			    error = "Illegal Value "+newvalue+" passed to "+a+".start()";
			}
		    }
                }
            }
        }

        //set the value of an Action
        private void stopAction(String type, String uid, Action.CompletionCode cc, String error) {

            ActionData ad;
            Action a;
            
            //find the action
            Iterator i = actions.iterator();
            while (i.hasNext()) {
                ad = (ActionData)i.next();
                
                if (type.equals("ACTIONS")) {
                    a = (Action)ad.getAction();
                    if (a.getUID().toString().equals(uid)) {
			try {
			    a.stop(cc);
			    blackboard.openTransaction();
			    blackboard.publishChange(a);
			    blackboard.closeTransaction();
			} catch (NoStartedActionException nsae) {
			    error = "No Started Action for "+a;
			} catch (IllegalValueException ive) {
			    error = "Illegal Completion Code "+cc+" passed to "+a+".stop()";
			}
		    }                        
                } else { //wrapper change
                    a = (Action)ad.getWrapper();
                    if (a.getUID().toString().equals(uid)) {
			try {
			    a.stop(cc);
			    blackboard.openTransaction();
			    blackboard.publishChange(a.getWrapper());
			    blackboard.closeTransaction();
			} catch (NoStartedActionException nsae) {
			    error = "No Started Action for "+a;
			} catch (IllegalValueException ive) {
			    error = "Illegal Completion Code "+cc+" passed to "+a+".stop()";
			}
                    }
                }
            }
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitHeader(PrintWriter out, int refresh, boolean useShortName, 
                                String actiondataFilter, String updateResult, String nameformat,
                                String updateResponse, boolean wasUpdated, String assetFilter ) {
            
            out.println("<html><META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\"> ");
            out.println("<script type=\"text/javascript\">");
            out.println("function refreshPage(){");   
            out.println("     var szURL = \"?REFRESH="+refresh+"&NAMEFORMAT="+nameformat+"&FILTER="+actiondataFilter+" \";");
            out.println("     self.location.replace(szURL);");
            out.println("}</script>");
            out.println("<head></head><body onload=\"setTimeout('refreshPage()', " +refresh+");\">");
            out.println("<center><h1>Coordinator Action Monitoring Servlet</h1>");
            out.println("<p>Will refresh every " + (refresh/1000) + " seconds. ");
            out.println("You can change this rate at the bottom of the page");
            out.println("<a href=\"PublishServlet\">Publish Actions</a>");
            out.println("<a href=\"ActionMonitorServlet\">Actions</a>");
            out.println("<a href=\"DiagnosisMonitorServlet\">Diagnoses</a>");
            if (wasUpdated) {
                if (updateResult == null) {
                   ///// out.println("<p><b>Object Updated.</b><p>");
                } else {
                    out.println("<p><b>Update Error: "+updateResult+"</b><p>");
                }
            }
            out.println("</center><hr>");
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitFooter(PrintWriter out, int refresh, boolean useShortName, 
                                String actiondataFilter, String updateResult, String nameformat,
                                String updateResponse, boolean wasUpdated, String assetFilter ) {           
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
            out.println("   <input type=hidden name=\"ASSETFILTER\" value=\""+assetFilter+"\" ><br>");
            
            out.println("<input type=submit name=\"Submit\" value=\"Submit\" size=10 ><br>");
            out.println("\n</form>");
            out.println("</html>");
        }
        
        /** Emit data for the given CostBenefitAction vector
         *
         */
        private boolean emitData(PrintWriter out, boolean useShortName, String actiondataFilter, int refresh, String nameformat, String assetFilter) {
            
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

            boolean filterAll = false;
            if (assetFilter.equals("ALL")) { filterAll = true; }
                        
            tableHeader(out, refresh, nameformat, actiondataFilter, assetFilter);
            
            boolean twoRows = actiondataFilter.equals("BOTH");
            //Print out each action
            while (i.hasNext()) {
                
                //Get action record
                ad = (ActionData)i.next();

                //Filter if user selected a specific asset, o.w. print all
                if (!filterAll) {
                    if (!assetFilter.equals(ad.getAssetName())) {
                        continue; //skip printing this one out.
                    }
                }
                out.print("<TR>\n");

                //Output the asset name
                out.print("   <TD " + (twoRows ? "ROWSPAN=2":"") + ">"
			  +( (ad.getAction() == null) 
			     ? ad.getWrapper().getAssetName()
			     : ad.getAction().getAssetName()
			     )
			  +"</TD>\n");
                
                //Output the action class name
                out.print("   <TD " + (twoRows ? "ROWSPAN=2":"") + ">"+ ad.getName(useShortName) +"</TD>\n");
                emitActionData(out, ad, actiondataFilter, refresh, nameformat, assetFilter);
                
                //End of row
                if (!twoRows)
		    out.print("</TR>");
            }
            
            tableFooter(out);

        
            return emittedData;
        }
        
        
        private void emitActionData(PrintWriter out, ActionData ad, String what, int refresh, String nameformat, String assetFilter) {

            boolean twoRows = what.equals("BOTH");  
            
            //emit the action object data
            if (what.equals("BOTH") || what.equals("ACTIONS")) {
                
                emitActionDataItem(out, ad, true, twoRows, what, refresh, nameformat, assetFilter);
                
            }

            if (twoRows) { //then end the first row, and start the next
                out.print("</TR>");    
                out.print("<TR>");    
            }
            
            //now emit the wrapper data
            if (what.equals("BOTH") || what.equals("WRAPPERS")) {
                
                emitActionDataItem(out, ad, false, twoRows, what, refresh, nameformat, assetFilter);
                
            }
            //sjf out.print("</TD>");    
            out.print("</TR>");    
        }
        
        private void emitActionDataItem(PrintWriter out, ActionData ad, boolean isActionObject, boolean twoRows, 
                                        String actiondataFilter, int refresh, String nameformat, String assetFilter) {

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
            
            emitSelect(out, action.getPossibleValues());
            emitModifiableSelect(out, action.getValuesOffered(), ad, isActionObject, actiondataFilter, refresh, nameformat, "SETPERMITTEDVALUES", "Set Permitted", true, assetFilter);

	    emitActionControl(out, action.getPermittedValues(), ad, isActionObject, actiondataFilter, refresh, nameformat, assetFilter);
            
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
                                          String actiondataFilter, int refresh, String nameformat, String setName, 
                                          String buttonStr, boolean selectMultiple, String assetFilter) {
                       
            String str;
            if (s != null && s.size() >0 ) {
                Iterator iter = s.iterator();
                out.print("    <TD><form clsname=\"UPDATEVALUE\" method=\"get\" ><br><br>" );
                out.print("    <SELECT " + (selectMultiple ? "MULTIPLE" : "") + " NAME=\""+setName+"\" size=\"3\" ");
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
                out.println("   <input type=hidden name=\"FILTER\" value=\""+actiondataFilter+"\" >");
                out.println("   <input type=hidden name=\"ASSETFILTER\" value=\""+assetFilter+"\" >");
                out.println("   <br><input type=submit name=\"Submit\" value=\"" + buttonStr + "\" size=15 ");
                out.println("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
                out.println("   \n</form></TD>");
            } else { //no values
                out.print("   <TD>Null</TD>\n");
            }
        }         

        private void emitActionControl(PrintWriter out, Set s, ActionData ad, boolean isActionObject, 
                                          String actiondataFilter, int refresh, String nameformat, String assetFilter) {
                       
            String str;
            if (s != null && s.size() >0 ) {
                Iterator iter = s.iterator();
                out.print("    <TD><form clsname=\"UPDATEVALUE\" method=\"get\" ><br><br>" );
                out.print("    <SELECT  NAME=\"ACTIONCONTROL\" size=\"3\" ");
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
                out.println("   <input type=hidden name=\"FILTER\" value=\""+actiondataFilter+"\" >");
                out.println("   <input type=hidden name=\"ASSETFILTER\" value=\""+assetFilter+"\" >");
                out.println("<br><input type=submit name=\"STARTACTION\" value=\"Start\" size=15 ");
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
                out.println("   <input type=submit name=\"STOPACTION\" value=\"Stop\" size=15 ");
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
                out.println("   <input type=submit name=\"STOPACTION\" value=\"Fail\" size=15 ");
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
                out.println("   <input type=submit name=\"STOPACTION\" value=\"Abort\" size=15 ");
                out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
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
        
        private void tableHeader(PrintWriter out, int refresh, String nameformat, String actiondataFilter, String assetFilter) {

            out.print("<h2><font color=\"#891728\">Actions</font></h2>");            
            generateAssetSelect(out, refresh, nameformat, actiondataFilter, assetFilter);
            
            out.print("<p><p><TABLE cellspacing=\"20\">");
            //out.print("<CAPTION align=left ><font color=\"#891728\">Actions</font></CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>AssetName <sp> </TH>");
            out.print("   <TH>Action <sp> </TH>");
            out.print("   <TH>Type</TH>");
            out.print("   <TH>Poss. Values</TH>");
            out.print("   <TH>Offered</TH>");
            out.print("   <TH>Permitted</TH>");
            out.print("   <TH>Value / Completed?</TH>");
            out.print("   <TH>BB Status</TH>");
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
        
        
        /*
         * Generate a select list of all assets, allowing the user to select which 
         * assets to display.
         */
        private void generateAssetSelect(PrintWriter out, int refresh, String nameformat, String actiondataFilter, String assetFilter) {
            out.print("    <form clsname=\"ASSETFILTER\" method=\"get\" >" );
            out.println("    <input type=submit name=\"Submit\" value=\"Show Asset:\" size=15 ");
            out.println("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: blue; font-size: 6pt;\">");
            out.print("    <SELECT  NAME=\"ASSETFILTER\" size=\"1\" ");
            out.print("  STYLE=\"margin: 0em 0 0 0em; color: white; background-color: red; font-size: 8pt;\">\n");            
            Iterator iter = assetNames.iterator();
            String str;
            while (iter.hasNext()) {
                str = iter.next().toString();
                out.print("        <OPTION value=\""+ str +"\" "+(assetFilter.equals(str)?"":"UN")+"SELECTED />" + str + "\n");
            }
            out.print("        <OPTION value=\"ALL\" "+(assetFilter.equals("ALL")?"":"UN")+"SELECTED />ALL\n");
            out.print("   </SELECT>\n");            
	    out.println("   <input type=hidden name=\"REFRESH\" value=\""+refresh+"\" >");
	    out.println("   <input type=hidden name=\"NAMEFORMAT\" value=\""+nameformat+"\" >");
	    out.println("   <input type=hidden name=\"FILTER\" value=\""+actiondataFilter+"\" >");
            out.println("   \n</form>");
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
    
}
