package org.cougaar.tools.robustness.ma.controllers;

/**
 */

public interface StateController {

  //public void stateTransition(int priorState, int newState, String name);

  public void enter(String name);

  public void exit(String name);

  public void expired(String name);

}