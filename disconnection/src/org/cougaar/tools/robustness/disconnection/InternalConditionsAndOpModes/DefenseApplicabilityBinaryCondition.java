/*
 * DefenseApplicabilityCondition.java
 *
 * Created on March 19, 2003, 4:07 PM
 * 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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
    
    
