/*
 * Action.java
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

package org.cougaar.coordinator;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.UIDService;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedHashSet;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.core.relay.Relay;

import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.core.component.ComponentDescription;

/**
 * This class is the superclass for all Action objects. 
 */
public abstract class Action 
       implements NotPersistable, Serializable, UniqueObject, Relay.Source, Relay.Target
{
    
    /** CompletionCode to indicate that an action has ended without the actuator calling stop() */
    static public final CompletionCode ENDED_UNKNOWN = new CompletionCode(4);
    
    /** CompletionCode to indicate that an action has completed */
    static public final CompletionCode COMPLETED = new CompletionCode(1);

    /** CompletionCode to indicate that an action has aborted */
    static public final CompletionCode ABORTED = new CompletionCode(2);

    /** CompletionCode to indicate that an action has failed */
    static public final CompletionCode FAILED = new CompletionCode(3);
    
    /** CompletionCode to indicate that an action has finished its setup stage and is actively working */
    static public final CompletionCode ACTIVE = new CompletionCode(5);

    /** TRUE if the class attributes have been initialized */
    private boolean inited = false;
    
    /** The asset type of this object */
    private  AssetType assetType = null;

    /** The possible values that getValue() can return */
    private Set possibleValues;
    
    /** TRUE if the class attributes have been initialized */
    private transient ServiceBroker serviceBroker = null;
        
    /** The ActionTechSpec for this action class */
    private transient ActionTechSpecInterface actionTechSpec = null;

    /** UID Service */
    private transient UIDService uidService;

    private transient Set newPermittedValues;

    /** 
     *  The address of the node agent. May change if the action is moved. 
     *  Making this transient will cause the target to go to null when this object moves.
     *  So, we will need to check for null each time & lookup the local node if necessary.
     */
    transient private  MessageAddress nodeId;
    
    /** The address of this agent */
    private  MessageAddress agentId;

    /** last action set by start() or stop() */
    ActionRecord lastAction = null;

    /** previous action set by start() or stop() */
    ActionRecord prevAction = null;
    
    /** UID for this relayable object */
    private UID uid = null;
    
    /** a single string identifier of this object, e.g. might be assetType:assetName */
    private AssetID assetID = null;

    //TRUE if the ActionRelayManager is on this agent (if so, this is the mgmt agent)
    boolean isActionRelayManagerLoadedInThisAgent = false;

    /** The last action set */
    
    /** Logger */
    transient Logger logger = null;
    
    /** Name of the asset that this object describes */
    private  String assetName = null;

    /** State Dimension name that this action affects */
    private  String assetStateDimensionName = null;
    
    /** The action values the coordinator permits the actuator to take */
    Set permittedValues = null;

    /** A cloned copy of the action values the coordinator permits the actuator to take */
    Set copyOfPermittedValues = null;
    
    /** The values the action offers to perform */
    Set valuesOffered = null;

    /**
     * Creates an action instance for actions to be performed on the specified asset
     */
    public Action(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException
    {

        logger = Logging.getLogger(getClass());         

        this.serviceBroker = serviceBroker;        
        this.assetName = assetName;

        this.permittedValues = new LinkedHashSet(); //initialize
        this.valuesOffered = new LinkedHashSet(); //initialize
        this.possibleValues = new LinkedHashSet(); //initialize

        //this.actionHistory = new ActionHistory(); //initialize
        if (!inited) { //only called once!
            init(); //get the possibleValues from the techSpec.
        }
        
        // get the diagnosis tech spec & load the possibleValues
        initPossibleValues();

        this.assetID = new AssetID(assetName, assetType);
        this.setUID(uidService.nextUID());
        
    }


    /**
     * Creates an action instance for actions to be performed on the specified asset. Includes initialiValuesOffered to
     * override the defaultValuesOffered specified in the techSpecs. initialValuesOffered CANNOT be null or an
     * IllegalValueException will be thrown.
     */
    public Action(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {
        
        this(assetName, serviceBroker);
        
        this.setValuesOffered(initialValuesOffered);

    }  
    
    // just called by create on the target
    public Action(Action action) {
	actionTechSpec = action.actionTechSpec;
	agentId = action.agentId;
	assetID = action.assetID;
	assetName = action.assetName;
	assetStateDimensionName = action.assetStateDimensionName;
	assetType = action.assetType;
        inited = action.inited;
	lastAction = action.lastAction;
	logger = Logging.getLogger(getClass());         
	newPermittedValues = null;
	nodeId = action.nodeId;
	permittedValues = new LinkedHashSet(action.permittedValues);
	possibleValues = action.possibleValues;
	prevAction = action.prevAction;
	serviceBroker = null;
	uid = action.uid;
	valuesOffered = action.valuesOffered;
    }
    
    /**
      * This method is intended to be called ONLY once when the first instance is created,
      * or more precisely, it is called as many times as required until the necessary attributes are
      * assigned values.
      */
    private synchronized boolean init() throws TechSpecNotFoundException {
     
        //Set up a vector to hold all found tech specs, so we can look them up later.
        //actionTechSpecs = new Vector();
        
        //set up the relay mechanism to the node level coordinator
        initSourceAndTarget(); 
        
        this.inited = true;

        logger = Logging.getLogger(getClass());
        
        return inited;
    }
    
    
    /**
     * Load the needed services, populate agentId and nodeId attrs, and
     * set up the relay mechanism
     */
    private void initSourceAndTarget() throws TechSpecNotFoundException {

        //** This is intended to be called once when the first instance is created.
        //** However, it can be called again if values are lost for some reason, e.g.
        //   the object is moved to another node & the target needs to be reset.
        
        if (serviceBroker == null) {
            throw new TechSpecNotFoundException("ServiceBroker is null.");            
        }
        
        if (agentId != null && nodeId != null)  { return; } //nothing to do.
        
        // **********************************************get the agentId
        AgentIdentificationService agentIdService =
                (AgentIdentificationService) serviceBroker.getService(this, AgentIdentificationService.class, null);
        if (agentIdService == null) {
            throw new RuntimeException(
            "Unable to obtain agent-id service");
        }
        this.agentId = agentIdService.getMessageAddress();

        serviceBroker.releaseService(this, AgentIdentificationService.class, agentIdService);
        if (agentId == null) {
            throw new RuntimeException(
            "Unable to obtain agent id");
        }

        
        // **********************************************get the nodeId
        NodeIdentificationService nodeIdService = (NodeIdentificationService)
            serviceBroker.getService(this, NodeIdentificationService.class, null);
        if (nodeIdService == null) {
            throw new RuntimeException("Unable to obtain node-id service");
        }
        nodeId = nodeIdService.getMessageAddress();
        serviceBroker.releaseService(this, NodeIdentificationService.class, nodeIdService);
        if (nodeId == null) {
            throw new RuntimeException(
            "Unable to obtain agent id");
        }
        
        this.uidService = (UIDService) 
            serviceBroker.getService( this, UIDService.class, null);
        if (uidService == null) {
            throw new RuntimeException(
            "Unable to obtain UIDService");
        }
        

        AgentContainmentService acs =
        (AgentContainmentService)serviceBroker.getService(this, org.cougaar.core.service.AgentContainmentService.class, null);
        ComponentDescription cd = new
            ComponentDescription("org.cougaar.coordinator.ActionRelayManager", // component name
                "Node.AgentManager.Agent.PluginManager.Plugin",    // insertion point
                "org.cougaar.coordinator.ActionRelayManager",      // component classname
            null,null,null,null,null);
        isActionRelayManagerLoadedInThisAgent = acs.contains(cd);
        
    }

    /**
     * Retrieves the ActionTechSpec for this class from the ActionTechSpecService.
     * Then it populates the possibleValues & assetType
     */
    private void initPossibleValues() throws TechSpecNotFoundException {
            
        if (serviceBroker == null) {
            throw new TechSpecNotFoundException("ServiceBroker is null.");            
        }
        
        //Don't look up the techspecs if we have already successfully done so
        if (actionTechSpec != null && this.possibleValues != null) { return; }
                
        if (!serviceBroker.hasService( ActionTechSpecService.class )) {
            throw new TechSpecNotFoundException("TechSpec Service not available.");            
        }
        
        // **********************************************get the tech spec service
        ActionTechSpecService ActionTechSpecService =
                (ActionTechSpecService) serviceBroker.getService(this, ActionTechSpecService.class, null);
        if (ActionTechSpecService == null) {
            throw new TechSpecNotFoundException(
            "Unable to obtain tech spec service");
        }
      
        //call tech spec service & get action tech spec        
        actionTechSpec = (ActionTechSpecInterface) ActionTechSpecService.getActionTechSpec(this.getClass().getName());
        if (actionTechSpec == null) {
            throw new TechSpecNotFoundException("Cannot find Action Tech Spec for "+ this.getClass().getName() );
        }        
        this.setPossibleValues(actionTechSpec.getPossibleValues());
        this.assetType = actionTechSpec.getAssetType();
        this.assetStateDimensionName = actionTechSpec.getStateDimension().getStateName();
        
        
        LinkedHashSet defaults = new LinkedHashSet(); 
        //get the initial values offered set
        Vector a = actionTechSpec.getActions();
        if (a != null) {
            Iterator i = a.iterator();
            while (i.hasNext()) {
                ActionDescription ad = (ActionDescription)i.next();
                if (ad.isDefaultAction()) {
                    defaults.add(this.getValueFromXML(ad.name()) );
                }
            }
        }
        try {
            this.setValuesOffered(defaults);
        } catch (Exception e) {
            logger.warn("Tried to set default actions offered, but an exception occurred: "+e, e);
        }
        
        serviceBroker.releaseService(this, ActionTechSpecService.class, ActionTechSpecService);
        
    }
    
    
    
    //*************************************************** PossibleValues
    /**
     *     Not for public viewing / access. For use only at init time. Sets the values retrieved by tech spec.
     *      The set is expected to be a set of strings.
     */
    private void setPossibleValues ( Set values) 
    {
    
        Iterator i = values.iterator();
        Object o;
        //boolean canClone = true;
        possibleValues.clear();
        while (i.hasNext()) {
            o = getValueFromXML((String) i.next());
            possibleValues.add(o);
            
            //** Cannot call clone() on Object type -- we'd need to know the actual type, so
            // the implementing subclass would need to do this...
            //Now store clone of object in a duplicate set, so others can see but not modify the original
            //if (canClone) { //if() used to avoid throwing more than one exception if object cannot be cloned.
            //    try {
            //        possibleValuesCloned.add(o.clone());
            //    } catch (CloneNotSupportedException cns) { //implementing cloneable does not guarantee the object supports clone()!
            //        canClone = false;
            //        possibleValuesCloned.add(o);
            //    }
            //} else {
            //    possibleValuesCloned.add(o);
            //}
        }
        
    }

    /**
     * Return an object for the possibleValue string used in the techSpec. 
     * This could be just this String, or it could be parsed into an Integer, etc.
     * It could also be an ID for some object defined in your class or elsewhere.
     *
     * The default behavior is to return the String that was passed in. Override
     * this behavior if you want something else to be returned.
     *
     */
    public Object getValueFromXML(String value) {
        
        return value;
    }
    
    
    /**
     * Get the full set of possible parameterizations of the Action (e.g. 0 gal, 1 gal, 2 gal, 5 gal, 10 gal, 20 gal), as specified in the TechSpec.
     * @return the (cloned) set of possible values that this Action can return from the getValue() method.
     */
    public Set getPossibleValues ( ) { return this.possibleValues; }
    

    //***************************************************ValuesOffered

    /**
     * Set a subset of the PossibleValues that the Actuator is offering to execute at the present time 
     * (e.g. 0 gal, 1 gal, 2 gal, 5 gal).  
     *     Not for public viewing / access - should only be called by the action/actuator.
     */
    protected void setValuesOffered( Set values) throws IllegalValueException {

        if (values == null) {
            throw new IllegalValueException("setValuesOffered must not be called with a null value. An empty set IS permitted.");
        }

        //Empty the valuesOffered set
        valuesOffered.clear();
        
        Set newValuesOffered = new LinkedHashSet(values);
        Object o;
        Iterator i = newValuesOffered.iterator();
        while (i.hasNext()) {
            o = i.next() ;
            if ( possibleValues.contains(o) ) {
                valuesOffered.add(o);
            } else {
                throw new IllegalValueException("The following value is not a permitted value: " + o.toString() );
            }
        }
    }
    
   
    /**
     * @return the subset of the PossibleValues that the Actuator is offering to execute at the 
     * present time (e.g. 0 gal, 1 gal, 2 gal, 5 gal).
     */
    public Set getValuesOffered ( ) { return valuesOffered; }

    
    //***************************************************GetValue, Start, Stop
    /**
     * Set the value of the action to be started. Unless otherwise noted this should 
     * only be called by the actuator/action. The value must be in the set of permittedValues.
     * Calling this method causes an ActionRecord to be created. The ActionRecord holds information
     * about the action started, the start time, end time, and completion code.
     * @see ActionRecord
     *
     * <p>
     * <b>Call stop() when the action is complete, failed, or aborted.</b> This will cause the last ActionRecord
     * to be updated, describing when the action was completed/failed or aborted.
     * <p>
     *
     * If start() is called twice in a row without calling stop, the last ActionRecord will be
     * updated for the previous start action that shows the completion status as ENDED_UNKNOWN, and the 
     * time will be set to the time of the current call to start().
     *
     * @exception IllegalValueException if the actionValue is not in the set of permittedValues
     * 
     */
    protected void start(Object actionValue) throws IllegalValueException {
        
        if (actionValue == null) {
            throw new IllegalValueException ("Action to be started is NULL!");
        }
        
        if ( !permittedValues.contains(actionValue) ) {
	    if (logger.isDebugEnabled()) logger.debug("actionValue="+actionValue+" not in permittedValues="+permittedValues);
            throw new IllegalValueException ("Action to be started is not in permittedValues set.");
        }

        if (lastAction != null && !lastAction.hasCompleted()) { //then the actuator didn't call stop, so update Last action
            lastAction.setCompletionCode( this.ENDED_UNKNOWN, System.currentTimeMillis() );
        } 

        prevAction = lastAction;
        lastAction = new ActionRecord(actionValue, System.currentTimeMillis());
    }

    /**
     * Call to indicate that an action that was started has finished. The CompletionCode defaults to
     * COMPLETED. Use {@link #stop(Action.CompletionCode) stop(CompletionCode)} to set a different CompletionCode.
     *
     * @exception NoStartedActionException if this method is not called AFTER a call to {@link Action#start start()}.
     *
     * @see Action#COMPLETED
     * @see Action#FAILED
     * @see Action#ABORTED
     */
    protected void stop() throws NoStartedActionException {
        
        try {
            stop(Action.COMPLETED );
        } catch (IllegalValueException ive) {} //cannot happen
    }

    /**
     * Call to indicate that an action that was started has finished. The CompletionCode can
     * be COMPLETED, ABORTED, FAILED, or ACTIVE.
     *<p>
     * @exception IllegalValueException if the completionCode is not Action.COMPLETED, Action.FAILED, or Action.ABORTED.
     * @exception NoStartedActionException if this method is not called AFTER a call to {@link Action#start start()}.
     * @see Action#COMPLETED
     * @see Action#FAILED
     * @see Action#ABORTED
     */
    protected void stop(CompletionCode completionCode) throws IllegalValueException, NoStartedActionException  {
        
        if (completionCode != Action.COMPLETED && 
            completionCode != Action.ABORTED &&
            completionCode != Action.FAILED &&
            completionCode != Action.ACTIVE ) {
                
            throw new IllegalValueException("Illegal completionCode: " + completionCode);
        }
                
        if ( lastAction == null || lastAction.hasCompleted() ) {

            throw new NoStartedActionException("Last action was: " + lastAction);            
        }
        
        //Update action record since this action is being stopped.
        lastAction.setCompletionCode(completionCode, System.currentTimeMillis() );
    }
    
    /**
     * Get the ActionRecord recording the last action set by the Actuator, representing what it is actually 
     * executing at the present time (e.g. "2 gal").  
     */
    public ActionRecord getValue ( ) { return lastAction; }

    /**
     * Get the ActionRecord recording the previous action set by the Actuator. The previous value
     * could be an implicit stop action if start() was called twice in a row 
     * @see Action#start(Object)
     */
    public ActionRecord getPreviousValue ( ) { return prevAction; }
    
    //***************************************************PermittedValues
    /*
     * Get the (cloned) subset of the PossibleValues that the Coordinator has permitted the Actuator to execute 
     * at the present time  (e.g. 0 gal, 1 gal, 2 gal).  
     * These values are set by the Coordinator.
     */
    public Set getPermittedValues ( ) { return permittedValues; }    
    
    /**
     * The coordinator sets this. 
     * @see #getPermittedValues
     */
    public void setPermittedValues (Set values) throws IllegalValueException { 

        if (logger == null) {
            logger = Logging.getLogger(getClass());         
        }
        Object o;
        Iterator i = values.iterator();
        permittedValues = new LinkedHashSet(); //eliminate chance that this method was called with permittedValues gotten from getPermittedValues().
	newPermittedValues = permittedValues;  // to support case where ActionsWrapper is local and not relayed.
        while (i.hasNext()) {
            o = i.next();
            if ( possibleValues.contains(o) ) {
                permittedValues.add(o);
            } else {
                throw new IllegalValueException("The following value is not a permitted value: " + o.toString() );
            }
        }
    }
    
    
    //***************************************************Miscellaneous

    /**
     * @return the asset type related to this defense condition
     */
    AssetType getAssetType() { return assetType; }
    
    /**
     * @return Get the name of the asset (e.g. "123-MSB") that this action controls
     */
    public String getAssetName() { return assetName; }
    
    /**
     * @return the asset name related to this diagnosis (e.g. "123-MSB")
     */
    public String getAssetStateDimensionName() { return assetStateDimensionName; }

   /**
     * @return AssetID
     */
    public AssetID getAssetID() { return assetID; }

    /**
     * @return the (read-only) tech spec for this action type
     */
    public ActionTechSpecInterface getTechSpec ( ) { return actionTechSpec; }

    /**
     * @return a string representing an instance of this class of the form "<classname:assetType:assetName=value>"
     */
    public String toString ( ) { return "<" + getClass().getName()+'@'+Integer.toHexString(hashCode())+":" +
                                              assetType + ":" + 
                                              assetName + "=" + 
                                              permittedValues + "/" +
                                              (lastAction != null ? lastAction.toString() : "NULL" ) + ">";
    }

    /**
     * Returns a verbose pretty-printed representation for an Action.
     */
    public String dump() {
	return "\n" +
            "<"+getClass().getName()+'@'+Integer.toHexString(hashCode()) + "\n" +
	    "   assetID = " + assetID + "\n" +
	    "   assetName = " + assetName + "\n" +
	    "   assetType = " + assetType + "\n" +
	    "   assetStateDimensionName = " + assetStateDimensionName + "\n" +
	    "   agentId = " + agentId + "\n" +
	    "   nodeId = " + nodeId + "\n" +
	    "   possibleValues = " + possibleValues + "\n" +
	    "   valuesOffered = " + valuesOffered + "\n" +
	    "   permittedValues = " + permittedValues + "\n" +
	    "   newPermittedValues = " + newPermittedValues + "\n" +
	    "   copyOfPermittedValues = " + copyOfPermittedValues + "\n" +
	    "   lastAction = " + lastAction + "\n" +
	    "   prevAction = " + prevAction + "\n" +
//          "   actionTechSpec = " + actionTechSpec + "\n" +
	    "   inited = " + inited + "\n" +
	    "   isActionRelayManagerLoadedInThisAgent = " + isActionRelayManagerLoadedInThisAgent + "\n" +
            "   wrapper = " + wrapper + "\n" +
	    "   uid = " + uid + "\n" +
	    "   source = " + getSource() + "\n" +
	    "   targets = " + getTargets() + "\n" +
	    "   content = " + getContent() + "\n" +
	    "   response = " + getResponse() + ">";
    }
    
   /** UniqueObject implementation */
    public org.cougaar.core.util.UID getUID() {
        return uid;
    }
    
    /**
     * Set the UID (unique identifier) of this UniqueObject. Used only
     * during initialization.
     * @param uid the UID to be given to this
     **/
    public void setUID(UID uid) {
        if (this.uid != null) throw new RuntimeException("Attempt to change UID");
        this.uid = uid;
    }
    
    
    //*******************************compare routines*******************************************
    //Unsure if still needed, but likely
    
    //boolean compareSignature(String assetID, String defenseName) {
    //    Logger logger = Logging.getLogger(getClass());
    //    if (logger.isDebugEnabled()) logger.debug("DefOpMode/compareSignature");
    //    return ((this.getActuatorName().equals(defenseName)) &&
    //    (this.getassetID().equals(assetID)));
    // }
    
    //boolean compareSignature(String type, String id, String defenseName) {
    //    return ((this.assetType.equals(type)) &&
    //    (this.assetName.equals(id)) &&
    //    (this.defenseName.equals(defenseName)));
    //}
    
    /** Compares equality based upon UID */    
    public boolean compareSignature(UID uid) {
        return ((uid.equals(this.getUID())));
    }
    
    
    //*******************************relay routines*******************************************
    
    
    // Relay.Source implementation -------------------------------------------
    /**
     * Get all the addresses of the target agents to which this Relay
     * should be sent. For this implementation this is always a
     * singleton set contain just one target.
     **/
    public Set getTargets() {
        if (logger == null) {
            logger = Logging.getLogger(getClass());         
        }
//        return Collections.singleton(agentId);
        if (!isActionRelayManagerLoadedInThisAgent)
            return Collections.singleton(nodeId);
        else 
            return Collections.singleton(agentId); //i.e., don't relay anywhere
    }
    
    /**
     * Get an object representing the value of this Relay suitable
     * for transmission. This implementation uses itself to represent
     * its Content.
     **/
    public Object getContent() {
        return this;
    }
    
    private static final class SimpleRelayFactory
	implements TargetFactory, java.io.Serializable {

	public static final SimpleRelayFactory INSTANCE = 
	    new SimpleRelayFactory();

	private SimpleRelayFactory() {}
	
	/**
	 * Convert the given content and related information into a Target
	 * that will be published on the target's blackboard. 
	 **/
	public Relay.Target create(UID uid, 
				   MessageAddress source, 
				   Object content,
				   Token token) {
	    try {
		Class c = content.getClass();
		Class [] classes = new Class[1];
		classes[0] = c;
		Object[] args = new Object[1];
		args[0] = (Action)content;
		Object o = c.getConstructor(classes).newInstance(args);
		return (Relay.Target)o;
	    } catch (Exception e) {
		e.printStackTrace();	
	    }
	    return null;
	}
	
	private Object readResolve() {
	    return INSTANCE;
	}
    };

    /**
     * Get a factory for creating the target. 
     */
    public TargetFactory getTargetFactory() {
	return SimpleRelayFactory.INSTANCE;
    }
    
    /**
     * Receive the new PermittedValues that was sent from the target.
     **/
    public int updateResponse(MessageAddress target, Object response)  {
	if (logger.isDebugEnabled())logger.debug("updateResponse: r="+response+",a="+this);        
	if (!this.getPossibleValues().equals(newPermittedValues)) {
	    newPermittedValues = (Set)response;
	    return Relay.RESPONSE_CHANGE;	  
	}
	return Relay.NO_CHANGE;
    }   

    public Set getNewPermittedValues() {
	return newPermittedValues;
    }

    public void clearNewPermittedValues() {
	newPermittedValues = null;
    }
    
    // Relay.Target implementation -----------------------------------------
    /**
     * Get the address of the Agent holding the Source copy of
     * this Relay.
     **/
    public MessageAddress getSource() {
        return agentId;
    }
    
    /**
     * Only send the permittedValues back to the source.
     **/
    public Object getResponse() {
        return getPermittedValues();
    }
    
    /**
     * Update target with new content.
     * @return true if the update changed the Relay. The LP should
     * publishChange the Relay.
     **/
    public int updateContent(Object content, Relay.Token token) {
        Action a = (Action) content;
        
        //At this pt it only propagates the getValue() attribute
        this.lastAction = a.getValue(); //don't call setValue() it'll run add'l code.
        this.prevAction = a.getPreviousValue(); 
        this.valuesOffered = a.getValuesOffered();
        return Relay.CONTENT_CHANGE;
    }
    
    
    //*******************************Action Wrapper routines*************************************
    /**
     * The ActionsWrapper that is wrapping this object
     **/
    ActionsWrapper wrapper = null;
    /**
     * @return the ActionsWrapper that is wrapping this diagnosis
     **/
    public ActionsWrapper getWrapper() {
        return wrapper;
    }
    
    /**
     * Set the ActionsWrapper that is wrapping this diagnosis
     **/
    void setWrapper(ActionsWrapper w) {
        wrapper = w;
    }
    
    
    //*******************************Action History tracking routines*************************************

    /** Used to ensure that only this type is used to set the action code */
    public static class CompletionCode implements Serializable {
    
        private int val;
        protected CompletionCode(int i) { this.val = i; }
        private int getVal() { return val; }

        public String toString() {
            switch (val)
                { 
                    case 1: return "Completed";
                    case 2: return "Aborted";
                    case 3: return "Failed";
                    case 4: return "Ended_Unknown";
                    case 5: return "Active";
                }
                return null;
        }

        public boolean equals(CompletionCode cc) {
            return (this.getVal() == cc.getVal());
        }

    }
    
    
    
    /**
     * The history of what actions have taken place. A vector of ActionRecords
     */
    class ActionHistory extends Vector {
     
    }
    
    
}
