/*
 * DisconnectPluginBase.java
 *
 *
 * @author  David Wells - OBJS
 * @version 
 *
 * Created on August 27, 2003, 9:48 PM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

public abstract class DisconnectPluginBase extends ServiceUserPluginBase {

  private UIDService us = null;
  private NodeIdentificationService nodeIdentificationService;
  private AgentIdentificationService agentIdentificationService;
  protected EventService eventService;

  private static final Class[] requiredServices = {
    UIDService.class,
    AgentIdentificationService.class,
    NodeIdentificationService.class,
    EventService.class
  };
  
    /** Creates new DisconnectPluginBase */
  public DisconnectPluginBase() {
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
  
  private void initObjects() {
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
  
}
