/*
 * CostBenefitDiagnosisIndex.java
 *
 * Created on April 23, 2004, 9:38 AM
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


package org.cougaar.coordinator;
/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.housekeeping.IndexKey;
import org.cougaar.core.persist.NotPersistable;

public class CostBenefitEvaluationIndex implements NotPersistable {

    private Hashtable entries = new Hashtable();

    /** Creates new Class */
    public CostBenefitEvaluationIndex() {
    }
    
    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof CostBenefitEvaluationIndex);
            }
        };

    protected CostBenefitEvaluation indexCostBenefitEvaluation(CostBenefitEvaluation cbe, IndexKey key) {
        return (CostBenefitEvaluation) entries.put(cbe.getAssetID(), cbe);
    }
    
    protected CostBenefitEvaluation findCostBenefitEvaluation(AssetID assetID) {
        return (CostBenefitEvaluation) entries.get(assetID);
    }  

    protected CostBenefitEvaluation removeCostBenefitEvaluation(CostBenefitEvaluation cbe, IndexKey key) {
        return (CostBenefitEvaluation) entries.remove(cbe.getAssetID());
    }

}
