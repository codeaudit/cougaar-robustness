/*
 * ActionMonitorPlugin.java
 *
 * Created on February 9, 2004, 1:55 PM
 * <copyright>
 *  Copyright 2003 Object Services & Consulting, Inc.
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

package org.cougaar.coordinator.test.coordination;

import org.cougaar.coordinator.*;

import java.util.Iterator;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.core.persist.NotPersistable;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;

import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.AgentIdentificationService;


/**
 * This class watches the actions being published and relayed, and reports when the
 * values change. Place this in the node to see how the actions are relayed.
 */
public class ActionMonitorPlugin extends ComponentPlugin
implements NotPersistable {
    
    private ActionMonitorServlet servlet = null;
    private IncrementalSubscription servletSubscription;
    private IncrementalSubscription actionsSubscription;
    private Logger logger = null;
   
    /** Create a new ActionMonitorPlugin instance */
    public ActionMonitorPlugin() {
    }
    
    /** load method */
    public void load() {
        super.load();

        logger =
        (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);        
    }
    
    /** Set up needed subscriptions */
    public void setupSubscriptions() {
        
        
        //logger.debug("setupSubscriptions called.");
        
        //Listen for changes in out defense mode object
        actionsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof Action ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        //logger.debug("Listening for Actions");
        
        servletSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionMonitorServlet ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
    }
    
    
    /**
     * Watch for new actions & changes in the actions.
     */
    public void execute() {
        
        Iterator iter;
       
        //********* Check for the servlet being added ***********
        if (servlet == null) {
            iter = servletSubscription.getAddedCollection().iterator();
            if (iter.hasNext()) {
                servlet = (ActionMonitorServlet)iter.next();
                logger.debug("**** Saw new ActionMonitorServlet");
            }
        }
        
        //********* Check for actions being added ***********
        iter = actionsSubscription.getAddedCollection().iterator();
        while (iter.hasNext()) {
            Action a = (Action)iter.next();
            if (servlet != null) { servlet.addAction(a); }
            //At least temp for testing -- next 2 lines
            String target = ((MessageAddress) a.getTargets().iterator().next()).toString();
            logger.debug("[AgentId="+agentId+"]**** Saw new Action["+ActionUtils.getAssetID(a)+"], with ActionRecord = " + a.getValue() + " UID=" + a.getUID()+" src="+a.getSource()+",tgt="+target);
        }
        
        //********* Check for changes in our modes ************        
        iter = actionsSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            Action a = (Action)iter.next();
            if (servlet != null) { servlet.changedAction(a); }
            logger.debug("[AgentId="+agentId+"]**** Saw changed Action["+ActionUtils.getAssetID(a)+"], with ActionRecord = " + a.getValue() + " UID=" + a.getUID());
        }

        //Emit # of action wrappers on BB
        int size = actionsSubscription.getCollection().size();
        logger.debug("[AgentId="+agentId+"]**** Total # of Action objects on BB right now = "+size);
        
    }
    
    
    
}

