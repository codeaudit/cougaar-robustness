/*
 * DefenseApplicabilityConditionSnapshot.java
 *
 * Created on October 28, 2003, 11:27 AM
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
public class DefenseApplicabilityConditionSnapshot extends DefenseCondition {

    /* 
     * The allowed values of the DefenseApplicabilityCondition. Set in the subclasses. 
     */
    //protected static OMCRangeList allowed_Values;
    

    private long timeToBeDone;

    private long completionTimestamp = 0L;
    

    public DefenseApplicabilityConditionSnapshot(DefenseApplicabilityCondition dc) {
        super(dc.getAssetType(), dc.getAsset(), dc.getDefenseName(), DefenseConstants.BOOL_RANGELIST, dc.getValue());
        timeToBeDone = dc.getTimeToBeDone();
        
        this.timestamp = dc.getTimestamp();
    }
   

    public long getTimeToBeDone() { return timeToBeDone; }

    /** @return the completion time - the time this defense set applicability to false */
    public long getCompletionTime() { return completionTimestamp; }
    
    /** set the completion time - the time this defense set applicability to false */
    public void setCompletionTime(long t) { completionTimestamp = t; }
    
    private final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DefenseApplicabilityCondition);
            }
        };
    
    
  /**
   * Update with new content. THis subtype has no additional slots so it
   * doesn't inspect the content. It just passes the object up for inspection
   **/
    public int updateContent(Object content, Relay.Token token) {
        return super.updateContent(content, token);
    }
    
    public static DefenseApplicabilityConditionSnapshot find(String defenseName, String expandedName, BlackboardService blackboard) {

        DefenseApplicabilityConditionSnapshot dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (DefenseApplicabilityConditionSnapshot)iter.next();
           if (dc.compareSignature(expandedName, defenseName)) {
               return dc;
           }
        }
        return null;
    } 
    

    public static DefenseApplicabilityConditionSnapshot find(String defenseName, String expandedName, Collection c) {

        DefenseApplicabilityConditionSnapshot dc = null;
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof DefenseApplicabilityConditionSnapshot) {
               dc = (DefenseApplicabilityConditionSnapshot) o;
               if (dc.compareSignature(expandedName, defenseName)) {
                return dc;
               }
           }
        }
        return null;
    }      
}
