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

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.core.agent.service.alarm.Alarm;


import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.LinkedList;
import java.sql.Timestamp;


/**
 * CHANGE ALL PRIVATE VARS TO START WITH _
 * Contains, and updates the StateEstimation for a 
 * fixed (sliding) window of time, based on incoming BelievabilityDiagnosis.
 * @author Shilpa Bondale
 */
 public class BeliefStateWindow implements BelievabilityDiagnosisConsumerInterface, Alarm {
      /**
      * Initial BeliefState - set in construstor.
      */
     private BeliefState _apriori_state;     
     /**`	
      * AssetID & AssetType
      */
     private AssetID _asset_id;
     private AssetType _asset_type;
     /**
      * Handle for POMDPModelInterface on Diagnosis receipt updated Belief State is obtained from POMDPModelInterce 
      */
     private POMDPModelInterface _pmdp_mi;
     /**
      * _se_pub is an obejct for now should be substituted by StateEstimationPublisher
      */
     private StateEstimationPublisher _se_pub;
     /**
      * Determines the delta between start time and end time of the AssetStateWindow,
      * where start time is always shifting ahead.
      * window length is gathered based on the latency for each sensor associated with the AssetModelType.
      * - set in constructor.
      */
      private double _max_window_length;
      /**
       * Diagnosis and BeliefState linkkedlist
       */
      private LinkedList _bst_queue;
      private LinkedList _diag_queue;
     /**
      * Constructor, sets the length of window. AssetStateWindow is created from AssetModel
      * @param ast_model an instance of AssteModel which represents thresholds for an asset.
      */
     public BeliefStateWindow(AssetID ast_id, double max_diag_latency, POMDPModelInterface _pmdp_mif, StateEstimationPublisher _se_publisher)
     	throws BelievabilityException {
	    // Argument assignment 
	    try {
		_se_pub = _se_publisher;
		_asset_id = ast_id;
		_asset_type = _asset_id.getType();
		_max_window_length = max_diag_latency;
		_pmdp_mi =  _pmdp_mif;
	    } catch (Exception e){
		throw new BelievabilityException("BeliefStateWindow - constructor","Argument assignment error");
	    }
	    // Get defualt BeliefState from POMDPModel, set it as first element of the linked list and apply a timer to it.
	    // this try block can be in consumeDiagnosis
	    try {
		_apriori_state = _pmdp_mi.getInitialBeliefState(_asset_type);
		_apriori_state.setAssetID(ast_id);  //set assetid for retrieved initial belief state
		//Belief State queue
		_bst_queue = new LinkedList();
		_bst_queue.addFirst(_apriori_state);
		//Diagosis queue
		_diag_queue = new LinkedList();
		_diag_queue.addFirst(null);
		
		try {
		    this.publishBeliefState(_apriori_state);
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
		_expiration_time = _start_time + (long)_max_window_length;
	    } catch (Exception e){
		throw new BelievabilityException("BeliefStateWindow - constructor" , "Error setting timer");
	    }
	}
     /**
      * Post receipt of of diagnosis, updated BeliefState is published to the StateEstimationPublisher.
      * SEPublisher may or may not actuallly decide to publish the updated BeliefState
      * This method can be private
      */
     public void publishBeliefState(BeliefState current_belife_state) throws BelievabilityException {
	 _se_pub.consumeBeliefState(_apriori_state);
     }
     /**
      * StateEstimationPublisher may request BeliefState from BeliefStateWindow.
      *@param BeliefState a dummy BeliefState for now 
      *@return BeliefState an updated BeliefState from POMDPModel interface
      *@throws BelievabilityException
      * this function may require to accept AssetID as a parameter if/when the class becomes multithreaded
      */
     public BeliefState getCurrentBeliefState() throws BelievabilityException {
	 return _apriori_state;
     }
     /**
      *@return the length of AssetStateWindow
      *@throws BelievabilityException
      */
      public double getWindowLength() throws BelievabilityException{
	  return _max_window_length;
      }
      
     /**
      * sets the length of AssetStateWindow 
      *@throws BelievabilityException
      */
       public void setWindowLength () throws BelievabilityException{
	   // null
       }
     /**
      * closeWindow() also gets called by the distructor for AssetStateWindow
      * Releases/resets  all the assets such as StateEstimation linked list,_max_window_length.
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
     public void consumeBelievabilityDiagnosis (BelievabilityDiagnosis bb_diagnosis) throws BelievabilityException{
	 try {
	     //temporary--remove this 
	     //this.publishBeliefState(_apriori_state);
	     
	     BeliefState current_belief_state = null;
	     
	     //Pass the BelievabilityDiagnosis to POMDPModelInterface
	     current_belief_state = _pmdp_mi.updateBeliefState(_apriori_state,bb_diagnosis);
	     
	     //insert current belief state to believability queue
	     insertBeliefState(current_belief_state, bb_diagnosis);
	     
	     //remove the oldest belief state & publish current belief state
	     if (_bst_queue.size() != _diag_queue.size()) {
		 throw new BelievabilityException ("BeliefStateWindow.consumeBelievabilityDiagnosis","Diagnosis:BeliefState coordianation mismatched");
	     }
	     if (_bst_queue.size() > 1){
		     _bst_queue.removeFirst();
		     _diag_queue.removeFirst();
	     }
	     this.publishBeliefState(current_belief_state);
	 } catch (BelievabilityException be){
	     throw new BelievabilityException ("BeliefStateWindow.consumeBelievabilityDiagnosis", be.getMessage());	
	 } catch (Exception e){
	     throw new BelievabilityException ("BeliefStateWindow.consumeBelievabilityDiagnosis", e.getMessage());
	 }
     }
     
     private void insertBeliefState(BeliefState current_belief_state, BelievabilityDiagnosis bb_diagnosis){
	 //Veirfy its placement in the queue based on the timestamp
	 int insert_position = _bst_queue.size();
	 for (int i = 0; i < insert_position; i++){
	     BeliefState temp_state = (BeliefState)_bst_queue.get(i);
	     if (current_belief_state.getTimestamp() < temp_state.getTimestamp()){
		 insert_position = i;
		 break;
	     }
	 }
	 _diag_queue.add(insert_position,bb_diagnosis);
	 _bst_queue.add(insert_position,current_belief_state); //Java linkedlist automatically shifts subsequent elements right  and adjusts respective indices
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
    /**
     * Here the alarm has expired. Perform following set of actions
     * Check how many diagnosis & belief state estimation elements exist.
     * If there is more than one diagnosis/beliefstate pair, then discard all the pairs that have 
     * time stamp earlier than current system time. 
     */
      public void expire () {
	  try {
	      //Check how many belief state estimation(s) exists.
	      if (_bst_queue.size() <= 1){
		  // do nothing??
	      } else {
		  //Check how many belief states have timestamp prior to current time.
		  //Timestamp cur_time = new Timestamp(System.currentTimeMillis());
		  for (int i = 0; i < _bst_queue.size(); i++){
		      BeliefState temp_state = (BeliefState) _bst_queue.get(i);
		      BelievabilityDiagnosis temp_bb_diagnosis = (BelievabilityDiagnosis) _diag_queue.get(i);
		      if( _expiration_time < temp_state.getTimestamp()){
			  //If Belief State is timestamped later than current time then update the belief state, with the associated diagnosis.
			  //The belief state itself is of no use here because, the creation/updation of belief state is done based on diagnosis.
			  // no need for new belief state -- reuse temp_state
			  BeliefState current_belief_state = _pmdp_mi.updateBeliefState(temp_state,temp_bb_diagnosis);
			  this.insertBeliefState(current_belief_state,temp_bb_diagnosis);
		      }
		  }
	      }
	  }
	  catch (Exception e){
	      _expired = false;
	      System.out.println("Error in BeliefStateWindow.expire method."); 
	      // Cannot throw an exception here...
	  }
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


