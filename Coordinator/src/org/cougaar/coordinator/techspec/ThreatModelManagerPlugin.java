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
 * MetaThreatModels are read in via an XML file. This filename MUST be provided as a
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

    /** Container for the imported metaThreatModels */
    private Vector metaThreatModels = null;

    /** Set to true when the asset listener has been added */
    private boolean ADDED_ASSET_LISTENER = false;
    
    /** Creates a new instance of ThreatModelManagerPlugin 
     *
     * The plugin MUST include a filename for the XML file containing the MetaThreatModel definitions
     *
     */
    public ThreatModelManagerPlugin() {
    
    }
    
    private Vector changesToProcess = new Vector(20,20);
    
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
            if (changesToProcess.size() == 0) { return; }

            MetaThreatModel metaModel;
            AssetChangeEvent event;
            AssetTechSpecInterface asset;
            DefaultThreatModel threatModel;

            Vector changedModels = new Vector(); //keep track so we can publish change them to the BB.

            logger.debug("evaluateThreatAssetMembership called with " + changesToProcess.size() + " events");

            //Iterate over metaThreatModels
            if ( (metaThreatModels != null && metaThreatModels.size() > 0) ) {


                Iterator i = metaThreatModels.iterator();
                while (i.hasNext()) {

                    metaModel = (MetaThreatModel)i.next();

                        Iterator ctp = changesToProcess.iterator();
                        while (ctp.hasNext()) {

                            threatModel = null;
                            event = (AssetChangeEvent)ctp.next();
                            asset = event.getAsset();
                            
                            if (event.moveEvent() || event.newAssetEvent() ) {
                                //IF the agent moved or is new
                                //check to see if the asset qualifies

                                //** First check type of asset against type of threatModel. Ignore if they don't match
                                if (!(metaModel.getAssetType().equals(asset.getAssetType()))) {
                                    continue;
                                }

                                boolean qualifies = true; //assume the asset qualifies, until a filter rejects it.

                                ThreatMembershipFilter[] filters = metaModel.getFilters();

                                //NO FILTERS -- so add the asset as a member
                                if (filters == null || filters.length ==0 ) {

                                    qualifies = true;

                                } else { //OK, there are FILTERS, so see if the asset qualifies

                                    for (int k=0; k<filters.length; k++) {

                                        if (!filters[k].qualifies(asset)) {

                                            //Asset doesn't qualify, remove if nec.
                                            qualifies = false;
                                            break;

                                        }
                                    }
                                }

                                if (qualifies) {
                                    logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! Adding to the "+metaModel.getThreatName()+" threatModel ["+metaModel.getAssetType().getName()+"]");
                                    threatModel = addAssetAsMember(asset, metaModel);
                                } else { //remove the asset from the threat model's membership, if it's there                         
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                        
                                    if (threatModel != null) {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getThreatName()+" threatModel ["+metaModel.getAssetType().getName()+"]");
                                    }
                                }
                                
                              //If the asset was removed, just remove from this threat, if there
                            } else if (event.assetRemovedEvent() ) {
                                    threatModel = removeAssetAsMember(asset, metaModel);                                                       
                                    if (threatModel != null) {
                                        logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+metaModel.getThreatName()+" threatModel ["+metaModel.getAssetType().getName()+"]");
                                    }
                            }

                            //Now if the threatModel != null, then it was changed so add it to the list to publishChange
                            if (threatModel != null) {
                                if (! changedModels.contains(threatModel)) {
                                    changedModels.addElement(threatModel);
                                }
                            }
                            
                        } //while loop - on asset change events
                } //while() loop on metaThreatModels iterator

                if (changedModels.size() > 0) { 

                    Iterator cModels = changedModels.iterator();
                    while (cModels.hasNext()) {
                        DefaultThreatModel dtm = (DefaultThreatModel) cModels.next();
                        Collection changes = Collections.singleton( new ThreatModelChangeEvent( dtm, ThreatModelChangeEvent.MEMBERSHIP_CHANGE ) );
                        this.blackboard.publishChange(dtm, changes);
                    }
                }

            } //if - there are any threat models

            //Now remove all processed events
            changesToProcess.removeAllElements();
        } //synchronized
    }
        
    /**
     * Adds an asset to a threatModel, creating the model if nec.
     * @return the DefaultThreatModel, if modified. It will not be modified if the
     * asset was already a member. ONLY call from an open Transaction.
     */
    private DefaultThreatModel addAssetAsMember(AssetTechSpecInterface asset, MetaThreatModel metaModel) {
        
        boolean createdModel = false;
        DefaultThreatModel dtm = metaModel.getInstantiation();
        if (dtm == null) {
            //logger.debug("========================================== 6 ==> Creating threat model!");
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
    private DefaultThreatModel removeAssetAsMember(AssetTechSpecInterface asset, MetaThreatModel metaModel) {
        
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

        Collection assetMgrs = assetManagerSub.getAddedCollection();
        Iterator it = assetMgrs.iterator(); 
        if (it.hasNext() && !ADDED_ASSET_LISTENER ) { // *** This should only execute once.
            AssetManagerPlugin aMgr = (AssetManagerPlugin)it.next();
            logger.info("Found AssetManagerPlugin");
            
//////////pp2004 - now loaded in xml pkg            readThreatModels();
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
    
        
}
