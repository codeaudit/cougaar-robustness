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

package org.cougaar.core.mts;

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
import org.cougaar.core.qos.metrics.MetricNotificationQualifier;
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

import org.cougaar.tools.robustness.deconfliction.*;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

public class MsglogPlugin extends ComponentPlugin
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
    private HashMap agents = new HashMap();
    private MessageAddress thisAgent;
    private Community community = null;
    private static final String HEARD_TIME = "HeardTime";
    private static final String SPOKE_ERROR_TIME = "SpokeErrorTime";

    // Deconfliction subscriptions
    private IncrementalSubscription applicabilityCondSub;
    private IncrementalSubscription enablingOpModeSub;
    private IncrementalSubscription monitoringOpModeSub;

    // Deconfliction predicates
    private UnaryPredicate applicabilityCondPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof MsglogApplicabilityCondition);}};
    private UnaryPredicate enablingOpModePred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof MsglogEnablingOpMode);}};
    private UnaryPredicate monitoringOpModePred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof MsglogMonitoringOpMode);}};
    
    private String getParameter(String key) {
	return parameters.getProperty(key);
    }

    public void setParameter(Object param) {
	if (param instanceof List) {
	    Iterator itr = ((List) param).iterator();
	    while(itr.hasNext()) {
		String property = (String) itr.next();
		int sepr = property.indexOf('=');
		if (sepr < 0) continue;
		String key = property.substring(0, sepr);
		String value = property.substring(++sepr);
		parameters.setProperty(key, value);
	    }
	}
    }

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
	condSvc = (ConditionService)
	    sb.getService(this, ConditionService.class, null);
	opModeSvc = (OperatingModeService)
	    sb.getService(this, OperatingModeService.class, null);

	thisAgent = getAgentIdentifier();
    }

    public synchronized void unload() {
	Iterator itr = agents.values().iterator();
	while (itr.hasNext()) {
	    ((AgentEntry)itr.next()).unsubscribe();
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

	// Deconfliction subscriptions
	applicabilityCondSub = 
	    (IncrementalSubscription)bb.subscribe(applicabilityCondPred);
        enablingOpModeSub = 
	    (IncrementalSubscription)bb.subscribe(enablingOpModePred);
	monitoringOpModeSub = 
	    (IncrementalSubscription)bb.subscribe(monitoringOpModePred);
    } 

    public synchronized void execute() {
	if (community == null) return;
	
	Iterator iter = applicabilityCondSub.getAddedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogApplicabilityCondition ac = 
		(MsglogApplicabilityCondition)iter.next();
	    if (ac != null) {
		if (log.isDebugEnabled()) 
		    log.debug(ac+" added.");
	    }
	}
	iter = enablingOpModeSub.getAddedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogEnablingOpMode eom = 
		(MsglogEnablingOpMode)iter.next();
	    if (eom != null) {
		if (log.isDebugEnabled()) 
		    log.debug(eom+" added.");
	    }
	}
	iter = monitoringOpModeSub.getAddedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogMonitoringOpMode mom = 
		(MsglogMonitoringOpMode)iter.next();
	    if (mom != null) {
		if (log.isDebugEnabled()) 
		    log.debug(mom+" added.");
	    }
	}
	iter = applicabilityCondSub.getChangedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogApplicabilityCondition ac = 
		(MsglogApplicabilityCondition)iter.next();
	    if (ac != null) {
		if (log.isDebugEnabled()) 
		    log.debug(ac+" changed.");
	    }
	}
	iter = enablingOpModeSub.getChangedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogEnablingOpMode eom = 
		(MsglogEnablingOpMode)iter.next();
	    if (eom != null) {
		if (log.isDebugEnabled()) 
		    log.debug(eom+" changed.");
		Comparable opModeVal = eom.getValue();
		Boolean metricsVal = null;
//		if (opModeVal.compareTo(DefenseConstants.DEF_ENABLED) == 0) {
		if (DefenseConstants.DEF_ENABLED.contains(opModeVal)) {
		    metricsVal = Boolean.TRUE;
//		} else if (opModeVal.compareTo(DefenseConstants.DEF_DISABLED) == 0) {
		} else if (DefenseConstants.DEF_DISABLED.contains(opModeVal)) {
		    metricsVal = Boolean.FALSE;
		}
		if (metricsVal != null) {
		    String key = "Agent"+KEY_SEPR+eom.getAgent()+KEY_SEPR+"MsglogEnable";
		    MetricImpl metric = 
			new MetricImpl(metricsVal,
				       SECOND_MEAS_CREDIBILITY,
				       "",
				       "MsglogPlugin");
		    if (log.isDebugEnabled()) 
			log.debug("execute: Updating MetricsService:key="+key+",value="+metric);
		    updateSvc.updateValue(key,metric);
		} else {
		    if (log.isWarnEnabled()) 
			log.warn("execute: MsglogEnablingOpMode set to illegal value:agent="+eom.getAgent()+",value="+opModeVal);    
		}
	    }
	}
	iter = monitoringOpModeSub.getChangedCollection().iterator();
	while (iter.hasNext()) {
	    MsglogMonitoringOpMode mom = 
		(MsglogMonitoringOpMode)iter.next();
	    if (mom != null) {
		if (log.isDebugEnabled()) 
		    log.debug(mom+" changed.");
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
            if (log.isDebugEnabled()) {
		log.debug("addMembers: attrs = " + attrs);
	    }
            Attribute types = attrs.get("EntityType");
            Attribute roles = attrs.get("Role");
            if ((types != null) 
                && (roles != null)
		&& (types.contains("Agent") || types.contains("Node"))
                && roles.contains("Member")) {
		String name = entity.getName();
                AgentEntry entry = (AgentEntry)agents.get(name);
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
	
	AgentEntry entry  = new AgentEntry (name, true, 0, 0);
	agents.put(name,entry);

	// setup Deconfliction conditions and operating modes for new agent
        entry.applCond = 
	    new MsglogApplicabilityCondition(name); 
        entry.enabOpMode = 
	    new MsglogEnablingOpMode(name); 
	entry.enabOpMode.setUID(uidSvc.nextUID()); 
        entry.monOpMode = 
	    new MsglogMonitoringOpMode(name); 
	entry.monOpMode.setUID(uidSvc.nextUID()); 

	bb.openTransaction();
	bb.publishAdd(entry.applCond);
	bb.publishAdd(entry.enabOpMode);
	bb.publishAdd(entry.monOpMode);
	bb.closeTransaction();

	// setup MetricsService subscription for each new agent
	entry.lastHeardObserver = 
	    new AgentObserver(entry, HEARD_TIME, new LongQualifier(10000));
	entry.lastSpokeErrorObserver = 
	    new AgentObserver(entry, SPOKE_ERROR_TIME, new LongQualifier(10000));
    }

    /**
     * Check for membership changes.
     */
    private void checkForMembershipChange(Community community) {
	Collection currentMembers = community.getEntities();
	Collection addedMembers = getAdded(currentMembers, priorMembers);
	if (!addedMembers.isEmpty()) {
	    if (log.isDebugEnabled()) 
		log.debug("checkForMembershipChange: Agent(s) added to community: " 
			  + addedMembers);
	    addMembers(addedMembers);
	}
	Collection removedMembers = getRemoved(currentMembers, priorMembers);
	if (!removedMembers.isEmpty()) {
	    if (log.isDebugEnabled()) 
		log.debug("checkForMembershipChange: Agent(s) removed from community: " 
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
    
    private class AgentObserver implements Observer {
        AgentEntry entry;
        String formula;
	Object key;

	AgentObserver(AgentEntry entry, String formula, LongQualifier qualifier) {
	    if (log.isDebugEnabled()) 
		log.debug("AgentObserver("+entry+","+formula+","+qualifier+")");
            this.entry = entry;
            this.formula = formula;
	    String path = "Agent("+entry.agentName+")"+PATH_SEPR+formula;
	    key = metricsSvc.subscribeToValue(path, this, qualifier);
	}

	public void update(Observable obs, Object obj) {
	    //if (log.isDebugEnabled()) 
	    //    log.debug("AgentObserver.update("+obs+","+obj+")");
	    Metric metric = (Metric)obj; 
	    if (metric != null) {
		long newTime = metric.longValue();
		if (log.isDebugEnabled()) 
		  log.debug("AgentObserver.update: agent="+entry.agentName
			    +",formula="+formula
			    +",metric="+newTime);
		boolean changed = false;
		if (formula.equals(HEARD_TIME)) {
		    if (newTime > entry.lastHeardTime) {
			entry.lastHeardTime = newTime;
			changed = true;
			if (log.isDebugEnabled()) 
			    log.debug("AgentObserver.update: new HeardTime="
				      +entry.lastHeardTime);
		    }
		} else if (formula.equals(SPOKE_ERROR_TIME)) {
		    if (newTime > entry.lastSpokeErrorTime) {
			entry.lastSpokeErrorTime = newTime;
			changed = true;
			if (log.isDebugEnabled()) 
			    log.debug("AgentObserver.update: new SpokeErrorTime="
				      +entry.lastSpokeErrorTime);
		    }
		}
		if (changed) {
		    //  stand-in for deconfliction notification
		    if (entry.lastSpokeErrorTime > entry.lastHeardTime) {
			if (log.isInfoEnabled()) 
			    log.info("MsglogApplicabilityCondition for "
				     +entry+" set to TRUE");
			if (entry.applCond != null) {
			    entry.applCond.setValue(DefenseConstants.BOOL_TRUE);
			    bb.openTransaction();
			    bb.publishChange(entry.applCond);
			    bb.closeTransaction();
			}
		    } else {
			if (log.isInfoEnabled()) 
			    log.info("MsglogApplicabilityCondition for "
				     +entry+" set to FALSE");
			if (entry.applCond != null) {
			    entry.applCond.setValue(DefenseConstants.BOOL_FALSE);
			    bb.openTransaction();
			    bb.publishChange(entry.applCond);
			    bb.closeTransaction();
			}
		    }
		}
	    }
	}

	void unsubscribe() {
	    metricsSvc.unsubscribeToValue(key);
	    key = null;
	}

	public String toString () {
	    return "AgentObserver("+entry+","+formula+")";
	}

    }

    private class LongQualifier 
	implements MetricNotificationQualifier 
    {
	private long min_delta;
	private Metric last_qualified;
	
	public LongQualifier(long min_delta) {
	    this.min_delta = min_delta;
	}
	public boolean shouldNotify(Metric metric) {
	    if (last_qualified == null) {
		last_qualified = metric;
		return true; }
	    long old_value = last_qualified.longValue();
	    long new_value = metric.longValue();
	    if (Math.abs(new_value-old_value) > min_delta) {
		last_qualified = metric;
		return true;}
	    return false;
	}
	public String toString () {
	    return "LongQualifier("+min_delta+","+last_qualified+")";
	}
    }

    private class AgentEntry {
        String agentName;
        boolean msglogEnabled;
        long lastHeardTime;
        long lastSpokeErrorTime;
	AgentObserver lastHeardObserver;
	AgentObserver lastSpokeErrorObserver;
	MsglogApplicabilityCondition applCond; 
	MsglogEnablingOpMode enabOpMode;
	MsglogMonitoringOpMode monOpMode;
      
        AgentEntry (String agentName,
		    boolean msglogEnabled,
		    long lastHeardTime,
		    long lastSpokeErrorTime) {
            this.agentName = agentName;
            this.msglogEnabled = msglogEnabled;
            this.lastHeardTime = lastHeardTime;
            this.lastSpokeErrorTime = lastSpokeErrorTime;
        }
	void unsubscribe () {
	    if (lastHeardObserver != null)
		lastHeardObserver.unsubscribe();
	    if (lastSpokeErrorObserver != null)
		lastSpokeErrorObserver.unsubscribe();
	}
	public String toString () {
	    return agentName;
	}
    }

    // Deconfliction Conditions and OperatingModes

    public class MsglogApplicabilityCondition 
	extends MsglogDefenseApplicabilityBinaryCondition {
        private String agent;
	public MsglogApplicabilityCondition(String assetType,
					    String asset,
					    String defenseName) {
	    super(assetType, asset, defenseName, DefenseConstants.BOOL_FALSE);
	    agent = asset;
	} 
	public MsglogApplicabilityCondition(String assetType,
					    String asset,
					    String defenseName,
					    DefenseConstants.OMCStrBoolPoint initialValue) {
	    super(assetType, asset, defenseName, initialValue);
	    agent = asset;
	} 
	public MsglogApplicabilityCondition(String agent) {
	    this("Agent", agent, "Msglog", DefenseConstants.BOOL_FALSE);
//	    this("Agent", agent, "BackwardDefense", DefenseConstants.BOOL_FALSE);
	} 
	public String getAgent() { return agent;}
	public String toString() { return "MsglogApplicabilityCondition:"+super.toString();}
    }
    public class MsglogEnablingOpMode 
	extends DefenseEnablingOperatingMode {
        private String agent;
	public MsglogEnablingOpMode(String assetType,
                                    String asset,
                                    String defenseName) {
	    super(assetType, asset, defenseName);
	    agent = asset;
	} 
	public MsglogEnablingOpMode(String agent) {
	    this("Agent", agent, "Msglog");
//	    this("Agent", agent, "BackwardDefense");
	} 
	public String getAgent() { return agent;}
	public void setValue(Comparable value) {
	    super.setValue(value);}
	public String toString() { return "MsglogEnablingOpMode:"+super.toString();}
    }
    public class MsglogMonitoringOpMode 
	extends MonitoringEnablingOperatingMode {
        private String agent;
	public MsglogMonitoringOpMode(String assetType,
				      String asset,
				      String defenseName) {
	    super(assetType, asset, defenseName);
	    agent = asset;
	} 
	public MsglogMonitoringOpMode(String agent) {
	    this("Agent", agent, "Msglog");
//	    this("Agent", agent, "BackwardDefense");
	} 
	public String getAgent() { return agent;}
	public void setValue(Comparable value) {
	    super.setValue(value);}
	public String toString() { return "MsglogMonitoringOpMode:"+super.toString();}
    }

}


