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
 */

package org.cougaar.coordinator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

import org.cougaar.coordinator.*;

import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/* 
 * Helper class to find the Robustness Manager agent. 
 */
public class RobustnessManagerFinderPlugin extends ComponentPlugin
{
    private LoggingService log;
    private BlackboardService bb;
    private CommunityService commSvc;
    private UIDService uidSvc;
    private Community community = null;
    private RobustnessManagerID mgr = null;
    private CommunityChangeListener listener = null;

    private IncrementalSubscription sub;

    private UnaryPredicate pred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof RobustnessManagerID);}};
    
    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	log = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	bb = getBlackboardService();
        uidSvc = (UIDService)
	    sb.getService(this, UIDService.class, null);
        commSvc = (CommunityService)
	    sb.getService(this, CommunityService.class, null);
    }

    public void setupSubscriptions() 
    {
	Collection parents = 
	    commSvc.listParentCommunities(agentId.toString(),  
					  "(CommunityType=Robustness)");
	if (parents != null && !parents.isEmpty())
	    gotCommunity(parents);
	if (community == null) {
	    listener = new CommunityChangeListener() {
		    public void communityChanged(CommunityChangeEvent cce) {
			if (log.isDebugEnabled()) 
			    log.debug(agentId+": CommunityChangeListener.communityChanged("+cce+")");
			if ((cce.getType() == cce.ADD_COMMUNITY)
			    || ((cce.getType() == cce.ADD_ENTITY)
				&& (cce.getWhatChanged().equals(agentId.toString())))) {
			    Collection parents = 
				commSvc.listParentCommunities(agentId.toString(),  
							      "(CommunityType=Robustness)");
			    if (parents != null) {
				if (log.isDebugEnabled())
				    log.debug(agentId+": ParentCommunities = " + parents);
				gotCommunity(parents);
			    }
			}
		    }
		    public String getCommunityName() { 
			return "ALL_COMMUNITIES"; 
		    }
		};
	    commSvc.addListener(listener);
	}
	
	sub = (IncrementalSubscription)bb.subscribe(pred);
    } 

    public synchronized void execute() {

	Iterator iter;

	if (log.isDebugEnabled()) {
	    iter = sub.getAddedCollection().iterator();
	    while (iter.hasNext()) {
		RobustnessManagerID mgr = (RobustnessManagerID)iter.next();
		if (mgr != null)
		    log.debug(agentId+": "+mgr+" added.");
	    }
	    iter = sub.getChangedCollection().iterator();
	    while (iter.hasNext()) {
		RobustnessManagerID mgr = (RobustnessManagerID)iter.next();
		if (mgr != null)
		    log.debug(agentId+": "+mgr+" changed.");
	    }
	}
	
	if (community != null) {
	    Attributes attrs = community.getAttributes();
            if (log.isDebugEnabled())
		log.debug(agentId+
			  ": execute: community attributes = " 
			  +attrs);
            Attribute mgrs = attrs.get("RobustnessManager");
            if (mgrs == null) {
		if (log.isErrorEnabled()) {
		    log.error(agentId+
			      ": execute: no RobustnessManager attribute found in community = "
			      +community);
		    community = null;
		}
	    } else {
		try {
		    NamingEnumeration enum = mgrs.getAll();
		    while (enum.hasMore()) {
			Object obj = enum.next();
			if (obj == null) {
			    if (log.isErrorEnabled()) 
				log.error(agentId+
					  ": execute: null value in Attribute  " 
					  +mgrs);
			} else if (!(obj instanceof String)) {
			    if (log.isErrorEnabled()) 
				log.error(agentId+
					  ": execute: non-String value="+obj+" found in Attribute "
					  +mgrs);	
			} else if (mgr == null) {
			    String mgrAgentName = (String)obj;
			    MessageAddress mgrAgentAddr = MessageAddress.getMessageAddress(mgrAgentName);
			    mgr = new RobustnessManagerID(mgrAgentAddr,uidSvc.nextUID());
			    if (log.isDebugEnabled()) 
				log.debug(agentId+
					  ": execute: RobustnessManager "+mgrAgentName+" found.");
			    bb.publishAdd(mgr);
			    if (listener != null) {
				commSvc.removeListener(listener);
				listener = null;
			    }
			} else {
			    if (log.isDebugEnabled()) 
				log.debug(agentId+
					  ": execute: RobustnessManager already found.  Additional value="
					  +obj+" found and discarded.");
			}
		    }
		} catch (NamingException e) {
			    if (log.isErrorEnabled()) 
				log.error(agentId+
					  ": execute: NamingException thrown while enumerating RobustnessManager Attribute = "
					  + mgrs, e);
		}
	    }				
	}
    }

    private void gotCommunity(Collection comms) {
	if (comms == null) {
	    if (log.isDebugEnabled()) 
		log.debug(agentId+": gotCommunity: received null Collection");
	    return;
	} else if (comms.size() == 0) {
	    if (log.isDebugEnabled()) 
		log.debug(agentId+": gotCommunity: received empty Collection");
	    return;
	} else if (comms.size() > 1) {
	    if (log.isWarnEnabled())
		log.warn(agentId+": gotCommunity received more than one Robustness community"+
			 "with RobustnessManager = " + agentId +
			 ", using the first.");
	}
        Iterator it = comms.iterator();
	String commName = (String)it.next();
	if (community != null) {
	    if (log.isDebugEnabled()) 
		log.debug(agentId+": gotCommunity: found an additional RobustnessCommunity for me. Ignored it. "+commName);
	    return;
        } else {
	    Community comm = commSvc.getCommunity(commName,null);
            if (comm == null) {
		if (log.isWarnEnabled()) 
		    log.warn(agentId+": gotCommunity: getCommunity("+commName+",null) returned null.");
		return;
	    } else {
		if (log.isInfoEnabled()) 
		    log.info(agentId+": gotCommunity: found my RobustnessCommunity = "+commName);
		community = comm;
	    } 
	}
        bb.signalClientActivity();
    }

}


