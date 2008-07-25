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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
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
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.agent.service.alarm.Alarm;
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

public class LinksStatusSensor extends ComponentPlugin
    implements Constants
{
    private ServiceBroker sb;
    private LoggingService log;
    private MetricsService metricsSvc;
    private MetricsUpdateService updateSvc;
    private BlackboardService bb;
    private CommunityService commSvc;
    private Properties parameters= new Properties();
    private HashMap agents = new HashMap();
    private HashMap nodes = new HashMap();
    private Vector newAgentQueue = new Vector();
    private Vector statsQueue = new Vector();
    private MessageAddress thisAgent;
    private Community community = null;
    private ProcessStatsAlarm statsAlarm = null;

    private static final String MSGLOG_STATISTICS = "MsglogStatistics";
    private static final long DEFAULT_SAMPLE_PERIOD = 60000; // 1 minute
    private long period = -1;
    private static final long DEFAULT_DELIVERY_LATENCY = 60000; // 1 minute
    private long latency = -1;
    private static final int DEFAULT_EXCELLENT = 99;
    private int excellent = -1;
    private static final int DEFAULT_GOOD = 90;
    private int good = -1;
    private static final int DEFAULT_FAIR = 50;
    private int fair = -1;
    private static final String EXCELLENT = "Excellent";
    private static final String GOOD      = "Good";
    private static final String FAIR      = "Fair";
    private static final String POOR      = "Poor";
    private static final String NONE      = "None";
    private static final String NODATA    = "NoData";
    
    public void load() {
	super.load();
	
	sb = getServiceBroker();
	log = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	metricsSvc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	updateSvc = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	bb = getBlackboardService();
        commSvc = (CommunityService)
	    sb.getService(this, CommunityService.class, null);
	thisAgent = getAgentIdentifier();

	Iterator iter = getParameters().iterator(); 
	while (iter.hasNext()) {
	    String s = (String) iter.next();
	    if (s.startsWith("period=")) {
		s = s.substring("period=".length());
		period = Long.parseLong(s);
	    } else if (s.startsWith("latency=")) {
		s = s.substring("latency=".length());
		latency = Long.parseLong(s);
	    } else if (s.startsWith("excellent=")) {
		s = s.substring("excellent=".length());
		excellent = Integer.parseInt(s);
	    } else if (s.startsWith("good=")) {
		s = s.substring("good=".length());
		good = Integer.parseInt(s);
	    } else if (s.startsWith("fair=")) {
		s = s.substring("fair=".length());
		fair = Integer.parseInt(s);
	    }
	}
	if (period == -1)
	    period = DEFAULT_SAMPLE_PERIOD;
	if (log.isInfoEnabled())
	    log.info("Sampling period set to "+period+" milliseconds.");
	if (latency == -1)
	    latency = DEFAULT_DELIVERY_LATENCY;
	if (log.isInfoEnabled())
	    log.info("Delivery latency set to "+latency+" milliseconds.");
	if (excellent == -1)
	    excellent = DEFAULT_EXCELLENT;
	if (log.isInfoEnabled())
	    log.info("Minumum value for Excellent set to "+excellent+"/100.");
	if (good == -1)
	    good = DEFAULT_GOOD;
	if (log.isInfoEnabled())
	    log.info("Minumum value for Good set to "+good+"/100.");
	if (fair == -1)
	    fair = DEFAULT_FAIR;
	if (log.isInfoEnabled())
	    log.info("Minumum value for Fair set to "+fair+"/100.");
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

//TODO: don't create diagoses until we get a status report for that agent/protocol tuple
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
		AgentDiagnoses ad = (AgentDiagnoses)iter.next();
		Diagnosis rmiDiag, sfDiag, altDiag, allDiag;
		String agentName = ad.agentName;
		String initialValue = NODATA;
		try {
		    rmiDiag = new RMILinksStatusDiagnosis(agentName,initialValue,sb);
		    sfDiag  = new StoreAndForwardLinksStatusDiagnosis(agentName,initialValue,sb);
		    altDiag = new AlternateDirectLinksStatusDiagnosis(agentName,initialValue,sb);
		    allDiag = new AllLinksStatusDiagnosis(agentName,initialValue,sb);
		} catch  (TechSpecNotFoundException e) {
		    if (tsLookupCnt > 10) {
			if (log.isWarnEnabled())
			    log.warn("TechSpec not found for LinksStatusDiagnosis.  Will retry.", e);
			tsLookupCnt = 0;
		    }
		    bb.signalClientActivity();
		    break;
		} catch (IllegalValueException e) {
		    if (log.isErrorEnabled())
			log.error("Attempt to create Diagnosis with illegal initial value="
				  +initialValue+". No LinksStatusDiagnoses created for agent="+agentName);
		    iter.remove();
		    break;
		}
		ad.rmiDiag = rmiDiag;
		ad.sfDiag  = sfDiag;
		ad.altDiag = altDiag;
		ad.allDiag = allDiag;
		bb.publishAdd(rmiDiag);
		bb.publishAdd(sfDiag);
		bb.publishAdd(altDiag);
		bb.publishAdd(allDiag);
		iter.remove();
		
		if (statsAlarm == null) {
		    statsAlarm = new ProcessStatsAlarm(period);
		    alarmService.addRealTimeAlarm(statsAlarm);
		}
	    }
	}

	//process stats
	if (statsAlarm != null && statsAlarm.hasExpired()) {
	    Vector q;
	    long notTooOld = System.currentTimeMillis()-period-latency;
	    synchronized (statsQueue) {
		q = statsQueue;
		statsQueue = new Vector();
	    }
	    if (log.isDebugEnabled()) 
		log.debug("execute: old statsQueue="+q);
	    if (log.isDebugEnabled()) 
		log.debug("execute: new statsQueue="+statsQueue);
            // create a vector of MessageHistory.AgentEntries extracted 
            // from all the stats vectors in the statsQueue
	    Vector aeV = new Vector(); 
	    Iterator iter = q.iterator();
	    while (iter.hasNext()) {
		Vector stats = (Vector)iter.next(); 
		if (stats != null) {
		    Hashtable aeht = (Hashtable)stats.elementAt(1);
		    if (aeht != null) {
			Enumeration enumeration = aeht.elements();
			while (enumeration.hasMoreElements()) {
			    MessageHistory.AgentEntry ae = 
				(MessageHistory.AgentEntry)enumeration.nextElement();
			    if (ae.timestamp >= notTooOld) {
				aeV.add(ae);
			    }
			}
		    }
		}
	    }
	    Object[] aeA = aeV.toArray();
	    Arrays.sort(aeA,comparator);
	    String agent = null;
	    int rmiSends=0; 
	    int sfSends=0; 
	    int altSends=0; 
	    int allSends=0;
	    int rmiSuccesses=0;
            int sfSuccesses=0;
            int altSuccesses=0;
            int allSuccesses=0;
	    AgentDiagnoses diags = null;
	    for (int i=0 ; i<aeA.length ; i++) {
		MessageHistory.AgentEntry ae = (MessageHistory.AgentEntry)aeA[i];
		if (ae.agent != agent) { // next agent
		    //update last one
		    if (diags != null) {
			updateDiag(diags.rmiDiag, rmiSends, rmiSuccesses);
			updateDiag(diags.sfDiag, sfSends, sfSuccesses);
			updateDiag(diags.altDiag, altSends, altSuccesses);
			updateDiag(diags.allDiag, allSends, allSuccesses);
		    }
		    agent = ae.agent;
		    diags = (AgentDiagnoses)agents.get(agent);
		    rmiSends=0; sfSends=0; altSends=0; allSends=0;
		    rmiSuccesses=0; sfSuccesses=0; altSuccesses=0; allSuccesses=0;
		}
		Enumeration enumeration = ae.protocolTbl.elements();
		while (enumeration.hasMoreElements()) {
		    MessageHistory.ProtocolEntry pe = 
			(MessageHistory.ProtocolEntry)enumeration.nextElement();
		    char prot = pe.linkLetter;
		    allSends = allSends + pe.successes;
		    allSuccesses = allSuccesses + pe.successes;
		    switch (prot)
			{
			case 'R': // RMI 
			case 'S': // SSLRMI
			    rmiSends = rmiSends + pe.successes;
			    rmiSuccesses = rmiSuccesses + pe.successes;
			    break;
			case 'E': // Email 
                        case 'N': // NNTP
			    sfSends = sfSends + pe.successes;
			    sfSuccesses = sfSuccesses + pe.successes;
			    break;
			case 'T': // TCP
                        case 'U': // UDP
			    altSends = altSends + pe.successes;
			    altSuccesses = altSuccesses + pe.successes;
			    break;
			default:
			    if (log.isWarnEnabled()) 
				log.warn("execute: stats for unexpected protocol seen, linkLetters="+prot);
			}
		}
	    }
	    //update last one
	    if (diags != null) {
		updateDiag(diags.rmiDiag, rmiSends, rmiSuccesses);
		updateDiag(diags.sfDiag, sfSends, sfSuccesses);
		updateDiag(diags.altDiag, altSends, altSuccesses);
		updateDiag(diags.allDiag, allSends, allSuccesses);
	    }           
	    statsAlarm = new ProcessStatsAlarm(period);
	    alarmService.addRealTimeAlarm(statsAlarm);
	}
    }
    
    private void updateDiag (Diagnosis diag, int sends, int successes) {
	String newValue = calcDiag(sends, successes);
	if (!newValue.equals(diag.getValue())) {
	    try {
		diag.setValue(newValue);
		bb.publishChange(diag);
	    } catch (IllegalValueException e) {
		if(log.isWarnEnabled())
		    log.warn("updateDiag: attempt to set an illegal value failed. Diagnosis="
			     +diag+", bad value="+newValue);
	    }
	}
    }

    private String calcDiag (int sends, int successes) {
	if (sends == 0) return NODATA;
	if (successes == 0) return NONE;
	int rate = (successes*100)/sends;
	if (rate >= excellent) return EXCELLENT;
	if (rate >= good) return GOOD;
	if (rate >= fair) return FAIR;
	return POOR;
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
                && roles.contains("Member")) {
		if (types.contains("Node")) {
		    String name = entity.getName();
		    NodeEntry entry = (NodeEntry)nodes.get(name);
		    if (entry == null) addNode(name);
		} else if (types.contains("Agent")) {
		    String name = entity.getName();
		    AgentDiagnoses entry = (AgentDiagnoses)agents.get(name);
		    if (entry == null) addAgent(name);
		}
	    }
	}
    }
    
    private void addAgent(String name) {
	if (log.isDebugEnabled()) 
	    log.debug("addAgent("+name+")");
	AgentDiagnoses ad  = new AgentDiagnoses (name);
	agents.put(name,ad);
	synchronized (newAgentQueue) {
	    newAgentQueue.add(ad);
	}
    }

    private void addNode(String name) {
	if (log.isDebugEnabled()) 
	    log.debug("addNode("+name+")");
	NodeEntry entry  = new NodeEntry (name);
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
	    String path = 
		"Integrater(Node"+KEY_SEPR+entry.nodeName+KEY_SEPR+formula+")"+PATH_SEPR+"Formula";
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
			log.info("NodeObserver.update: stats="+stats);
		    synchronized (statsQueue) {
			statsQueue.add(stats);
		    }
		    if (log.isDebugEnabled()) 
			log.debug("NodeObserver.update: statsQueue="+statsQueue);
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

    private class AgentDiagnoses {
        String agentName;
	Diagnosis rmiDiag, sfDiag, altDiag, allDiag;
      
        AgentDiagnoses (String agentName) {
            this.agentName = agentName;
        }

	public String toString () {
	    return agentName;
	}
    }

    private class NodeEntry {
        String nodeName;
        Vector statistics;
	NodeObserver statisticsObserver;
      
        NodeEntry (String nodeName) {
            this.nodeName = nodeName;
        }
	void unsubscribe () {
	    if (statisticsObserver != null)
		statisticsObserver.unsubscribe();
	}
	public String toString () {
	    return nodeName;
	}
    }

    private class ProcessStatsAlarm implements Alarm {
	private long detonate = -1;
	private boolean expired = false;

	/**
	 * Create an Alarm to go off in the milliseconds specified,
	 * to process collected MsglogStatistics and update Diagnoses.
	 **/
	public ProcessStatsAlarm (long delay) {
	    detonate = delay + System.currentTimeMillis();
	}
	
	/** @return absolute time (in milliseconds) that the Alarm should
	 * go off.  
	 * This value must be implemented as a fixed value.
	 **/
	public long getExpirationTime () {
	    return detonate;
	}
	
	/** 
	 * Called by the cluster clock when clock-time >= getExpirationTime().
	 **/
	public synchronized void expire () {
	    if (!expired) {
		expired = true;
		// process stats in execute method
		bb.signalClientActivity();
	    }
	}
	
	/** @return true IFF the alarm has expired or was canceled. **/
	public boolean hasExpired () {
	    return expired;
	}
	
	/** can be called by a client to cancel the alarm.  May or may not remove
	 * the alarm from the queue, but should prevent expire from doing anything.
	 * @return false IF the the alarm has already expired or was already canceled.
	 **/
	public synchronized boolean cancel () {
	    if (!expired)
		return expired = true;
	    return false;
	}
    }

    private final MyComparator comparator = new MyComparator();
    private class MyComparator implements Comparator {
	public int compare (Object o1, Object o2) {
/*
	    if (log.isDebugEnabled()) {
		log.debug("compare: o1="+o1);
		log.debug("compare: o2="+o2);
	    }
*/
	    MessageHistory.AgentEntry ae1 = (MessageHistory.AgentEntry)o1;
	    MessageHistory.AgentEntry ae2 = (MessageHistory.AgentEntry)o2;
	    if (ae1 == null && ae2 == null) return 0;
	    if (ae1 == null) return 1; //sort nulls to end
	    if (ae2 == null) return -1; //sort nulls to end
	    return ae1.agent.compareTo(ae2.agent);
	}
	public boolean equals (Object obj) {
	    return false;
	}
    }
}

