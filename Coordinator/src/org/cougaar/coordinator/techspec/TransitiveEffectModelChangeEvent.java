/*
 * TransitiveEffectModelChangeEvent.java
 *
 * Created on May 7, 2004, 10:03 AM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */

package org.cougaar.coordinator.techspec;


//import org.cougaar.util.log.Logging;
//import org.cougaar.util.log.Logger;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.persist.NotPersistable;

/**
 * This class describes a filter that is used to determine membership in a transitiveEffectModel
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class TransitiveEffectModelChangeEvent implements ChangeReport, NotPersistable {
    
    public static final EventType DISTRIBUTION_CHANGE = new TransitiveEffectModelChangeEvent.EventType();
    public static final EventType LIKELIHOOD_CHANGE = new TransitiveEffectModelChangeEvent.EventType();
    public static final EventType MEMBERSHIPFILTER_CHANGE = new TransitiveEffectModelChangeEvent.EventType();
    public static final EventType MEMBERSHIP_CHANGE = new TransitiveEffectModelChangeEvent.EventType();

    private TransitiveEffectModel model;
    private EventType eventType;
    
    /** Creates a new instance of TransitiveEffectModelChangeEvent */
    public TransitiveEffectModelChangeEvent(TransitiveEffectModel model, EventType eventType) {
        this.model = model;
        this.eventType = eventType;
    }
    
    public TransitiveEffectModelChangeEvent.EventType getEventType() { return eventType; }
    public TransitiveEffectModel getTransitiveEffectModel() { return model; }
    
    static class EventType{
        private EventType() {}
    }
    
}
