/*
 * <copyright>
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
package org.cougaar.tools.robustness.ma.controllers;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.tools.robustness.ma.util.HeartbeatHelper;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.PingListener;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.RestartListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.DeconflictHelper;
import org.cougaar.tools.robustness.ma.util.DeconflictListener;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;
import java.util.*;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Robustness controller base class (for sledgehammer use case).  Dispatches
 * state controllers in response to raw events received from
 * CommunityStatysModel.  Also provides a number of utility methods and helper
 * classes for use by state controllers.
 */
public abstract class RobustnessControllerBase
    implements RobustnessController, StatusChangeListener {

  protected static final int NEVER = -1;

  // Default parameter values, may be overridden by community attributes
  protected static long DEFAULT_EXPIRATION = 2 * 60 * 1000;
  protected static final long PING_TIMEOUT = 2 * 60 * 1000;
  protected static final long HEARTBEAT_REQUEST_TIMEOUT = 1 * 60 * 1000;
  protected static final long HEARTBEAT_FREQUENCY = 60 * 1000;
  protected static final long HEARTBEAT_TIMEOUT = 2 * 60 * 1000;
  protected static final long HEARTBEAT_PCT_OUT_OF_SPEC = 50;

  public static final String deconflictionProperty = "org.cougaar.tools.robustness.restart.deconfliction";
  public static final String defaultEnabled = "ENABLED";


  /**
   * Inner class used to maintain info about registered state controller.
   */
  class ControllerEntry {
    private int state;
    private String stateName;
    private StateController controller;
    private ControllerEntry(int state, String name, StateController sc) {
      this.state = state;
      this.stateName = name;
      this.controller = sc;
    }
  }

  // Map of ControllerEntry instances
  private Map controllers = new HashMap();

  // Helper classes
  private HeartbeatHelper heartbeatHelper;
  private MoveHelper moveHelper;
  private PingHelper pingHelper;
  private RestartHelper restartHelper;
  private LoadBalancer loadBalancer;
  private DeconflictHelper deconflictHelper = null;
  private DeconflictListener dl = null;

  protected MessageAddress agentId;
  protected LoggingService logger;
  protected EventService eventService;
  private BindingSite bindingSite;

  // Status model containing current information about monitored community
  protected CommunityStatusModel model;

  /**
   * @param agentId    Agent address
   * @param bs         BindingSite
   * @param csm        CommunityStatusModel for monitored community
   */
  public void initialize(MessageAddress agentId,
                         final          BindingSite bs,
                         final          CommunityStatusModel csm) {
    this.agentId = agentId;
    this.model = csm;
    this.bindingSite = bs;
    logger =
      (LoggingService)bs.getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    eventService = (EventService) bs.getServiceBroker().getService(this, EventService.class, null);
    heartbeatHelper = new HeartbeatHelper(bs);
    moveHelper = new MoveHelper(bs);
    pingHelper = new PingHelper(bs);
    restartHelper = new RestartHelper(bs);
    loadBalancer = new LoadBalancer(bs, this);
  }

  /**
   * Add a new StateController.
   * @param state     State number
   * @param stateName
   * @param sc        StateController
   */
  public void addController(int state, final String stateName, StateController sc) {
    logger.debug("Adding state controller: state=" + stateName);
    if (sc instanceof StateControllerBase) {
      final StateControllerBase scb = (StateControllerBase) sc;
      new Thread() {
        public void run() {
          try {
            scb.setBindingSite(bindingSite);
            scb.initialize(bindingSite, model);
            scb.load();
            scb.start();
          }
          catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
          }
        }
      }.start();
    }
    controllers.put(new Integer(state), new ControllerEntry(state, stateName, sc));
  }

  public BindingSite getBindingSite() {
    return bindingSite;
  }

  /**
   * Get reference to controllers move helper.
   */
  public MoveHelper getMoveHelper() {
    return moveHelper;
  }

  /**
   * Get reference to controllers heartbeat helper.
   */
  public HeartbeatHelper getHeartbeatHelper() {
    return heartbeatHelper;
  }

  /**
   * Get reference to controllers restart helper.
   */
  public RestartHelper getRestartHelper() {
    return restartHelper;
  }

  /**
   * Get reference to controllers ping helper.
   */
  public PingHelper getPingHelper() {
    return pingHelper;
  }

  /**
   * Get reference to controllers deconflict helper.
   */
  public DeconflictHelper getDeconflictHelper() {
    return deconflictHelper;
  }

  /**
   * Get reference to controllers load balancer interface.
   */
  public LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  /**
   * Default handler for receiving and dissemminating model change events.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  public void statusChanged(CommunityStatusChangeEvent[] csce) {
                            //CommunityStatusModel csm) {
    boolean interestingChange = false;
    for(int i = 0; i < csce.length; i++) {
      if(logger.isDebugEnabled()) {
        logger.debug(csce[i].toString());
      }
      if(csce[i].membersAdded() || csce[i].membersRemoved()) {
        processMembershipChanges(csce[i]);
        interestingChange = true;
      }
      if(csce[i].stateChanged()) {
        processStatusChanges(csce[i]);
        interestingChange = true;
      }
      if(csce[i].leaderChanged()) {
        processLeaderChanges(csce[i]);
        if(!(csce[i].getCurrentLeader() == null && csce[i].getPriorLeader() == null))
          interestingChange = true;
      }
      if(csce[i].locationChanged()) {
        processLocationChanges(csce[i]);
      }
      if(csce[i].stateExpired()) {
        processStatusExpirations(csce[i]);
      }
    }
    if(interestingChange && isLeader(agentId.toString()) && logger.isInfoEnabled()) {
      logger.info(statusSummary());
    }
  }

  /**
   * Default handler for membership changes in monitored community.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processMembershipChanges(CommunityStatusChangeEvent csce) {
    //robustness manager should publish deconfliction objects for every agent member.
    if(deconflictHelper != null) {
      deconflictHelper.initObjs();
    }
  }

  private static final String ROBUSTNESS_MANAGER = "RobustnessManager";

  /**
   * Default handler for changes in health monitor leader.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processLeaderChanges(CommunityStatusChangeEvent csce) {
    String manager = model.getStringAttribute(ROBUSTNESS_MANAGER); //the robustness manager
    //the robustness manager invokes DeconflictHelper object and add necessary deconflict
    //listener.
    if(agentId.toString().equals(manager) && deconflictHelper == null) {
      String enable = System.getProperty(deconflictionProperty, defaultEnabled);
      if(enable.equals(defaultEnabled)) {
        deconflictHelper = new DeconflictHelper(bindingSite, model);
        if (dl != null)
          deconflictHelper.addListener(dl);
        logger.info("==========deconflictHelper applies to " + thisAgent);
        deconflictHelper.initObjs();
      }
    }

    leaderChange(csce.getPriorLeader(), csce.getCurrentLeader());
  }

  /**
   * Receives notification of leader changes.
   */
  public void leaderChange(String priorLeader, String newLeader) {
  }

  /**
   * Processes state changes in a monitored agent/node.  When a state change
   * occurs the following actions are performed:
   * <pre>
   *   1) The expiration value for the new state expiration is updated using
   *      community attribute data.
   *   2) The enter() method is called on the new state handler
   *   3) The exit() method is called on the old state handler
   * </pre>
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processStatusChanges(CommunityStatusChangeEvent csce) {
    // Notify controllers of state transition
    logger.debug("State change:" +
                " name=" + csce.getName() +
                " state=" + stateName(csce.getCurrentState()) +
                " prior=" + stateName(csce.getPriorState()));
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      ControllerEntry ce = (ControllerEntry)it.next();
      if (ce.state == csce.getCurrentState()) {
        setExpiration(csce.getName(), stateName(csce.getCurrentState()));
        ce.controller.enter(csce.getName());
      }
      if (ce.state == csce.getPriorState()) {
        ce.controller.exit(csce.getName());
      }
    }
  }

  /**
   * Default handler for changes in agent location.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processLocationChanges(CommunityStatusChangeEvent csce) {
  }

  protected void processStatusExpirations(CommunityStatusChangeEvent csce) {
    logger.debug("Status expiration:" +
                " name=" + csce.getName() +
                " state=" + stateName(csce.getCurrentState()));
    // Notify controllers of state expirations
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      ControllerEntry ce = (ControllerEntry)it.next();
      if (ce.state == csce.getCurrentState()) {
        ce.controller.expired(csce.getName());
      }
    }
  }

  /**
   * Receives notification of change in agent location.
   */
  public void locationChange(String name, String priorLocation, String newLocation) {
  }

  /**
   * Returns name of state.
   * @param state  State code
   * @return       String representation of state code
   */
  public String stateName(int state) {
    ControllerEntry ce = (ControllerEntry)controllers.get(new Integer(state));
    if (ce != null) {
      return ce.stateName;
    } else {
      return "UNKNOWN";
    }
  }

  /**
   * Sends Cougaar event via EventService.
   */
  protected void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  /**
   * Returns true if specified agent/node is currently manager of robustness
   * community.
   * @param name  Agent/node name
   * @return      True if leader
   */
  protected boolean isLeader(String name) {
    return (model != null && model.isLeader(name));
  }

  /**
   * Returns true if specified agent is running on local node.
   * @param name  Agent name
   * @return      True if running on local node
   */
  protected boolean isLocal(String name) {
    return (model != null &&
            agentId.toString().equals(model.getLocation(name)));
  }

  /**
   * Change state for specified agent/node.
   * @param name  Agent/node name
   * @param state New state
   */
  protected void newState(String name, int state) {
    model.setCurrentState(name, state, NEVER);
  }

  /**
   * Get current state for specified agent/node.
   * @param name  Agent/node name
   * @return Current state
   */
  protected int getState(String name) {
    return model.getCurrentState(name);
  }

  /**
   * Set expiration for current state.
   * @param name  Agent/node name
   * @param expiration Expiration in milliseconds
   */
  protected void setExpiration(String name, int expiration) {
    model.setStateExpiration(name, expiration);
  }

  /**
   * Returns true if name is associated with an Agent.
   * @param name  Agent/node name
   * @return True if type is AGENT
   */
  protected boolean isAgent(String name) {
    return (model.getType(name) == model.AGENT);
  }

  /**
   * Returns true if name is associated with a Node.
   * @param name  Agent/node name
   * @return True if type is NODE
   */
  protected boolean isNode(String name) {
    return (model.getType(name) == model.NODE);
  }

  /**
   * Set agents location.
   * @param name  Agent name
   * @location    Node name
   */
  protected void setLocation(String name, String location) {
    model.setLocation(name, location);
  }

  /**
   * Get agents current location.
   * @param name  Name of agent
   * @return Node name or null if unknown
   */
  protected String getLocation(String name) {
    return model.getLocation(name);
  }

  /**
   * Get community attribute from model.
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected long getLongAttribute(String id, long defaultValue) {
    if (model.hasAttribute(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Get agent/node attribute from model.
   * @param name Agent/node name
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected long getLongAttribute(String name, String id, long defaultValue) {
    if (model.hasAttribute(name, id)) {
      return model.getLongAttribute(name, id);
    } else if (model.hasAttribute(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Initiate an agent restart.
   * @param name Agent to be restarted
   * @param dest Name of destination node
   */
  protected void restartAgent(String name, String dest) {
    String orig = model.getLocation(name);
    model.setLocation(name, "");
    restartHelper.restartAgent(name, orig, dest,
                               model.getCommunityName());
  }

  /**
   * Ping an agent and update current state based on result.
   * @param name            Agent to ping
   * @param stateOnSuccess  New state if ping succeeds
   * @param stateOnFail     New state if ping fails
   */
  protected void doPing(String name,
                        final int stateOnSuccess,
                        final int stateOnFail) {
    long pingTimeout = getLongAttribute(name, "PING_TIMEOUT", PING_TIMEOUT);
    pingHelper.ping(name, pingTimeout, new PingListener() {
      public void pingComplete(String name, int status) {
        logger.debug("Ping:" +
                     " agent=" + name +
                     " state=" + stateName(model.getCurrentState(name)) +
                     " result=" + (status == PingHelper.SUCCESS ? "SUCCESS" : "FAIL") +
                     (status == PingHelper.SUCCESS ? "" : " newState=" + stateName(stateOnFail)));
        if (status == PingHelper.SUCCESS) {
          newState(name, stateOnSuccess);
        } else {
          newState(name, stateOnFail);
        }
      }
    });
  }

  /**
   * Start heartbeats on specified agent.
   * @param name Agent name
   */
  protected void startHeartbeats(String name) {
    long hbReqTimeout = getLongAttribute(name, "HEARTBEAT_REQUEST_TIMEOUT", HEARTBEAT_REQUEST_TIMEOUT);
    long hbFreq = getLongAttribute(name, "HEARTBEAT_FREQUENCY", HEARTBEAT_FREQUENCY);
    long hbTimeout =getLongAttribute(name, "HEARTBEAT_TIMEOUT", HEARTBEAT_TIMEOUT);
    long hbPctOutofSpec = getLongAttribute(name, "HEARTBEAT_PCT_OUT_OF_SPEC", HEARTBEAT_PCT_OUT_OF_SPEC);
    getHeartbeatHelper().startHeartbeats(name,
                                         hbReqTimeout,
                                         hbFreq,
                                         hbTimeout,
                                         hbPctOutofSpec);
  }

  /**
   * Stop heartbeats on specified agent.
   * @param name Agent name
   */
  protected void stopHeartbeats(String name) {
    getHeartbeatHelper().stopHeartbeats(name);
  }

  /**
   * Add heartbeat listener to receive heartbeat events.
   * @param hbl Listener
   */
  protected void addHeartbeatListener(HeartbeatListener hbl) {
    getHeartbeatHelper().addListener(hbl);
  }

  /**
   * Add restart listener to receive restart events.
   * @param rl Listener
   */
  protected void addRestartListener(RestartListener rl) {
    getRestartHelper().addListener(rl);
  }

  /**
   * Add move listener to receive move events.
   * @param ml Listener
   */
  protected void addMoveListener(MoveListener ml) {
    getMoveHelper().addListener(ml);
  }

  /**
   * Add deconflict listener to receive deconflict events.
   * @param dl Listener
   */
  protected void addDeconflictListener(DeconflictListener dl) {
    if(getDeconflictHelper() == null) {
      this.dl = dl;
    }
    else
      getDeconflictHelper().addListener(dl);
  }

 /**
   * Returns a String containing top-level health status of monitored community.
   */
  public String statusSummary() {
    String activeNodes[] = model.listEntries(CommunityStatusModel.NODE, getNormalState());
    String agents[] = model.listEntries(CommunityStatusModel.AGENT);
    StringBuffer summary = new StringBuffer("community=" + model.getCommunityName());
    summary.append(" leader=" + model.getLeader());
    summary.append(" activeNodes=" + activeNodes.length + arrayToString(activeNodes));
    summary.append(" agents=" + agents.length);
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      int state = ( (ControllerEntry) it.next()).state;
      String agentsInState[] = model.listEntries(CommunityStatusModel.AGENT,
                                                 state);
      if (agentsInState.length > 0) {
        summary.append(" " + stateName(state) + "=" + agentsInState.length);
        if (!stateName(state).equals("ACTIVE") && agentsInState.length > 0)
          summary.append(arrayToString(agentsInState));
      }
    }
    return summary.toString();
  }


  /**
   * Creates an XML representation of an Attribute set.
   */
  protected String attrsToXML(Attributes attrs, String indent) {
    StringBuffer sb = new StringBuffer(indent + "<attributes>\n");
    if (attrs != null) {
      try {
        for (NamingEnumeration enum = attrs.getAll(); enum.hasMore(); ) {
          Attribute attr = (Attribute)enum.next();
          sb.append(indent + "  <attribute id=\"" + attr.getID() + "\" >\n");
          for (NamingEnumeration enum1 = attr.getAll(); enum1.hasMore(); ) {
            sb.append(indent + "    <value>" + enum1.next() + "</value>\n");
          }
          sb.append(indent + "  </attribute>\n");
        }
      } catch (NamingException ne) {}
    }
    sb.append(indent + "</attributes>\n");
    return sb.toString();
  }

  /**
   * Set state expiration using attribute information obtained from community
   * descriptor.
   * If the agent/node has the XXX_EXPIRATION attribute defined this value
   * is used for the expiration.  If the attribute is not defined for the
   * agent/node the community-level attribute is used if it exists.  If neither
   * the community or agent/node defines the attribute, the value defined by
   * DEFAULT_EXPIRATION is used.
   * @param name       Agent/node name
   * @param stateName  State name
   */
  protected void setExpiration(String name, String stateName) {
    String propertyName = stateName + "_EXPIRATION";
    long expiration = DEFAULT_EXPIRATION;
    if (model.hasAttribute(name, propertyName)) {
      expiration = model.getLongAttribute(name, propertyName);
    } else if (model.hasAttribute(propertyName)){
      expiration = model.getLongAttribute(propertyName);
    }
    model.setStateExpiration(name, expiration);
  }

  /**
   * Return current time in milliseconds.
   * @return
   */
  private long now() {
    return System.currentTimeMillis();
  }

  /**
   * Returns XML formatted dump of all community status information.
   * @return XML formatted status
   */
  public String getCompleteStatus() {
    StringBuffer sb = new StringBuffer("<community" +
                                      " name=\"" + model.getCommunityName() + "\"" +
                                      " leader=\"" + model.getLeader() + "\"" +
                                      " >\n");
    String nodeNames[] = model.listEntries(CommunityStatusModel.NODE);
    String leader = model.getLeader();
    if (leader != null && isAgent(leader)) {
      String nodesPlusLeader[] = new String[nodeNames.length + 1];
      System.arraycopy(nodeNames, 0, nodesPlusLeader, 0, nodeNames.length);
      nodesPlusLeader[nodesPlusLeader.length-1] = model.getLeader();
      nodeNames = nodesPlusLeader;
    }
    sb.append("  <healthMonitors count=\"" + nodeNames.length + "\" >\n");
    for (int i = 0; i < nodeNames.length; i++) {
      sb.append(healthMonitorStatusToXML("    ", nodeNames[i]) + "\n");
    }
    sb.append("  </healthMonitors>\n");
    String agentNames[] = model.listEntries(CommunityStatusModel.AGENT);
    sb.append("  <agents count=\"" + agentNames.length + "\" >\n");
    for (int i = 0; i < agentNames.length; i++) {
      sb.append(agentStatusToXML("    ", agentNames[i]) + "\n");
    }
    sb.append("  </agents>\n");
    sb.append("</community>\n");
    return sb.toString();
  }


  public String agentStatusToXML(String indent, String agentName) {
    long now = now();
    long expiresAt = NEVER;
    if (model.getStateExpiration(agentName) != NEVER) {
      expiresAt = ((model.getTimestamp(agentName) + model.getStateExpiration(agentName)) - now);
    }
    StringBuffer sb = new StringBuffer();
    sb.append(indent + "<agent name=\"" + agentName + "\" >\n");
    sb.append(indent + "  <status " +
        " state=\"" + stateName(model.getCurrentState(agentName)) + "\"" +
        " last=\"" + (now - model.getTimestamp(agentName)) + "\"" +
        " expires=\"" + (expiresAt == NEVER ? "NEVER" : Long.toString(expiresAt)) + "\" />\n");
    String priorLoc = model.getPriorLocation(agentName);
    sb.append(indent + "  <location " +
        " current=\"" + model.getLocation(agentName) + "\"" +
        " prior=\"" + (priorLoc == null ? "" : priorLoc) + "\" />\n");
    sb.append(attrsToXML(model.getAttributes(agentName), indent + "  "));
    sb.append(indent + "</agent>\n");
    return sb.toString();
  }

  public String healthMonitorStatusToXML(String indent, String name) {
    String type = model.getType(name) == model.AGENT ? "agent" : "node";
    long now = now();
    long expiresAt = NEVER;
    if (model.getStateExpiration(name) != NEVER) {
      expiresAt = ((model.getTimestamp(name) + model.getStateExpiration(name)) - now);
    }
    StringBuffer sb = new StringBuffer();
    sb.append(indent + "<healthMonitor name=\"" + name + "\" type=\"" + type + "\" >\n");
    sb.append(indent + "<vote>" + model.getLeaderVote(name) + "</vote>\n");
    sb.append(indent + "  <status " +
        " state=\"" + stateName(model.getCurrentState(name)) + "\"" +
        " last=\"" + (now - model.getTimestamp(name)) + "\"" +
        " expires=\"" + (expiresAt == NEVER ? "NEVER" : Long.toString(expiresAt)) + "\" />\n");
    sb.append(attrsToXML(model.getAttributes(name), indent + "  "));
    sb.append(indent + "</healthMonitor>\n");
    return sb.toString();
  }

  public static String arrayToString(String[] strArray) {
    StringBuffer sb = new StringBuffer("[");
    for (int i = 0; i < strArray.length; i++) {
      sb.append(strArray[i]);
      if (i < strArray.length - 1) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

}
