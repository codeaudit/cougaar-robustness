/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: IntermediateModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/IntermediateModel.java,v $
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

import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;
import org.cougaar.coordinator.DefenseConstants;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.AssetStateDescriptor;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.MonitoringLevel;
import org.cougaar.coordinator.techspec.DiagnosisAccuracyFunction;
import org.cougaar.coordinator.techspec.DamageDistribution;
import org.cougaar.coordinator.techspec.StateValue;

import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;


/**
 * This class contains all of the generic information for the intermediate
 * model for the believability plugin, as well as a slow interface that 
 * can access the information directly.
 *
 * @author Misty Nodine
 */
public class IntermediateModel extends Object implements POMDPModelInterface 
{
    
    // An IntermediateModel contains hashtables of all the current 
    // Asset, threat, and defense objects. It provides a very slow
    // probability interface into the model that they describe

    // Convenience to allow finer grain control over debug logging.
    //  The higher the number, the more messages that appear.
    //
    public static final int LOCAL_DEBUG_LEVEL = 1;

    // Use this as the default name for the "good" diagnosis.  It
    // depends on there being only two possible diagnosis names, and
    // that the one that is not of this label is the "bad" one.
    //
    public static final String DIAGNOSIS_OK_STR = "OK";

    //************************************************************
    /* There is one IntermediateModel per believability plugin.
     * Constructor
     */ 

    public IntermediateModel() 
    {
        _logger = Logging.getLogger( this.getClass().getName() );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Initializing Believability intermediate model");

    }

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
            throws BelievabilityException 
    {
        
        // Get the AssetStateDescriptor. This may throw a
        // BelievabilityException
        AssetStateDescriptor asd = 
                getAssetStateDescriptor ( asset_expanded_name, 
                                          state_descriptor_name );
        
        // Get the set of possible values and return the length
        Vector aspv = asd.getPossibleValues();
        return aspv.size();
    }

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
            throws BelievabilityException
    {

        AssetStateDescriptor asd = 
                getAssetStateDescriptor ( asset_ext_name, 
                                          state_descriptor_name );

        Vector aspv = asd.getPossibleValues();

        for ( int index = 0; index < aspv.size(); index++ )
        {
            StateValue sv = (StateValue) aspv.elementAt( index );

            if ( state_desc_value.equalsIgnoreCase( sv.getName() ))
                return index;
        } // for index
        
        throw new BelievabilityException( "State name cannot be found." );
        
    } // method stateNameToIndex

    //************************************************************
    /*
     * We assume there is some mapping between state names and index
     * values. This routine converts index values into state mnemonic
     * names. 
     *
     * @param asset_ext_name  The asset name
     * @param state_descriptor_nameThe asset desciptor name
     * @param index The state index number
     * @return The mnemonic name of the state
     */
    public String indexToStateName( String asset_ext_name,
                                    String state_descriptor_name,
                                    int index )
            throws BelievabilityException
    {

        AssetStateDescriptor asd = 
                getAssetStateDescriptor ( asset_ext_name, 
                                          state_descriptor_name );

        Vector aspv = asd.getPossibleValues();

        if (( index < 0 )
            || ( index >= aspv.size()))
            throw new BelievabilityException( "Bad state index." );

        return ((StateValue) aspv.elementAt( index )).getName();
        
    } // method indexToStateName



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
            throws BelievabilityException 
    {

        // Get the AssetStateDescriptor. This may throw a
        // BelievabilityException
        AssetStateDescriptor asd = 
                getAssetStateDescriptor ( asset_expanded_name, 
                                          state_descriptor_name );

        // Get the default state value and compare it to the input state
        // value. Return 1 if they are equal, otherwise return zero
        StateValue default_state_value = asd.getDefaultValue();
        String default_state_value_name = default_state_value.getName();
        if ( default_state_value_name.equalsIgnoreCase
             ( state_descriptor_value ) ) {

         return 1.0;
        }
     return 0.0;
    }

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
     * @return The probability of a transition from
     *         state from_descriptor_value to state to_descriptor_value.
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
            throws BelievabilityException 
    {

        // Get the AssetModel. May throw BeleivabilityException
        AssetModel am = getAssetModel( asset_expanded_name );


        if ( (LOCAL_DEBUG_LEVEL >= 5) && _logger.isDebugEnabled() ) 
            _logger.debug( "getTransProb : "
                           + from_descriptor_value
                           + " -> " 
                           + to_descriptor_value
                           + "\n   time = [ "
                           + start_time + ", "
                           + end_time + " ]" );

        if (_logger.isDebugEnabled() && (start_time >= end_time) ) 
            _logger.debug( "getTransProb -- negative time from state " ); 

        // Get the AssetStateDescriptor. This may throw a
        // BelievabilityException
        AssetStateDescriptor asd = 
                getAssetStateDescriptor ( asset_expanded_name, 
                                          state_descriptor_name );
        
        // Iterate through the threat models, looking for one that 
        // has a damage distribution vector that pertains to the state
        // descriptor. Choose the first one you find.
        //
        boolean threat_found = false;
        Enumeration tm_enum = _threat_models.elements();
        while ( tm_enum.hasMoreElements() ) 
        {
            // Get the threat model
            ThreatModelInterface tmi = 
                    (ThreatModelInterface) tm_enum.nextElement();
                  // See if the threat model covers this asset. If not, skip
            // to next
            if ( ! tmi.containsAsset( am.getAssetTS() ) ) {
		if (_logger.isDebugEnabled() ) 
		    _logger.debug( " getTransProb : threat "
				   + tmi.getName()
				   + " does not contain asset " 
				   + am.getAssetTS().getExpandedName() );
                continue;
	    }

            threat_found = true;

            // Check if the threat model has a damage distribution
            // vector that covers the named state descriptor
            //
            Vector ddv = tmi.getDamageDistributionVector();
            Enumeration dd_enum = ddv.elements();
            while ( dd_enum.hasMoreElements() ) 
            {
                DamageDistribution dd =
                        (DamageDistribution) dd_enum.nextElement();

		if ( ! dd.getAssetState().equals( asd ) ) {
		    continue;
		}
		
		if ( dd.getStartState().getName().equalsIgnoreCase
		     ( from_descriptor_value ) &&
		     dd.getEndState().getName().equalsIgnoreCase
		     ( to_descriptor_value ) ) {

                    // HA! Found it!
                    double threat_likelihood =
                            tmi.getThreatLikelihood( start_time, end_time );
                    double transition_likelihood_given_threat = 
                            dd.getProbability();

                    // FIXME: Here is a big assumption that should be verified:
                    // If the threat *does not* get realized, then the asset
                    // does not change state (i.e., transition matrix is
                    // identity matrix.
                    //
                    double transition_likelihood_without_threat;

                    if ( from_descriptor_value.equalsIgnoreCase
                         ( to_descriptor_value ))
                        transition_likelihood_without_threat 
                                = 1.0 - threat_likelihood;
                    else
                        transition_likelihood_without_threat = 0.0;

                    if (_logger.isDebugEnabled() ) 
                        _logger.debug( " getTransProb : Pr("
                                       + from_descriptor_value
                                       + " -> " 
                                       + to_descriptor_value
                                       + ") =  Pr(T)="
                                       + threat_likelihood + " * "
                                       + "Pr(next|prev,T)="
                                       + transition_likelihood_given_threat
                                       + " + Pr(next ^ ~T)="
                                       + transition_likelihood_without_threat);
 
                    // ASSUMPTION: There is only one threat that
                    // affects any given state descriptor.  This is
                    // why we return the first matching value we find.
                    //
                    return ( ( threat_likelihood 
                               * transition_likelihood_given_threat)
                             + transition_likelihood_without_threat );

                }  // if found the appropriate damage distribution

            } // end iteration over damage distribution vector

            // NOTE: Even when we find an applicable threat, this may
            // not actually affect the state descriptor we care about
            // here.  Thus, if we find no damage distributions, we
            // still need to loop over the rest of the threat model to
            // see if another threat impacts it.

        } // end iteration over threat models
        
        // If we loop over all threats and damage distributions and do
        // not find one that is applicable to this asset state
        // desciptor, then we assume that there is nothing that will
        // cause this asset desciptor's state to change.  Thus, the
        // transition probabilities will be 1.0 if it is a
        // self-transition, and zero otherwise (the identity
        // transition function).
        //
        double prob = 0.0;
        if ( from_descriptor_value.equalsIgnoreCase( to_descriptor_value ))
            prob = 1.0;
        
        // Nothing found, return zero probability
        if (_logger.isDebugEnabled() ) 
            _logger.debug( "getTransProb : Pr("
                           + from_descriptor_value
                           + " -> " 
                           + to_descriptor_value
                           + ") Pr(T)=" + prob + " (no threat found)" );

        return prob;
    }

    /************************************************************
     * Get the diagnosis probability -- that is, the probability of
     * the observed diagnosis given the actual state of the asset.
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
            throws BelievabilityException 
    {
        
        if ( (LOCAL_DEBUG_LEVEL >= 5) && this._logger.isDebugEnabled()) 
            this._logger.debug("getObsProb : "
                               + "\n    asset=" + asset_expanded_name 
                               + ", defense=" + defense_name 
                               + ", mon-level=" + monitoring_level
                               + ", asd=" + state_descriptor_name
                               + "\n    Computing Pr( diag=" + diagnosis_name 
                               + " | state=" 
                               + state_descriptor_value + ")");

	// Get the asset TechSpec.
	AssetTechSpecInterface atsi =
	    getAssetModel( asset_expanded_name ).getAssetTS();
        
        // Get the AssetStateDescriptor. This may throw a
        // BelievabilityException
        AssetStateDescriptor asset_sd = 
                getAssetStateDescriptor ( asset_expanded_name, 
                                          state_descriptor_name );

        // Get the DefenseTechSpecInterface. This may throw a
        // BelievabilityException
        DefenseTechSpecInterface defense_tsi =  
	    getDefenseTS( defense_name, atsi.getAssetType().getName() );

        // FIXME: Should we also be checking to see if this defense
        // actually pertains to the asset we are trying to compute the
        // belief for?  Maybe this is not necessary since we would not
        // have gotten the TimedDefenseDiagnosis object for this asset
        // if it wasn't relevant.  In the latter case maybe there
        // should be a sanity check? 

        // *** FIX WHEN READY **MonitoringLevel ml =
        // *** defense_tsi.getMonitoringLevel( monitoring_level );
        MonitoringLevel ml = 
                (MonitoringLevel) 
                defense_tsi.getMonitoringLevels().firstElement();

        Vector diagnosis_accuracy_functions = ml.getDiagnosisAccuracy();

        // FIXME: Assumptions: 
        //
        // The Pr( diag=OK | state ) is *NOT* explicitly
        // represented in the defense tech specs.  The way we compute
        // this is to find the Pr( diag=BAD | state ) and then
        // subtracting it from 1.0.  The sticky issue is in the naming
        // of the diagnoses.  
        //
        // First, it is assumed that no matter what the Defense, one
        // diagnosis name is "OK" and this is the "good" state.
        // Second, it is assumed that there are only two possible
        // diagnosis names, so any name that is not "OK" must be the
        // name fo the "bad" diagnosis (which will be different for
        // each defense).
        //
        String search_name;
        double found_prob = 0.0;
        boolean found = false;

        // The name we search for is not always the same as the one we
        // want the probability for (see assumption note above).
        //
        if ( diagnosis_name.equalsIgnoreCase( DIAGNOSIS_OK_STR ))
            search_name = 
                    getBadDiagnosisName( diagnosis_accuracy_functions );
        else
            search_name = diagnosis_name;

        if (this._logger.isDebugEnabled()) 
            this._logger.debug("getObsProb : "
                               + "Looking for defense="
                               + defense_name
                               + ", diagnosis=" + search_name );

        // Iterate through the diagnosis accuracy functions to see which
        // one should contain the probability needed
        Enumeration daf_enum = diagnosis_accuracy_functions.elements();
        while ( daf_enum.hasMoreElements() ) 
        {
            DiagnosisAccuracyFunction daf =
                    (DiagnosisAccuracyFunction) daf_enum.nextElement();

            /* 
            if (this._logger.isDebugEnabled()) 
                this._logger.debug
                        ( "#### Diagnosis Acc Func -- asset state desc = " 
                          + daf.getAssetState().getStateName() 
                          + ", state descriptor value = " 
                          +  daf.getAssetStateValue().getName() 
                          + ", diagnosis name = " 
                          + daf.getDiagnosisStateValue().getName() );
            */

            // NOTE: This next 'if' predicate used to compare asset
            // state descriptor objects rather than their String
            // names.  I did not hunt it down, but this was causing a
            // problem, and the string comparison seems to have fixed
            // it.
            //
            if ( state_descriptor_name.equalsIgnoreCase
                 ( daf.getAssetState().getStateName() ) 
                 && state_descriptor_value.equalsIgnoreCase
                 ( daf.getAssetStateValue().getName() ) 
                 && search_name.equalsIgnoreCase
                 ( daf.getDiagnosisStateValue().getName() ) ) 
            {
                found_prob = daf.getProbability();
                found = true;
                break;
            }
        } // end iteration over diagnosis accuracy functions

        if ( ! found )
        {
            if (this._logger.isDebugEnabled()) 
                this._logger.debug
                        ("getObsProb : No obs prob found in "
                         + "diag acc func. Assuming zero." );
        }

        // Now we have to factor in fact that the OK diagnosis is not
        // explicitly represented. 
        //
        if ( diagnosis_name.equalsIgnoreCase( DIAGNOSIS_OK_STR ))
        {
            if (this._logger.isDebugEnabled()) 
                this._logger.debug("getObsProb : "
                                   + "[indirect: (1-p)] : " 
                                   + "Pr( diag=" + diagnosis_name 
                                   + " | state=" 
                                   + state_descriptor_value + ") = "
                                   + "1.0 - " + found_prob );
            
            return 1.0 - found_prob;
        }
        else
        {
            if (this._logger.isDebugEnabled()) 
                this._logger.debug("getObsProb : "
                                   + "[direct] : " 
                                   + "Pr( diag=" + diagnosis_name 
                                   + " | state=" 
                                   + state_descriptor_value + ") = "
                                   + found_prob );
 
            return found_prob;
        }

    }

    //******************************************************************
    /**
     * Gets the observation probability for the composite diagnosis
     * consisting of multiple defense conditions.
     *
     * @param asset_expanded_name The expanded name of the asset (a String)
     * @param state_descriptor_name The name of the state descriptor (a String)
     * @param state_descriptor_value The name of the state in the state 
     *                               descriptor (a String)
     * @param composite_diagnosis The composite diagnosis
     */
    public double getObsProbability( String asset_expanded_name,
                                     String state_descriptor_name,
                                     String state_descriptor_value,
                                     CompositeDiagnosis composite_diagnosis ) 
            throws BelievabilityException 
    {

        double prob = 1.0;

        if (this._logger.isDebugEnabled()) 
            this._logger.debug("getObsProb : "
                               + "Computing composite obs prob for\n    "
                               + composite_diagnosis.toString() );

        for ( int diag_idx = 0; 
              diag_idx < composite_diagnosis.size(); 
              diag_idx++ )
        {
            DiagnosisComponent diag_comp 
                    = composite_diagnosis.elementAt( diag_idx );
            
            prob *= getObsProbability
                         (  asset_expanded_name,
                            diag_comp.defenseCondition.getDefenseName(),
                            diag_comp.monitorLevel, 
                            state_descriptor_name,
                            state_descriptor_value,
                            diag_comp.diagnosisName );

        } // for diag_idx

        if (this._logger.isDebugEnabled()) 
            this._logger.debug("getObsProb : "
                               + "Composite probability=" + prob );

        return prob;

    } // method getObsProbability

    /******************************************************************
     *
     * Get the state descriptor with name state_descriptor_name from
     * the asset called asset_expanded_name. A state descriptor has a 
     * set of states pertaining to some particular aspect of the asset,
     * e.g., its network connectivity
     *
     * @param asset_expanded_name The name of the asset
     * @param state_descriptor_name The name of the state descriptor.
     * @return The state descriptor
     * @exception BelievabilityException if either no such asset exists, or the 
     *            asset does not have the named state descriptor.
     *
     *****************************************************************/

    protected AssetStateDescriptor 
     getAssetStateDescriptor ( String asset_expanded_name,
                      String state_descriptor_name ) 
     throws BelievabilityException {

     // May throw model exception, which will just be passed up
     AssetTechSpecInterface asset_tsi = getAssetTS( asset_expanded_name );

     // Have the asset model, now find the state descriptor
     AssetType asset_type = asset_tsi.getAssetType();
     AssetStateDescriptor asd = 
         asset_type.findState( state_descriptor_name );

     // Check if null, if so, throw exception
     if ( asd == null ) {
         throw new BelievabilityException ( " IntermediateModel.getAssetStateDescriptor -- "
                           + " No such state descriptor named " 
                           + state_descriptor_name 
                           + " in asset "
                           + asset_expanded_name );
     }

     return asd;
    }
   

    /******************************************************************
     *
     * Get the defense tech spec interface for the defense named 
     * defense_name over the asset type asset_type.
     *
     * @param defense_name The name of the defense
     * @param asset_type The name of the type of asset this defense defends
     * @return The associated DefenseTechSpecInterface
     * @exception BelievabilityException if either no such defense exists.
     *
     *****************************************************************/

    protected DefenseTechSpecInterface getDefenseTS ( String defense_name,
						      String asset_type )
            throws BelievabilityException 
    {

	String extended_name = getDefenseModelKey( defense_name, asset_type );

        // Locate the defense in the hash table
        DefenseTechSpecInterface dtsi = 
                (DefenseTechSpecInterface) 
                _defense_techspecs.get( extended_name );

        if ( dtsi == null ) 
        {
            throw new BelievabilityException 
                    ( " IntermediateModel.getDefenseTS -- "
                      + " No defense named " 
                      + extended_name );
        }

        else 
            return dtsi;
    }

    /******************************************************************
     *
     * Get the asset called asset_expanded_name. This will return the 
     * AssetModel for the asset
     *
     * @param asset_expanded_name The name of the asset
     * @return The AssetModel of the asset
     * @exception BelievabilityException if no such asset exists
     *
     *****************************************************************/

    public AssetModel getAssetModel( String asset_expanded_name )
     throws BelievabilityException {

     // Locate the asset in the hash table
     AssetModel am = (AssetModel) _asset_models.get( asset_expanded_name );
     if ( am == null ) {
         throw new BelievabilityException ( " IntermediateModel.getAssetModel -- "
                           + " No asset named " 
                           + asset_expanded_name );
     }
     else return am;
    }


    /******************************************************************
     *
     * Get the techspec for the asset called asset_expanded_name. 
     * This will return the AssetTechSpecInterface for the TechSpec
     * of the asset.
     *
     * @param asset_expanded_name The name of the asset
     * @return The AssetTechSpecInterface of the asset
     * @exception BelievabilityException if no such asset exists
     *
     *****************************************************************/

    protected AssetTechSpecInterface getAssetTS( String asset_expanded_name )
     throws BelievabilityException {

     // Locate the asset in the hash table. May throw a BelievabilityException
     AssetModel am = (AssetModel) getAssetModel( asset_expanded_name );
     return am.getAssetTS();
    }

    /**************************************************************
     *
     * Add an asset to the model
     *
     * @param asset_ts The AssetTechSpecInterface of the model to add
     * @return true if the asset was successfully added, 
     *         otherwise false (e.g. if there already was an asset techspec
     *          for the named asset in the model.
     *
     **************************************************************/

    public boolean addAsset( AssetTechSpecInterface asset_ts ) 
    {

        // Get the key, which is the asset's expanded name
        String asset_expanded_name = asset_ts.getExpandedName();
        
        if (_logger.isDebugEnabled()) 
            _logger.debug("Adding asset: " + asset_expanded_name);

        // Return false if the asset is already there
        if ( _asset_models.containsKey( asset_expanded_name ) ) 
        {
            _logger.warn( " Attempting to add duplicate asset " 
                          + asset_expanded_name );
            
            return false;
        }
        
        else 
        {
            // Make the asset model and add it to the hashtable
            AssetModel asset_model = new AssetModel( asset_ts, this );
            _asset_models.put( asset_expanded_name, asset_model );

         // Debugging information
         if ( _logger.isDebugEnabled() ) 
          _logger.debug( "Added asset " + asset_model.toString() );
            return true;
        }
    }


    /**************************************************************
     *
     * Change an asset in the model. If there is no techspec already
     * in existence for this asset, it adds the techspec. This should 
     * not happen, however.
     *
     * @param asset_ts The changed AssetTechSpecInterface
     * @return true if the asset was successfully changed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean changeAsset( AssetTechSpecInterface asset_ts ) 
    {

        // Get the key, which is the asset's expanded name
        String asset_expanded_name = asset_ts.getExpandedName();
        
        if (_logger.isDebugEnabled()) 
            _logger.debug("Changing asset: " + asset_expanded_name);

        // Give a warning if the asset is not already there
        AssetModel asset_model = 
                (AssetModel) _asset_models.get( asset_expanded_name );

        if ( asset_model == null ) 
        {
            _logger.warn( " Change request to unknown asset. " 
                          + asset_expanded_name );
            addAsset( asset_ts );
        }
      
        // Update the asset tech spec hashtable to contain the new techspec
        asset_model.setAssetTS( asset_ts );

     // Debugging information
     if ( _logger.isDebugEnabled() ) 
         _logger.debug( "Changed asset " + asset_model.toString() );
     return true;
    }


    /**************************************************************
     *
     * Remove an asset from the model. If there is no techspec already
     * in existence for this asset, do nothing. This should not happen, 
     * however.
     *
     * @param asset_ts The AssetTechSpecInterface to remove
     * @return true if the asset was successfully removed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean removeAsset( AssetTechSpecInterface asset_ts ) 
    {

        // Get the key, which is the asset's expanded name
        String asset_expanded_name = asset_ts.getExpandedName();

        if (_logger.isDebugEnabled()) 
            _logger.debug("Removing asset: " + asset_expanded_name);
        
        // Update the asset tech spec hashtable to remove the techspec
        AssetModel am = 
                (AssetModel) _asset_models.remove( asset_expanded_name );

        // If you got out the right one, return true.
        // Otherwise, put it back and return false.
        if ( am == null ) 
        {
            // Remove request for asset that was not there
            _logger.warn( " Remove request for unknown asset "
                          + asset_expanded_name);
            return true;
        }

        else {
         // Debugging information
         if ( _logger.isDebugEnabled() ) 
          _logger.debug( "Removed asset " + asset_expanded_name );
            return true;
     }
    }

    /**************************************************************
     *
     * Add a defense to the model
     *
     * @param defense_ts The DefenseTechSpecInterface of the model to add
     * @return true if the defense was successfully added, 
     *         otherwise false (e.g. if there already was a defense techspec
     *          for the defense in the model.)
     *
     **************************************************************/
    public boolean addDefense( DefenseTechSpecInterface defense_ts ) 
    {

        // Get the key, which is the defense's name
        String defense_name = getDefenseModelKey( defense_ts );
        
        if (_logger.isDebugEnabled()) 
            _logger.debug("Adding defense: " + defense_name );

        // Return false if the defense is already there
        if ( _defense_techspecs.containsKey( defense_name ) ) 
        {
            _logger.warn( " Attempt to add duplicate defense "
                          + defense_name );
            return false;
        }
        
        else 
        {
            _defense_techspecs.put( defense_name, defense_ts );

         // Debugging information
         if ( _logger.isDebugEnabled() ) 
          _logger.debug( "Added defense " 
                      + getDefenseTSString( defense_ts ) );
            return true;
        }
    }


    /**************************************************************
     *
     * Change a defense in the model. If there is no techspec already
     * in existence for this defense, add the techspec. This should 
     * not happen, however.
     *
     * @param defense_ts The changed DefenseTechSpecInterface
     * @return true if the defensewas successfully changed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean changeDefense( DefenseTechSpecInterface defense_ts ) 
    {

        // Get the key, which is the defense's name
        String defense_name = getDefenseModelKey( defense_ts );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Changing defense: " + defense_name );

        // Give a warning if the defense is not already there
        if ( ! _defense_techspecs.containsKey( defense_name ) ) 
        {
            _logger.warn( " Change request to unknown defense "
                          + defense_name );
        }
      
        // Update the tech spec hashtable to contain the new defense techspec
        _defense_techspecs.put( defense_name, defense_ts );

     // Debugging information
     if ( _logger.isDebugEnabled() ) 
         _logger.debug( "Changed defense " 
                  + getDefenseTSString( defense_ts ) );
        return true;
    }


    /**************************************************************
     *
     * Remove a defense from the model. If there is no techspec already
     * in existence for this defense, do nothing. This should not happen, 
     * however.
     *
     * @param defense_ts The DefenseTechSpecInterface to remove
     * @return true if the defensewas successfully removed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean removeDefense( DefenseTechSpecInterface defense_ts ) 
    {

        // Get the key, which is the defense's name
        String defense_name = getDefenseModelKey( defense_ts );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Removing defense: " + defense_name );

        // Update the defense tech spec hashtable to remove the techspec
        DefenseTechSpecInterface dtsi = 
                (DefenseTechSpecInterface) 
                _defense_techspecs.remove( defense_name );

        // If you got out the right one, return true.
        // Otherwise, put it back and return false.
        if ( dtsi == null ) 
        {
            // Remove request for techspec that was not there
            _logger.warn( " Remove request for unknown defense. " 
                 + defense_name );
            return true;
        }
        else if ( dtsi.equals( defense_ts ) ) {
         if (_logger.isDebugEnabled()) 
          _logger.debug("Removed defense: " + defense_name );
            return true;
     }
        else {
            _logger.warn( " Remove request for defense, "
                          + defense_name
                          + " but techspecs do not match. "
                          + "Keeping old techspec. " );
            _defense_techspecs.put( defense_name, dtsi );
            return false;
        }

    }

    /**************************************************************
     *
     * Add a threat type to the model
     *
     * @param threat_model The ThreatModelInterface of the model to add
     * @return true if the threat type was successfully added, 
     *         otherwise false (e.g. if the threat type is already
     *          in the model.)
     *
     **************************************************************/
    public boolean addThreatType( ThreatModelInterface threat_model ) 
    {

        // Get the key, which is the threat type's name
        String threat_name = getThreatModelKey( threat_model );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Adding threat: " + threat_name );

        // Return false if the threat type is already there
        if ( _threat_models.containsKey( threat_name ) ) 
        {
            _logger.warn( " Attempt to add duplicate threat_model "
                          + threat_name );
            return false;
        }
     
        else {
            _threat_models.put( threat_name, threat_model );

         // Debugging information
         if ( _logger.isDebugEnabled() ) 
          _logger.debug( "Added threat " 
                      + getThreatTSString( threat_model ) );
            return true;
        }
    }


    /**************************************************************
     *
     * Change a threat type in the model. If the threat type does not
     * already exist, add it. This should not happen, however.
     *
     * @param threat_model The changed ThreatModelInterface
     * @return true if the threat type was successfully changed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean changeThreatType( ThreatModelInterface threat_model ) 
    {

        // Get the key, which is the threat's name
        String threat_name = getThreatModelKey( threat_model );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Changing threat: " + threat_name );

        // Give a warning if the threat type is not already there
        if ( ! _threat_models.containsKey( threat_name ) ) 
        {
            _logger.warn( " Change request to unknown threat type. " 
                          + threat_name );
        }
      
        // Update the hashtable to contain the new threat type
        _threat_models.put( threat_name, threat_model );

     // Debugging information
     if ( _logger.isDebugEnabled() ) 
         _logger.debug( "Changed threat " 
                  + getThreatTSString( threat_model ) );
        return true;
    }


    /**************************************************************
     *
     * Remove a threat type from the model. If the threat type does not
     * exist, do nothing. This should not happen, however.
     *
     * @param threat_model The ThreatModelInterface to remove
     * @return true if the threat type was successfully removed, 
     *         otherwise false.
     *
     **************************************************************/
    public boolean removeThreatType( ThreatModelInterface threat_model ) 
    {

        // Get the key, which is the name of the threat type
        String threat_name = getThreatModelKey( threat_model );

        if (_logger.isDebugEnabled()) 
            _logger.debug("Removing threat: " + threat_name );

        // Update the threat model hashtable to remove the threat model
        ThreatModelInterface tmi = 
                (ThreatModelInterface) _threat_models.remove( threat_name );

        // If you got out the right one, return true.
        // Otherwise, put it back and return false.
        if ( tmi == null ) 
        {
            // Remove request for threat model that was not there
            _logger.warn( " Remove request for unknown threat model "
                          + threat_name );
            return true;
        }
        else if ( tmi.equals( threat_model ) ) {
	    if (_logger.isDebugEnabled()) 
		_logger.debug("Removed threat: " + threat_name );
            return true;
	}
        else {
            _logger.warn( " Remove request for threat type "
                          + threat_name
                          + ", but specs do not match. Keeping old spec. " );
            _threat_models.put( threat_name, tmi );
            return false;
        }

    }

    //************************************************************
    /**
     * Gets the first non-ok diagnosis name from the list of diagnosis
     * accuracy functions
     *
     * @param daf_list A vector of DiagnosisAccuracyFunction objects
     * @return  The diagnosis name as a String: This will either be
     * the first non-ok name found.
     */
    public String getBadDiagnosisName( Vector daf_list )
            throws BelievabilityException 
    {

        Enumeration daf_enum = daf_list.elements();
        while ( daf_enum.hasMoreElements() )
        {
            DiagnosisAccuracyFunction daf 
                    = (DiagnosisAccuracyFunction) daf_enum.nextElement();

            String cur_diag_name = daf.getDiagnosisStateValue().getName();

            // The first non-OK diagnosis name we see, we assume it is
            // the bad diagnosis name 
            //
            if ( ! cur_diag_name.equalsIgnoreCase( DIAGNOSIS_OK_STR ) )
            {
                return cur_diag_name;
            }
        }

        throw new BelievabilityException
                ( "Problem getting diagnosis name." );
    }

    //************************************************************
    /**
     * Converts a DefenseApplicabilityConditionSnapshot object
     * into a diagnosis name.
     *
     * @param defense_cond The defense condition to convert to a
     * diagnsois name. 
     * @param daf_list A vector of DiagnosisAccuracyFunction objects
     * @return  The diagnosis name as a String: This will either be
     * the non-ok name if the defense_condition is valid, or the ok
     * name if the defense_condition is null.
     */
    public String getDiagnosisName( DefenseApplicabilityConditionSnapshot defense_cond,
                                    Vector daf_list )
            throws BelievabilityException 
    {

        // DefenseApplicabilityConditionSnapshot objects do 
	// not seem to report the
        // diagnosis name directly. Instead, it assume the
        // diagnoses are boolean valued, so that a defense
        // that generates a DefenseApplicabilityConditionSnapshot
	// is assuming that the
        // diagnosis is "BAD".  If a Defense does not report anything,
        // then it is assumed that the diagnosis is "GOOD".
        //
        // This all causes some problems, because there is nothing in
        // the tech specs that defines what the good diagnosis name is
        // and what the bad diagnosis name is: It simply enumerates
        // the diagnosis names wihtout attaching any semantic
        // information to them.  Thus, the defense doesn't tell us the
        // diagnosis name and the tech specs do not tell us what the
        // "BAD" or "GOOD" diagnosis names are.  This leaves an
        // impossible problem that can only be solved by making some
        // assumptions.
        //
        // Until this problem is fixed, we assume that the "OK"
        // diagnosis name always exists and that any other
        // diagnosis name is the "BAD" diagnosis.  A "BAD" diagnosis
        // name is returned if the DefenseApplicabilityConditionSnapshot 
	// object is present,
        // and the "OK" diagnosis name is returned if the
        // DefenseApplicabilityConditionSnapshot object is null. 
        // 

        if ( defense_cond == null )
            return DIAGNOSIS_OK_STR;

        return getBadDiagnosisName( daf_list );

    } // method getDiagnosisName

    /**************************************************************
     *
     * Deal with an incoming TimedDefenseDiagnosis. Update the belief
     * state and return the resulting StateEstimation for posting on
     * the BB
     *
     * @param timed_diagnosis The incoming diagnosis
     *
     */
    public StateEstimation 
            processDiagnosis( TimedDefenseDiagnosis timed_diagnosis ) 
            throws BelievabilityException 
    {
        if ( (LOCAL_DEBUG_LEVEL >= 5) && _logger.isDebugEnabled()) 
            _logger.debug("Processing diagnosis...");

        // Get the current asset information, including the current 
        // belief state of the asset. GetAssetModel may throw a 
        // BelievabilityException
        String asset_ext_name = timed_diagnosis.getAssetName();
        AssetModel asset_model = getAssetModel( asset_ext_name );
        BeliefUpdate belief = asset_model.getBeliefUpdate();

        // This is the object to be returned.
        //
        StateEstimation se = new StateEstimation( asset_ext_name );

        // The timed_diagnosis is a set of 
	// DefenseApplicabilityConditionSnapshots for a
        // given asset.  Each DefenseApplicabilityConditionSnapshot
	// we assume (for now) to
        // be applicable to *all* asset state descriptors of this
        // asset.  Thus, we loop over all of the
        // DefenseApplicabilityConditionSnapshots to create a
	// composite diagnosis, which is
        // used as the observation for all asset state descriptors. 
        // We then loop over all the state descriptors and
        // independently compute the belief state over each one.
        //  We do not assume that all defenses will be reporting
        // something in the TimedDefenseDiagnosis.  We consult the
        // techspecs for all defenses for a given asset, and any that
        // are not present, we assume to be reporting "OK". 
        //
        CompositeDiagnosis composite_diagnosis = new CompositeDiagnosis();

        if ( _logger.isDebugEnabled() ) 
            _logger.debug("  Building composite diagnosis for: "
                          + asset_ext_name );

        Iterator td_iter = timed_diagnosis.iterator();
        while ( td_iter.hasNext() )
        {

            DefenseApplicabilityConditionSnapshot dc =
		(DefenseApplicabilityConditionSnapshot) td_iter.next();
            
	    String defense_name = getDefenseModelKey( dc.getDefenseName(),
						       dc.getAssetType() );
            if (  _logger.isDebugEnabled() ) 
                _logger.debug("    Handling defense condition for "
			      + defense_name );

            DefenseTechSpecInterface defense_tsi;
            // It is possible that not all the defenses are currently
            // known.  If we do not know it, then we will just go with
            // those we do know about.
            try
            {
                defense_tsi = getDefenseTS( dc.getDefenseName(), 
					    dc.getAssetType() );
            }
            catch (BelievabilityException be)
            {
                if ((LOCAL_DEBUG_LEVEL >= 5) && _logger.isDebugEnabled()) 
                    _logger.debug("    " + be.getMessage() + " (skipping)" );
                continue;
            }

            // We also assume there is only one monitoring level for a
            // defense (again, another assumption that wil not hold in
            // the long term.)
            //
            MonitoringLevel ml = 
                    (MonitoringLevel) 
                    defense_tsi.getMonitoringLevels().firstElement();

            // We need to find the asset sate descriptor that this
            // defense is monitoring. 
            //
            Vector diagnosis_accuracy_functions = ml.getDiagnosisAccuracy();

            // Assume there is only one diagnosis accuracy function
            // for a defense. (i.e., a defense only monitors a singl;e
            // asset state descriptor.
            //
            DiagnosisAccuracyFunction daf 
                    = (DiagnosisAccuracyFunction) 
                    diagnosis_accuracy_functions.elementAt( 0 );

            // The diagnosis name is needed to do a belief update
            // since this translates into the POMDP observation needed
            // in the belief update formula.
            //
            String diagnosis_name;
            if ( dc.getValue().toString().equalsIgnoreCase
                 ( DefenseConstants.BOOL_TRUE.toString() ))
            {
                // This should get us the "non-ok" diagnosis name
                diagnosis_name 
                        = getDiagnosisName( dc, 
                                            diagnosis_accuracy_functions );
            }  // if defense thinks it is applicable
            else
            {
                // Passing in a 'null' should get us the "ok"
                // diagnosis name.
                diagnosis_name 
                        = getDiagnosisName( null, 
                                            diagnosis_accuracy_functions );
            }  // defense does not think it is applicable

            if (_logger.isDebugEnabled()) 
                _logger.debug("      Adding diagnosis component: "
                              + dc.getDefenseName() + "=" 
                              + diagnosis_name );

            // The actual diagnosis used to compute the belief state
            // is a composite set of all the defenses that monitor
            // this asset. Here we are entering the individual
            // components of that diagnosis in a form that will be
            // easier for the belief state update routines to handle
            // (it will save those routines from having to iterate
            // through all the techspecs.
            //
            composite_diagnosis.add( dc,  
                                     ml.getName(), 
                                     diagnosis_name );

            // It is useful to be able to access the individual
            // defenbse conditions downstream.  Since they are the
            // same for all AssetBeliefState, we store them also in
            // the StateEstimation object.
            //
            se.addDefenseCondition( dc );

        } // end while loop over defense conditions in timed def diag

        // Even if we have a problem with finding defense techspecs,
        // we would like to notify the downstream elements that there
        // were some TimedDefenseDiagnosis objects published.  Maybe
        // those component will know what to do about this exceptional
        // condition.
        //
        if ( composite_diagnosis.size() < 1 )
        {
            if (_logger.isDebugEnabled()) 
                _logger.debug("Could not find any defense"
                              + " techspecs for defense conditions." );
            
            se.logError( "Believability update could not find any defense"
                         + " techspecs for defense conditions.\n" );
            
            return se;
        }

	if (_logger.isDebugEnabled()) 
	    _logger.debug("Composite diagnosis is " 
			  + composite_diagnosis.toString() );

        // FIXME: Because the threat likelihood probabilities can vary
        // over time, if the threat probability changes in the middle
        // of a belief state computation we will have problems with
        // the belief update computations.  We need to prevent this
        // from happening.
        //
        // One possible solution is to put a call right here that is
        // something like:
        //
        //   this.snapshotThreatLikelihood( asset_ext_name );
        //
        // Whch will look up the current threat likelihood once and
        // cache the value.  Then, all references to looking up the
        // threat likelihood should *not* refer to the techspecs, but
        // to this cached value.
        //

	// NOTE: The defense conditions are now snapshots, and will not
	// change. This should stabilize this some. -- Misty

        // Once we have the composite diagnosis, we now have to
        // iterate over all the asset state descriptors, computing a
        // new belief state for each desciptor (independently).
        //
        if ((LOCAL_DEBUG_LEVEL >= 5) && _logger.isDebugEnabled()) 
            _logger.debug("  Computing a.s.d belief states: "
                          + asset_ext_name );

        Vector asd_list = asset_model.getAssetStateDescriptors();
        Enumeration asd_enum = asd_list.elements();
        while ( asd_enum.hasMoreElements() )
        { 
            AssetStateDescriptor asd 
                    = (AssetStateDescriptor) asd_enum.nextElement();

            if (_logger.isDebugEnabled()) 
                _logger.debug("Handling asset state: " + asd.getStateName() );

            // If something goes wrong i the belief calculation, we
            // want to still return a state estimation for as much
            // stuff as we can.  However, we also want to signal that
            // a problem occurred.
            //
            try
            {
                // This is where the calculations really happen.
                //
                AssetBeliefState asset_bs 
                        = belief.update( asd, composite_diagnosis );
            
                se.put( asd, asset_bs );
            }
            catch (BelievabilityException be)
            {
                if (this._logger.isDebugEnabled()) 
                    this._logger.debug
                            ( "Logging belief update error to S.E. (a.s.d. "
                              + asd.getStateName() 
                              + ")\n    Exception msg: "
                              + be.getMessage() );

                se.logError( "Problem with belief update for a.s.d. "
                             + asd.getStateName() + "\n"
                             + be.getMessage() + "\n" );
            }
            
        }  // while asd_enum

        if ( se.size() < 1 )
        {
            if (this._logger.isDebugEnabled()) 
                this._logger.debug
                        ( "Returning empty StateEstimation" );
            
            se.logError( "None of the belief updates succeeded.\n" );
        }

        return se;
        
    }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Temporary method for printing defenses
    private String getDefenseTSString( DefenseTechSpecInterface dts ) {
     if ( dts == null ) return null;
     String retstring = "< defense_name:" + getDefenseModelKey( dts )
         + " monitoring_levels:[";
     Enumeration mls = dts.getMonitoringLevels().elements();
     while ( mls.hasMoreElements() ) {
         MonitoringLevel ml = (MonitoringLevel) mls.nextElement();
         retstring += ml.getName();
         retstring += "--( ";
         
         Enumeration dafs = ml.getDiagnosisAccuracy().elements();
         while ( dafs.hasMoreElements() ) {
          DiagnosisAccuracyFunction daf = (DiagnosisAccuracyFunction) dafs.nextElement();
          retstring = retstring + "{" + daf.getDiagnosisStateValue().getName() + " diagnosis} " 
              + daf.getAssetState().getStateName() + " " + daf.getAssetStateValue().getName() + ":" 
              + daf.getProbability() + " ";
         }
         retstring += " )";
     }
     retstring += "] >";
     return retstring;
    }

    // Temporary method for printing threats
    private String getThreatTSString( ThreatModelInterface tmi ) {
     if ( tmi == null ) return null;
     String retstring = "< threat_model:" + getThreatModelKey(tmi);
     retstring += " damage_distributions:[ ";
     Enumeration dd_enum = tmi.getDamageDistributionVector().elements();
     while ( dd_enum.hasMoreElements() ) {
         DamageDistribution dd = (DamageDistribution) dd_enum.nextElement();
         retstring += dd.toString();
         retstring += " ";
     }
     retstring += " ] >";
     return retstring;
    }

    // Hashtable of AssetTechSpecInterfaces. Key is the extended name of the
    // asset. Value is the AssetModel for the asset.
    //
    private Hashtable _asset_models = new Hashtable();


    // Hashtable of threat models. Key is the name of the threat type
    // concatenated with the name of the asset type.
    // Value is the ThreatModelnterface.
    //
    private Hashtable _threat_models = new Hashtable();

    // Method for generating the key for _threat_models. 
    private String getThreatModelKey( ThreatModelInterface tmi ) {
	if ( tmi == null ) return null;

	String keyname = tmi.getName() + ":" + tmi.getAssetType();
	keyname = keyname.toLowerCase();
	return keyname;
    }
    
    // Method for generating the key for _threat_models
    private String getThreatModelKey( String threat_name,
				       String asset_type ) {
	String keyname = threat_name + ":" + asset_type;
	keyname = keyname.toLowerCase();
	return keyname;
    }
    
    // Hashtable of defenses. Key is the defense techspec. Value is
    // the DefenseModelTechSpecInterface.
    //
    private Hashtable _defense_techspecs = new Hashtable();

    // Method for generating the key for _defense_techspecs.
    private String getDefenseModelKey( DefenseTechSpecInterface dtsi ) {
	if ( dtsi == null ) return null;

	String keyname = dtsi.getName() + ":" + dtsi.getAssetType();
	keyname = keyname.toLowerCase();
	return keyname;
    }
    
    // Method for generating the key for _defense_techspecs.
    private String getDefenseModelKey( String defense_name,
				       String asset_type ) {

	String keyname = defense_name + ":" + asset_type;
	keyname = keyname.toLowerCase();
	return keyname;
    }
    
    // Logger for error messages
    private Logger _logger;

} // class IntermediateModel
