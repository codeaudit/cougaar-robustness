/*
 * AssetStateDimensionLoader.java
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

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class AssetStateDimensionLoader extends XMLLoader {
    
    Vector assetTypes;
    
    /** Creates a new instance of AssetStateDimensionLoader */
    public AssetStateDimensionLoader(ServiceBroker serviceBroker, UIDService us) {
        
        super("AssetStateDimensions", null, serviceBroker, us);
        assetTypes = new Vector();
    }
  
    public void load() {}

    /** Called with a DOM "AssetType" element to process */
    protected Vector processElement(Element element) {
     
        Vector states = new Vector();
        //publish to BB during execute().
        //1. Create a new AssetType instance & 
        String type = element.getAttribute("assetType");
        AssetType assetType = AssetType.findAssetType(type);
        
        //what to do when assetType is null? - create it, process it later?
        if (assetType == null) {
            logger.warn("AssetType XML Error - AssetType unknown: "+type);
            return null;
        }
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) { 
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("StateDimension") ) {
                AssetStateDimension asd = parseStateDimension((Element)child, assetType);
                if (asd != null) { // then add it to the asset type
                    boolean added = assetType.addStateDimension(asd);
                    states.add(asd);
                    if (!added) {
                        if (logger.isInfoEnabled())
			    logger.info("Tried to add duplicate state dimension: "+asd.toString());
                        continue;
                    }
                }
                logger.debug(asd.toString());

            } //else, likely a text element - ignore
        }
        return states;
    }
    
    
    
    private AssetStateDimension parseStateDimension(Element element, AssetType assetType) {
        
        String stateDimName = element.getAttribute("name");
        AssetStateDimension asd = new AssetStateDimension(assetType, stateDimName);
        //logger.error("**************************************** Saw new state dim = "+ asd.getStateName() + "for type="+assetType.toString());
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) { 
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("State") ) {
                Element e = (Element) child;
                String stateName = e.getAttribute("name");
                String mcomp = e.getAttribute("relativeMauCompleteness");
                String msec  = e.getAttribute("relativeMauSecurity");
                String defaultState  = e.getAttribute("defaultStartState");               
                
                try {
                    float c = Float.parseFloat(mcomp);
                    float s = Float.parseFloat(msec);
                    //Add new state to state dimension
                    AssetState as = new AssetState(stateName, c, s);
                    asd.addState(as);
                    
                    //Set the default state if so indicated
                    if (defaultState.equalsIgnoreCase("TRUE")) {
                        asd.setDefaultState(as);
                    }
                } catch (Exception ex) {
                    logger.warn("AssetType XML Error - Bad float in state: " + 
                                " [relativeMauCompleteness = "+ mcomp +"] [relativeMauSecurity = "+msec+"] ");
                    continue; // ignore this one & move on.
                }
            } //else, likely a text element - ignore
        }
        return asd;
    }
    
    
    
    
}


