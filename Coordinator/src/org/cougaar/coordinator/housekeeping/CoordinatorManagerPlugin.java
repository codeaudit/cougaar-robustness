/*
 * CoordinatorManagerPugin.java
 *
 * Created on April 13, 2004, 11:51 AM
 *
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


package org.cougaar.coordinator.housekeeping;

/**
 *
 * @author  David Wells
 * @version 
 */

import org.cougaar.coordinator.techspec.*;
import org.cougaar.coordinator.believability.StateEstimation;

import org.cougaar.coordinator.DeconflictionPluginBase;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;

import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosisUtils;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.ActionUtils;
import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.activation.ActionPatience;

import org.cougaar.coordinator.ActionIndex;
import org.cougaar.coordinator.DiagnosisIndex;
import org.cougaar.coordinator.CostBenefitEvaluationIndex;
import org.cougaar.coordinator.StateEstimationIndex;
import org.cougaar.coordinator.ActionPatienceIndex;

import org.cougaar.coordinator.believability.StateEstimation;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

public class CoordinatorManagerPlugin extends DeconflictionPluginBase implements NotPersistable {

    private IncrementalSubscription diagnosesWrapperSubscription;    
    private IncrementalSubscription actionsWrapperSubscription;
    private IncrementalSubscription costBenefitEvaluationSubscription;   
    private IncrementalSubscription actionPatienceSubscription;   
    private IndexKey key;
    private boolean indicesCreated = false;
        
    /** Creates new CoordinatorManagerPugin */
    public CoordinatorManagerPlugin() {
    }
    
    public void load() {
        super.load();
        key = new IndexKey();
    }

    protected void execute() {

        if (!indicesCreated) {
            publishAdd(new DiagnosisIndex());
            publishAdd(new ActionIndex());
            publishAdd(new CostBenefitEvaluationIndex());
            publishAdd(new ActionPatienceIndex());
            if (logger.isDebugEnabled()) logger.debug("Created ALL BB Indices");
            indicesCreated = true;
            }
      
        //Handle the addition of new DiagnosesWrapper(s)
        for ( Iterator iter = diagnosesWrapperSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DiagnosesWrapper dw = (DiagnosesWrapper)iter.next();
            Diagnosis d = (Diagnosis) dw.getContent();
            indexDiagnosis(dw, key);
        }   
        
        //Handle the addition of new ActionsWrapper(s)
        for ( Iterator iter = actionsWrapperSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ActionsWrapper aw = (ActionsWrapper)iter.next();
            Action a = (Action) aw.getContent();
            indexAction(aw, key);
            if (logger.isDebugEnabled()) logger.debug("Indexed: "+a+" AssetID="+a.getAssetID());
        }
}

    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

        super.setupSubscriptions();

        diagnosesWrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof DiagnosesWrapper) {
                return true ;
            }
            return false ;
        }
        }) ;


        actionsWrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
        public boolean execute(Object o) {
            if ( o instanceof ActionsWrapper) {
                return true ;
            }
            return false ;
        }
        }) ;


        costBenefitEvaluationSubscription = 
        ( IncrementalSubscription ) getBlackboardService().subscribe( CostBenefitEvaluation.pred );


        actionPatienceSubscription = 
        ( IncrementalSubscription ) getBlackboardService().subscribe( ActionPatience.pred );

}
    

}
