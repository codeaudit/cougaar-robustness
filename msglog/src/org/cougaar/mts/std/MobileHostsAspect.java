/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc. (OBJS),
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
 */

package org.cougaar.mts.std;

import java.net.InetAddress;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 **  An aspect for detecting and handling host moves.
 **/
public class MobileHostsAspect extends StandardAspect
    implements Runnable
{
    private ServiceBroker sb;
    private LoggingService log;
    private EventService eventSvc;
    MessageTransportRegistryService registry;
    private Schedulable schedulable;
    String myAddr = null;
    String myHost = null;
    private static final long DEFAULT_PERIOD = 10000; // 10 secs
    private long period = -1;

    public MobileHostsAspect () 
    {}
    
    public void load () {
	super.load();
	
	sb = getServiceBroker();
	log = loggingService;
        eventSvc = (EventService)
	    sb.getService(this, EventService.class, null);
	registry = getRegistry();
	
	period = getParameter("period", DEFAULT_PERIOD);
	if (log.isInfoEnabled())
	    log.info("InetAddress will be checked for change every "+period+" milliseconds.");
	
	ThreadService threadSvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	schedulable = threadSvc.getThread(this, this, "MobileHostsAspect");
	schedulable.schedule(0, period);  // run now and every period milliseconds thereafter
	sb.releaseService(this, ThreadService.class, threadSvc);
    }

    public void run() {
	try {
	    InetAddress inetAddr = InetAddress.getLocalHost();
	    if (log.isDetailEnabled())
		log.detail("run: InetAddress="+inetAddr);
	    String addr = inetAddr.getHostAddress();
	    String host = inetAddr.getHostName();
	    if (log.isDetailEnabled()) {
		log.detail("run: addr="+addr);
		log.detail("run: host="+host);
	    }
	    if (myAddr == null && myHost == null) {
		myAddr = addr;
		myHost = host;
		if (log.isInfoEnabled()) {
		    log.info("run: myAddr initialized to "+myAddr);
		    log.info("run: myHost initialized to "+myHost);
		}
	    } else if (!addr.equals(myAddr) && host.equals(myHost)) {
		if (eventSvc.isEventEnabled())
		    eventSvc.event("IP Address of "+myHost+" changed from "+myAddr+" to "+addr);
		myAddr = addr;
		registry.ipAddressChanged();
	    }
	} catch (java.net.UnknownHostException e) {
	    if (log.isWarnEnabled())
		log.warn("run: getLocalHost returned an unknown host.", e);
	    if (log.isInfoEnabled())
		log.info("", e);
	}
    }
    
}
