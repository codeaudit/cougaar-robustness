/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

package org.cougaar.tools.robustness.ma.ldm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.persist.NotPersistable;

/**
 * Implementation of NodeStatusRelay interface.
 **/
public class NodeStatusRelayImpl
    implements NodeStatusRelay, NotPersistable, java.io.Serializable {

  private MessageAddress source;
  private String communityName;
  private String leaderVote;
  private int nodeStatus;
  private String location;
  private AgentStatus[] agentStatus;
  private UID uid;

  /**
   * Constructor.
   */
  public NodeStatusRelayImpl(MessageAddress source,
                             String         communityName,
                             int            nodeStatus,
                             AgentStatus[]  agentStatus,
                             String         leader,
                             String         location,
                             UID            uid) {
    this.source = source;
    this.communityName = communityName;
    this.nodeStatus = nodeStatus;
    this.agentStatus = agentStatus;
    this.leaderVote = leader;
    this.location = location;
    this.uid = uid;
  }

  public AgentStatus[] getAgentStatus() {
    return agentStatus;
  }
  public void setAgentStatus(AgentStatus[] status) {
    agentStatus = status;
  }

  public String getLeaderVote() {
    return leaderVote;
  }
  public void setLeaderVote(String leader) {
    leaderVote = leader;
  }

  public int getNodeStatus() {
    return this.nodeStatus;
  }

  public String getCommunityName() {
    return communityName;
  }

  public String getLocation() {
    return this.location;
  }

  public void setLocation(String loc) {
    this.location = loc;
  }

  //
  // Relay.Target Interface methods
  //
  public Object getResponse() {
    return null;
  }

  public MessageAddress getSource() {
    return source;
  }

  public int updateContent(Object content, Relay.Token token) {
    try {
      NodeStatusRelay nsr = (NodeStatusRelay)content;
      communityName = nsr.getCommunityName();
      agentStatus = nsr.getAgentStatus();
      leaderVote = nsr.getLeaderVote();
    } catch (Exception ex) {
      System.out.println(ex + ", " + content.getClass().getName());
      ex.printStackTrace();
    }
    return Relay.CONTENT_CHANGE;
  }

  //
  // UniqueObject Interface methods
  //
  public void setUID(UID uid) {
    if (uid != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }
    this.uid = uid;
  }
  public UID getUID() {
    return this.uid;
  }
}
