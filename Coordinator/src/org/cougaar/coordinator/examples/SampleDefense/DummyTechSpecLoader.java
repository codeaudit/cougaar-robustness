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

package org.cougaar.coordinator.examples.SampleDefense;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;

/**
 * Stand-in for real TechSpecLoader, which loads TechSpecs from XML files.
 */
public class DummyTechSpecLoader extends ComponentPlugin {
    
    private ServiceBroker sb;
    private LoggingService log = null;
    private DiagnosisTechSpecService diagnosisSvc;
    private ActionTechSpecService actionSvc;
    
    public void load() {
        super.load();

	sb = getServiceBroker();

        log = (LoggingService)sb.getService(this, LoggingService.class, null);
        if (log == null) log = LoggingService.NULL;

	actionSvc = (ActionTechSpecService)sb.getService(this, 
							 ActionTechSpecService.class, 
							 null);
        if (actionSvc == null) {
            log.error("Unable to get ActionTechSpecService");
        } else {
            actionSvc.addActionTechSpec("org.cougaar.coordinator.examples.SampleDefense.SampleAction", 
					new SampleActionTechSpec());
        }
        diagnosisSvc = (DiagnosisTechSpecService)sb.getService(this, 
							       DiagnosisTechSpecService.class, 
							       null);
        if (diagnosisSvc == null) {
            log.error("Unable to get DiagnosisTechSpecService.");
        } else {
            diagnosisSvc.addDiagnosisTechSpec("org.cougaar.coordinator.examples.SampleDefense.SampleDiagnosis", 
					      new SampleDiagnosisTechSpec());
        }
    }
    
    public void unload() {
        if ((log != null) && (log != LoggingService.NULL)) {
            sb.releaseService(this, LoggingService.class, log);
            log = LoggingService.NULL; }
        if (diagnosisSvc != null)
            sb.releaseService(this, DiagnosisTechSpecService.class, diagnosisSvc);
        if (actionSvc != null)
            sb.releaseService(this, ActionTechSpecService.class, actionSvc);
        super.unload();
    }

    public void setupSubscriptions() {}
    public void execute() {}
}
