/*
 * ActionPatience.java
 *
 * Created on September 18, 2003, 2:06 PM
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


package org.cougaar.coordinator.activation;

import org.cougaar.core.util.UID;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class ActionPatience implements NotPersistable {

    Action action;
    Set allowedVariants;
    Action.CompletionCode result = null;
    long timeoutTime;
    long startTime;
    CostBenefitEvaluation cbe;

    /** Creates new ActionPatience */
    public ActionPatience(Action action, Set allowedVariants, long timeoutTime, CostBenefitEvaluation cbe) {
        this.action = action;
        this.allowedVariants = allowedVariants;
        this.timeoutTime = timeoutTime;
        startTime = System.currentTimeMillis();
        this.cbe = cbe;
    }

    public Action getAction() { return action; }

    public Set getAllowedVariants() { return allowedVariants; }

    public long getDuration() { return timeoutTime; }

    public long getStartTime() { return startTime; }

    public void setResult(Action.CompletionCode result) { this.result = result; }

    public Action.CompletionCode getResult() { return result; }

    public CostBenefitEvaluation getCBE() { return cbe; }

    public String toString() {
        String buff = "<ActionPatience: \n";
        buff = buff + action.toString() + "\n";
        buff = buff + "waiting for: " + allowedVariants.toString() + "\n";
        buff = buff + "startTime=" + getStartTime() + ", duration=" + getDuration() + "\n";
        return buff;
    }
    
    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionPatience);
            }
        };

}
