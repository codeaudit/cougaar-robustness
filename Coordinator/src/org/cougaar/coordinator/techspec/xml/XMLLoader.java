/*
 * XMLLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
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

import org.cougaar.core.adaptivity.ServiceUserPluginBase;
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

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public abstract class XMLLoader extends ComponentPlugin implements NotPersistable  {
    
    protected LoggingService logger;
    private String singleTag;
    private String pluralTag;
    
    protected UIDService us = null;
    
    /** Creates a new instance of AssetTypeLoader */
    public XMLLoader(String singleTag, String pluralTag) { 
        super();
        this.singleTag = singleTag;
        this.pluralTag = pluralTag;
    }
    
    
    private boolean haveServices() {
        
        us = (UIDService )
        getServiceBroker().getService( this, UIDService.class, null ) ;
        if (us == null) {
            throw new RuntimeException("Unable to obtain EventService");
        }
        
        this.logger = (LoggingService)  this.getServiceBroker().getService(this, LoggingService.class, null);
        if (logger == null) {
            throw new RuntimeException("Unable to obtain LoggingService");
        }
        return true;
    }
    
    
    
    
    
    protected void setupSubscriptions() {}
    
    /** Read in xml files */
    public void load() {
        
        super.load();
        haveServices();
        
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
                //Call subclass to process the dom tree (both plural elements & single (e.g. <AssetType> and <AssetTypes>)
                processDocument(doc);
                
            } catch (Exception e) {
                logger.error("Error parsing XML file [" + filename + "]. Error was: "+ e.toString(), e);
            }
            
        }
        
    }
    
    /**
     *  Called with a DOM element to process.
     *  This is where the custom subclass code goes to walk thru the tree
     *  and to instantiate a tech spec instance.
     */
    protected abstract void processElement(Element element);
    
    
    /** Called with a DOM document to process */
    protected void processDocument(org.w3c.dom.Document doc) {
        
        //First see if there is more than one
        if (doc == null) return;
        
        Element root = doc.getDocumentElement();
        String tag = root.getTagName();
        
        if (tag.equalsIgnoreCase(singleTag)) {
            processElement(root);
        } else if (pluralTag != null && tag.equalsIgnoreCase(pluralTag)) {
            
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase(singleTag) ) {
                    processElement( (Element)child );
                }
            }
            
        } //else it's a text Node, so don't process.
        
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
    
    private void getServices() {
/*
        logger =
        (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
 
        us = (UIDService ) getBindingSite().getServiceBroker().getService( this, UIDService.class, null ) ;
 
        haveServices = true;
 */
    }
    
    
    public void unload() {
        
        super.unload();
        
        if ((logger != null) && (logger != LoggingService.NULL)) {
            getServiceBroker().releaseService(
            this, LoggingService.class, logger);
            logger = LoggingService.NULL;
        }
        
    }
    
    
    protected void execute() {}
    
    
    
}
