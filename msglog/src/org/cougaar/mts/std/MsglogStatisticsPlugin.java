/*
 * <copyright>
 *  
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.mts.std;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Vector;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

public class MsglogStatisticsPlugin 
    extends ComponentPlugin
    implements Runnable, Constants
{
    private static final long DEFAULT_SAMPLE_PERIOD = 60000; // 1 minute
    private long period = -1;

    private MetricsUpdateService updateSvc;
    private LoggingService log;
    private MessageAddress nodeID;
    private Schedulable schedulable;

    public MsglogStatisticsPlugin() {
	super();
    }

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	log = (LoggingService)
	    sb.getService(this, LoggingService.class, null);

	updateSvc = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);

	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	nodeID = nis.getMessageAddress();

	for (Iterator iter = getParameters().iterator(); iter.hasNext();) {
	    String s = (String) iter.next();
	    if (!s.startsWith("period=")) {
		continue;
	    }
	    s = s.substring("period=".length());
	    long period = Long.parseLong(s);
	    break;
	}
	if (period == -1)
	    period = DEFAULT_SAMPLE_PERIOD;
	if (log.isInfoEnabled())
	    log.info("Sampling period set to "+period+" milliseconds.");

	if (updateSvc != null) {
	    ThreadService threadSvc = (ThreadService)
		sb.getService(this, ThreadService.class, null);
	    schedulable = threadSvc.getThread(this, this, "AgentStatus");
	    schedulable.schedule(period, period);
	    sb.releaseService(this, ThreadService.class, threadSvc);
	}
    }

    public void run() {
	Hashtable snapshot = MessageHistory.snapshotSendHistory(period);
	if (snapshot != null) {
	    String key = "Node"+KEY_SEPR+nodeID+KEY_SEPR+"MsglogStatistics";
	    Vector value = new Vector(2);
	    value.add(0,nodeID);
	    value.add(1,snapshot);
	    MetricImpl metric = 
		new MetricImpl(value,
			       SECOND_MEAS_CREDIBILITY,
			       "",
			       "MsglogStatisticsPlugin");
	    if (log.isInfoEnabled()) 
		log.info("run: Updating MetricsService:key="+key+",value="+metric);
	    updateSvc.updateValue(key, metric);
	}
    }
    
    protected void setupSubscriptions() {
    }
    
    protected void execute() {
    }

}
