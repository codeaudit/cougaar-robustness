/*
 * DiagnosisMangerPlugin.java
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
 * This plugin reads in the XML definitions of the Diagnosis Tech Specs and makes them
 * available via a service.
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DiagnosisManagerPlugin extends ComponentPlugin implements NotPersistable  {
    
    
    private LoggingService logger;
    private UIDService us = null;
    private DiagnosisTechSpecServiceProvider dtssp = null;
    private boolean haveServices = false;
    
    private Hashtable allDiagnoses;
    private Vector newDiagnoses;
    private Vector fileParams = null;
    
    /** The Max latency of all defenses. Used to throttle the TimedDiagnosisPlugin via its knob
     *  ***ASSUMES ONLY ONE MONITORING LEVEL PER DEFENSE!!! ***
     */
    //private long maxDefenseLatency = 0L;
    
    /**
     * Read in parameters passed in via configuration file.
     */
    private void getPluginParams() {
        
        fileParams = new Vector();
        
        Iterator iter = getParameters().iterator ();
        if (iter.hasNext()) {
            fileParams.add( (String) iter.next() );
        }
        logger.debug("Total # of diagnosis file arguments = " + fileParams.size());
    }
    
    /** Read in the Diagnoses from the XML tech spec files */
    private void readInDiagnoses(Vector fileParams) {
        
        //**** Iterator thru all of the fileParams & load the deserialized objects into
        // allDiagnoses indexed by class/file name (minus ".xml" suffix).
        if (fileParams == null) { return; }

        File f;
        DiagnosisTechSpecInterface diagnosis = null;
        Iterator i = fileParams.iterator();
        while (i.hasNext()) {
        
            String name = (String)i.next(); 
            
            try {
    
    /////            DiagnosisXMLParser parser = new DiagnosisXMLParser (us);

                f = getConfigFinder().locateFile((String)i.next()); 
                if (!f.exists()) { //look
                    logger.warn("*** Did not find Diagnosis XML file in = " + f.getAbsolutePath());
                    logger.error("**** CANNOT FIND Diagnosis XML file!");
                    return;
                }
                //logger.debug("path for Diagnosis XML file = " + f.getAbsolutePath());

    /////            parser.parse(new InputSource(new FileInputStream(f)));

    /////            diagnosis = parser.getParsedDiagnosis();
                allDiagnoses.put(name, diagnosis); 
                newDiagnoses.add(diagnosis);

            } catch (Exception e) {
                logger.error("Exception while importing Diagnosis!",e);
            }

            logger.debug("Imported "+allDiagnoses.size()+" Diagnoses!");
            
        }
    }
    
    
    /**
     * Prepare to run this plugin. Announce DiagnosisTechSpecService
     *
     */
    public void load() {
        
        getServices();
        allDiagnoses = new Hashtable(100);
        newDiagnoses = new Vector();
        
        //load tech specs
        getPluginParams();
        readInDiagnoses(fileParams);
        
        // create and advertise our service
        this.dtssp = new DiagnosisTechSpecServiceProvider(this);
        getServiceBroker().addService(DiagnosisTechSpecService.class, dtssp);
    }
    
    public void unload() {
        // revoke our service
        if (dtssp != null) {
            getServiceBroker().revokeService(DiagnosisTechSpecService.class, dtssp);
            dtssp = null;
        }
        super.unload();
    }
    
    /**
     * Reads in the Diagnoses from XML, publishes them,
     * and then publishes itself.
     */
    public void setupSubscriptions() {
    }
    
    private void getServices() {
        
        logger =
        (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        
        us = (UIDService ) getServiceBroker().getService( this, UIDService.class, null ) ;
        
        haveServices = true;
    }
    
    
    /** Publishes new Diagnoses to the BB */
    public void execute() {
        
        if (newDiagnoses.size() > 0) {
            
            synchronized(newDiagnoses) { //no new diagnoses to be added while we're getting them!
                Iterator i = newDiagnoses.iterator();
                while (i.hasNext() ) {
                    this.blackboard.publishAdd((DiagnosisTechSpecInterface)i.next());
                }            

                //Now empty the vector since we've added them
                newDiagnoses.clear();
            }
        }
        
    }
    
    
    /**
     * Returns the DiagnosisTechSpec for the given class. If it has not been loaded,
     * the TechSpec is searched for, parsed, and loaded.
     *
     * @return NULL if the DiagnosisTechSpec cannot be found.
     */
    DiagnosisTechSpecInterface getTechSpec(Class cls) {
    
        DiagnosisTechSpecInterface dts = (DiagnosisTechSpecInterface)allDiagnoses.get( cls.getName() );
        if (dts == null) {
            
            //Tech Spec is not loaded...
            //... try finding it, parsing it, putting it in allDiagnoses, and returning it.

            //Now add it to newDiagnoses so it gets published to the BB
            synchronized(newDiagnoses) {
                    //add to new Diagnoses
            }
        }
        
        return dts; //even if null
    }
    
}


