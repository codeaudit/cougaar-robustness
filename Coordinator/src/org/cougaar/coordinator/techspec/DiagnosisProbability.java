/*
 * DiagnosisProbability.java
 *
 * Created on March 24, 2004, 3:01 PM
 * <copyright>
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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
public class DiagnosisProbability {
    
    AssetState actualState;
    Vector diagnosisProbs;
    
    
    /** Creates a new instance of DiagnosisProbability */
    public DiagnosisProbability(AssetState actualState) {
        
        this.actualState = actualState;
        diagnosisProbs = new Vector();
    }
    

    /** @return the actual state */
    public AssetState getActualState() {
        return actualState;
    }

    /** @return the diagnoses probabilities */
    public Vector getProbabilities() {
        return diagnosisProbs;
    }
    
    /** add a diagnosis probability */
    public void addProbability(String willBe, float prob) {
        diagnosisProbs.add(new DiagnosisProbability.DiagnoseAs(willBe, prob));
    }
    
    public class DiagnoseAs {
     
        String diagnosisState;
        float prob;
        
        public DiagnoseAs(String name, float prob) {
         
            this.diagnosisState = name;
            this.prob = prob;            
        }
        
        /** @return the name of the diagnosis state */
        public String getDiagnosisValue() { return diagnosisState; }

        /** @return the name of the diagnosis state */
        public float getProbability() { return prob; }
        
    }

    
}
