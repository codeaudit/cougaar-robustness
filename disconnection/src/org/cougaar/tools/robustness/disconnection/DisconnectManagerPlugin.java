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
import org.cougaar.core.persist.PersistenceNotEnabledException;


public class DisconnectManagerPlugin extends DisconnectPluginBase {

    private ServiceBroker sb;
    private MessageAddress managerAddress;
    private long lateReportingForgiveness;
    
    private IncrementalSubscription reconnectTimeConditionSubscription;
    private IncrementalSubscription ActionSubscription; // for both agent & node Actions
    private IncrementalSubscription managerAddressSubscription;
    
    private RequestToDisconnectNodeDiagnosisIndex requestToDisconnectNodeDiagnosisIndex;
    private RequestToDisconnectAgentDiagnosisIndex requestToDisconnectAgentDiagnosisIndex;
    private DisconnectActionIndex disconnectActionIndex;

    // Legal Diagnosis Values
    private final static String DISCONNECT_REQUEST = DisconnectConstants.DISCONNECT_REQUEST;
    private final static String CONNECT_REQUEST = DisconnectConstants.CONNECT_REQUEST;
    private final static String TARDY = DisconnectConstants.TARDY;
    private final static String DISCONNECTED = DisconnectConstants.DISCONNECTED;
    private final static String CONNECTED = DisconnectConstants.CONNECTED;

    // Legal Action Values
    private final static String ALLOW_DISCONNECT = DisconnectConstants.ALLOW_DISCONNECT;
    private final static String ALLOW_CONNECT = DisconnectConstants.ALLOW_CONNECT;
    private final static String AUTONOMOUS_RESTART = DisconnectConstants.AUTONOMOUS_RESTART;  // only performed when agent is restarted 

    private static Set ALLOW_DISCONNECT_SET;
    private static Set ALLOW_CONNECT_SET;
    private static Set NULL_SET;

    // index of individual actions still outstanding
    // used to match grants of agent-level disconnects to Node-level disconnects
    private Hashtable pendingRequests = new Hashtable();
   
    // index of nodes that are disconnected and must be monitored to make sure they repoort back on time
    private Hashtable activeOverdueAlarms = new Hashtable();

    private boolean rehydrating = false;

    // A Record of info that needs to survive moves and rehydrations
    private NodeStatus nodeStatus;

    // an indicator that at least 1 Tardyness alarm has expired
    private boolean somethingOverdueExpired = false;
    private Set expiredOverdueAlarms = new HashSet();

    // an indicator that at least one permission request has timed out
    private boolean somethingRequestedExpired = false;
    private Set expiredRequestedAlarms = new HashSet();
    

    public DisconnectManagerPlugin() {
        super();
    }
    
    
    public void load() {
        super.load();
	sb = getServiceBroker();
        cancelTimer();
        activeOverdueAlarms = new Hashtable();
        ALLOW_DISCONNECT_SET = new HashSet();
        ALLOW_DISCONNECT_SET.add(ALLOW_DISCONNECT);
        ALLOW_CONNECT_SET = new HashSet();
        ALLOW_CONNECT_SET.add(ALLOW_CONNECT);
        NULL_SET = new HashSet();

        blackboard.openTransaction();

        Collection c;
        Iterator iter;
        Iterator iter2;
        Iterator iter3;

        requestToDisconnectAgentDiagnosisIndex = new RequestToDisconnectAgentDiagnosisIndex();
        blackboard.publishAdd(requestToDisconnectAgentDiagnosisIndex);
        requestToDisconnectNodeDiagnosisIndex = new RequestToDisconnectNodeDiagnosisIndex();
        blackboard.publishAdd(requestToDisconnectNodeDiagnosisIndex);
        disconnectActionIndex = new DisconnectActionIndex();
        blackboard.publishAdd(disconnectActionIndex);
        
        c = blackboard.query(NodeStatus.pred);
        iter = c.iterator();
        if (iter.hasNext()) {
            rehydrating = true;
        }
        else {
            rehydrating = false;
            nodeStatus = new NodeStatus();
            if (logger.isDebugEnabled()) logger.debug("Creating new NodeStatus object");
            blackboard.publishAdd(nodeStatus);
        }


        blackboard.closeTransaction();

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
 
        //Listen for changes to ANY Actions
        ActionSubscription = (IncrementalSubscription ) getBlackboardService().subscribe(DisconnectAction.pred);

        blackboard.setShouldBePersisted(false);
    }
    
    
    
    public void execute() {
    
        if (logger.isDebugEnabled()) logger.debug("Entering ** execute() **");
        if (logger.isDetailEnabled()) logger.detail("RTC's: " + reconnectTimeConditionSubscription.toString());
        if (logger.isDebugEnabled()) logger.debug("Added RTC's: " + reconnectTimeConditionSubscription.getAddedCollection().toString());
        if (logger.isDebugEnabled()) logger.debug("Changed RTC's: " + reconnectTimeConditionSubscription.getChangedCollection().toString());

        if (logger.isDetailEnabled()) logger.detail("OpModes's: " + blackboard.query(DisconnectDefenseAgentEnabler.pred).toString());
       

        Iterator iter;

        if (rehydrating) {
            Collection c = blackboard.query(NodeStatus.pred);
            iter = c.iterator();
            if (iter.hasNext()) {
                nodeStatus = (NodeStatus)iter.next();
                if (logger.isDebugEnabled()) logger.debug(nodeStatus.toString());
                Iterator iter2 = nodeStatus.values().iterator();
                while (iter2.hasNext()) {
                   // create the Action(s) & Diagnosis(s) for the Node & its agents
                   NodeStatusRecord nsr = (NodeStatusRecord)iter2.next();
                   RequestToDisconnectNodeDiagnosis diag = createNodeDiagnosisAndAction(nsr.getNodeID(), nsr.getDiagnosis());
                   if (nsr.getDiagnosis().equals(DISCONNECTED)) 
                        createOverdueAlarm(diag, nsr.getAgents(), (nsr.getReconnectTime()-System.currentTimeMillis() < 120000) ? 120000 :  (nsr.getReconnectTime()-System.currentTimeMillis())); // give the node at least 2 minutes from now due to the move
                   Iterator iter3 = nsr.getAgents().iterator();
                   while (iter3.hasNext()) {
                       createAgentDiagnosisAndAction((AssetID)iter3.next(), nsr.getDiagnosis());
                   }
                   // create an OverdueAlarm if the Node is Disconnected
                }
                rehydrating = false;
            }
        }

        // Handle timed-out requests & deny permission 
         if (somethingRequestedExpired) {
            iter = expiredRequestedAlarms.iterator();
            while(iter.hasNext()) {
                RequestedAlarm thisAlarm = (RequestedAlarm)iter.next();
                if (thisAlarm.hasExpired()) {
                    thisAlarm.handleExpiration();
                }
            }
        expiredRequestedAlarms.clear();
        somethingRequestedExpired = false;
        }
        

        // Handle expired Alarms for overdue nodes
        if (somethingOverdueExpired) {
            iter = expiredOverdueAlarms.iterator();
            while(iter.hasNext()) {
                OverdueAlarm thisAlarm = (OverdueAlarm)iter.next();
                if (thisAlarm.hasExpired()) {
                    thisAlarm.handleExpiration();
                }
            }
        expiredOverdueAlarms.clear();
        somethingOverdueExpired = false;
        }
        
        if (managerAddress != null) {// already know the ManagerAgent, so create Diagnoes & Actions for newly announced Nodes & Agents
            if (logger.isDebugEnabled()) logger.debug("Know MA, so create Diagnosis & Action for newly announce Nodes & Agents");
            iter = reconnectTimeConditionSubscription.getAddedCollection().iterator();
            while (iter.hasNext()) {
                ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
                createDiagnosisAndAction(new AssetID(rtc.getAsset(), AssetType.findAssetType(rtc.getAssetType())));
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
                createDiagnosisAndAction(new AssetID(rtc.getAsset(), AssetType.findAssetType(rtc.getAssetType())));
            }            
        }
        
        iter = reconnectTimeConditionSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            ReconnectTimeCondition rtc = (ReconnectTimeCondition)iter.next();
            if (rtc != null) {
                double encodedTime = ((Double)rtc.getValue()).doubleValue();
                if (logger.isDebugEnabled()) logger.debug("Changed "+rtc.toString());
                handleNodeRequest(rtc);
            }
        }

        boolean anActionHasChangedPermittedValues = false;
        iter = ActionSubscription.getChangedCollection().iterator();
        while (iter.hasNext()) {
            DisconnectAction action = (DisconnectAction)iter.next();
            if (logger.isDebugEnabled()) logger.debug("Saw Changed: " + action.dump());
            Set newPV = action.getNewPermittedValues();
            if (newPV != null) {
                if (logger.isDebugEnabled()) logger.debug("NPV: " + newPV.toString());
                if (logger.isDebugEnabled()) logger.debug("Permitted change to: " + action.dump());
                try {
                    anActionHasChangedPermittedValues = true;
                    action.setPermittedValues(newPV);
                    action.clearNewPermittedValues();
                } catch (IllegalValueException e)  {
                logger.error("Attempt to set: "+action.dump()+" with illegal value "+e.toString());
                return;
                } 
            } 
        }

        if (anActionHasChangedPermittedValues) {
            iter = pendingRequests.values().iterator();
            while (iter.hasNext()) {
                RequestRecord rr = (RequestRecord)iter.next();
                Iterator iter2 = rr.getOriginalActions().iterator();
                boolean allPermittedSoFar = true;
                while (iter2.hasNext()) {
                    Action thisAction = (Action)iter2.next();
                    if ((thisAction.getPermittedValues() != null ) && (thisAction.getPermittedValues().contains(rr.getRequest()))) {
                        if (logger.isDebugEnabled()) logger.debug(rr.getRequest() + " of: " + thisAction.getAssetID().toString() + " is permitted");
                    }
                    else {
                        if (logger.isDebugEnabled()) logger.debug(rr.getRequest() + " of: " + thisAction.getAssetID().toString() + " is NOT permitted");
                        allPermittedSoFar = false;
                    }                            
                }
                if (allPermittedSoFar) {
                    propagatePermissions(rr);
                    iter.remove();
                    rr.getAlarm().cancel();
                    if (logger.isDebugEnabled()) logger.debug("Removing RequestedAlarm for: " + rr.getNodeID());
                }
            }                    
        }        
    }
    

    private Diagnosis createDiagnosisAndAction(AssetID id) {
        if (id.getType().getName().equalsIgnoreCase("Node"))  return createNodeDiagnosisAndAction(id);
        else return createAgentDiagnosisAndAction(id);
    }


    private RequestToDisconnectNodeDiagnosis createNodeDiagnosisAndAction(AssetID id) {
       return createNodeDiagnosisAndAction(id, CONNECTED);
    }

    private RequestToDisconnectNodeDiagnosis createNodeDiagnosisAndAction(AssetID id, String currentDiagnosis) {
        // create the Defense-Level Diagnosis & Action seen by the Coordinator

        RequestToDisconnectNodeDiagnosis diag = requestToDisconnectNodeDiagnosisIndex.getDiagnosis(id);
        if (diag != null) { // the Diagnosis already exists, so don't make another set of conditions & opmodes
            if (logger.isDebugEnabled()) logger.debug("Not creating redundant Diagnosis & Action for already known "+id.toString());
            return diag;
        }

        try {
            diag = new RequestToDisconnectNodeDiagnosis(id.getName(), sb);
            try {
                diag.setValue(currentDiagnosis);
                requestToDisconnectNodeDiagnosisIndex.putDiagnosis(diag);
                blackboard.publishAdd(diag);
                if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.dump()); }
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                return null;
            }
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeDiagnosis");
            return null;
        }

	  NodeDisconnectAction action;

        try {
		if (currentDiagnosis.equals(CONNECTED)) {
            	action = new NodeDisconnectAction(id.getName(), NULL_SET, sb);
		}
		else {
            	action = new NodeDisconnectAction(id.getName(), ALLOW_DISCONNECT_SET, sb);
			action.setPermittedValues(ALLOW_DISCONNECT_SET);
			action.start(ALLOW_DISCONNECT);
			try {
				action.stop(Action.ACTIVE);
			}
			catch (NoStartedActionException e) {
				logger.error(e.toString());
			}
		}
            action.setValuesOffered(NULL_SET); // remove offers - this has the added effect that the action will be considered by the Coord to be irrecocable
			/*
            	HashSet offers = new HashSet();
            	action.setValuesOffered(offers); // everything is allowed
			*/

            disconnectActionIndex.putAction(action);
            blackboard.publishAdd(action);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
        } catch (IllegalValueException e) {
            logger.error(e.toString());
            return null;
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for NodeDisconnectAction");
            return null;
        }
        if (logger.isDebugEnabled()) logger.debug("Created Diagnosis & Action for: " + id.toString());        
        return diag;
    }

    private RequestToDisconnectAgentDiagnosis createAgentDiagnosisAndAction(AssetID id) {
        return createAgentDiagnosisAndAction(id, CONNECTED);
    }

    private RequestToDisconnectAgentDiagnosis createAgentDiagnosisAndAction(AssetID id, String currentDiagnosis) {
        // create the Defense-Level Diagnosis & Action seen by the Coordinator

        RequestToDisconnectAgentDiagnosis diag = requestToDisconnectAgentDiagnosisIndex.getDiagnosis(id);
        if (diag != null) { // the Diagosis already exists, so don't create another Diagnosis & Action
            if (logger.isDebugEnabled()) logger.debug("Not creating redundant Diagnosis & Action for already known "+id.toString());
            return diag;
        }

        try {
            diag = new RequestToDisconnectAgentDiagnosis(id.getName(), sb);
            diag.setValue(currentDiagnosis);
            requestToDisconnectAgentDiagnosisIndex.putDiagnosis(diag);
            blackboard.publishAdd(diag);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+diag.dump()); }
        } catch (IllegalValueException e) {
            logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
            return null;
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for RequestToDisconnectNodeDiagnosis");
            return null;
        }

	  AgentDisconnectAction action;

        try {
		if (currentDiagnosis.equals(CONNECTED)) {
            	action = new AgentDisconnectAction(id.getName(), NULL_SET, sb);
		}
		else {
            	action = new AgentDisconnectAction(id.getName(), ALLOW_DISCONNECT_SET, sb);
			action.setPermittedValues(ALLOW_DISCONNECT_SET);
			action.start(ALLOW_DISCONNECT);
			try {
				action.stop(Action.ACTIVE);
			}
			catch (NoStartedActionException e) {
				logger.error(e.toString());
			}
		}
            action.setValuesOffered(NULL_SET); // remove offers - this has the added effect that the action will be considered by the Coord to be irrecocable

			/*
            	HashSet offers = new HashSet();
            	action.setValuesOffered(offers); // everything is allowed
			*/

            disconnectActionIndex.putAction(action);
            blackboard.publishAdd(action);
            if (logger.isDebugEnabled()) { logger.debug("Created: "+action.dump()); }
        } catch (IllegalValueException e) {
            logger.error(e.toString());
            return null;
        } catch (TechSpecNotFoundException e) {
            logger.error("TechSpec not found for AgentDisconnectAction");
            return null;
        }

        if (logger.isDebugEnabled()) logger.debug("Created Diagnosis & Action for: " + id.toString());        
        return diag;
    }
    

    private boolean handleNodeRequest(ReconnectTimeCondition rtc) {
        // Set the values of the Node Diagnosis & all Agent Diagnosiss

        double time = ((Double)rtc.getValue()).doubleValue();
        String request =  time > 0.0 ? DISCONNECT_REQUEST : CONNECT_REQUEST;
        String response =  time > 0.0 ? ALLOW_DISCONNECT : ALLOW_CONNECT;
        RequestRecord rr = new RequestRecord();
        Set originalActions = new HashSet();
        Set originalDiagnoses = new HashSet();
        AgentVector agentVector = new AgentVector();
        Set whichToOffer;

        RequestToDisconnectNodeDiagnosis diag = requestToDisconnectNodeDiagnosisIndex.getDiagnosis(new AssetID(rtc.getAsset(), AssetType.findAssetType("Node")));
        
        if (request == DISCONNECT_REQUEST) {
            createOverdueAlarm(diag, rtc, time);
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
            AssetID assetID = new AssetID(rtc.getAsset(), AssetType.findAssetType("Node"));
            NodeStatusRecord nsr = (NodeStatusRecord)nodeStatus.get(assetID);
            nsr.setReconnectTime(0.0);
            nsr.setDiagnosis(request);
            blackboard.publishChange(nodeStatus);
            DisconnectAction Action = disconnectActionIndex.getAction(assetID);
            Action.setValuesOffered(whichToOffer);
            if (logger.isDebugEnabled()) logger.debug(Action.dump());
            blackboard.publishChange(Action);
            rr.setNodeID(Action.getAssetID());
            originalActions.add(Action);
            originalDiagnoses.add(diag);
        } catch (IllegalValueException e) {
            logger.error("Attempt to set: "+diag.dump()+" with illegal value "+e.toString());
            return false;
        }

        Iterator iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
            String agentName = (String) iter.next();
            AssetID id = new AssetID(agentName, AssetType.findAssetType("Agent"));
            RequestToDisconnectAgentDiagnosis agentDiag = requestToDisconnectAgentDiagnosisIndex.getDiagnosis(id);
            try {
                agentDiag.setValue(request); // wants to disconnect
                originalDiagnoses.add(agentDiag);
                if (logger.isDebugEnabled()) logger.debug("DisconnectChange set "
                        +agentDiag.dump()+ " "+time
                        +" for the Coordinator");
                blackboard.publishChange(agentDiag);
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+agentDiag.dump()+" with illegal value "+e.toString());
                return false;
            }
            DisconnectAction action = disconnectActionIndex.getAction(id);
            try {
                action.setValuesOffered(whichToOffer);
                originalActions.add(action);
                agentVector.add(id);
                blackboard.publishChange(action);
                if (logger.isDebugEnabled()) logger.debug(action.dump());
            } catch (IllegalValueException e) {
                logger.error("Attempt to set: "+action.dump()+" with illegal value "+e.toString());
                return false;
            }
        }
        rr.setAlarm(createRequestedAlarm(rr, 120000.0));
        rr.setOriginalActions(originalActions);
        rr.setOriginalDiagnoses(originalDiagnoses);
        rr.setAgentVector(agentVector);
        rr.setRequest(response); 
        rr.setReconnectTime((long)time + System.currentTimeMillis());
        if (logger.isInfoEnabled()) { logger.info(rr.dump()); }
        return true;
    }


    private boolean propagatePermissions(RequestRecord rr) {

        if (logger.isDebugEnabled()) logger.debug("Starting propagateChange() for: " + rr.getNodeID().toString());
        
        DisconnectDefenseAgentEnabler enabler = DisconnectDefenseAgentEnabler.findOnBlackboard(rr.getNodeID().getType().toString(), rr.getNodeID().getName().toString(), blackboard);
        if (enabler == null) {
            logger.warn("could not find Enabler for" + rr.dump());
            return false;
        }
        else { // propagate the permission
            String request = rr.getRequest();
            AssetID nodeID = rr.getNodeID();
            Action.CompletionCode completionCode;
            String enablerValue;
            String finalState;
            if (request.equals(ALLOW_DISCONNECT)) {
                completionCode = Action.ACTIVE;
                enablerValue = "ENABLED";
                finalState = DISCONNECTED;
            }
            else {
                completionCode = Action.COMPLETED;
                enablerValue = "DISABLED";
                finalState = CONNECTED;
                cancelOverdueAlarm_Returned(nodeID);
            }
            // persist the knowledge Disconnect status change
            try {
                NodeStatusRecord nsr = new NodeStatusRecord(nodeID, rr.getAgentVector(), rr.getReconnectTime(), finalState);
                nodeStatus.put(nodeID, nsr);
                blackboard.persistNow();
            }
            catch (org.cougaar.core.persist.PersistenceNotEnabledException e)
            {
                logger.error(e.toString());
            }

            // tell the Node it can do what it requested
            enabler.setValue(enablerValue);
            blackboard.publishChange(enabler);
            if (logger.isDebugEnabled()) logger.debug("Sent back to Node: " + enabler.toString());

            // tell the Coord that all Actions have started & completed
            Iterator iter = rr.getOriginalActions().iterator();
            while (iter.hasNext()) {
                DisconnectAction action = (DisconnectAction)iter.next();
                try {
                    action.start(request);
                    action.stop(completionCode);
                    action.setValuesOffered(NULL_SET); // remove offers - this has the added effect that the action will be considered by the Coord to be irrecocable
                    if (logger.isDebugEnabled()) logger.debug("Setting Action: " +  action.dump());
                    blackboard.publishChange(action);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to start: "+action.dump()+" with illegal value "+e.toString());
                    return false;
                } catch (NoStartedActionException e) {
                    logger.error("Attempt to stop: "+action.dump()+" with illegal value "+e.toString());
                    return false;
                }
            }
            // tell the Coord the new Diagnosis states
            iter = rr.getOriginalDiagnoses().iterator();
            while (iter.hasNext()) {
                Diagnosis thisDiagnosis = (Diagnosis)iter.next();
                try {
                    thisDiagnosis.setValue(finalState);
                    blackboard.publishChange(thisDiagnosis);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+thisDiagnosis.toString()+" with illegal value "+e.toString());
                    return false;
                }
            }
            return true;
        }
    }

    private boolean denyPermissions(RequestRecord rr) {
        pendingRequests.remove(rr.getNodeID());
        rr.getAlarm().cancel();
        if (logger.isDebugEnabled()) logger.debug("Removing RequestedAlarm for: " + rr.getNodeID());

        DisconnectDefenseAgentEnabler enabler = DisconnectDefenseAgentEnabler.findOnBlackboard(rr.getNodeID().getType().toString(), rr.getNodeID().getName().toString(), blackboard);
        String request = rr.getRequest();
        AssetID nodeID = rr.getNodeID();

        if (enabler != null) {
            enabler.setValue(request.equals(ALLOW_DISCONNECT) ? "DISABLED" : "ENABLED");  // Tell the node it can NOT do what it requested
            blackboard.publishChange(enabler);
            // change the Diagnosis back to its value before the denied request
            Iterator iter = rr.getOriginalDiagnoses().iterator();
            while (iter.hasNext()) {
                Diagnosis diag = (Diagnosis)iter.next();
                try {
                    diag.setValue(rr.getRequest().equals(ALLOW_DISCONNECT) ? CONNECTED : DISCONNECTED);
                    blackboard.publishChange(diag);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+diag.toString()+" with illegal value "+e.toString());
                }
            }
            return true;
        }
        else {
            logger.warn("could not find Enabler for" + rr.dump());
            return false;
        } 
    }

    private void createOverdueAlarm(RequestToDisconnectNodeDiagnosis diag, ReconnectTimeCondition rtc, double time) {
        AgentVector agents = new AgentVector();
        Iterator iter = rtc.getAgents().iterator();
        while (iter.hasNext()) {
            agents.add(new AssetID((String)iter.next(), AssetType.findAssetType("Agent")));
        }
        createOverdueAlarm(diag, agents, time);
    }

    private void createOverdueAlarm(RequestToDisconnectNodeDiagnosis diag, AgentVector agents, double time) {
        OverdueAlarm overdueAlarm = new OverdueAlarm(diag, agents, time > 60000.0 ? (time + lateReportingForgiveness) : (60000.0 + lateReportingForgiveness));  // Don't monitor for less than 10 sec
        activeOverdueAlarms.put(diag.getAssetID(), overdueAlarm);
        getAlarmService().addRealTimeAlarm(overdueAlarm);     
        // nodeStatus.put(diag.getAssetID(), new NodeStatusRecord(diag.getAssetID(), agents, time, DISCONNECT_REQUEST));
        // blackboard.publishChange(nodeStatus);
        if (logger.isDebugEnabled()) logger.debug("Added alarm from handleNodeRequest()");
    }

    private void cancelOverdueAlarm_Returned(AssetID assetID) {
        OverdueAlarm overdueAlarm = (OverdueAlarm)activeOverdueAlarms.remove(assetID);
        if (overdueAlarm != null) {
            overdueAlarm.cancel();
            NodeStatusRecord nsr = (NodeStatusRecord)nodeStatus.get(assetID);
            nsr.setReconnectTime(0.0);
            nsr.setDiagnosis(CONNECTED);
            blackboard.publishChange(nodeStatus);
        }
    }

    private void cancelOverdueAlarm_Tardy(AssetID assetID) {
        OverdueAlarm overdueAlarm = (OverdueAlarm)activeOverdueAlarms.remove(assetID);
        if (overdueAlarm != null) {
            overdueAlarm.cancel();
            NodeStatusRecord nsr = (NodeStatusRecord)nodeStatus.get(assetID);
            nsr.setReconnectTime(-1.0);
            nsr.setDiagnosis(TARDY);
            blackboard.publishChange(nodeStatus);
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
                somethingOverdueExpired=true;
                expiredOverdueAlarms.add(this);
                blackboard.signalClientActivity();
            }
        }

        public void handleExpiration() {
            AssetID assetID = diag.getAssetID();
            if (logger.isDebugEnabled()) logger.debug("Alarm expired for: " + assetID + " with agents " + agentsAffected);
            DisconnectAction action = disconnectActionIndex.getAction(assetID);
            if (!action.getValue().isActive()) {
                if (logger.isDebugEnabled()) logger.debug("BUT - The Node made it back just before timing out");
                return;
            }
            else {
                if (eventService.isEventEnabled()) eventService.event(diag.getAssetID()+" is Tardy and is no longer legitimately Disconnected");
                try {
                    diag.setValue(TARDY);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+diag.toString()+" to illegal value " + TARDY);
                }
                try {
                    action.stop(Action.FAILED);
                } catch (IllegalValueException e) {
                    logger.error("Attempt to set: "+action.toString()+" to illegal value " + Action.FAILED);
                } catch (NoStartedActionException e) {
                    logger.error(e.toString());
                }
                blackboard.publishChange(diag);
                blackboard.publishChange(action);
                Iterator iter = agentsAffected.iterator();
                while (iter.hasNext()) {
                    AssetID agentID = (AssetID)iter.next();
                    RequestToDisconnectAgentDiagnosis agentDiag = requestToDisconnectAgentDiagnosisIndex.getDiagnosis(agentID);
                    DisconnectAction agentAction = disconnectActionIndex.getAction(agentID);
                    try {
                        agentDiag.setValue(TARDY);
                    } catch (IllegalValueException e) {
                        logger.error("Attempt to set: "+diag.toString()+" to illegal value " + TARDY);
                    }
                    try {
                        agentAction.stop(Action.FAILED);
                    } catch (IllegalValueException e) {
                        logger.error("Attempt to set: "+agentAction.toString()+" to illegal value " + Action.FAILED);
                    } catch (NoStartedActionException e) {
                    logger.error(e.toString());
                    }
                    blackboard.publishChange(agentDiag);
                    blackboard.publishChange(agentAction);
                }
                cancelOverdueAlarm_Tardy(assetID);
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
    

    private RequestedAlarm createRequestedAlarm(RequestRecord rr, double time) {
        RequestedAlarm requestedAlarm = new RequestedAlarm(rr, time);  
        getAlarmService().addRealTimeAlarm(requestedAlarm); 
        pendingRequests.put(rr.getNodeID(), rr);
        if (logger.isDebugEnabled()) logger.debug("Adding RequestedAlarm for: " + rr.getNodeID());
        return requestedAlarm;
    }

    
    public class RequestedAlarm implements Alarm {
        private long detonate;
        private boolean expired;
        private RequestRecord rr;
        
        public RequestedAlarm(RequestRecord rr, double t) {
            detonate = System.currentTimeMillis() + (long) t;
            this.rr = rr;
            if (logger.isDebugEnabled()) logger.debug("RequestedAlarm created : "+rr.toString() + " at time "+detonate + " for " + t/1000L + " seconds");
        }
        
        public long getExpirationTime() {return detonate;
        }
        
        public void expire() {
            if (!expired) {
                if (logger.isDebugEnabled()) logger.debug("expire(): RequestedAlarm expired for: " + rr.getNodeID());
                expired = true;
                somethingRequestedExpired=true;
                expiredRequestedAlarms.add(this);
                blackboard.signalClientActivity();
            }
        }

        public void handleExpiration() {
            if (logger.isInfoEnabled()) logger.info("RequestAlarm expired for: " + rr.toString() + ". Request denied");
            if (eventService.isEventEnabled()) eventService.event(rr.getNodeID()+" is denied permission to " + rr.getRequest()==ALLOW_DISCONNECT?DISCONNECT_REQUEST:CONNECT_REQUEST);
            denyPermissions(rr);
            cancel();
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
