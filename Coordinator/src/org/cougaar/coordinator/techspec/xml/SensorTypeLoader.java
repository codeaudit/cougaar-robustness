/*
 * SensorTypeLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
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
import java.util.Set;

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class SensorTypeLoader extends XMLLoader {
    
    Vector sensorTypes;
    DiagnosisTechSpecService diagnosisTechSpecService = null;
    
    /** Creates a new instance of SensorTypeLoader */
    public SensorTypeLoader(DiagnosisTechSpecService diagnosisTechSpecService, ServiceBroker serviceBroker, UIDService us) {
        
        super("SensorType", "SensorTypes", serviceBroker, us); //, requiredServices);
        sensorTypes = new Vector();
        this.diagnosisTechSpecService = diagnosisTechSpecService;
    }
    
    
    
    /* Acquire needed services */
    
    public void load() {    }
    
    
    /** Called with a DOM "SensorType" element to process */
    protected Vector processElement(Element element) {
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String sensorName = element.getAttribute("name");
        String type = element.getAttribute("sensesAssetType");
        String stateDim = element.getAttribute("sensesStateDimension");
        String lat = element.getAttribute("sensorLatency"); //int msec
        
        try {
            
            //covert sensor latency to an int
            int latency = Integer.parseInt(lat);
            
            AssetType sensesAssetType = AssetType.findAssetType(type);
            
            //what to do when assetType is null? - create it, process it later?
            if (sensesAssetType == null) {
                logger.error("SensorType XML Error - sensesAssetType unknown: "+type);
                return null;
            }
            
            AssetStateDimension asd = sensesAssetType.findStateDimension(stateDim);
            if (asd == null) {
                logger.error("SensorTypeLoader XML Error - senses state dimension not found! unknown sensesStateDimension : "+stateDim+ " for asset type = " + type);
                return null;
            }
                
            
            if (us == null) {
                logger.warn("SensorType XML Error - UIDService is null!");
                return null;
            }
            UID uid = us.nextUID();
            DiagnosisTechSpecImpl sensor = new DiagnosisTechSpecImpl( sensorName, uid, sensesAssetType, asd, latency);
            
            //Create a SensorType
            Element e;
            for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("PotentialDiagnoses") ) {
                    e = (Element)child;
                    parsePotentialDiagnoses(e, sensor);
                } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Diagnoses") ) {
                    e = (Element)child;
                    parseDiagnoses(e, sensor);
                } //else, likely a text element - ignore
            }

            //Add diagnosis to service
            diagnosisTechSpecService.addDiagnosisTechSpec( sensorName, sensor );
            
            logger.debug("Added new Sensor: \n"+sensor.toString() );
            
        } catch (NumberFormatException nfe) { //converting string to int
            logger.warn("SensorType XML Error for ["+sensorName+"]- Bad integer in latency: " + lat);
        }
        return null;
    }
    
    
    private void parsePotentialDiagnoses(Element element, DiagnosisTechSpecImpl sensor) {
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Diagnosis") ) {
                Element e = (Element) child;
                String diagnosis = e.getAttribute("name");
                String defaultValue = e.getAttribute("defaultValue");
                
                if (diagnosis != null) {
                    sensor.addPossibleValue(diagnosis);
                    if (defaultValue != null && defaultValue.equalsIgnoreCase("TRUE")) {
                        sensor.setDefaultValue(diagnosis);
                    }
                }
                
            } //else, likely a text element - ignore
        }
    }
    
    
    
    
    private void parseDiagnoses(Element element, DiagnosisTechSpecImpl sensor) {
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WhenActualStateIs") ) {
                Element e = (Element) child;
                String whenState = e.getAttribute("name");
                
                AssetState when_as;
                if (whenState.equals("*")) { when_as = AssetState.ANY; } 
                else {
                    when_as = sensor.getStateDimension().findAssetState(whenState);
                }
                if (when_as == null) {
                    logger.error("Sensor Type XML Error - diagnosis asset state not found! unknown WhenActualStateIs: "+whenState+ " [AssetStateDimension="+sensor.getStateDimension()+"]for sensor = " + sensor.getName());
                    return;
                }
                
                
                DiagnosisProbability dp = new DiagnosisProbability(when_as);
                sensor.addDiagnosisProbability(dp);
                parseWhenActualStateIsElements(e, dp, sensor);
                
            } //else, likely a text element - ignore
        }
    }
    
    
    
    
    private void parseWhenActualStateIsElements(Element element, DiagnosisProbability probs, DiagnosisTechSpecImpl sensor) {
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WillDiagnoseAs") ) {
                Element e = (Element) child;
                String willState = e.getAttribute("name");
                String p = e.getAttribute("withProbability");
    
                Set s= sensor.getPossibleValues();
                if ( s == null || !s.contains(willState) ) {
                    logger.error("Sensor Type XML Error - diagnosis state not found! unknown WillDiagnoseAs: "+willState+ " for sensor = " + sensor.getName());
                    return;
                }
                
                try {
                    float prob = Float.parseFloat(p);
                    probs.addProbability(willState, prob);
                } catch (Exception ex) {
                    logger.warn("SensorType XML Error for ["+sensor.getName()+"]- Bad float in probability for ["+probs.getActualState()+"]: " + p);
                    continue; // ignore this one & move on.
                }
            } //else, likely a text element - ignore
        }
    }
    
    
}


