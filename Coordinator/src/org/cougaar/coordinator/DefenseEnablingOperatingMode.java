/*
 * DefenseEnablingOperatingMode.java
 *
 * Created on March 19, 2003, 4:08 PM
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

import org.cougaar.core.util.UID;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;

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
        
        super(assetType, asset, defenseName, DefenseConstants.DEF_RANGELIST, DefenseConstants.DEF_DISABLED.toString());

    }
    
    /*
     * @return the String value of the state of this mode.
     */
    public String getState() { 
        return getValue().toString();
    }
    
    static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DefenseEnablingOperatingMode);
            }
        };

    // searches the BB for an object of this type with a given signature 
    public static DefenseEnablingOperatingMode find(String defenseName, String expandedName, BlackboardService blackboard) {

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
    
    public static DefenseEnablingOperatingMode find(UID uid, BlackboardService blackboard) {

        DefenseEnablingOperatingMode deom = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           deom = (DefenseEnablingOperatingMode)iter.next();
           if (deom.compareSignature(uid)) {
               return deom;
           }
        }
        return null;
    }
    
    // searches the BB for an object of this type with a given signature 
    public static DefenseEnablingOperatingMode find(String defenseName, String expandedName, Collection c) {

        DefenseEnablingOperatingMode deom = null;
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof DefenseEnablingOperatingMode) {
               deom = (DefenseEnablingOperatingMode) o;
               if (deom.compareSignature(expandedName, defenseName)) {
                return deom;
               }
           }
        }
        return null;
    } 

    public static Collection findDefenseCollection(String defenseName, Collection c) {

        Collection result = new HashSet();
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
           DefenseEnablingOperatingMode dc = (DefenseEnablingOperatingMode)iter.next();
           if ((dc instanceof DefenseEnablingOperatingMode) && (dc.getDefenseName().equals(defenseName))) {
               result.add(dc);
           }
        }
        return result;
    }    
    
    public static Collection findDefenseCollection(String defenseName, BlackboardService blackboard) {

        Collection c = blackboard.query(pred);
        Collection result = new HashSet();
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
           DefenseEnablingOperatingMode dc = (DefenseEnablingOperatingMode)iter.next();
           if ((dc instanceof DefenseEnablingOperatingMode) && (dc.getDefenseName().equals(defenseName))) {
               result.add(dc);
           }
        }
        return result;
    } 
}