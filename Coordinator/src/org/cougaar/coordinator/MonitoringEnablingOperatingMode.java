/*
 * MonitoringEnablingOperatingMode.java
 *
 * Created on March 19, 2003, 4:09 PM
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

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.util.UID;

/**
 * The Defense creates a MonitoringEnablingOperatingMode instance for each asset 
 * it can defend. The Defense only <b>queries</b> this instance (via the isEnabled()
 * method. The value return states whether the Defense should be monitoring the asset 
 * or not (as described in the Defense Deconfliction API & Architecture paper).
 */
public class MonitoringEnablingOperatingMode extends DefenseOperatingMode {
    
    
    /** Creates a new instance of MonitoringEnablingOperatingMode. This mode 
     *  supports two values: ENABLED and DISABLED. The default is set to DISABLED.
     *@param name - the name of the OperatingMode
     */
    public MonitoringEnablingOperatingMode(String assetType, String asset, String defenseName) {
        
        super(assetType, asset, defenseName, DefenseConstants.MON_RANGELIST, DefenseConstants.MON_DISABLED.toString());

    }
    
    /*
     * @return TRUE if the state of this mode interface ENABLED. Returns
     * FALSE if DISABLED. Throws an exception if neither (should not happen).
     */
    public boolean isEnabled() throws UnmappableValueException { 
        if (getValue().compareTo(DefenseConstants.MON_ENABLED) == 0)
            return true;
        if (getValue().compareTo(DefenseConstants.MON_DISABLED) == 0)
            return false;
        throw new UnmappableValueException("Value was "+getValue());
    }

    static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof MonitoringEnablingOperatingMode);
            }
        };
        
        
  // searches the BB for an object of this type with a given signature 
    public static MonitoringEnablingOperatingMode find(String defenseName, String expandedName, BlackboardService blackboard) {

        MonitoringEnablingOperatingMode meom = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           meom = (MonitoringEnablingOperatingMode)iter.next();
           if (meom.compareSignature(expandedName, defenseName)) {
               return meom;
           }
        }
        return null;
    }  


    public static MonitoringEnablingOperatingMode find(String defenseName, String expandedName, Collection c) {

        MonitoringEnablingOperatingMode meom = null;
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof MonitoringEnablingOperatingMode) {
               meom = (MonitoringEnablingOperatingMode) o;
               if (meom.compareSignature(expandedName, defenseName)) {
                return meom;
               }
           }
        }
        return null;
    }  

    public static MonitoringEnablingOperatingMode find(UID uid, BlackboardService blackboard) {

        MonitoringEnablingOperatingMode meom = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           meom = (MonitoringEnablingOperatingMode)iter.next();
           if (meom.compareSignature(uid)) {
               return meom;
           }
        }
        return null;
    }  

}
