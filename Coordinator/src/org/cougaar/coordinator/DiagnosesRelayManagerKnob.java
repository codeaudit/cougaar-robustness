/*
 * DiagnosesRelayManagerKnob.java
 *
 * Created on January 21, 2004, 2:28 PM
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


package org.cougaar.coordinator;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

/**
 *
 * @author  Administrator
 */
public class DiagnosesRelayManagerKnob  implements NotPersistable, Serializable {
    
    private boolean shouldRelay;
    private MessageAddress coordinator;
    
    /** Creates a new instance of DiagnosesRelayManagerKnob */
    public DiagnosesRelayManagerKnob(boolean sr) {
        shouldRelay = sr;
        coordinator = null;
    }

    /** Creates a new instance of DiagnosesRelayManagerKnob  - defaults to true */
    public DiagnosesRelayManagerKnob() {
        shouldRelay = true;
        coordinator = null;
    }
    
    public boolean getShouldRelay() { return shouldRelay; }
    public void setShouldRelay(boolean sr) { shouldRelay = sr; }
    
    
    public boolean isCoordinatorLocated() { return (coordinator != null); }
    public void setFoundCoordinator(MessageAddress c) { coordinator = c; }
    public MessageAddress getCoordinatorAddress() { return coordinator; }
    
}
