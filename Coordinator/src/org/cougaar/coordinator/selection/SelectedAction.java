/*
 * SelectedAction.java
 *
 * Created on July 9, 2003, 9:26 AM
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

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.costBenefit.ActionEvaluation;

import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author  David Wells - OBJS
 */
public class SelectedAction implements NotPersistable {
    /** Base class for SelectedAction(s) with varius kinds of preconditions */
    
    private AssetID assetID;
    private ActionEvaluation actionEval;
    private Set actionVariants;
    private Precondition precondition;
    
    public SelectedAction(AssetID assetID, ActionEvaluation actionEval, Set actionVariants) {
        this.assetID = assetID;
        this.actionEval = actionEval;
        this.actionVariants = actionVariants;
        this.precondition = null;
    }

    public AssetID getAssetID() {
        return assetID;
    }

    public ActionEvaluation getActionEvaluation() {
        return actionEval;
    }
        
    public Set getActionVariants() {
        return actionVariants;
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
