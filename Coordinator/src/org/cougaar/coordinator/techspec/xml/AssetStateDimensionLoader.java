/*
 * AssetStateDimensionLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
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
                if (logger.isDebugEnabled()) logger.debug(asd.toString());

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


