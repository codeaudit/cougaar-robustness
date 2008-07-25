/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefStateDimension.java,v $
 *</NAME>
 *
 *<COPYRIGHT>
 *  Copyright 2004 Telcordia Technologies, Inc.
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
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import java.text.DecimalFormat;

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetStateDimension;

/**
 * This represents one component of a full BeliefState, paralleling
 * the AssetType structure of consisting of multiple state
 * dimensions. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2008-07-25 20:47:16 $
 */
public class BeliefStateDimension
        implements Cloneable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Gets the asset state dimension's name for which this is the
     * belief of
     */
    public String getName()
    {
        return _state_dim_name;
    } // method getName()

    //************************************************************
    /**
     * Gets the tech spec AssetStateDimension object for this state
     * dimension.  This will give access to the possible values for
     * this state dimension.
     */
    public AssetStateDimension getAssetStateDimension()
    {

        return _asset_dim_model.getAssetStateDimension();
    } // method getAssetStateDimension

    //************************************************************
    /** 
     * Simple accessor
     *
     *  @return the asset ID for this belief sate dimension.
     */
    public AssetID getAssetID( )
    {
        return _asset_id;
    } // method getAssetID

    //************************************************************
    /**
     * Gets a probability for a given state value in this state
     * dimension. 
     *
     * @param state_name The name of the state value for this state
     * dimension. 
     */
    public double getProbability( String state_name )
            throws BelievabilityException
    {
        if ( state_name == null )
            throw new BelievabilityException
                    ( "BeliefStateDimension.getProbability()",
                      "State name is NULL." );
        
        int val_idx = _asset_dim_model.getStateDimValueIndex( state_name );

        if ( val_idx < 0 )
            throw new BelievabilityException
                    ( "BeliefStateDimension.getProbability()",
                      "State name not found: " +  state_name );
        
        return _belief_probs[val_idx];

    } // method getProbability

    //************************************************************
    /**
     * Sets Asset ID for this belief state
     *
     * @param aid The asset id to be set
     */
    void setAssetID( AssetID aid ) 
    {
        _asset_id = aid; 
    } // method setAssetID

    //************************************************************
    /**
     * Standard conversion of object to string
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();
        
        buff.append( _state_dim_name + ": [" );
        
        for ( int i = 0; i < _belief_probs.length; i++ )
            buff.append( " " + _double_format.format(_belief_probs[i]) );
        
        buff.append( " ]" );
        
        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Constructor used for cloning
     *
     * @param at_model The asset type model encompassing the state
     * dimension for which this is part of the belief state for
     * @param dim_idx The positional index of the state dimension, in
     * tersm fo the local internal models.
     * @param probs The probability vector defining this belief
     *state. 
     */
    BeliefStateDimension( AssetTypeDimensionModel dim_model,
                          double[] probs,
                          AssetID asset_id )
            throws BelievabilityException
    {
        _asset_dim_model = dim_model;
        _state_dim_name = dim_model.getStateDimensionName( );
        _asset_id = asset_id;

        _belief_probs = new double[probs.length];
        
        for ( int i = 0; i < probs.length; i++ )
            _belief_probs[i] = probs[i];

    } // constructor BeliefStateDimension

    //************************************************************
    /**
     *  Returns the entire array of belief probabbilities. 
     */
    double[] getProbabilityArray( )
    {
        return _belief_probs;
    }

    //************************************************************
    /**
     *  Returns the entire array of belief probabbilities. 
     */
    void setProbabilityArray( double[] probs )
    {
        for ( int i = 0; i < _belief_probs.length; i++ )
            _belief_probs[i] = probs[i];

    }
 
   //************************************************************
    /**
     * This routine will blur the belief state probabilities.  The
     * bluring means it take some amount of probability (the blur
     * factor) and distributes that evenly across all possibly states,
     * then renormalizing afterwards.  
    *
     * @param blur_factor The amount of probability to spread across
     * each state
     */
    void blurProbabilities( double blur_factor )
    {

        // Distributing the blur_factor evenly means that the number
        // of states affects what will be added.  
        //
        double blur_amount = blur_factor / _belief_probs.length;

        double sum = 0.0;
        for ( int i = 0; i < _belief_probs.length; i++ )
        {
            _belief_probs[i] += blur_amount;
            sum += _belief_probs[i];
        } // for i

        for ( int i = 0; i < _belief_probs.length; i++ )
            _belief_probs[i] /= sum;

    } // method blurProbabilities

 
    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Does a deep clone of this object.
     */
    protected Object clone()
    {
        try
        {
            return new BeliefStateDimension( _asset_dim_model,
                                             _belief_probs,
                                             _asset_id );
        }
        catch (BelievabilityException be)
        {
            return null;
        }

    } // method clone

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _asset_id;

    private AssetTypeDimensionModel _asset_dim_model;

    private String _state_dim_name;

    private double[] _belief_probs;

    private DecimalFormat _double_format = new DecimalFormat("0.0000");

} // class BeliefStateDimension
