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

/**
 * Implementation of HealthMonitorRequest type.  Instances of this type are
 * used by external (non-robustness) agents to request operations to be
 * performed by restart manager.  This type is also used internally to
 * send requests between the robustness manager and node agents.  When used
 * by non-robustness clients the request should be published to the manager
 * agent, this is most easily accomplished via a AttributeBasedAddress defining
 * the robustness community and manager attribute (i.e., "Role=RobustnessManager").
 * For example:
 * <pre>
 * HealthMonitorRequest myRequest =
 *   new HealthMonitorRequestImpl(communityName,
 *                                HealthMonitorRequest.FORCED_RESTART,
 *                                new String[] {mobileAgent},
 *                                null,
 *                                null,
 *                                uidService.nextUID()));
 * AttributeBasedAddress target =
 *   AttributeBasedAddress.getAttributeBasedAddress(communityName,
 *                                                  "Role",
 *                                                  "RobustnessManager");
 * Object requestToPublish = myRequest;
 * if (!target.equals(agentId)) {  // send to remote agent using Relay
 *   request = new RelayAdapter(agentId, hmr, hmr.getUID());
 *   ((RelayAdapter)request).addTarget(target);
 * }
 * blackboard.publishAdd(request);
 * </pre>
 **/
public class HealthMonitorRequestImpl
  implements HealthMonitorRequest, java.io.Serializable {

  private MessageAddress source;
  private String communityName;
  private int requestType = UNDEFINED;
  private String agents[];
  private String destNode;
  private String origNode;
  private UID uid;
  private HealthMonitorResponse resp;

  /**
   * Construct a HealthMonitorRequest.  When this request is sent to the
   * robustness manager the orig and dest fields can be null.  The only exception
   * to this is a MOVE request in which case the dest field must be specified.
   * However, if this request is sent directly to a robustness node agent the orig
   * field is also required for the MOVE and KILL request.
   * @param communityName Name of agents robustness community
   * @param reqType       Action to be performed (i.e., MOVE, RESTART, etc)
   * @param agents        Name of affected agents
   * @param orig          Name of agents current node (Required for MOVE)
   * @param dest          Name of agents destination node
   * @param               Unique identifier
   */
  public HealthMonitorRequestImpl(MessageAddress     source,
                                  String             communityName,
                                  int                reqType,
                                  String[]           agents,
                                  String             orig,
                                  String             dest,
                                  UID                uid) {
    this.source = source;
    this.communityName = communityName;
    this.requestType = reqType;
    this.agents = agents;
    this.origNode = orig;
    this.destNode = dest;
    this.uid = uid;
  }

  public String getCommunityName() {
    return communityName;
  }

  /**
   * Defines the type of request.
   *
   * @param reqType Request type
   */
  public void setRequestType(int reqType) {
    this.requestType = reqType;
  }


  public int getRequestType() {
    return this.requestType;
  }


  public void setResponse(HealthMonitorResponse resp) {
    this.resp = resp;
  }

  public String[] getAgents() {
    return this.agents;
  }

  public String getDestinationNode() {
    return this.destNode;
  }

  public String getOriginNode() {
    return this.origNode;
  }

  //
  // Relay.Target Interface methods
  //
  public Object getResponse() {
    return resp;
  }

  public MessageAddress getSource() {
    return source;
  }

  public int updateContent(Object content, Relay.Token token) {
    HealthMonitorRequest hmr = (HealthMonitorRequest)content;
    this.communityName = hmr.getCommunityName();
    this.requestType = hmr.getRequestType();
    this.agents = hmr.getAgents();
    this.destNode = hmr.getDestinationNode();
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


  public String toXML() {
    StringBuffer sb = new StringBuffer();
    sb.append("<HealthMonitorRequest type=\"" + getRequestTypeAsString() + "\"" +
      " community=\"" + communityName + "\"" +
      " source=\"" + getSource() + "\"" +
      " agents=\"" + agents + "\"" +
      " orig=\"" + origNode + "\"" +
      " dest=\"" + destNode + "\" />\n");
    return sb.toString();
  }

  public String getRequestTypeAsString() {
    switch (requestType) {
      case HealthMonitorRequest.UNDEFINED: return "UNDEFINED";
      case HealthMonitorRequest.RESTART: return "RESTART";
      case HealthMonitorRequest.KILL: return "KILL";
      case HealthMonitorRequest.FORCED_RESTART: return "FORCED_RESTART";
      case HealthMonitorRequest.MOVE: return "MOVE";
      case HealthMonitorRequest.GET_STATUS: return "GET_STATUS";
      case HealthMonitorRequest.LOAD_BALANCE: return "LOAD_BALANCE";
      case HealthMonitorRequest.ADD: return "ADD";
    }
    return "INVALID_VALUE";
  }

  /**
   * Returns a string representation of the request
   *
   * @return String - a string representation of the request.
   **/
  public String toString() {
    return "HealthMonitorRequest:" +
           " request=" + getRequestTypeAsString() +
           " community=" + communityName +
           " source=" + source +
           " agents=" + agentNamesToString(agents) +
           " orig=" + origNode +
           " dest=" + destNode;
  }

  public String agentNamesToString(String[] agentNames) {
    if (agentNames == null) {
      return "null";
    } else {
      StringBuffer sb = new StringBuffer("[");
      for (int i = 0; i < agentNames.length; i++) {
        sb.append(agentNames[i]);
        if (i < agentNames.length - 1)
          sb.append(",");
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
