/*
 * AssetTypeContainer.java
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

import java.util.Hashtable;


/**
 * The class that indexes AssetTypeModel objects by the name of the asset
 * type. There is one AssetTypeContainer for the whole believability plugin.
 * @author Misty Nodine
 */
class AssetTypeContainer extends Hashtable 
{

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Constructor
     */
    AssetTypeContainer( ) 
    {
        super();
    } // constructor AssetTypeContainer


    //************************************************************
    /**
     * Add a new AssetTypeModel to the index. Does nothing if the input is
     * null. Does not check for duplicates.
     *
     * @param asset_type_model The AssetTypeModel to add
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void add( AssetTypeModel asset_type_model ) 
            throws BelievabilityException 
    {

        if ( asset_type_model == null ) 
            return;
        
        String name = asset_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "AssetTypeContainer.add",
                      "No valid name for input model" );

        super.put( name, asset_type_model );
    } // method add


    //************************************************************
    /**
     * Remove an AssetTypeModel from the index. Does nothing if the input is
     * null or if the AssetTypeModel is not there.
     *
     * @param asset_type_model The AssetTypeModel to remove
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void remove( AssetTypeModel asset_type_model ) 
            throws BelievabilityException 
    {

        if ( asset_type_model == null ) 
            return;
     
        String name = asset_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "AssetTypeContainer.remove",
                      "No valid name for input model" );

        super.remove( name );
    } // method remove


    //************************************************************
    /**
     * Get the AssetTypeModel for the input asset type name.
     * Returns null if there isn't such an AssetModel in the index.
     * @param asset_type_name The name the asset type you are concerned with
     */
    AssetTypeModel get( String asset_type_name ) 
    {
        if ( asset_type_name == null ) 
            return null;
        
        return (AssetTypeModel) super.get( asset_type_name );

    } // method getAssetTypeModel

} // class AssetTypeContainer
