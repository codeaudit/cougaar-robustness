/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/AssetModel.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:21 $
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

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetStateDescriptor;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;

import java.util.Enumeration;
import java.util.Vector;


/**
 * The class representing assets, holding the basic information and modeling
 * shortcut information for the asset.
 * There is one AssetModel object for each asset (or each 
 * AssetTechSpecInterface).
 *
 * @author Misty Nodine
 * @version $Revision: 1.1 $Date: 2004-02-26 15:18:21 $
 */
public class AssetModel extends Object
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Each asset will have its own AssetModel, and it makes
     * no sense to have a AssetModel without an asset, so we
     * require one in the constructor.
     *
     * @param asset_ts The handle to the asset's TechSpec
     * @param pomdp_model The POMDPModelInterface for the believability model
     */
    public AssetModel( AssetTechSpecInterface asset_ts,
                 POMDPModelInterface pomdp_model ) {
        _asset_ts = asset_ts;
        _pomdp_model = pomdp_model;
	_belief_update = new BeliefUpdate( this, pomdp_model );
    } // constructor AssetModel


    /*********************************************************************
     *
     * Return the asset's TechSpec handle.
     *
     * @return the asset TechSpec handle
     *********************************************************************/
    public AssetTechSpecInterface getAssetTS() {
        return _asset_ts;
    } // method getAssetTS


    /*********************************************************************
     *
     * Modify the asset's TechSpec handle.
     *
     * @param asset_ts the new asset TechSpec handle
     ********************************************************************/
    public void setAssetTS( AssetTechSpecInterface asset_ts ) {
        _asset_ts = asset_ts;
    } // method setAssetTS


    /*********************************************************************
     *
     * Return the asset's type
     *
     * @return the asset type
     *********************************************************************/
    public AssetType getAssetType() {
        if ( _asset_ts == null ) return null;

	return _asset_ts.getAssetType();
    } // method getAssetTS


    /*********************************************************************
     *
     * Return the asset's name
     *
     * @return the asset expanded name
     *********************************************************************/
    public String getAssetName() {
        if ( _asset_ts == null )
            return null;

        return _asset_ts.getExpandedName();
    } // method getAssetName


    /*********************************************************************
     *
     * Return the asset's list of state descriptors
     *
     * @return a Vector containing the AssetStateDescriptors
     *********************************************************************/
    public Vector getAssetStateDescriptors() {
        if ( _asset_ts == null )
            return new Vector();

        return _asset_ts.getAssetStates();
    } // method getAssetStateDescriptors


    /*********************************************************************
     *
     * Return a string representation of the asset
     * descriptors.
     *
     * @return a string representation of the asset
     *********************************************************************/
    public String toString() {
        if ( _asset_ts == null ) return null;

	String retstring = "< asset_name:" + getAssetName()
	    + " asset_type:" + getAssetType()
	    + " state_descriptors:[";
	
	
	Enumeration sds = getAssetStateDescriptors().elements();
	while ( sds.hasMoreElements() ) {
	    AssetStateDescriptor sd = (AssetStateDescriptor) sds.nextElement();
	    retstring += sd.getStateName();
	    retstring += " ";
	}
	retstring += "] >";
        return retstring;
    } // method toString


    /*********************************************************************
     *
     * Return the POMDP Intermediate model that this asset is a part of
     *
     * @return the POMDP Model
     *********************************************************************/
    public POMDPModelInterface getPOMDPModel() {
        return _pomdp_model;
    } // method getPOMDPModel
    

    /*********************************************************************
     *
     * Return the BeliefUpdate object associated with this asset
     *
     * @return the BeliefUpdate object
     *********************************************************************/
    public BeliefUpdate getBeliefUpdate() {
        return _belief_update;
    } // method getBeliefUpdate


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Asset TechSpec for this asset
    //
    private AssetTechSpecInterface _asset_ts;

    // Vector of AssetStateDescriptor objects, one for each 
    // asset state dimension
    //
    private Vector _asset_states;

    // POMDP Believability Model that this asset is associated with
    //
    private POMDPModelInterface _pomdp_model;

    // BeliefUpdate object for maintaining our belief states about this 
    // asset
    //
    private BeliefUpdate _belief_update;

} // class AssetModel
