/*
 * PublishServlet.java
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
import org.cougaar.coordinator.test.defense.*;

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
import org.cougaar.core.util.UID;

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
public class PublishServlet extends BaseServletComponent implements BlackboardClient, NotPersistable {
    
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

        //call tech spec service & add action tech spec        
        // **********************************************get the tect spec service
        ActionTechSpecService ActionTechSpecService =
                (ActionTechSpecService) serviceBroker.getService(this, ActionTechSpecService.class, null);
        if (ActionTechSpecService == null) {
            logger.warn("Unable to obtain tech spec service");
        } else {
            ActionTechSpecService.addActionTechSpec("org.cougaar.coordinator.test.coordination.TestAction", new TestActionTechSpec());
        }
        //call tech spec service & add diagnosis tech spec        
        // **********************************************get the tect spec service
        DiagnosisTechSpecService DiagnosisTechSpecService =
                (DiagnosisTechSpecService) serviceBroker.getService(this, DiagnosisTechSpecService.class, null);
        if (DiagnosisTechSpecService == null) {
            logger.warn("Unable to obtain tech spec service");
        } else {
            DiagnosisTechSpecService.addDiagnosisTechSpec("org.cougaar.coordinator.test.coordination.TestDiagnosis", new TestDiagnosisTechSpec());
        }
        
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
        return "/PublishServlet";
    }
    
    private String createAndPublishTestObjects(String assetname) {
        
        String error = null;
        //Publish the Action and Diagnosis test objects
        blackboard.openTransaction();
        try {
            createDiagnoses(assetname); 
        } catch (Exception e) {
            error = e.toString();
        }

        try {
            Action a = createAction(assetname);    
            blackboard.publishAdd(a);
            logger.debug("**** Saw new Action["+ActionUtils.getAssetID(a)+" UID=" + a.getUID());
        } catch (Exception e) {
            error = error + "\n" + e.toString();
        }
        blackboard.closeTransaction();
        return error;
    }        

    
    private void createDiagnoses(String s) throws IllegalValueException, TechSpecNotFoundException {
        AgentCommunicationDiagnosis1 d1 = new AgentCommunicationDiagnosis1(s, "OK", serviceBroker);
        DiagnosesWrapper dw1 = new DiagnosesWrapper(d1, null, null, new UID());
        d1.setWrapper(dw1);
        blackboard.publishAdd(d1);
        blackboard.publishAdd(dw1);

        AgentCommunicationDiagnosis2 d2 = new AgentCommunicationDiagnosis2(s, "OK", serviceBroker);
        DiagnosesWrapper dw2 = new DiagnosesWrapper(d2, null, null, new UID());
        d2.setWrapper(dw2);
        blackboard.publishAdd(d2);
        blackboard.publishAdd(dw2);

        logger.debug("**** Published Diagnoses for: "+s);
        return;
    }
    
    private Action createAction(String s) throws IllegalValueException, TechSpecNotFoundException {
        TestAction a = new TestAction(s, serviceBroker);        
        return a;
    }
    
    private class MyServlet extends HttpServlet {
        
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
            String error = null;
            String assetname = null;
            if (request != null) {
                
                assetname = request.getParameter("ASSETNAME");
                if (assetname != null) {
                    error = createAndPublishTestObjects(assetname);                    
                }
                
            }
            response.setContentType("text/html");
            
            try {
                PrintWriter out = response.getWriter();

                emitHeader(out);
                if (assetname != null) { emitOK(out, error); }
                emitError(out, error);
                emitFooter(out);
                out.close();
            } catch (java.io.IOException ie) { ie.printStackTrace(); }
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitHeader(PrintWriter out) {
            out.println("<html><META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\">");
            out.println("<head></head><body>");
            out.println("<center><h1>Coordinator - Publish Diagnosis and Actions Servlet</h1></center><hr>");

            //Form for creating a Action & Diagnosis
            out.print("<form clsname=\"myForm\" method=\"get\" >" );
            out.println("Create Action & Diagnosis objects:");
            out.println("for Asset name = <input type=text name=\"ASSETNAME\" value=\"nameOfAsset\" size=12 >");
            out.println("<input type=submit name=\"Submit\" value=\"Create\" size=10 ><br>");            
            out.println("\n</form>");            
            out.println("<a href=\"PublishServlet\">Publish Actions</a>");
            out.println("<a href=\"ActionMonitorServlet\">Actions</a>");
            out.println("<a href=\"DiagnosisMonitorServlet\">Diagnoses</a>");
        }


        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void emitFooter(PrintWriter out) {
            out.println("</html>");
        }

        /**
         */
        private void emitError(PrintWriter out, String errors) {
            if (errors == null) { return; }
            out.println("<hr>");
            out.println("<h2>Saw Errors:</h2>");
            out.println(errors);
            out.println("<hr>");
            
        }

        /**
         */
        private void emitOK(PrintWriter out, String errors) {
            if (errors != null) { return; }
            out.println("<hr>");
            out.println("<h2>Objects published.</h2>");
            out.println("<hr>");
            
        }
        
    }
    //**End of servlet class


    
}
