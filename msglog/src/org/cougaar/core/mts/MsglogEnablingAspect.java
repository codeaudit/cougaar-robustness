/*
 * <copyright>
 *  Copyright 2001-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 08 Aug 2003: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.io.*;
import java.util.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.acking.MessageAckingService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 **  An aspect which implements the MsglogEnablingService.
 **/
public class MsglogEnablingAspect extends StandardAspect
{
    private LoggingService log;
    private MetricsService metricsSvc;
    private MessageAckingService ackingSvc;
    private EventService eventSvc;
    
    private static final Hashtable agents = new Hashtable();
    
    private Impl enablingService;
    
    public MsglogEnablingAspect () 
    {}
    
    public void load () {
	super.load();
	
	log = loggingService;
	metricsSvc = (MetricsService)
	    getServiceBroker().getService(this, MetricsService.class, null);
	ackingSvc = (MessageAckingService)
	    getServiceBroker().getService(this, MessageAckingService.class, null);
        eventSvc = (EventService)
	    getServiceBroker().getService(this, EventService.class, null);

	Provider provider = new Provider();
	enablingService = new Impl();
	getServiceBroker().addService(MsglogEnablingService.class, provider);
	if (log.isDebugEnabled()) 
	    log.debug("load: MsglogEnablingService was added.");
    }
    
    public Object getDelegate (Object delegate, Class type) {
	if (type == DestinationLink.class)
	    return new Link((DestinationLink)delegate);
	return null;
    }
    
    public Object getReverseDelegate (Object delegate, Class type) {
	if (type == MessageDeliverer.class) 
	    return new Deliverer((MessageDeliverer)delegate);
	return null;
    }
    
    private class Link extends DestinationLinkDelegateImplBase 
    {
	private Link (DestinationLink link) {
	    super(link);
	}
	
	public MessageAttributes forwardMessage (AttributedMessage msg) 
	    throws UnregisteredNameException, NameLookupException, 
		   CommFailureException, MisdeliveredMessageException
	{
	    // subscribe to MsglogEnable for remote agents only
	    MessageAddress target = msg.getTarget();
	    if (!getRegistry().isLocalClient(target)) {
		synchronized (agents) {
		    AgentEntry entry = (AgentEntry)agents.get(target);
		    if (entry == null) {
			entry = new AgentEntry(target,true);
			agents.put(target, entry);
			entry.observer = new AgentObserver(target);
		    }
		}
	    }   
	    return super.forwardMessage(msg);
	}

	public int cost(AttributedMessage msg) 
	{
	    // Return Integer.MAX_VALUE if messaging is disabled to the target agent. 
	    // This causes the LinkSelectionPolicy to not select a link.
	    MessageAddress target = msg.getTarget();
	    if (!getRegistry().isLocalClient(target)) {
		synchronized (agents) {
		    AgentEntry entry = (AgentEntry)agents.get(target);
		    if (entry == null) {
			entry = new AgentEntry(target,true);
			agents.put(target, entry);
			entry.observer = new AgentObserver(target);
		    } else if (!entry.msglogEnabled) {
			if (log.isDebugEnabled()) 
			    log.debug("Messaging disabled to agent "+target
				      +", cost() returning Integer.MAX_VALUE for msg "
				      +MessageUtils.toString(msg));
			return Integer.MAX_VALUE;
		    } 
		}
	    }
	    return super.cost(msg);
	}
    }
    
    public class Deliverer extends MessageDelivererDelegateImplBase
    {
	public Deliverer (MessageDeliverer deliverer) 
	{
	    super (deliverer);
	}
	
	public MessageAttributes deliverMessage (AttributedMessage msg, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{ 
	    // subscribe to MsglogEnable for remote agents only
	    MessageAddress orig = msg.getOriginator();
	    if (!getRegistry().isLocalClient(orig)) {
		synchronized (agents) {
		    AgentEntry entry = (AgentEntry)agents.get(orig);
		    if (entry == null) {
			entry = new AgentEntry(orig,true);
			agents.put(orig, entry);
			entry.observer = new AgentObserver(orig); 
		    } else {
			if (!entry.msglogEnabled) {
			    // enabling messaging to any agent that can send me a message
			    entry.msglogEnabled = true;
			    // this causes message resending to originator to be re-enabled
			    if (ackingSvc != null) ackingSvc.release(orig);
			    if (eventSvc.isEventEnabled())                         
				eventSvc.event("Messaging Enabled from Node " 
					       + getRegistry().getIdentifier()
					       + " to Agent " 
					       + orig);
			}
		    }
		}
	    }   
	    return super.deliverMessage(msg, dest);
	}
    }
    
    private class Provider implements ServiceProvider
    {
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) {
	    if (serviceClass == MsglogEnablingService.class)
		return enablingService;
	    else
		return null;
	}
	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service) {
	}
    } 
    
    private class Impl implements MsglogEnablingService
    {
	Impl() {}

	/*
	 *  Enable messaging to remote agent.
	 */
	public void enable(MessageAddress remoteAgent) {
	    synchronized (agents) {
		AgentEntry entry = (AgentEntry)agents.get(remoteAgent);
		if (entry == null) {
		    if (log.isErrorEnabled()) 
			log.error("Impl.enable: AgentEntry not found for agent="+remoteAgent);
		} else {
		    if (!entry.msglogEnabled) {
			if (log.isDebugEnabled()) 
			    log.debug("Impl.enable: Messaging to "+remoteAgent+" enabled.");
			entry.msglogEnabled = true;
			// this causes message resending to remoteAgent to be re-enabled
			if (ackingSvc != null) ackingSvc.release(remoteAgent);
			if (eventSvc.isEventEnabled())                         
			    eventSvc.event("Messaging Enabled from Node " 
					   + getRegistry().getIdentifier()
					   + " to Agent " 
					   + remoteAgent);
		    }
		}
	    }
	}
	    
	/*
	 *  Is messaging to remote agent?
	 */
	public boolean isEnabled(MessageAddress remoteAgent) {
	    synchronized (agents) {
		AgentEntry entry = (AgentEntry)agents.get(remoteAgent);
		if (entry == null) {
		    if (log.isErrorEnabled()) 
			log.error("Impl.isEnabled: No AgentEntry for agent="
				  +remoteAgent+", returning true.");
		    return true;
		} else {
		    return entry.msglogEnabled;
		}
	    }
	}
	
	/*
	 *  Disable messaging to remote agent.
	 */
	public void disable(MessageAddress remoteAgent) {
	    synchronized (agents) {
		AgentEntry entry = (AgentEntry)agents.get(remoteAgent);
		if (entry == null) {
		    if (log.isErrorEnabled()) 
			log.error("Impl.disable: AgentEntry not found for agent="+remoteAgent);
		} else {
		    if (entry.msglogEnabled) {
			if (log.isDebugEnabled()) 
			    log.debug("Impl.disable: Messaging to "+remoteAgent+" disabled.");
			entry.msglogEnabled = false;
			// this causes message resending to remoteAgent to be disabled
			if (ackingSvc != null) ackingSvc.hold(remoteAgent);
			if (eventSvc.isEventEnabled())                         
			    eventSvc.event("Messaging Disabled from Node " 
					   + getRegistry().getIdentifier()
					   + " to Agent " 
					   + remoteAgent);
		    }
		}
	    }
	}
	
	/*
	 *  Is messaging disabled to remote agent?
	 */
	public boolean isDisabled(MessageAddress remoteAgent) {
	    return !isEnabled(remoteAgent);
	}
    }
    
    private class AgentObserver implements Observer, Constants {
        MessageAddress agent;
	Object key;
	String path;
	
	AgentObserver(MessageAddress agent) {
	    if (log.isDebugEnabled()) 
		log.debug("AgentObserver("+agent+")");
            this.agent = agent;
	    path = "Integrater(Agent"+KEY_SEPR+agent+KEY_SEPR+"MsglogEnable"+")"+PATH_SEPR+"Formula";
	    key = metricsSvc.subscribeToValue(path, this);
	}

	public void update(Observable obs, Object obj) {
	    Metric metric = (Metric)obj; 
	    if (metric != null) {
		//if (log.isDebugEnabled()) {
		//    log.debug("AgentObserver.update: metric.getRawValue() = "+metric.getRawValue());
		//    log.debug("AgentObserver.update: metric.getRawValue().getClass() = "+metric.getRawValue().getClass());
		//}
		if (metric.getRawValue() instanceof Boolean) {
		    boolean msglogEnabled = metric.booleanValue();
		    if (log.isDebugEnabled()) 
			log.debug("AgentObserver.update: agent="+agent
				  +",msglogEnabled="+msglogEnabled);
		    if (msglogEnabled) {
			enablingService.enable(agent);
		    } else {
			enablingService.disable(agent);
		    }
		}
            }
	}

	void unsubscribe() {
	    metricsSvc.unsubscribeToValue(key);
	    key = null;
	}

	public String toString () {
	    return "AgentObserver("+agent+","+path+")";
	}

    }

    private class AgentEntry {
        MessageAddress agentAddress;
        String agentName;
        boolean msglogEnabled;
	AgentObserver observer;
      
        AgentEntry (MessageAddress agentAddress,
		    boolean msglogEnabled) {
            this.agentAddress = agentAddress;
            agentName = agentAddress.getAddress();
            this.msglogEnabled = msglogEnabled;
        }
	void unsubscribe () {
	    if (observer != null)
		observer.unsubscribe();
	}
	public String toString () {
	    return agentName;
	}
    }

}
