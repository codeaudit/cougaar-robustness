/*
 * BelievabilityAction.java
 *
 * Created on June 8, 2004
 * <copyright>
 *  Copyright 2004 Telcordia Technoligies, Inc.
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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.AssetID;


/**
 * The class that contains a local copy of pertinent information
 * related to the action.
 * 
 * @author Tony Cassandra
 * @author Shilpa Bondale
 * @see org.cougaar.coordinator.Action
 * @see org.cougaar.coordinator.ActionRecord
 */


class BelievabilityAction extends BeliefUpdateTrigger {

    //---------------------------------------------------------------
    // package interface
    //---------------------------------------------------------------

    /**
     * Constructor, using an Action from the blackboard
     *
     * @param action The action from the blackboard
     */
    public BelievabilityAction( Action action, ActionTechSpecInterface tech) 
    {
        super( action.getAssetID() );
        _blackboard_action = action;
	_actuator_info = tech;
	_action_ts = _blackboard_action.getValue().getEndTime();
	_action_state_dim = _blackboard_action.getAssetStateDimensionName();
	_actuator_name = _actuator_info.getName();
        logError( "Got time stamp frmo Actuator Tech Spec Handle.\n BelievabilitAction constructor done." );
    } // constructor BelievabilityAction
    

    //---------------------------------------------------------------
    // public interface
    //---------------------------------------------------------------

    /**
     * Return the believability action as a string
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();

        // FIXME: Change this when body of this class is filled out.

        buff.append( "BelievabilityAction: ??? " );

        return buff.toString();
    } // method toString

    /**
    /**
     * Return the time that this trigger was asserted.  This should be
     * the time for which the belief update calculation should take
     * place.  The exact semantics and source for this time will be
     * dependent on the particular trigger.
     * @return timestamp
     **/
    public long getTriggerTimestamp() 
    {
        try {
	    logError( "Getting TriggerTimeStamp for action record :" + _blackboard_action.getValue().toString() );
	    return _action_ts; 
	} catch (Exception e){
	    logError( "Error getting Trigger Timestamp for the action record :" + _blackboard_action.getValue().toString());
	    return 0;
	}
    } // method getTriggerTimestamp


    /**
     * This routine should return the asset state dimension name that
     * this trigger pertains to.
     */
    public String getStateDimensionName()
    {
	try{
	    logError("Getting StateDimensionName for :" + _asset_id.toString()); 
	    return _action_state_dim;
	} catch (Exception e){
	    return null;
	}
    } // method getStateDimensionName
    
    /**
     * A string value indicating the current status of the Believability action taken by the actuator.
     *@return status of BelievabilityAction 
     */
    public String getBelivabilityActionStatus(){
	logError("Believability Action status for " + _asset_id.toString() + " is " + _blackboard_action.getValue().getCompletionCodeString());
	 return _blackboard_action.getValue().getCompletionCodeString();
    }
 
    /**
     *
     *
     */
    public boolean getBelievabilityActionSuccessStatus(){
	try {
	    if ((_blackboard_action.getValue().hasCompleted()) && 
		(_blackboard_action.getValue().getCompletionCodeString().compareToIgnoreCase("COMPLETED") == 0)){
		logError("Believability Action  for " + _asset_id.toString() + " is  successfully completed");
		return true;
	    } else {return false; } 
	} catch (Exception e){
	    return false;
	}
	
    }
    
    
    /**
     * Returns the name of the actuator that this action is a part of.
     * This comes from the getName() method of the
     * ActionTechSpecInterface object.
     * @return the actuator name
     */
     public String getActuatorName( ){
	 return _actuator_name;
     }
     
    /**
     * Returns the action value for the action. i.e., of all the
     * possible values the actuator offers (possible actions) which
     * one does this action correspond to.  This should be one of the
     * values returned by name() method on the ActionDescription
     * obejcts, where the getActions() method of the
     * ActionTechSpecInterface object gives you a vector of the
     * ActionDescriptions for the given actuator.
     * @param the action value name
     */
     public String getActionValue( ){
	 /*Vector actuator_actions = _actuator_info.getActions();
	 for (i=0; i< actuator_actions.size(); i++){
	     ActionDescription _descriptions = (ActionDescriptions) actuator_actions.elementAt(i);
	 }*/
	 return null;
     }
    
    //---------------------------------------------------------------
    // private interface
    //---------------------------------------------------------------

    // The object where the action information came from
    private Action _blackboard_action;
    //private ActionTechSpecInterface _action_tech;
    private long _action_ts;
    private ActionTechSpecInterface _actuator_info;
    private String _action_state_dim;
    private String actuator_name;
    private String _actuator_name;
    private AssetID _asset_id;

} // class BelievabilityAction

