/**
 * BeliefStateWindow.java
 *
 * Created on April 30, 2004
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

import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosisUtils;
import org.cougaar.core.agent.service.alarm.Alarm;


import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.lang.Object.*;
import java.util.LinkedList;


/**
 * Contains, and updates the StateEstimation for a 
 * fixed (sliding) window of time, based on incoming BelievabilityDiagnosis.
 * @author Shilpa Bondale
 */
 public class BeliefStateWindow implements BelievabilityDiagnosisConsumerInterface, Alarm {
      /**
      * Initial BeliefState - set in construstor.
      */
     BeliefState apriori_state;     
     /**`	
      * AssetID & AssetType
      */
     AssetID asset_id;
     AssetType asset_type;
     /**
      * Handle for POMDPModelInterface on Diagnosis receipt updated Belief State is obtained from POMDPModelInterce 
      */
     POMDPModelInterface pmdp_mi;
     /**
      * se_pub is an obejct for now should be substituted by StateEstimationPublisher
      */
     StateEstimationPublisher se_pub;
     /**
      * Determines the delta between start time and end time of the AssetStateWindow,
      * where start time is always shifting ahead.
      * window length is gathered based on the latency for each sensor associated with the AssetModelType.
      * - set in constructor.
      */
      double max_window_length;
      /**
       * BeliefState linkkedlist
       */
      LinkedList bst_queue;
      /**
       * Believability Dignosis local copy
       */
       BelievabilityDiagnosis bb_diag; 
      
     /**
      * Constructor, sets the length of window. AssetStateWindow is created from AssetModel
      * @param ast_model an instance of AssteModel which represents thresholds for an asset.
      */
     public BeliefStateWindow(AssetID ast_id, double max_diag_latency, POMDPModelInterface pmdp_mif, StateEstimationPublisher se_publisher)
     	throws BelievabilityException {
	    // Argument assignment 
	    try {
		se_pub = se_publisher;
		asset_id = ast_id;
		asset_type = asset_id.getType();
		max_window_length = max_diag_latency;
		pmdp_mi =  pmdp_mif;
	    } catch (Exception e){
		throw new BelievabilityException("BeliefStateWindow - constructor","Argument assignment error");
	    }
	    // Get defualt BeliefState from POMDPModel, set it as first element of the linked list and apply a timer to it.
	    // this try block can be in consumeDiagnosis
	    try {
		apriori_state = pmdp_mi.getInitialBeliefState(asset_type);
		apriori_state.setAssetID(ast_id);  //set assetid for retrieved initial belief state
		bst_queue = new LinkedList();
		bst_queue.addFirst(apriori_state);
		try {
		    this.publishBeliefState(apriori_state);
		} catch (BelievabilityException e){
		    throw new BelievabilityException("BeliefStateWindow - constructor","Error publishing to the StateEstimation Publisher " + e.getMessage());
		}
	    } catch (Exception e){
		throw new BelievabilityException("BeliefStateWindow - constructor","Error retrieving and//or publishing initial BeliefState " + e.getMessage());
	    }
	    // Set cougaar timer options.
	    try {
		_expired = false;
		_start_time = System.currentTimeMillis();
		_expiration_time = _start_time + (long)max_window_length;
	    } catch (Exception e){
		throw new BelievabilityException("BeliefStateWindow - constructor" , "Error setting timer");
	    }
	}
     /**
      * Post receipt of of diagnosis, updated BeliefState is published to the StateEstimationPublisher.
      * SEPublisher may or may not actuallly decide to publish the updated BeliefState
      * Will I have a handle to SEPublisher???
      * This method can be private
      */
     public void publishBeliefState(BeliefState current_belife_state) throws BelievabilityException {
	 se_pub.consumeBeliefState(apriori_state);
     }
     /**
      * StateEstimationPublisher may request BeliefState from BeliefStateWindow.
      *@param BeliefState a dummy BeliefState for now 
      *@return BeliefState an updated BeliefState from POMDPModel interface
      *@throws BelievabilityException
      * this function may require to accept AssetID as a parameter if/when the class becomes multithreaded
      */
     public BeliefState getBeliefState(BeliefState assumed_belief_state) throws BelievabilityException {
	 return apriori_state;
	 //null
     }
     /**
      *@return the length of AssetStateWindow
      *@throws BelievabilityException
      */
      public double getWindowLength() throws BelievabilityException{
	  return max_window_length;
      }
      
     /**
      * sets the length of AssetStateWindow 
      */
       public void setWindowLength (){
	   // null
       }
     /**
      * closeWindow() also gets called by the distructor for AssetStateWindow
      * Releases/resets  all the assets such as StateEstimation linked list,max_window_length.
      * @param AssetModel creates and closes the BeliefStateWindow.
      * @throws BelievabilityException
      */
     public void closeWindow(AssetModel ast_model)throws BelievabilityException {
	//null 
     }
     
     /**
      * @param BelievabilityDiagnosis is passed as an arguement from DiagnosisConsumer which intern receives the diagnosis from the BlackBoard.
      * @throws BelievabilityException
      * Impl. for abstract class BelievabilityDiagnosisConsumerInterface
      */
     public void consumeBelievabilityDiagnosis (BelievabilityDiagnosis bb_diagnosis) throws BelievabilityException {
	 try {
	 //temporary--remove this 
	 //this.publishBeliefState(apriori_state);
	 
	 BeliefState current_belief_state = null;
	 
	 //Pass the BelievabilityDiagnosis to POMDPModelInterface
	 current_belief_state = pmdp_mi.updateBeliefState(apriori_state,bb_diagnosis);
	 
	 //insert current belief state to believability queue
	 insertBeliefState(current_belief_state);
	 
	 //remove the oldest belief state & publish current belief state
	 if (bst_queue.size() > 1){
	 	 bst_queue.removeFirst();
	 }
	 this.publishBeliefState(current_belief_state);
	 
	 } catch (Exception e){
	     throw new BelievabilityException ("BeliefStateWindow.consumeBelievabilityDiagnosis",e.getMessage());
	 }
     }
     
     private void insertBeliefState(BeliefState current_belief_state){
	 //Veirfy its placement in the queue based on the timestamp
	 int insert_position = bst_queue.size();
	 for (int i = 0; i < insert_position; i++){
	     BeliefState temp_state = (BeliefState)bst_queue.get(i);
	     if (current_belief_state.getTimestamp() < temp_state.getTimestamp()){
		 insert_position = i;
		 break;
	     }
	 }
	 bst_queue.add(insert_position,current_belief_state); //Java linkedlist automatically shifts subsequent elements right  and adjusts respective indices
     }
     /**
      * implementation of methods for Alarm interface
      */
     public boolean cancel () {
	 return true;
     }
     public boolean hasExpired () {
	return true;
    }
      public void expire () {
	  //null
    }
    public long getExpirationTime () {
	return 0;
    }
    
    // The start time of the alarm
    private long _start_time;

    // The expiration time of the alarm
    private long _expiration_time;

    // An indicator that the alarm has been canceled.
    private boolean _canceled = false;

    // An indicator that the alarm has expired.
    private boolean _expired = false;
 }


