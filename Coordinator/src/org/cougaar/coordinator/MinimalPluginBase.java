/*
 * MinimalPluginBase.java
 *
 * Created on May 11, 2004, 12:06 PM
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


package org.cougaar.coordinator;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;


public abstract class MinimalPluginBase extends ServiceUserPluginBase {

    /* 
    Provides access to basic Services required by various Coordinator plugIns.
    Also provides predicates and index access for the various BB object types 
    used by Coordinator plugIns.
    */

  protected UIDService us = null;
  protected NodeIdentificationService nodeIdentificationService;
  protected AgentIdentificationService agentIdentificationService;
  protected EventService eventService;

  private static final Class[] requiredServices = {
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class,
    EventService.class
  };
  
    /** Creates new DisconnectPluginBase */
  public MinimalPluginBase() {
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

   
}
