package org.cougaar.tools.robustness.ma.controllers;

/**
 * Interface for a single state controller.
 */
public interface StateController {

  /**
   * Method that is called when a monitored agent/node enters this state.
   * @param name  Agent/node name.
   */
  public void enter(String name);

  /**
   * Method that is called when a monitored agent/node exists this state.
   * @param name  Agent/node name.
   */
  public void exit(String name);

  /**
   * Method that is when updated status is not received for a monitored
   * agent/node within the state expiration period.
   * @param name  Agent/node name.
   */
  public void expired(String name);

}