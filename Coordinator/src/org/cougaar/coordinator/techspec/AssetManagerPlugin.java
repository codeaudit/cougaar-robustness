/*
 * <copyright>
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */

package org.cougaar.coordinator.techspec;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;
import org.xml.sax.InputSource;

//Parts of this code were taken from Ron Snyder's StatusChangeListenerPlugin.java.

/**
 * A plugin to monitor Assets. Sends notification of new Assets via a change 
 * listener & when assets move by directly notifying subscribers. Only the
 * ThreatModelManager should probably be a subscriber.
 *
 * @author  Paul Pazandak, Ph.D. OBJS
 */
public class AssetManagerPlugin extends ComponentPlugin implements NotPersistable, AssetChangeEventConstants {
    
    private ServiceBroker sb;
    private LoggingService logger;
    private UIDService us;
    private AssetTechSpecServiceImpl assetSvc;
    // temporarily static for Believability
    private static AssetTechSpecTable assetTechSpecs = null;
    private boolean assetTechSpecsChanged = false;
    
    private AssetChangeEventQueue allEvents = null;
    private boolean allEventsChanged = false;
    private AssetChangeEventQueue pendingEvents;
    
    private CommunityStatusModel csm = null;
    private List changeListeners = Collections.synchronizedList(new ArrayList());
    /** True if the enclave (community name) was found */
    private boolean enclaveNameFound = false;
    
    private IncrementalSubscription communityStatusModelSub;
    private UnaryPredicate communityStatusModelPredicate = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof CommunityStatusModel);
	    }
	};
    
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
		
		AssetTechSpecInterface hostAsset;
		AssetTechSpecInterface nodeAsset;
		AssetTechSpecInterface agentAsset;
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

		    if (logger.isDebugEnabled()) 
			logger.debug("!!!! [STATUS CHANGED CALLED] on asset="+
				     csce[i].getName()+" with:  \n= "+csce[i].toString()+
				     "  getCurrentLocation = " + csce[i].getCurrentLocation()+
				     " getPriorLocation = " + csce[i].getPriorLocation());
                
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
            		if (logger.isDebugEnabled()) 
			    logger.debug("newAgent on asset="+csce[i].getName()+
					 " with "+csce[i].toString()+
					 " getCurrentLocation="+ csce[i].getCurrentLocation()+
					 " getPriorLocation="+csce[i].getPriorLocation());
		    } else if (csce[i].locationChanged() && csce[i].getPriorLocation() != null) {
			movingAgent = true;
            		if (logger.isDebugEnabled()) 
			    logger.debug("movingAgent on asset="+csce[i].getName()+
					 " with "+csce[i].toString()+
					 " getCurrentLocation = " + csce[i].getCurrentLocation()+
					 " getPriorLocation = " + csce[i].getPriorLocation());
		    } else if (csce[i].membersRemoved()) {
			removedAsset = true;
            		if (logger.isDebugEnabled()) 
			    logger.debug("removedAgent on asset="+csce[i].getName()+
					 " with "+csce[i].toString()+
					 " getCurrentLocation="+csce[i].getCurrentLocation()+
					 " getPriorLocation="+csce[i].getPriorLocation());
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
                        //node & node agent have the same name
			} else if ( csce[i].getType() == CommunityStatusModel.NODE ) { 
			    nodeName = agentName;
			    hostName = csce[i].getCurrentLocation();
			}
			if (hostName == null || 
			    nodeName == null || 
			    hostName.length() == 0 || 
			    nodeName.length() == 0) {
			    if (logger.isDebugEnabled()) 
				logger.debug("!!!- Queued new agent asset ["+agentName+"]"+
					     " without host/node info: hostName="+hostName+
					     " nodeName="+nodeName);
			    pendingEvents.add(csce[i]);
			    continue;
			} else { //continue
			    hostAsset = getHost(hostName);
			    nodeAsset = getNode(hostAsset, nodeName);
			}
			
			//*** Handle a new agent
			if ( newAgent )  {
			    
			    //Make sure we haven't seen this agent before we create a new one!
			    agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
			    if (agentAsset != null) {
				if (logger.isDebugEnabled()) 
				    logger.debug("Saw DUPLICATE agent asset ["+agentName+
						 "] with host/node info: hostName = "+hostName + 
						 "  nodeName = "+nodeName);
				continue;
			    }                    
			    if (logger.isDebugEnabled()) 
				logger.debug("Saw new agent asset ["+agentName+
					     "] with host/node info: hostName = "+hostName + 
					     "  nodeName = "+nodeName);
			    agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, 
								   AssetType.AGENT, us.nextUID());
			    assetSvc.putAssetTechSpec(agentAsset);
/*
			    //Use NODE name to assign FWD / REAR property -- doing this also for nodes in getNode()
			    if (nodeName.startsWith("REAR")) {
				agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "REAR"));
			    } else if (nodeName.startsWith("FWD")) {
				agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "FORWARD"));
			    }
*/
			    queueChangeEvent(new AssetChangeEvent( agentAsset, NEW_ASSET));
			    
                        //*** Handle an agent moving
			} else if (movingAgent) { //it's a movingAgent
			    
			    logger.info("Agent location changed: agent=" + agentName +
					" priorLocation=" + csce[i].getPriorLocation() +
					" newLocation=" + csce[i].getCurrentLocation());
			    
			    agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
			    if (agentAsset == null) { // then this is an agent we've never seen before!
				agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, 
								       AssetType.AGENT, us.nextUID());                            
				assetSvc.putAssetTechSpec(agentAsset);
			    }
			    agentAsset.setNewLocation(hostAsset, nodeAsset);
			    queueChangeEvent(new AssetChangeEvent( agentAsset, MOVED_ASSET));
/*			    
			    //Use NODE name to assign FWD / REAR property
			    if (nodeName.startsWith("REAR")) {
				agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "REAR"));
			    } else if (nodeName.startsWith("FWD")) {
				agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "FWD"));
			    }
*/			    
			    //*** Handle an agent that has been removed
			} 
			
		    } else if (removedAsset) { //it's a removedAsset
                        
                        //if it's a node agent, remove the node asset.
			
                        if (csce[i].getType() == CommunityStatusModel.AGENT) {
                            logger.info("Agent REMOVED: agent=" + agentName);                        
                            agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
                            if (agentAsset != null) {
                                queueChangeEvent(new AssetChangeEvent( agentAsset, REMOVED_ASSET));                                                
                            }
                        } else if (csce[i].getType() == CommunityStatusModel.NODE) {
                            //then we must remove both the node asset and the node-agent asset
                            logger.info("NODE AND NODE-AGENT REMOVED: node=" + agentName);                        
                            nodeAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.NODE));
                            if (nodeAsset != null) {
                                queueChangeEvent(new AssetChangeEvent( nodeAsset, REMOVED_ASSET));                                                
                            }
                            agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
                            if (agentAsset != null) {
                                queueChangeEvent(new AssetChangeEvent( agentAsset, REMOVED_ASSET));                                                
                            }
                        }
                        
                        
                } else {
                   // logger.debug("!!!! [STATUS CHANGED CALLED] - UNKNOWN change");
                }
            }

            // dw - check if any of the agents for which there was previously incomplete info can now be completed
            Iterator iter = pendingEvents.iterator();
            while (iter.hasNext()) {
                CommunityStatusChangeEvent thisCSCE = (CommunityStatusChangeEvent) iter.next();
                agentName = thisCSCE.getName();
                if (logger.isDebugEnabled()) logger.debug("Considering the pending CSCE: " + thisCSCE.toString());
                // this is basically a clone of the handling of the original handling - really should be merged
                if ( thisCSCE.getType() == CommunityStatusModel.AGENT ) {
                    nodeName = thisCSCE.getCurrentLocation();
                    hostName = csm.getLocation(nodeName);
                }
                else if ( thisCSCE.getType() == CommunityStatusModel.NODE ) { //node & node agent have the same name
                    nodeName = agentName;
                    hostName = thisCSCE.getCurrentLocation();
                }

                if (hostName == null || nodeName == null || hostName.length() == 0 || nodeName.length() == 0) {
                    if (logger.isDebugEnabled()) 
			logger.debug("!!!- Still incomplete: "+agentName+
				     "] without host/node info: hostName = "+
				     hostName + "  nodeName = "+nodeName);
                    continue;
                } else { //continue
                    hostAsset = getHost(hostName);
                    nodeAsset = getNode(hostAsset, nodeName);
                }

                //*** Handle an agent whose info is now complete - it becomes a new agent entry

                // Make sure we haven't seen this agent before we create a new one! 
		// this should be impossible when handling pending requests
                agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
                if (agentAsset != null) {
                    if (logger.isDebugEnabled()) 
			logger.debug("Saw DUPLICATE agent asset ["+agentName+
				     "] with host/node info: hostName = "+
				     hostName + "  nodeName = "+nodeName);
                    continue;
                }                    
                if (logger.isDebugEnabled()) 
		    logger.debug("Saw new agent asset from pending queue ["+
				 agentName+"] with host/node info: hostName = "+
				 hostName + "  nodeName = "+nodeName);
                agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, AssetType.AGENT, us.nextUID());
		assetSvc.putAssetTechSpec(agentAsset);
/*
                //Use NODE name to assign FWD / REAR property -- doing this also for nodes in getNode()
                if (nodeName.startsWith("REAR")) {
                    agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "REAR"));
                } else if (nodeName.startsWith("FWD")) {
                    agentAsset.addProperty(new AgentAssetProperty(AgentAssetProperty.location, "FORWARD"));
                }
*/
                iter.remove();
                queueChangeEvent(new AssetChangeEvent( agentAsset, NEW_ASSET));
            }
        }
    };

    public void load() {
        super.load();
	sb = getServiceBroker();
	logger =(LoggingService)
	    sb.getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
	Provider provider = new Provider();
	assetSvc = new AssetTechSpecServiceImpl();
	sb.addService(AssetTechSpecService.class, provider);
	if (logger.isDebugEnabled()) 
	    logger.debug("load: AssetTechSpecService was added.");
        us = (UIDService)sb.getService(this, UIDService.class, null) ;
        if (logger.isDebugEnabled()) 
	    logger.debug("AssetManagerPlugin loaded.");
    }
    
    /**
     * Subscribes to the CommunityStatusModel, reads in the AssetStateDimensions from XML,
     * and then publishes itself.
     */
    public void setupSubscriptions() {
	Collection c = blackboard.query(new UnaryPredicate() {
		public boolean execute(Object o) {
		    return (o instanceof AssetTechSpecTable);
		}
	    });
	if (c.isEmpty()) {
	    if (logger.isDebugEnabled()) 
		logger.debug("AssetTechSpecTable not found on blackboard.");
	    blackboard.publishAdd(assetTechSpecs);
	} else {
	    assetTechSpecs = (AssetTechSpecTable)c.iterator().next();
	    if (logger.isDebugEnabled()) 
		logger.debug("AssetTechSpecTable found on blackboard = "+assetTechSpecs);
	}
	c = blackboard.query(new UnaryPredicate() {
		public boolean execute(Object o) {
		    return (o instanceof AssetChangeEventQueue);
		}
	    });
	if (c.isEmpty()) {
	    if (logger.isDebugEnabled()) 
		logger.debug("AssetChangeEventQueue not found on blackboard.");
	    allEvents = new AssetChangeEventQueue(100,100);
	    blackboard.publishAdd(allEvents);
	} else {
	    allEvents = (AssetChangeEventQueue)c.iterator().next();
	    if (logger.isDebugEnabled()) 
		logger.debug("AssetChangeEventQueue found on blackboard = "+allEvents);
	}
        pendingEvents = new AssetChangeEventQueue(100);
        communityStatusModelSub =
	    (IncrementalSubscription)blackboard.subscribe(communityStatusModelPredicate);                
        blackboard.publishAdd(this); //publish myself, ONLY after reading in XML
    }
    
    public void execute() {
        sendEvents();
        Collection models = communityStatusModelSub.getAddedCollection();
        for (Iterator it = models.iterator(); it.hasNext();) {
            CommunityStatusModel csm = (CommunityStatusModel)it.next();
            logger.info("Found CommunityStatusModel: community=" + csm.getCommunityName());
            csm.addChangeListener(myChangeListener);
            
            //According to Ron there will be only ONE csm per BB.
            this.csm = csm;
            
            //get all agents already known by the csm - called once.
            getCurrentAssetsInCSM();
        }
	synchronized (assetTechSpecs) {
	    if (assetTechSpecsChanged) {
		blackboard.publishChange(assetTechSpecs);
		assetTechSpecsChanged = false;
	    }
	}
	synchronized (allEvents) {
	    if (allEventsChanged) {
		blackboard.publishChange(allEvents);
		allEventsChanged = false;
	    }
	}
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
	//String cName = csm.getCommunityName();
	// change to use Robustness Manager name
	String cName = csm.getAttribute("RobustnessManager"); 
        if (cName != null) {
            AssetTechSpecInterface enclave = new DefaultAssetTechSpec( null, null, cName, AssetType.ENCLAVE, us.nextUID());
	    assetSvc.putAssetTechSpec(enclave);
            queueChangeEvent(new AssetChangeEvent( enclave, NEW_ASSET));
            enclaveNameFound = true;
        }
    }
    
    /**
     * Create an asset
     *
     */
    private void addAsset(String agentName, int type) {

        AssetTechSpecInterface hostAsset;
        AssetTechSpecInterface nodeAsset;
        AssetTechSpecInterface agentAsset;
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
            if ( type == CommunityStatusModel.AGENT ) {
                //logger.warn("!!!! [ASSET NOT ADDED] Saw new AGENT asset ["+agentName+
                //"] without host/node info: hostName = "+hostName + "  nodeName = "+nodeName);
            } else if ( type == CommunityStatusModel.NODE ) {
                //logger.warn("!!!! [ASSET NOT ADDED] Saw new NODE asset ["+agentName+
                //"] without host/node info: hostName = "+hostName + "  nodeName = "+nodeName);
            }
            return;
        }

        hostAsset = getHost(hostName);
        nodeAsset = getNode(hostAsset, nodeName);

        //Make sure we haven't seen this agent before we create a new one!
        agentAsset = assetSvc.getAssetTechSpec(new AssetID(agentName, AssetType.AGENT));
        if (agentAsset == null) {
            agentAsset = new DefaultAssetTechSpec( hostAsset, nodeAsset,  agentName, AssetType.AGENT, us.nextUID());
	    assetSvc.putAssetTechSpec(agentAsset);
            queueChangeEvent(new AssetChangeEvent( agentAsset, NEW_ASSET));
        }
    }
    
    /** Return a host asset for the given host name. Create a new host asset if one is not found. */
    private AssetTechSpecInterface getHost(String hostName) {
        
        if (hostName == null || hostName.length() == 0) { return null; }
        
        AssetTechSpecInterface hostAsset = assetSvc.getAssetTechSpec(new AssetID(hostName, AssetType.HOST));
        if (hostAsset == null) {
            hostAsset = new DefaultAssetTechSpec( null, null,  hostName, AssetType.HOST, us.nextUID() );
	    assetSvc.putAssetTechSpec(hostAsset);
            queueChangeEvent(new AssetChangeEvent( hostAsset, NEW_ASSET));
            if (logger.isDebugEnabled()) logger.debug("Queued new host asset");
        }
        return hostAsset;
    }
    
    /** Return a node asset for the given node name. Create a new node asset if one is not found. */
    private AssetTechSpecInterface getNode(AssetTechSpecInterface hostAsset, String nodeName) {
        
        if (nodeName == null || nodeName.length() == 0) { return null; }
        
        AssetTechSpecInterface nodeAsset = assetSvc.getAssetTechSpec(new AssetID(nodeName, AssetType.NODE));
        if (nodeAsset == null) {
            nodeAsset = new DefaultAssetTechSpec(hostAsset, null, nodeName, AssetType.NODE, us.nextUID());
	    assetSvc.putAssetTechSpec(nodeAsset);
            queueChangeEvent(new AssetChangeEvent( nodeAsset, NEW_ASSET));
            if (logger.isDebugEnabled()) logger.debug("Queued new node asset");
/*            
            //Use NODE name to assign FWD / REAR property
            if (nodeName.startsWith("REAR")) {
                nodeAsset.addProperty(new NodeAssetProperty(NodeAssetProperty.location, "REAR"));
            } else if (nodeName.startsWith("FWD")) {
                nodeAsset.addProperty(new NodeAssetProperty(NodeAssetProperty.location, "FORWARD"));
            }
*/            
        }
        return nodeAsset;
    }
      
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
            myArray = new AssetChangeEvent[allEvents.size()];
            allEvents.copyInto(myArray);
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
                if (logger.isDebugEnabled()) logger.debug("AssetChangeEvent -- sendEvents(): numEvents=" + eventQueue.size());
                allEvents.addAll(eventQueue); //add current set into permanent set   
		allEventsChanged = true;
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
            if (logger.isDebugEnabled()) 
		logger.debug("queueEvent: ("+eventQueue.size()+") "+ 
			     ace.toString());
        }
        // Could potentially delay & announce every x seconds
        // IF there has been new events added to the queue
        blackboard.signalClientActivity();
    }
    
    private class Provider implements ServiceProvider {
	
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) {
	    if (serviceClass == AssetTechSpecService.class)
		return assetSvc;
	    else
		return null;
	}
	
	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)  {
	}
    }
    
    private class AssetTechSpecServiceImpl implements AssetTechSpecService {
	private AssetTechSpecServiceImpl() {
	    if (assetTechSpecs != null &&
		logger.isErrorEnabled())
		logger.error("AssetManagerPlugin loaded more than once per node.", 
			     new Throwable());
	    assetTechSpecs = new AssetTechSpecTable();
	}
	void putAssetTechSpec(AssetTechSpecInterface ts) {
	    synchronized(assetTechSpecs) {
		if (logger.isDetailEnabled()) 
		    logger.detail("putAssetTechSpec("+ts+")");
		assetTechSpecs.put(ts.getAssetID(), ts);
		assetTechSpecsChanged = true;
		if (blackboard != null)
		    blackboard.signalClientActivity();
	    }
	}
	public AssetTechSpecInterface getAssetTechSpec(AssetID assetID) {
	    synchronized(assetTechSpecs) {
		AssetTechSpecInterface ts = 
		    (AssetTechSpecInterface)assetTechSpecs.get(assetID);
		if (logger.isDetailEnabled()) 
		    logger.detail("getAssetTechSpec("+assetID+")="+ts);
		return ts;
	    }
	}       
    }

    // temporarily required by Believability
    public static AssetTechSpecInterface getAssetTechSpec(AssetID assetID) {
	synchronized(assetTechSpecs) {
	    AssetTechSpecInterface ts = 
		(AssetTechSpecInterface)assetTechSpecs.get(assetID);
            Logger log = Logging.getLogger(AssetManagerPlugin.class);
	    if (log.isDetailEnabled()) 
		log.detail("getAssetTechSpec("+assetID+")="+ts);
	    return ts;
	}
    }       	
}
