/*
 * DefenseInterAgentOperatingMode.java
 *
 * Created on March 20, 2003, 3:47 PM
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
