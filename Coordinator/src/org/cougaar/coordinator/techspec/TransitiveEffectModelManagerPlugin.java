/*
 * TransitiveEffectModelManagerPlugin.java
 *
 * Created on May 6, 2004, 3:44 PM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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

//org.cougaar.coordinator.techspec.TransitiveEffectModelManagerPlugin
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
public class TransitiveEffectModelManagerPlugin extends ComponentPlugin {
    
    boolean haveServices = false;
    private LoggingService logger;
    private UIDService us = null;

    /** The file param passed in via the plugin */
    private String fileParam  = null;

    /** Set to true when the asset listener has been added */
    private boolean ADDED_ASSET_LISTENER = false;

    /** The collection of transitiveEffectDescriptions */
    private Collection transitiveEffectDescriptions;

    /** List of all assets */
    private Vector assets;
    
    /** Creates a new instance of TransitiveEffectModelManagerPlugin 
     *
     */
    public TransitiveEffectModelManagerPlugin() {
    
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
    private void evaluateAssetMembership() {

        synchronized(changesToProcess) {
            //If no new assets or new threats, don't do anything
            if (changesToProcess.size() == 0 ) { return; }

            TransitiveEffectDescription transitiveEffectDesc;
            AssetChangeEvent event;
            TransitiveEffectModel transitiveEffectModel;
            DefaultAssetTechSpec asset;
            
            Vector changedModels = new Vector(); //keep track so we can publish change them to the BB.

            if (logger.isDebugEnabled()) logger.debug("evaluateAssetMembership called with " + changesToProcess.size() + " events");

            //Iterate over transitiveEffectDescriptions
            if ( (transitiveEffectDescriptions != null && transitiveEffectDescriptions.size() > 0) ) {


                Iterator i = transitiveEffectDescriptions.iterator();
                while (i.hasNext()) {

                    transitiveEffectDesc = (TransitiveEffectDescription)i.next();

                        Iterator ctp = changesToProcess.iterator();
                        while (ctp.hasNext()) {

                            transitiveEffectModel = null;
                            event = (AssetChangeEvent)ctp.next();
                            asset = (DefaultAssetTechSpec) event.getAsset();
                            
                            if (event.moveEvent() || event.newAssetEvent() ) {
                                //IF the agent moved or is new
                                //check to see if the asset qualifies

                                //** First check type of asset against type of transitiveEffectModel. Ignore if they don't match
                                if (!(transitiveEffectDesc.getTransitiveAssetType().equals(asset.getAssetType()))) {
                                    continue;
                                }

                                boolean qualifies = true; //assume the asset qualifies, until a filter rejects it.

                                TransitiveEffectVulnerabilityFilter filter = transitiveEffectDesc.getTransitiveVulnerabilityFilter();

                                //IF NO FILTER -- add the asset as a member
                                if (filter == null) {

                                    qualifies = true;

                                } else { //OK, there are FILTERS, so see if the asset qualifies

                                    qualifies = filter.qualifies(this, asset);
                                    
                                }

                                if (qualifies) {
                                    if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] Qualifies! Adding to the transitive effect for event: "+transitiveEffectDesc.getTransitiveEventName()+" transitiveEffectModel ["+transitiveEffectDesc.getTransitiveAssetType().getName()+"]");
                                    transitiveEffectModel = addAssetAsMember(asset, transitiveEffectDesc);
                                } else { //remove the asset from the threat model's membership, if it's there                         
                                    transitiveEffectModel = removeAssetAsMember(asset, transitiveEffectDesc);                                                        
                                    if (transitiveEffectModel != null) {
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from the transitive effect for event: "+transitiveEffectDesc.getTransitiveEventName()+" transitiveEffectModel ["+transitiveEffectDesc.getTransitiveAssetType().getName()+"]");
                                    }
                                }
                                
                              //If the asset was removed, just remove from this threat, if there
                            } else if (event.assetRemovedEvent() ) {
                                    transitiveEffectModel = removeAssetAsMember(asset, transitiveEffectDesc);                                                       
                                    if (transitiveEffectModel != null) {
                                        if (logger.isDebugEnabled()) logger.debug("==> "+asset.getName()+"["+asset.getAssetType().getName()+"] was REMOVED from "+transitiveEffectDesc.getTransitiveEventName()+" transitiveEffectModel ["+transitiveEffectDesc.getTransitiveAssetType().getName()+"]");
                                    }
                            }

                            //Now if the transitiveEffectModel != null, then it was changed so add it to the list to publishChange
                            if (transitiveEffectModel != null) {
                                if (! changedModels.contains(transitiveEffectModel)) {
                                    changedModels.addElement(transitiveEffectModel);
                                }
                            }
                            
                        } //while loop - on asset change events
                } //while() loop on ThreatDescriptions iterator

                if (changedModels.size() > 0) { 

                    Iterator cModels = changedModels.iterator();
                    while (cModels.hasNext()) {
                        TransitiveEffectModel dtm = (TransitiveEffectModel) cModels.next();
                        Collection changes = Collections.singleton( new TransitiveEffectModelChangeEvent( dtm, TransitiveEffectModelChangeEvent.MEMBERSHIP_CHANGE ) );
                        this.blackboard.publishChange(dtm, changes);
                    }
                }

            } //if - there are any threat models

            //Now remove all processed events
            changesToProcess.removeAllElements();
        } //synchronized
    }
        
    /**
     * Adds an asset to a transitiveEffectModel, creating the model if nec.
     * @return the TransitiveEffectModel, if modified. It will not be modified if the
     * asset was already a member. ONLY call from an open Transaction.
     */
    private TransitiveEffectModel addAssetAsMember(AssetTechSpecInterface asset, TransitiveEffectDescription transitiveEffectDesc) {
        
        boolean createdModel = false;
        TransitiveEffectModel dtm = transitiveEffectDesc.getInstantiation();
        if (dtm == null) {
            //if (logger.isDebugEnabled()) logger.debug("========================================== 6 ==> Creating threat model!");
            dtm = transitiveEffectDesc.instantiate(us.nextUID());
            
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
     * @return the TransitiveEffectModel, if modified. It will not be modified if the
     * asset was not a member.
     */
    private TransitiveEffectModel removeAssetAsMember(AssetTechSpecInterface asset, TransitiveEffectDescription transitiveEffectDesc) {
        
        TransitiveEffectModel dtm = transitiveEffectDesc.getInstantiation();
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

        transitiveEffectDescriptionsSub =
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

        //Update the transitiveEffectDescriptions collection if new ones were added
        if (transitiveEffectDescriptionsSub.getAddedCollection().size() > 0) {
            if (transitiveEffectDescriptions != null && transitiveEffectDescriptions.size() > 0) {
                logger.warn("Read in threat descriptions on subsequent execute() invocations. They should have been all seen at once!");
                //Should not happen. If it did, the assets already seen would not be compared against these threats until
                //they changed in some way. Since the threat descriptions are published in a setupSubscription, this should not occur.
            }
            transitiveEffectDescriptions = transitiveEffectDescriptionsSub.getCollection();
            if (logger.isDebugEnabled()) logger.debug("Got " + transitiveEffectDescriptions.size() + " ThreatDescriptions from the blackboard.");
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
        
        evaluateAssetMembership();            
            
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
    

    private IncrementalSubscription transitiveEffectDescriptionsSub;
    private UnaryPredicate threatDescriptionsPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return (o instanceof TransitiveEffectDescription);
        }
    };
    
}
