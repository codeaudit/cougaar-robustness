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
 * 02 Dec  2001: Remove setting the message number in the subject line. (OBJS)
 * 26 Sept 2001: Added InfoDebug. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import java.util.*;
import java.net.InetAddress;

import org.cougaar.util.log.Logging;

/**
 *  EmailOutputStream is an OutputStream that outputs email
 *  to a specified mailbox.
**/

public class EmailOutputStream extends OutputStream
{
  private MailBox mbox;
  private ByteArrayOutputStream buf;

  private boolean InfoDebug;
  private boolean Debug;
  private boolean DebugMail;


  public EmailOutputStream (MailBox mbox)
  {  
    this.mbox = mbox;
    buf = new ByteArrayOutputStream (2048);
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

  public void write (int b) throws IOException
  {
    if (Debug)
    {
      Logging.getLogger(EmailOutputStream.class).debug ("write: " +now()+ " writing int = " +b);
    }

    buf.write (b);
  }

  public void write (byte b[]) throws IOException
  {
    if (Debug)
    {
      Logging.getLogger(EmailOutputStream.class).debug ("write: " +now()+ " writing bytes = " +b.length);
    }

    buf.write (b, 0, b.length);
  }

  public void write (byte b[], int off, int len) throws IOException
  {
    if (Debug)
    {
      Logging.getLogger(EmailOutputStream.class).debug ("write: " +now()+ " writing bytes = " +len);
    }

    buf.write (b, off, len);
  }

  public void flush () throws IOException
  {
    flush (null);  // causes runtime exception
  }

  public synchronized void flush (MailMessageHeader header) throws IOException
  {
    if (header == null)
    {
      throw new IOException ("EmailOutputSteam: Unexpected - missing msg header!");
    }

    if (Debug)
    {
      Logging.getLogger(EmailOutputStream.class).debug ("flush: " +now()+ " called");
    }

    //  Set the contents of the buffer as the body of an email message and
    //  reset the buffer.

    MailMessageBody body = new MailMessageBody (buf.toByteArray());
    buf.reset();

    int len = body.getBodyBytes().length;

    if (Debug)
    {
      Logging.getLogger(EmailOutputStream.class).debug ("flush: body bytes len = " + len);
      if (len == 0) Logging.getLogger(EmailOutputStream.class).debug ("flush: no msg sent since len = 0");
    }

    if (len > 0)
    {
      boolean success = false;

      try
      {
        MailMessage msg = new MailMessage (header, body);

        if (InfoDebug) Logging.getLogger(EmailOutputStream.class).info ("Sending email:\n" + msg);

        MailMan.setDebug (DebugMail);
        success = MailMan.sendMessage (mbox, msg);
      }
      catch (Exception e)
      {
        if (Debug) 
        {
          Logging.getLogger(EmailOutputStream.class).debug ("flush: Error sending message: ");
          e.printStackTrace();
        }

        success = false;
      }

      if (success == false)
      {
        throw new IOException ("EmailOutputStream.flush: Failure sending message");
      }
    }
  }

  public final static long now ()
  {
    return System.currentTimeMillis();
  }

  public String toString ()
  {
    return "EmailOutputStream[" + mbox + "]";
  }
}
