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
 * 19 Sep 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;
import java.net.Socket;
import java.net.DatagramSocket;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;


/**
 *  SocketClosingService implementation and service provider
 */

public class SocketClosingServiceImpl implements SocketClosingService, ServiceProvider
{
  private static SocketCloser socketCloser = null;
  private LoggingService log;

  public SocketClosingServiceImpl (ServiceBroker sb) 
  {
    log = (LoggingService) sb.getService (this, LoggingService.class, null);
    if (log.isInfoEnabled()) log.info ("Creating " +this);

    synchronized (this)
    {
      if (socketCloser == null)
      {
        socketCloser = new SocketCloser();
        Thread thread = new Thread (socketCloser, "SocketCloser");
        thread.start();
      }
    }

    sb.addService (SocketClosingService.class, this);
  }

  public Object getService (ServiceBroker sb, Object requestor, Class serviceClass) 
  {
    if (serviceClass == SocketClosingService.class) return this;
    return null;
  }

  public void releaseService (ServiceBroker sb, Object requestor, Class serviceClass, Object service)
  {}

  public String toString ()
  {
    return getClass().getName();
  }


  //  SocketClosingService interface

  public void scheduleClose (Socket socket, int timeout)
  {
    socketCloser.add (new SocketRecord (socket, timeout, now()+timeout));
  }

  public void scheduleClose (DatagramSocket socket, int timeout)
  {
    socketCloser.add (new SocketRecord (socket, timeout, now()+timeout));
  }

  public void scheduleClose (Socket socket, long deadline)
  {
    socketCloser.add (new SocketRecord (socket, (int)(deadline-now()), deadline));
  }

  public void scheduleClose (DatagramSocket socket, long deadline)
  {
    socketCloser.add (new SocketRecord (socket, (int)(deadline-now()), deadline));
  }

  public void unscheduleClose (Socket socket)
  {
    socketCloser.remove (new SocketRecord (socket));
  }

  public void unscheduleClose (DatagramSocket socket)
  {
    socketCloser.remove (new SocketRecord (socket));
  }


  //  Implementation

  private static class GenericSocket
  {
    private Socket socket;
    private DatagramSocket dsocket;

    public GenericSocket (Socket socket)
    {
      this.socket = socket;
    }

    public GenericSocket (DatagramSocket socket)
    {
      this.dsocket = socket;
    }

    public boolean equals (Object obj)
    {
           if (socket  != null) return ( socket == ((GenericSocket)obj).socket);
      else if (dsocket != null) return (dsocket == ((GenericSocket)obj).dsocket);
      return false;      
    }

    public void close () throws java.io.IOException
    {
      if (socket != null) socket.close();
      else if (dsocket != null) dsocket.close();
    }

    public boolean isNull ()
    {
      return (socket == null && dsocket == null);
    }

    public boolean isClosed ()
    {
      if (socket  != null) return  socket.isClosed();
      if (dsocket != null) return dsocket.isClosed();
      return true;
    }

    public String toString ()
    {
      if (socket  != null) return socket.toString();
      if (dsocket != null) return datagramSocketToString (dsocket);
      return "null";
    }
  }

  private static class SocketRecord
  {
    public final GenericSocket socket;
    public final int timeout;
    public final long deadline;

    public SocketRecord (Socket socket)
    {
      this (socket, 0, 0);
    }

    public SocketRecord (DatagramSocket socket)
    {
      this (socket, 0, 0);
    }

    public SocketRecord (Socket socket, int timeout, long deadline)
    {
      this.socket = new GenericSocket (socket);
      this.timeout = timeout;
      this.deadline = deadline;
    }

    public SocketRecord (DatagramSocket socket, int timeout, long deadline)
    {
      this.socket = new GenericSocket (socket);
      this.timeout = timeout;
      this.deadline = deadline;
    }

    public boolean equals (Object obj)
    {
      return socket.equals (((SocketRecord)obj).socket);
    }

    public String toString ()
    {
      return "timeout=" +timeout+ " socket=" +socket; 
    }
  }

  private class SocketCloser implements Runnable
  {
    private Vector queue;
    private SocketRecord sockets[];
    private Comparator deadlineSort;
    private long minCloseDeadline;
    
    public SocketCloser ()
    {
      queue = new Vector();
      sockets = new SocketRecord[32];
      deadlineSort = new DeadlineSort();
      minCloseDeadline = 0;
    }

    private boolean debug ()
    {
      return log.isDebugEnabled();
    }

    public void add (SocketRecord sr) 
    {
      if (debug()) log.debug ("SocketCloser: adding " +sr);

      synchronized (queue) 
      {
        remove (sr);  // any existing close is now superceded
        queue.add (sr);
        offerNewCloseDeadline (sr.deadline);

        if (queue.size() > 30)  // control buildup
        {
          if (minCloseDeadline-now() > 5000) offerNewCloseDeadline (0);
        }
      }
    }

    private void remove (SocketRecord sr) 
    {
      SocketRecord oldsr = null;

      synchronized (queue) 
      {
        int i = queue.indexOf (sr);
        if (i >= 0) oldsr = (SocketRecord) queue.remove (i);
        if (queue.size() == 0) offerNewCloseDeadline (0);
      }

      if (debug() && oldsr != null) log.debug ("SocketCloser: removed " +oldsr);
    }
    
    private void offerNewCloseDeadline (long deadline)
    {
      if (deadline < 0) return;

      synchronized (queue) 
      {
        if (deadline < minCloseDeadline || (minCloseDeadline == 0 && deadline > 0))
        {
          minCloseDeadline = deadline;
          queue.notify();
        }
      }
    }

    public void run () 
    {
      while (true)
      {
        String s = "Unexpected exception, restarting thread";

        try
        { 
          try 
          { 
            doRun(); 
          } 
          catch (Exception e) 
          {
            s += ": " + stackTraceToString (e);
            log.error (s);
          }
        }
        catch (Exception e)
        {
          try { e.printStackTrace(); } catch (Exception ex) { /* !! */ }
        }
      }
    }

    private void doRun () 
    {
      int len;

      while (true) 
      {
        //  Wait until we have some new sockets or we have timed out to 
        //  re-examine old sockets.

        synchronized (queue) 
        {
          while (true)
          {
            //  Check how long to wait before we need to satisfy a close deadline

            long waitTime = 0;  // 0 = wait till notify (or interrupt)

            if (queue.size() > 0)
            {
              waitTime = minCloseDeadline - now();
              if (waitTime <= 0) { minCloseDeadline = 0;  break; }
            }

            //  Wait until timeout, notify, or interrupt

            try { queue.wait (waitTime); } catch (Exception e) {}
          }

          sockets = (SocketRecord[]) queue.toArray (sockets);  // try array reuse
          len = queue.size();
          if (len > 1) Arrays.sort (sockets, 0, len, deadlineSort);
        }

        //  Check if it is time to close any sockets

        if (len > 0 && debug()) 
        {
          log.debug ("SocketCloser: reviewing queue (" +len+ " socket" +(len==1? ")" : "s)"));
        }

        synchronized (queue)
        {
          //  Prune already closed sockets

          for (int i=0; i<len; i++)
          {
            if (sockets[i].socket.isNull() || sockets[i].socket.isClosed()) 
            {
              remove (sockets[i]);
              sockets[i] = null;
            }
          } 
        }

        for (int i=0; i<len; i++) if (sockets[i] != null)
        {
          SocketRecord sr = sockets[i];
          long closeDeadline = sr.deadline;
          long timeLeft = closeDeadline - now();

          if (debug()) log.debug ("SocketCloser: timeLeft=" +timeLeft+ "  " +sr);

          if (timeLeft <= 0)
          {
            //  Time to close the socket

            if (!sr.socket.isClosed())
            {
              if (debug()) log.debug ("SocketCloser: Closing " +sr);
              try { sr.socket.close(); } catch (Exception e) {}
            }

            remove (sr);
          }
          else 
          {
            //  Since the deadlines are time-ordered no other closings will (thread
            //  willing) occur in this go round, so we can quit if we want to.

            offerNewCloseDeadline (closeDeadline);
            if (!debug()) break;  // quit if not showing queue review
          }
        }

        Arrays.fill (sockets, null);  // release references
      }
    }

    private class DeadlineSort implements Comparator
    {
      public int compare (Object sr1, Object sr2)
      {
        if (sr1 == null)  // drive nulls to bottom (top is index 0)
        {
          if (sr2 == null) return 0;
          else return 1;
        }
        else if (sr2 == null) return -1;

        //  Sort on close deadline (sooner deadlines come first)

        long d1 = ((SocketRecord)sr1).deadline;
        long d2 = ((SocketRecord)sr2).deadline;

        if (d1 == d2) return 0;
        return (d1 > d2 ? 1 : -1);
      }

      public boolean equals (Object obj)
      {
        return (this == obj);
      }
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String datagramSocketToString (DatagramSocket ds)
  {
    if (ds == null) return "null";
    boolean conn = ds.isConnected();
    String data = conn ? ds.getInetAddress()+":"+ds.getPort() : "unconnected";
    return "DatagramSocket[" +data+ "]";
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
