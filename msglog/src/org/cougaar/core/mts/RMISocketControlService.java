/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.mts;

import org.cougaar.core.component.Service;

import java.rmi.Remote;
import java.net.Socket;

public interface RMISocketControlService extends Service
{
    /**
     * The SO Timeout is set for ALL sockets that go to the remote RMI
     * reference The side effect of this is that other agents that are
     * on the same node will also have their time out changed.
     */
    boolean setSoTimeout(MessageAddress addr, int timeout);

    /** 
     * The RMILinkProtocol calls this method, Other Aspects should not
     * call this method.
     */
    void setReferenceAddress(Remote reference, 
				    MessageAddress addr);

    /**
     * Get the socket associated with this MessageAddress.
     * Used by RMISendTimeoutAspect.
     */
    Socket getSocket(MessageAddress addr); 
}
