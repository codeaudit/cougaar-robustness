/*
 * ActionRelayManager.java
 *
 * Created on January 21, 2004, 2:28 PM
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator;

import java.util.Iterator;
import java.util.Collection;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;

import java.util.Vector;
import java.util.Iterator;

/**
 * This Plugin is used to control relaying Action objects to the Coordinator
 *
 */
public class ActionRelayManager extends MinimalPluginBase implements NotPersistable {
    
    private IncrementalSubscription actionsSubscription;
    private IncrementalSubscription knobSubscription;
    private IncrementalSubscription managerAddressSubscription;
    private IncrementalSubscription wrapperSubscription;
    
    private boolean shouldRelayActions = true;
    private MessageAddress managerAddress = null;
    
    private Vector relayFilters = null;
    
    private ActionRelayManagerKnob ActionRelayManagerKnob;

    private Vector newWrappers = null;
    
    /** 
      * Creates a new instance of DefenseActivation 
      */
    public ActionRelayManager() {
        super();
        relayFilters = new Vector();
        newWrappers = new Vector();
    }
    
    /**
     * Add a filter to control whether a Action is relayed
     */
    public void addFilter(ActionRelayFilter drf) {
        relayFilters.add(drf);
    }

    /**
     * Remove a filter.
     * @return TRUE if the element was found & removed.
     */
    public boolean removeFilter(ActionRelayFilter drf) {
        return relayFilters.remove(drf);
    }
    

    /**
     * Run the relay filters to determine if a Action should be relayed now
     * @return TRUE if the Action should be relayed.
     */
    private boolean runFilters(ActionsWrapper dw) {
        Iterator iter = relayFilters.iterator();
        while (iter.hasNext()) {
            ActionRelayFilter drf = (ActionRelayFilter)iter.next();
            FilterValue result = drf.filterAction(dw);
            if (result != FilterValue.DONT_CARE) {
                return (result == FilterValue.DO_RELAY) ;
            }
        }
        return true; // relay if no filter says not to
    }
    
    
    static class FilterValue { 
        public static final FilterValue DONT_RELAY = new FilterValue(1);
        public static final FilterValue DO_RELAY = new FilterValue(2);
        public static final FilterValue DONT_CARE = new FilterValue(0);
        int v;
        private FilterValue(int i) { v=i; }
    }
    
    /**
      * Read in default value for shouldRelayActions. The default is true. 
      */
    private void getPluginParams() {
      if (logger.isDebugEnabled() && getParameters().isEmpty()) { 
          logger.debug("ActionRelayManager accepts one boolean param to control relaying to the coordinator. None supplied, so defaulting to true");
          return;
      }

      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           shouldRelayActions = Boolean.valueOf(((String)iter.next() )).booleanValue() ;
           if (logger.isDebugEnabled()) { 
               logger.debug("ActionRelayManager accepts one boolean param to control relaying to the coordinator. Param = " + shouldRelayActions);
           }
      }

    }       

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams(); //sets shouldRelayActions
        managerAddress = null;
    }
    
    
    /** Called every time this component is scheduled to run. Any time objects that belong to its
     *  subscription sets change (add/modify/remove), this will be called. This method is executed
     *  within the context of a blackboard transaction (so do NOT use transaction syntax inside it).
     *  You may only need to monitor one or two type of actions (e.g. additions), in which case you
     *  can safely remove the sections of code dealing with collection changes you are not interested
     *  in.
     */
    protected void execute() {

        boolean mgrJustFound = false;
        Iterator iter;
        //First check to see if we know where the manager is. If not, look it up (if shouldRelayActions == true).
        if (managerAddress == null && shouldRelayActions) {
            //Check to see if the Coordinator's address is known...
            iter = managerAddressSubscription.getAddedCollection().iterator();
            if (iter.hasNext()) {
              // find & set the ManagerAgent address
               managerAddress = ((RobustnessManagerID)iter.next()).getMessageAddress();
               mgrJustFound = true; //signal that there might be unwrapped actions that need to be wrapped
                                    // -- They weren't wrapped because the mgr wasn't found.
               if (logger.isDebugEnabled()) logger.debug("++++++++++++++++++++++++++++> ManagerAddress: "+managerAddress.toString());
               
               //Update the knob with status -- we know where the coordinator is
               iter = knobSubscription.getCollection().iterator();        
               if (iter.hasNext()) {
                   ActionRelayManagerKnob = (ActionRelayManagerKnob)iter.next();
                   ActionRelayManagerKnob.setFoundCoordinator(managerAddress);
               }               
                } else {
               if (logger.isDebugEnabled()) logger.debug("++++++++++++++++++++++++++++> ManagerAddress subscription empty.");
            }                
        }
        
        if (managerAddress == null || !shouldRelayActions) 
            return; //we cannot forward Actions until we known where the coordinator is

        //See if our knob has changed...
        iter = knobSubscription.getChangedCollection().iterator();        
        if (iter.hasNext()) {
            ActionRelayManagerKnob = (ActionRelayManagerKnob)iter.next();
            shouldRelayActions = ActionRelayManagerKnob.getShouldRelay();
        }
        
        //Wrap & Relay the Actions -- won't get here if managerAddress == null
        if (shouldRelayActions) {
            
            Collection added = null;
            
            if (mgrJustFound) { //then all of the actions should need to be wrapped.
                added = actionsSubscription.getCollection();                
            } else {           //Only need to get all added Actions & wrap them with a ActionsWrapper
                added = actionsSubscription.getAddedCollection();
            }
            
            for ( iter = added.iterator(); iter.hasNext() ; ) 
            {
                Action a = (Action)iter.next();                
                if (a.getWrapper() == null) { //Make sure it wasn't already wrapped... just a precaution
                    logger.debug("============= On asset ["+this.agentId+"] Saw new Action (with UID="+a.getUID()+")-- Wrapping it & relaying to "+managerAddress);
                    UID uid = this.us.nextUID(); //gen UID for this wrapper
                    ActionsWrapper aw = new ActionsWrapper(a, this.agentId, managerAddress, uid); //wrap the Action
                    //registerUID(uid); //record all UIDs so we know what wrappers are out there.
                    a.setWrapper(aw);
                    this.publishAdd(aw);                
                    //newWrappers.add(aw);
                }
            }
            
            //Get current list of action wrappers - this SHOULD include the wrappers we just added!
            Collection wrappers = wrapperSubscription.getCollection();        

            //add newly created wrappers (that aren't yet on the BB) to the ones from the BB
            //wrappers.addAll(newWrappers);  -- should already be there, since publishAdd was called.
            
            //Now look at changed Actions & publish change the associated wrapper if the action was publish-changed.
            Collection changed  = actionsSubscription.getChangedCollection();        
            //added.addAll(changed);

            
            for ( iter = changed.iterator(); iter.hasNext() ; ) 
            {
                Action a = (Action)iter.next();
                
                //Find wrapper for this Action
                ActionsWrapper aw = findWrapper(a, wrappers);
                //runFilters
                if (aw != null && runFilters(aw)) {
                    this.publishChange(aw);
                }
                
            }
            //newWrappers.clear(); // clear this as these wrappers will now have been committed to the BB
            
        }           
    }
     
    /**
     * Locates the wrapper for a given Action
     */
    private ActionsWrapper findWrapper(Action a, Collection wrappers ) {
     
        if (a.getWrapper() == null) {
            logger.error("=============>>>>>>>>> Saw Action with null wrapper attr (with UID="+a.getUID()+")");
        }
        UID uid = a.getWrapper().getUID();
        for ( Iterator iter = wrappers.iterator(); iter.hasNext() ; ) 
        {
            ActionsWrapper aw = (ActionsWrapper)iter.next();
            if ( uid.equals(aw.getUID()) ) {
                return aw;
            }
        }
        return null;
        
    }
    
    protected void setupSubscriptions() {

        //create new knob to control whether this mgr relays the Action objects to
        //the coordinator
        ActionRelayManagerKnob knob = new ActionRelayManagerKnob(shouldRelayActions); 
        this.publishAdd(knob);        

        //Set the agentId
        agentId = this.getAgentAddress();
        
        
        wrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionsWrapper) {
                    return true ;
                }
                return false ;
            }
        }) ;        

        actionsSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof Action) {
                    return true ;
                }
                return false ;
            }
        }) ;        

        knobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionRelayManagerKnob) {
                    return true ;
                }
                return false ;
            }
        }) ;        
        
        //Watch for the robustness manager's id
        managerAddressSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof RobustnessManagerID ) {
                    return true ;
                }
                return false ;
            }
        }) ;
        
    }

    
}
