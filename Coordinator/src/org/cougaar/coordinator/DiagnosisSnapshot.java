/*
 * DiagnosisSnaphot.java
 *
 * Created on April 19, 2004, 2:43 PM
 */

/*
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

package org.cougaar.coordinator;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */


import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


public class DiagnosisSnapshot {

    private final AssetID assetID;
    private final String sensorType;
    private final Object value;
    

    public DiagnosisSnapshot(Diagnosis d) {
        this.assetID = d.getAssetID();
        sensorType = DiagnosisUtils.getSensorType(d);
        value = d.getValue();
    }

    public AssetID getAssetID() { return assetID; }

    public String getSensorType() { return sensorType; }

    public Object getValue() { return value; }
   
    
    private final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DiagnosisSnapshot);
            }
        };

}
