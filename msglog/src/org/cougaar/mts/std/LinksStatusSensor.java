/*
 * <copyright>
 *  Copyright 2003,2004 Object Services and Consulting, Inc.
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
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.cougaar.core.adaptivity.OMCPoint;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsService;
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
import org.cougaar.tools.robustness.deconfliction.DefenseConstants;
import org.cougaar.tools.robustness.deconfliction.DefenseEnablingOperatingMode;
import org.cougaar.tools.robustness.deconfliction.MonitoringEnablingOperatingMode;
import org.cougaar.util.UnaryPredicate;

public class LinksStatusSensor extends ComponentPlugin
    implements Constants
{
    private LoggingService log;
    private MetricsService metricsSvc;
    private MetricsUpdateService updateSvc;
    private BlackboardService bb;
    private CommunityService commSvc;
    private UIDService uidSvc;
    private OperatingModeService opModeSvc;
    private ConditionService condSvc;
    private Properties parameters= new Properties();
    private HashMap nodes = new HashMap();
    private MessageAddress thisAgent;
    private Community community = null;

    private static final String MSGLOG_STATISTICS = "MsglogStatistics";

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	log = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	metricsSvc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	updateSvc = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	bb = getBlackboardService();
        uidSvc = (UIDService)
	    sb.getService(this, UIDService.class, null);
        commSvc = (CommunityService)
	    sb.getService(this, CommunityService.class, null);
	thisAgent = getAgentIdentifier();
    }

    public synchronized void unload() {
	Iterator itr = nodes.values().iterator();
	while (itr.hasNext()) {
	    ((NodeEntry)itr.next()).unsubscribe();
	}
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
    } 

    public synchronized void execute() {
	if (community == null) return;
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
            if (log.isDebugEnabled()) {
		log.debug("addMembers: attrs = " + attrs);
	    }
            Attribute types = attrs.get("EntityType");
            Attribute roles = attrs.get("Role");
            if ((types != null) 
                && (roles != null)
		&& (types.contains("Node"))
                && roles.contains("Member")) {
		String name = entity.getName();
                NodeEntry entry = (NodeEntry)nodes.get(name);
                if (entry == null) {
		    addMember(name);
		}
	    }
	}
    }

    private void addMember(String name) 
    {
	if (log.isDebugEnabled()) 
	    log.debug("addMember("+name+")");
	
	NodeEntry entry  = new NodeEntry (name, null);
	nodes.put(name,entry);

	// setup MetricsService subscription for each new node
	entry.statisticsObserver = 
	    new NodeObserver(entry, MSGLOG_STATISTICS);
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
    
    private class NodeObserver implements Observer {
        NodeEntry entry;
        String formula;
	Object key;

	NodeObserver(NodeEntry entry, String formula) {
	    if (log.isDebugEnabled()) 
		log.debug("NodeObserver("+entry+","+formula+")");
            this.entry = entry;
            this.formula = formula;
	    String path = "Integrater(Node"+KEY_SEPR+entry.nodeName+KEY_SEPR+formula+")"+PATH_SEPR+"Formula";
	    key = metricsSvc.subscribeToValue(path, this);
	}

	public void update(Observable obs, Object obj) {
	    //if (log.isDebugEnabled()) 
	    //    log.debug("NodeObserver.update("+obs+","+obj+")");
	    Metric metric = (Metric)obj; 
	    if (metric != null) {
		Object rawValue = metric.getRawValue();
		//if (log.isDebugEnabled()) 
		//  log.debug("NodeObserver.update: rawValue="+rawValue);
		if (rawValue instanceof Vector) {
		    boolean changed = false;
		    Vector stats = (Vector)rawValue;
		    if (log.isInfoEnabled()) 
			log.info("NodeObserver.update: stats="
				  +stats);
		}
	    }
	}

	void unsubscribe() {
	    metricsSvc.unsubscribeToValue(key);
	    key = null;
	}

	public String toString () {
	    return "NodeObserver("+entry+","+formula+")";
	}

    }

    private class NodeEntry {
        String nodeName;
        Vector statistics;
	NodeObserver statisticsObserver;
//	MsglogDiagnosis diag; 
//	MsglogAction act;
      
        NodeEntry (String nodeName,
		   Vector statistics) {
            this.nodeName = nodeName;
            this.statistics = statistics;
        }
	void unsubscribe () {
	    if (statisticsObserver != null)
		statisticsObserver.unsubscribe();
	}
	public String toString () {
	    return nodeName;
	}
    }

}


