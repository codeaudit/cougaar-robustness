/*
 * DefenseEnablingOperatingMode.java
 *
 * Created on March 19, 2003, 4:08 PM
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

import org.cougaar.core.adaptivity.OMCPoint;
import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

/**
 * The Defense publishes a DefenseEnablingOperatingMode for each asset it may defend.
 * The DefenseEnablingOperatingMode is used to control the actions of the Defense as
 * described in the Defense Deconfliction API & Architecture paper. The Defense does
 * <b>NOT</b> change the values of this instance. It should only call getState() to find
 * what the value is & then act accordingly.
 */
public class DefenseEnablingOperatingMode extends DefenseOperatingMode {
    
    
    /** Creates a new instance of DefenseEnablingOperatingMode. This mode 
     *  supports two values: ENABLED and DISABLED. The default is set to DISABLED.
     *@param name - the name of the OperatingMode
     */
    public DefenseEnablingOperatingMode(String assetType, String asset, String defenseName) {
        
        super(assetType, asset, defenseName, DefenseConstants.DEF_RANGELIST, DefenseConstants.DISCONNECT_DENIED.toString());

    }
    
    /*
     * @return the String value of the state of this mode.
     */
    public String getState() { 
        return getValue().toString();
    }
    
  
    // searches the BB for an object of this type with a given signature 
    public static DefenseEnablingOperatingMode findOnBlackboard(String defenseName, String expandedName, BlackboardService blackboard) {
        UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DefenseEnablingOperatingMode);
            }
        };

        DefenseEnablingOperatingMode deom = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           deom = (DefenseEnablingOperatingMode)iter.next();
           if (deom.compareSignature(expandedName, defenseName)) {
               return deom;
           }
        }
        return null;
    }  
}