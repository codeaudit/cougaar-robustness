/*
 * ThreatLoader.java
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

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Hashtable;

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class ThreatLoader extends XMLLoader {
    
    static final String probMapFile = "ProbabilityMap.xml";
    private Hashtable probabilityMap = null;
    
    private Vector threats;
    
    /** Creates a new instance of ThreatLoader */
    public ThreatLoader() {
        
        super("Threat", "Threats"); //, requiredServices);
        threats = new Vector();
    }
    
       
    /* Acquire needed services */
    //private boolean haveServices() { //don't use logger here... until after super.load() is called
    //}
    
    public void load() {
               
        //load probability map (maps form user string probabilities to Coordinator internal floats
        try {
            probabilityMap = MapLoader.loadMap(getConfigFinder(),probMapFile);
            if (probabilityMap == null) {
                logger.error("Error loading probability map file [" + probMapFile + "]. ");
            }
        } catch (Exception e) {
            logger.error("Error parsing XML file [" + probMapFile + "]. Error was: "+ e.toString(), e);
        }
        
        //haveServices(); //call have services first !!!
        super.load(); //loads in & begins parsing of xml files.
        
    }
    
    
    /** Called with a DOM "Threat" element to process */
    protected void processElement(Element element) {
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String threatName = element.getAttribute("name");
        String assetType = element.getAttribute("affectsAssetType");
        String causesEvent = element.getAttribute("causesEvent");
        String defaultProbStr = element.getAttribute("defaultEventLikelihoodProb"); //string
        
        //Convert probability String to a float.
        float fProb = 0;
        if (probabilityMap != null) {
            Float f = (Float) probabilityMap.get(defaultProbStr);
            if (f != null) {
                fProb = f.floatValue();
            }
        }
            
        AssetType affectsAssetType = AssetType.findAssetType(assetType);

        //what to do when assetType is null? - create it, process it later?
        if (affectsAssetType == null) {
            logger.warn("Threat XML Error - affectsAssetType unknown: "+assetType);
            return;
        }

        if (us == null) {
            logger.warn("Threat XML Error - UIDService is null!");
            return;
        }
        
        UID uid = us.nextUID();

        ThreatDescription threat = new ThreatDescription( threatName, affectsAssetType, causesEvent, fProb );
        
        //Create a Threat
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("VulnerableAssets") ) {
                e = (Element)child;
                parseVulnerableAssets(e, threat);
            } //else, likely a text element - ignore
        }

        logger.debug("Added new Threat: \n"+threat.toString() );
            
    }
    
    
    protected void execute() {
    
    
    //should possibly publish all threats at this point??
    
    }
    
    
    
    private void parseVulnerableAssets(Element element, ThreatDescription threat) {

        VulnerabilityFilter vf = VulnerabilityLoader.parseElement(element, probabilityMap);
        if (vf != null) {
            threat.addVulnerabilityFilter(vf);
        } else {
            logger.error("Error parsing XML file for threat [" + threat.getName() + "]. VulnerabilityFilter was null for element = "+element.getNodeName() );
        }
    }
    
}