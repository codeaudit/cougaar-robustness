/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: POMDPModelInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/POMDPModelInterface.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:24 $
*</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;


/**
 * This is the interface to the POMDP model.
 *
 * @author Misty Nodine
 */
public interface POMDPModelInterface {

    /************************************************************
     * An asset state descriptor describes a number of possible states
     * along a particular dimension of the state space. 
     * Get the number of values in the indicated asset state descriptor.
     *
     * @param asset_expanded_name The expanded name of the asset (a String)
     * @param state_descriptor_name The name of the state descriptor (a String)
     * @return The number of states in the dimension of the asset indicated
     *         by the state descriptor name
     * @exception BelievabilityException if the asset does not exist or the
     *                           state descriptor does not exist
     ************************************************************/

    public int getNumStates( String asset_expanded_name,
                             String state_descriptor_name ) 
     throws BelievabilityException;
    
    //************************************************************
    /*
     * We assume there is some mapping between state names and index
     * values. This routine converts the state names into the index
     * values. 
     *
     * @param asset_ext_name The asset name
     * @param state_descriptor_name The asset desciptor name
     * @param state_desc_value The state value name
     * @return The integer index of this state name
     */
    public int stateNameToIndex( String asset_ext_name,
                                 String state_descriptor_name,
                                 String state_desc_value )
            throws BelievabilityException;

    //************************************************************
    /*
     * We assume there is some mapping between state names and index
     * values. This routine converts index values into state mnemonic
     * names. 
     *
     * @param asset_ext_name  The asset name
     * @param state_descriptor_nameThe asset desciptor name
     * @param state The state index number
     * @return The mnemonic name of the state
     */
    public String indexToStateName( String asset_ext_name,
                                    String state_descriptor_name,
                                    int index )
            throws BelievabilityException;

    /************************************************************
     * Get the a priori probability that the indicated asset and the
     * indicated state descriptor take a specific given state (value).
     * This probability will be 1 for the default state of the asset
     * along the dimension described by the state descriptor, and 
     * zero otherwise.
     *
     * @param asset_expanded_name The expanded name of the asset (a String)
     * @param state_descriptor_name The name of the state descriptor (a String)
     * @param state_descriptor_value The name of the stae in the 
     *                               state descriptor (a String)
     * @return The a priori probability that the asset is in the named state 
     *         with respect to the dimension of the state descriptor.
     * @exception BelievabilityException if the asset does not exist or the
     *                           state descriptor does not exist
     ************************************************************/
    public double getAprioriProbability( String asset_expanded_name,
                                         String state_descriptor_name,
                                         String state_descriptor_value )
            throws BelievabilityException;


    /************************************************************
     * Get the state transition probability for the given state 
     * descriptor for the asset, given a particular threat. This is 
     * computed as the product
     *  P(state transition) = 
     *        P(threat is realized) * P(state transition | threat is realized)
     *
     * @param asset_expanded_name The expanded name of the asset (a String)
     * @param state_descriptor_name The name of the state descriptor (a String)
     * @param from_descriptor_value The name of the state in the state 
     *                              descriptor that the transition is from
     *                              (a String)
     * @param to_descriptor_value The name of the state in the state 
     *                              descriptor that the transition is to
     *                              (a String)
     * @param start_time The time of the start of the time window this concerns
     * @param end_time The time that this window ends
     * @return The probability that the threat has caused a transition from
     *         state from_descriptor_value to state to_descriptor_value;
     *         zero if the probability cannot be computed.
     * @exception BelievabilityException if the asset does not exist,
     *                           the state descriptor does not exist,
     *                           or the probability cannot be computed
     ************************************************************/

    public double getTransProbability( String asset_expanded_name,
                                       String state_descriptor_name,
                                       String from_descriptor_value,
                                       String to_descriptor_value,
                                       long start_time,
                                       long end_time )
     throws BelievabilityException;


    /************************************************************
     * Get the observation probability -- that is, the probability of
     * the observation given the actual state of the asset.
     *
     * @param asset_expanded_name The expanded name of the asset (a String)
     * @param defense_name The name of the defense 
     * @param monitoring_level The monitoring level at the current 
     *                             time (currently ignored)
     * @param state_descriptor_name The name of the state descriptor (a String)
     * @param state_descriptor_value The name of the state in the state 
     *                               descriptor (a String)
     * @param diagnosis_name The name of the diagnosis
     * @return The probability of the observed diagnosis given the actual
     *         state of the asset; zero if no probability can be computed.
     * @exception BelievabilityException if the asset does not exist,
     *                           the state descriptor does not exist
     *                           or the defense does not exist
     ************************************************************/

    public double getObsProbability( String asset_expanded_name,
                                     String defense_name,
                                     String monitoring_level,
                                     String state_descriptor_name,
                                     String state_descriptor_value,
                                     String diagnosis_name ) 
     throws BelievabilityException;


    public double getObsProbability( String asset_expanded_name,
                                     String state_descriptor_name,
                                     String state_descriptor_value,
                                     CompositeDiagnosis composite_diagnosis ) 
            throws BelievabilityException;
   
} // class POMDPModelInterface
