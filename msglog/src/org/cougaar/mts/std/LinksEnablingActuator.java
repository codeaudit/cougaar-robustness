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

package org.cougaar.mts.std;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.NoStartedActionException;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.service.alarm.Alarm;

public class LinksEnablingActuator extends ComponentPlugin
    implements Constants
{
    private ServiceBroker sb;
    private LoggingService log;
    private BlackboardService bb;
    private MetricsUpdateService updateSvc;
    private CommunityService commSvc;
    private HashMap agents = new HashMap();
    private Vector newAgentQueue = new Vector();
    private Community community = null;
    private MessageAddress thisAgent;

    private static final String NONE = "Disable";
    private static final String RMI  = "Normal";
    private static final String ALT  = "AlternateDirect";
    private static final String SF   = "StoreAndForward";

    private static final LinkedHashSet myValuesOffered = new LinkedHashSet(3);
    static {
	myValuesOffered.add(RMI);
	myValuesOffered.add(ALT);
	myValuesOffered.add(SF);
    }

    private IncrementalSubscription actionSub;
    
    private UnaryPredicate actionPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof LinksEnablingAction);}};

    public void load() {
	super.load();
	
	sb = getServiceBroker();
	log = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	updateSvc = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	bb = getBlackboardService();
        commSvc = (CommunityService)
	    sb.getService(this, CommunityService.class, null);

	thisAgent = getAgentIdentifier();
    }
    
    public synchronized void unload() {
    }

    public void setupSubscriptions() 
    {
	// Add a listener for changes in robustness community
	commSvc.addListener(new CommunityChangeListener() {
		public void communityChanged(CommunityChangeEvent cce) {
		    if (log.isDebugEnabled()) 
			log.debug("CommunityChangeListener.communityChanged("+cce+")");
		    communitySearch(); }
		public String getCommunityName() { 
		    return (community != null) ? community.getName() : null; } });

	actionSub = 
	    (IncrementalSubscription)blackboard.subscribe(actionPred);
    } 
    
    int tsLookupCnt = 0;
    public synchronized void execute() {
	if (community == null) return;

        // handle new agents
	if (newAgentQueue.size() > 0) {
	    Vector q;
	    synchronized (newAgentQueue) {
		q = newAgentQueue;
		newAgentQueue = new Vector();
	    }
	    int tsLookupCnt = 0;
	    Iterator iter = q.iterator();
	    while (iter.hasNext()) {
//              AgentEntry ae = (AgentEntry)iter.next();
		String agentName = (String)iter.next();
		Action action;
//		String agentName = ae.agentName;
		try {
		    action = new LinksEnablingAction(agentName,myValuesOffered,sb);
		} catch  (TechSpecNotFoundException e) {
		    if (tsLookupCnt > 10) {
			if (log.isWarnEnabled())
			    log.warn("TechSpec not found for LinksEnablingAction.  Will retry.", e);
			tsLookupCnt = 0;
		    }
		    bb.signalClientActivity();
		    break;
		} catch (IllegalValueException e) {
		    if (log.isErrorEnabled())
			log.error("Attempt to create Action with illegal initial valuesOffered="
				  +myValuesOffered+". No LinksEnablingAction created for agent="+agentName);
		    iter.remove();
		    break;
		}
//		ae.action = action;
		bb.publishAdd(action);
		iter.remove();
	    }
	}
	
        Iterator iter = actionSub.getChangedCollection().iterator();
	while (iter.hasNext()) {
	    LinksEnablingAction action = (LinksEnablingAction)iter.next();
	    if (action != null) {
		Set pvs = action.getPermittedValues();
		if (pvs != null) {
		    action.clearNewPermittedValues();
		    String pv = NONE;
		    Iterator iter2 = pvs.iterator();
		    if (iter2.hasNext()) {  // there's 0 or 1 permitted values
			pv = (String)iter2.next();
		    }
		    String agent = action.getAssetName();
		    String key = "Agent"+KEY_SEPR+agent+KEY_SEPR+"MsglogLinksEnable";
		    MetricImpl metric = 
			new MetricImpl(pv,
				       SECOND_MEAS_CREDIBILITY,
				       "",
				       "LinksEnablingActuator");
		    if (log.isDebugEnabled()) 
			log.debug("execute: Updating MetricsService:key="+key+",value="+metric);
		    updateSvc.updateValue(key, metric);
		    boolean startOK = true;
		    if (!pv.equals(NONE)) {
			try {
			    action.start(pv);
			} catch (IllegalValueException e) {
			    if (log.isErrorEnabled())
				log.error("start called with illegal action value="+pv+"=. "
					  +"action="+action+" was not started.");
			    startOK = false;
			}
		    }
		    if (startOK) {
			Action.CompletionCode cc = pv.equals(NONE) ? Action.COMPLETED : Action.ACTIVE;
			try {
			    action.stop(cc);
			    bb.publishChange(action);
			} catch (IllegalValueException e) {
			    if (log.isErrorEnabled())
				log.error("stop called with illegal completion code="+cc+"=. "
					  +"action="+action+" was not stopped.");
			} catch (NoStartedActionException e) {
			    if (log.isErrorEnabled())
				log.error("stop called before action started. "
					  +"action="+action+" was not stopped.");
			}
			
		    }
		}
	    }
	}
    }
    
    private void communitySearch() {
	Collection communities = 
	    commSvc.searchCommunity(null,
				    "(&(CommunityType=Robustness)"
				    +"(RobustnessManager="+thisAgent+"))",
				    false,
				    Community.COMMUNITIES_ONLY,
				    new CommunityResponseListener() {
					public void getResponse(CommunityResponse response) {
					    if (log.isDebugEnabled()) 
						log.debug("getResponse("+response+")");
					    if (response.getStatus()==CommunityResponse.SUCCESS) {
						Collection communities = 
						    (Collection)response.getContent();
						if (communities != null) 
						    gotCommunity(communities);}}});
	if (communities != null) gotCommunity(communities);
    }
    
    private void gotCommunity(Collection comms) {
	if (log.isDebugEnabled()) 
	    log.debug("gotCommunity("+comms+")");
	if (comms == null) {
	    if (log.isDebugEnabled()) 
		log.debug("gotCommunity: received null Collection");
	    return;
	} else if (comms.size() == 0) {
	    if (log.isDebugEnabled()) 
		log.debug("gotCommunity: received empty Collection");
	    return;
	} else if (comms.size() > 1) {
	    if (log.isErrorEnabled())
		log.error("gotCommunity received more than one Robustness community"+
                          "with RobustnessManager = " + thisAgent +
                          ", using the first.");
	}
        Iterator it = comms.iterator();
	Community comm = (Community)it.next();
	if (community != null) {
	    checkForMembershipChange(community);
        } else {
	    community = comm;
	    Collection entities = comm.getEntities();
	    addMembers(entities);
	}
	
        bb.signalClientActivity();
    }
    
    private void addMembers(Collection entities) {
	if (log.isDebugEnabled()) 
	    log.debug("addMembers("+entities+")");
	Iterator i = entities.iterator();
	while (i.hasNext()) {
	    Entity entity = (Entity)i.next();
            Attributes attrs = entity.getAttributes();
            if (log.isDebugEnabled())
		log.debug("addMembers: attrs = " + attrs);
            Attribute types = attrs.get("EntityType");
            Attribute roles = attrs.get("Role");
            if ((types != null) 
                && (roles != null)
                && roles.contains("Member")
		&& (types.contains("Node") ||
		    types.contains("Agent"))) {
		String name = entity.getName();
//		AgentEntry entry = (AgentEntry)agents.get(name);
//		if (entry == null) addAgent(name);
		if (agents.get(name) == null) addAgent(name);
	    }
	}
    }
    
    private void addAgent(String name) {
	if (log.isDebugEnabled()) 
	    log.debug("addAgent("+name+")");
//	AgentEntry ae  = new AgentEntry(name);
//	agents.put(name,ae);
	agents.put(name,name);
	synchronized (newAgentQueue) {
//	    newAgentQueue.add(ae);
	    newAgentQueue.add(name);
	}
    }

    /**
     * Check for membership changes.
     */
    private void checkForMembershipChange(Community community) {
	Collection currentMembers = community.getEntities();
	Collection addedMembers = getAdded(currentMembers, priorMembers);
	if (!addedMembers.isEmpty()) {
	    if (log.isDebugEnabled()) 
		log.debug("checkForMembershipChange: Node(s) added to community: " 
			  + addedMembers);
	    addMembers(addedMembers);
	}
	Collection removedMembers = getRemoved(currentMembers, priorMembers);
	if (!removedMembers.isEmpty()) {
	    if (log.isDebugEnabled()) 
		log.debug("checkForMembershipChange: Node(s) removed from community: " 
			  + removedMembers);
            //removeMembers(removedMembers);
	}
	priorMembers = currentMembers;  // Save membership for future comparison
    }

    private Collection priorMembers = new ArrayList();
    private Collection getAdded(Collection current, Collection prior) {
	Collection copyOfCurrent = new ArrayList(current);
	copyOfCurrent.removeAll(prior);
	return copyOfCurrent;
    }
    
    private Collection getRemoved(Collection current, Collection prior) {
	Collection copyOfPrior = new ArrayList(current);
	copyOfPrior.removeAll(current);
	return copyOfPrior;
    }
    
/*
    private class AgentEntry {
        String agentName;
	Action action;
      
        AgentEntry (String agentName) {
            this.agentName = agentName;
        }

	public String toString () {
	    return agentName;
	}
    }
*/

}


