/*
 * ThreatModelChangeEvent.java
 *
 * Created on September 22, 2003, 9:45 AM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.techspec;


//import org.cougaar.util.log.Logging;
//import org.cougaar.util.log.Logger;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.persist.NotPersistable;

/**
 * This class describes a filter that is used to determine membership in a threatModel
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class ThreatModelChangeEvent implements ChangeReport, NotPersistable {
    
    public static final EventType DISTRIBUTION_CHANGE = new ThreatModelChangeEvent.EventType();
    public static final EventType LIKELIHOOD_CHANGE = new ThreatModelChangeEvent.EventType();
    public static final EventType MEMBERSHIPFILTER_CHANGE = new ThreatModelChangeEvent.EventType();
    public static final EventType MEMBERSHIP_CHANGE = new ThreatModelChangeEvent.EventType();

    private ThreatModelInterface model;
    private EventType eventType;
    
    /** Creates a new instance of ThreatModelChangeEvent */
    public ThreatModelChangeEvent(ThreatModelInterface model, EventType eventType) {
        this.model = model;
        this.eventType = eventType;
    }
    
    public ThreatModelChangeEvent.EventType getEventType() { return eventType; }
    public ThreatModelInterface getThreatModel() { return model; }
    
    static class EventType{
        private EventType() {}
    }
    
}
