/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: SensorTypeContainer.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/SensorTypeContainer.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-20 21:39:49 $
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

import java.util.Hashtable;

/**
 * Convenience container for storing the many local sensor type
 * models.  
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-20 21:39:49 $
 * 
 *
 */
class SensorTypeContainer extends Hashtable
{

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     */
    SensorTypeContainer( )
    {
        super();
    }  // constructor SensorTypeContainer

    //************************************************************
    /**
     * Add a new SensorTypeModel to the index. Does nothing if the input is
     * null. Does not check for duplicates.
     * @param sensor_type_model The SensorTypeModel to add
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void add( SensorTypeModel sensor_type_model ) 
            throws BelievabilityException 
    {

        if ( sensor_type_model == null ) 
            return;
        
        String name = sensor_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "SensorTypeContainer.add",
                      "No valid name for input model" );

        super.put( name, sensor_type_model );
    } // method add


    //************************************************************
    /**
     * Remove an SensorTypeModel from the index. Does nothing if the input is
     * null or if the SensorTypeModel is not there.
     * @param sensor_type_model The SensorTypeModel to remove
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void remove( SensorTypeModel sensor_type_model ) 
            throws BelievabilityException 
    {

        if ( sensor_type_model == null )
            return;
     
        String name = sensor_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "SensorTypeContainer.remove",
                      "No valid name for input model" );

        super.remove( name );
    } // method remove


    //************************************************************
    /**
     * Get the SensorTypeModel for the input sensor type name.
     * Returns null if there isn't such an SensorModel in the index.
     * @param sensor_type_name The name the sensor type you are concerned with
     */
    SensorTypeModel get( String sensor_type_name ) 
    {
        if ( sensor_type_name == null ) 
            return null;
        
        return (SensorTypeModel) super.get( sensor_type_name );

    } // method getSensorTypeModel
    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

} // class SensorTypeContainer
