/*
 * DisconnectPluginBase.java
 *
 *
 * @author  David Wells - OBJS
 * @version 
 *
 * Created on August 27, 2003, 9:48 PM
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

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.coordinator.techspec.ActionTechSpecService;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.housekeeping.IndexKey;

import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.believability.StateEstimation;
import org.cougaar.coordinator.techspec.AssetID;

import java.util.Hashtable;
import java.util.Collection;
import java.util.Iterator;

public abstract class DeconflictionPluginBase extends ServiceUserPluginBase {

    /* 
    Provides access to basic Services required by various Coordinator plugIns.
    Also provides predicates and index access for the various BB object types 
    used by Coordinator plugIns.
    */

  protected UIDService us = null;
  protected NodeIdentificationService nodeIdentificationService;
  protected AgentIdentificationService agentIdentificationService;
  protected EventService eventService;
  protected ActionTechSpecService actionTechSpecService;

  private static final Class[] requiredServices = {
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class,
    EventService.class
  };
  
    /** Creates new DisconnectPluginBase */
  public DeconflictionPluginBase() {
      super(requiredServices);
    }

  private boolean haveServices() {
    if (acquireServices()) {
      if (logger.isDebugEnabled()) logger.debug(".haveServices - acquiredServices.");
      ServiceBroker sb = getServiceBroker();
      
      us = (UIDService ) 
        sb.getService( this, UIDService.class, null ) ;
        
      // get the EventService
      this.eventService = (EventService)
          sb.getService(this, EventService.class, null);
      if (eventService == null) {
          throw new RuntimeException("Unable to obtain EventService");
      }

      agentIdentificationService = (AgentIdentificationService)
        sb.getService(this, AgentIdentificationService.class, null);
      if (agentIdentificationService == null) {
          throw new RuntimeException("Unable to obtain agent-id service");
      }
      
      nodeIdentificationService = (NodeIdentificationService)
        sb.getService(this, NodeIdentificationService.class, null);
      if (nodeIdentificationService == null) {
          throw new RuntimeException("Unable to obtain noe-id service");
      }

      actionTechSpecService = (ActionTechSpecService) 
        sb.getService(this, ActionTechSpecService.class, null);
      if (actionTechSpecService == null) {
          throw new RuntimeException("TechSpec Service not available.");            
        }
        
        
        return true;
    }
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }


  public void load() {
      super.load();
      haveServices();
      cancelTimer();
  }
  
     
  protected UIDService getUIDService() {
      return us;
  }
  
  protected MessageAddress getAgentAddress() {
      return agentIdentificationService.getMessageAddress();
  }
  
  protected MessageAddress getNodeAddress() {
      return nodeIdentificationService.getMessageAddress();
  }
  
  protected String getAgentID() {
      return agentIdentificationService.getName();
  }
  
  protected String getNodeID() {
      return getNodeAddress().toString();
  }

  
    // Helper methods to publish objects to the Blackboard
    public boolean publishAdd(Object o) {
        getBlackboardService().publishAdd(o);
        return true;
    }

    public boolean publishChange(Object o) {
	getBlackboardService().publishChange(o);
        return true;
    }

    public boolean publishRemove(Object o) {
	getBlackboardService().publishRemove(o);
        return true;
    }
    
    public void openTransaction() {
        getBlackboardService().openTransaction();
    }
    
    public void closeTransaction() {
        getBlackboardService().closeTransaction();
    }


    // Access to Indices
          



   
    // Indexing for Diagnosis objects
       
    protected DiagnosesWrapper indexDiagnosis(DiagnosesWrapper dw, IndexKey key) {
        return getDiagnosisIndex().indexDiagnosis(dw, key);
    }
    
    protected DiagnosesWrapper findDiagnosis(AssetID assetID, String sensorType) {
        return getDiagnosisIndex().findDiagnosis(assetID, sensorType);
    }
    
    protected Collection findDiagnosisCollection(AssetID assetID) {
        return getDiagnosisIndex().findDiagnosisCollection(assetID);
    }

    private DiagnosisIndex getDiagnosisIndex() {
        Collection c = blackboard.query(DiagnosisIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext())
           return (DiagnosisIndex)iter.next();
        else
           return null;
    }    
    
    
    // Indexing for Action objects
        
    protected ActionsWrapper indexAction(ActionsWrapper aw, IndexKey key) {
        return getActionIndex().indexAction(aw, key);
    }

    protected ActionsWrapper findAction(AssetID assetID, String actuatorType) {
        //System.out.println(getActionIndex());
        return getActionIndex().findAction(assetID, actuatorType);
    }

    protected Collection findActionCollection(AssetID assetID) {
        return getActionIndex().findActionCollection(assetID);
    }

    private ActionIndex getActionIndex() {
        Collection c = blackboard.query(ActionIndex.pred);
        //System.out.println(c.size());
        Iterator iter = c.iterator();
        if (iter.hasNext())
           return (ActionIndex)iter.next();
        else
           return null;
    } 

    
    // Indexing for CostBenefitEvaluations objects
           
    protected CostBenefitEvaluation indexCostBenefitEvaluation(CostBenefitEvaluation cbe, IndexKey key) {
        return getCostBenefitEvaluationIndex().indexCostBenefitEvaluation(cbe, key);
    }
              
    protected CostBenefitEvaluation findCostBenefitEvaluation(AssetID assetID) {
        return getCostBenefitEvaluationIndex().findCostBenefitEvaluation(assetID);
    }
    
    private CostBenefitEvaluationIndex getCostBenefitEvaluationIndex() {
        Collection c = blackboard.query(CostBenefitEvaluationIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext())
           return (CostBenefitEvaluationIndex)iter.next();
        else
           return null;
    }    

    // Indexing for Statestimation objects
           
    protected StateEstimation indexStateEstimation(StateEstimation se, IndexKey key) {
        return getStateEstimationIndex().indexStateEstimation(se, key);
    }
              
    protected StateEstimation findStateEstimation(AssetID assetID) {
        return getStateEstimationIndex().findStateEstimation(assetID);
    }
    
    private StateEstimationIndex getStateEstimationIndex() {
        Collection c = blackboard.query(StateEstimationIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext())
           return (StateEstimationIndex)iter.next();
        else
           return null;
    }    
}
