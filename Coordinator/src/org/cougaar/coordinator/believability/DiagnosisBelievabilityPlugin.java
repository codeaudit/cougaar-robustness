/*
 * DiagnosisBelievabilityPlugin.java
 *
 * Created on July 8, 2003, 4:09 PM
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

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;

import java.util.Iterator;
import java.util.Collection;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.adaptivity.ServiceUserPluginBase;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.EventService;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;


/**
 * This Plugin is used to handle the believability functions for Defense Deconfliction
 * It emits StateEstimation objects.
 *
 */
public class DiagnosisBelievabilityPlugin extends ServiceUserPluginBase implements NotPersistable {
    
    
    private IncrementalSubscription defenseTechSpecsSubscription;
    private IncrementalSubscription threatModelSubscription;
    private IncrementalSubscription timedDefenseDiagnosesSubscription;
    private IncrementalSubscription pluginControlSubscription;
        
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };

    
    /** 
      * Creates a new instance of DiagnosisBelievabilityPlugin 
      */
    public DiagnosisBelievabilityPlugin() {
        super(requiredServices);
    }
    

    /**
      * Demonstrates how to read in parameters passed in via configuration files. Use/remove as needed. 
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
             logger.debug("Parameter = " + (String)iter.next());
        }
    }       

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams();
        haveServices();
    }
    
    
    /** Called every time this component is scheduled to run. Any time objects that belong to its
     *  subscription sets change (add/modify/remove), this will be called. This method is executed
     *  within the context of a blackboard transaction (so do NOT use transaction syntax inside it).
     *  You may only need to monitor one or two type of actions (e.g. additions), in which case you
     *  can safely remove the sections of code dealing with collection changes you are not interested
     *  in.
     */
    protected void execute() {

        //***************************************************DefenseTechSpecs
        //Handle the addition of new DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsa = (DefenseTechSpecInterface)iter.next();            
            //Process - e.g. determine if it is a DefenseTechSpecInterface you care about first...
        }

        //Handle the modification of DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsc = (DefenseTechSpecInterface)iter.next();
            //Process
        }
        
        //Handle the removal of DefenseTechSpecs
        for ( Iterator iter = defenseTechSpecsSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefenseTechSpecInterface dtsr = (DefenseTechSpecInterface)iter.next();
            //Process
        }

        //***************************************************TimedDefenseDiagnosis
        //Handle the addition of new TimedDefenseDiagnoses
        for ( Iterator iter = timedDefenseDiagnosesSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            TimedDefenseDiagnosis tdda = (TimedDefenseDiagnosis)iter.next();
            //Process
        }

        //Handle the modification of TimedDefenseDiagnoses
        for ( Iterator iter = timedDefenseDiagnosesSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            TimedDefenseDiagnosis tddc = (TimedDefenseDiagnosis)iter.next();
            //Process
        }
        
        //Handle the removal of TimedDefenseDiagnoses
        for ( Iterator iter = timedDefenseDiagnosesSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            TimedDefenseDiagnosis tddr = (TimedDefenseDiagnosis)iter.next();
            //Process
        }

        //***************************************************ThreatModelInterface
        //Handle the addition of new ThreatModels
        for ( Iterator iter = threatModelSubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tma = (ThreatModelInterface)iter.next();
            //Process
        }

        //Handle the modification of ThreatModels
        for ( Iterator iter = threatModelSubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tmc = (ThreatModelInterface)iter.next();
            //Process
        }
        
        //Handle the removal of ThreatModels
        for ( Iterator iter = threatModelSubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            ThreatModelInterface tmr = (ThreatModelInterface)iter.next();
            //Process
        }
        
        //******************************************************Publishing to the BB
        //Adding a timeDefenseDiagnosis to the BB
        StateEstimation se = new StateEstimation();
        publishAdd(se);
           
    }
    
    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

        defenseTechSpecsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseTechSpecInterface) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
        timedDefenseDiagnosesSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof TimedDefenseDiagnosis) {
                    return true ;
                }
                return false ;
            }
        }) ;

        threatModelSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ThreatModelInterface) {
                    return true ;
                }
                return false ;
            }
        }) ;

        //Not used at this time - Will be used to provide out-of-band control of this plugin
        pluginControlSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosisBelievabilityKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
    }

    /**
      * Called to ensure that the services we need get loaded.
      */
    private boolean haveServices() {
        if (eventService != null) return true;
        if (acquireServices()) {
            if (logger.isDebugEnabled()) logger.debug(".haveServices - acquiredServices.");
                ServiceBroker sb = getServiceBroker();

            eventService = (EventService ) 
            sb.getService( this, EventService.class, null ) ;      
            if (eventService == null) {
                throw new RuntimeException(
                    "Unable to obtain EventService");
            }

            return true;
        }
        else if (logger.isDebugEnabled()) logger.debug(".haveServices - did NOT acquire services.");
            return false;
    }
    

    // Helper methods to publish objects to the Blackboard
    public boolean publishAdd(Object o) {
        getBlackboardService().publishAdd(o);
        return true;
    }

    public boolean publishChange(Object o) {
	getBlackboardService().publishChange(o);
        return true;
    }

    public boolean publishRemove(Object o) {
	getBlackboardService().publishRemove(o);
        return true;
    }
    
    
    
}
