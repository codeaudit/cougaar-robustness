/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
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
 * 25 Jul 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.component.Service;

public interface RTTService extends Service
{
  public void setInitialCommRTTForLinkPair (DestinationLink sendLink, DestinationLink recvLink, int rtt);
  public void setInitialCommRTTForLinkPair (String sendLink, String recvLink, int rtt);
  public void setInitialNodeTime (int nodeTime);
  public boolean isSomeCommRTTStartDelaySatisfied (DestinationLink sendLink, String node);
  public boolean isCommRTTStartDelaySatisfied (DestinationLink sendLink, String node, DestinationLink recvLink);
  public float getHighestCommRTTPercentFilled (DestinationLink sendLink, String node);
  public float getCommRTTPercentFilled (DestinationLink sendLink, String node, DestinationLink recvLink);
  public int getBestCommRTTForLink (DestinationLink link, String node);
  public int getBestFullRTTForLink (DestinationLink link, String node);
  public void updateInbandRTT (DestinationLink sendLink, String node, int rtt);
  public void updateInbandRTT (String node, String recvLink, int rtt);
}
