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
import org.cougaar.core.component.ServiceBroker;

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
    private org.cougaar.util.ConfigFinder cf;
    
    private static ThreatLoader loader = null;
    public static ThreatLoader getLoader() { return loader; }
    /** Creates a new instance of ThreatLoader */
    public ThreatLoader(ServiceBroker serviceBroker, UIDService us, org.cougaar.util.ConfigFinder cf) {
        
        super("Threat", "Threats", serviceBroker, us); //, requiredServices);
        threats = new Vector();
        loader = this;
        this.cf = cf;
    }
    
           
    public void load() {
               
        //load probability map (maps form user string probabilities to Coordinator internal floats
        try {
            probabilityMap = MapLoader.loadMap(cf, probMapFile);
            if (probabilityMap == null) {
                logger.error("Error loading probability map file [" + probMapFile + "]. ");
            }
        } catch (Exception e) {
//            logger.error("Error parsing XML file [" + probMapFile + "]. Error was: "+ e.toString(), e);
            logger.error("Error parsing XML file Error was: ", e);
            return;
        }
                
    }
    
    
    /** Called with a DOM "Threat" element to process */
    protected Vector processElement(Element element) {
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String threatName = element.getAttribute("name");
        String assetType = element.getAttribute("affectsAssetType");
        String causesEvent = element.getAttribute("causesEvent");
        String defaultProbStr = element.getAttribute("defaultEventLikelihoodProb"); //string
        String defaultProbInterval = element.getAttribute("probIntervalInMins"); //string

        if (threatName == null || threatName.length()==0 || assetType == null || assetType.length() == 0 || causesEvent == null || causesEvent.length() == 0) {
            logger.error("Threat XML Error threat ["+threatName+"]- exception parsing - threat name, assetType, or causesEvent was NULL. Ignoring.");
            return threats;
        }
        
        //Convert probability String to a float.
        int probInt = 0;
        float fProb = 0;
        if (probabilityMap != null) {
            Float f = (Float) probabilityMap.get(defaultProbStr);
            if (f != null) {
                fProb = f.floatValue();
            }
            
            if (fProb > 0) { //don't process if fProb = 0
                try {
                    probInt = Integer.parseInt(defaultProbInterval);
                } catch (Exception e) {
                    logger.error("Threat XML Error threat ["+threatName+"]- exception parsing default probability interval: "+defaultProbInterval+", Ignoring.", e);
                    return threats;                
                }
            }
        }
            
        AssetType affectsAssetType = AssetType.findAssetType(assetType);

        //what to do when assetType is null? - create it, process it later?
        if (affectsAssetType == null) {
            logger.warn("Threat XML Error - affectsAssetType unknown: "+assetType);
            return threats;
        }

        if (us == null) {
            logger.warn("Threat XML Error - UIDService is null!");
            return threats;
        }
        
        UID uid = us.nextUID();

        //Create default threat
        ThreatDescription threat = new ThreatDescription( threatName, affectsAssetType, causesEvent, fProb, probInt );
/*        
        try {
            double prob = threat.getEventProbability().computeIntervalProbability(System.currentTimeMillis(), System.currentTimeMillis()+600000);
            if (logger.isDebugEnabled()) { logger.debug("Testing event likelihood. Prob = "+prob); }            
        } catch (Exception e1) {
            if (logger.isDebugEnabled()) { logger.debug("Testing event likelihood interval. EXCEPTION:"+e1); e1.printStackTrace(); }            
        }
*/        
        threats.add(threat);

        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("VulnerableAssets") ) {
                e = (Element)child;
                //logger.debug("calling parseVulnerableAssets...");
                parseVulnerableAssets(e, threat);
            } //else, likely a text element - ignore
        }

        logger.debug("Added new Threat (default) with no filter: \n"+threat.toString() );
            
        return threats;
    }
    
    
    /** Go back thru the threats & link them up with the events they referenced */ 
    protected void setEventLinks(Vector allThreats, Vector allEvents) {
     
        Iterator i = allThreats.iterator();
        while (i.hasNext() ) {
            ThreatDescription threat = (ThreatDescription)i.next();
            String eventName = threat.getEventNameThreatCauses();

            Iterator j = allEvents.iterator();
            boolean found = false;
            while (j.hasNext() ) {
                EventDescription e = (EventDescription)j.next();
                if (eventName.equals(e.getName()) ) {
                    threat.setEventThreatCauses(e);
                    found = true;
                    break;
                }
            }
            if (!found) {                
                logger.error("Could not find event["+eventName+"] referenced in Threat["+threat.getName()+"]. Threat will be ignored!");
            }
        }   
    }
    
    
    //publish all threats at this point
    protected void publishThreats(org.cougaar.core.service.BlackboardService blackboard) {
        if (threats == null) {
            logger.warn("No threats published the blackboard.");
            return;
        }
        Iterator i = threats.iterator();
        while (i.hasNext() ) {
            ThreatDescription td = (ThreatDescription)i.next();
            //Don't publish if there's no prob. that the threat will occur, or if the causesEvent was not found
            if (td.getEventProbability() != null && td.getEventThreatCauses() != null) { 
                blackboard.publishAdd(td);
            } 
        }
        logger.debug("Published "+threats.size()+" threats to the blackboard.");
    }
    

    /** Parse sub kinds of threats & create new ThreatDescriptions for each entry */
    private void parseVulnerableAssets(Element element, ThreatDescription threat) {

        logger.debug("Calling ThreatVulnerabilityLoader.parseElement...");
        
        ThreatVulnerabilityFilter vf = ThreatVulnerabilityLoader.parseElement(element, probabilityMap);
        if (vf != null) {
            ThreatDescription td = new ThreatDescription(threat, vf);
            threats.add(td);
            logger.debug("Added new Threat (with filter): \n"+td.toString() );
        } else {
            logger.error("Error parsing XML file for threat [" + threat.getName() + "]. ThreatVulnerabilityFilter was null for element = "+element.getNodeName() );
        }
    }
    
}