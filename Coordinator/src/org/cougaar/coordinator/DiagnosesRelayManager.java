/*
 * DiagnosesRelayManager.java
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
 * This Plugin relays diagnoses between the node and the agent, and
 * the node and the coordinator.
 *
 *
 */
public class DiagnosesRelayManager extends MinimalPluginBase implements NotPersistable {
    
    private IncrementalSubscription diagnosesSubscription;
    private IncrementalSubscription knobSubscription;
    private IncrementalSubscription managerAddressSubscription;
    private IncrementalSubscription wrapperSubscription;
    
    private boolean shouldRelayDiagnoses = true;
    private MessageAddress managerAddress = null;
    
    private Vector relayFilters = null;
    
    private DiagnosesRelayManagerKnob diagnosesRelayManagerKnob;
    
    private Vector newWrappers = null;

    /** 
      * Creates a new instance of DefenseActivation 
      */
    public DiagnosesRelayManager() {
        super();
        relayFilters = new Vector();
        newWrappers = new Vector();
    }
    
    /**
     * Add a filter to control whether a diagnosis is relayed
     */
    public void addFilter(DiagnosisRelayFilter drf) {
        relayFilters.add(drf);
    }

    /**
     * Remove a filter.
     * @return TRUE if the element was found & removed.
     */
    public boolean removeFilter(DiagnosisRelayFilter drf) {
        return relayFilters.remove(drf);
    }
    

    /**
     * Run the relay filters to determine if a diagnosis should be relayed now
     * @return TRUE if the diagnosis should be relayed.
     */
    private boolean runFilters(DiagnosesWrapper dw) {
        Iterator iter = relayFilters.iterator();
        while (iter.hasNext()) {
            DiagnosisRelayFilter drf = (DiagnosisRelayFilter)iter.next();
            FilterValue result = drf.filterDiagnosis(dw);
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
      * Read in default value for shouldRelayDiagnoses. The default is true. 
      */
    private void getPluginParams() {
      if (logger.isDebugEnabled() && getParameters().isEmpty()) { 
          logger.debug("DiagnosisManager accepts one boolean param to control relaying to the coordinator. None supplied, so defaulting to true");
          return;
      }

      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           shouldRelayDiagnoses = Boolean.valueOf(((String)iter.next() )).booleanValue() ;
           if (logger.isDebugEnabled()) { 
               logger.debug("DiagnosisManager accepts one boolean param to control relaying to the coordinator. Param = " + shouldRelayDiagnoses);
           }
      }

    }       

    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams(); //sets shouldRelayDiagnoses
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

        Iterator iter;
        boolean mgrJustFound = false;

        //First check to see if we know where the manager is. If not, look it up (if shouldRelayDiagnoses == true).
        if (managerAddress == null && shouldRelayDiagnoses) {
            //Check to see if the Coordinator's address is known...
            iter = managerAddressSubscription.getAddedCollection().iterator();
            if (iter.hasNext()) {
              // find & set the ManagerAgent address
               managerAddress = ((RobustnessManagerID)iter.next()).getMessageAddress();
               mgrJustFound = true; //signal that there might be unwrapped actions that need to be wrapped
                                    // -- They weren't wrapped because the mgr wasn't found.
               if (logger.isDebugEnabled()) logger.debug("ManagerAddress: "+managerAddress.toString());
               
               //Update the knob with status -- we know where the coordinator is
               iter = knobSubscription.getCollection().iterator();        
               if (iter.hasNext()) {
                   diagnosesRelayManagerKnob = (DiagnosesRelayManagerKnob)iter.next();
                   diagnosesRelayManagerKnob.setFoundCoordinator(managerAddress);
               }               
            } else {
               if (logger.isDebugEnabled()) logger.debug("++++++++++++++++++++++++++++> ManagerAddress subscription empty.");
            }                
        }
        
        if (managerAddress == null || !shouldRelayDiagnoses) 
            return; //we cannot forward diagnoses until we known where the coordinator is

        //See if our knob has changed...
        iter = knobSubscription.getChangedCollection().iterator();        
        if (iter.hasNext()) {
            diagnosesRelayManagerKnob = (DiagnosesRelayManagerKnob)iter.next();
            shouldRelayDiagnoses = diagnosesRelayManagerKnob.getShouldRelay();
        }
        
        //Wrap & Relay the diagnoses
        if (shouldRelayDiagnoses) {
            
            Collection added = null;
            
            if (mgrJustFound) { //then all of the actions should need to be wrapped.
                added = diagnosesSubscription.getCollection();                
            } else {           //Only need to get all added Actions & wrap them with a ActionsWrapper
                added = diagnosesSubscription.getAddedCollection();
            }
            
                        
            for ( iter = added.iterator(); iter.hasNext() ; ) 
            {
                Diagnosis d = (Diagnosis)iter.next();                
                if (d.getWrapper() == null) { //Make sure it wasn't already wrapped... just a precaution
                    logger.debug("============= Saw new Diagnosis (with UID="+d.getUID()+") -- Wrapping it.");
                    UID uid = this.us.nextUID(); //gen UID for this wrapper
                    DiagnosesWrapper dw = new DiagnosesWrapper(d, this.agentId, managerAddress, uid); //wrap the diagnosis
                    //registerUID(uid); //record all UIDs so we know what wrappers are out there.
                    d.setWrapper(dw);
                    this.publishAdd(dw);                
                    //newWrappers.add(dw);
                }
            }
            
            //Now look at changed diagnoses
            Collection changed  = diagnosesSubscription.getChangedCollection();        
            Collection wrappers = wrapperSubscription.getCollection();  //should include the wrappers we just added!
            //added.addAll(changed);

            //wrappers.addAll(newWrappers); //add newly created wrappers (that aren't yet on the BB) to the ones from the BB

            for ( iter = changed.iterator(); iter.hasNext() ; ) 
            {
                Diagnosis d = (Diagnosis)iter.next();
                
                //Find wrapper for this Diagnosis
                DiagnosesWrapper dw = findWrapper(d, wrappers);
                //runFilters
                if (dw != null && runFilters(dw)) {
                    this.publishChange(dw);
                }
                
            }
            //newWrappers.clear(); // clear this as these wrappers will now be committed to the BB
            
        }           
    }
    
    /**
     * Locates the wrapper for a given diagnosis
     */
    private DiagnosesWrapper findWrapper(Diagnosis d, Collection wrappers ) {
     
        if (d.getWrapper() == null) {
            logger.error("=============>>>>>>>>> Saw Diagnosis with null wrapper attr (with UID="+d.getUID()+")");
        }
        UID uid = d.getWrapper().getUID();
        for ( Iterator iter = wrappers.iterator(); iter.hasNext() ; ) 
        {
            DiagnosesWrapper dw = (DiagnosesWrapper)iter.next();
            if ( uid.equals(dw.getUID()) ) {
                return dw;
            }
        }
        return null;
        
    }
    
    protected void setupSubscriptions() {

        //create new knob to control whether this mgr relays the diagnosis objects to
        //the coordinator
        DiagnosesRelayManagerKnob knob = new DiagnosesRelayManagerKnob(shouldRelayDiagnoses); 
        this.publishAdd(knob);        

        //Set the agentId
        agentId = this.getAgentAddress();
        
        
        wrapperSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosesWrapper) {
                    return true ;
                }
                return false ;
            }
        }) ;        

        diagnosesSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof Diagnosis) {
                    return true ;
                }
                return false ;
            }
        }) ;        

        knobSubscription = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DiagnosesRelayManagerKnob) {
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
