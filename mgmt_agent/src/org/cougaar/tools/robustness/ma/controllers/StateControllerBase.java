package org.cougaar.tools.robustness.ma.controllers;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;

/**
 * Base implementation for StateController inferface.
 */
public abstract class StateControllerBase
    extends BlackboardClientComponent
    implements StateController {

  protected CommunityStatusModel model;
  protected EventService eventService;

  public void initialize(BindingSite bs, CommunityStatusModel csm) {
    super.initialize();
    this.model = csm;
    this.setBindingSite(bs);
  }

  /**
   * Load required services.
   */
  public void load() {
    setAgentIdentificationService(
      (AgentIdentificationService)getServiceBroker().getService(this, AgentIdentificationService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    eventService = (EventService) getServiceBroker().getService(this, EventService.class, null);
    super.load();
  }

  // BlackboardClientComponent methods.
  public void setupSubscriptions() {}
  public void execute() {}

  // StateController methods.
  public void enter(String name) {}
  public void exit(String name) {}
  public void expired(String name) {}

  /**
   * Sends Cougaar event via EventService.
   */
  protected void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

}