/**
 * AssetStateWindow.java
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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.lang.Object.*;


/**
 * Contains, and updates the StateEstimation for a 
 * fixed (sliding) window of time, based on incoming BelievabilityDiagnosis.
 * @author Shilpa Bondale
 */
 public class AssetStateWindow implements DiagnosisConsumerInterface{
     /**
      * Determines the delta between start time and end time of the AssetStateWindow,
      * where start time is always shifting ahead.
      */
      int max_window_length;
     /**
      * Don't know what this is suppose to be...
      */
     StateEstimation apriori_state;     
     /**
      * Current impression of the StateEstimation object
      */
     StateEstimation current_state;
     /**
      * A linked list of StateEstimate objects that are time ordered. 
      * The linked list will be formed based on the timestamp from StateEstimation object. 
      */
     StateEstimation timeordered_states;
     
     
     /**
     * Constructor, sets the length of window. AssetStateWindow is created from AssetModel
     * @param ast_model an instance of AssteModel which represents thresholds for an asset.
     **/
     public AssetStateWindow(AssetModel ast_model ) throws Exception {
	 
     }
     
     
     /**
      * @return StateEstimation object at current time.
      * current time is not required 
      **/
     public StateEstimation getCurrentState() throws Exception {
	 return null;
     }
     
     
     /**
      * @param BelievabilityDiagnosis is a DiagnsisWrapper representation from Believability plugin.
      */
     public void consumeDiagnosis (BelievabilityDiagnosis bb_diagnosis) throws Exception {
	 //null
     }
     
     
     /**
      * closeWindow() also gets called by the distructor for AssetStateWindow
      * Releases/resets  all the assets such as StateEstimation linked list,max_window_length.   
      */
     public void closeWindow(AssetModel ast_model)throws Exception {
	//null 
     }
     
     
     /**
      * Don't know what this function is supposed to be doing.
      */
     public void stateIterator() throws Exception {
	 //null
     }
     
     
 }


