/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StressGraph.java,v $
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.TransitiveEffectDescription;
import org.cougaar.coordinator.techspec.TransitiveEffectModel;

/**
 * For the purposes of the believability component, we treat
 * ThreatModelInterface and TransitiveEffectModel objects the same and
 * call them both "stresses".  Because of transitve effects, there is
 * a relationship between stresses that makes it form a graph.  The
 * techspecs themselves, have a linkage structure through these two
 * type of objects, but this is "causation" linking, with multiple
 * potentially causing some other stress.  What we need to do the
 * believability calculation is to have the linkages go in the other
 * direction: for a given stress, we need to be able to find all the
 * stresses that can cause it.  For this reason, we need to build up a
 * graph of stresses with the linkages going from child to parent (in
 * the causation relationship).  The StressInstance objects do this
 * for us.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.21 $Date: 2004-12-14 01:41:47 $
 *
 */
class StressGraph extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Constructor to create an empty graph.
     *
     */
    StressGraph( )
    {
    }  // constructor StressGraph

    //************************************************************
    /**
     * Creating a stress objects, linking to the graph and storing the
     * node. This is part of a mutually recursive structure
     * to walk down the the stress transitivity relationships.  This
     * is the one that links two nodes in the graph and recurses to
     * the next level.
     *
     * @param stress The stress to recurse on
     * @param new_stress_set The vector of current new stresses which
     * must not be null
     * @return a handle to the newly created stress instance.
     *
     */
    void connectAndRecurse( StressInstance stress, Vector new_stress_set )
    {
        if ( stress == null )
            return;

        EventDescription event = stress.getEventDescription();

        if ( event == null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "NULL event found for stress instance "
                      + stress.getName() );
            return;
        }

        // We need to walk the entire causation path and add every

        TransitiveEffectDescription caused_ted = event.getTransitiveEffect();

        // If there is no transitive effect, then we are done.
        //
        if ( caused_ted == null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "No transitve effect for stress " 
                                + stress.getName() );
            return;
        }

        if ( _recursion_depth > MAX_RECURSION_DEPTH )
        {
            if ( _logger.isDebugEnabled() )
                _logger.debug( "connectAndRecurse(): Maximum recursion depth reached.");
            return;
        }

        _recursion_depth += 1;

        TransitiveEffectModel caused_tem = caused_ted.getInstantiation();   

        StressInstance caused_stress;

        if ( caused_tem == null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "No TransitiveEffectModel found for"
                      + " TransitiveEffectDescription of: " 
                      + stress.getName() );

            caused_stress = add( caused_ted, new_stress_set );

        }
        else
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Found transitve effect model for stress "
                      + stress.getName());

            caused_stress = add( caused_tem, new_stress_set );

            if ( caused_stress == null )
            {
                if ( _logger.isDebugEnabled() )
                    _logger.debug( "Unknown stress object type. Ignoring." );
                _recursion_depth -= 1;
                return;
            }
        }

        // This creates the linkage up the causation stream.
        //
        caused_stress.addParent( stress );

        _recursion_depth -= 1;

    } // method connectAndRecurse

    //************************************************************
    /**
     * Creating a stress objects, linking to the graph and storing the
     * node. This is part of a mutually recursive structure to walk
     * down the the stress transitivity relationships.  This is the
     * one that creates and add the StressInstance, then calls the
     * othger connectAndRecurse() method to handle linkage and
     * continue recursion if needed.
     *
     * As this recursion happens, we will add all the new stresses
     * into the 'new_stess' vector.  

     * @return This routine will return the stress instance that was
     * immediately derived from the stress_obj (i.e., the ancestor of
     * all the new stresses.
     *
     * @param tmi The stress as a ThreatModelInterface object
     * @param new_stress_set The vector of current new stresses which
     * must not be null
     */
    StressInstance add( Object stress_obj, Vector new_stress_set )
    {
        // We need to walk the entire causation path and add every
        // stress we find along the way.

        StressInstance stress 
                = (StressInstance) _nodes_by_object.get( stress_obj );

        // First see whether or not we have already added this. We do
        // not need to add anything twice.
        //
        if ( stress != null )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Not adding existing "
                      + stress.getTypeStr() 
                      + " stress " + stress.getName() );
            return stress;
        }
        
        stress = StressInstanceFactory.create( stress_obj );

        if ( _logger.isDetailEnabled() )
            _logger.detail( "Adding new "
                  + stress.getTypeStr() 
                  + " stress " + stress.getName() );
        
        _nodes_by_object.put( stress_obj, stress );

        new_stress_set.addElement( stress );

        // This will do two things: link a transitive event up to this
        // stress, and rcurse down the transitive relationship. 
        //
        connectAndRecurse( stress, new_stress_set );

        return stress;

    } // method add

    //************************************************************
    /**
     * Display of this structure, mostly for debugging.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "\nStress Graph: num nodes = " 
                     + _nodes_by_object.size() + "\n" );

        Enumeration enm = _nodes_by_object.elements();
        while( enm.hasMoreElements() )
        {
            StressInstance stress 
                    = (StressInstance) enm.nextElement();

            buff.append( stress.toString( "\t" ) + "\n" );
            
        } // while enm

        return buff.toString();

    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // We currently only have 4 levels of containment, so we really
    // have no business going beyond this. However, for future
    // expansion, we create a bit more.
    //
    private static final int MAX_RECURSION_DEPTH = 100;

    // This hashtable contains all the StressInstances we know about.
    // It is keyed on the object that gives rise to the stress: either
    // a ThreatModelInterfac, TransitiveEffectModel or
    // TransitiveEffectDescription object.  As we add StressInstances
    // we will build up the linkage structure.
    //
    private Hashtable _nodes_by_object = new Hashtable();

    // We want to make sure this does not recurse too far and signal a
    // problem if it does.
    //
    private int _recursion_depth = 0;

} // class StressGraph
