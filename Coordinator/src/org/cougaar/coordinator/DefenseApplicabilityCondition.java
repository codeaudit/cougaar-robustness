/*
 * DefenseApplicabilityCondition.java
 *
 * Created on March 19, 2003, 4:07 PM
 * 
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

package org.cougaar.coordinator;

import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.relay.Relay;

import org.cougaar.core.util.UID;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * The defense uses this object to signal that it believes it has detected 
 * a problem with a particular asset and that it believes it can solve the 
 * problem.
 *
 * This class is abstract. Subclasses may implement different types of belief 
 * representation, e.g. TRUE|FALSE, a confidence level, etc.
 */
public abstract class DefenseApplicabilityCondition  extends DefenseCondition {

    /* 
     * The allowed values of the DefenseApplicabilityCondition. Set in the subclasses. 
     */
    //protected static OMCRangeList allowed_Values;

    private long timeToBeDone = 0L;
    
    /*
     * Do not use. Only use the constructors of the subclasses.
     */
    protected DefenseApplicabilityCondition(String assetType, String asset, String defenseName, OMCRangeList allowedValues) {
        super(assetType, asset, defenseName, allowedValues);
    }
   
    /*
     * Do not use. Only use the constructors of the subclasses.
     */
    protected DefenseApplicabilityCondition(String assetType, String asset, String defenseName, OMCRangeList allowedValues, java.lang.Comparable initialValue) {
        super(assetType, asset, defenseName, allowedValues, initialValue);
    }    
    
    /**
     *
     *
     *   TEMPORARILY PUBLIC!!!!! SHould be protected.
     *
     */
    public void setValue(String newValue) {
        super.setValue(newValue);
    }
    
    public void setTimeToBeDone (long time) { timeToBeDone = time; }
    
    public long getTimeToBeDone () { return timeToBeDone; }
    
    private final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return false;
            }
        };
    
    
  /**
   * Update with new content. THis subtype has no additional slots so it
   * doesn't inspect the content. It just passes the object up for inspection
   **/
    public int updateContent(Object content, Relay.Token token) {
        return super.updateContent(content, token);
    }
    
    public static DefenseApplicabilityCondition find(String defenseName, String expandedName, BlackboardService blackboard) {

        return null;
    } 
    
    public static DefenseApplicabilityCondition find(UID uid, BlackboardService blackboard) {

        return null;
    }     

    public static Collection findCollection(String expandedName, BlackboardService blackboard) {

        return null;
    }    

    public static Collection findCollection(String expandedName, Collection c) {
/*
        Collection result = new HashSet();
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
           DefenseCondition dc = (DefenseCondition)iter.next();
           if ((dc instanceof DefenseApplicabilityCondition) && (dc.getExpandedName().equals(expandedName))) {
               result.add(dc);
           }
        }
 */
        return null; //2004result;
    }    

    public static Collection findDefenseCollection(String defenseName, Collection c) {

        return null;
    }    
    
    public static Collection findDefenseCollection(String defenseName, BlackboardService blackboard) {

        return null;
    } 
    
    public static DefenseApplicabilityCondition find(String defenseName, String expandedName, Collection c) {

        return null;
    }      
}
