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
 * 21 Mar  2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 20 Nov  2001: Cougaar 8.6.1 compatibility changes (marked 8.6.1) (OBJS)
 * 27 Oct  2001: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.HashMap;


/**
 *  An aspect which prints a ">" followed by a single character
 *  each time a LinkProtocol successfully sends a message. 
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.transport.aspects</b>
 * To cause this aspect to be loaded at init time, add 
 * org.cougaar.core.mts.ShowTrafficAspect to this property.
 * <br>(e.g. -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.ShowTrafficAspect)
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
/* 8.6.1
        if (protocolClass.getName().equals("org.cougaar.core.mts.LoopbackLinkProtocol")) 
        {
          System.err.print (">L");
        } 
        else 
        {
          String prot = (String) protocolClass.getField("PROTOCOL_TYPE").get(null); 
          char statusChar = Character.toUpperCase (prot.charAt (1));
8.6.1 */
        String name = protocolClass.getName();                                                            //8.6.1
        char statusChar;                                                                                  //8.6.1
        if (name.equals("org.cougaar.core.mts.LoopbackLinkProtocol")) statusChar = 'L';                   //8.6.1
        else if (name.equals("org.cougaar.core.mts.RMILinkProtocol")) statusChar = 'R';                   //8.6.1
        else if (name.equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol")) statusChar = 'E';   //8.6.1
        else if (name.equals("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol")) statusChar = 'S'; //8.6.1
        else if (name.equals("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol")) statusChar = 'U'; 
        else if (name.equals("org.cougaar.core.mts.NNTPLinkProtocol")) statusChar = 'N';                  //8.6.1
        else if (name.equals("org.cougaar.core.mts.SSLRMILinkProtocol")) statusChar = 'V';                //8.6.1
        else if (name.equals("org.cougaar.core.mts.SerializedRMILinkProtocol")) statusChar = 'Z';         //8.6.1
        else if (name.equals("org.cougaar.core.mts.FutileSerializingRMILinkProtocol")) statusChar = 'F';  //8.6.1
        else if (name.equals("org.cougaar.lib.quo.CorbaLinkProtocol")) statusChar = 'C';                  //8.6.1
        else statusChar = '?';                                                                            //8.6.1

        System.err.print (">"+statusChar);
      } 
      catch (Exception e) { e.printStackTrace(); }
    }
  }
}
