/*
 * DefenseOperatingMode.java
 *
 * @author David Wells - OBJS
 *
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

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;


public class DisconnectManagerPlugin extends DisconnectPluginBase {
    
    private MessageAddress managerAddress;
    
    private IncrementalSubscription reconnectTimeConditionSubscription;
    private IncrementalSubscription defenseOpModeSubscription;
    private IncrementalSubscription monitoringOpModeSubscription;
    private IncrementalSubscription managerAddressSubscription;
    
    private Hashtable activeDisconnects = new Hashtable();
    
   
    public DisconnectManagerPlugin() {
        super();
    }
    
    
    public void load() {
        super.load();
        cancelTimer();
    }
    
    public void setupSubscriptions() {
        
        initObjects(); 
        
        //Listen for the ManagerAddress
        managerAddressSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof RobustnessManagerID) {
                    return true ;
                }
                return false ;
            }
        });
        
        //Listen for changes to Conditions
        reconnectTimeConditionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ReconnectTimeCondition ) {
                    return true ;
                }
                return false ;
            }
        });
        
        //Listen for changes to DefenseOpModes
        defenseOpModeSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DisconnectDefenseEnabler ) {
                    return true ;
                }
                return false ;
            }
        });
        
        //Listen for changes to MonitoringOpModes
        monitoringOpModeSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DisconnectMonitoringEnabler ) {
                    return true ;
                }
                return false ;
            }
        });
        
    }
    
    
    //Create one condition and one of each type of operating mode
    private void initObjects() {
    }
    

    
    public void execute() {
        
        Iterator iter;
        
        if (managerAddress != null) {// already know the ManagerAgent, so create conditions & opmodes for newly announced Nodes & Agents
            iter = reconnectTimeConditionSubscription.getAddedCollection().iterator();
            while (iter.hasNext()) {
                ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
                
                DefenseApplicabilityCondition item = DefenseApplicabilityCondition.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, rtc.getExpandedName(), blackboard);
                if (item != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
                    if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
                }
                else { // make new conditions & opmodes
                    createCondAndOpModes(rtc);
                }
            }
        }
        
        Iterator i = managerAddressSubscription.getAddedCollection().iterator();
        if (i.hasNext()) { // just found the ManagerAgentAddress
            managerAddress = ((RobustnessManagerID)i.next()).getMessageAddress();
            if (logger.isDebugEnabled()) logger.debug("ManagerAddress: "+managerAddress.toString());
            // so create conditions & opmodes for everything we've seen so far but not created
            iter = reconnectTimeConditionSubscription.iterator();
            while (iter.hasNext()) {
                ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
                
                DefenseApplicabilityCondition item = DefenseApplicabilityCondition.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, rtc.getExpandedName(), blackboard);
                if (item != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
                    if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
                }
                else { // make new conditions & opmodes
                    createCondAndOpModes(rtc);
                }
            }            
        }
        
        iter = reconnectTimeConditionSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            ReconnectTimeCondition sc = (ReconnectTimeCondition)iter.next();
            if (sc != null) {
                if (logger.isDebugEnabled()) logger.debug("Changed "+sc.getAsset() + " set to " + sc.getValue());
                handleDisconnectChange(sc);
            }
        }
        
        iter = defenseOpModeSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            DisconnectDefenseEnabler sc = (DisconnectDefenseEnabler)iter.next();
            if (sc != null) {
                if (logger.isDebugEnabled()) logger.debug(sc.getClass()+":"+sc.getAsset() + " set to " + sc.getValue());
            }
            if (sc.getValue().equals(DefenseConstants.DEF_DISABLED.toString())) {  // cancel the alarm if Coordinator denies permission to disconnect
                OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(sc.getExpandedName());
                if (overdueAlarm != null) overdueAlarm.cancel();
            }
            else {
                OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.get(sc.getExpandedName());
                if (overdueAlarm != null) getAlarmService().addRealTimeAlarm(overdueAlarm);  // start the previously created alarm because we have permission to disconnect
            }
            propagateDefenseEnablerChange(sc);
        }
        
        
        iter = monitoringOpModeSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            DisconnectMonitoringEnabler sc = (DisconnectMonitoringEnabler)iter.next();
            if (sc != null) {
                if (logger.isDebugEnabled()) logger.debug(sc.getClass()+":"+sc.getAsset() + " set to " + sc.getValue());
            }
            propagateMonitoringEnablerChange(sc);
        }
        
    }
    
    
    private void createCondAndOpModes(ReconnectTimeCondition rtc) {
        // create the Defense-Level conditions & opmodes seen by the Coordinator
        DisconnectApplicabilityCondition dac = new DisconnectApplicabilityCondition(rtc.getAssetType(), rtc.getAsset());
        double t = ((Double)rtc.getValue()).doubleValue();
        if (t > 0.0) {
            dac.setValue(DefenseConstants.BOOL_TRUE); // disconnected
            OverdueAlarm overdueAlarm = new OverdueAlarm(dac, t > 10000.0 ? t : 10000.0);  // Don't monitor for less than 5 sec
            activeDisconnects.put(dac.getExpandedName(), overdueAlarm);
            //getAlarmService().addRealTimeAlarm(overdueAlarm);  do this after getting permission
        }
        else {
            dac.setValue(DefenseConstants.BOOL_FALSE); // not disconnected
        }
        dac.setUID(getUIDService().nextUID());
        dac.setSourceAndTarget(getAgentAddress(), managerAddress);
        blackboard.publishAdd(dac);
        
        DisconnectDefenseEnabler dde = new DisconnectDefenseEnabler(rtc.getAssetType(), rtc.getAsset());
        dde.setUID(getUIDService().nextUID());
        dde.setSourceAndTarget(getAgentAddress(), managerAddress);
        blackboard.publishAdd(dde);
        
        DisconnectMonitoringEnabler dme = new DisconnectMonitoringEnabler(rtc.getAssetType(), rtc.getAsset());
        dme.setUID(getUIDService().nextUID());
        dme.setSourceAndTarget(getAgentAddress(), managerAddress);
        blackboard.publishAdd(dme);
        
        if (logger.isDebugEnabled()) logger.debug("Added "+rtc.getAsset()+" Conditions & OpModes for Coordinator");
        
    }
    
    private boolean handleDisconnectChange(ReconnectTimeCondition rtc) {
        
        double t = ((Double)rtc.getValue()).doubleValue();
        
        DisconnectApplicabilityCondition item = (DisconnectApplicabilityCondition)DefenseApplicabilityCondition.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, rtc.getExpandedName(), blackboard);
        if (item != null) {
            if (t > 0.0) {
                OverdueAlarm oldAlarm = (OverdueAlarm) activeDisconnects.get(item);
                if (oldAlarm != null) oldAlarm.cancel();
                item.setValue(DefenseConstants.BOOL_TRUE); // disconnected
                OverdueAlarm overdueAlarm = new OverdueAlarm(item, t > 10000.0 ? t : 10000.0);  // Don't monitor for less than 5 sec
                activeDisconnects.put(item.getExpandedName(), overdueAlarm);
                //getAlarmService().addRealTimeAlarm(overdueAlarm);  do this after getting permission
            }
            else {
                item.setValue(DefenseConstants.BOOL_FALSE); // not disconnected
                OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(item);
                if (overdueAlarm != null) overdueAlarm.cancel();
            }
            if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                +item.getClass()+" "
                +item.getAssetType()+" "+item.getAsset()+" " +item.getDefenseName()+" " +item.getValue()+ " "+t
                +" for the Coordinator");
            blackboard.publishChange(item);
            return true;
        }
        else {
            return false;
        }
    }
    
    private boolean propagateMonitoringEnablerChange(DisconnectMonitoringEnabler dme) {
        
        DisconnectMonitoringAgentEnabler item = DisconnectMonitoringAgentEnabler.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, dme.getExpandedName(), blackboard);
        if (item != null) {
            item.setValue(dme.getValue());
            blackboard.publishChange(item);
            return true;
        }
        else {
            return false;
        }
    }
    
    private boolean propagateDefenseEnablerChange(DisconnectDefenseEnabler dme) {
        
        DisconnectDefenseAgentEnabler item = DisconnectDefenseAgentEnabler.findOnBlackboard(dme.getAssetType(), dme.getAsset(), blackboard);
        if (item != null) {
            item.setValue(dme.getValue());
            blackboard.publishChange(item);
            return true;
        }
        else {
            return false;
        }
    }
    
    private class OverdueAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        DisconnectApplicabilityCondition cond;
        
        public OverdueAlarm(DisconnectApplicabilityCondition cond, double t) {
            detonate = System.currentTimeMillis() + (long) t;
            this.cond = cond;
            if (logger.isDebugEnabled()) logger.debug("OverdueAlarm created : "+cond.getAsset()+ " at time "+detonate);
        }
        
        public long getExpirationTime() {return detonate;
        }
        
        public void expire() {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + cond.getAsset()+" no longer legitimately Disconnected");
                if (eventService.isEventEnabled()) eventService.event(cond.getAsset()+" is no longer legitimately Disconnected");
                blackboard.openTransaction();
                cond.setValue(DefenseConstants.BOOL_FALSE);
                blackboard.publishChange(cond);
                blackboard.closeTransaction();
            }
        }
        public boolean hasExpired() {return expired;
        }
        public boolean cancel() {
            if (!expired)
                return expired = true;
            return false;
        }
        
    }
    
    
}