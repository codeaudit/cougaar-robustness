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


package org.cougaar.coordinator.examples.SampleDefense;

import java.util.Iterator;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.agent.service.alarm.Alarm;
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
    private boolean start = true;

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

    private class DelayedStartAlarm implements Alarm {
	private long detonate = -1;
	private boolean expired = false;
	
	public DelayedStartAlarm (long delay) {
	    detonate = delay + System.currentTimeMillis();
	}
	
	public long getExpirationTime () {
	    return detonate;
	}
	
	public synchronized void expire () {
	    if (!expired) {
		expired = true;
		start = true;
		blackboard.signalClientActivity();
	    }
	}
	
	public boolean hasExpired () {
	    return expired;
	}
	
	public synchronized boolean cancel () {
	    if (!expired)
		return expired = true;
	    return false;
	}
    }

    public void setupSubscriptions() 
    {
        rawSensorDataSub = 
	    (IncrementalSubscription)blackboard.subscribe(rawSensorDataPred);
//	alarmService.addRealTimeAlarm(new DelayedStartAlarm(120000));
    } 

    int tsLookupCnt = 0;
    public synchronized void execute() {
	
	if (start == true) {
	    try {
		diagnosis = new SampleDiagnosis(agentId.toString(), sb);
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
		start = false;
	    } catch (TechSpecNotFoundException e) {
		if (tsLookupCnt > 10) {
		    log.warn("TechSpec not found for SampleDiagnosis.  Will retry.", e);
		    tsLookupCnt = 0;
		}
		blackboard.signalClientActivity();
	    }
	}

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


