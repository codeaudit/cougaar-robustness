/*
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

import org.cougaar.coordinator.examples.SampleDefense.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.cougaar.coordinator.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * Stand-in for real Coordinator. Hard-coded logic.
 */
public class DummyCoordinator extends ComponentPlugin {
    
    private ServiceBroker sb;
    private LoggingService log = null;

    private IncrementalSubscription diagnosisSub;
    private IncrementalSubscription actionSub;

    private UnaryPredicate diagnosisPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof SampleDiagnosis);}};

    private UnaryPredicate actionPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof SampleAction);}};
    
    public void load() {
        super.load();

	sb = getServiceBroker();

        log = (LoggingService)sb.getService(this, LoggingService.class, null);
        if (log == null) log = LoggingService.NULL;
    }
    
    public void unload() {
        if ((log != null) && (log != LoggingService.NULL)) {
            sb.releaseService(this, LoggingService.class, log);
            log = LoggingService.NULL; }
        super.unload();
    }

    public void setupSubscriptions() 
    {
        diagnosisSub = (IncrementalSubscription)blackboard.subscribe(diagnosisPred);
        actionSub = (IncrementalSubscription)blackboard.subscribe(actionPred);
    } 

    public synchronized void execute() {
	
        Iterator diags = diagnosisSub.getChangedCollection().iterator();
	while (diags.hasNext()) {
	    Diagnosis diag = (Diagnosis)diags.next();
	    if (diag != null) {
		Iterator actions = actionSub.getCollection().iterator();
		while (actions.hasNext()) {
		    Action action = (Action)actions.next();
		    if (action.getAssetName().equals(diag.getAssetName())) {
			String val = (String)diag.getValue();
			if (val != null) {
			    Set offered = action.getValuesOffered();
			    Set permitted = new HashSet();
			    if (val.equals("No Threat")) {
				if (offered.contains("NoEncryption")) permitted.add("NoEncryption"); 
				if (offered.contains("64Encryption")) permitted.add("64Encryption"); 
				if (offered.contains("128Encryption")) permitted.add("128Encryption"); 
			    } else if (val.equals("Low Threat")) {
				if (offered.contains("64Encryption")) permitted.add("64Encryption");
				if (offered.contains("128Encryption")) permitted.add("128Encryption");
				if (offered.contains("256Encryption")) permitted.add("128Encryption");
			    } else if (val.equals("Medium Threat")) {
				if (offered.contains("128Encryption")) permitted.add("64Encryption");
				if (offered.contains("256Encryption")) permitted.add("128Encryption");
			    } else if (val.equals("High Threat")) {
				if (offered.contains("256Encryption")) permitted.add("64Encryption");
			    }
			    try {
				action.setPermittedValues(permitted);
				blackboard.publishChange(action);
				if (log.isDebugEnabled()) {
				    log.debug(action + " changed.");
				}
			    } catch (IllegalValueException e) {
				log.error("Illegal value in "+permitted, e);
			    }	
			}		
			break; // in this example, only one diagnosis and one action per asset
		    }
		}
	    }
	}
    }
}
