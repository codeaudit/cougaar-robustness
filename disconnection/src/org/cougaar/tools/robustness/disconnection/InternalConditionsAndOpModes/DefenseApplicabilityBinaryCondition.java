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
import org.cougaar.core.adaptivity.OMCPoint;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.relay.Relay;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This class supports a binary condition - TRUE or FALSE. The defense sets this value 
 * to TRUE if it believes it has detected a problem with a particular asset and that 
 * it believes it can solve the problem.
 */
public class DefenseApplicabilityBinaryCondition extends DefenseApplicabilityCondition {

    
    public String testSlot = "test";
    
    /** Creates a new instance of DefenseApplicabilityBinaryCondition */
    public DefenseApplicabilityBinaryCondition(String assetType, String asset, String defenseName) {
        super(assetType, asset, defenseName, DefenseConstants.BOOL_RANGELIST);       
    }
   
    public DefenseApplicabilityBinaryCondition(String assetType, String asset, String defenseName, DefenseConstants.OMCStrBoolPoint initialValue) {
        super(assetType, asset, defenseName, DefenseConstants.BOOL_RANGELIST, initialValue.toString());
    }

    /* Called by Defense to set current condition. Limited to statically defined values
     * of this class. **This methd should NOT be public as anyone can modify the value.
     * Rather should be subclassed, and super.setValue() called.
     *@param new value
     */
    protected void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
        super.setValue(newValue.toString());
    }
    

  /**
   * Update with new content. 
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. This implementation returns true only
   * if the new value differs from the current value.
   **/
  public int updateContent(Object content, Relay.Token token) {
    /* removed as unnecessary sjf 8/29/2003
    if (token != owner) {
        Logger logger = Logging.getLogger(getClass());
        if (logger.isInfoEnabled()) {
          logger.info(
            "Ignoring \"Not owner\" bug in \"updateContent()\","+
            " possibly a rehydration bug (token="+
            token+", owner="+owner+")");
        }
    }
    */
    DefenseApplicabilityBinaryCondition newDC = (DefenseApplicabilityBinaryCondition) content;
    if (!testSlot.equals(newDC.testSlot)) {
        this.testSlot = newDC.testSlot;
        super.updateContent(content, token);
        return Relay.CONTENT_CHANGE;
    } else {
        return super.updateContent(content, token);
    }
  }
    
        
}
    
    
