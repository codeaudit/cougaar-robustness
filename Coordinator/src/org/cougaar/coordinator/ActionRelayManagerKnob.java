/*
 * ActionRelayManagerKnob.java
 *
 * Created on January 21, 2004, 2:28 PM
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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
package org.cougaar.coordinator;

import org.cougaar.core.mts.MessageAddress;

/**
 *
 * @author  Administrator
 */
public class ActionRelayManagerKnob {
    
    private boolean shouldRelay;
    private MessageAddress coordinator;
    
    /** Creates a new instance of ActionRelayManagerKnob */
    public ActionRelayManagerKnob(boolean sr) {
        shouldRelay = sr;
        coordinator = null;
    }

    /** Creates a new instance of ActionRelayManagerKnob  - defaults to true */
    public ActionRelayManagerKnob() {
        shouldRelay = true;
        coordinator = null;
    }
    
    public boolean getShouldRelay() { return shouldRelay; }
    public void setShouldRelay(boolean sr) { shouldRelay = sr; }
    
    
    public boolean isCoordinatorLocated() { return (coordinator != null); }
    public void setFoundCoordinator(MessageAddress c) { coordinator = c; }
    public MessageAddress getCoordinatorAddress() { return coordinator; }
    
}
