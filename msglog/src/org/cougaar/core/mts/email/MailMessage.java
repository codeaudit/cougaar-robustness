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
 * 13 Aug  2001: Added Cc and Bcc support. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.util.Date;


/**
 *  Top-level class for holding all the data for an email message.
**/

public class MailMessage
{
  MailMessageHeader header;
  MailMessageBody body;


  public MailMessage (String from, String to, String subject)
  {
    this (from, null, to, null, null, subject, null);
  }

  public MailMessage (String from, String replyTo, String to, String subject)
  {
    this (from, replyTo, to, null, null, subject, null);
  }

  public MailMessage (String from, String replyTo, String to, String subject, String body)
  {
    this (from, replyTo, to, null, null, subject, body);
  }

  public MailMessage (String from, String replyTo, 
                      String to, String cc, String bcc, 
                      String subject)
  {
    this (from, replyTo, to, cc, bcc, subject, null);
  }

  public MailMessage (String from, String replyTo, 
                      String to, String cc, String bcc, 
                      String subject, String body)
  {
    this (new MailMessageHeader (from, replyTo, to, cc, bcc, subject), 
          new MailMessageBody (body));
  }

  public MailMessage (String from, String replyTo, 
                      String to, String cc, String bcc, 
                      Date sent, Date recv, String subject, String body)
  {
    this (new MailMessageHeader (from, replyTo, to, cc, bcc, sent, recv, subject), 
          new MailMessageBody (body));
  }

  public MailMessage (MailAddress from, MailAddress replyTo, 
                      MailAddress to, MailAddress cc, MailAddress bcc, 
                      String subject, String body)
  {
    this (new MailMessageHeader (from, replyTo, to, cc, bcc, subject), 
          new MailMessageBody (body));
  }

  public MailMessage (MailAddress from, MailAddress replyTo, 
                      MailAddress to, MailAddress cc, MailAddress bcc, 
                      Date sent, Date recv, String subject, String body)
  {
    this (new MailMessageHeader (from, replyTo, to, cc, bcc, sent, recv, subject), 
          new MailMessageBody (body));
  }

  public MailMessage (MailMessageHeader header)
  {
    this (header, null);
  }

  public MailMessage (MailMessageHeader header, MailMessageBody body)
  {
    if (header == null) header = new MailMessageHeader();
    if (body == null)   body   = new MailMessageBody();

    this.header = header;
    this.body = body;
  }

  public MailAddress getFrom ()
  {
    return header.getFrom();
  }

  public void setFrom (MailAddress f)
  {
    header.setFrom (f);
  }

  public MailAddress getReplyTo ()
  {
    return header.getReplyTo();
  }

  public void setReplyTo (MailAddress t)
  {
    header.setReplyTo (t);
  }

  public MailAddress getTo ()
  {
    return header.getTo();
  }

  public void setTo (MailAddress t)
  {
    header.setTo (t);
  }

  public MailAddress getCc ()
  {
    return header.getCc();
  }

  public void setCc (MailAddress cc)
  {
    header.setCc (cc);
  }

  public MailAddress getBcc ()
  {
    return header.getBcc();
  }

  public void setBcc (MailAddress bcc)
  {
    header.setBcc (bcc);
  }

  public Date getSentDate ()
  {
    return header.getSentDate();
  }
  
  public Date getReceivedDate ()
  {
    return header.getReceivedDate();
  }

  public String getSubject ()
  {
    return header.getSubject();
  }

  public void setSubject (String s)
  {
    header.setSubject (s);
  }

  public void setBodyBytes (String text)
  {
    body.setBodyBytes (text);
  }

  public void setBodyBytes (byte[] bytes)
  {
    body.setBodyBytes (bytes);
  }

  public byte[] getBodyBytes ()
  {
    return body.getBodyBytes();
  }

  public void setBodyContent (String c)
  {
    body.setBodyContent (c);
  }

  public String getBodyContent ()
  {
    return body.getBodyContent();
  }

  public void setHeader (MailMessageHeader h)
  {
    header = h;
  }

  public MailMessageHeader getHeader ()
  {
    return header;
  }

  public void setBody (MailMessageBody b)
  {
    body = b;
  }

  public MailMessageBody getBody ()
  {
    return body;
  }

  public boolean equals (Object obj)
  {
    if (!(obj instanceof MailMessage)) return false;

    MailMessage a = this;
    MailMessage b = (MailMessage) obj;

    if (!a.getHeader().equals (b.getHeader())) return false;
    if (!a.getBody()  .equals (b.getBody()))   return false;

    return true;
  }

  public String toString ()
  {
    return header + "    " + body;
  }
}
