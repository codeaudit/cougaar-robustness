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
 * 11 Jun 2002: Change Thread.sleep to CougaarThread.sleep. (OBJS)
 * 21 Oct 2001: Make msg cache be passed in instead of local. (OBJS)
 * 26 Sep 2001: Added InfoDebug and pollTime getter/setter. (OBJS)
 * 06 Sep 2001: Add support to ignore messages older than a given date. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
import javax.mail.Store;

import org.cougaar.core.thread.CougaarThread;


/**
 *  EmailInputStream is an InputStream that reads email for its data source.
**/

public class EmailInputStream extends InputStream
{
  private boolean InfoDebug;
  private boolean Debug;
  private boolean DebugMail;

  private MailBox mbox;
  private MailFilters filters;
  private MailMessageCache cache;
  private byte buffer[];
  private int index, bufsize;
  private int pollTime;
  private String fromFilter, subjectFilter;
  private MailMessage lastMsgRead;
  private boolean streamClosed;
  private boolean haveReadMail;
  private boolean ignoreOldMessages;
  private Date earliestMessageDate;  


  public EmailInputStream (MailBox mb, int pollTime, MailMessageCache cache) throws Exception
  {
    this (mb.getProtocol(), mb.getServerHost(), mb.getServerPort(),
          mb.getUsername(), mb.getPassword(),   mb.getFolder(),  pollTime, cache); 
  }

  public EmailInputStream (String protocol, String host, String port, String user, 
    String pswd, String folder, int pollTime, MailMessageCache cache) throws Exception
  {
    mbox = new MailBox (protocol, host, port, user, pswd, folder);
    filters = new MailFilters();
    buffer = new byte[2048];
    index = bufsize = 0;
    streamClosed = false;
    haveReadMail = false;
    this.pollTime = pollTime;
    this.cache = cache;
    
    if (cache == null)
    {
      throw new RuntimeException ("EmailInputStream: Null cache not allowed!");
    }
    
    MailMan.setDebug (DebugMail);
    
    if (Debug) 
    {
      System.err.println ("\nEmailInputStream: created: " +host+ ":" +port+ " user=" + user);
      cache.dump (System.err);
    }
  }

  public void setInfoDebug (boolean b)
  {
    InfoDebug = b;
  }

  public void setDebug (boolean b)
  {
    Debug = b;
  }

  public void setDebugMail (boolean b)
  {
    DebugMail = b;
  }

  public int getPollTime ()
  {
    return pollTime;
  }

  public void setPollTime (int t)
  {
    pollTime = t;
  }

  public MailMessageCache getCache ()
  {
    return cache;
  }

  public void setFromFilter (String s)
  {
    fromFilter = s;
  }

  public void setSubjectFilter (String s)
  {
    subjectFilter = s;
  }

  public void ignoreOlderMessages (Date earliestDate)
  {
    if (earliestDate != null)
    {
      ignoreOldMessages = true;
      earliestMessageDate = earliestDate;
    }
    else
    {
      ignoreOldMessages = false;
      earliestMessageDate = null;
    }
  }

  public int read () throws IOException
  {
    // if (Debug) System.out.print ("\nEmailInputStream.read: reading 1 byte: ");

    if (streamClosed) return -1;

    if (index >= bufsize)
    {
      if (haveReadMail == false) 
      {
        readMail();
        if (streamClosed) return -1;
        haveReadMail = true;
      }
      else 
      { 
        haveReadMail = false;
        return -1; 
      }

      if (bufsize == 0) 
      {
        if (Debug) System.err.println ("\nEmailInputStream.read: ERROR - no more bytes from mail");
        return -1;
      }

      index = 0;
    }

    // if (Debug) System.out.println ("" + (buffer[index] & 0xFF));

    return buffer[index++] & 0xFF;
  }

  public int read (byte bytes[], int off, int len) throws IOException
  {
    if (Debug) System.out.println ("\nEmailInputStream.read: " +now()+ " reading " +len+ " bytes ...");

    if (streamClosed) return -1;

    int n;

    haveReadMail = false;

    try
    {
      for (n=0; n<len; n++)
      {
        int b;

        if ((b = read()) < 0)
        {
          if (n == 0) n = -1;
          break;
        }

        bytes[off + n] = (byte)b;
      }
    }
    catch (Exception e)
    {
      n = -1;
    }

    if (Debug) System.out.println ("\nEmailInputStream.read: " +now()+ " read " +n+ " of " +len+ " bytes.");

    return n;  // number of bytes read or -1 for error/eof
  }

  private final void readMail ()
  {
    if (streamClosed) return;

    //  Our local buffer is empty.  Possibly read some mail and refill it.

    bufsize = 0;      

    try
    {
      //  Goto the cache for the next mail message in line.
      //  If none there, try getting more from the mail server.

      MailMessage msg=null, msgs[];

      MailMan.setDebug (DebugMail);

      while (streamClosed == false)
      {
        msg = cache.getNextMessage();  // messages NOT necessarily in order
        if (msg != null) break;

        //  We call readMessages() such that it will not wait until there 
        //  is at least one message meeting the filter criterion before 
        //  returning - instead we wait here (if pollTime > 0) so that we 
        //  have more control over shutting down this stream.

        filters.set (fromFilter, subjectFilter);

        msgs = MailMan.readMessages (mbox, filters, 0);

        if (msgs != null)
        {
          if (msgs.length == 0 && pollTime > 0)
          {
            try { CougaarThread.sleep (pollTime); } catch (Exception e) {}
            if (streamClosed) return;
          }

          //  Possibly ignore old messages

          for (int i=0; i<msgs.length; i++)
          {
            if (InfoDebug) System.err.println ("\nRead email:\n" + msgs[i]);

            if (ignoreOldMessages)
            {
              Date msgSent = msgs[i].getSentDate();
             
              if (msgSent == null) 
              {
                if (Debug) System.err.println ("\nFYI Email message has no sent date!");
                continue;
              }

              if (earliestMessageDate.compareTo (msgSent) > 0)
              {
                if (InfoDebug) System.err.println ("\nNote: This old message is discarded!");
                msgs[i] = null;                
              }
            }
          }

          if (msgs.length > 0) cache.addMessages (msgs);
        }
      }

      if (streamClosed) return;

      if (msg == null)
      {
        throw new RuntimeException ("EmailInputStream: Unexpected null msg!");
      }

      if (Debug) System.err.println ("\nEmailInputStream.readMail: read " +msg);

      //  Save a pointer to the current message so that info like from 
      //  and reply-to can be accessed.

      lastMsgRead = msg;

      //  Put mail bytes into our buffer
      
      byte bytes[] = msg.getBodyBytes();

      if (bytes == null)
      {
        if (Debug) System.err.println ("\nEmailInputStream.readMail: ERROR: Got msg with null bytes!!");
      }
      else
      { 
        for (int j=0; j<bytes.length; j++)
        {
          //  Enlarge our buffer if needed
      
          if (bufsize == buffer.length)
          {
            byte tmp[] = new byte[buffer.length*2];
            for (int k=0; k<buffer.length; k++) tmp[k] = buffer[k];
            buffer = tmp;
          }
      
          buffer[bufsize++] = bytes[j];
        }
      }
    }
    catch (javax.mail.AuthenticationFailedException afe)
    {
      if (Debug) System.err.println ("\nEmailInputStream.readMail: Is your mail server running? : " +afe);
    }
    catch (Exception e)
    {
      if (Debug) System.err.println ("\nEmailInputStream.readMail: Error reading messages: " +e);
    }
  }

  public MailMessage getLastMsgRead ()
  {
    return lastMsgRead;
  }

  public void close ()
  {
    streamClosed = true;
    try { super.close(); } catch (Exception e) {}
  }

  public final static long now ()
  {
    return System.currentTimeMillis();
  }
}
