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

package org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes;

import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.relay.Relay;

import org.cougaar.core.util.UID;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;
/**
 * The defense uses this object to signal that it believes it has detected 
 * a problem with a particular asset and that it believes it can solve the 
 * problem.
 *
 * This class is abstract. Subclasses may implement different types of belief 
 * representation, e.g. TRUE|FALSE, a confidence level, etc.
 */
public abstract class DefenseApplicabilityCondition extends DefenseCondition {

    /* 
     * The allowed values of the DefenseApplicabilityCondition. Set in the subclasses. 
     */
    //protected static OMCRangeList allowed_Values;
    
    
    /*
     * Do not use. Only use the constructors of the subclasses.
     */
    public DefenseApplicabilityCondition(String assetType, String asset, String defenseName, OMCRangeList allowedValues) {
        super(assetType, asset, defenseName, allowedValues);
    }
   
    /*
     * Do not use. Only use the constructors of the subclasses.
     */
    public DefenseApplicabilityCondition(String assetType, String asset, String defenseName, OMCRangeList allowedValues, java.lang.Comparable initialValue) {
        super(assetType, asset, defenseName, allowedValues, initialValue);
    }    
    
    protected void setValue(String newValue) {
        super.setValue(newValue);
    }
    
    
  /**
   * Update with new content. THis subtype has no additional slots so it
   * doesn't inspect the content. It just passes the object up for inspection
   **/
  public int updateContent(Object content, Relay.Token token) {
        return super.updateContent(content, token);
  }
    
    public static DefenseApplicabilityCondition findOnBlackboard(String defenseName, String expandedName, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DefenseApplicabilityCondition);
            }
        };

        DefenseApplicabilityCondition dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (DefenseApplicabilityCondition)iter.next();
           if (dc.compareSignature(expandedName, defenseName)) {
               return dc;
           }
        }
        return null;
    }      
    
}
