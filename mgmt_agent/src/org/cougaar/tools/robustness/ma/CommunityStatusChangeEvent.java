package org.cougaar.tools.robustness.ma;

/**
 */

public class CommunityStatusChangeEvent {

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
    sb.append(" changeFlags=" + changeFlags);
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

  public int getChangeFlags() { return changeFlags; }
  public String getName() { return name; }
  public int getType() { return type; }
  public int getCurrentState() { return currentState; }
  public int getPriorState() { return priorState; }
  public String getCurrentLocation() { return currentLocation; }
  public String getPriorLocation() { return priorLocation; }
  public String getCurrentLeader() { return currentLeader; }
  public String getPriorLeader() { return priorLeader; }

  public boolean membersAdded() {
    return (changeFlags & MEMBERS_ADDED) != 0;
  }

  public boolean membersRemoved() {
    return (changeFlags & MEMBERS_REMOVED) != 0;
  }

  public boolean stateChanged() {
    return (changeFlags & STATE_CHANGE) != 0;
  }

  public boolean leaderChanged() {
    return (changeFlags & LEADER_CHANGE) != 0;
  }

  public boolean locationChanged() {
    return (changeFlags & LOCATION_CHANGE) != 0;
  }

  public boolean stateExpired() {
    return (changeFlags & STATE_EXPIRATION) != 0;
  }
}