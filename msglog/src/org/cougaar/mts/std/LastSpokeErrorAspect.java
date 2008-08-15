/*
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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
 *
 * CHANGE RECORD 
 * 22 Jul 2003: Created. (OBJS)
 */

package org.cougaar.mts.std;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.Router;
import org.cougaar.mts.base.RouterDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/*
 * Place this aspect first (outer) in the aspect list.
 */
public class LastSpokeErrorAspect extends StandardAspect implements Constants {
    
    private LoggingService log;
    private MetricsUpdateService updateSvc;
    
    public LastSpokeErrorAspect() {}
    
    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	log = (LoggingService)sb.getService(this, LoggingService.class, null);
        updateSvc = (MetricsUpdateService)sb.getService(this, MetricsUpdateService.class, null);
    }
    
    public Object getDelegate(Object delegate, Class type)
    {
	if ((type == DestinationLink.class)) {
	    return new DestinationLinkDelegate((DestinationLink)delegate);
	}
	if ((type == Router.class)) {
	    return new RouterDelegate((Router)delegate);
	}
	return null;
    }
    
    private class DestinationLinkDelegate extends DestinationLinkDelegateImplBase 
    {
	DestinationLink link;
        String dest;
	String key;
	
	private DestinationLinkDelegate(DestinationLink link) {
	    super(link);
	    this.link = link;
            dest = link.getDestination().getAddress();
            key = "Agent"+KEY_SEPR+dest+KEY_SEPR+"SpokeErrorTime";
	}
	
	public MessageAttributes forwardMessage (AttributedMessage msg) 
	    throws UnregisteredNameException, NameLookupException, 
		   CommFailureException, MisdeliveredMessageException
	{
	    MessageAttributes attrs = null;
	    try {	    
		attrs = link.forwardMessage(msg);
	    } catch (CommFailureException cfe) {
                MetricImpl metric = new MetricImpl(new Long(System.currentTimeMillis()),
						   SECOND_MEAS_CREDIBILITY,
						   "",
						   "LastSpokeErrorAspect");
		if (log.isDebugEnabled()) 
		    log.debug("DestinationLinkDelegate: Updating MetricsService:key="+key+",value="+metric);
		updateSvc.updateValue(key,metric);
                throw cfe;
	    }
	    return attrs;
	}
    }
    
    private class RouterDelegate extends RouterDelegateImplBase
    {
	public RouterDelegate (Router router) {
	    super(router);
	}
	
	public void routeMessage(AttributedMessage msg) {
            // if its a resend, an ack was late, so record LastSpokeError
	    if (MessageUtils.getSendTry(msg) >= 1) {
		String dest = msg.getTarget().getAddress();
		String key = "Agent"+KEY_SEPR+dest+KEY_SEPR+"SpokeErrorTime";
                MetricImpl metric = new MetricImpl(new Long(System.currentTimeMillis()),
						   SECOND_MEAS_CREDIBILITY,
						   "",
						   "LastSpokeErrorAspect");
		if (log.isDebugEnabled()) 
		    log.debug("RouterDelegate: Updating MetricsService:key="+key+",value="+metric);
		updateSvc.updateValue(key,metric);		
	    }
	    super.routeMessage(msg);
	}
    }
    
}
