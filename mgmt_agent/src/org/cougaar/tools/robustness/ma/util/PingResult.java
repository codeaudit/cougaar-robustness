package org.cougaar.tools.robustness.ma.util;

/**
 * Ping results.
 */
public class PingResult {

  public static final int UNDEFINED = -1;
  public static final int SUCCESS = 0;
  public static final int FAIL = 1;

  private String name = null;
  private int status = UNDEFINED;
  private long roundTripTime = -1;

  /**
   * Ping results.
   * @param name  Name of agent/node pinged.
   * @param status Ping status
   * @param rtt    Ping round trip time.
   */
  public PingResult(String name, int status, long rtt) {
    this.name = name;
    this.status = status;
    this.roundTripTime = rtt;
  }

  public String getName() { return this.name; }
  public int getStatus() { return this.status; }
  public long getRoundTripTime() { return this.roundTripTime; }

  public String toString() {
    return "agent=" + name + " status=" + statusAsString(status);
  }

  public static String statusAsString(int statusCode) {
    switch (statusCode) {
      case SUCCESS: return "SUCCESS";
      case FAIL: return "FAIL";
      case UNDEFINED: return "UNDEFINED";
      default: return "INVALID_VALUE";
    }
  }
}