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
 * 07 Sep 2002: Now allow email message numbers of 0. (OBJS)
 * 15 Dec 2001: Rework filtering to deal with possible "-" msg num, and re-base
 *              sorting from message number to sent date. (OBJS)
 * 29 Nov 2001: Redesign to a much simpler cache to fit with new MessageAckingAspect 
 *              which handles message caching and sequence ordering now. (OBJS)
 * 19 Oct 2001: Redesign to fix limitation where messages from different
 *              nodes would get confused with each other. (OBJS)
 * 08 Sep 2001: Relaxed getNextMessage to allow gaps going forward. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.email;

import java.io.PrintStream;
import java.util.*;

import org.cougaar.util.log.Logging;

/**
 *  A simple cache of mail messages read from a mail server.  Needed as
 *  multiple Cougaar messages can be read from one mail server interaction.
**/

public class MailMessageCache
{
  private Vector cache;

  public MailMessageCache ()
  {
    cache = new Vector();
  }
 
  public synchronized void addMessages (MailMessage msgs[])
  {
    if (msgs == null) return;

    //  Filter the messages a bit.  May want to do more here in the future.

    int nMsgs = 0;

    for (int i=0; i<msgs.length; i++)
    {
      //  A valid message number string is an integer or the string "-"

      String num = getMessageNum (msgs[i]);
      boolean valid = false;

      if (num != null)
      {
        if (num.equals ("-")) 
        {
          valid = true;
        }
        else 
        {
          try  
          { 
            Integer.valueOf(num).intValue();
            valid = true;
          } 
          catch (Exception e) {}
        }
      }

      if (!valid)
      {
        if (Logging.getLogger(MailMessageCache.class).isWarnEnabled())
        {
          String hdr = msgs[i].getHeader().toString();
          Logging.getLogger(MailMessageCache.class).warn ("Invalid email msg num (msg ignored): " +hdr);
        }

        msgs[i] = null;
      }
      else nMsgs++;
    }

    //  Sort the messages by their sent date

    if (nMsgs > 1) Arrays.sort (msgs, MessageDateSort.getInstance());

    //  Add the messages to the cache.  Note that we do not include
    //  the messages in the cache in the sort above because typically
    //  we will not be adding messages to the cache until it is empty.

    for (int i=0; i<nMsgs; i++) if (msgs[i] != null) cache.add (msgs[i]);
  }

  public synchronized MailMessage getNextMessage ()
  {
    MailMessage msg = null;
    if (cache.size() > 0) msg = (MailMessage) cache.remove (0);
    return msg;
  }

  private static String getMessageNum (MailMessage msg)
  {    
    if (msg == null) return null;  
    String subject = msg.getSubject();
    int i = subject.indexOf ("Msg: ");
    if (i < 0) return null;
    return subject.substring (i+5);
  }

  private static class MessageDateSort implements Comparator
  {
    private static final MessageDateSort instance = new MessageDateSort();
  
    private MessageDateSort () {}
  
    public static MessageDateSort getInstance ()
    {
      return instance;
    }

    public int compare (Object o1, Object o2)
    {
      if (o1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (o2 == null) return 0;
        else return 1;
      }
      else if (o2 == null) return -1;
  
      MailMessage m1 = (MailMessage) o1;
      MailMessage m2 = (MailMessage) o2;
  
      //  Sort on message send date.  If for some reason the date is missing,
      //  we don't change the position of that message in the list.

      Date d1 = m1.getSentDate();
      Date d2 = m2.getSentDate();

      long t1 = (d1 != null ? d1.getTime() : 0);
      long t2 = (d2 != null ? d2.getTime() : 0);
  
      if (t1 == t2 || t1 == 0 || t2 == 0) return 0;
      else return (t1 > t2 ? 1 : -1);
    }
  
    public boolean equals (Object obj)
    {
      return false;  // method not applicable
    }
  }

  public synchronized void dump (PrintStream out)
  {
    out.println ("\nMailMessageCache dump start");

    for (Enumeration e=cache.elements(); e.hasMoreElements(); )
    {
       MailMessage msg = (MailMessage) e.nextElement();
       out.println ("-\n " + msg);
    }

    out.println ("MailMessageCache dump end.");
  }
}
