/*
 * DefenseApplicabilityConditionSnapshot.java
 *
 * Created on October 28, 2003, 11:27 AM
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
public class DefenseApplicabilityConditionSnapshot {// extends DefenseCondition {

    /* 
     * The allowed values of the DefenseApplicabilityCondition. Set in the subclasses. 
     */
    //protected static OMCRangeList allowed_Values;
    
/*
    private long timeToBeDone;

    private long completionTimestamp = 0L;
    
*/
    public DefenseApplicabilityConditionSnapshot(DefenseApplicabilityCondition dc) {
  //      super(dc.getAssetType(), dc.getAsset(), dc.getDefenseName(), DefenseConstants.BOOL_RANGELIST, dc.getValue());
  //      timeToBeDone = dc.getTimeToBeDone();
        
  //      this.timestamp = dc.getTimestamp();
    }
   

    public long getTimeToBeDone() { return 0; }//timeToBeDone; }

    // @return the completion time - the time this defense set applicability to false 
    public long getCompletionTime() { return 0; } //completionTimestamp; }
    
    // set the completion time - the time this defense set applicability to false 
    public void setCompletionTime(long t) { 
    //    completionTimestamp = t; 
    }
    
//    private final static UnaryPredicate pred = new UnaryPredicate() {
//            public boolean execute(Object o) {  
//                return 
//                    (o instanceof DefenseApplicabilityCondition);
//            }
//        };
    
    
  /**
   * Update with new content. THis subtype has no additional slots so it
   * doesn't inspect the content. It just passes the object up for inspection
   **/
 //   public int updateContent(Object content, Relay.Token token) {
 //       return super.updateContent(content, token);
 //   }
   
    public static DefenseApplicabilityConditionSnapshot find(String defenseName, String expandedName, BlackboardService blackboard) {
/*
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
    */
        return null;
    } 
    

    public static DefenseApplicabilityConditionSnapshot find(String defenseName, String expandedName, Collection c) {
/*
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
    */
        return null;
    }
        
}
