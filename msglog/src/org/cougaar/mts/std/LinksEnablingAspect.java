/*
 * <copyright>
 *  Copyright 2003-2004 Object Services and Consulting, Inc. (OBJS),
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

package org.cougaar.mts.std;

import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.acking.MessageAckingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 **  An aspect which implements the LinksEnablingService.
 **/
public class LinksEnablingAspect extends StandardAspect
    implements LinksEnablingConstants
{
    private LoggingService log;
    private MetricsService metricsSvc;
    private MessageAckingService ackingSvc;
    private EventService eventSvc;
    
    private LinksEnablingService enablingService;

    String defaultAdvice = RMI;
    
    public LinksEnablingAspect () 
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
	enablingService = new LinksEnablingServiceImpl();
	getServiceBroker().addService(LinksEnablingService.class, provider);
	if (log.isDebugEnabled()) 
	    log.debug("load: LinksEnablingService was added.");
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
	DestinationLink link;

	private Link (DestinationLink link) {
	    super(link);
	    this.link = link;
	}
	
	public MessageAttributes forwardMessage (AttributedMessage msg) 
	    throws UnregisteredNameException, NameLookupException, 
		   CommFailureException, MisdeliveredMessageException
	{
	    // subscribe to Coordinator advice for remote agents only
	    MessageAddress target = msg.getTarget().getPrimary();
	    if (!getRegistry().isLocalClient(target))
		enablingService.register(target);
	    return super.forwardMessage(msg);
	}
	
	//TODO: Change ALSP to use isValid rather than cost()
	public boolean isValid()
	{
	    // Return false if messaging is disabled to the target agent. 
	    // This causes the LinkSelectionPolicy to not select a link.
	    MessageAddress agent = link.getDestination();
	    if (!getRegistry().isLocalClient(agent)) {
		if (!enablingService.isEnabled(agent)){
		    if (log.isDebugEnabled()) 
			log.debug("Messaging disabled to agent "+agent
				  +". isValid() returning false.");
		    return false;
		} 
	    }
	    return super.isValid();
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
	    // subscribe to Coordinator advice for remote agents only
	    MessageAddress orig = msg.getOriginator().getPrimary();
	    if (!getRegistry().isLocalClient(orig)) {

// hack at the moment - not sure I want to let incoming messages re-enable messaging, but
// that's what I did before.  Ideally, messaging would only be re-enabled as a result
// of gossip arriving from the coordinator re-enabling it, but gossip is not so reliable.
// if there was no message travelling from the coordinator's node to this one, then no 
// gossip would flow, so maybe if I'm using gossip for control, it should only be advice,
// and if I want reliable control, I should use piggybacking or Action Relays.
// So, I leave open this back door for the present.

		// enabling messaging to any agent that can send me a message
		enablingService.enable(orig);
	    }   
	    return super.deliverMessage(msg, dest);
	}
    }
    
    private class Provider implements ServiceProvider
    {
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) {
	    if (serviceClass == LinksEnablingService.class)
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
    
    private class LinksEnablingServiceImpl implements LinksEnablingService
    {
	private final Hashtable agents;
	
	LinksEnablingServiceImpl() {
	    agents = new Hashtable();
        }

	/*
	 *  Register an agent if not already registered.
	 */
        public void register(MessageAddress agent) {
            assureRegistered(agent);
	}   
	
	/*
	 *  Register an agent if not already registered.
	 */
        private AgentEntry assureRegistered(MessageAddress agent) {
            AgentEntry entry = (AgentEntry)agents.get(agent);
	    if (entry == null) {
	        entry = new AgentEntry(agent, defaultAdvice);
                agents.put(agent, entry);
		entry.observer = new AgentObserver(agent);
	    }
	    return entry;
	}   
	
	/*
	 *  Set Link Selection Advice for messaging to remote agent.
	 */
	public void setAdvice(MessageAddress agent, String newAdvice) {
	    synchronized (agents) {
                AgentEntry entry = assureRegistered(agent);
                setAdvice(entry,newAdvice);
	    }
	}
	
	private void setAdvice(AgentEntry entry, String newAdvice) {
	    String currentAdvice = entry.advice;
	    if (!newAdvice.equals(currentAdvice)) { 
		MessageAddress agent = entry.agentAddress;
		if (log.isDebugEnabled()) 
		    log.debug("LinksEnablingServiceImpl.setAdvice: Link Selection Advice for Messaging to "
			      +agent+" changed from "+currentAdvice+" to "+newAdvice+".");
		entry.previous = currentAdvice;
		entry.advice = newAdvice;
		if (eventSvc.isEventEnabled())                         
		    eventSvc.event("Link Selection Advice for Messaging from Node " 
				   +getRegistry().getIdentifier()
				   +" to Agent "+agent
				   +" changed from "+currentAdvice 
				   +" to "+newAdvice);
		// this causes message resending to agent to be re-enabled
		if (NONE.equals(currentAdvice) 
		    && ackingSvc != null) {
		    ackingSvc.release(agent);
		    if (eventSvc.isEventEnabled())                         
			eventSvc.event("Messaging Enabled from Node " 
				       + getRegistry().getIdentifier()
				       + " to Agent " 
				       + agent);
		}
	    }
	}

	/*
	 *  Enable messaging to remote agent.  Reverts to previous advice.
	 */
	public void enable(MessageAddress agent) {
	    synchronized (agents) {
		AgentEntry entry = assureRegistered(agent);
		if (NONE.equals(entry.advice)) {
		    setAdvice(entry, entry.previous);
		}
	    }
	}
    	
        /*
	 *  Get Link Selection Advice for messaging to remote agent.
	 */
        public String getAdvice(MessageAddress agent) {
	    synchronized (agents) {
		AgentEntry entry = assureRegistered(agent);
		String advice = entry.advice;
		if (log.isDebugEnabled()) 
		    log.debug("LinksEnablingServiceImpl.getAdvice: Link Selection Advice for Messaging to "
			      +agent+" is "+advice);
		return advice;
	    }
	}
	
	/* 
	 *  Is messaging to remote agent enabled?
	 */
	public boolean isEnabled(MessageAddress agent) {
	    synchronized (agents) {
		AgentEntry entry = assureRegistered(agent);
		return !NONE.equals(entry.advice);
	    }
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
	    path = "Integrater(Agent"+KEY_SEPR+agent+KEY_SEPR+"MsglogLinksEnable"+")"+PATH_SEPR+"Formula";
	    key = metricsSvc.subscribeToValue(path, this);
	}
	public void update(Observable obs, Object obj) {
	    Metric metric = (Metric)obj; 
	    if (metric != null) {
		if (log.isDebugEnabled()) {
		    log.debug("AgentObserver.update: metric.getRawValue() = "+metric.getRawValue());
		    log.debug("AgentObserver.update: metric.getRawValue().getClass() = "+metric.getRawValue().getClass());
		}
		if (metric.getRawValue() instanceof String) {
		    String advice = (String)metric.getRawValue();
		    if (log.isDebugEnabled()) 
			log.debug("AgentObserver.update: agent="+agent
				  +",advice="+advice);
		    enablingService.setAdvice(agent, advice);
		}
            }
	}
	void unsubscribe() {
	    metricsSvc.unsubscribeToValue(key);
	    key = null;
	}
	public String toString() {
	    return "AgentObserver("+agent+","+path+")";
	}
    }

    private class AgentEntry {
        MessageAddress agentAddress;
        String agentName;
	String advice;
        String previous;
	AgentObserver observer;
	
        AgentEntry(MessageAddress agentAddress,
		   String advice) {
            this.agentAddress = agentAddress;
            agentName = agentAddress.getAddress();
            this.advice = advice;
	    previous = null;
        }
	void unsubscribe() {
	    if (observer != null)
		observer.unsubscribe();
	}
	public String toString() {
	    return agentName;
	}
    }
    
}
