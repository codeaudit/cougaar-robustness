/*
 * DefensePolicyPlugin.java
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

package org.cougaar.coordinator.policy;


import org.cougaar.coordinator.*;

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
 * This Plugin is used to handle the defense policy functions for Defense Deconfliction
 * It emits CostBenefitDiagnosis objects.
 *
 */
public class DefensePolicyPlugin extends ServiceUserPluginBase implements NotPersistable {
    
    
    private IncrementalSubscription defensePolicySubscription;
    private IncrementalSubscription pluginControlSubscription;
    
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };

    
    /** 
      * Creates a new instance of DefensePolicyPlugin 
      */
    public DefensePolicyPlugin() {
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

        //***************************************************DefensePolicy
        //Handle the addition of new DefensePolicies
        for ( Iterator iter = defensePolicySubscription.getAddedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpa = (DefensePolicy)iter.next();
            //Process
        }

        //Handle the modification of DefensePolicies
        for ( Iterator iter = defensePolicySubscription.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpc = (DefensePolicy)iter.next();
            //Process
        }
        
        //Handle the removal of DefensePolicies
        for ( Iterator iter = defensePolicySubscription.getRemovedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            DefensePolicy dpr = (DefensePolicy)iter.next();
            //Process
        }

        //******************************************************Publishing to the BB
        //Adding a DefensePolicy to the BB
        DefensePolicy dp = new DefensePolicy();
        publishAdd(dp);
           
    }
    
    /** 
      * Called from outside once after initialization, as a "pre-execute()". This method sets up the 
      * subscriptions to objects that we'return interested in. In this case, defense tech specs and
      * defense conditions.
      */
    protected void setupSubscriptions() {

        defensePolicySubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefensePolicy) {
                    return true ;
                }
                return false ;
            }
        }) ;

        //Not used at this time - Will be used to provide out-of-band control of this plugin
        pluginControlSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefensePolicyKnob) {
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
