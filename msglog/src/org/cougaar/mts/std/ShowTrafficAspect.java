/*
 * <copyright>
 *  Copyright 2001-2004 Object Services and Consulting, Inc. (OBJS),
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
 * 20 Mar  2003: Removed call to System.err. (OBJS)
 * 21 Mar  2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 20 Nov  2001: Cougaar 8.6.1 compatibility changes (marked 8.6.1) (OBJS)
 * 27 Oct  2001: Created. (OBJS)
 */

package org.cougaar.mts.std;

import java.util.HashMap;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;


/**
 *  An aspect which prints a ">" followed by a single character
 *  each time a LinkProtocol successfully sends a message. 
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.transport.aspects</b>
 * To cause this aspect to be loaded at init time, add 
 * org.cougaar.mts.std.ShowTrafficAspect to this property.
 * <br>(e.g. -Dorg.cougaar.message.transport.aspects=org.cougaar.mts.std.ShowTrafficAspect)
 * */

public class ShowTrafficAspect extends StandardAspect
{
  public ShowTrafficAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
	if (type == DestinationLink.class) 
    {
	    return new Link ((DestinationLink)delegate);
	} 
    else return null;
  }

  private class Link extends DestinationLinkDelegateImplBase 
  {
    DestinationLink link;

    private Link (DestinationLink link) 
    {
      super (link);
      this.link = link;
    }
            
    public MessageAttributes forwardMessage (AttributedMessage message) 
      throws UnregisteredNameException, NameLookupException, 
             CommFailureException, MisdeliveredMessageException
    {
      MessageAttributes attrs = link.forwardMessage (message);

      //  Print the traffic indicator here, after a successful send.
      //  If unsuccessful, an exception would be thrown above.

//  HACK

Object attr = attrs.getAttribute (MessageAttributes.DELIVERY_ATTRIBUTE);
if (attr.equals (MessageAttributes.DELIVERY_STATUS_DELIVERED)) showTraffic (link);

      return attrs;
    }

    /**
     *  Prints a ">" followed by the second character (after the dash(-))   
     *  from the static field PROTOCOL_TYPE in the LinkProtocol class
     *  in which a DestinationLink is embedded as an indicator of which 
     *  protocol was actually used to send the message. 
     **/

    private void showTraffic (DestinationLink link)
    {
      try 
      { 
        Class protocolClass = link.getProtocolClass();
        String name = protocolClass.getName();                                                        
        char statusChar;                                                                             
        if      (name.equals("org.cougaar.mts.base.LoopbackLinkProtocol"))             statusChar = 'L';
        else if (name.equals("org.cougaar.mts.base.RMILinkProtocol"))                  statusChar = 'R';
        else if (name.equals("org.cougaar.mts.std.SSLRMILinkProtocol"))                statusChar = 'S';
        else if (name.equals("org.cougaar.mts.std.email.OutgoingEmailLinkProtocol"))   statusChar = 'E';
        else if (name.equals("org.cougaar.mts.std.socket.OutgoingSocketLinkProtocol")) statusChar = 'T'; // for TCP
        else if (name.equals("org.cougaar.mts.std.udp.OutgoingUDPLinkProtocol"))       statusChar = 'U';
        else if (name.equals("org.cougaar.mts.std.NNTPLinkProtocol"))                  statusChar = 'N';
        else if (name.equals("org.cougaar.mts.std.SerializedRMILinkProtocol"))         statusChar = 'X';
        else if (name.equals("org.cougaar.mts.std.FutileSerializingLinkProtocol"))     statusChar = 'F';
        else if (name.equals("org.cougaar.lib.mquo.CorbaLinkProtocol"))                statusChar = 'C';
        else                                                                           statusChar = '?';

        System.out.print(">"+statusChar);
      } 
      catch (Exception e) { e.printStackTrace(); }
    }
  }
}
