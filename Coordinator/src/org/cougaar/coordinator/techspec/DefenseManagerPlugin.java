 /*
 * DefenseManagerPlugin.java
 *
 * Created on September 12, 2003, 4:53 PM
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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;

import org.cougaar.core.persist.NotPersistable;

/**
 *
 * This plugin reads in the XML definitions of the Defense Tech Specs & publishes them to the Blackboard.
 * @deprecated Likely to be deprecated April 2004.
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DefenseManagerPlugin extends ComponentPlugin implements NotPersistable  {
    
    
    private LoggingService logger;
    private UIDService us = null;
    
    private boolean haveServices = false;
    
    private Vector allDefenses;
    private String fileParam = null;

    /** Set to true when the asset listener has been added */
    private boolean ADDED_ASSET_LISTENER = false;
       
    /** The Max latency of all defenses. Used to throttle the TimedDiagnosisPlugin via its knob 
     *  ***ASSUMES ONLY ONE MONITORING LEVEL PER DEFENSE!!! ***
     */
    private long maxDefenseLatency = 0L;
    
    /**
      * Read in Defense XML file parameter passed in via configuration file. 
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        //if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
            fileParam = (String) iter.next();
            logger.debug("Read in plugin file Parameter = " + fileParam);
        }
    }       
    
    /** Read in the Defenses from an XML file & publishes them to the BB */
    private void readInDefenses() {
/*        
        getPluginParams();
        if (fileParam == null) {
            logger.error("No Defense definitions to import! Must include xml file as plugin parameter!!");
            return;
        }

        try {
            DefenseXMLParser parser = new DefenseXMLParser (us);
//steve     File f = new File(fileParam);
            File f = getConfigFinder().locateFile(fileParam); //steve            
            if (!f.exists()) { //look 
                logger.debug("*** Did not find Defense XML file in = " + f.getAbsolutePath()+". Checking CIP...");
                String installpath = System.getProperty("org.cougaar.install.path");
                String defaultPath = installpath + File.separatorChar + "csmart" + File.separatorChar + "config" +
                   File.separatorChar + "lib" + File.separatorChar + "robustness" + 
                   File.separatorChar + "uc9" + File.separatorChar + fileParam;

                f = new File(defaultPath);
                if (!f.exists()) {                    
                    logger.warn("*** Did not find Defense XML file in = " + f.getAbsolutePath());
                    logger.error("**** CANNOT FIND Defense XML file!");
                    return;
                }
            }                            
            logger.debug("path for Defense XML file = " + f.getAbsolutePath());
            
            parser.parse(new InputSource(new FileInputStream(f)));

            Vector allDefenses = parser.getParsedDefenses();
            DefaultDefenseTechSpec def;
            
            // Now add each read in descriptor to its AssetType
            if (allDefenses != null && allDefenses.size() > 0) {             
                    for (Iterator i = allDefenses.iterator(); i.hasNext(); ) {
                        def = (DefaultDefenseTechSpec)i.next();                        
                        
                        //Now publish the defense
                        blackboard.publishAdd(def);
                    }
            }
            
            logger.debug("Imported "+allDefenses.size()+" Defenses!");
        } catch (Exception e) {
            
            logger.error("Exception while importing Defenses!",e);
        }
 */
    }        
    
    
    
    
    /**
     * Reads in the Defenses from XML, publishes them,
     * and then publishes itself.
     */
    public void setupSubscriptions() {
        
        
        getServices();
        
        allDefenses = new Vector(10);
        
        assetManagerSub =
        (IncrementalSubscription)blackboard.subscribe(assetManagerModelPredicate);
        
        blackboard.publishAdd(this); //publish myself, ONLY after reading in XML
        
    }
    
    private void getServices() {
        
        logger =
        (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        
        us = (UIDService ) getBindingSite().getServiceBroker().getService( this, UIDService.class, null ) ;
        
        haveServices = true;
    }
    
    /** Does nothing */
    public void execute() {
    
    
        Collection assetMgrs = assetManagerSub.getAddedCollection();
        Iterator it = assetMgrs.iterator(); 
        if (it.hasNext() && !ADDED_ASSET_LISTENER ) { // *** This should only execute once.
            AssetManagerPlugin aMgr = (AssetManagerPlugin)it.next();
            logger.info("Found AssetManagerPlugin");
            ADDED_ASSET_LISTENER = true;            
            readInDefenses(); //Read in & publish defenses
        }    
    
    }

    private IncrementalSubscription assetManagerSub;
    private UnaryPredicate assetManagerModelPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof AssetManagerPlugin);
        }
    };
    

    
    
}

