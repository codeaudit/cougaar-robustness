/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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
 * 15 Aug 2002: Add setAverage() and isStartDelaySatisfied(). (OBJS)
 * 25 Jul 2002: Add startDelay and getLastSampleTimestamp(). (OBJS)
 * 23 Apr 2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;


public class RunningAverage
{
  private double lastSample;
  private long lastSampleTimestamp;
  private double data[];
  private int n, point;
  private int startDelay, fillCount, changeLimitDelay, delayCount;
  private boolean hasChangeLimit;
  private double changeLimit;

  public RunningAverage (int poolsize)
  {
    this (poolsize, 0, 0.0, 0);
  }

  public RunningAverage (int poolsize, double percentChangeLimit)
  {
    this (poolsize, 0, percentChangeLimit, poolsize);
  }

  public RunningAverage (int poolsize, int startDelay, double percentChangeLimit)
  {
    this (poolsize, startDelay, percentChangeLimit, poolsize);
  }

  public RunningAverage (int poolsize, int startDelay, double percentChangeLimit, int changeLimitDelay)
  {
    if (poolsize < 1 || startDelay < 0 || percentChangeLimit < 0.0 || changeLimitDelay < 0)
    {
      throw new IllegalArgumentException ("RunningAverage: Bad arg(s)!");
    }

    data = new double[poolsize];
    n = 0;
    point = 0;

    this.startDelay = startDelay;
    fillCount = 0;

    hasChangeLimit = (percentChangeLimit > 0.001);  // avoid zero comparison
    if (hasChangeLimit) changeLimit = percentChangeLimit;

    this.changeLimitDelay = (hasChangeLimit ? changeLimitDelay : 0);
    delayCount = 0;
  }

  public synchronized void add (int entry)
  {
    add ((double) entry);
  }

  public synchronized void add (double entry)
  {
    //  Save and timestamp last sample

    lastSample = entry;
    lastSampleTimestamp = now();

    //  Throw away data until startDelay is satisfied

    if (startDelay > 0)
    {
      startDelay--;
      return;
    }
    else if (fillCount < data.length) fillCount++;

    //  Special case - first entry

    if (n == 0)
    {
      data[point++] = entry;
      if (point == data.length) point = 0;
      n = 1;
      delayCount = 1;
      return;
    }

    //  Non or delay change-limited case

    if (!hasChangeLimit || delayCount < changeLimitDelay)
    {
      data[point++] = entry;
      if (point == data.length) point = 0;
      if (n < data.length) n++;
      if (hasChangeLimit) delayCount++;
      return;
    }

    //  Change-limited case

    double avg = getAverage();
    double sum = avg*n - data[point]; // close enough
    double n2 = (double) (n == data.length ? n : n+1);
  
    if (entry > avg)
    {
      double avgUpperBound = avg + avg*changeLimit;
      double entryUpperBound = avgUpperBound*n2 - sum;
      if (entry > entryUpperBound) entry = entryUpperBound;
    }
    else
    {
      double avgLowerBound = avg - avg*changeLimit;
      double entryLowerBound = avgLowerBound*n2 - sum;
      if (entry < entryLowerBound) entry = entryLowerBound;
    }

    data[point++] = entry;
    if (point == data.length) point = 0;
    if (n < data.length) n++;
    return;
  }
  
  public synchronized double boundEntry (int entry, double percentWithinAverage)
  {
    return boundEntry ((double)entry, percentWithinAverage);
  }

  public synchronized double boundEntry (double entry, double percentWithinAverage)
  {
    double avg = getAverage();

    if (entry > avg)
    {
      double upperBound = avg + avg*percentWithinAverage;
      return (entry > upperBound ? upperBound : entry);
    }
    else
    {
      double lowerBound = avg - avg*percentWithinAverage;
      return (entry < lowerBound ? lowerBound : entry);
    } 
  }

  public synchronized void setAverage (int value)
  {
    setAverage ((double) value);
  }

  public synchronized void setAverage (double value)
  {
    //  Preserve startDelay, and bypass any changeLimit, zero fillCount

    int startDelay_save = startDelay;
    boolean hasChangeLimit_save = hasChangeLimit;
    
    startDelay = 0;
    hasChangeLimit = false;
    n = 0;
    point = 0;
    for (int i=0; i<data.length; i++) add (value);
    
    startDelay = startDelay_save;
    hasChangeLimit = hasChangeLimit_save;
    fillCount = 0;
  }

  public synchronized double getAverage ()
  {
    if (n == 0) return 0.0;

    double sum = 0.0;
    for (int i=0; i<n; i++) sum += data[i];
    return sum/((double)n);
  }

  public boolean isStartDelaySatisfied ()
  {
    return (startDelay == 0);
  }

  public float percentFilled ()
  {
    return (float)fillCount/(float)data.length;  // 0 to 100
  }

  public synchronized double getLastSample ()  // sync because of double
  {
    return lastSample;
  }

  public synchronized long getLastSampleTimestamp ()  // sync because of long
  {
    return lastSampleTimestamp;
  }

  public synchronized String toString ()
  {
    return 
    (
      "size=" +data.length+ " " +
      "changeLimit=" +changeLimit+ " " +
      "avg=" +getAverage()+ " " +
      "n=" +n+ " " +
      "point=" + point
    );
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  public synchronized void debugAdd (double entry)
  {
    System.out.println ("\nBfore add: avg=" +getAverage()+ " n=" +n+ " point=" +point);
    System.out.println ("Adding: " +entry);
    add (entry);
    System.out.println ("After add: avg=" +getAverage()+ " n=" +n+ " point=" +point);
  }

  public static void main (String args[])
  {
    RunningAverage ra = new RunningAverage (4);
    System.out.println ("\nsetup = " +ra);
    ra.debugAdd (2);
    ra.debugAdd (4);
    ra.debugAdd (6);
    ra.debugAdd (8);
    ra.debugAdd (6);
    ra.debugAdd (6);
    ra.debugAdd (ra.boundEntry (10, 0.5));

    ra = new RunningAverage (4, 0.1);
    System.out.println ("\nsetup = " +ra);
    ra.debugAdd (2);
    ra.debugAdd (4);
    ra.debugAdd (6);
    ra.debugAdd (8);
    ra.debugAdd (6);
    ra.debugAdd (6);
    ra.debugAdd (ra.boundEntry (10, 0.5));

    ra = new RunningAverage (4);
    System.out.println ("\nsetup = " +ra);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (0);

    ra = new RunningAverage (4, 0.1);
    System.out.println ("\nsetup = " +ra);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (10);
    ra.debugAdd (0);
  }
}
