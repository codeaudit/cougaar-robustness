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
 * 26 Mar  2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 20 Nov  2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 26 Sept 2001: Rename: MessageTransport to LinkProtocol. (OBJS)
 * 16 Sept 2001: Updated from Cougaar 8.3.1 to 8.4. (OBJS)
 * 09 Sept 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import org.cougaar.core.mts.*;


/**
 * Subclass of LinkProtocol, to allow for split message transports.
**/

public abstract class IncomingLinkProtocol extends LinkProtocol
{
  protected DestinationLink dummyDestLink;

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

  public String getTransportType ()
  {
    return "email";
  }

  public boolean isIncomingTransport ()
  {
    return true;
  }

  public boolean isOutgoingTransport ()
  {
    return false;
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
      return "IncomingEmailLinkProtocol";
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

    public void addMessageAttributes (MessageAttributes attrs)
    {
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
  }    
}
