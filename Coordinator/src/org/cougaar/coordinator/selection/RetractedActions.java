/*
 * RetractedActions.java
 *
 * Created on July 6, 2004, 4:41 PM
 *
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.core.persist.NotPersistable;
import java.util.Set;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.costBenefit.VariantEvaluation;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.util.UnaryPredicate;

public class RetractedActions extends EnablingControl implements NotPersistable {

    private Object retractedVariant;

    /** Creates new RetractedActions */
    public RetractedActions(Action action, Object retractedVariant, CostBenefitEvaluation cbe) {
        super(action, cbe);
        this.retractedVariant = retractedVariant;
    }

    public RetractedActions(Action action, CostBenefitEvaluation cbe) {
        super(action, cbe);
        this.retractedVariant = null;
    }
    
    public Object getRetractedVariant() { return retractedVariant; }

    public String toString() {
        String buff = "RetractedActions: ";
        buff = buff + super.getAction().toString() + "\n";
        buff = buff + "retracting: ";
        buff = buff + retractedVariant == null?"ALL":retractedVariant.toString() + "\n";
        return buff;
    }

    
    public static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof RetractedActions ) {
                return true ;
            }
            return false ;
        }
     };
     }
