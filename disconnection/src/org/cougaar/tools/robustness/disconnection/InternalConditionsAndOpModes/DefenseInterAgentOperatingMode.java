/*
 * DefenseInterAgentOperatingMode.java
 *
 * Created on March 20, 2003, 3:47 PM
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

import org.cougaar.core.adaptivity.InterAgentOperatingMode;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.util.UID;

/**
 * This class is used by DefenseApplicabilityCondition(s) when the AdaptivityEngine(AE)
 * is located in a different agent. In this situation the published conditions require 
 * a relay (aka a remote surrogate) in the AE agent's Blackboard. This class provides 
 * that linkage transparently (in conjunction with the RemoteDefenseConditionMgrPlugin).
 *
 * Instances of this class are only intended to be created by the 
 * RemoteDefenseConditionMgrPlugin.
 *
 * @author  Paul Pazandak, pazandak@objs.com
 */
public class DefenseInterAgentOperatingMode { // extends DefenseOperatingMode {

    /** 
     *
     *   ********************** NOT USED *********************
     *
     *Creates a new instance of DefenseInterAgentOperatingMode using the values 
     *  from the supplied DefenseCondition.
     *@param dac - the DefenseCondition we'return creating this instance for.
     *@param uid - the uid for this instance.
     */
//    DefenseInterAgentOperatingMode(DefenseCondition dac, UID uid) {

        //NOT USED***
        //super(dac.getName(), dac.getAllowedValues(), dac.getValue());        
        //setUID(uid);
//    }    
}
