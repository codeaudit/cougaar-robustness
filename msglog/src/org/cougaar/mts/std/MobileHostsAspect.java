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
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Observable;
import java.util.Observer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.acking.MessageAckingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import java.security.Security;

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
    LinkedHashSet clients = new LinkedHashSet();

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

    public Object getDelegate (Object delegate, Class type) {
	if (type == SendLink.class)
	    return new SendLinkDelegate((SendLink)delegate);
	return null;
    }

    private class SendLinkDelegate extends SendLinkDelegateImplBase 
    {
	public SendLinkDelegate (SendLink link)
	{
	    super (link);
	}
	public void registerClient (MessageTransportClient client) 
	{ 
	    synchronized (clients) {
		if (log.isInfoEnabled())
		    log.info("SendLinkDelegate.registerClient("+client+")");
		clients.add(client);
	    }
	    super.registerClient(client);
	}
	public void unregisterClient (MessageTransportClient client) 
	{ 
	    synchronized (clients) {
		if (log.isInfoEnabled())
		    log.info("SendLinkDelegate.unregisterClient("+client+")");
		clients.remove(client);
	    }
	    super.unregisterClient(client);
	}
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
		if (log.isInfoEnabled())
		    log.info("run: "+myHost+"'s address changed from "+myAddr+" to "+addr+".  Re-registering clients.");
		myAddr = addr;
		synchronized (clients) {
		    Iterator iter = clients.iterator();
		    while (iter.hasNext()) {
			MessageTransportClient client = (MessageTransportClient)iter.next();
			if (log.isInfoEnabled())
			    log.info("run: Unregistering client "+client);
			registry.unregisterClient(client);
			if (log.isInfoEnabled())
			    log.info("run: Registering client "+client);		
			registry.registerClient(client);
		    }
		}					
	    }
	} catch (java.net.UnknownHostException e) {
	    log.error("run: getLocalHost returned an unknown host.", e);
	}
    }

}
