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

    private static Set ALLOW_DISCONNECT_SET;
    private static Set ALLOW_CONNECT_SET;
    private static Set NULL_SET;

    // index of individual actions still outstanding
    // used to match grants of agent-level disconnects to Node-level disconnects
    private Hashtable pendingRequests = new Hashtable();
   
    // index of nodes that are disconnected and must be monitored to make sure they repoort back on time
    private Hashtable activeDisconnects = new Hashtable();

    // an indicator that at least 1 Tardyness alarm has expired
    private boolean somethingExpired = false;
    private Set expiredAlarms = new HashSet();

    public DisconnectManagerPlugin() {
        super();
    }
    
    
    public void load() {
        super.load();
	sb = getServiceBroker();
        cancelTimer();
        ALLOW_DISCONNECT_SET = new HashSet();
        ALLOW_DISCONNECT_SET.add(ALLOW_DISCONNECT);
        ALLOW_CONNECT_SET = new HashSet();
        ALLOW_CONNECT_SET.add(ALLOW_CONNECT);
        NULL_SET = new HashSet();
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
 
        //Listen for changes to ANY Actuators
        actuatorSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(DisconnectActuator.pred);
    }
    
    
    //Create one condition and one of each type of operating mode
    private void initObjects() {
    }
    

    
    public void execute() {
    
        if (logger.isDebugEnabled()) logger.debug("Entering ** execute() **");
        
        Iterator iter;

        if (somethingExpired) {
            iter = expiredAlarms.iterator();
            while(iter.hasNext()) {
                OverdueAlarm thisAlarm = (OverdueAlarm)iter.next();
                if (thisAlarm.hasExpired()) {
                    thisAlarm.handleExpiration();
                }
            }
        expiredAlarms.clear();
        somethingExpired = false;
        }
        
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

        iter = actuatorSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            DisconnectActuator actuator = (DisconnectActuator)iter.next();
            Set newPV = actuator.getNewPermittedValues();
            if (newPV != null) {
                if (logger.isDebugEnabled()) logger.debug("NPV: " + newPV.toString());
                if (logger.isDebugEnabled()) logger.debug("Permitted change to: " + actuator.dump());
                try {
                    actuator.setPermittedValues(newPV);
                    actuator.clearNewPermittedValues();
                    RequestRecord rr = (RequestRecord)pendingRequests.get(actuator);
                        if (rr != null) { // processing a request
                            if (logger.isDebugEnabled()) logger.debug("Requested Action is: " + rr.getRequest().toString());
                            if (actuator.getPermittedValues().contains(rr.getRequest())) {
                                boolean deleted = rr.getRemainingActions().remove(actuator);
                                if (logger.isDebugEnabled()) logger.debug("Removal of Action: " + actuator.dump() + " is " + deleted);
                                pendingRequests.remove(actuator);
                                if (rr.getRemainingActions().isEmpty()) {   
                                    propagateChange(rr, true);
                                }
                            }
                            else {
                                if (logger.isInfoEnabled()) logger.info("Permission NOT granted for: " + actuator.dump());
                               // Iterator iter2 = (rr.getOriginalActions()).removeAll(rr.getRemainingActions()).iterator();
                               // while (iter2.hasNext()) {
                                //    DisconnectActuator thisActuator = (DisconnectActuator)iter.next();
                                //    thisActuator.setPermittedValues(new HashSet());
                                //    blackboard.publishChange(thisActuator);
                                //    pendingRequests.remove(thisActuator); 
                              // }
                                propagateChange(rr, false);
                            }
                            if (logger.isDebugEnabled()) logger.debug("RequestRecord after getting permission for: " + actuator.dump() + " is " + rr.dump());
                        }
                    } catch (IllegalValueException e)  {
                    logger.error("Attempt to set: "+actuator.dump()+" with illegal value "+e.toString());
                    return;
                    } 
            } 
            else { // do not want to do anything
            }
        }
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
            NodeDisconnectActuator action = new NodeDisconnectActuator(rtc.getAsset(), NULL_SET, sb);
            HashSet offers = new HashSet();
            action.setValuesOffered(offers); // everything is allowed
            disconnectActuatorIndex.putAction(action);
            blackboard.publishAdd(action);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
        } catch (IllegalValueException e) {
            logger.error(e.toString());
            return false;
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
            diag.setValue(CONNECTED); // wants to disconnect
            requestToDisconnectAgentSensorIndex.putDiagnosis(diag);
            blackboard.publishAdd(diag);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.dump()); }
        } catch (IllegalValueException e) {
            logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
            return false;
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeSensor");
            return false;
        }

        try {
            AgentDisconnectActuator action = new AgentDisconnectActuator(rtc.getAsset(), NULL_SET, sb);
            HashSet offers = new HashSet();
            action.setValuesOffered(offers); // everything is allowed
            disconnectActuatorIndex.putAction(action);
            blackboard.publishAdd(action);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
        } catch (IllegalValueException e) {
            logger.error(e.toString());
            return false;
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
        String response =  time > 0.0 ? ALLOW_DISCONNECT : ALLOW_CONNECT;
        RequestRecord rr = new RequestRecord();
        Set originalActions = new HashSet();
        Set remainingActions = new HashSet();
        Set whichToOffer;

        RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
        
        if (request == DISCONNECT_REQUEST) {
            OverdueAlarm overdueAlarm = new OverdueAlarm(diag, rtc.getAgents(), time > 10000.0 ? (time + lateReportingForgiveness) : (10000.0 + lateReportingForgiveness));  // Don't monitor for less than 10 sec
            activeDisconnects.put(diag.getAssetID(), overdueAlarm);
            getAlarmService().addRealTimeAlarm(overdueAlarm);            
            if (logger.isDebugEnabled()) logger.debug("Added alarm from handleNodeRequest()");
            whichToOffer = ALLOW_DISCONNECT_SET;
        }
        else {
            whichToOffer = ALLOW_CONNECT_SET;
        }

        try {
            diag.setValue(request); 
            if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                    +diag.dump()+ " "+time
                    +" for the Coordinator");
            blackboard.publishChange(diag);
            DisconnectActuator actuator = disconnectActuatorIndex.getAction(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
            actuator.setValuesOffered(whichToOffer);
            if (logger.isDebugEnabled()) logger.debug(actuator.dump());
            blackboard.publishChange(actuator);
            rr.setAssetID(actuator.getAssetID());
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
                actuator.setValuesOffered(whichToOffer);
                blackboard.publishChange(actuator);
                if (logger.isDebugEnabled()) logger.debug(actuator.dump());
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
        rr.setRequest(response); 
       
        if (logger.isDebugEnabled()) { logger.debug(rr.dump()); }
        return true;
    }


    private boolean handleNodeAcknowledgment(ReconnectTimeCondition rtc) {
    
        if (logger.isDebugEnabled()) logger.debug("Starting handleNodeAcknoweledgement() for: " + rtc.toString());
        AssetID assetID = new AssetID(rtc.getAsset(), AssetType.findAssetType("Node"));
        DisconnectActuator action = disconnectActuatorIndex.getAction(assetID);
        if (logger.isDebugEnabled()) logger.debug("Searching for: " + rtc.getAsset() +" in: " + disconnectActuatorIndex.toString());
        if (logger.isDebugEnabled()) logger.debug(action.dump());

        if (action.getValue().getAction() != null) { // i.e., this isn't the initial connection
            if (logger.isDebugEnabled()) logger.debug("Value: " + action.getValue());
            if (action.getValue().getAction().equals(ALLOW_CONNECT)) {
                // Reconnected, so cancel any outstanding alarms
                OverdueAlarm overdueAlarm = (OverdueAlarm)activeDisconnects.remove(action.getAssetID());
                if (overdueAlarm != null) overdueAlarm.cancel();
                try {
                    action.stop(Action.COMPLETED); // ACK the completion
                    if (logger.isDebugEnabled()) logger.debug("**** " + action.dump());
                    blackboard.publishChange(action);
                    // Assert a new Diagnosis
                    RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(assetID);
                    diag.setValue(CONNECTED);
                    if (logger.isDebugEnabled()) logger.debug("**** " + diag.dump());
                    blackboard.publishChange(diag);
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
                        // Reconnected, so cancel any outstanding alarms
                        overdueAlarm = (OverdueAlarm)activeDisconnects.remove(agentAction.getAssetID());
                        if (overdueAlarm != null) overdueAlarm.cancel();
                        agentAction.stop(Action.COMPLETED); // ACK the completion
                        if (logger.isDebugEnabled()) logger.debug(agentAction.dump());
                        blackboard.publishChange(agentAction);
                        // Assert a new Diagnosis
                        RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(assetID);
                        diag.setValue(CONNECTED);
                    if (logger.isDebugEnabled()) logger.debug(diag.dump());
                        blackboard.publishChange(diag);
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
            if (action.getValue().getAction().equals(ALLOW_DISCONNECT)) {
                // Reconnected, so cancel any outstanding alarms
               try {
                    action.stop(Action.COMPLETED); // ACK the completion
                    if (logger.isDebugEnabled()) logger.debug(action.dump());
                    blackboard.publishChange(action);
                    // Assert a new Diagnosis
                    RequestToDisconnectNodeSensor diag = requestToDisconnectNodeSensorIndex.getDiagnosis(assetID);
                    diag.setValue(DISCONNECTED);
                if (logger.isDebugEnabled()) logger.debug(diag.dump());
                    blackboard.publishChange(diag);
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
                    assetID = new AssetID(agentName, AssetType.findAssetType("Agent"));
                    if (logger.isDebugEnabled()) logger.debug("Searching for: " + agentName +" in: " + disconnectActuatorIndex.toString());
                    DisconnectActuator agentAction = disconnectActuatorIndex.getAction(assetID);
                    try {
                        agentAction.stop(Action.COMPLETED); // ACK the completion
                        if (logger.isDebugEnabled()) logger.debug(agentAction.dump());
                        blackboard.publishChange(agentAction);
                        // Assert a new Diagnosis
                        RequestToDisconnectAgentSensor diag = requestToDisconnectAgentSensorIndex.getDiagnosis(assetID);
                        diag.setValue(DISCONNECTED);
                        if (logger.isDebugEnabled()) logger.debug(diag.dump());
                        blackboard.publishChange(diag);
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
            if (logger.isDebugEnabled()) logger.debug("progapateChange() failed to match: " + action.getValue().getAction() + " when compared to " + ALLOW_DISCONNECT + " & " + ALLOW_CONNECT);
        }
        return false;
    }

    private boolean propagateChange(RequestRecord rr, boolean allowed) {

        if (logger.isDebugEnabled()) logger.debug("Starting propagateChange() for: " + rr.getAssetID().toString());
        
        DisconnectDefenseAgentEnabler item = DisconnectDefenseAgentEnabler.findOnBlackboard(rr.getAssetID().getType().toString(), rr.getAssetID().getName().toString(), blackboard);
        if (item != null) {
            if (allowed) item.setValue(rr.getRequest().equals(ALLOW_DISCONNECT) ? "ENABLED" : "DISABLED");  // Tell the node it can do what it requested
            else item.setValue(rr.getRequest().equals(ALLOW_CONNECT) ? "DISABLED" : "ENABLED");  // Tell the node it can NOT do what it requested
            blackboard.publishChange(item);
            if (logger.isDebugEnabled()) logger.debug("Sent back to Node: " + item.toString());

            if (allowed) { // tell the Coord that all the Actions are now started
                Iterator iter = rr.getOriginalActions().iterator();
                while (iter.hasNext()) {
                    DisconnectActuator actuator = (DisconnectActuator)iter.next();
                    try {
                        actuator.start(rr.getRequest());
                        blackboard.publishChange(actuator);
                    } catch (IllegalValueException e) {
                        logger.error("Attempt to start: "+actuator.dump()+" with illegal value "+e.toString());
                        return false;
                    }
                }
            }
            return true;
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("could not find Enabler for" + rr.dump());
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
            if (logger.isDebugEnabled()) logger.debug("OverdueAlarm created : "+diag.getAssetID()+ " with agents " + agentsAffected.toString() + " at time "+detonate + " for " + t/1000L + " seconds");
        }
        
        public long getExpirationTime() {return detonate;
        }

        public AgentVector getAgentsAffected() { return agentsAffected; }
        
        public void expire() {
            if (!expired) {
                if (logger.isDebugEnabled()) logger.debug("expire(): Alarm expired for: " + diag.getAssetID());
                expired = true;
                somethingExpired=true;
                expiredAlarms.add(this);
                blackboard.signalClientActivity();
            }
        }

        public void handleExpiration() {
            if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + diag.getAssetID() + " with agents " + agentsAffected + " no longer legitimately Disconnected");
            if (eventService.isEventEnabled()) eventService.event(diag.getAssetID()+" is no longer legitimately Disconnected");
            try {
                diag.setValue(TARDY);
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" to illegal value " + TARDY);
            }
            activeDisconnects.remove(diag);
            blackboard.publishChange(diag);
            Iterator iter = agentsAffected.iterator();
            while (iter.hasNext()) {
                String agentName = (String)iter.next();
                RequestToDisconnectAgentSensor agentDiag = requestToDisconnectAgentSensorIndex.getDiagnosis(new AssetID(agentName, AssetType.findAssetType("Agent")));
                try {
                    agentDiag.setValue(TARDY);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+diag.toString()+" to illegal value " + TARDY);
                }
            blackboard.publishChange(agentDiag);
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