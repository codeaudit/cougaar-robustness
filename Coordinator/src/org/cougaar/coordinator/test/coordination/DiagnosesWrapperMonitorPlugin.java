/*
 * DiagnosesWrapperMonitorPlugin.java
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
import org.cougaar.coordinator.techspec.*;
import org.cougaar.coordinator.test.coordination.*;

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
 * This class watches the diagnosesWrappers being published and relayed, and reports when the
 * values change. Place this in the node to see how the diagnosesWrappers are relayed.
 */
public class DiagnosesWrapperMonitorPlugin extends ComponentPlugin
implements NotPersistable {
    

    private DiagnosisMonitorServlet servlet = null;
    private IncrementalSubscription servletSubscription;
    private IncrementalSubscription diagnosesWrappersSubscription;
    private Logger logger = null;
   
    /** Create a new DiagnosesWrapperMonitorPlugin instance */
    public DiagnosesWrapperMonitorPlugin() {
    }
    
    /** load method */
    public void load() {
        super.load();

        logger =
        (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);        
    }
    
    /** Set up needed subscriptions */
    public void setupSubscriptions() {
        
        
        logger.debug("setupSubscriptions called.");
        
        //Listen for changes in out defense mode object
        diagnosesWrappersSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosesWrapper ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        logger.debug("Listening for DiagnosesWrappers");

        servletSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosisMonitorServlet ) {
                    return true ;
                }
                return false ;
            }
        }) ;

    }
    
    
    /**
     * Watch for new diagnosesWrappers & changes in the diagnosesWrappers.
     */
    public void execute() {
        
        Iterator iter;
        
        //********* Check for the servlet being added ***********
        if (servlet == null) {
            iter = servletSubscription.getAddedCollection().iterator();
            if (iter.hasNext()) {
                servlet = (DiagnosisMonitorServlet)iter.next();
                logger.debug("**** Saw new DiagnosisMonitorServlet");
            }
        }

        //********* Check for diagnosesWrappers being added ***********
        iter = diagnosesWrappersSubscription.getAddedCollection().iterator();
        if (iter == null) logger.debug("****nothing added to the collection...");
        while (iter.hasNext()) {
            DiagnosesWrapper a = (DiagnosesWrapper)iter.next();
            if (servlet != null) { servlet.addDiagnosesWrapper(a); }
            logger.debug("**** Saw new DiagnosesWrapper["+DiagnosisUtils.getExpandedName((Diagnosis)a.getContent())+"], with value = " + ((Diagnosis)a.getContent()).getValue());
        }
        
        //********* Check for changes in our modes ************
        
        //We have one defense mode, so we only get the one from iter.next();
        iter = diagnosesWrappersSubscription.getChangedCollection().iterator();
        if (iter == null) logger.debug("****nothing changed in the collection...");
        while (iter.hasNext()) {
            DiagnosesWrapper a = (DiagnosesWrapper)iter.next();
            if (servlet != null) { servlet.changedDiagnosesWrapper(a); }
            logger.debug("**** Saw changed DiagnosesWrapper["+DiagnosisUtils.getExpandedName((Diagnosis)a.getContent())+"], with value = " + ((Diagnosis)a.getContent()).getValue());
        }
        
    }
    
    
    
}


