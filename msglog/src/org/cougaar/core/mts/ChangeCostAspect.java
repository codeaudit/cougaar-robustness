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
 * 17 Apr 2002: Update from Cougaar 9.0.0 to 9.1.x (OBJS)
 * 21 Mar 2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 24 Nov 2001: Created. (OBJS)
 */

package org.cougaar.core.mts;


/**
 * Use this aspect to change the cost of RMILinkProtocol and
 * SSLRMILinkProtocol links. 
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.RMILinkProtocol.cost</b>
 * To modify the cost of a link, set this property to an integer. 
 * <br>(e.g. -Dorg.cougaar.message.protocol.RMILinkProtocol.cost=100)
 * <p>
 * <b>org.cougaar.message.protocol.SSLRMILinkProtocol.cost</b>
 * To modify the cost of a link, set this property to an integer. 
 * <br>(e.g. -Dorg.cougaar.message.protocol.SSLRMILinkProtocol.cost=100)
 * <p>
 * <b>org.cougaar.message.transport.aspects</b>
 * To cause this aspect to be loaded at init time, add 
 * org.cougaar.core.mts.ChangeCostAspect to this property.
 * <br>(e.g. -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.ChangeCostAspect)
 * */

public class ChangeCostAspect extends StandardAspect
{

  private static final String RMICOST = "org.cougaar.message.protocol.RMILinkProtocol.cost";
  private static final String SSLCOST = "org.cougaar.message.protocol.SSLRMILinkProtocol.cost";
  private int rmicost;
  private int sslcost;

  public ChangeCostAspect () {
    rmicost = Integer.getInteger(RMICOST,-1).intValue();
    sslcost = Integer.getInteger(SSLCOST,-1).intValue();
  }

  public Object getDelegate(Object delegate, Class type) {
    if (type == DestinationLink.class) {
      return new Link((DestinationLink)delegate);
    } else return null;
  }

  private class Link extends DestinationLinkDelegateImplBase 
  {
    DestinationLink link;

    private Link (DestinationLink l) {
      super (l);
      link = l;
    }

    public int cost (AttributedMessage message) {
      if (rmicost != -1 
          && link.getProtocolClass().getName().equals("org.cougaar.core.mts.RMILinkProtocol")) 
        return rmicost;
      else if (sslcost != -1 
               && link.getProtocolClass().getName().equals("org.cougaar.core.mts.SSLRMILinkProtocol")) 
        return sslcost;
      else return link.cost(message);
    }
  }
}
