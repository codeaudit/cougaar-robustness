/*
 * CrossDiagnosis.java
 *
 * Created on March 25, 2004, 3:09 PM
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

package org.cougaar.coordinator.techspec;

import java.util.Vector;

/**
 *
 * @author  Administrator
 */
public class CrossDiagnosis {
    
    private DiagnosisTechSpecInterface sensor;
    private AssetStateDimension isAffectedByStateDimension;
    private Vector probs;
    
    /** Creates a new instance of CrossDiagnosis */
    public CrossDiagnosis(DiagnosisTechSpecInterface sensor, AssetStateDimension affectedState) {
        
        this.sensor = sensor;
        isAffectedByStateDimension = affectedState;
        probs = new Vector();
    }

    /** @return the cross diagnosis sensor type */
    public String getSensorName() { return sensor.getName(); }
    
    /** @return the name of the cross diagnosis affected dimension */
    public String getAffectedDimensionName() { return isAffectedByStateDimension.getStateName(); }

    /** @return the cross diagnosis sensor  */
    public DiagnosisTechSpecInterface getSensor() { return sensor; }
    
    /** @return the cross diagnosis affected dimension */
    public AssetStateDimension getAffectedDimension() { return isAffectedByStateDimension; }
    
    
    /** Add a cross diagnosis probability */
    public void addProbability(DiagnosisProbability dp) {
     
        probs.add(dp);
    }

    /** @return the cross diagnosis probabilities */
    public Vector getProbabilities() {
     
        return probs;
    }
    
}
