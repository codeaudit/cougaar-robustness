/*
 * LoadTechSpecsPlugin.java
 *
 * Created on May 26, 2004, 8:59 AM
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

package org.cougaar.coordinator.techspec.xml;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

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

import org.w3c.dom.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class LoadTechSpecsPlugin extends ServiceUserPluginBase implements NotPersistable  {
    
    //protected LoggingService logger;

    protected UIDService us = null;

    //Vectors of tech spec files, each will contain zero or more Document objects
    Vector actuatorTypes;
    Vector assetStateDims;
    Vector assetSubtypes;
    Vector assetTypes;
    Vector crossDiagnoses;
    Vector threats;
    Vector sensors;
    Vector events;
    Vector utilities;
    
    ActuatorTypeLoader actuatorLoader;
    AssetStateDimensionLoader assetStateDimLoader;
    AssetSubtypeLoader assetSubtypeLoader;
    AssetTypeLoader assetTypeLoader;
    CrossDiagnosisLoader crossDiagnosisLoader;
    ThreatLoader threatLoader;
    SensorTypeLoader sensorLoader;
    EventLoader eventLoader;
    SocietalUtilityLoader utilityLoader;
    DiagnosisTechSpecService diagnosisTechSpecService = null;
    ActionTechSpecService actionTechSpecService = null;

    Vector eventDescriptions = null;
    Vector threatDescriptions = null;
    
    ActionTechSpecServiceProvider atssp;
    DiagnosisTechSpecServiceProvider dtssp;

    ServiceBroker rootsb; //root service broker -- used to make services available to all agents on a node.
    
    public LoadTechSpecsPlugin() {
        super(requiredServices);   
    }
    
    private static final Class[] requiredServices = {
        ActionTechSpecService.class, 
        DiagnosisTechSpecService.class,
        UIDService.class
    };

    
    /** Creates a new instance of AssetTypeLoader */
//    public LoadTechSpecsPlugin() { 

    public void initialize() {

        super.initialize();
        
        actuatorTypes = new Vector(3,3);
        assetStateDims = new Vector(3,3);
        assetSubtypes = new Vector(3,3);
        assetTypes = new Vector(3,3);
        crossDiagnoses = new Vector(3,3);
        threats = new Vector(3,3);
        sensors = new Vector(3,3);
        events = new Vector(3,3);
        utilities = new Vector(3,3);
        
    }
    
    
    private boolean haveServices() {
        
            us = (UIDService )
            getServiceBroker().getService( this, UIDService.class, null ) ;
            if (us == null) {
                throw new RuntimeException("Unable to obtain UIDService");
            }

            //this.logger = (LoggingService)  this.getServiceBroker().getService(this, LoggingService.class, null);
            //if (logger == null) {
            //    throw new RuntimeException("Unable to obtain LoggingService");
            //}

            NodeControlService ncs = (NodeControlService)
                getServiceBroker().getService(this, NodeControlService.class, null);
            if (ncs != null) {
                rootsb = ncs.getRootServiceBroker();
                getServiceBroker().releaseService(this, NodeControlService.class, ncs);
            } else {
                logger.error("Unable to obtain NodeControlService");
            }

            // create and advertise our service node-wide
            this.dtssp = new DiagnosisTechSpecServiceProvider();
            rootsb.addService(DiagnosisTechSpecService.class, dtssp);

            // create and advertise our service node-wide
            this.atssp = new ActionTechSpecServiceProvider();
            rootsb.addService(ActionTechSpecService.class, atssp);

            diagnosisTechSpecService =
            (DiagnosisTechSpecService) getServiceBroker().getService(this, DiagnosisTechSpecService.class, null);
            if (diagnosisTechSpecService == null) {
                logger.error(
                "Unable to obtain DiagnosisTechSpecService");
            } 

            actionTechSpecService =
            (ActionTechSpecService) getServiceBroker().getService(this, ActionTechSpecService.class, null);
            if (actionTechSpecService == null) {
                logger.error(
                "Unable to obtain ActionTechSpecService");
            } 

            return true;
//        }
//        else if (logger.isDebugEnabled()) logger.error(".haveServices - at least one service not available!");
//        return false;

    }
    
    
    
    
    /** Read in xml files */
    public void load() {
        
        super.load();
        
        haveServices();

        //Instantiate each tech spec loader
        actuatorLoader = new ActuatorTypeLoader(actionTechSpecService, getServiceBroker(), us, getConfigFinder());
        assetStateDimLoader = new AssetStateDimensionLoader(getServiceBroker(), us);
        assetSubtypeLoader = new AssetSubtypeLoader(getServiceBroker(), us);
        assetTypeLoader = new AssetTypeLoader(getServiceBroker(), us);
        crossDiagnosisLoader = new CrossDiagnosisLoader(diagnosisTechSpecService, getServiceBroker(), us);
        threatLoader = new ThreatLoader(getServiceBroker(), us, getConfigFinder());
        sensorLoader = new SensorTypeLoader(diagnosisTechSpecService, getServiceBroker(), us);
        eventLoader = new EventLoader(getServiceBroker(), us, getConfigFinder());
        utilityLoader = new SocietalUtilityLoader(getServiceBroker(), us);

        actuatorLoader.load();
        assetStateDimLoader.load();
        assetSubtypeLoader.load();
        assetTypeLoader.load();
        crossDiagnosisLoader.load();
        threatLoader.load();
        sensorLoader.load();
        eventLoader.load();
        utilityLoader.load();
        
        
        //------------------------------
        //read in the xml files to parse
        //------------------------------
        Vector files = getPluginParams();
        
        //Create DOM parser
        DOMifier dom = new DOMifier(getConfigFinder());
        Document doc;
        String filename;
        
        //Start processing files.
        for (Iterator iter = files.iterator(); iter.hasNext(); ) {
            //1. Call dom to parse
            filename = (String)iter.next();
            try {
                doc = dom.parseFile(filename);
                
                //Now store doc into appropriate vector
                storeDoc(doc, filename);
                                
            } catch (Exception e) {
                logger.error("Error parsing XML file [" + filename + "]. Error was: "+ e.toString(), e);
            }
            
        }
        
        //Now, process the tech specs in the desired order.
        processTechSpecs();
        
        
    }
    
    //Publish the events & threats
    public void setupSubscriptions() {
        eventLoader.publishEvents(blackboard, eventDescriptions);
        threatLoader.publishThreats(blackboard);        
    }
    

    /** This method controls the loading of tech specs to ensure ordered processing to account
     * for inter-dependencies.
     */
    private void processTechSpecs() {


        //1. Load new asset types first
        loadDocs( assetTypeLoader, assetTypes );
        //2. Load asset subtypes
        loadDocs( assetSubtypeLoader, assetSubtypes );
        //3. Load asset state dimensions
        loadDocs( assetStateDimLoader, assetStateDims );
        //4. Load societal utilities
        loadDocs( utilityLoader, utilities );

        //5. Load events & publish them
        eventDescriptions = loadDocs( eventLoader, events ); //must publish
        eventLoader.setEventLinks(eventDescriptions);

        //Load actuators & sensors & cross diagnoses
        loadDocs( actuatorLoader,actuatorTypes );
        loadDocs( sensorLoader, sensors );
        loadDocs( crossDiagnosisLoader, crossDiagnoses );

        //Load threats & publish them
        threatDescriptions = loadDocs( threatLoader, threats ); //must publish
        threatLoader.setEventLinks(threatDescriptions, eventDescriptions);
        
    }
    
    /** Causes the XMLLoader to load the tech specs */
    private Vector loadDocs(XMLLoader loader, Vector docs) {
     
        Vector objects = null;
        for (Iterator i = docs.iterator(); i.hasNext(); ) {
        
            objects = loader.processDocument( (Document) i.next() ) ;
        }
        return objects; //vector is accumulative in class
    }
    
    
    /** This method simply examines the tech spec file for the root tag to determine
     *  which vector it should be stored in.
     */
    private void storeDoc(Document doc, String filename) {
     
        //First see if there is more than one
        if (doc == null) return;
        
        Element root = doc.getDocumentElement();
        String tag = root.getTagName();
        
        if (actuatorLoader.isValidTag(tag))  { actuatorTypes.add(doc); return; }
        if (assetStateDimLoader.isValidTag(tag))  { assetStateDims.add(doc); return; }
        if (assetSubtypeLoader.isValidTag(tag))  { assetSubtypes.add(doc); return; }
        if (assetTypeLoader.isValidTag(tag))  { assetTypes.add(doc); return; }
        if (crossDiagnosisLoader.isValidTag(tag))  { crossDiagnoses.add(doc); return; }
        if (threatLoader.isValidTag(tag))  { threats.add(doc); return; }
        if (sensorLoader.isValidTag(tag))  { sensors.add(doc); return; }
        if (eventLoader.isValidTag(tag))  { events.add(doc); return; }
        if (utilityLoader.isValidTag(tag))  { utilities.add(doc); return; }
        else { // unknown tag!
            logger.warn("Saw unknown tag ["+tag+"]. Ignoring file = "+ filename);
        }
        
    }
    
    
    
    
    /**
     * Read in XML file parameters passed in via configuration file.
     *
     * *****!!!!!
     * Temp - read in the # of assets to see before emitting AllAssetsSeenCondition ****************** STILL DO THIS IN 2004???
     * *****!!!!!
     *
     */
    private Vector getPluginParams() {
        
        if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");
        
        String fileParam;
        int count = 0;
        Vector files = new Vector();
        Iterator iter = getParameters().iterator();
        while (iter.hasNext()) {
            fileParam = (String) iter.next();
            files.add(fileParam);
            count++;
        }
        if (logger.isDebugEnabled()) logger.debug("*** Plugin read " +count+" XML files ***");
        
        return files;
        
    }
        
    
    public void unload() {
        
        super.unload();
        
        if ((logger != null) && (logger != LoggingService.NULL)) {
            getServiceBroker().releaseService(
            this, LoggingService.class, logger);
            logger = LoggingService.NULL;
        }

        if (us != null) {
            getServiceBroker().releaseService(
            this, UIDService.class, logger);
            us = null;
        }
        
    }
    
    
    protected void execute() {}
    
    
    
}
