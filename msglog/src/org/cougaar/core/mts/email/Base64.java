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

package org.cougaar.core.mts.email;

import java.io.*;
import com.sun.mail.util.*;


/**
 *  Base64 is a utility class providing Base64 encoding and 
 *  decoding of byte arrays into Strings and vice-versa.
**/

public class Base64
{
  private static ByteArrayOutputStream out = new ByteArrayOutputStream (2048);
  private static BASE64EncoderStream encoder = new  BASE64EncoderStream (out);

  public static String encodeBytes (byte bytes[])
  {
    if (bytes == null) return null;

    String encodedString = null;
    out.reset();

    try
    {
      encoder.write (bytes);
      encoder.flush();
      encodedString  = new String (out.toByteArray());
    }
    catch (Exception e)
    {
      System.err.println ("Base64.encodeBytes: " + e);
    }

    out.reset();
    return encodedString;
  }

  public static byte[] decodeString (String str)
  {
    if (str == null) return null;

    byte bytes[] = str.getBytes();  // HACK? - str encoding issues
    ByteArrayInputStream in = new ByteArrayInputStream (bytes);
    BASE64DecoderStream decoder = new  BASE64DecoderStream (in);

    int n = -1;
    byte tmp[] = new byte[bytes.length];

    try
    {
      n = decoder.read (tmp);
    }
    catch (Exception e)
    {
      System.err.println ("Base64.decodeString: " + e);
    }
    
    if (n >= 0)
    {
      byte decodedBytes[] = new byte[n];
      for (int i=0; i<n; i++) decodedBytes[i] = tmp[i];
      return decodedBytes;
    }
    else return null;
  }
}
