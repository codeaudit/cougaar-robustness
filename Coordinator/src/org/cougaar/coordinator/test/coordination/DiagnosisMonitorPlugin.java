/*
 * DiagnosisMonitorPlugin.java
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
 * This class watches the diagnoses being published and relayed, and reports when the
 * values change. Place this in the node to see how the diagnoses are relayed.
 */
public class DiagnosisMonitorPlugin extends ComponentPlugin
implements NotPersistable {
    
    private DiagnosisMonitorServlet servlet = null;
    private IncrementalSubscription servletSubscription;
    private IncrementalSubscription diagnosesSubscription;
    private LoggingService logger = null;
   
    /** Create a new DiagnosisMonitorPlugin instance */
    public DiagnosisMonitorPlugin() {
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
        diagnosesSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof Diagnosis ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        //logger.debug("Listening for Diagnoses");


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
     * Watch for new diagnoses & changes in the diagnoses.
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
        
        //********* Check for diagnoses being added ***********
        iter = diagnosesSubscription.getAddedCollection().iterator();
        if (iter == null) logger.debug("****nothing added to the collection...");
        while (iter.hasNext()) {
            Diagnosis a = (Diagnosis)iter.next();
            if (servlet != null) { servlet.addDiagnosis(a); }
            logger.debug("**** Saw new Diagnosis["+DiagnosisUtils.getAssetID(a)+"], with value = " + a.getValue() + " UID=" + a.getUID());
        }
        
        //********* Check for changes in our modes ************
        
        //We have one defense mode, so we only get the one from iter.next();
        iter = diagnosesSubscription.getChangedCollection().iterator();
        if (iter == null) logger.debug("****nothing changed in the collection...");
        while (iter.hasNext()) {
            Diagnosis a = (Diagnosis)iter.next();
            if (servlet != null) { servlet.changedDiagnosis(a); }
            logger.debug("**** Saw changed Diagnosis["+DiagnosisUtils.getAssetID(a)+"], with value = " + a.getValue() + " UID=" + a.getUID());
        }
        
    }
    
    
    
}


