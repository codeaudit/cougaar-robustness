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
 * 06 Oct 2002: Created. (OBJS)
 */

package org.cougaar.mts.std.socket;


public class EventWindow
{
  private int nEvents;
  private int timeWindow;
  private long[] events;
  private long latestTime;
  private boolean triggered;
  private int next;

  public EventWindow (int nEvents, int timeWindow)
  {
    this.nEvents = nEvents;
    this.timeWindow = timeWindow;
    events = new long[nEvents];
    next = -1;
  }

  public boolean hasTriggered ()
  {
    return triggered;
  }

  public synchronized void reset ()
  {
    triggered = false;
    latestTime = 0;
    for (int i=0; i<events.length; i++) events[i] = 0;
  }

  public synchronized boolean addEvent ()
  {
    return addEvent (now());
  }

  public synchronized boolean addEvent (long time)
  {
    if (triggered) return false;  // event not added

    if (time > latestTime) latestTime = time;
    events[(++next < events.length ? next : (next = 0))] = time;

    int count = 0;
    long windowEdge = latestTime - timeWindow;
    for (int i=0; i<events.length; i++) if (events[i] >= windowEdge) count++;
    if (count == events.length) triggered = true;

    return true;  // event added
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
