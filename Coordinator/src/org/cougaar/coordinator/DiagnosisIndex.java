/*
 * DiagnosesIndex.java
 *
 * Created on April 13, 2004, 12:04 PM
 */
/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.housekeeping.IndexKey;
import org.cougaar.core.persist.NotPersistable;

public class DiagnosisIndex implements NotPersistable {

    private Hashtable entries = new Hashtable();

    /** Creates new DiagnosesIndex */
    public DiagnosisIndex() {
    }
    
    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DiagnosisIndex);
            }
        };

    protected DiagnosesWrapper indexDiagnosis(DiagnosesWrapper dw, IndexKey key) {
        Diagnosis d = dw.getDiagnosis();
        Hashtable c = (Hashtable) entries.get(d.getAssetID());
        if (c == null) {
            c = new Hashtable();
            entries.put(d.getAssetID(), c);
            }
        return (DiagnosesWrapper) c.put(DiagnosisUtils.getSensorType(d), dw);
    }
    
    protected DiagnosesWrapper findDiagnosis(AssetID assetID, String sensorType) {
        Hashtable c = (Hashtable) findDiagnosisCollection(assetID);
        return (DiagnosesWrapper) c.get(sensorType);
    }

    protected Collection findDiagnosisCollection(AssetID assetID) {
        Hashtable c = (Hashtable) entries.get(assetID);
        if (c != null) return c.values();
        else return null;
    }

}
