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


package org.cougaar.coordinator;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.coordinator.techspec.ActionTechSpecService;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.housekeeping.IndexKey;

import org.cougaar.coordinator.costBenefit.CostBenefitEvaluation;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.activation.ActionPatience;

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
  protected DiagnosisTechSpecService diagnosisTechSpecService;

  private IncrementalSubscription actionIndexSubscription;
  private IncrementalSubscription diagnosisIndexSubscription;
  private IncrementalSubscription stateEstimationIndexSubscription;
  private IncrementalSubscription costBenefitEvaluationIndexSubscription;
  private IncrementalSubscription actionPatienceIndexSubscription;

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
        
      diagnosisTechSpecService = (DiagnosisTechSpecService) 
        sb.getService(this, DiagnosisTechSpecService.class, null);
      if (diagnosisTechSpecService == null) {
          throw new RuntimeException("TechSpec Service not available.");            
        }
        
        return true;
    }
    else if (logger.isDebugEnabled()) logger.warn(".haveServices - did NOT acquire services.");
    return false;
  }

  protected void setupSubscriptions() {
    actionIndexSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( ActionIndex.pred);
    diagnosisIndexSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( DiagnosisIndex.pred);;
    costBenefitEvaluationIndexSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( CostBenefitEvaluationIndex.pred);
    actionPatienceIndexSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( ActionPatienceIndex.pred);
    //if (logger.isDebugEnabled()) logger.debug("Subscribed to Indices");
  }


  public void load() {
      super.load();
      haveServices();
      //setupSubscriptions();
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

    public void signalClientActivity() {
        getBlackboardService().signalClientActivity();
    }


    // Access to Indices
          



   
    // Indexing for Diagnosis objects
       
    protected DiagnosesWrapper indexDiagnosis(DiagnosesWrapper dw, IndexKey key) {
        DiagnosisIndex index = getDiagnosisIndex();
        if (index == null) return null;
        //if (logger.isDebugEnabled()) logger.debug("Indexed: " + dw);
        return index.indexDiagnosis(dw, key);
    }
    
    protected DiagnosesWrapper findDiagnosis(AssetID assetID, String sensorType) {
        DiagnosisIndex index = getDiagnosisIndex();
        if (index == null) return null;
        else return index.findDiagnosis(assetID, sensorType);
    }
    
    protected Collection findDiagnosisCollection(AssetID assetID) {
        DiagnosisIndex index = getDiagnosisIndex();
        if (index == null) return null;
        else return index.findDiagnosisCollection(assetID);
    }

    private DiagnosisIndex getDiagnosisIndex() {
        Collection c = diagnosisIndexSubscription.getCollection(); // blackboard.query(DiagnosisIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
           //if (logger.isDebugEnabled()) logger.debug("Found Diagnosis Index");
           return (DiagnosisIndex)iter.next();
           }
        else {
           //if (logger.isDebugEnabled()) logger.debug("No Diagnosis Index");
           return null;
           }
    }    
    
    
    // Indexing for Action objects
        
    protected ActionsWrapper indexAction(ActionsWrapper aw, IndexKey key) {
        ActionIndex index = getActionIndex();
        if (index == null) return null;
        //if (logger.isDebugEnabled()) logger.debug("Indexed: " + aw);
        return index.indexAction(aw, key);
    }

    protected ActionsWrapper findAction(AssetID assetID, String actuatorType) {
        ActionIndex index = getActionIndex();
        if (index == null) return null;
        else return index.findAction(assetID, actuatorType);
    }

    protected Collection findActionCollection(AssetID assetID) {
        ActionIndex index = getActionIndex();
        if (index == null) return null;
        //if (logger.isDebugEnabled()) logger.debug("Found the ActionIndex while searching"+index.toString());
        Collection c = index.findActionCollection(assetID);
        //if (logger.isDebugEnabled()) logger.debug((c==null)?"null":new Integer(c.size()).toString());
        return c;
    }

    private ActionIndex getActionIndex() {
        Collection c = actionIndexSubscription.getCollection(); // blackboard.query(ActionIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
           //if (logger.isDebugEnabled()) logger.debug("Found Action Index");
           return (ActionIndex)iter.next();
           }
        else {
           //if (logger.isDebugEnabled()) logger.debug("No Action Index");
           return null;
           }
    } 

    
    // Indexing for CostBenefitEvaluations objects
           
    protected CostBenefitEvaluation indexCostBenefitEvaluation(CostBenefitEvaluation cbe, IndexKey key) {
        CostBenefitEvaluationIndex index = getCostBenefitEvaluationIndex();
        if (index == null) return null;
        else return index.indexCostBenefitEvaluation(cbe, key);
    }
              
    protected CostBenefitEvaluation findCostBenefitEvaluation(AssetID assetID) {
        CostBenefitEvaluationIndex index = getCostBenefitEvaluationIndex();
        if (index == null) return null;
        else {
            return index.findCostBenefitEvaluation(assetID);
        }
    }
    
    protected CostBenefitEvaluation removeCostBenefitEvaluation(CostBenefitEvaluation cbe, IndexKey key) {
        CostBenefitEvaluationIndex index = getCostBenefitEvaluationIndex();
        if (index == null) return null;
        else return index.removeCostBenefitEvaluation(cbe, key);
    }

    private CostBenefitEvaluationIndex getCostBenefitEvaluationIndex() {
        Collection c = costBenefitEvaluationIndexSubscription.getCollection(); // blackboard.query(CostBenefitEvaluationIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext()) {
           //if (logger.isDebugEnabled()) logger.debug("Found CBE Index");
           return (CostBenefitEvaluationIndex)iter.next();
           }
        else {
           if (logger.isDebugEnabled()) logger.debug("No CBE Index");
           return null;
           }
    }    



    // Indexing for ActionPatience objects
           
    protected ActionPatience indexActionPatience(ActionPatience ap) {
        ActionPatienceIndex index = getActionPatienceIndex();
        if (index == null) return null;
        else return index.indexActionPatience(ap);
    }
              
    protected ActionPatience findActionPatience(Action a) {
        ActionPatienceIndex index = getActionPatienceIndex();
        if (index == null) return null;
        else return index.findActionPatience(a);
    }
    
    protected ActionPatience removeActionPatience(ActionPatience ap) {
        ActionPatienceIndex index = getActionPatienceIndex();
        if (index == null) return null;
        else return index.removeActionPatience(ap);
    }
    
    private ActionPatienceIndex getActionPatienceIndex() {
        Collection c = actionPatienceIndexSubscription.getCollection(); // blackboard.query(ActionPatienceIndex.pred);
        Iterator iter = c.iterator();
        if (iter.hasNext())
           return (ActionPatienceIndex)iter.next();
        else
           return null;
    }    

}
