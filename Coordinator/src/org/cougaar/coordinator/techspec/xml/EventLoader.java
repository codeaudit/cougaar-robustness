/*
 * EventLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

package org.cougaar.coordinator.techspec.xml;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;
import org.cougaar.core.component.ServiceBroker;


import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Hashtable;

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public class EventLoader extends XMLLoader {
    
    static final String probMapFile = "ProbabilityMap.xml";
    private Hashtable probabilityMap = null;
    
    private Vector events;
    private org.cougaar.util.ConfigFinder cf;
    
    
    private static EventLoader loader = null;
    public static EventLoader getLoader() { return loader; }
    /** Creates a new instance of EventLoader */
    public EventLoader(ServiceBroker serviceBroker, UIDService us, org.cougaar.util.ConfigFinder cf) {
        
        super("Event", "Events", serviceBroker, us); //, requiredServices);
        events = new Vector();
        loader = this;
        this.cf = cf;
        
    }

    
    public void load() {
               
        //load probability map (maps form user string probabilities to Coordinator internal floats
        try {
            probabilityMap = MapLoader.loadMap(cf,probMapFile);
            if (probabilityMap == null) {
                logger.error("Error loading probability map file [" + probMapFile + "]. ");
            }
        } catch (Exception e) {
//            logger.error("Error parsing XML file [" + probMapFile + "]. Error was: "+ e.toString(), e);
            logger.error("Error parsing XML file Error was: ", e);
            return;
        }
        
        
    }
    
    
    /** Called with a DOM "Event" element to process */
    protected Vector processElement(Element element) {
        
        String eventName = element.getAttribute("name");
        String assetType = element.getAttribute("affectsAssetType");
        String stateDim = element.getAttribute("affectsStateDimension");
        
        AssetType affectsAssetType = AssetType.findAssetType(assetType);

        //Report error if asset type wasn't found
        if (affectsAssetType == null) {
            logger.error("Event XML Error - affectsAssetType unknown: "+assetType);
            return null;
        }
        
        AssetStateDimension asd = affectsAssetType.findStateDimension(stateDim);
        //what to do when assetType is null? - create it, process it later?
        if (asd == null) {
            logger.error("Event XML Error - asset state dimension not found! unknown dimension: "+stateDim+ " for asset type = " + assetType);
            return null;
        }
        

        if (us == null) {
            logger.warn("Event XML Error - UIDService is null!");
            return null;
        }
        
        UID uid = us.nextUID();

        //Create default threat
        EventDescription event = new EventDescription( eventName, affectsAssetType, asd );

        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("DirectEffect") ) {
                e = (Element)child;
                parseDirectEffect(e, event);
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("TransitiveEffect") ) {
                e = (Element)child;
                parseTransitiveEffect(e, event);
            } //else, likely a text element - ignore
        }

        
        //Now verify that all states are covered by this action - we have closure -- if not, do not add action.
        boolean error = false;
        Iterator states = asd.getPossibleStates().iterator();
        while ( states.hasNext() ) {
            AssetState as = (AssetState) states.next();
            if ( event.getDirectEffectTransitionForState(as) == null) {
                logger.error(" ERROR: cannot find transition from "+assetType+":"+asd.getStateName()+":"+as.getName()+ " --> IGNORING EVENT ["+event.getName()+"]!!");
                error = true;
            }
        }

        if (!error) {
            events.add(event);
            if (logger.isDebugEnabled()) logger.debug("Added new Event: \n"+event.toString() );
        }
        
        return events;
    }

    
    /** Go back thru the events & links up transitive effects with the events they referenced */ 
    protected void setEventLinks(Vector allEvents) {
     
        Iterator i = allEvents.iterator();
        while (i.hasNext() ) {
            EventDescription orig = (EventDescription)i.next();
            TransitiveEffectDescription ted = orig.getTransitiveEffect();
            if (ted != null) { // then find the event it refers to
                Iterator j = allEvents.iterator();
                boolean found = false;
                while (j.hasNext() ) {
                    EventDescription e = (EventDescription)j.next();
                    if (ted.getTransitiveEventName().equals(e.getName()) && ted.getTransitiveAssetType().equals(e.getAffectedAssetType() )) {
                        ted.setTransitiveEvent(e);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.error("Could not find event["+ted.getTransitiveEventName()+"] referenced in TransitiveEffect for Event["+orig.getName()+"].");
                }
            }
        }                
    }
    
    //publish all events & transitive effects at this point
    protected void publishEvents(org.cougaar.core.service.BlackboardService blackboard, Vector allEvents) {
        
        //link up transitive event names to actual events
        if (allEvents == null) {
            logger.warn("No events published the blackboard.");
            return;
        }
        
        Iterator i = allEvents.iterator();
        int tec = 0;
        while (i.hasNext() ) {
            EventDescription e = (EventDescription)i.next();
            blackboard.publishAdd(e);
            if (e.getTransitiveEffect() != null) {
                blackboard.publishAdd(e.getTransitiveEffect());
                tec++;
            }
        }
        if (logger.isDebugEnabled()) { logger.debug("Published " + events.size() + " event descriptions & " + tec + " transitive effects to the blackboard."); }
    }

    /** Parse DirectEffect*/
    private void parseDirectEffect(Element element, EventDescription event) {

        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Transition") ) {
                e = (Element)child;

                String whenState = e.getAttribute("WhenActualStateIs");
                String endState = e.getAttribute("EndStateWillBe");
                if (whenState == null || endState == null) {
                    logger.error("Event XML Error - one direct effect asset state is NULL for event["+event.getName()+"]! unknown EndStateWillBe: "+endState+ ", WhenActualStateIs: "+whenState);
                    return;
                }

                AssetState when_as;
                if (whenState.equals("*")) { when_as = AssetState.ANY; } 
                else {
                    when_as = event.getAffectedStateDimension().findAssetState(whenState);
                }
                if (when_as == null) {
                    logger.error("Event XML Error - direct effect asset state not found! unknown WhenActualStateIs: "+whenState+ " for asset type = " + event.getAffectedAssetType());
                    return;
                }
                
                AssetState end_as = event.getAffectedStateDimension().findAssetState(endState);
                if (end_as == null) {
                    logger.error("Event XML Error - direct effect asset state not found! unknown EndStateWillBe: "+endState+ " for asset type = " + event.getAffectedAssetType());
                    return;
                }
                
                
                //Add transition to Event
                event.addDirectEffectTransition(when_as, end_as);
                if (logger.isDebugEnabled()) logger.debug("Saw direct transition: "+whenState+", "+endState);
            } //else, likely a text element - ignore
        }
                
    }

    /** Parse TransitiveEffect. Expecting only ONE of these in 2004. If more in the XML, only the last will be used. */
    private void parseTransitiveEffect(Element element, EventDescription event) {

        TransitiveEffectDescription ted = null;
        TransitiveEffectVulnerabilityFilter vf = null;
        
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("CausesEvent") ) {
                e = (Element)child;

                String transName = e.getAttribute("name");
                String transType = e.getAttribute("assetType");
                AssetType assetType = AssetType.findAssetType(transType);
                if (assetType == null) {
                    logger.error("No AssetType for transitive effect [event="+event.getName()+"] with type="+transType+". Ignoring.");
                    return;
                } 
                if (! (event.getAffectedAssetType().isSupertypeOf(assetType))) { //then the event cannot have a containment relationship with it!
                    logger.error("Invalid containment relationship: Transitive effect's asset type ["+transType+"] is not a subtype of the event's["+event.getName()+"] assetType ["+event.getAffectedAssetType()+"]. Ignoring.");
                    return;
                } 
                
                ted = new TransitiveEffectDescription(transName, assetType, us.nextUID());
                event.setTransitiveEffect(ted);
                
            } else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("VulnerableAssets") ) {
                e = (Element)child;

                //if (logger.isDebugEnabled()) logger.debug("Calling TransitiveEffectVulnerabilityFilter, element name = " + e.getNodeName());
                vf = TransitiveEffectVulnerabilityLoader.parseElement(e,  probabilityMap, event);
                if (vf == null) {
                    logger.error("Error parsing XML file for event [" + event.getName() + "]. TransitiveEffectVulnerabilityFilter was null for element = "+e.getNodeName() );
                }                
            } //else, likely a text element - ignore
        }        
        
        if (ted != null) {
            ted.setTransitiveVulnerabilityFilter(vf);
            event.setTransitiveEffect(ted);
        } else {
            logger.error("Error parsing Event XML file for event [" + event.getName() + "]. TransitiveEffect element was null for element = "+element.getNodeName() );
        }                
    }
    
}