/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ThreatVariationCollection.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/ThreatVariationCollection.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-18 00:16:39 $
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
import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashSet;

/**
 * Collects ThreatVariationModels together for a given asset instance
 * (collection will only refer to a singler state dimension as an
 * artifact of the nature of the threat and event objects) 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-06-18 00:16:39 $
 *
 */
class ThreatVariationCollection
{

    // Note we do not implement the java.util.Collection interface
    // because we do not yet need most of that API.  If this gets more
    // broadly used, then we would retrofit this.
    //

    // At the top-level of this data structure is a hashtable, which
    // maps event names into a Set of threat variations.  This
    // organization is done because grouping by event names is the way
    // the infomration needs to be accessed in the belief update
    // calculations.
    //

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    ThreatVariationCollection( )
    {
    }  // constructor ThreatVariationCollection

    //************************************************************
    /**
     * Adds a threat variation to this collection.
     *
     * @param tvm The threat variation to add.
     */
    boolean add( ThreatVariationModel tvm )
    {
        if ( tvm == null )
            return false;

        String event_name = tvm.getEventName();

        // See if we have already started a set fo this event name.
        HashSet threat_set = (HashSet) _threat_hash_set.get( event_name );
        if ( threat_set == null )
        {
            threat_set = new HashSet();
            _threat_hash_set.put( event_name, threat_set );
        } // if no threat set yet

        threat_set.add( tvm );

        _num_variations += 1;
        return true;

    } // method remove
    
    //************************************************************
    /**
     * simple accessor
     */
    int getNumThreatVariations() { return _num_variations; }

    //************************************************************
    /**
     * simple accessor
     */
    int getNumEvents() { return _threat_hash_set.size(); }

    //************************************************************
    /**
     * simple accessor
     */
    Enumeration eventNameEnumeration() { return _threat_hash_set.keys(); }

    //************************************************************
    /**
     * simple accessor
     */
    HashSet getThreatVariationSet( String event_name )
    {
        return (HashSet) _threat_hash_set.get( event_name ); 
    }

    //************************************************************
    /**
     * Mostly for debugging.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "ThreatVariationCollection: Num events: "
                     + getNumEvents() + ", Num total threat vars: "
                     + _num_variations + "\n" );

        Enumeration event_name_enum = _threat_hash_set.keys();
        while( event_name_enum.hasMoreElements() )
        {
            String event_name = (String) event_name_enum.nextElement();

            buff.append( "\tEvent: " + event_name + ", threats = [ " );

            HashSet threat_var_set
                    = (HashSet) _threat_hash_set.get( event_name );
            Iterator threat_var_iter = threat_var_set.iterator();
            while( threat_var_iter.hasNext() )
            {
                ThreatVariationModel threat_var 
                        = (ThreatVariationModel) threat_var_iter.next();
                
                buff.append( " " + threat_var.getName() );
               
            } // while threat_var_iter
            
            buff.append( " ]\n" );
            
        } // while event_name_enum
       
        return buff.toString();

    } // method toString
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    Hashtable _threat_hash_set = new Hashtable();

    // Keep this to stop from having to do a double iteration.
    int _num_variations = 0;

} // class ThreatVariationCollection
