/*
 * <copyright>
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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

package org.cougaar.mts.std.acking;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.std.AgentID;
import org.cougaar.core.component.Service;

public interface MessageAckingService extends Service
{
    /*
     * Retarget messages queued for an agent that has since restarted.
     */
    public void handleMessagesToRestartedAgent(AgentID localAgent,
					       AgentID oldRestartedAgent,
					       AgentID newRestartedAgent) 
	throws NameLookupException, CommFailureException;
    
    /*
     * Hold (don't resend) any messages to this address until released.
     */
    public void hold (MessageAddress addr);
    
    /*
     * Release (for possible resend) any messages to this address that are on hold.
     */
    public void release (MessageAddress addr);
}

