/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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

import java.util.Iterator;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

public class SampleSensor extends ComponentPlugin
{
    private LoggingService log;
    private SampleDiagnosis diagnosis;
    private ServiceBroker sb;

    private IncrementalSubscription rawSensorDataSub;

    private UnaryPredicate rawSensorDataPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof SampleRawSensorData);}};
    
    public void load() {
	super.load();
	sb = getServiceBroker();
	log = (LoggingService)sb.getService(this, LoggingService.class, null);
    }
    
    public synchronized void unload() {
	sb.releaseService(this, LoggingService.class, log);
	super.unload();
    }

    public void setupSubscriptions() 
    {
        rawSensorDataSub = 
	    (IncrementalSubscription)blackboard.subscribe(rawSensorDataPred);
	Object initialValue = "No Threat";
	try {
	    diagnosis = new SampleDiagnosis(agentId.toString(), initialValue, sb);
	    blackboard.publishAdd(diagnosis);
	    if (log.isDebugEnabled()) 
		log.debug(diagnosis + " added.");
	    if (log.isDebugEnabled()) log.debug(diagnosis.dump());
	    SampleRawSensorData data = new SampleRawSensorData(diagnosis.getAssetName(),
								 diagnosis.getPossibleValues(),
								 diagnosis.getValue());
	    blackboard.publishAdd(data);
	    if (log.isDebugEnabled()) 
		log.debug(data + " added.");
	} catch (IllegalValueException e) {
	    log.error("Illegal initialValue = "+initialValue, e);
	} catch (TechSpecNotFoundException e) {
	    log.error("TechSpec not found for SampleDiagnosis", e);
	}
    } 

    public synchronized void execute() {
	
        Iterator iter = rawSensorDataSub.getChangedCollection().iterator();
	while (iter.hasNext()) {
	    SampleRawSensorData data = (SampleRawSensorData)iter.next();
	    if (data != null) {
		try {
		    diagnosis.setValue(data.value);
		    blackboard.publishChange(diagnosis);
		    if (log.isDebugEnabled()) 
			log.debug(diagnosis + " changed.");
		    if (log.isDebugEnabled()) log.debug(diagnosis.dump());
		} catch (IllegalValueException e) {
		    log.error("Illegal value = "+data.value, e);
		}
	    }
	}

/* if data goes away, just leave diagnosis at its last setting
        iter = rawSensorDataSub.getRemovedCollection().iterator();
	while (iter.hasNext()) {
	    SampleRawSensorData data = (SampleRawSensorData)iter.next();
	    if (data != null) {
		try {

                    diagnosis.setValue(null);
		    blackboard.publishChange(diagnosis);
		    if (log.isDebugEnabled()) 
			log.debug(diagnosis + " changed.");
		    if (log.isDebugEnabled()) log.debug(diagnosis.dump());
		} catch (IllegalValueException e) {
		    log.error("Illegal value = "+data.value, e);
		}
	    }
	}
*/

    }
}


