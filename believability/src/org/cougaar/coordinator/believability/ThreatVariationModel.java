/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ThreatVariationModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/ThreatVariationModel.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-06-21 22:36:17 $
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

import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DefaultAssetTechSpec;
import org.cougaar.coordinator.techspec.NegativeIntervalException;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

/**
 * Wraps the combination of a ThreatDiagnosis.  Mainly provides an
 * indirection layer to reduced the believability package coupling
 * with tech spec package.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-06-21 22:36:17 $
 *
 */
class ThreatVariationModel extends Model
{

    // We will (possibly) get many threat variations, all with the
    // same threat name.  For all threat variations of the same
    // name, they share much common information, but there are two
    // pieces of information which differs and which canchange
    // dynamically: the event probabilities and the assets
    // affected.
    //

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Main constructor.
     *
     * @param threat_desc
     */
    ThreatVariationModel( ThreatRootModel threat_root_model,
                          ThreatModelInterface threat_model )
    {
        this._threat_mi = threat_model;
        this._threat_root_model = threat_root_model;
    }  // constructor ThreatVariationModel

    //************************************************************
    /**
     * Simple accessor
     */
    String getName() { return getThreatDescription().getName(); }

    //************************************************************
    /**
     * Simple accessor
     */
    Vector getAssetList() { return _threat_mi.getAssetList(); }

    //************************************************************
    /**
     * Simple accessor
     */
    ThreatDescription getThreatDescription() 
    {
        return _threat_mi.getThreatDescription(); 
    } // method getThreatdescription

    //************************************************************
    /**
     * Simple accessor
     */
    String getStateDimName() 
    { 
        // Note that a ThreatDesciption does not directly tell you
        // what sate dimension it affects  It'll tell you what asset
        // types it affects and what event it will lead to, but a
        // given event only afects a single state dimension.  Thus, a
        // threat really just pertinas to a single state dimension, so
        // we can get at that through the event description.
        //

        return getThreatDescription().getEventThreatCauses
                ().getAffectedStateDimensionName(); 
    } // method getStateDimName

    
    //************************************************************
    /**
     * Returns the name of the event caused by this threat.
     */
    String getEventName()
    {
        return _threat_root_model.getEventName();
    } // method getEventName

    //************************************************************
    /**
     * Returns the matrix of transition probabilities for the event
     * that is associated with this threat.  These are conditioned on the
     * event that the threat is realized
     */
    double[][] getEventTransitionMatrix()
    {
        return _threat_root_model.getEventTransitionMatrix();
    } // method getEventTransitionMatrix

    //************************************************************
    /**
     * Returns the probability of the event associated with this
     * threat variation over a given time interval.
     *
     * @param start_time The start of the time interval
     * @param end_time The end of the time interval
     * @return the probability that the evenbt occurs in the given
     */
    double getEventProbability( long start_time, long end_time )
           throws BelievabilityException
    {
        try
        {
            double prob =  getThreatDescription().getEventProbability
                    ().computeIntervalProbability( start_time, end_time );

            logDebug( "Threat " + getName() + " causes event "
                      + getEventName() + " from " + start_time
                      + " to " + end_time
                      + " with prob. = " + prob );
                      
            return prob;
        }
        catch (NegativeIntervalException nie)
        {
            throw new BelievabilityException
                    ( "ThreatRootModel.getEventProbability()",
                      "NegativeInterval: " + nie.getMessage() );
        }
        

    } // method getEventProbability

    //************************************************************
    /**
     * Determines if this threat variation is applicable to the given
     * asset ID.
     *
     * @param asset_id The asset to chekc for applicability
     */
    boolean isApplicable( AssetID asset_id )
    {
        // Method implementation comments go here ...
        
        return _threat_mi.containsAsset
                ( DefaultAssetTechSpec.findAssetByID( asset_id ));

    } // method isApplicable

    //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "ThreatVariation for: " + getName() + "\n" );

        buff.append( "\tState dimension: " + getStateDimName()  + "\n" );

        buff.append( "\tEvent name: " + getEventName() + "\n" );

        buff.append( "\tAssetIDs: [" );

        Enumeration asset_enum = getAssetList().elements();
        while( asset_enum.hasMoreElements() )
        {
            AssetTechSpecInterface asset_ts
                    = (AssetTechSpecInterface) asset_enum.nextElement();
            
            buff.append( " " + asset_ts.getAssetID() );
            
        } // while asset_enum

        buff.append( " ]\n" );

        return buff.toString();

    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private ThreatModelInterface _threat_mi;
    private ThreatRootModel _threat_root_model;

} // class ThreatVariationModel
