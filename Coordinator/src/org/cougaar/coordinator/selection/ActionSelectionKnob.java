/*
 * CostBenefitKnob.java
 *
 * Created on July 8, 2003, 4:13 PM
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

package org.cougaar.coordinator.selection;

/**
 *  Used to control aspects of the Action Selection plugin
 */

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.persist.NotPersistable;

public class ActionSelectionKnob implements NotPersistable {

    private int maxActions = 1;
    private double patienceFactor = 1.5; // how much extra time to give an Action to complete before giving up


    /** Creates a new instance of DefenseSelectionKnob */
    public ActionSelectionKnob() {

    }

    public void setMaxActions(int n) { maxActions = n; }
    public int getMaxActions() { return maxActions; }

    public void setPatienceFactor(double pf) { patienceFactor = pf; }
    public double getPatienceFactor() { return patienceFactor; }


    public static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionSelectionKnob ) {
                    return true ;
                }
                return false ;
            }
         };
         
}
