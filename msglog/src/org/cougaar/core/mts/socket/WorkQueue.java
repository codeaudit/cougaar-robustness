/*
 * <copyright>
 *  Copyright 2002-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 16 Apr 2003: Handle "Array index out of range: 0" in run method. (tagged 102B)
 * 02 Oct 2002: Created. (OBJS)
 */

package org.cougaar.core.mts.socket;

import java.util.*;

import org.cougaar.core.service.LoggingService;


public class WorkQueue
{
  private final int minThreads, maxThreads, idleTimeout;
  private final LoggingService log;
  private final Vector queue, threads;

  public WorkQueue (int minThreads, int maxThreads, int idleTimeout, LoggingService log)
  {
    this.minThreads = minThreads;
    this.maxThreads = maxThreads;
    this.idleTimeout = idleTimeout;
    this.log = log;

    queue = new Vector (maxThreads*4);
    threads = new Vector (maxThreads);
    for (int i=0; i<minThreads; i++) possiblyAddThread();
  }

  public void add (Runnable r) 
  {
    synchronized (queue) 
    {
      queue.add (r);
    }

    possiblyWakeOrAddThread();
  }

  public void flush ()
  {
    //  Remove all queued work and if possible call their quit methods

    synchronized (queue) 
    {
      for (Enumeration q=queue.elements(); q.hasMoreElements(); )
      {
        try
        {
          Runnable r = (Runnable) q.nextElement();
          if (r instanceof QuitableRunnable) ((QuitableRunnable)r).quit();
        }
        catch (Exception e) {}
      }
      
      queue.clear();
    }
  }

  public void shutdown ()
  {
    //  Shut down everything

    quitAllThreads();
    flush();
  }

  private void possiblyWakeOrAddThread ()
  {
    synchronized (threads)
    {
      for (Enumeration t=threads.elements(); t.hasMoreElements(); )
      {
        WorkThread wt = (WorkThread) t.nextElement();
        if (wt.isAvailable()) { wt.wakeup(); return; } 
      }

      possiblyAddThread();
    }
  }

  private boolean possiblyAddThread ()
  {
    synchronized (threads)
    {
      int n = threads.size();

      if (n < maxThreads)
      {
        WorkThread wt = new WorkThread (n < minThreads ? 0 : idleTimeout);
        wt.start();
        threads.add (wt);
        if (doDebug()) log.debug ("WorkQueue: Now " +threads.size()+ " work threads");
        return true;
      }
      else return false;
    }
  }

  private void removeThread (WorkThread wt)
  {
    synchronized (threads)
    {
      threads.remove (wt);
      if (doDebug()) log.debug ("WorkQueue: Now " +threads.size()+ " work threads");
    }
  }

  private void quitAllThreads ()
  {
    synchronized (threads)
    {
      for (Enumeration t=threads.elements(); t.hasMoreElements(); )
      {
        WorkThread wt = (WorkThread) t.nextElement();
        wt.quit();
      }

      threads.clear();
    }
  }

  private class WorkThread extends Thread 
  {
    int idleTimeout;
    boolean available;
    boolean quitNow;

    public WorkThread (int idleTimeout)
    {
      this.idleTimeout = idleTimeout;
      available = true;
      quitNow = false;
    }

    public void setAvailable (boolean b)
    {
      available = b;
    }

    public boolean isAvailable ()
    {
      return available;
    }

    public void quit ()
    {
      quitNow = true;
      wakeup();
    }

    public void wakeup ()
    {
      synchronized (this)
      {
        notify();
      }
    }

    public void run() 
    {
      Runnable r;

      while (!quitNow) 
      {
        synchronized (this) 
        {
          while (!quitNow && queue.isEmpty()) 
          {
            //  Set available for work

            if (doDebug()) log.debug ("WorkQueue: Waiting for work: " +this);
            setAvailable (true);

            //  Wait for new work or idle timeout 

            long startWait = now();
            try { wait (idleTimeout); } catch (Exception e) {}
            int waitTime = (int) (now() - startWait);

            //  Check if time to prune this thread

            if (idleTimeout > 0 && waitTime >= idleTimeout && queue.isEmpty())
            {
              if (doDebug()) log.debug ("WorkQueue: Idle timeout - exiting: " +this);
              removeThread (this);
              return;
            }
            else if (quitNow) return;
          }

          //102B r = (Runnable) queue.remove (0);
          r = (queue.isEmpty() ? null : (Runnable)queue.remove(0));
          
          if (r != null)
          {
            if (doDebug()) log.debug ("WorkQueue: Got work: " +this);
            setAvailable (false);
          }
          else continue;
        }

        try 
        {
          r.run();
        }
        catch (Exception e) 
        {
          if (log.isWarnEnabled()) log.warn ("WorkQueue: Runnable "+r+" got exception: "+e);
        }

        r = null;  // allow gc
      }
    }

    public String toString ()
    {
      return "WorkThread#" + this.hashCode();
    }
  }

  private boolean doDebug ()
  {
    return (log != null && log.isDebugEnabled());
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
