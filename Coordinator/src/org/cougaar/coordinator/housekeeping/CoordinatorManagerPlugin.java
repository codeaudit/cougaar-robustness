/*
 * CoordinatorManagerPugin.java
 *
 * Created on April 13, 2004, 11:51 AM
 */
/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.housekeeping;

/**
 *
 * @author  David Wells
 * @version 
 */

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

import org.cougaar.coordinator.ActionIndex;
import org.cougaar.coordinator.DiagnosisIndex;
import org.cougaar.coordinator.CostBenefitEvaluationIndex;
import org.cougaar.coordinator.believability.StateEstimation;

import java.util.Iterator;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

public class CoordinatorManagerPlugin extends DeconflictionPluginBase implements NotPersistable {

    private IncrementalSubscription diagnosesWrapperSubscription;    
    private IncrementalSubscription actionsWrapperSubscription;
    private IncrementalSubscription costBenefitEvaluationSubscription;   
    private IncrementalSubscription stateEstimationSubscription;   
    private IndexKey key;
        
    /** Creates new CoordinatorManagerPugin */
    public CoordinatorManagerPlugin() {
    }
    
    public void load() {
        super.load();
        openTransaction();
        publishAdd(new DiagnosisIndex());
        publishAdd(new ActionIndex());
        publishAdd(new CostBenefitEvaluationIndex());
        key = new IndexKey();
        closeTransaction();
    }

    protected void execute() {
      
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
        }

        //Handle the addition of new StateEstimation(s)
        for ( Iterator iter = stateEstimationSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            StateEstimation se = (StateEstimation)iter.next();
            indexStateEstimation(se, key);
        }

        //Handle the addition of new CostBenefitEvaluation(s)
        for ( Iterator iter = costBenefitEvaluationSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            CostBenefitEvaluation cbe = (CostBenefitEvaluation)iter.next();
            indexCostBenefitEvaluation(cbe, key);
        }


}

    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

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

      stateEstimationSubscription = 
        ( IncrementalSubscription ) getBlackboardService().subscribe( StateEstimation.pred );
}
    

}
