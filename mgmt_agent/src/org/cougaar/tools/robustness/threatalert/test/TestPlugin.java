package org.cougaar.tools.robustness.threatalert.test;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.tools.robustness.threatalert.*;

/**
 * This plugin ...
 */
public class TestPlugin extends SimplePlugin implements ThreatAlertListener {

  private LoggingService log;
  private BlackboardService bbs = null;
  private MessageAddress myAgent = null;

  private ThreatAlertService taService;

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);
    taService = (ThreatAlertService) getBindingSite().getServiceBroker().getService(this, ThreatAlertService.class, null);
    taService.addListener(this);

    bbs = getBlackboardService();

    myAgent = getMessageAddress();

  }

  public void newAlert(ThreatAlert ta) {
    log.info("new alert: " + ta.toString());
  }

  public void changedAlert(ThreatAlert ta) {
    log.info("changed alert: " + ta.toString());
  }

  public void removedAlert(ThreatAlert ta) {
    log.info("removed alert: " + ta.toString());
  }

  public void execute() {
  }


}
