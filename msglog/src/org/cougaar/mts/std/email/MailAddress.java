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
 * 21 Aug  2001: Created. (OBJS)
 */

package org.cougaar.mts.std.email;


/**
 *  MailAddress is the address used in an email for the To, From, etc. fields.
**/

public class MailAddress
{
  private String personal, address;


  public MailAddress ()
  {
    this (null, null);
  }

  public MailAddress (String address)
  {
    this (null, address);
  }

  public MailAddress (String personal, String address)
  {
    this.personal = personal;
    this.address = address;
  }

  public String getPersonal ()
  {
    return personal;
  }

  public void setPersonal (String s)
  {
    personal = s;
  }

  public String getAddress ()
  {
    return address;
  }

  public void setAddress (String s)
  {
    address = s;
  }

  public String getMaxAddress ()
  {
    if (personal != null && address != null) return personal + " <" + address + ">";
    if (address != null) return address;
    return null;
  }

  public String getShortDisplay ()
  {
    //  Return first available: personal, address, ""

    if (personal != null) return personal;
    if (address != null) return address;
    return "";
  }

  public String getLongDisplay ()
  {
    String max = getMaxAddress();
    return (max != null ? max : getShortDisplay());
  }

  public String toString ()
  {
    return getLongDisplay();
  }

  public boolean equals (Object obj)
  {
    if (!(obj instanceof MailAddress)) return false;

    MailAddress a = this;
    MailAddress b = (MailAddress) obj;

    //  Elements must be both null or equals()

    if (!equalsTo (a.personal, b.personal)) return false;
    if (!equalsTo (a.address,  b.address))  return false;

    return true;
  }

  private boolean equalsTo (Object a, Object b)
  {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.equals (b);
  }

  public boolean isEmpty ()
  {
    return (personal == null && address == null);
  }
}
