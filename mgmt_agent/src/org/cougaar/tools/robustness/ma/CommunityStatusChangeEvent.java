package org.cougaar.tools.robustness.ma;

/**
 * Event that is generated when changes occur in CommunityStatusModel.
 */
public class CommunityStatusChangeEvent {

  // Change event types
  public static final int MEMBERS_ADDED      =  1;
  public static final int MEMBERS_REMOVED    =  2;
  public static final int LEADER_CHANGE      =  4;
  public static final int STATE_CHANGE       =  8;
  public static final int LOCATION_CHANGE    = 16;
  public static final int STATE_EXPIRATION   = 32;

  private int changeFlags;
  private String name;
  private int type;
  private int currentState;
  private int priorState;
  private String currentLocation;
  private String priorLocation;
  private String currentLeader;
  private String priorLeader;

  public CommunityStatusChangeEvent(int    changeFlags,
                                    String name,
                                    int    type,
                                    int    currentState,
                                    int    priorState,
                                    String currentLocation,
                                    String priorLocation,
                                    String currentLeader,
                                    String priorLeader ) {
    this.changeFlags = changeFlags;
    this.name = name;
    this.type = type;
    this.currentState = currentState;
    this.priorState = priorState;
    this.currentLocation = currentLocation;
    this.priorLocation = priorLocation;
    this.currentLeader = currentLeader;
    this.priorLeader = priorLeader;
  }

  public CommunityStatusChangeEvent(int changeFlags,
                                    CommunityStatusModel.StatusEntry se,
                                    String currentLeader,
                                    String priorLeader) {
    this(changeFlags,
         se.name,
         se.type,
         se.currentState,
         se.priorState,
         se.currentLocation,
         se.priorLocation,
         currentLeader,
         priorLeader);
  }

  public CommunityStatusChangeEvent(int changeFlags,
                                    CommunityStatusModel.StatusEntry se) {
    this(changeFlags, se, null, null);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("CommunityStatusChangeEvent:");
    sb.append(" changeFlags=" + changeFlagsToString());
    sb.append(" name=" + name);
    sb.append(" type=" + type);
    sb.append(" currentState=" + currentState);
    if (priorState >= 0) sb.append(" priorState=" + priorState);
    if (currentLocation != null) sb.append(" currentLocation=" + currentLocation);
    if (priorLocation != null) sb.append(" priorLocation=" + priorLocation);
    if (currentLeader != null) sb.append(" currentLeader=" + currentLeader);
    if (priorLeader != null) sb.append(" priorLeader=" + priorLeader);
    return sb.toString();
  }

  public String changeFlagsToString() {
    StringBuffer sb = new StringBuffer();
    if ((changeFlags & MEMBERS_ADDED) != 0) {
      sb.append(sb.length() > 0 ? "|MEMBERS_ADDED" : "MEMBERS_ADDED");
    }
    if ((changeFlags & MEMBERS_REMOVED) != 0) {
      sb.append(sb.length() > 0 ? "|MEMBERS_REMOVED" : "MEMBERS_REMOVED");
    }
    if ((changeFlags & LEADER_CHANGE) != 0) {
      sb.append(sb.length() > 0 ? "|LEADER_CHANGE" : "LEADER_CHANGE");
    }
    if ((changeFlags & STATE_CHANGE) != 0) {
      sb.append(sb.length() > 0 ? "|STATE_CHANGE" : "STATE_CHANGE");
    }
    if ((changeFlags & LOCATION_CHANGE) != 0) {
      sb.append(sb.length() > 0 ? "|LOCATION_CHANGE" : "LOCATION_CHANGE");
    }
    if ((changeFlags & STATE_EXPIRATION) != 0) {
      sb.append(sb.length() > 0 ? "|STATE_EXPIRATION" : "STATE_EXPIRATION");
    }
    return sb.toString();
  }

  /**
   * Get change flags depicting change type(s) asociated with this event.
   * @return
   */
  public int getChangeFlags() { return changeFlags; }

  /**
   * Get name of changed entity.  Can be an agent or a node.
   * @return  Name of changed entity
   */
  public String getName() { return name; }

  /**
   * Get entity type.
   * @return  Entity type (AGENT or NODE)
   */
  public int getType() { return type; }

  /**
   * Get entities current state.
   * @return Current state
   */
  public int getCurrentState() { return currentState; }

  /**
   * Get entities prior state.
   * @return Prior state
   */
  public int getPriorState() { return priorState; }

  /**
   * Get entities current location.  For an agent this will be a node name
   * and for a node this will be the host name.
   * @return Current location
   */
  public String getCurrentLocation() { return currentLocation; }

  /**
   * Get entities prior location.  For an agent this will be a node name
   * and for a node this will be the host name.  The returned value will be
   * null if this event is associated with the initial discovery of the entity.
   * @return Prior location
   */
  public String getPriorLocation() { return priorLocation; }

  /**
   * Returns name of current robustness community leader.
   * @return  Leader name
   */
  public String getCurrentLeader() { return currentLeader; }

  /**
   * Returns name of prior robustness community leader.  This could be
   * null if this event is associated with the selection of initial leader.
   * @return  Leader name
   */
  public String getPriorLeader() { return priorLeader; }

  /**
   * Helper method for determining if event reflects the addition of
   * a new robustness community member.
   * @return True if a member was added to community
   */
  public boolean membersAdded() {
    return (changeFlags & MEMBERS_ADDED) != 0;
  }

  /**
   * Helper method for determining if event reflects the removal of
   * a robustness community member.
   * @return True if a member was removed from community
   */
  public boolean membersRemoved() {
    return (changeFlags & MEMBERS_REMOVED) != 0;
  }

  /**
   * Helper method for determining if event reflects the state change of
   * a robustness community member.
   * @return True if a member state has changed
   */
  public boolean stateChanged() {
    return (changeFlags & STATE_CHANGE) != 0;
  }

  /**
   * Helper method for determining if event reflects a change in robustness
   * community leader.
   * @return True if leader changed
   */
  public boolean leaderChanged() {
    return (changeFlags & LEADER_CHANGE) != 0;
  }

  /**
   * Helper method for determining if event reflects a change in an agents
   * location.
   * @return True if a agent location has changed
   */
  public boolean locationChanged() {
    return (changeFlags & LOCATION_CHANGE) != 0;
  }

  /**
   * Helper method for determining if event reflects a state expiration.
   * @return True if a agents state has not been refreshed within expiration
   * period
   */
  public boolean stateExpired() {
    return (changeFlags & STATE_EXPIRATION) != 0;
  }
}