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
import java.util.Enumeration;
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

    /** All transitive effects */
    Hashtable transEffects;

    /** The file param passed in via the plugin */
    private String fileParam  = null;

    /** Set to true when the asset listener has been added */
    private boolean ADDED_ASSET_LISTENER = false;

    /** Set to true when the TechSpecsLoadedCondition has been added */
    private boolean TECHSPECS_ARE_LOADED = false;
    
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
        transEffects = new Hashtable( );
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
            
            //vectors of assets added /moved that need to be added to transitive effects
            Vector hosts  = new Vector();
            Vector nodes  = new Vector();
            Vector agents = new Vector();

            Vector removedAssets = new Vector();

            //Emit list of assets to consider
            Iterator itr = changesToProcess.iterator();
            while (itr.hasNext()) {
                event = (AssetChangeEvent)itr.next();
                if (event.assetRemovedEvent()) { 
                    removedAssets.add(event.getAsset()); //place here for later processing
                    continue; //don't add these assets to the lists of current assets!
                } 
                asset = (DefaultAssetTechSpec) event.getAsset();
                if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership looking at asset " + asset.getName() + "["+asset.getAssetType()+"]");
                if (asset.getAssetType().equals(AssetType.HOST)) { hosts.add(asset); }
                else if (asset.getAssetType().equals(AssetType.NODE)) { nodes.add(asset); }
                else if (asset.getAssetType().equals(AssetType.AGENT)) { agents.add(asset); }
            }
            
            
            Vector changedModels = new Vector(); //keep track so we can publish change them to the BB.

            if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership called with " + changesToProcess.size() + " events");

            //Iterate over threatDescriptions
            if ( (threatDescriptions != null && threatDescriptions.size() > 0) ) {

//if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership has " + threatDescriptions.size() + " threatDescriptions to examine.");

                Iterator i = threatDescriptions.iterator();
                while (i.hasNext()) {

                    metaModel = (ThreatDescription)i.next();
//if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership looking at threat " + metaModel.getName() );

                        
                        Iterator ctp = changesToProcess.iterator();
                        while (ctp.hasNext()) {

                            threatModel = null;
                            event = (AssetChangeEvent)ctp.next();
                            asset = (DefaultAssetTechSpec) event.getAsset();

                            if (event.moveEvent() || event.newAssetEvent() ) {
                                //IF the agent moved or is new
                                //check to see if the asset qualifies

                                //** First check type of asset against type of threatModel. Ignore if they don't match
                                if (!(metaModel.getAffectedAssetType().equals(asset.getAssetType()))) {
//if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership -- asset types don't match: \n modelAssetType="+metaModel.getAffectedAssetType()+"\nasset's type["+asset.getName()+"] = "+asset.getAssetType());
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
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! But already exists in the "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
                                    } else {
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! Adding to the "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"], memberCount="+threatModel.getAssetList().size());
                                    }
                                } else { //remove the asset from the threat model's membership, if it's there                         
//if (logger.isDebugEnabled()) logger.debug("evaluateThreatAssetMembership - doesn't qualify.");
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                        
                                    if (threatModel != null) { // then the asset WAS a member of the threat & now it isn't
                                        removeAssetsChildrenFromTransitiveEffects(asset, threatModel);
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
                                    }
                                }
                                
                              //If the asset was removed, just remove from this threat, if there
                            } else if (event.assetRemovedEvent() ) {
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                       
                                    if (threatModel != null) {
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getName()+" threatModel ["+metaModel.getAffectedAssetType().getName()+"]");
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
                            if (logger.isDebugEnabled()) logger.debug("Announced "+added.size()+" assets were added, and " +removed.size()+" assets were removed in threat = " + dtm.getName());
                        }                        
                    }

                    //If any assets were removed from the enclave, further process by now removing this asset from any transitive effects 
                    //it was a member of.
                    for (Iterator it2 = removedAssets.iterator(); it2.hasNext(); ) {
                        asset = (DefaultAssetTechSpec )it2.next();
                        if (asset != null && (asset.getName() != null) && asset.getAssetType().getName() != null) {
                            if (logger.isDebugEnabled()) logger.debug("...Trying to remove "+asset.getName()+"["+asset.getAssetType().getName()+"] from all transitive effects.");
                            removeAssetFromAllTransitiveEffects(asset);
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
            if (logger.isDebugEnabled()) logger.debug("========================================== 11 ==> processEnclaveTransitiveEffects - no assets to process");
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
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating host transitive effect model!");
                    tem = hostTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = hosts.iterator();
                while (it.hasNext()) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 8 ==> Adding host to transitive effect model!");
                    tem.addAsset((AssetTechSpecInterface)it.next());
                }        
            }
                
            //Add nodes to transEffectModel
            if (nodeTransEffect != null && nodes.size() > 0) {
                TransitiveEffectModel tem = nodeTransEffect.getInstantiation();
                if (tem == null) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating node transitive effect model!");
                    tem = nodeTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = nodes.iterator();
                while (it.hasNext()) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 8 ==> Adding node to transitive effect model!");
                    tem.addAsset((AssetTechSpecInterface)it.next());
                }        
            }

            //Add agents to transEffectModel
            if (agentTransEffect != null && agents.size() > 0) {
                TransitiveEffectModel tem = agentTransEffect.getInstantiation();
                if (tem == null) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating agent transitive effect model!");
                    tem = agentTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                        
                Iterator it = agents.iterator();
                while (it.hasNext()) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 8 ==> Adding agent to transitive effect model!");
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
            if (logger.isDebugEnabled()) logger.debug("========================================== 11 ==> processHostTransitiveEffects - no assets to process");
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
                
                if (agentTransEffect != null && !(agentTransEffect.getTransitiveAssetType().equals(AssetType.AGENT))) { //then there must be an error -- node trans effects can only contain agents
                    logger.warn("Found a transEffect beneath a node trans effect (on event["+nodeTransEffect.getTransitiveEvent().getName()+"])that was not on an agent container! IGNORING IT. It was: "+agentTransEffect.getTransitiveAssetType() );
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
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating node transitive effect model!");
                    node_tem = nodeTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(node_tem);
                }

                Iterator it = nodes.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface node = (AssetTechSpecInterface)it.next();
                    if (dtm.containsAsset(node.getHost()) ) { // then this node should be added to the transitive effect
                        if (logger.isDebugEnabled()) logger.debug("========================================== 9 ==> Adding node["+node.getName()+"] to transitive effect model!");
                        node_tem.addAsset(node);
                    } else {
//                        if (logger.isDebugEnabled()) logger.debug("========================================== 9.5 ==> Node not added to transitive effect model! Host Threat="+dtm.getName()+" did not contain this node's["+node.getName()+"] host="+node.getHost().getName());
                        if (logger.isDebugEnabled()) logger.debug("############################################ 9.5 ==> Node not added to transitive effect model! Host Threat="+dtm.getName());
                        if (logger.isDebugEnabled()) logger.debug("############################################ 9.5 did not contain this node's["+node.getName()+"] host");
                        if (logger.isDebugEnabled()) logger.debug("############################################ 9.5  host="+node.getHost().getName());
                    }
                }   
            }
            
            //Now, search the agent assets, if an agentTransEffect exists. Add an agent if its host is a member of the current threat, or node is a member of the nodeTransEffect.
            if (agentTransEffect != null) { //get its instantiation
                agent_tem = agentTransEffect.getInstantiation();
                if (agent_tem == null) {
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating a child agent transitive effect model!");
                    agent_tem = agentTransEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(agent_tem);
                }
                
                Iterator it = agents.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface agent = (AssetTechSpecInterface)it.next();
                    //Add this agent ONLY if its node is in the parent (node) transEffect. If no node transEffect
                    //exists, then add only if its host is in the threat.
                    if (node_tem != null && node_tem.containsAsset(agent.getNode()) ) { 
                        if (logger.isDebugEnabled()) logger.debug("========================================== 9.1 ==> Adding agent["+agent.getName()+"] to transitive effect model! Agent's Node is in parent transEffect");
                        agent_tem.addAsset(agent);
                    } else if (dtm.containsAsset(agent.getHost()) ) { // then this node should be added to the transitive effect
                        if (logger.isDebugEnabled()) logger.debug("========================================== 9.1 ==> Adding agent["+agent.getName()+"] to transitive effect model! Agent's host is in threat");
                        agent_tem.addAsset(agent);
                    } else {
                        if (logger.isDebugEnabled()) logger.debug("========================================== 9.5 ==> Agent not added to transitive effect model! Host Threat="+dtm.getName()+" did not contain this agent's["+agent.getName()+"] host="+agent.getHost().getName());
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
            if (logger.isDebugEnabled()) logger.debug("========================================== 11 ==> processNodeTransitiveEffects - no assets to process");
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
                    if (logger.isDebugEnabled()) logger.debug("========================================== 7 ==> Creating a child agent transitive effect model!");
                    tem = transEffect.instantiate(us.nextUID());
                    this.blackboard.publishAdd(tem);
                }
                
                Iterator it = agents.iterator();
                while (it.hasNext()) {                
                    AssetTechSpecInterface agent = (AssetTechSpecInterface)it.next();
                    if (dtm.containsAsset(agent.getNode()) ) { // then this agent should be added to the transitive effect
                        if (logger.isDebugEnabled()) logger.debug("========================================== 10 ==> Adding agent["+agent.getName()+"] to transitive effect model!");
                        tem.addAsset(agent);
                    } else {
                        if (logger.isDebugEnabled()) logger.debug("========================================== 10.5 ==> Agent not added to transitive effect model! Node Threat="+dtm.getName()+" did not contain this agent's node="+agent.getNode().getName());
                    }
                }   
            }
        }      
    }

    /**
     *
     * Call to remove an asset from ALL transitive effects, e.g., when the asset is removed from the enclave.
     *<p>
     * In this case we will remove the asset from all transitive effects. The method does NOT remove the asset's children
     * from subsequent child transitive effects however. This is because the society model should inform us itself that 
     * each child has been removed. E.g. we should hear that if a node is removed, that each of the node's agents have also
     * been removed.
     */
    private void removeAssetFromAllTransitiveEffects(AssetTechSpecInterface atsi) {
     
        ThreatDescription metaModel;        

        Enumeration keys = transEffects.keys();
        while (keys.hasMoreElements()) {
            
            TransitiveEffectDescription ted = (TransitiveEffectDescription)transEffects.get(keys.nextElement());
            if (ted != null && ted.getInstantiation() != null && ted.getTransitiveAssetType().equals(atsi.getAssetType()) ) {                
                if (ted.getInstantiation().removeAsset(atsi) ) {
                    if (logger.isDebugEnabled()) logger.debug("Removed "+atsi.getAssetID()+" from transitive effect for event["+ted.getTransitiveEventName()+"].");
                }                
            }
        }
    }

    /**
     *  Remove an asset from a transitive effect.
     * <p>
     *  When an asset is removed from a threat, we must remove all assets "contained" by it in any related
     *  transitive effects. E.g. if it's a host that is removed, then we have to remove any nodes or agents;
     *  if it is a node, then we have to remove any agents.  The complex part of this is that since multiple 
     *  threats can cause the same event, we first need to make sure that the original (aka parent) asset 
     *  does not belong to multiple threats that cause the same event (if it did, then it's contained assets
     *  can remain in the respective transitive effects.
     * <p>
     *  The current version of this method will NOT work when assets can belong to multiple containers, i.e.,
     *  more than just hosts containing nodes, and nodes containing agents; when a computer room can contain a
     *  node/agent.
     * <p>
     *  Assumption: Only hosts and nodes can be removed. If an agent, there are no possible transitive effects;
     *  if the enclave, then nothing will exist.
     *
     *@param atsi The asset removed from the threat
     *@param dtm The threat the asset was removed from
     */
    private void removeAssetsChildrenFromTransitiveEffects(AssetTechSpecInterface atsi, DefaultThreatModel dtm) {
        
        ThreatDescription metaModel;        
        TransitiveEffectDescription  nodeTransEffect = null;
        TransitiveEffectDescription  agentTransEffect = null;
        TransitiveEffectDescription  transEffect = null;

        AssetType affectedAssetType = dtm.getAssetType();
        
        //Get the event that the threat caused
        EventDescription causedEvent = dtm.getThreatDescription().getEventThreatCauses();    
        
        //First, make sure there is a transitive effect to consider.
        if (causedEvent.getTransitiveEffect() == null) { return; }
        
        //First, check to be sure that this asset isn't a member of another threat that causes the same event.
        Iterator i = threatDescriptions.iterator();
        while (i.hasNext()) {
            
            metaModel = (ThreatDescription)i.next();
            //to be considered, the metaModel must match asset type & cause the same event as the threat that the asset was removed from
            if ( !(metaModel.getAffectedAssetType().equals(affectedAssetType) &&  
                  (metaModel.getEventThreatCauses().equals(causedEvent) ) ) ) {
                continue; 
            }

            //OK, we have a possibility. If this threat contains the asset then just return without doing anything.
            DefaultThreatModel threat = metaModel.getInstantiation();
            //Look only for HOST threats
            if (threat == null ) { continue; } // this threat has no members
            
            if (threat.containsAsset(atsi)) { //we found another threat with this asset, so do nothing
                return;
            }
        }
         
        //OK, no other threats contain this asset. We need to inspect all transitive effects & remove its children
        //from them.
        
            
        transEffect = causedEvent.getTransitiveEffect();
        TransitiveEffectModel tem = transEffect.getInstantiation();
        if (tem == null) {
            return; // this shouldn't occur, but we need to check for this.
        }

            
        if (affectedAssetType.equals(AssetType.HOST)) { // then we have a potential indirect transEffect to look at
                                                        //A host threat could have both node and agent trans effects
            
            Vector removedNodes = new Vector();

            //Now, we need to see what assets the transEffect contains
            Vector nodes = tem.getAssetList();
            
            //See if any of these have the asset as a host
            i = nodes.iterator();
            while (i.hasNext()) {
             
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                if (asset.getHost().equals(atsi)) {
                    removedNodes.add(asset); //found a node, add to list to remove
                }                
            }            

            //Now remove the nodes
            i = removedNodes.iterator();
            while (i.hasNext()) {
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                tem.removeAsset(asset); 
                if (logger.isDebugEnabled()) logger.debug("**Removed asset["+asset.getAssetID()+"] from transitiveEffect["+tem.getName()+"].");
            }   
            
            //----------------------------------------------------------
            //Now, see if there is a child transitive effect (on agents)
            EventDescription childEvent = transEffect.getTransitiveEvent();
            if (childEvent == null) { // then we'return done
                return;
            }
            
            TransitiveEffectDescription ted = childEvent.getTransitiveEffect();
            TransitiveEffectModel childTEM = ted.getInstantiation();
            if (childTEM == null) {
                return; // this shouldn't occur, but we need to check for this.
            }
        
            //Now, we need to see what assets the transEffect contains
            Vector agents = childTEM.getAssetList();
            Vector removedAgents = new Vector();
            
            //See if any of these have the asset as a host
            i = agents.iterator();
            while (i.hasNext()) {
             
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                if (asset.getHost().equals(atsi)) {
                    removedAgents.add(asset); //found an agent, add to list to remove
                }                
            }            

            //Now remove the agents
            i = removedAgents.iterator();
            while (i.hasNext()) {
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                childTEM.removeAsset(asset); 
                if (logger.isDebugEnabled()) logger.debug("**Removed asset["+asset.getAssetID()+"] from transitiveEffect["+tem.getName()+"].");
            }   
            
        

        
        } else if (affectedAssetType.equals(AssetType.NODE)) { // then we have no potential indirect transEffect to look at
                                                               //A node threat could only have an agent trans effect
            
            Vector removedAgents = new Vector();

            //Now, we need to see what AGENT assets the transEffect contains
            Vector agents = tem.getAssetList();
            
            //See if any of these have the asset as a host
            i = agents.iterator();
            while (i.hasNext()) {
             
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                if (asset.getNode().equals(atsi)) {
                    removedAgents.add(asset); //found an agent, add to list to remove
                }                
            }            

            //Now remove the agents
            i = removedAgents.iterator();
            while (i.hasNext()) {
                AssetTechSpecInterface asset = (AssetTechSpecInterface)i.next();
                tem.removeAsset(asset); 
                if (logger.isDebugEnabled()) logger.debug("**Removed asset["+asset.getAssetID()+"] from transitiveEffect["+tem.getName()+"].");
            }            

        
        }
        
        
        return;
            
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
            if (logger.isDebugEnabled()) logger.debug("========================================== 6 ==> Creating threat model!");
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

        techSpecsLoadedSub =
        (IncrementalSubscription)blackboard.subscribe(techSpecsLoadedPredicate);
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

        //Now, see if the tech specs have been loaded...
        if (!TECHSPECS_ARE_LOADED ) { // *** This should only execute once.
            Collection temp = techSpecsLoadedSub.getAddedCollection();
            Iterator it = temp.iterator(); 
            if (it.hasNext()) { //then the TechSpecsLoadedCondition object was seen on the BB.
                TECHSPECS_ARE_LOADED = true;
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
            if (logger.isDebugEnabled()) logger.debug("Got " + threatDescriptions.size() + " ThreatDescriptions from the blackboard.");
            
            //With new threats comes new transitive effects, so generate the list of all of them (for later use)
            regenerateTransitiveEffectsList();
        }
        
        //Get the asset manager plugin & add a change listener
        if (!ADDED_ASSET_LISTENER ) { 
            Collection assetMgrs = assetManagerSub.getAddedCollection();
            Iterator it = assetMgrs.iterator(); 
            if (it.hasNext()) { 
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
        }
        
        if (TECHSPECS_ARE_LOADED) { //don't process threat memberships until the tech specs have been loaded.
            evaluateThreatAssetMembership();            
        }        
    }
    
    
    /**
     * Runs thru the list of all threats & extracts out all transitive effects up to three layers down. <b>This may need to change
     * once/if the containment model changes.</b>
     */
    private void regenerateTransitiveEffectsList() {
        
        TransitiveEffectDescription  transEffect;
        TransitiveEffectDescription  childTE;
        TransitiveEffectDescription  grandchildTE;

        ThreatDescription metaModel;
       
        //First get all transitive effects
        Iterator i = threatDescriptions.iterator();
        while (i.hasNext()) {
            
            metaModel = (ThreatDescription)i.next();
            transEffect = metaModel.getEventThreatCauses().getTransitiveEffect();
            if (transEffect == null ) { continue; } // no transitive effect to process
            transEffects.put(transEffect.getUID(), transEffect); 

            childTE = transEffect.getTransitiveEvent().getTransitiveEffect(); //this could be a node or agent TE
            if (childTE != null ) { //see if there is a grandchild TE
                transEffects.put(childTE.getUID(), childTE);                 
                grandchildTE = childTE.getTransitiveEvent().getTransitiveEffect(); // this could only be an agent TE
                if (grandchildTE != null ) { // then it must be agent typed
                    transEffects.put(grandchildTE.getUID(), grandchildTE);           
                }
            }
            
        }
        logger.info("Found "+transEffects.size()+" transitive effects associated with threats on the BB.");
        
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

    private IncrementalSubscription techSpecsLoadedSub;
    private UnaryPredicate techSpecsLoadedPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof TechSpecsLoadedCondition);
        }
    };
    
}
