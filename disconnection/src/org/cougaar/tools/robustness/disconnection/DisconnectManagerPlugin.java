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

import org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.NoStartedActionException;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.RobustnessManagerID;


public class DisconnectManagerPlugin extends DisconnectPluginBase {

    private ServiceBroker sb;
    private MessageAddress managerAddress;
    private long lateReportingForgiveness;
    
    private IncrementalSubscription reconnectTimeConditionSubscription;
    private IncrementalSubscription nodeActuatorSubscription;
//    private IncrementalSubscription agentActuatorSubscription;
    private IncrementalSubscription managerAddressSubscription;
    
    private Hashtable activeDisconnects = new Hashtable();
    private RequestToDisconnectNodeSensorIndex requestToDisconnectNodeSensorIndex = new RequestToDisconnectNodeSensorIndex();
    private NodeDisconnectActuatorIndex nodeDisconnectActuatorIndex = new NodeDisconnectActuatorIndex();
    private RequestToDisconnectAgentSensorIndex requestToDisconnectAgentSensorIndex = new RequestToDisconnectAgentSensorIndex();

    // Legal Diagnosis Values
    private final static String DISCONNECT_REQUEST = DisconnectConstants.DISCONNECT_REQUEST;
    private final static String CONNECT_REQUEST = DisconnectConstants.CONNECT_REQUEST;
    private final static String TARDY = DisconnectConstants.TARDY;

    // Legal Action Values
    private final static String ALLOW_DISCONNECT = DisconnectConstants.ALLOW_DISCONNECT;
    private final static String ALLOW_CONNECT = DisconnectConstants.ALLOW_CONNECT;

    
   
    public DisconnectManagerPlugin() {
        super();
    }
    
    
    public void load() {
        super.load();
	sb = getServiceBroker();
        cancelTimer();
    }
    
    private void getPluginParams() {
      // A forgiveness period for late reconnect reporting - mostly to compensate for messaging delays
      Iterator iter = getParameters().iterator (); 
      if (iter.hasNext()) {
           lateReportingForgiveness = Long.parseLong((String)iter.next()) * 1000L;
           logger.debug("Setting lateReportingForgiveness = " + lateReportingForgiveness/1000L + " sec.");
      }
      else {
          lateReportingForgiveness = 10000L;
          logger.debug("Defaulting lateReportingForgiveness = " + lateReportingForgiveness/1000 + " sec.");
      }
    }

    public void setupSubscriptions() {

        getPluginParams();
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
        
        //Listen for changes to NodeActuators
        nodeActuatorSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof NodeDisconnectActuator ) {
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
    
        if (logger.isDebugEnabled()) logger.debug("Entering ** execute() **");
        
        Iterator iter;
        
        if (managerAddress != null) {// already know the ManagerAgent, so create conditions & opmodes for newly announced Nodes & Agents
            iter = reconnectTimeConditionSubscription.getAddedCollection().iterator();
            while (iter.hasNext()) {
                ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
                
                //DefenseApplicabilityCondition item = DefenseApplicabilityCondition.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, rtc.getExpandedName(), blackboard);
                RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
                if (diag != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
                    if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
                }
                else { // make new conditions & opmodes
                    createDiagnosesAndActions(rtc);
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
                /*
                DefenseApplicabilityCondition item = DefenseApplicabilityCondition.findOnBlackboard(DisconnectConstants.DEFENSE_NAME, rtc.getExpandedName(), blackboard);
                if (item != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
                    if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
                }
                else { */
                // make new conditions & opmodes
                createDiagnosesAndActions(rtc);
                //}
            }            
        }
        
        iter = reconnectTimeConditionSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
            if (rtc != null) {
                double encodedTime = ((Double)rtc.getValue()).doubleValue();
                if (logger.isDebugEnabled()) logger.debug("Changed "+rtc.toString());
                if (encodedTime >= 0.0) { // it's a request to do something
                    handleNodeRequest(rtc);
                }
                else { // it's an acknowledgement that the Action happened
                    handleNodeAcknowledgment(rtc);
                }
            }
        }
        
        iter = nodeActuatorSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            NodeDisconnectActuator nda = (NodeDisconnectActuator)iter.next();
            if (nda != null) {
                if (logger.isDebugEnabled()) logger.debug(nda.dump() + " seen");
            }
            Set newPV = nda.getNewPermittedValues();
            if (newPV != null) {
                try {
                    nda.setPermittedValues(newPV);
                    nda.clearNewPermittedValues();
                    if (nda.getPermittedValues().contains(ALLOW_DISCONNECT)) {
                        nda.start(ALLOW_DISCONNECT);
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.get(nda.getAssetID());
                        if (overdueAlarm != null) getAlarmService().addRealTimeAlarm(overdueAlarm);  // start the previously created alarm because we have permission to disconnect
                        propagateChange(nda, ALLOW_DISCONNECT);
                        //nda.stop(Action.COMPLETED); // really should interact with the Node to determine that the Disconnect happened
                        blackboard.publishChange(nda);
                   }
                    else if (nda.getPermittedValues().contains(ALLOW_CONNECT)) {
                        nda.start(ALLOW_CONNECT);
                        propagateChange(nda, ALLOW_CONNECT);
                        //nda.stop(Action.COMPLETED); // really should interact with the Node to determine that the Connect happened
                        blackboard.publishChange(nda);
                    }
                    else {  // cancel any alarm if Coordinator denies permission to disconnect
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(nda.getAssetID());
                        if (overdueAlarm != null) overdueAlarm.cancel();
                    }
                } catch (IllegalValueException e)  {
                    logger.error("Attempt to set: "+nda.dump()+" with illegal value "+e.toString());
                    return;
                } 
            }
        }
       
    }
    
    
    private void createDiagnosesAndActions(ReconnectTimeCondition rtc) {
        // create the Defense-Level Diagnosis & Action seen by the Coordinator

        try {
            RequestToDisconnectNodeSensor diag = new RequestToDisconnectNodeSensor(rtc.getAsset(), sb);
            double time = ((Double)rtc.getValue()).doubleValue();
            try {
                if (time > 0.0) {
                    diag.setValue(DISCONNECT_REQUEST); // wants to disconnect
                    OverdueAlarm overdueAlarm = new OverdueAlarm(diag, rtc.getAgents(), time > 60000.0 ? (time + lateReportingForgiveness) : (60000.0 + lateReportingForgiveness));  // Don't monitor for less than 60 sec
                    activeDisconnects.put(diag.getAssetID(), overdueAlarm);
                //getAlarmService().addRealTimeAlarm(overdueAlarm);  do this after getting permission
                }
                else {
                    diag.setValue(CONNECT_REQUEST); // not disconnected
                }
                requestToDisconnectNodeSensorIndex.putDiagnosis(diag);
                blackboard.publishAdd(diag);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.toString()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeSensor");
            return;
        }

        try {
            NodeDisconnectActuator action = new NodeDisconnectActuator(rtc.getAsset(), sb);
            try {
                HashSet offers = new HashSet();
                offers.add( ALLOW_CONNECT );
                offers.add( ALLOW_DISCONNECT );
                action.setValuesOffered(offers); // everything is allowed
                nodeDisconnectActuatorIndex.putAction(action);
                blackboard.publishAdd(action);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+action.toString()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+action.dump()+" with illegal value "+e.toString());
                return;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for NodeDisconnectActuator");
            return;
        }
        if (logger.isDebugEnabled()) logger.debug("Created Diagnosis & Action for: " + rtc.toString());
        

    }
    
    private boolean handleNodeRequest(ReconnectTimeCondition rtc) {

        // Set the values of the Node Sensor & all Agent Sensors
        double time = ((Double)rtc.getValue()).doubleValue();
        Diagnosis diag = requestToDisconnectNodeSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));

        try {
            if (time > 0.0) {
                diag.setValue(DISCONNECT_REQUEST); // wants to disconnect
                OverdueAlarm overdueAlarm = new OverdueAlarm(diag, rtc.getAgents(), time > 10000.0 ? (time + lateReportingForgiveness) : (10000.0 + lateReportingForgiveness));  // Don't monitor for less than 10 sec
                activeDisconnects.put(diag.getAssetID(), overdueAlarm);
            //getAlarmService().addRealTimeAlarm(overdueAlarm);  do this after getting permission
            }
            else {
                diag.setValue(CONNECT_REQUEST); // not disconnected
            }
        } catch (IllegalValueException e) {
            logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
            return false;
        }

        Iterator iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
            Diagnosis agentDiag = requestToDisconnectAgentSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Agent")));
            try {
                if (time > 0.0) {
                    diag.setValue(DISCONNECT_REQUEST); // wants to disconnect
                }
                else {
                    diag.setValue(CONNECT_REQUEST); // not disconnected
                }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return false;
            }
        }

        iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
        }

        // If all Sensor settings succeeded, publish all the sensors
        if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
            +diag.getAssetID()+" "
            +diag.getValue()+ " "+time
            +" for the Coordinator");
        blackboard.publishChange(diag);

        iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
        }

        
        return true;
    }


    private boolean handleNodeAcknowledgment(ReconnectTimeCondition rtc) {
        
        NodeDisconnectActuator action = nodeDisconnectActuatorIndex.getAction(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));

        try {
            if (action.getValue() != null) { // i.e., this isn't the initial connection
                action.stop(Action.COMPLETED); // ACK the completion
                blackboard.publishChange(action);
            }
            return true;
        } catch (IllegalValueException e) {
            logger.error("Attempt to stop: "+action.dump()+" with illegal value "+e.toString());
        } catch (NoStartedActionException e) {
            logger.error(e.toString());
        }
        return false;
    }


    private boolean propagateChange(NodeDisconnectActuator nda, String action) {
        
        DisconnectDefenseAgentEnabler item = DisconnectDefenseAgentEnabler.findOnBlackboard("Node", nda.getAssetID().getName().toString(), blackboard);
        if (item != null) {
            item.setValue(action==ALLOW_DISCONNECT?"ENABLED":"DISABLED");
            blackboard.publishChange(item);
            if (logger.isDebugEnabled()) logger.debug("Sent back to Node: " + item.toString());
            return true;
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("could not find Enabler for" + nda.dump());
            return false;
        }
    }


    private class OverdueAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        Diagnosis diag;
        AgentVector agentsAffected;
        
        public OverdueAlarm(Diagnosis diag, AgentVector agentsAffected, double t) {
            detonate = System.currentTimeMillis() + (long) t;
            this.diag = diag;
            this.agentsAffected = agentsAffected;
            if (logger.isDebugEnabled()) logger.debug("OverdueAlarm created : "+diag.getAssetID()+ " with agents " + agentsAffected.toString() + " at time "+detonate + " for " + t + " seconds");
        }
        
        public long getExpirationTime() {return detonate;
        }

        public AgentVector getAgentsAffected() { return agentsAffected; }
        
        public void expire() {
            if (!expired) {
                expired = true;
                if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + diag.getAssetID() + " with agents " + agentsAffected + " no longer legitimately Disconnected");
                if (eventService.isEventEnabled()) eventService.event(diag.getAssetID()+" is no longer legitimately Disconnected");
                blackboard.openTransaction();
                try {
                    diag.setValue(TARDY);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+diag.toString()+" to illegal value " + TARDY);
                }
                blackboard.publishChange(diag);
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