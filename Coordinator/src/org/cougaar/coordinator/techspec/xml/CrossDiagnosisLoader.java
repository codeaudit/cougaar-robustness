/*
 * CrossDiagnosisLoader.java
 *
 * Created on March 25, 2004, 2:38 PM
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

import org.cougaar.core.component.ServiceBroker;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class CrossDiagnosisLoader extends XMLLoader {
    
    Vector sensorTypes;
    DiagnosisTechSpecService diagnosisTechSpecService;
    
    /** Creates a new instance of CrossDiagnosisLoader */
    public CrossDiagnosisLoader(DiagnosisTechSpecService diagnosisTechSpecService, ServiceBroker serviceBroker, UIDService us) {
        
        super("CrossDiagnosis", "CrossDiagnoses", serviceBroker, us); 
        sensorTypes = new Vector();
        this.diagnosisTechSpecService = diagnosisTechSpecService;
    }
    
    
    public void load() {    }
    
    
/*    
<CrossDiagnosis sensorType=”AgentCommunication” isAffectedByStateDimension=”Liveness” >
	<WhenActualStateIs name=”DEAD” >
<WillDiagnoseAs name=”DEGRADED” withProbability=”0.01” />
<WillDiagnoseAs name=”NOT_COMMUNICATING” withProbability=”0.99” />
</WhenActualStateIs>
</CrossDiagnosis>
*/    
    
    /** Called with a DOM "CrossDiagnosis" element to process */
    protected Vector processElement(Element element) {
        
        
        //publish to BB during execute().
        //1. Create a new AssetType instance &
        String sensorType = element.getAttribute("sensorType");
        String affectedBy = element.getAttribute("isAffectedByStateDimension");
        
        DiagnosisTechSpecInterface dtsi = diagnosisTechSpecService.getDiagnosisTechSpec(sensorType);
        if (dtsi == null) {
            logger.error("CrossDiagnosis XML Error - sensor type not found = " + sensorType );
            return null;
        }
        
        AssetStateDimension affectedBy_asd = dtsi.getAssetType().findStateDimension(affectedBy);
        if (affectedBy_asd == null) {
            logger.error("CrossDiagnosis XML Error -  asset state dimension not found! unknown isAffectedByStateDimension dimension: "+affectedBy+ " for asset type = " + dtsi.getAssetType());
            return null;
        }
        
        
        CrossDiagnosis crossD = new CrossDiagnosis( dtsi, affectedBy_asd);

        //Create a SensorType
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WhenActualStateIs") ) {
                e = (Element)child;
                String whenActualStateIs = e.getAttribute("name");
                
                AssetState when_as = affectedBy_asd.findAssetState(whenActualStateIs);
                if (when_as == null) {
                    logger.error("Event XML Error - direct effect asset state not found! unknown WhenActualStateIs : "+whenActualStateIs+ " for asset type = " + dtsi.getAssetType());
                    return null;
                }
                
                
                DiagnosisProbability dp = new DiagnosisProbability(when_as);
                //sensor.addDiagnosisProbability(dp);
                parseWhenActualStateIsElements(e, dp, whenActualStateIs, dtsi, affectedBy_asd);
            } //else, likely a text element - ignore            
        }

        diagnosisTechSpecService.addCrossDiagnosis(crossD);
        
        if (logger.isDebugEnabled()) logger.debug("Added new Cross Diagnosis for sensor: \n"+sensorType.toString() );
            
        return null;
    }
    
    
    private void parseWhenActualStateIsElements(Element element, DiagnosisProbability probs, String whenActualStateIs, DiagnosisTechSpecInterface dtsi, AssetStateDimension affectedBy_asd ) {
        
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("WillDiagnoseAs") ) {
                Element e = (Element) child;
                String willState = e.getAttribute("name");
                String p = e.getAttribute("withProbability");
                
                AssetState will_as = dtsi.getStateDimension().findAssetState(willState);
                if (will_as == null) {
                    logger.error("CrossDiagnosis XML Error - diagnosis asset state not found! unknown WillDiagnoseAs: "+willState+ " for asset type = " + dtsi.getAssetType());
                    return;
                }
                
                Set s= dtsi.getPossibleValues();
                if ( s == null || !s.contains(willState) ) {
                    logger.error("CrossDiagnosis XML Error - diagnosis state not found! unknown WillDiagnoseAs: "+willState+ " for sensor = " + dtsi.getName());
                    return;
                }
                
                
                try {
                    float prob = Float.parseFloat(p);
                    probs.addProbability(willState, prob);
                } catch (Exception ex) {
                    logger.warn("CrossDiagnosis XML Error for ["+whenActualStateIs+"]- Bad float in probability for ["+willState+"]: " + p);
                    continue; // ignore this one & move on.
                }
            } //else, likely a text element - ignore
        }
    }
    
    
}


