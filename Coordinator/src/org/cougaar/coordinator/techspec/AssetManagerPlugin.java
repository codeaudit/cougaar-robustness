/*
 * AssetManagerPlugin.java
 *
 * Created on September 12, 2003, 4:53 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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

package org.cougaar.coordinator.techspec;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;

import org.cougaar.core.persist.NotPersistable;

/**
 * A plugin to monitor Assets. Parts of this code were taken from Ron Snyder's
 * StatusChangeListenerPlugin.java. Sends notification of new Assets via a change 
 * listener & when assets move by directly notifying subscribers. Only the
 * ThreatModelManager should probably be a subscriber.
 *
 * *** Could consider publishing the assets to the BB instead... ***
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetManagerPlugin extends ComponentPlugin implements NotPersistable {
    
    
    private LoggingService logger;
    private UIDService us = null;
    
    private boolean haveServices = false;
    
    private Vector allAssets;
    
    //CommunityStatusModel
    private CommunityStatusModel csm = null;
    private List changeListeners = Collections.synchronizedList(new ArrayList());
    
    private int expectedAssetCount = 0;
    private int assetCount = 0;
    private boolean emittedAssetCountCondition = false;
    
    /** True if the enclave (community name) was found */
    private boolean enclaveNameFound = false;
    
    //Subscribe to new assets / changes announced by the CommunityStatusModel
    private StatusChangeListener myChangeListener = new StatusChangeListener() {
        
        /**
         * This is a subscriber to CommunityStatusModel change events, which occur
         * each time an agent is added to the community.
         * 
         * A change event ONLY reports on agents & node agents -- we create an asset 
         * for each. We also create a node asset for every node agent we see. We can 
         * get at the host name from these events, and create a host asset when one 
         * doesn't already exist.
         */
        public void statusChanged(CommunityStatusChangeEvent[] csce) {
            
            DefaultAssetTechSpec hostAsset;
            DefaultAssetTechSpec nodeAsset;
            DefaultAssetTechSpec agentAsset;
            String agentName = null;
            String nodeName = null;
            String hostName = null;
            
            boolean newAgent;
            boolean movingAgent;
            boolean removedAsset;
            
            //Look at the events for new agent assets
            for (int i = 0; i < csce.length; i++) {

                newAgent = false;
                movingAgent = false;
                removedAsset = false;

/*                
logger.warn("!!!! ****************************************************");
logger.warn("!!!! [STATUS CHANGED CALLED] asset = "+csce[i].getName());
if (csce[i].locationChanged())
    logger.warn("!!!! location changed");
if (csce[i].membersAdded())
    logger.warn("!!!! membersAdded");
if (csce[i].leaderChanged())
    logger.warn("!!!! leaderChanged");
if (csce[i].membersRemoved())
    logger.warn("!!!! membersRemoved");
if (csce[i].stateChanged())
    logger.warn("!!!! stateChanged");
if (csce[i].stateExpired())
    logger.warn("!!!! stateExpired");
logger.warn("!!!! getCurrentLocation = " + csce[i].getCurrentLocation());
logger.warn("!!!! getPriorLocation = " + csce[i].getPriorLocation());
logger.warn("!!!! **********************************************************");

*/ 
               // Is this a new agent? 
                if ( (csce[i].membersAdded() && csce[i].getCurrentLocation() != null) ||
                    (csce[i].locationChanged() && csce[i].getPriorLocation() == null) )  {
                        
                    newAgent = true;
                        
                } else if (csce[i].locationChanged() && csce[i].getPriorLocation() != null) {
                    
                    movingAgent = true;

                } else if (csce[i].membersRemoved()) {
                    
                    removedAsset = true;
                }

                
                agentName = csce[i].getName();
                //***********************************************************************
                //******** Ignore if not a new agent or moving agent -- for NOW *********
                //***********************************************************************
                //Later could monitor other changes in an agent / node.
                if ( newAgent || movingAgent ) {
                                        
                    if ( csce[i].getType() == CommunityStatusModel.AGENT ) {
                        nodeName = csce[i].getCurrentLocation();
                        hostName = csm.getLocation(nodeName);
                    }
                    else if ( csce[i].getType() == CommunityStatusModel.NODE ) { //node & node agent have the same name
                        nodeName = agentName;
                        hostName = csce[i].getCurrentLocation();
                    }
                    
                    if (hostName == null || nodeName == null) {
                        logger.warn("!!!! [ASSET NOT ADDED] Saw new agent asset ["+agentName+"] without host/node info: hostName = "+hostName + "  nodeName = "+nodeName);
                        continue;
                    } else { //continue
                        hostAsset = getHost(hostName);
                        nodeAsset = getNode(hostAsset, nodeName);
                    }
                    
                    //*** Handle a new agent
                    if ( newAgent )  {

                        logger.debug("============>>> Saw new agent asset ["+agentName+"] with host/node info: hostName = "+hostName + "  nodeName = "+nodeName);
                        
                        agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, AssetType.AGENT, us.nextUID());
                        //Use NODE name to assign FWD / REAR property -- doing this also for nodes in getNode()
                        if (nodeName.startsWith("REAR")) {
                            agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "REAR"));
                        } else if (nodeName.startsWith("FWD")) {
                            agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "FORWARD"));
                        }
                        
                        queueChangeEvent(new AssetChangeEvent( agentAsset, AssetChangeEvent.NEW_ASSET));
                        
                        //*** Handle an agent moving
                    } else if (movingAgent) { //it's a movingAgent
                        
                        logger.info("Agent location changed: agent=" + agentName +
                        " priorLocation=" + csce[i].getPriorLocation() +
                        " newLocation=" + csce[i].getCurrentLocation());
                        
                        agentAsset = DefaultAssetTechSpec.findAssetByID(new AssetID(agentName, AssetType.AGENT));
                        agentAsset.setNewLocation(hostAsset, nodeAsset);
                        queueChangeEvent(new AssetChangeEvent( agentAsset, AssetChangeEvent.MOVED_ASSET));

                        //Use NODE name to assign FWD / REAR property
                        if (nodeName.startsWith("REAR")) {
                            agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "REAR"));
                        } else if (nodeName.startsWith("FWD")) {
                            agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "FWD"));
                        }

                        //*** Handle an agent that has been removed
                    } 
                    
                } else if (removedAsset) { //it's a removedAsset
                        
                        //if it's a node agent, remove the node asset.

                        if (csce[i].getType() == CommunityStatusModel.AGENT) {
                            logger.info("Agent REMOVED: agent=" + agentName);                        
                            agentAsset = DefaultAssetTechSpec.findAssetByID(new AssetID(agentName, AssetType.AGENT));
                            queueChangeEvent(new AssetChangeEvent( agentAsset, AssetChangeEvent.REMOVED_ASSET));                                                
                        } else if (csce[i].getType() == CommunityStatusModel.NODE) {
                            logger.info("NODE REMOVED: node=" + agentName);                        
                            nodeAsset = DefaultAssetTechSpec.findAssetByID(new AssetID(agentName, AssetType.NODE));
                            queueChangeEvent(new AssetChangeEvent( nodeAsset, AssetChangeEvent.REMOVED_ASSET));                                                
                        }
                        
                        //REMOVE the node asset too???
                        //if (csce[i].getType() == CommunityStatusModel.NODE) { }
                        
                }                    
            }
        }
    };

    /**  read in the plugin params*/
    public void load() {
    
        getPluginParams();
    }
    
    
    /**
     * Temp - read in the # of assets to see before emitting AllAssetsSeenCondition
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        //if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
            String i = (String) iter.next();
            try {
                expectedAssetCount = Integer.parseInt(i);
            } catch (Exception e) {
                logger.warn("expectedAssetCount integer was not a valid integer. It was: "+i);
            }
        } 
    }       
    
    
    /**
     * Subscribes to the CommunityStatusModel, reads in the AssetStateDimensions from XML,
     * and then publishes itself.
     */
    public void setupSubscriptions() {
        
        
        getServices();
        
        allAssets = new Vector(100,100);
        
        // Subscribe to CommunityStatusModel
        communityStatusModelSub =
        (IncrementalSubscription)blackboard.subscribe(communityStatusModelPredicate);                
        
        
        blackboard.publishAdd(this); //publish myself, ONLY after reading in XML
        
    }
    
    private void getServices() {
        
        logger =
        (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        
        us = (UIDService ) getBindingSite().getServiceBroker().getService( this, UIDService.class, null ) ;
        
        haveServices = true;
    }
    
    public void execute() {
        
        if (!haveServices) {
            getServices();
            if (!haveServices) {
                //throw some nasty error...
                Logger logger = Logging.getLogger(this.getClass().getName());
                logger.error("Could not get needed services!!");
                return;
            }
        }
        
        sendEvents();
/*
        if ((wakeAlarm != null) &&
        ((wakeAlarm.hasExpired()))) {
            logger.debug("Perform periodic tasks");
            wakeAlarm = new WakeAlarm(now() + TIMER_INTERVAL);
            alarmService.addRealTimeAlarm(wakeAlarm);
        }
 */
        
        Collection models = communityStatusModelSub.getAddedCollection();
        for (Iterator it = models.iterator(); it.hasNext();) {
            CommunityStatusModel csm = (CommunityStatusModel)it.next();
            logger.info("************************ Found CommunityStatusModel: community=" + csm.getCommunityName());
            csm.addChangeListener(myChangeListener);
            
            //According to Ron there will be only ONE csm per BB.
            this.csm = csm;
            
            //get all agents already known by the csm - called once.
            getCurrentAssetsInCSM();
        }
        
        if (!emittedAssetCountCondition && assetCount >= expectedAssetCount) {
            blackboard.publishAdd(new AllAssetsSeenCondition());
            emittedAssetCountCondition = true;
            logger.debug("Published AllAssetsSeenCondition.");
        }
        
        
    }
    
    private void incAssetCount() {        
        assetCount++;
    }
    
    
    /**
     * Retrieves the current list of agents/node agents that the CommunityStatusModel
     * is aware of & creates assets. This is done once at the beginning just after
     * subscribing to CommunityStatusModel change events.
     */
    private void getCurrentAssetsInCSM() {
        
        if (!enclaveNameFound) { //try getting the name of the community
            addEnclave();
        }
        
        //Get all node agents
        String nodes[] = csm.listEntries(CommunityStatusModel.NODE);
        if (nodes != null && nodes.length > 0) {
            for (int i=0; i<nodes.length; i++) {
                addAsset(nodes[i], CommunityStatusModel.NODE);        
            }    
        }
        
        //Get all agents
        String agents[] = csm.listEntries(CommunityStatusModel.AGENT);
        if (agents != null && agents.length > 0) {
            for (int i=0; i<agents.length; i++) {
                addAsset(agents[i], CommunityStatusModel.AGENT);        
            }    
        }
    }


    /** 
     * Look up community name & instantiate new asset with AssetType = ENCLAVE
     * This is called until the community name is found.
     */
    private void addEnclave() {
        String cName = csm.getCommunityName();
        if (cName != null) {
            DefaultAssetTechSpec enclave = new DefaultAssetTechSpec( null, null, cName, AssetType.ENCLAVE, us.nextUID());
            queueChangeEvent(new AssetChangeEvent( enclave, AssetChangeEvent.NEW_ASSET));
            enclaveNameFound = true;
        }
    }
    
    /**
     * Create an asset
     *
     */
    private void addAsset(String agentName, int type) {

        DefaultAssetTechSpec hostAsset;
        DefaultAssetTechSpec nodeAsset;
        DefaultAssetTechSpec agentAsset;
        String nodeName = null;
        String hostName = null;

        
        if ( type == CommunityStatusModel.AGENT ) {
                nodeName = csm.getLocation(agentName);
                if (nodeName != null) {
                    hostName = csm.getLocation(nodeName);
                }
        }
        else if ( type == CommunityStatusModel.NODE ) { //node & node agent have the same name
            nodeName = agentName;
            hostName = csm.getLocation(nodeName);
        }

        if (hostName == null || nodeName == null) {
            logger.warn("!!!! [ASSET NOT ADDED] Saw new agent asset ["+agentName+"] without host/node info: hostName = "+hostName + "  nodeName = "+nodeName);
            return;
        }

        hostAsset = getHost(hostName);
        nodeAsset = getNode(hostAsset, nodeName);

        agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, AssetType.AGENT, us.nextUID());
        queueChangeEvent(new AssetChangeEvent( agentAsset, AssetChangeEvent.NEW_ASSET));
    }
    
    
    /** Return a host asset for the given host name. Create a new host asset if one is not found. */
    private DefaultAssetTechSpec getHost(String hostName) {
        
        if (hostName == null || hostName.length() == 0) { return null; }
        
        DefaultAssetTechSpec hostAsset = DefaultAssetTechSpec.findAssetByID(new AssetID(hostName, AssetType.HOST));
        if (hostAsset == null) {
            hostAsset = new DefaultAssetTechSpec( null, null,  hostName, AssetType.HOST, us.nextUID() );
            queueChangeEvent(new AssetChangeEvent( hostAsset, AssetChangeEvent.NEW_ASSET));
        }
        return hostAsset;
    }
    
    
    /** Return a host asset for the given host name. Create a new host asset if one is not found. */
    private DefaultAssetTechSpec getNode(AssetTechSpecInterface hostAsset, String nodeName) {
        
        if (nodeName == null || nodeName.length() == 0) { return null; }
        
        DefaultAssetTechSpec nodeAsset = DefaultAssetTechSpec.findAssetByID(new AssetID(nodeName, AssetType.NODE));
        if (nodeAsset == null) {
            nodeAsset = new DefaultAssetTechSpec( hostAsset, null,  nodeName, AssetType.NODE, us.nextUID() );
            queueChangeEvent(new AssetChangeEvent( nodeAsset, AssetChangeEvent.NEW_ASSET));
            
            //Use NODE name to assign FWD / REAR property
            if (nodeName.startsWith("REAR")) {
                nodeAsset.addProperty(new NodeAssetProperty(NodeAssetProperty.location, "REAR"));
            } else if (nodeName.startsWith("FWD")) {
                nodeAsset.addProperty(new NodeAssetProperty(NodeAssetProperty.location, "FORWARD"));
            }
            
        }
        return nodeAsset;
    }
    
    
    private IncrementalSubscription communityStatusModelSub;
    private UnaryPredicate communityStatusModelPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof CommunityStatusModel);
        }
    };
    
    
    /**
     * Adds a AssetChangeListener to community.
     * @param acl  AssetChangeListener to add
     *
     * @return the array of currently known assets (as AssetChangeEvent instances)
     */
    public AssetChangeEvent[] addChangeListener(AssetChangeListener acl) {
        if (!changeListeners.contains(acl)) {
            changeListeners.add(acl);
        }
        //Now return the current list of assets. This way the lsitener won't miss any new assets.
        AssetChangeEvent myArray[];
        synchronized(eventQueue) {
            myArray = new AssetChangeEvent[allAssets.size()];
            allAssets.copyInto(myArray);
        }
        return myArray;
    }
    
    /**
     * Removes a AssetChangeListener from community.
     * @param acl  AssetChangeListener to remove
     */
    public void removeChangeListener(AssetChangeListener acl) {
        if (changeListeners.contains(acl))
            changeListeners.remove(acl);
    }
    
    
    /**
     * Send CommunityStatusChangeEvent to listeners.
     */
    protected void notifyListeners(AssetChangeEvent[] ace) {
        List listenersToNotify = new ArrayList();
        synchronized (changeListeners) {
            listenersToNotify.addAll(changeListeners);
        }
        for (Iterator it = listenersToNotify.iterator(); it.hasNext(); ) {
            AssetChangeListener asl = (AssetChangeListener) it.next();
            asl.statusChanged(ace);
        }
    }
    
    private List eventQueue = new ArrayList();
    
    /**
     * Distributes all events in queue to listeners. Send each time execute is called()
     */
    protected void sendEvents() {
        if (!eventQueue.isEmpty()) {
            AssetChangeEvent[] events = new AssetChangeEvent[0];
            synchronized (eventQueue) {
                logger.debug("AssetChangeEvent -- sendEvents(): numEvents=" + eventQueue.size());
                allAssets.addAll(eventQueue); //add current set into permanent set                
                events =
                   (AssetChangeEvent[]) eventQueue.toArray(new AssetChangeEvent[0]);
                eventQueue.clear();
            }
            publishAssets(events); //publish new assets first
            notifyListeners(events); // now notify listeners            
        }
    }
    
    /**
     * Publishes Added or changed assets. Call only in/direclty within execute method
     */
    private void publishAssets(AssetChangeEvent[] events) {
        
        for (int i=0; i<events.length; i++) {
            
            if (events[i].newAssetEvent()) {
                blackboard.publishAdd(events[i].getAsset());                
                incAssetCount();
            } else if (events[i].moveEvent()) {
                blackboard.publishChange(events[i].getAsset());                
            }            
        }
    }
    
    
    /**
     * Add a AssetChangeEvent to dissemmination queue.
     * @param ace AssetChangeEvent to send
     */
    protected void queueChangeEvent(AssetChangeEvent ace) {
        synchronized (eventQueue) {
            eventQueue.add(ace);
            logger.debug("queueEvent: (" + eventQueue.size() + ") " + ace);
        }
        // Could potentially delay & announce every x seconds
        // IF there has been new events added to the queue
        blackboard.signalClientActivity();
    }
    
    
    
}

