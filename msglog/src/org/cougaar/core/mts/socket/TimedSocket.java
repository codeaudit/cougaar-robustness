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
 * 10 Jun 2002: Revamped thread use, allow use of new Cougaar threads. (OBJS)
 * 11 Dec 2001: Revamped thread use, made reusable threads the default. (OBJS)
 * 27 Oct 2001: Added code to ensure socket close in case of timeout. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.socket;

import java.io.*;
import java.net.Socket;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.CougaarThread;


/**
 *  Utility class to create client socket connections under a time limit
 *  to avoid potentially hanging on a bad or slow connection.
**/ 

public class TimedSocket
{
  public static Socket getSocket (String host, int port, int timeout)
    throws InterruptedIOException, Exception
  {
    return getSocket (host, port, timeout, null);
  }

  public static Socket getSocket (String host, int port, int timeout, MyThreadService threadSvc)
    throws InterruptedIOException, Exception
  {
    //  Get the socket in another thread so we can timeout on it here

    SocketConnector socketConn = new SocketConnector (host, port);
    String name = "SocketConn_" + host + ":" + port;

    if (threadSvc != null)
    {
      //  Use Cougaar threads

      Schedulable thread = threadSvc.getThread (socketConn, name);
      thread.start();
    }
    else
    {
      //  Use Java threads

      Thread thread = new Thread (socketConn, name);
      thread.start();
    }

    //  Wait till timeout for the socket

    final int POLL_TIME = 100;
    int sleepTime = 0;

    while (true)
    {
      if (socketConn.isConnected()) return socketConn.getSocket();

      if (socketConn.isError()) throw (socketConn.getException());

      try 
      { 
        if (threadSvc != null) CougaarThread.sleep (POLL_TIME); 
        else Thread.sleep (POLL_TIME); 
      } 
      catch (Exception e) {}

      sleepTime += POLL_TIME;

      if (sleepTime > timeout)
      {
        socketConn.ensureClose();
        String s = "TimedSocket: Timeout trying to connect in " +timeout+ " msecs";
        throw new InterruptedIOException (s);
      }
    }
  }

  private static class SocketConnector implements Runnable
  {
    private String host;
    private int port;
    private boolean doCloseSock;
    private Socket sock;
    private Exception exception;

    public SocketConnector (String host, int port)
    {
      this.host = host;
      this.port = port;

      doCloseSock = false;
    }

    public void run ()
    {
      Socket s = null;

      try
      {
        s = new Socket (host, port);
      }
      catch (Exception e)
      {
        exception = e;
      }

      synchronized (this)
      {
        if (doCloseSock)
        {
          if (s != null) 
          {
            try { s.close(); } catch (Exception e) {}
            s = null;
          }
        }
        else sock = s;
      }
    }

    public void ensureClose ()
    {
      synchronized (this)
      {
        if (sock != null)
        {
          try { sock.close(); } catch (Exception e) {}
          sock = null;
        }
        else doCloseSock = true;
      }
    }

    public boolean isConnected ()
    {
      return (sock != null);
    }

    public boolean isError ()
    {
      return (exception != null);
    }

    public Exception getException ()
    {
      return exception;
    }

    public Socket getSocket ()
    {
      return sock;
    }
  }
}
