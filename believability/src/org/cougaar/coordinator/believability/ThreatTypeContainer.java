/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ThreatTypeContainer.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/ThreatTypeContainer.java,v $
 * $Revision: 1.2 $
 * $Date: 2004-05-28 20:01:17 $
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
 * Container convenience class to hold the multiple threat type
 * models. Note that this includes the event models that are directly
 * related to the threat models. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
 */
class ThreatTypeContainer extends Hashtable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    ThreatTypeContainer( )
    {
        super();
    }  // constructor ThreatTypeContainer

    //************************************************************
    /**
     * Add a new ThreatTypeModel to the index. Does nothing if the input is
     * null. Does not check for duplicates.
     * @param threat_type_model The ThreatTypeModel to add
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void add( ThreatTypeModel threat_type_model ) 
            throws BelievabilityException 
    {

        if ( threat_type_model == null ) 
            return;
        
        String name = threat_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "ThreatTypeContainer.add",
                      "No valid name for input model" );

        super.put( name, threat_type_model );

    } // method add

    //************************************************************
    /**
     * Remove an ThreatTypeModel from the index. Does nothing if the input is
     * null or if the ThreatTypeModel is not there.
     * @param threat_type_model The ThreatTypeModel to remove
     * @throws BelievabilityException if there is no valid name in the model.
     */
    void remove( ThreatTypeModel threat_type_model ) 
            throws BelievabilityException 
    {

        if ( threat_type_model == null )
            return;
     
        String name = threat_type_model.getName();
        if ( name == null ) 
            throw new BelievabilityException
                    ( "ThreatTypeContainer.remove",
                      "No valid name for input model" );

        super.remove( name );

    } // method remove

    //************************************************************
    /**
     * Get the ThreatTypeModel for the input threat type name.
     * Returns null if there isn't such an ThreatModel in the index.
     * @param threat_type_name The name the threat type you are concerned with
     */
    ThreatTypeModel get( String threat_type_name ) 
    {
        if ( threat_type_name == null ) 
            return null;
        
        return (ThreatTypeModel) super.get( threat_type_name );

    } // method getThreatTypeModel
    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

} // class ThreatTypeContainer
