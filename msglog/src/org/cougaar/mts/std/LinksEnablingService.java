/*
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.mts.std;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

public interface LinksEnablingService extends Service
{
    /*
     *  Register an agent if not already registered.
     */
    public void register(MessageAddress agent);
    
    /*
     *  Get Link Selection Advice for messaging to remote agent.
     */
    public String getAdvice(MessageAddress remoteAgent);	

    /*
     *  Set Link Selection Advice for messaging to remote agent.
     */
    public void setAdvice(MessageAddress remoteAgent, String advice);

    /*
     *  Is messaging enabled to remote agent?
     */
    public boolean isEnabled(MessageAddress remoteAgent);

    /*
     *  Enable messaging to remote agent (reverts to previous advice).
     */
    public void enable(MessageAddress agent);

}

