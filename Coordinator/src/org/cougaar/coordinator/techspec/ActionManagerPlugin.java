/*
 * ActionMangerPlugin.java
 *
 * Created on February 6, 2004, 11:05 AM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

import org.cougaar.coordinator.timedDiagnosis.TimedDiagnosisKnob;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;

import org.cougaar.core.persist.NotPersistable;

/**
 *
 * This plugin reads in the XML definitions of the Action Tech Specs and makes them
 * available via a service.
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class ActionManagerPlugin extends ComponentPlugin implements NotPersistable  {
    
    
    private LoggingService logger;
    private UIDService us = null;
    private ActionTechSpecServiceProvider atssp = null;
    private boolean haveServices = false;
    
    private Hashtable allActions;
    private Vector newActions;
    private Vector fileParams = null;
    
    /** The Max latency of all defenses. Used to throttle the TimedDiagnosisPlugin via its knob
     *  ***ASSUMES ONLY ONE MONITORING LEVEL PER DEFENSE!!! ***
     */
    //private long maxDefenseLatency = 0L;

    
    public void initialize() {

        super.initialize();
        
        // create and advertise our service
        this.atssp = new ActionTechSpecServiceProvider(this);
        getServiceBroker().addService(ActionTechSpecService.class, atssp);

    }
    
    /**
     * Read in the filename parameters passed in via configuration file.
     * These should be the action xml tech specs.
     */
    private void getPluginParams() {
        
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        //if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");
        fileParams = new Vector();
        
        Iterator iter = getParameters().iterator ();
        if (iter.hasNext()) {
            fileParams.add( (String) iter.next() );
        }
        logger.debug("Total # of action file arguments = " + fileParams.size());
    }
    
    /** Read in the Actions from the XML tech spec files */
    private void readInActions(Vector fileParams) {
        
        //**** Iterator thru all of the fileParams & load the deserialized objects into
        // allActions indexed by class/file name (minus ".xml" suffix).
        if (fileParams == null) { return; }

        File f;
        ActionTechSpecInterface action = null;
        Iterator i = fileParams.iterator();
        while (i.hasNext()) {
        
            String name = (String)i.next(); 
            
            try {
    
    /////            ActionXMLParser parser = new ActionXMLParser (us);

                f = getConfigFinder().locateFile((String)i.next()); 
                if (!f.exists()) { //look
                    logger.warn("*** Did not find Action XML file in = " + f.getAbsolutePath());
                    logger.error("**** CANNOT FIND Action XML file!");
                    return;
                }
                //logger.debug("path for Action XML file = " + f.getAbsolutePath());

    /////            parser.parse(new InputSource(new FileInputStream(f)));

    /////            action = parser.getParsedAction();
                allActions.put(name, action); 
                newActions.add(action);

            } catch (Exception e) {
                logger.error("Exception while importing Action!",e);
            }

            logger.debug("Imported "+allActions.size()+" Actions!");
            
        }
    }
    
    /**
     * Prepare to run this plugin. 
     *
     */
    public void load() {

        super.load();
        getServices();
        allActions = new Hashtable(100);
        newActions = new Vector();

    }
    
    
    /**
     * Reads in the Actions from XML, publishes them,
     * and then publishes the ActionTechSpecService
     */
    public void setupSubscriptions() {
        //load tech specs
        getPluginParams();
        readInActions(fileParams);
        
    }
    
    /**
     * Unload the ActionTechSpecService
     */
    public void unload() {
        // revoke our service
        if (atssp != null) {
            getServiceBroker().revokeService(ActionTechSpecService.class, atssp);
            atssp = null;
        }
        super.unload();
    }
    
    
    private void getServices() {
        
        logger =
        (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
        if (logger == null) {
            logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        }
        
        us = (UIDService ) getServiceBroker().getService( this, UIDService.class, null ) ;
        
        haveServices = true;
    }
    
    /** Publishes new Actions to the BB */
    public void execute() {
        
        if (newActions.size() > 0) {
            
            synchronized(newActions) { //no new actions to be added while we're getting them!
                Iterator i = newActions.iterator();
                while (i.hasNext() ) {
                    this.blackboard.publishAdd((ActionTechSpecInterface)i.next());
                }            

                //Now empty the vector since we've added them
                newActions.clear();
            }
        }
        
    }
    
    /**
     * Returns the ActionTechSpec for the given class. If it has not been loaded,
     * the TechSpec is searched for, parsed, and loaded.
     *
     * @return NULL if the ActionTechSpec cannot be found.
     */
    ActionTechSpecInterface getTechSpec(String cls) {
    
        ActionTechSpecInterface ats = (ActionTechSpecInterface)allActions.get( cls);
        if (ats == null) {
            
            logger.warn("************* action tech spec NOT FOUND: "+cls);        
            
            //Tech Spec is not loaded...
            //... try finding it, parsing it, putting it in allActions, and returning it.
            
            //Now add it to newActions so it gets published to the BB
            synchronized(newActions) {
                    //add to new Actions
            }

        }
        
        return ats; //even if null
    }
    
    /**
     * Add an ActionTechSpec for a class. Targeted to testing
     */
    public void addTechSpec(String cls, ActionTechSpecInterface a) {

        allActions.put(cls, a); 
        newActions.add(a);
        logger.debug("************* add action tech spec: "+cls);        
    }
    
    
}


