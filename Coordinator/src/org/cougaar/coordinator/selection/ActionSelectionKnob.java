/*
 * CostBenefitKnob.java
 *
 * Created on July 8, 2003, 4:13 PM
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


package org.cougaar.coordinator.selection;

/**
 *  Used to control aspects of the Action Selection plugin
 */

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

public class ActionSelectionKnob  implements Serializable {

    private int maxActions;
    private double patienceFactor; // how much extra time to give an Action to complete before giving up
    private boolean isLeashed = true;  // are defenses unselectable?


    /** Creates a new instance of DefenseSelectionKnob */
    public ActionSelectionKnob(int maxActions, double patienceFactor) {
        this.maxActions = maxActions;
        this.patienceFactor = patienceFactor;
    }

    public void setMaxActions(int n) { maxActions = n; }
    public int getMaxActions() { return maxActions; }

    public void setPatienceFactor(double pf) { patienceFactor = pf; }
    public double getPatienceFactor() { return patienceFactor; }

    public void leash() { isLeashed = true; }
    public void unleash() { isLeashed = false; }
    public boolean isEnclaveLeashed() { return isLeashed; }


    public static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionSelectionKnob ) {
                    return true ;
                }
                return false ;
            }
         };
         
}
