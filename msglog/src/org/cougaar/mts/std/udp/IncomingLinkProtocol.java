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
 * 26 Apr 2002: Created from socket link protocol. (OBJS)
 */

package org.cougaar.mts.std.udp;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;


/**
 * Subclass of LinkProtocol, to allow for split transports.
**/

public abstract class IncomingLinkProtocol extends LinkProtocol
{
  private DestinationLink dummyDestLink;

  public IncomingLinkProtocol ()
  {
    dummyDestLink = new DummyDestinationLink (null);
  }

  public DestinationLink getDestinationLink (MessageAddress destination) 
  {
    return dummyDestLink;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    return false;  // we are not an Outgoing transport
  }

  private class DummyDestinationLink implements DestinationLink 
  {
    MessageAddress destination;

    DummyDestinationLink (MessageAddress dest) 
    {
      destination = dest;
    }

    public MessageAddress getDestination () 
    {
      return destination;
    }

    public String toString ()
    {
      return "IncomingUDPLinkProtocol";
    }

    public Class getProtocolClass ()
    {
      return IncomingLinkProtocol.class;
    }
 
    public int cost (AttributedMessage message) 
    {
      return Integer.MAX_VALUE;
    }

    public Object getRemoteReference ()
    {
      return null;
    }

    public MessageAttributes forwardMessage (AttributedMessage message) 
      throws NameLookupException, UnregisteredNameException,
             CommFailureException, MisdeliveredMessageException
    {
      throw new CommFailureException (new Exception ("DummyDestinationLink!!!"));
    }

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return false;  // incoming transport
    }

    public void addMessageAttributes (MessageAttributes attrs) 
    {
    }
  }    
}
