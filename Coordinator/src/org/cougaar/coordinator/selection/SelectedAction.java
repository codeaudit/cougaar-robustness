/*
 * SelectedAction.java
 *
 * Created on July 9, 2003, 9:26 AM
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

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author  David Wells - OBJS
 */
public class SelectedAction extends EnablingControl implements NotPersistable {
    /** Base class for SelectedAction(s) with varius kinds of preconditions */
    
    private Set actionVariants;
    private long patience;
    private Precondition precondition;
    
    public SelectedAction(Action action, Set actionVariants, long patience, CostBenefitEvaluation cbe) {
        super(action, cbe);
        this.actionVariants = actionVariants;
        this.patience = patience;
        this.precondition = null;
    }
        
    public Set getActionVariants() {
        return actionVariants;
    }

    public long getPatience () {
        return patience;
    }

    public String toString() {
        String buff = "From Action: ";
        buff = buff + super.getAction().toString() + "\n";
        Iterator iter = actionVariants.iterator();
        while (iter.hasNext()) {
            buff = buff + "Selected: " + ((VariantEvaluation)iter.next()).toString() + "\n";
            }
        buff = buff + patience + "\n";
        return buff;
    }

    
    public static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof SelectedAction ) {
                return true ;
            }
            return false ;
        }
     };
     
}
