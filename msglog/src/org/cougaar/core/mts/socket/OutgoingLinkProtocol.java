/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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
 *
 * CHANGE RECORD
 * 16 May 2002: Port to Cougaar 9.2.x (OBJS)
 * 21 Mar 2002: Port from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol. (OBJS)
 * 14 Sep 2001: Port from Cougaar 8.3.1 to 8.4 (OBJS)
 * 08 Sep 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.socket;

import org.cougaar.core.mts.*;


/**
 * Subclass of LinkProtocol, to allow for split transports.
 */

public abstract class OutgoingLinkProtocol extends LinkProtocol
{
  public OutgoingLinkProtocol () // (AspectSupport aspectSupport)
  {
    //super (aspectSupport);
  }

  public void registerClient (MessageTransportClient client) 
  {}

  public void unregisterClient (MessageTransportClient client) 
  {}

  public final void registerMTS (MessageAddress addr)
  {
    //  ???
  }
}
