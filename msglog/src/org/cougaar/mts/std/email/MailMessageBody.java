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
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.mts.std.email;


/**
 *  Holds the contents of an email message.
**/

public class MailMessageBody
{
  private byte bodyBytes[];
  private String bodyContent;


  public MailMessageBody ()
  {}

  public MailMessageBody (String text)
  {
    setBodyBytes (text);
  }

  public MailMessageBody (byte[] bytes)
  {
    setBodyBytes (bytes);
  }

  public void setBodyBytes (String text)
  {
    byte bytes[] = null;
    if (text != null) bytes = text.getBytes();
    setBodyBytes (bytes);
  }

  public void setBodyBytes (byte[] bytes)
  {
    bodyBytes = bytes;
  }

  public byte[] getBodyBytes ()
  {
    return bodyBytes;
  }

  public void setBodyContent (String c)
  {
    bodyContent = c;
  }

  public String getBodyContent ()
  {
    return bodyContent;
  }

  public boolean equals (Object obj)
  {
    if (!(obj instanceof MailMessageBody)) return false;

    byte a[] = this.getBodyBytes();
    byte b[] = ((MailMessageBody) obj).getBodyBytes();

    int alen = (a != null ? a.length : 0);
    int blen = (b != null ? b.length : 0);

    if (alen != blen) return false;

    for (int i=0; i<alen; i++) if (a[i] != b[i]) return false;

    return true;
  }

  public String toString ()
  {
    int n = (bodyBytes != null ? bodyBytes.length : 0);
    return ("Body: " + n + " bytes");
  }
}
