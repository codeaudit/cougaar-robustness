/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * Copyright 2002 Object Services and Consulting, Inc.
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.tools.robustness.sensors;

import java.util.Set;
import java.util.Collections;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;
import org.cougaar.core.util.UniqueObject;
import java.util.Date;

/**
 * A Plugin developer requests a ping by publishing a PingRequest on the 
 * blackboard of a local agent with PingRequesterPlugin loaded.
 **/
public class PingRequest implements UniqueObject
{
  /**
  * A new PingRequest that has not yet resulted in a Ping being sent by PingRequesterPlugin to target Agent.
  */
  public static final int NEW      = 0;

  /**
  * A PingRequest that has resulted in a Ping being sent by PingRequesterPlugin to target Agent.
  */
  public static final int SENT     = 1;

  /**
  * A PingRequest for which a Ping has been echoed by PingServerPlugin in target Agent.
  */
  public static final int RECEIVED = 2;

  /**
  * A PingRequest for which a Ping has not been echoed before timing out or failing in some other way.
  */
  public static final int FAILED   = 3;

  private UID uid;
  private MessageAddress source;
  private MessageAddress target;
  private long timeout;  // milliseconds
  private int status;
  private Date timeSent;
  private Date timeReceived;
  private long roundTripTime; //milliseconds

  /**
   * @param source MessageAddress Agent requesting the ping
   * @param target MessageAddress Agent to be pinged
   * @param timeout long Timeout in milliseconds
   */
  public PingRequest(UID uid, MessageAddress source, MessageAddress target, long timeout) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.timeout = timeout;
    this.status = NEW; 
  }

  // UniqueObject implementation

  /**
  * Get the UID of this request.
  */
  public UID getUID () { return uid; } 

  /**
  * UIDs are not permitted to be set except by constructor, so this method throws an exception.
  *
  * @throws java.lang.RuntimeException
  **/ 
  public void setUID(UID uid) { throw new RuntimeException("Attempt to change UID"); }

  /**
  * Get the address of the Agent that sent this request.
  */
  public MessageAddress getSource() { return source; }
  public void setSource(MessageAddress addr) { source = addr; }

  /**
  * Get the address of the Agent to be pinged.
  */
  public MessageAddress getTarget() { return target; }
  
  /**
  * Get the number of milliseconds that either source or target agents should wait before giving up sending a ping.
  */
  public long getTimeout() { return timeout; }

  /**
  * Get the status of the request (i.e. NEW, SENT, RECEIVED, FAILED).
  */
  public int getStatus() { return status; }
  public void setStatus(int status) { this.status = status; }

  /**
  * Get the time that the Ping was sent to the target Agent.
  */
  public Date getTimeSent() { return timeSent; }
  public void setTimeSent(Date timeSent) { this.timeSent = timeSent; }

  /**
  * Get the time that the response was received from the target Agent.
  */
  public Date getTimeReceived() { return timeReceived; }
  public void setTimeReceived(Date timeReceived) { this.timeReceived = timeReceived; }

  /**
  * Get the number of milliseconds between sending the Ping and receiving a response.
  */
  public long getRoundTripTime() { return roundTripTime; }
  public void setRoundTripTime(long roundTripTime) { this.roundTripTime = roundTripTime; }

  public String toString() {
    return "(PingRequest: " + uid 
                     + ", " + source 
                     + ", " + target
                     + ", " + timeout
                     + ", " + status
                     + ", " + timeSent
                     + ", " + timeReceived
                     + ", " + roundTripTime + ")";
  }

}
