/*
 * DiagnosisSnaphot.java
 *
 * Created on April 19, 2004, 2:43 PM
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
