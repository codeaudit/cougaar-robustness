/*
 * ActionsWrapperMonitorPlugin.java
 *
 * Created on February 9, 2004, 1:55 PM
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

import java.util.Collection;


/**
 * This class watches the actionWrappers being published and relayed, and reports when the
 * values change. Place this in the node to see how the actionWrappers are relayed.
 */
public class ActionsWrapperMonitorPlugin extends ComponentPlugin
implements NotPersistable {
    
    private ActionMonitorServlet servlet = null;
    private IncrementalSubscription servletSubscription;
    private IncrementalSubscription actionWrappersSubscription;
    private LoggingService logger = null;
   
    /** Create a new ActionsWrapperMonitorPlugin instance */
    public ActionsWrapperMonitorPlugin() {
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
        actionWrappersSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionsWrapper ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        //logger.debug("Listening for ActionsWrappers");


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
     * Watch for new actionWrappers & changes in the actionWrappers.
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

        //********* Check for actionWrappers being added ***********
        iter = actionWrappersSubscription.getAddedCollection().iterator();
        if (iter == null) logger.debug("****nothing added to the collection...");
        while (iter.hasNext()) {
            ActionsWrapper a = (ActionsWrapper)iter.next();
            if (servlet != null) { servlet.addActionsWrapper(a); }
            
            //String target = "null";
            //Iterator it = null;
            //Collection c = a.getTargets();
            //if (c != null) it = a.getTargets().iterator();
            //if (it != null && it.hasNext()) target = ((MessageAddress) it.next()).toString();
            logger.debug("[AgentId="+agentId+"]**** Saw new ActionsWrapper["+ActionUtils.getAssetID((Action)a.getContent())+"], with ActionRecord = " + ((Action)a.getContent()).getValue()+" src="+a.getSource()+",tgt="+a.getTargets());
            logger.debug("ActionsWrapper="+a);
        }
        
        //********* Check for changes in our modes ************
        iter = actionWrappersSubscription.getChangedCollection().iterator();
        if (iter == null) logger.debug("****nothing changed in the collection...");
        while (iter.hasNext()) {
            ActionsWrapper a = (ActionsWrapper)iter.next();
            if (servlet != null) { servlet.changedActionsWrapper(a); }
            logger.debug("**** Saw changed ActionsWrapper["+ActionUtils.getAssetID((Action)a.getContent())+"], with ActionRecord = " + ((Action)a.getContent()).getValue());
            logger.debug("ActionsWrapper="+a);
        }

        //Emit # of action wrappers on BB
        int size = actionWrappersSubscription.getCollection().size();
        logger.debug("[AgentId="+agentId+"]**** Total # of Action WRAPPER objects on BB right now = "+size);
        
    }
    
    
    
}

