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

package org.cougaar.coordinator.examples.SampleDefense;

import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import org.cougaar.coordinator.*;  
import org.cougaar.coordinator.techspec.*;  
import org.cougaar.core.util.UID;

/**
 *
 * Stand-in for real XML Tech Spec.
 *
 */
public class SampleDiagnosisTechSpec implements DiagnosisTechSpecInterface  {
    
    HashSet pvs;
    
    /** Creates a new instance of SampleDiagnosisTechSpec */
    public SampleDiagnosisTechSpec() {

        pvs = new HashSet();
        pvs.add("Low Threat");
        pvs.add("Medium Threat");
        pvs.add("High Threat");
        pvs.add("No Threat");
    }
    
    /** @return the asset type that the threat cares about.
     */
    public AssetType getAssetType() {
        return AssetType.findAssetType("AGENT");
    }
    
    /** @return the vector of monitoring levels that this sensor supports
     *
     */
    public Vector getMonitoringLevels() {
        return null;
    }
    
    /** @return a user-readable name for this TechSpec
     *
     */
    public String getName() {
        return "MySampleDiagnosisTechSpec";
    }
    
    /** @return the possible values that Diagnoses generated by this Sensor can return via getValue()
     */
    public Set getPossibleValues() {
        return pvs;
    }
    
    /** @return a revision string for this TechSpec
     *
     */
    public String getRevision() {
        return "0.1";
    }
    
    /** @return the ThreatTypes associated with this Defense
     */
    public Vector getThreatTypes() {
        return null;
    }
    
    /** @return a unique cougaar level name for this TechSpec
     *
     */
    public UID getUID() {
        return null;
    }
    
}