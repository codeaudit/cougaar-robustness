/*
 * TimedDefenseDiagnosis.java
 *
 * Created on July 8, 2003, 4:17 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.timedDiagnosis;

import org.cougaar.coordinator.DefenseApplicabilityCondition;
import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.persist.NotPersistable;


import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * Temporarily this object will use a hashtable to store the data
 * required by consumers of it.
 * 
 */
public class TimedDefenseDiagnosis extends HashSet implements NotPersistable {
    
    /** The asset to which these diagnoses applies */
    String extendedAssetName;
    long firstDiagnosisTime;
    long publishTime;
    String disposition = null;
    long dispositionTime;
    
    /** Creates a new instance of TimedDefenseDiagnosis */
    public TimedDefenseDiagnosis(String extendedAssetName, long firstDiagnosisTime, long publishTime, Collection defenseConditionSnapshots) {
        //  THE CONDITION OBJECTS PROBABLY SHOULD BE CLONED TO PREVENT DEFENSES FROM MODIFYING THE SNAPSHOT 
        //  FUTURE WORK
        super(defenseConditionSnapshots);
        this.extendedAssetName = extendedAssetName;
        this.firstDiagnosisTime = firstDiagnosisTime;
        this.publishTime = publishTime;
    }
    
    public String getAssetName() {
        return extendedAssetName;
    }
    
    public void setDisposition(String disposition, long dispositionTime) {
        this.disposition = disposition;
        this.dispositionTime = dispositionTime;
    }
    
    public String toString() {
        String result = extendedAssetName+" has "+size()+" entries"+"\n";
        result = result+"First diagnosis at: "+firstDiagnosisTime+"\n";
        result = result+"Disposition: "+disposition+" at "+dispositionTime+"\n";
        Iterator iter = this.iterator();
        while (iter.hasNext()) {
            DefenseApplicabilityConditionSnapshot dac = (DefenseApplicabilityConditionSnapshot)iter.next();
            result = result+dac.getDefenseName()+":"+dac.getValue()+"\n";
        }
        return result;
    }
    
    private final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof TimedDefenseDiagnosis);
            }
        };
    
        
    public static TimedDefenseDiagnosis find(String expandedName, BlackboardService blackboard) {

        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        TimedDefenseDiagnosis tdd = null;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof TimedDefenseDiagnosis) {
               tdd = (TimedDefenseDiagnosis) o;
               if (tdd.getAssetName().equals(expandedName)) {
                return tdd;
               }
           }
        }
        return null;
    }      


}
