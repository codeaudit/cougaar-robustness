/*
 * ActuatorTypeLoader.java
 *
 * Created on April 5, 2004, 2:45 PM
 * <copyright>  
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
import org.cougaar.core.component.ServiceBroker;

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
 *
 * @author  Administrator
 */
public class ActuatorTypeLoader extends XMLLoader {
    
   
    Vector actuatorTypes;
    ActionTechSpecService actionTechSpecService = null;
    
    static final String bandwidthMapFile = "BandwidthCostMap.xml";
    private Hashtable bandwidthCostMap = null;

    static final String cpuMapFile = "CPUCostMap.xml";
    private Hashtable cpuCostMap = null;

    static final String memoryMapFile = "MemoryCostMap.xml";
    private Hashtable memoryCostMap = null;
    private org.cougaar.util.ConfigFinder cf;
    
    /** Creates a new instance of SensorTypeLoader */
    public ActuatorTypeLoader(ActionTechSpecService actionTechSpecService, ServiceBroker serviceBroker, UIDService us, org.cougaar.util.ConfigFinder cf) {
        
        super("ActuatorType", "ActuatorTypes", serviceBroker, us); //, requiredServices);
        actuatorTypes = new Vector();
        this.cf = cf;
        this.actionTechSpecService = actionTechSpecService;
    }
    
    
    public void load() {
        
        try {
            bandwidthCostMap = MapLoader.loadMap(cf, bandwidthMapFile);
            if (bandwidthCostMap == null) {
                logger.error("Error loading Bandwidth Cost Map file [" + bandwidthMapFile + "]. ");
            }

            cpuCostMap = MapLoader.loadMap(cf, cpuMapFile);
            if (cpuCostMap == null) {
                logger.error("Error loading CPU Cost map file [" + cpuMapFile + "]. ");
            }

            memoryCostMap = MapLoader.loadMap(cf, memoryMapFile);
            if (memoryCostMap == null) {
                logger.error("Error loading Memory Cost Map file [" + memoryMapFile + "]. ");
            }
            
        } catch (Exception e) {
            logger.error("Error parsing XML file Error was: ", e);
            return;
        }
        
        
    }
    
    
    /** Called with a DOM "SensorType" element to process */
    protected Vector processElement(Element element) {
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String actuatorName = element.getAttribute("name");
        String type = element.getAttribute("affectsAssetType");
        String stateDim = element.getAttribute("affectsStateDimension");
        String actionType = element.getAttribute("actionType");
        
        AssetType affectsAssetType = AssetType.findAssetType(type);

        //what to do when assetType is null? - create it, process it later?
        if (affectsAssetType == null) {
            logger.warn("ActuatorType XML Error - affectsAssetType unknown: "+type);
            return null;
        }

        AssetStateDimension asd = affectsAssetType.findStateDimension(stateDim);
        //what to do when assetType is null? - create it, process it later?
        if (asd == null) {
            logger.error("ActuatorType["+actuatorName+"]  XML Error - asset state dimension not found! unknown dimension: "+stateDim+ " for asset type = " + type);
            return null;
        }
        
        //assign actionType
        int actionTypeInt;
        if (actionType == null || actionType.equalsIgnoreCase("CORRECTIVE") ) { actionTypeInt = ActionTechSpecInterface.CORRECTIVE_ACTIONTYPE; }
        else if (actionType.equalsIgnoreCase("PREVENTIVE") ) { actionTypeInt = ActionTechSpecInterface.PREVENTIVE_ACTIONTYPE; }
        else if (actionType.equalsIgnoreCase("APPLICATION") ) { actionTypeInt = ActionTechSpecInterface.APPLICATION_ACTIONTYPE; }
        else if (actionType.equalsIgnoreCase("COMPENSATORY") ) { actionTypeInt = ActionTechSpecInterface.COMPENSATORY_ACTIONTYPE; }
        else {
            actionTypeInt = ActionTechSpecInterface.CORRECTIVE_ACTIONTYPE; //default
            logger.warn("ActuatorType["+actuatorName+"]  did not specify an action type. Defaulting to corrective.");
        }
        
        if (us == null) {
            logger.warn("ActuatorType["+actuatorName+"]  XML Error - UIDService is null!");
            return null;
        }
        UID uid = us.nextUID();
        ActionTechSpecImpl actuator = new ActionTechSpecImpl( actuatorName, uid, affectsAssetType, asd, actionTypeInt);
        
        //Create an ActuatorType
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Action") ) {
                e = (Element)child;
                parseAction(e, actuator, affectsAssetType, asd);
            } //else, likely a text element - ignore
        }

        //Add action to service
        actionTechSpecService.addActionTechSpec( actuatorName, actuator );
        
        logger.debug("Added new Actuator: \n"+actuator.toString() );
        return null;
    }
    
    
    private void parseAction(Element element, ActionTechSpecImpl actuator, AssetType assetType, AssetStateDimension asd) {

        String actionName = element.getAttribute("name");
        String defaultAction = element.getAttribute("aDefaultActionOffered");
        boolean isDefault = false;
        if (defaultAction != null && defaultAction.equalsIgnoreCase("TRUE")) {
            isDefault = true;
        }
            
        String description = null; 
        
        ActionDescription ad = new ActionDescription(actionName, assetType, asd, isDefault);
        
        //Create an ActionDescription
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Description") ) {
                e = (Element)child;
                description = e.getAttribute("value");
                ad.setDescription(description);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Transition") ) {
                e = (Element)child;
                parseTransition(e, ad, assetType, asd);
            } //else, likely a text element - ignore
        }
        
        //Now verify that all states are covered by this action - we have closure -- if not, do not add action.
        boolean error = false;
        Iterator states = asd.getPossibleStates().iterator();
        while ( states.hasNext() ) {
            AssetState as = (AssetState) states.next();
            if ( ad.getTransitionForState(as) == null) {
                logger.error("parseAction() - ERROR: cannot find transition from "+assetType+":"+asd.getStateName()+":"+as.getName()+ " --> IGNORING ACTION ["+ad.name()+"]!!");
                error = true;
            }
        }

        if (!error) {
            actuator.addAction(ad);
        }
        
    }


    private void parseTransition(Element element, ActionDescription desc, AssetType assetType, AssetStateDimension asd) {

        String whenState = element.getAttribute("WhenActualStateIs");
        String endState = element.getAttribute("EndStateWillBe");
        String interState = element.getAttribute("IntermediateStateWillBe");
 
        //desc.setWhenStateIs(whenState);
        //desc.setEndStateWillBe(endState);
          
        AssetState when_as;
        if (whenState.equals("*")) { when_as = AssetState.ANY; } 
        else {        
            when_as = desc.getAffectedStateDimension().findAssetState(whenState);
        }
        if (when_as == null) {
            logger.error("Actuator["+desc.name()+"] XML Error - asset state not found! unknown WhenActualStateIs: "+whenState+ " for asset type = " + desc.getAffectedAssetType());
            return;
        }

        AssetState inter_as = desc.getAffectedStateDimension().findAssetState(interState);
        if (inter_as == null) {
            logger.error("Actuator["+desc.name()+"] XML Error - asset state not found! unknown IntermediateStateWillBe: "+interState+ " for asset type = " + desc.getAffectedAssetType());
            return;
        }
        
        AssetState end_as = desc.getAffectedStateDimension().findAssetState(endState);
        if (end_as == null) {
            logger.error("Actuator["+desc.name()+"] XML Error - asset state not found! unknown EndStateWillBe: "+endState+ " for asset type = " + desc.getAffectedAssetType());
            return;
        }
        
        
        
        
        AssetTransitionWithCost atwc = new AssetTransitionWithCost(assetType, asd, when_as, end_as, inter_as );

        desc.addTransition(atwc);

        //Creating ActionCosts
        Element e;
        ActionCost ac;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("OneTimeCost") ) {
                e = (Element)child;
                ac = parseCost(e);
                atwc.setActionCost(ac, true);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("ContinuingCost") ) {
                e = (Element)child;
                ac = parseCost(e);
                atwc.setActionCost(ac, false);
            } //else, likely a text element - ignore
        }
    }
    

    private ActionCost parseCost(Element element) {

        //Creating ActionCosts
        Element e;
        ActionCost ac = new ActionCost();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Bandwidth") ) {
                e = (Element)child;
                parseBCM(e, ac, "Bandwidth", bandwidthCostMap);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("CPU") ) {
                e = (Element)child;
                parseBCM(e, ac, "CPU", cpuCostMap);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Memory") ) {
                e = (Element)child;
                parseBCM(e, ac, "Memory", memoryCostMap);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Time") ) {
                e = (Element)child;
                String duration = e.getAttribute("duration");
                try { 
                    int dur = Integer.parseInt(duration);
                    ac.setTimeCost(dur);
                } catch (NumberFormatException nfe) {
                    logger.warn("ActuatorType XML Error - NumberFormatException in Time (cost) element for duration ["+duration+"]. Setting to 0;");
                }
            } //else, likely a text element - ignore
        }
        return ac;
    }
    

    /** Parse the bandwidth, CPU, and Memory cost elements */
    private void parseBCM(Element e, ActionCost ac, String costName, Hashtable costMap) {
        
        String intensity = e.getAttribute("intensity");
        String agentSizeFactorBool = e.getAttribute("agentSizeFactor");
        String msgSizeFactorBool = e.getAttribute("msgSizeFactor");

        boolean asf = Boolean.getBoolean(agentSizeFactorBool);
        boolean msf = Boolean.getBoolean(msgSizeFactorBool);

       //Map intensity String to a float.
        float fIntensity = 0;
        if (costMap != null) {
            Float f = (Float) costMap.get(intensity);
            if (f != null) {
                fIntensity = f.floatValue();
            }
        }
        
        ac.setCost(costName, fIntensity, asf, msf);
    }
    
}
