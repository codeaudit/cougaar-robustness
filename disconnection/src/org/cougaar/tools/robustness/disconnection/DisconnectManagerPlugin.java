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
    private IncrementalSubscription actuatorSubscription; // for both agent & node actuators
    private IncrementalSubscription managerAddressSubscription;
    
    private RequestToDisconnectNodeSensorIndex requestToDisconnectNodeSensorIndex = new RequestToDisconnectNodeSensorIndex();
    private RequestToDisconnectAgentSensorIndex requestToDisconnectAgentSensorIndex = new RequestToDisconnectAgentSensorIndex();
    private DisconnectActuatorIndex disconnectActuatorIndex = new DisconnectActuatorIndex();

    // Legal Diagnosis Values
    private final static String DISCONNECT_REQUEST = DisconnectConstants.DISCONNECT_REQUEST;
    private final static String CONNECT_REQUEST = DisconnectConstants.CONNECT_REQUEST;
    private final static String TARDY = DisconnectConstants.TARDY;
    private final static String DISCONNECTED = DisconnectConstants.DISCONNECTED;
    private final static String CONNECTED = DisconnectConstants.CONNECTED;

    // Legal Action Values
    private final static String ALLOW_DISCONNECT = DisconnectConstants.ALLOW_DISCONNECT;
    private final static String ALLOW_CONNECT = DisconnectConstants.ALLOW_CONNECT;

    // index of individual actions still outstanding
    // used to match grants of agent-level disconnects to Node-level disconnects
    private Hashtable pendingRequests = new Hashtable();
   
    // index of nodes that are disconnected and must be monitored to make sure they repoort back on time
    private Hashtable activeDisconnects = new Hashtable();
    

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

        /*
        //Listen for changes to NodeActuators
        nodeActuatorSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof NodeDisconnectActuator ) {
                    return true ;
                }
                return false ;
            }
        });
        
        //Listen for changes to AgentActuators
        agentActuatorSubscription = (IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof AgentDisconnectActuator ) {
                    return true ;
                }
                return false ;
            }
        });
*/        
        //Listen for changes to ANY Actuators
        actuatorSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(DisconnectActuator.pred);
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
                createDiagnosisAndAction(rtc);
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
                createDiagnosisAndAction(rtc);
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

        /*
        iter = nodeActuatorSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            NodeDisconnectActuator nda = (NodeDisconnectActuator)iter.next();
            Set newPV = nda.getNewPermittedValues();
            if (newPV != null) {
                if (logger.isDebugEnabled()) logger.debug("Permitted change to: " + nda.dump());
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
       
        iter = agentActuatorSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            AgentDisconnectActuator ada = (AgentDisconnectActuator)iter.next();
            Set newPV = ada.getNewPermittedValues();
            if (newPV != null) {
                if (logger.isDebugEnabled()) logger.debug("Permitted change to: " + ada.dump());
                try {
                    ada.setPermittedValues(newPV);
                    ada.clearNewPermittedValues();
                    if (ada.getPermittedValues().contains(ALLOW_DISCONNECT)) {
                        ada.start(ALLOW_DISCONNECT);
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.get(ada.getAssetID());
                        if (overdueAlarm != null) getAlarmService().addRealTimeAlarm(overdueAlarm);  // start the previously created alarm because we have permission to disconnect
                        blackboard.publishChange(ada);
                   }
                    else if (ada.getPermittedValues().contains(ALLOW_CONNECT)) {
                        ada.start(ALLOW_CONNECT);
                        blackboard.publishChange(ada);
                    }
                    else {  // cancel any alarm if Coordinator denies permission to disconnect
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(ada.getAssetID());
                        if (overdueAlarm != null) overdueAlarm.cancel();
                    }
                } catch (IllegalValueException e)  {
                    logger.error("Attempt to set: "+ada.dump()+" with illegal value "+e.toString());
                    return;
                } 
            }
        }
  */  
    }
    

    private boolean createDiagnosisAndAction(ReconnectTimeCondition rtc) {
        if (rtc.getAssetType().equals("Node"))  createNodeDiagnosisAndAction(rtc);
        else createAgentDiagnosisAndAction(rtc);
        return true;
    }


    private boolean createNodeDiagnosisAndAction(ReconnectTimeCondition rtc) {
        // create the Defense-Level Diagnosis & Action seen by the Coordinator

        RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
        if (diag != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
            if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
            return true;
        }

        double time = ((Double)rtc.getValue()).doubleValue();
        try {
            diag = new RequestToDisconnectNodeSensor(rtc.getAsset(), sb);
            try {
                diag.setValue(CONNECTED); // not disconnected
                requestToDisconnectNodeSensorIndex.putDiagnosis(diag);
                blackboard.publishAdd(diag);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.dump()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return false;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeSensor");
            return false;
        }

        try {
            NodeDisconnectActuator action = new NodeDisconnectActuator(rtc.getAsset(), sb);
            try {
                HashSet offers = new HashSet();
                offers.add( ALLOW_CONNECT );
                offers.add( ALLOW_DISCONNECT );
                action.setValuesOffered(offers); // everything is allowed
                disconnectActuatorIndex.putAction(action);
                blackboard.publishAdd(action);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+action.dump()+" with illegal value "+e.toString());
                return false;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for NodeDisconnectActuator");
            return false;
        }
        if (logger.isDebugEnabled()) logger.debug("Created Diagnosis & Action for: " + rtc.toString());        
        return true;
    }

    private boolean createAgentDiagnosisAndAction(ReconnectTimeCondition rtc) {
        // create the Defense-Level Diagnosis & Action seen by the Coordinator

        RequestToDisconnectAgentSensor diag = requestToDisconnectAgentSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Agent")));
        if (diag != null) { // the DefenseApplicability condition already exists, so don't make anotherset of conditions & opmodes
            if (logger.isDebugEnabled()) logger.debug("Not creating redundant modes & conditions for already known "+rtc.getAsset());
            return true;
        }

        double time = ((Double)rtc.getValue()).doubleValue();
        try {
            diag = new RequestToDisconnectAgentSensor(rtc.getAsset(), sb);
            try {
                diag.setValue(CONNECTED); // wants to disconnect
                requestToDisconnectAgentSensorIndex.putDiagnosis(diag);
                blackboard.publishAdd(diag);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.dump()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return false;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeSensor");
            return false;
        }

        try {
            AgentDisconnectActuator action = new AgentDisconnectActuator(rtc.getAsset(), sb);
            try {
                HashSet offers = new HashSet();
                offers.add( ALLOW_CONNECT );
                offers.add( ALLOW_DISCONNECT );
                action.setValuesOffered(offers); // everything is allowed
                disconnectActuatorIndex.putAction(action);
                blackboard.publishAdd(action);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+action.dump()+" with illegal value "+e.toString());
                return false;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for AgentDisconnectActuator");
            return false;
        }
        if (logger.isDebugEnabled()) logger.debug("Created Diagnosis & Action for: " + rtc.toString());        
        return true;
    }
    

    private boolean handleNodeRequest(ReconnectTimeCondition rtc) {
        // Set the values of the Node Sensor & all Agent Sensors

        double time = ((Double)rtc.getValue()).doubleValue();
        String request =  time > 0.0 ? DISCONNECT_REQUEST : CONNECT_REQUEST;
        RequestRecord rr = new RequestRecord();
        Set originalActions = new HashSet();
        Set remainingActions = new HashSet();

        RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
        
        if (request == DISCONNECT_REQUEST) {
            OverdueAlarm overdueAlarm = new OverdueAlarm(diag, rtc.getAgents(), time > 10000.0 ? (time + lateReportingForgiveness) : (10000.0 + lateReportingForgiveness));  // Don't monitor for less than 10 sec
            activeDisconnects.put(diag.getAssetID(), overdueAlarm);
            if (logger.isDebugEnabled()) logger.debug("Added alarm from handleNodeRequest()");
        }

        try {
            diag.setValue(request); 
            if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                    +diag.dump()+ " "+time
                    +" for the Coordinator");
            blackboard.publishChange(diag);
            DisconnectActuator actuator = disconnectActuatorIndex.getAction(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
            originalActions.add(actuator);
            remainingActions.add(actuator);
            pendingRequests.put(actuator, rr);
        } catch (IllegalValueException e) {
            logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
            return false;
        }

        Iterator iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
            String agentName = (String) iter.next();
            AssetID id = new AssetID(agentName, AssetType.findAssetType("Agent"));
            RequestToDisconnectAgentSensor agentDiag = requestToDisconnectAgentSensorIndex.getDiagnosis(id);
            try {
                agentDiag.setValue(request); // wants to disconnect
                if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                        +diag.dump()+ " "+time
                        +" for the Coordinator");
                blackboard.publishChange(agentDiag);
                DisconnectActuator actuator = disconnectActuatorIndex.getAction(id);
                originalActions.add(actuator);
                remainingActions.add(actuator);
                pendingRequests.put(actuator, rr);
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return false;
            }
        }
        rr.setOriginalActions(originalActions);
        rr.setRemainingActions(remainingActions);
        rr.setRequest(request); 
       
        if (logger.isDebugEnabled()) { logger.debug(rr.dump()); }
        return true;
    }


    private boolean handleNodeAcknowledgment(ReconnectTimeCondition rtc) {
        
       DisconnectActuator action = disconnectActuatorIndex.getAction(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
        if (logger.isDebugEnabled()) logger.debug("Searching for: " + rtc.getAsset() +" in: " + disconnectActuatorIndex.toString());

        try {
            if (action.getValue() != null) { // i.e., this isn't the initial connection
                if (action.getValue().equals(ALLOW_CONNECT)) {
                    // Reconnected, so cancel any outstanding alarms
                    OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(action.getAssetID());
                    if (overdueAlarm != null) overdueAlarm.cancel();
                }
                action.stop(Action.COMPLETED); // ACK the completion
                if (logger.isDebugEnabled()) logger.debug(action.dump());
                blackboard.publishChange(action);
            }
        } catch (IllegalValueException e) {
            logger.error("Attempt to stop: "+action.dump()+" with illegal value "+e.toString());
            return false;
        } catch (NoStartedActionException e) {
            logger.error(e.toString());
            return false;
        }
        Iterator iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
            String agentName = (String) iter.next();
            if (logger.isDebugEnabled()) logger.debug("Searching for: " + agentName +" in: " + disconnectActuatorIndex.toString());
            DisconnectActuator agentAction = disconnectActuatorIndex.getAction(new AssetID(agentName, AssetType.findAssetType("Agent")));
            try {
                if (agentAction.getValue() != null) { // i.e., this isn't the initial connection
                    if (agentAction.getValue().equals(ALLOW_CONNECT)) {
                        // Reconnected, so cancel any outstanding alarms
                        OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(agentAction.getAssetID());
                        if (overdueAlarm != null) overdueAlarm.cancel();
                    }
                    agentAction.stop(Action.COMPLETED); // ACK the completion
                    if (logger.isDebugEnabled()) logger.debug(agentAction.dump());
                    blackboard.publishChange(agentAction);
                }
            } catch (IllegalValueException e) {
                logger.error("Attempt to stop: "+agentAction.dump()+" with illegal value "+e.toString());
                return false;
            } catch (NoStartedActionException e) {
                logger.error(e.toString());
                return false;
            }
        }
        return true;            
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