/*
 * CrossDiagnosisLoader.java
 *
 * Created on March 25, 2004, 2:38 PM
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

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class CrossDiagnosisLoader extends XMLLoader {
    
    Vector sensorTypes;
    DiagnosisTechSpecService diagnosisTechSpecService = null;
    
    /** Creates a new instance of CrossDiagnosisLoader */
    public CrossDiagnosisLoader() {
        
        super("CrossDiagnosis", "CrossDiagnoses"); 
        sensorTypes = new Vector();
    }
    
    
    private static final Class[] requiredServices = {
        DiagnosisTechSpecService.class
    };
    
    /* Acquire needed services */
    private boolean haveServices() { //don't use logger here... until after super.load() is called

            diagnosisTechSpecService =
            (DiagnosisTechSpecService) getServiceBroker().getService(this, DiagnosisTechSpecService.class, null);
            if (diagnosisTechSpecService == null) {
                throw new RuntimeException(
                "Unable to obtain tech spec service");
            } 
            
            return true;
            
    }
    
    public void load() {
        
        haveServices(); //call have services first !!!
        super.load(); //loads in & begins parsing of xml files.
        
    }
    
    
/*    
<CrossDiagnosis sensorType=”AgentCommunication” isAffectedByStateDimension=”Liveness” >
	<WhenActualStateIs name=”DEAD” >
<WillDiagnoseAs name=”DEGRADED” withProbability=”0.01” />
<WillDiagnoseAs name=”NOT_COMMUNICATING” withProbability=”0.99” />
</WhenActualStateIs>
</CrossDiagnosis>
*/    
    
    /** Called with a DOM "CrossDiagnosis" element to process */
    protected void processElement(Element element) {
        
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String sensorType = element.getAttribute("sensorType");
        String affectedBy = element.getAttribute("isAffectedByStateDimension");
                   
        CrossDiagnosis crossD = new CrossDiagnosis( sensorType, affectedBy);

        //Create a SensorType
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WhenActualStateIs") ) {
                e = (Element)child;
                String whenActualStateIs = e.getAttribute("name");
                DiagnosisProbability dp = new DiagnosisProbability(whenActualStateIs);
                //sensor.addDiagnosisProbability(dp);
                parseWhenActualStateIsElements(e, dp, whenActualStateIs);
            } //else, likely a text element - ignore            
        }

//        diagnosisTechSpecService.a

        
        logger.debug("Added new Cross Diagnosis for sensor: \n"+sensorType.toString() );
            
    }
    
    
    protected void execute() {}
    
    private void parseWhenActualStateIsElements(Element element, DiagnosisProbability probs, String whenActualStateIs) {
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WillDiagnoseAs") ) {
                Element e = (Element) child;
                String name = e.getAttribute("name");
                String p = e.getAttribute("withProbability");
                
                try {
                    float prob = Float.parseFloat(p);
                    probs.addProbability(name, prob);
                } catch (Exception ex) {
                    logger.warn("CrossDiagnosis XML Error for ["+whenActualStateIs+"]- Bad float in probability for ["+name+"]: " + p);
                    continue; // ignore this one & move on.
                }
            } //else, likely a text element - ignore
        }
    }
    
    
}


