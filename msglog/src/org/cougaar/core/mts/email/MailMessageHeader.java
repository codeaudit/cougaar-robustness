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
 * 04 Sept 2001: Added in Sent & Received dates. (OBJS)
 * 21 Aug  2001: Added in new MailAddress type (previously String). (OBJS)
 * 13 Aug  2001: Added Cc & Bcc support. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.util.Date;
import java.text.DateFormat;


/**
 *  Holds the header information of an email message.
**/

public class MailMessageHeader
{
  private MailAddress from, replyTo, to, cc, bcc;
  private Date sent, recv;
  private String subject;


  public MailMessageHeader ()
  {}

  public MailMessageHeader (String from, String subject)
  {
    this (from, null, null, null, null, subject);
  }

  public MailMessageHeader (String from, String replyTo, 
                            String to, String cc, String bcc, 
                            String subject)
  {
    this
    (
      new MailAddress (from), new MailAddress (replyTo),
      new MailAddress (to), new MailAddress (cc), new MailAddress (bcc),
      null, null, subject
    );
  }

  public MailMessageHeader (String from, String replyTo, 
                            String to, String cc, String bcc, 
                            Date sent, Date recv, String subject)
  {
    this
    (
      new MailAddress (from), new MailAddress (replyTo),
      new MailAddress (to), new MailAddress (cc), new MailAddress (bcc),
      sent, recv, subject
    );
  }

  public MailMessageHeader (MailAddress from, MailAddress replyTo, 
                            MailAddress to, MailAddress cc, MailAddress bcc, 
                            String subject)
  {
    this
    (
      from, replyTo,
      to, cc, bcc,
      null, null, subject
    );
  }

  public MailMessageHeader (MailAddress from, MailAddress replyTo, 
                            MailAddress to, MailAddress cc, MailAddress bcc, 
                            Date sent, Date recv, String subject)
  {
    this.from =    (from != null    ? from :    new MailAddress());
    this.replyTo = (replyTo != null ? replyTo : new MailAddress());
    this.to =      (to != null      ? to :      new MailAddress());
    this.cc =      (cc != null      ? cc :      new MailAddress());
    this.bcc =     (bcc != null     ? bcc :     new MailAddress());

    this.sent = sent;
    this.recv = recv;
    this.subject = subject;
  }

  public MailAddress getFrom ()
  {
    return from;
  }

  public void setFrom (MailAddress f)
  {
    from = f;
  }

  public MailAddress getReplyTo ()
  {
    return replyTo;
  }

  public void setReplyTo (MailAddress r)
  {
    replyTo = r;
  }

  public MailAddress getTo ()
  {
    return to;
  }

  public void setTo (MailAddress t)
  {
    to = t;
  }

  public MailAddress getCc ()
  {
    return cc;
  }

  public void setCc (MailAddress c)
  {
    cc = c;
  }

  public MailAddress getBcc ()
  {
    return bcc;
  }

  public void setBcc (MailAddress b)
  {
    bcc = b;
  }

  public Date getSentDate ()
  {
    return sent;
  }
  
  public Date getReceivedDate ()
  {
    return recv;
  }

  public String getSubject ()
  {
    return subject;
  }

  public void setSubject (String s)
  {
    subject = s;
  }

  public boolean equals (Object obj)
  {
    if (!(obj instanceof MailMessageHeader)) return false;

    MailMessageHeader a = this;
    MailMessageHeader b = (MailMessageHeader) obj;

    //  Elements must be both null or equals()

    if (!equalsTo (a.from, b.from))       return false;
    if (!equalsTo (a.replyTo, b.replyTo)) return false;
    if (!equalsTo (a.to, b.to))           return false;
    if (!equalsTo (a.cc, b.cc))           return false;
    if (!equalsTo (a.sent, b.sent))       return false;
    if (!equalsTo (a.recv, b.recv))       return false;
    if (!equalsTo (a.subject, b.subject)) return false;

    return true;
  }

  private boolean equalsTo (Object a, Object b)
  {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.equals (b);
  }

  public String toString ()
  {
    StringBuffer buf = new StringBuffer();

    boolean doReplyTo = !replyTo.isEmpty() && !replyTo.equals(from);
    
                        buf.append ("    From: " + from + "\n");
    if (doReplyTo)      buf.append ("Reply-To: " + replyTo + "\n");
    if (!to.isEmpty())  buf.append ("      To: " + to + "\n");
    if (!cc.isEmpty())  buf.append ("      Cc: " + cc + "\n");
    if (!bcc.isEmpty()) buf.append ("     Bcc: " + bcc + "\n");
    if (sent != null)   buf.append ("    Sent: " + DateFormat.getDateTimeInstance().format(sent) + "\n");
    if (recv != null)   buf.append ("Received: " + DateFormat.getDateTimeInstance().format(recv) + "\n");
                        buf.append (" Subject: " + subject + "\n");

    return buf.toString();
  }
}
