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
 * 28 Sep 2002: Improved buffering. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import com.sun.mail.util.*;

import org.cougaar.util.log.*;

/**
 *  Base64 is a utility class providing Base64 encoding and 
 *  decoding of byte arrays into Strings and vice-versa.
**/

public class Base64
{
  private static final int NOMINAL_BUFFER_SIZE = 16*1024;

  private static ByteArrayOutputStream out;
  private static BASE64EncoderStream encoder;

  public static synchronized String encodeBytes (byte bytes[])
  {
    if (bytes == null) return null;

    if (out == null)
    {
      out = new ByteArrayOutputStream (NOMINAL_BUFFER_SIZE);
      encoder = new  BASE64EncoderStream (out);
    }
    else out.reset();

    String encodedString = null;

    try
    {
      encoder.write (bytes);
      encoder.flush();
      encodedString  = new String (out.toByteArray());
    }
    catch (Exception e)
    {
      Logging.getLogger(Base64.class).error ("encoding bytes: " +stackTraceToString(e));
    }

    if (out.size() > NOMINAL_BUFFER_SIZE)
    {
      out = null;     // gc large bufs
      encoder = null;
    }
    else out.reset();

    return encodedString;
  }

  public static byte[] decodeString (String str)
  {
    if (str == null) return null;

    byte bytes[] = str.getBytes();  // HACK? - str encoding issues
    ByteArrayInputStream in = new ByteArrayInputStream (bytes);
    BASE64DecoderStream decoder = new  BASE64DecoderStream (in);

    byte buf[] = new byte[bytes.length];
    int n = -1;

    try
    {
      n = decoder.read (buf);
    }
    catch (Exception e)
    {
      Logging.getLogger(Base64.class).warn("in decodeString(): " + stackTraceToString(e));
      Logging.getLogger(Base64.class).warn("A few of these are ok, else lengthen email stream timeouts.");
      return null;
    }
    
    return (n >= 0 ? buf : null);
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
