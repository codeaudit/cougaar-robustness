/*
 * ThreatModelServlet.java
 *
 * Created on September 16, 2003, 10:57 AM
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

package org.cougaar.coordinator.techspec;

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
import org.cougaar.core.blackboard.SubscriberException;

import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;

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
public class ThreatModelServlet extends BaseServletComponent
implements BlackboardClient {
    
    private static final String THREAT_PARAM = "threat";
    private static final String INDEX_PARAM = "index";
    private static final String PROB_PARAM = "prob";
    private static final String LIKELIHOOD_PARAM = "likelihood";
    
    private UIDService uidService = null;
    private EventService eventService = null;
    private BlackboardService blackboard = null;
    private Logger logger = null;
    
    private MessageAddress agentId = null;
    
    private Collection threatModels = null;
    
    
    protected Servlet createServlet() {
        // create inner class
        return new MyThreatModelServlet();
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
        return "/DeconflictionThreatModelServlet";
    }
    
    private class MyThreatModelServlet extends HttpServlet {
        
        public void doGet(HttpServletRequest request, HttpServletResponse response) {

            response.setContentType("text/html");

            handleUpdates(request);
            
            findThreatModels();
            
            try {
                PrintWriter out = response.getWriter();
                sendData(out);
                out.close();
            } catch (java.io.IOException ie) { ie.printStackTrace(); }
        }

        private void handleUpdates(HttpServletRequest request) {
            
            logger.debug("=======> Query string is: " + request.getQueryString());
            String updateError = null;
            //See if there is an update request to modify a threatModel
            String threat = request.getParameter(THREAT_PARAM);
            String likelihoodChange = request.getParameter(LIKELIHOOD_PARAM);
            if (threat != null) { //then it's more likely that the remaining values are non-null
                String index = request.getParameter(INDEX_PARAM);
                String prob = request.getParameter(PROB_PARAM);

                if (index != null && prob != null) {
                    logger.debug("=======> Calling updateThreatModel()");
                    updateError = updateThreatModel(threat, index, prob);
                }
                if (updateError != null) {
                    if (eventService.isEventEnabled()) {
                        eventService.event("ERROR: Threat Model Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                        logger.warn("ERROR: Threat Model Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.warn("ERROR: Threat Model Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                    }
                }

            } else if (likelihoodChange != null) {
                String index = request.getParameter(INDEX_PARAM);
                String prob = request.getParameter(PROB_PARAM);

                if (index != null && prob != null) {
                    logger.debug("=======> Calling updateThreatLikelihood()");
                    updateError = updateThreatLikelihood(likelihoodChange, index, prob);
                }
                if (updateError != null) {
                    if (eventService.isEventEnabled()) {
                        eventService.event("ERROR: ThreatLikelihood Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                        logger.warn("ERROR: ThreatLikelihood Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.warn("ERROR: ThreatLikelihood Not Updated: Threat=" + threat + " index=" + index + " prob="+prob+ "\nError was: "+updateError);
                    }
                }
            }            
            
        }
        
        /**
         * Output page with disconnect  / reconnect button & reconnect time slot
         */
        private void sendData(PrintWriter out) {
            out.println("<html><head></head><body>");

            //Output threat model data
            if (threatModels == null || threatModels.size() == 0) {
                out.println("<h2>No Threat Models Found.</h2>");    
            } else {
                Iterator models = threatModels.iterator();
                while (models.hasNext()) {
                    DefaultThreatModel dtm = (DefaultThreatModel)models.next();
                    writeThreatGUI(dtm, out);
                }
            }
            
        }
        
        private void writeThreatGUI(DefaultThreatModel dtm, PrintWriter out) {
            
            out.print("<font color=\"#0C15FE\"><h2>Threat: "+ dtm.getName() + "</h2></font>");
            out.print("<p>ThreatType: "+ dtm.getThreatType().getName()); 
            out.print("<p>AssetType: "+ dtm.getAssetType().getName()); 

            // Asset List -----------------------
            out.print("<p><font color=\"#891728\">Assets in Membership: </font><p> <SELECT size=\"5\" >");            
            AssetTechSpecInterface atsi;
            Vector assets = dtm.getAssetList();
            Iterator ai = assets.iterator();
            while (ai.hasNext()) {
                atsi = (AssetTechSpecInterface)ai.next();
                out.print("<OPTION>"+ atsi.getName() +"</OPTION>");
            }
            out.print("</SELECT>");            

            // ThreatLikelihood Vector ----------------------
            out.print("<p><TABLE>");
            out.print("<CAPTION align=left ><font color=\"#891728\"> ThreatLikelihood Vector </font></CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>Period</TH>");            
            out.print("   <TH>Inverval(in msec)</TH>");            
            out.print("   <TH>Probability</TH>");            
            out.print("</TR>");
            
            ThreatLikelihoodInterval[] likelihoods = dtm.getLikelihoodIntervals();
            if (likelihoods != null && likelihoods.length > 0) {
                for (int j=0; j<likelihoods.length; j++) {
                    out.print("<TR>");
                    out.print("   <TD>"+likelihoods[j].getApplicabilityInterval().toString() +"</TD>");            
                    out.print("   <TD>"+likelihoods[j].getIntervalLength()+"</TD>");            
                    out.print("   <TD align=center>");            
                    out.print("          <form method=\"get\" align=bottom>");
                    out.print("            <input type=\"hidden\" name=\"likelihood\" value=\""+dtm.getName()+"\" >");
                    out.print("            <input type=\"hidden\" name=\"index\" value=\""+j+"\"             >");
                    out.print("            <input type=\"text\" name=\"prob\" value=\""+likelihoods[j].getProbability() +"\" >");
                    out.print("            <input type=\"submit\" value=\"Update\">");
                    out.print("          </form>");
                    out.print("   </TD>");            
                    out.print("</TR>");
                }            
                out.print("</TABLE>");
            }
            
            // Distribution Vector ----------------------
            out.print("<p><TABLE>");
            out.print("<CAPTION align=left > <font color=\"#891728\"> Damage Distribution Vector </font></CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>AssetStateDescriptor</TH>");            
            out.print("   <TH>StartState</TH>");            
            out.print("   <TH>EndState</TH>");            
            out.print("   <TH>Probability</TH>");            
            out.print("</TR>");
            
            DamageDistribution[] dists = dtm.getDamageDistributionArray();
            for (int i=0; i<dists.length; i++) {
                out.print("<TR>");
                out.print("   <TD>"+dists[i].getAssetState().getStateName()+"</TD>");            
                out.print("   <TD>"+dists[i].getStartState().getName()+"</TD>");            
                out.print("   <TD>"+dists[i].getEndState().getName()+"</TD>");            
                out.print("   <TD align=center>");            
                out.print("          <form method=\"get\" align=bottom>");
                out.print("            <input type=\"hidden\" name=\"threat\" value=\""+dtm.getName()+"\" >");
                out.print("            <input type=\"hidden\" name=\"index\" value=\""+i+"\"             >");
                out.print("            <input type=\"text\" name=\"prob\" value=\""+dists[i].getProbability() +"\" >");
                out.print("            <input type=\"submit\" value=\"Update\">");
                out.print("          </form>");
                out.print("   </TD>");            
                out.print("</TR>");
            }            
            out.print("</TABLE>");

            
            // Distribution Vector ----------------------
            out.print("<p><TABLE>");
            out.print("<CAPTION align=left ><font color=\"#891728\">  Membership Filters </font> </CAPTION>");
            out.print("<TR align=left>");
            out.print("   <TH>Property Name</TH>");            
            out.print("   <TH>Value</TH>");            
            out.print("   <TH>Operator</TH>");            
            out.print("</TR>");
            
            ThreatMembershipFilter[] filters = dtm.getMembershipFilters();
            for (int i=0; i<filters.length; i++) {
                out.print("<TR>");
                out.print("   <TD>"+filters[i].getPropertyName()+"</TD>");            
                out.print("   <TD>"+filters[i].getValue()+"</TD>");            
                out.print("   <TD>"+filters[i].getOperator()+"</TD>");            
                out.print("</TR>");
            }            
            out.print("</TABLE><HR>");
            
        }

    }
    //**End of servlet class
    

    private UnaryPredicate threatModelPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof DefaultThreatModel);
        }
    };

    /** Query the backboard for all current MetaThreatModels */
    private void findThreatModels() {
        
        this.blackboard.openTransaction();
        threatModels = this.blackboard.query(threatModelPredicate);
        try {
            this.blackboard.closeTransaction();
        } catch (SubscriberException se) {
            logger.warn("Got exception trying to close transaction! ==>\n" + se);
        }
    }

    /** 
     * Update the specified damage distribution entry in the specified threat model 
     * and perform a publish Change to the BB. Use IncrementalSubscription.getChangeReports(Object o) 
     * to get the list of change reports for each changed model. These reports will be of the type 
     * ThreatModelChangeEvent & have an eventType of DISTRIBUTION_CHANGE.
     * @param index index to position in distribution array
     * @param prob new probability value
     * @return May return a non-null error string
     */
    private String updateThreatModel(String threat, String index, String prob) {
        
        try {
            //Output threat model data
            if (threatModels == null || threatModels.size() == 0) {
                return "No Threat Models Found!";
            } else {
                Iterator models = threatModels.iterator();
                while (models.hasNext()) {
                    DefaultThreatModel dtm = (DefaultThreatModel)models.next();
                    if (dtm.getName().equals(threat)) {
                        DamageDistribution[] dists = dtm.getDamageDistributionArray();
                        DamageDistribution dd = dists[Integer.parseInt(index)];
                        dd.setProbability(Double.parseDouble(prob));
                        //Now publish change this threat model
                        Collection changes = Collections.singleton( new ThreatModelChangeEvent( dtm, ThreatModelChangeEvent.DISTRIBUTION_CHANGE ) );
                        this.blackboard.openTransaction();
                        this.blackboard.publishChange(dtm, changes);
                        logger.debug("=======> updateThreatModel(): PUBLISHED CHANGE on DefaultThreatModel with a ThreatModelChangeEvent=DISTRIBUTION_CHANGE!");
                        logger.debug("=======> setting probability = "+prob);
                        //logger.debug("=======> dd = "+dd+" ----- new Value = "+dd.getProbability());
                        try {
                            this.blackboard.closeTransaction();
                        } catch (SubscriberException se) {
                            logger.warn("Got exception trying to close transaction! ==>\n" + se);
                        }
                        return null;
                    }
                }
                return "Specified Threat Model not found.";
            }
        } catch (Exception e) {
            return e.toString();
        }
        
    }
    

    /** 
     * Update the specified threat likelihood 
     * and perform a publish Change to the BB. Use IncrementalSubscription.getChangeReports(Object o) 
     * to get the list of change reports for each changed model. These reports will be of the type 
     * ThreatModelChangeEvent & have an eventType of LIKELIHOOD_CHANGE.
     * @param index index to position in ThreatLikelihood array
     * @param prob new probability value
     * @return May return a non-null error string
     */
    private String updateThreatLikelihood(String threat, String index, String prob) {
        
        try {
            //Output threat model data
            if (threatModels == null || threatModels.size() == 0) {
                return "No Threat Models Found!";
            } else {
                Iterator models = threatModels.iterator();
                while (models.hasNext()) {
                    DefaultThreatModel dtm = (DefaultThreatModel)models.next();
                    if (dtm.getName().equals(threat)) {
                        ThreatLikelihoodInterval[] intervals = dtm.getLikelihoodIntervals();
                        ThreatLikelihoodInterval tli = intervals[Integer.parseInt(index)];
                        tli.setProbability(Double.parseDouble(prob));
                        Collection changes = Collections.singleton( new ThreatModelChangeEvent( dtm, ThreatModelChangeEvent.LIKELIHOOD_CHANGE ) );
                        //Now publish change this threat model
                        this.blackboard.openTransaction();
                            this.blackboard.publishChange(dtm, changes);
                        logger.debug("=======> updateThreatLikelihood(): PUBLISHED CHANGE on DefaultThreatModel with a ThreatModelChangeEvent=LIKELIHOOD_CHANGE!");
                        logger.debug("=======> setting probability = "+prob);
                        try {
                            this.blackboard.closeTransaction();
                        } catch (SubscriberException se) {
                            logger.warn("Got exception trying to close transaction! ==>\n" + se);
                        }
                        return null;
                    }
                }
                return "Specified Threat Model not found.";
            }
        } catch (Exception e) {
            return e.toString();
        }
        
    }
    
    
    
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
    
}
