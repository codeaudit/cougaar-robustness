/*
 * AssetContainer.java
 *
 * Created on April 24, 2004
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

import java.util.Hashtable;


/**
 * The class that indexes AssetModel objects by their AssetID.
 * There is one AssetContainer for the whole believability plugin.
 * @author Misty Nodine
 */
public class AssetContainer extends Hashtable 
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor
     **/
    public AssetContainer( ) {
     super();
    } // constructor AssetContainer


    /**
     * Add a new AssetModel to the index. Does nothing if the input is
     * null. Does not check for duplicates.
     * @param asset_model The AssetModel to add
     * @throws BelievabilityException if there is no valid AssetID in the 
     *                                model.
     **/
    public synchronized void addAssetModel( AssetModel asset_model ) 
     throws BelievabilityException {

     if ( asset_model == null ) return;
     
     AssetID aid = asset_model.getAssetID();
     if ( aid == null ) 
         throw new BelievabilityException( "AssetContainer.addAssetModel",
                               "No valid asset ID for input model" );

     this.put( aid, asset_model );
    } // method addAssetModel


    /**
     * Remove an AssetModel to the index. Does nothing if the input is
     * null or if the AssetModel is not there.
     * @param asset_model The AssetModel to remove
     * @throws BelievabilityException if there is no valid AssetID in the 
     *                                model.
     **/
    public synchronized void removeAssetModel( AssetModel asset_model ) 
     throws BelievabilityException {

     if ( asset_model == null ) return;
     
     AssetID aid = asset_model.getAssetID();
     if ( aid == null ) 
         throw new BelievabilityException( "AssetContainer.removeAssetModel",
                               "No valid asset ID for input model" );

     this.remove( aid );
    } // method removeAssetModel


    /**
     * Get the AssetModel for the input AssetID, returns null if there
     * isn't such an AssetModel in the index
     * @param asset_id The AssetID for the asset you are concerned with
     **/
    public synchronized AssetModel getAssetModel( AssetID asset_id ) {

     if ( asset_id == null ) return null;
     else return (AssetModel) super.get( asset_id );
    } // method getAssetModel

} // class AssetContainer
