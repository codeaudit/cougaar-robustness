/*
 * ThreatModelManagerPlugin.java
 *
 * Created on September 15, 2003, 4:39 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

//org.cougaar.coordinator.techspec.ThreatModelManagerPlugin

package org.cougaar.coordinator.techspec;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Hashtable;

import org.cougaar.core.util.UID;
import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate; 


/**
 * This class contains the ThreatModel Manager Plugin. It is responsible for
 * populating the Threat Model and for managing each model's asset membership. Membership
 * may change based upon changes to an asset's location, or a property change, as dictated
 * by any defined membership filters.
 *
 * ThreatDescriptions are read in via an XML file. This filename MUST be provided as a
 * plugin parameter.
 *
 * At *THIS* time the only changes to a threat model (via the servlet) are to probabilities.
 * Such changes will NOT affect membership. Once (and IF) we enable changes to the membership 
 * filters, ONLY then will we need to listen to ThreatModel changes & re-evaluate memberships.
 *
 * @author  Paul Pazandak Ph.D, OBJS
 */
public class ThreatModelManagerPlugin extends ComponentPlugin {
    
    boolean haveServices = false;
    private LoggingService logger;
    private UIDService us = null;

    /** The file param passed in via the plugin */
    private String fileParam  = null;

    /** Set to true when the asset listener has been added */
    private boolean ADDED_ASSET_LISTENER = false;

    /** The collection of threatDescriptions */
    private Collection threatDescriptions;

    /** List of all assets */
    private Vector assets;
    
    /** Creates a new instance of ThreatModelManagerPlugin 
     *
     *
     */
    public ThreatModelManagerPlugin() {
    
        assets = new Vector(100,50);
    }
    
    private Vector changesToProcess = new Vector(20,20);

    private boolean newThreatsAdded = false;
    
    
    //Subscribe to new assets / changes announced by the CommunityStatusModel
    private AssetChangeListener myChangeListener = new AssetChangeListener() {        
        public void statusChanged(AssetChangeEvent[] aces) {            
            //Evaluate membership
            queueChanges(aces);
            blackboard.signalClientActivity();            
        }
    };
    
    
    /** Queue changes that will be processed during the execute method */
    private void queueChanges(AssetChangeEvent[] aces) {
        
        synchronized(changesToProcess) {
            for (int i=0; i<aces.length; i++) {
                changesToProcess.addElement(aces[i]);
            }
    
        }
    }
    
    
  /** ********************************************************************* To DO
    *
    * Membership is granted as long as an asset's type is equal to the threat model's 
    * asset type, and no model filter excludes the asset.
    *                   
    * Performs a publish Change to the BB. Use IncrementalSubscription.getChangeReports(Object o) 
    * to get the list of change reports for each changed model. These reports will be of the type 
    * ThreatModelChangeEvent & have an eventType of MEMBERSHIP_CHANGE. Only call from an open TX!    
    */    
    private void evaluateThreatAssetMembership() {

        synchronized(changesToProcess) {
            //If no new assets or new threats, don't do anything
            if (changesToProcess.size() == 0 ) { return; }

            
            ThreatDescription metaModel;
            AssetChangeEvent event;
            DefaultThreatModel threatModel;
            DefaultAssetTechSpec asset;
            
            Vector hosts  = new Vector();
            Vector nodes  = new Vector();
            Vector agents = new Vector();

            //Emit list of assets to consider
            Iterator itr = changesToProcess.iterator();
            while (itr.hasNext()) {
                event = (AssetChangeEvent)itr.next();
                asset = (DefaultAssetTechSpec) event.getAsset();
                logger.debug("evaluateThreatAssetMembership looking at asset " + asset.getName() + "["+asset.getAssetType()+"]");
            }
            
            
            Vector changedModels = new Vector(); //keep track so we can publish change them to the BB.

            logger.debug("evaluateThreatAssetMembership called with " + changesToProcess.size() + " events");

            //Iterate over threatDescriptions
            if ( (threatDescriptions != null && threatDescriptions.size() > 0) ) {

//logger.debug("evaluateThreatAssetMembership has " + threatDescriptions.size() + " threatDescriptions to examine.");

                Iterator i = threatDescriptions.iterator();
                while (i.hasNext()) {

                    metaModel = (ThreatDescription)i.next();
//logger.debug("evaluateThreatAssetMembership looking at threat " + metaModel.getName() );

                        
                        Iterator ctp = changesToProcess.iterator();
                        while (ctp.hasNext()) {

                            threatModel = null;
                            event = (AssetChangeEvent)ctp.next();
                            asset = (DefaultAssetTechSpec) event.getAsset();

                            if (asset.getAssetType().equals(AssetType.HOST)) { hosts.add(asset); }
                            else if (asset.getAssetType().equals(AssetType.NODE)) { nodes.add(asset); }
                            else if (asset.getAssetType().equals(AssetType.AGENT)) { agents.add(asset); }
                            
                            
                            if (event.moveEvent() || event.newAssetEvent() ) {
                                //IF the agent moved or is new
                                //check to see if the asset qualifies

                                //** First check type of asset against type of threatModel. Ignore if they don't match
                                if (!(metaModel.getAffectedAssetType().equals(asset.getAssetType()))) {
//logger.debug("evaluateThreatAssetMembership -- asset types don't match: \n modelAssetType="+metaModel.getAffectedAssetType()+"\nasset's type["+asset.getName()+"] = "+asset.getAssetType());
                                    continue;
                                }

                                boolean qualifies = true; //assume the asset qualifies, until a filter rejects it.

                                ThreatVulnerabilityFilter filter = metaModel.getVulnerabilityFilter();

                                //IF NO FILTER -- add the asset as a member
                                if (filter == null) {

                                    qualifies = true;

                                } else { //OK, there are FILTERS, so see if the asset qualifies

                                    qualifies = filter.qualifies(this, asset);
                                    
                                }

                                if (qualifies) {
                                    threatModel = addAssetAsMember(asset, metaModel);
                                    if (threatModel == null) {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! But already exists in the "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
                                    } else {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! Adding to the "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"], memberCount="+threatModel.getAssetList().size());
                                    }
                                } else { //remove the asset from the threat model's membership, if it's there                         
//logger.debug("evaluateThreatAssetMembership - doesn't qualify.");
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                        
                                    if (threatModel != null) {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
                                    }
                                }
                                
                              //If the asset was removed, just remove from this threat, if there
                            } else if (event.assetRemovedEvent() ) {
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                       
                                    if (threatModel != null) {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
                                    }
                            }

                            //Now if the threatModel != null, then it was changed so add it to the list to publishChange
                            if (threatModel != null) {
                                if (! changedModels.contains(threatModel)) {
                                    changedModels.addElement(threatModel);
                                }
                            }
                            
                        } //while loop - on asset change events
                } //while() loop on ThreatDescriptions iterator

                if (changedModels.size() > 0) { 

                    Collection changes;
                    Iterator cModels = changedModels.iterator();
                    while (cModels.hasNext()) {
                        DefaultThreatModel dtm = (DefaultThreatModel) cModels.next();
                        
                        //Announce newly added assets
                        Vector added = dtm.clearNewAssets();
                        Vector removed = dtm.clearRemovedAssets();
                        if (added.size() > 0 || removed.size() > 0) { //some assets were added to this threat, so announce it
                            changes = Collections.singleton( new ThreatModelChangeEvent( dtm, added, removed, ThreatModelChangeEvent.MEMBERSHIP_CHANGE) );
                            this.blackboard.publishChange(dtm, changes);
                            logger.debug("Announced "+added.size()+" assets were added, and " +removed.size()+" assets were removed in threat = " + dtm.getName());
                        }                        
                    }
                }

                //Now process the transitive effects
                
                processEnclaveTransitiveEffects(hosts, nodes, agents);
                processHostTransitiveEffects(nodes, agents);
                processNodeTransitiveEffects(agents);
                
                
                
                
            } //if - there are any threat models

            //Now remove all processed events
            changesToProcess.removeAllElements();
        } //synchronized
    }
        
    
    /**
     * This is a special case method -- because any threat on enclaves will affect all assets in the enclave.
     * Therefore, all assets in the enclave with the asset type specified in the transitive effect containment filter 
     * will be added to the transitive effect.
     *
     */
    private void processEnclaveTransitiveEffects(Vector hosts, Vector nodes, Vector agents) {
     
        if (hosts.size() == 0 && nodes.size() == 0 && agents.size() == 0) { 
            logger.debug("========================================== 11 ==> processEnclaveTransitiveEffects - no assets to process");
            return; 
        }
        
        ThreatDescription metaModel;        
        TransitiveEffectDescription  transEffect;

        Vector childTEs = new Vector();
        
        Iterator i = threatDescriptions.iterator();
        while (i.hasNext()) {
            
            metaModel = (ThreatDescription)i.next();
            transEffect = metaModel.getEventThreatCauses().getTransitiveEffect();
            if (transEffect == null ) { continue; } // no transitive effect to process
            
            DefaultThreatModel dtm = metaModel.getInstantiation();
            //Look only for ENCLAVE threats
            if (dtm == null || !(dtm.getAssetType().equals(AssetType.ENCLAVE)) ) { continue; } // no threats to process

            
            TransitiveEffectDescription  hostTransEffect = null;
            TransitiveEffectDescription  nodeTransEffect = null;
            TransitiveEffectDescription  agentTransEffect = null;
            TransitiveEffectDescription  childTE = null;
            TransitiveEffectDescription  grandchildTE = null;
            //Look up all transitive effects for this transitive effect
            childTE = transEffect.getTransitiveEvent().getTransitiveEffect(); //this could be a node or agent TE
            if (childTE != null) { //see if there is a grandchild TE
                grandchildTE = childTE.getTransitiveEvent().getTransitiveEffect(); // this could only be an agent TE
                if (grandchildTE != null ) { // then it must be agent typed
                    if (grandchildTE.getTransitiveAssetType().equals(AssetType.AGENT)) {
                        agentTransEffect = grandchildTE;
                    } else { //error!
                        logger.warn("Found a transEffect beneath TWO trans effects (on event["+grandchildTE.getTransitiveEvent().getName()+"])that was not on an agent container! IGNORING IT. It was: "+transEffect.getTransitiveAssetType() );
                    }
                } else { // the childTE could either be an agent or node container
                    if (childTE.getTransitiveAssetType().equals(AssetType.AGENT)) {
                        agentTransEffect = childTE;
                    } else if (childTE.getTransitiveAssetType().equals(AssetType.NODE)) {
                        nodeTransEffect = childTE;
                    } else { //error!
                        logger.warn("Found a transEffect beneath ONE trans effects (on event["+childTE.getTransitiveEvent().getName()+"])that was not on an agent or node container! IGNORING IT. It was: "+transEffect.getTransitiveAssetType() );
                    }
                }
            }
            if (transEffect.getTransitiveAssetType().equals(AssetType.HOST)) {
                hostTransEffect = transEffect;
            } else if (transEffect.getTransitiveAssetType().equals(AssetType.AGENT)) {
                if (agentTransEffect != null) { // then we had a child/grandchild TE agent container already!
                    logger.warn("Found an agent transEffect on an enclave threat (on threat["+dtm.getName()+"])that had a grand/child trans effect on an agent container! IGNORING IT. ");
                } else {
                    agentTransEffect = transEffect;
                }
            } else if (transEffect.getTransitiveAssetType().equals(AssetType.NODE)) {
                if (nodeTransEffect != null) { // then we had a child/grandchild TE node container already!
                    logger.warn("Found a node transEffect on an enclave threat (on threat["+dtm.getName()+"])that had a grand/child trans effect on a node container! IGNORING IT. ");
                } else {
                    nodeTransEffect = transEffect;
                }
            } else { //error!
                logger.warn("Found a transEffect on an enclave threat (on threat["+dtm.getName()+"])that was NOT a HOST, NODE or AGENT container! IGNORING IT. It was: "+transEffect.getTransitiveAssetType());
            }
                
            //Add hosts to transEffectModel
            if (hostTransEffect != null && hosts.size() > 0) {
                TransitiveEffectModel tem = hostTransEffect.getInstantiation();
                if (tem == null) {
                    logger.debug("========================================== 7 ==> Creating host transitive effect model!");
                    tem = hostTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = hosts.iterator();
                while (it.hasNext()) {
                    logger.debug("========================================== 8 ==> Adding host to transitive effect model!");
                    tem.addAsset((AssetTechSpecInterface)it.next());
                }        
            }
                
            //Add nodes to transEffectModel
            if (nodeTransEffect != null && nodes.size() > 0) {
                TransitiveEffectModel tem = nodeTransEffect.getInstantiation();
                if (tem == null) {
                    logger.debug("========================================== 7 ==> Creating node transitive effect model!");
                    tem = nodeTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = nodes.iterator();
                while (it.hasNext()) {
                    logger.debug("========================================== 8 ==> Adding node to transitive effect model!");
                    tem.addAsset((AssetTechSpecInterface)it.next());
                }        
            }

            //Add agents to transEffectModel
            if (agentTransEffect != null && agents.size() > 0) {
                TransitiveEffectModel tem = agentTransEffect.getInstantiation();
                if (tem == null) {
                    logger.debug("========================================== 7 ==> Creating agent transitive effect model!");
                    tem = agentTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = agents.iterator();
                while (it.hasNext()) {
                    logger.debug("========================================== 8 ==> Adding agent to transitive effect model!");
                    tem.addAsset((AssetTechSpecInterface)it.next());
                }        
            }
            
        }
        
    }
    

    /**
     * This method inspects all host threats for direct & indirect transitive effects on nodes & agents
     *
     */
    private void processHostTransitiveEffects(Vector nodes, Vector agents) {
     
        if (nodes.size() == 0 && agents.size() == 0) { 
            logger.debug("========================================== 11 ==> processHostTransitiveEffects - no assets to process");
            return; 
        }
        
        ThreatDescription metaModel;        
        TransitiveEffectDescription  nodeTransEffect = null;
        TransitiveEffectDescription  agentTransEffect = null;
        TransitiveEffectDescription  transEffect = null;

        Iterator i = threatDescriptions.iterator();
        while (i.hasNext()) {
            
            metaModel = (ThreatDescription)i.next();
            transEffect = metaModel.getEventThreatCauses().getTransitiveEffect();
            if (transEffect == null ) { continue; } // no transitive effect to process
            
            DefaultThreatModel dtm = metaModel.getInstantiation();
            //Look only for HOST threats
            if (dtm == null || !(dtm.getAssetType().equals(AssetType.HOST)) ) { continue; } // only process host threats
            
            nodeTransEffect = null;
            agentTransEffect = null;
            
            if (transEffect.getTransitiveAssetType().equals(AssetType.HOST)) { //then there may be another trans effect on agents, look for it
                logger.warn("Found a transEffect on a host threat (on threat["+dtm.getName()+"])that was on a HOST container! IGNORING IT. ");
            }
            else if (transEffect.getTransitiveAssetType().equals(AssetType.NODE)) { //then there may be another trans effect on agents, look for it
                nodeTransEffect = transEffect;
                
                //look for a child trans eefect -- must be of agent type since it's under a node trans effect
                agentTransEffect = nodeTransEffect.getTransitiveEvent().getTransitiveEffect();
                
                if (agentTransEffect != null && agentTransEffect.getTransitiveAssetType().equals(AssetType.AGENT)) { //then there must be an error -- node trans effects can only contain agents
                    logger.warn("Found a transEffect beneath a node trans effect (on event["+nodeTransEffect.getTransitiveEvent().getName()+"])that was not on an agent container! IGNORING IT. It was: "+transEffect.getTransitiveAssetType() );
                    agentTransEffect = null;
                }
            }   
            else if (transEffect.getTransitiveAssetType().equals(AssetType.AGENT)) { //then there cannot be anymore transeffects below this one
                agentTransEffect = transEffect;
            }
                
            TransitiveEffectModel node_tem = null;
            TransitiveEffectModel agent_tem= null;
            
            //Now, search the node assets, if a nodeTransEffect exists. Add a node if its host is a member of the current threat.
            if (nodeTransEffect != null) { //get its instantiation
                node_tem = nodeTransEffect.getInstantiation();
                if (node_tem == null) {
                    logger.debug("========================================== 7 ==> Creating node transitive effect model!");
                    node_tem = nodeTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(agent_tem);
                }

                Iterator it = nodes.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface node = (AssetTechSpecInterface)it.next();
                    if (dtm.containsAsset(node.getHost()) ) { // then this node should be added to the transitive effect
                        logger.debug("========================================== 9 ==> Adding node to transitive effect model!");
                        node_tem.addAsset(node);
                    } else {
                        logger.debug("========================================== 9.5 ==> Node not added to transitive effect model! Host Threat="+dtm.getName()+" did not contain this node's["+node.getName()+"] host="+node.getHost().getName());
                    }
                }   
            }
            
            //Now, search the agent assets, if an agentTransEffect exists. Add an agent if its host is a member of the current threat, or node is a member of the nodeTransEffect.
            if (agentTransEffect != null) { //get its instantiation
                agent_tem = agentTransEffect.getInstantiation();
                if (agent_tem == null) {
                    logger.debug("========================================== 7 ==> Creating a child agent transitive effect model!");
                    agent_tem = agentTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(agent_tem);
                }
                
                Iterator it = agents.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface agent = (AssetTechSpecInterface)it.next();
                    if (dtm.containsAsset(agent.getHost()) ) { // then this node should be added to the transitive effect
                        logger.debug("========================================== 9 ==> Adding agent to transitive effect model!");
                        agent_tem.addAsset(agent);
                    } else {
                        logger.debug("========================================== 9.5 ==> Agent not added to transitive effect model! Host Threat="+dtm.getName()+" did not contain this agent's["+agent.getName()+"] host="+agent.getHost().getName());
                    }
                }   
            }
        }      
    }
    

    /**
     * This method inspects all node threats for transitive effects on agents
     *
     */
    private void processNodeTransitiveEffects(Vector agents) {
     
        if (agents.size() == 0) { 
            logger.debug("========================================== 11 ==> processNodeTransitiveEffects - no assets to process");
            return; 
        }

        ThreatDescription metaModel;        
        TransitiveEffectDescription  transEffect = null;

        Iterator i = threatDescriptions.iterator();
        while (i.hasNext()) {
            
            metaModel = (ThreatDescription)i.next();
            transEffect = metaModel.getEventThreatCauses().getTransitiveEffect();
            if (transEffect == null ) { continue; } // no transitive effect to process
            
            DefaultThreatModel dtm = metaModel.getInstantiation();
            //Look only for HOST threats
            if (dtm == null || !(dtm.getAssetType().equals(AssetType.NODE)) ) { continue; } // only process node threats
                        
            if (! (transEffect.getTransitiveAssetType().equals(AssetType.AGENT))) { //then this is an error
                logger.warn("Found a transEffect on a node threat (on threat["+dtm.getName()+"])that was not on an AGENT container! IGNORING IT. It was on asset type = " + transEffect.getTransitiveAssetType());
                continue;
            }
                
            TransitiveEffectModel tem= null;
                        
            //Now, search the agent assets, if a transEffect exists. Add an agent if its node is a member of the current threat
            if (transEffect != null) { //get its instantiation
                tem = transEffect.getInstantiation();
                if (tem == null) {
                    logger.debug("========================================== 7 ==> Creating a child agent transitive effect model!");
                    tem = transEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                
                Iterator it = agents.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface agent = (AssetTechSpecInterface)it.next();
                    if (dtm.containsAsset(agent.getNode()) ) { // then this agent should be added to the transitive effect
                        logger.debug("========================================== 10 ==> Adding agent to transitive effect model!");
                        tem.addAsset(agent);
                    } else {
                        logger.debug("========================================== 10.5 ==> Agent not added to transitive effect model! Node Threat="+dtm.getName()+" did not contain this agent's node="+agent.getNode().getName());
                    }
                }   
            }
        }      
    }
    
    
    /**
     * Adds an asset to a threatModel, creating the model if nec.
     * @return the DefaultThreatModel, if modified. It will not be modified if the
     * asset was already a member. ONLY call from an open Transaction.
     */
    private DefaultThreatModel addAssetAsMember(AssetTechSpecInterface asset, ThreatDescription metaModel) {
        
        boolean createdModel = false;
        DefaultThreatModel dtm = metaModel.getInstantiation();
        if (dtm == null) {
            logger.debug("========================================== 6 ==> Creating threat model!");
            dtm = metaModel.instantiate(us.nextUID());
            
            this.blackboard.publishAdd(dtm);
        }
        boolean newAddition = dtm.addAsset(asset);       
        if (newAddition) {
            return dtm;
        } else {
            return null;
        }
    }
    
    /**
     * Tries to remove an asset from the membership of a threat
     * @return the DefaultThreatModel, if modified. It will not be modified if the
     * asset was not a member.
     */
    private DefaultThreatModel removeAssetAsMember(AssetTechSpecInterface asset, ThreatDescription metaModel) {
        
        DefaultThreatModel dtm = metaModel.getInstantiation();
        if (dtm == null) { return null; } //nothing to remove
        
        boolean wasAMember = dtm.removeAsset(asset);
        if (wasAMember) {
            return dtm;
        } else {
            return null;
        }
        
    }
    
    
    
    /** 
     * set up subscriptions & load in the XML-based meta threat models
     */
    public void setupSubscriptions() {
        
        getServices();        
        
        // Subscribe to CommunityStatusModel
        assetManagerSub =
        (IncrementalSubscription)blackboard.subscribe(assetManagerModelPredicate);

        threatDescriptionsSub =
        (IncrementalSubscription)blackboard.subscribe(threatDescriptionsPredicate);
    }

    
    
    
    /*
     * Subscribe to the AssetManagerPlugin
     *
     */
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

        //Update the threatDescriptions collection if new ones were added
        if (threatDescriptionsSub.getAddedCollection().size() > 0) {
            if (threatDescriptions != null && threatDescriptions.size() > 0) {
                logger.warn("Read in threat descriptions on subsequent execute() invocations. They should have been all seen at once!");
                //Should not happen. If it did, the assets already seen would not be compared against these threats until
                //they changed in some way. Since the threat descriptions are published in a setupSubscription, this should not occur.
            }
            threatDescriptions = threatDescriptionsSub.getCollection();
            logger.debug("Got " + threatDescriptions.size() + " ThreatDescriptions from the blackboard.");
        }
        
        //Get the asset manager plugin
        Collection assetMgrs = assetManagerSub.getAddedCollection();
        Iterator it = assetMgrs.iterator(); 
        if (it.hasNext() && !ADDED_ASSET_LISTENER ) { // *** This should only execute once.
            AssetManagerPlugin aMgr = (AssetManagerPlugin)it.next();
            logger.info("Found AssetManagerPlugin");
            
            //Subscribe to the AssetMgrPlugin's change listener. 
            AssetChangeEvent currentAssets[] = aMgr.addChangeListener(myChangeListener);            
            synchronized(changesToProcess) {
                for (int i=0; i<currentAssets.length; i++) {
                    changesToProcess.addElement(currentAssets[i]);
                }

            }
            ADDED_ASSET_LISTENER = true;            
        } 
        
        evaluateThreatAssetMembership();            
            
    }

    /** get our services */
    private void getServices() {
        
        logger =
        (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        
        us = (UIDService ) getBindingSite().getServiceBroker().getService( this, UIDService.class, null ) ;
     
        haveServices = true;
    }
    
    private IncrementalSubscription assetManagerSub;
    private UnaryPredicate assetManagerModelPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof AssetManagerPlugin);
        }
    };
    

    private IncrementalSubscription threatDescriptionsSub;
    private UnaryPredicate threatDescriptionsPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof ThreatDescription);
        }
    };
    
}
