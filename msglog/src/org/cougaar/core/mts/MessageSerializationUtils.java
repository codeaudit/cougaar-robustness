/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
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
 * 18 Sep 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.io.*;
import java.security.*;


/**
 * Collection of utility serialization/deserialization methods
 */
    
public final class MessageSerializationUtils
{
  private static final int MAGIC_NUMBER = 1618033988;  // 1st 9 digits of Golden Ratio
  private static final int USING_MESSAGE_DIGEST_FLAG = 0x01;

  private static final byte[] magicBytes;
  private static final byte[] intBytes = new byte[4];

  static
  {
    try { magicBytes = convertIntToBytes (MAGIC_NUMBER); } 
    catch (Exception e) { throw new RuntimeException ("No magic bytes!"); }
  }

  public static byte[] writeMessageToByteArray (AttributedMessage msg) 
    throws Exception
  {
    return writeObjectToByteArray (msg, null);
  }

  public static byte[] writeMessageToByteArray (AttributedMessage msg, MessageDigest digest)
    throws IOException
  {
    return writeObjectToByteArray (msg, digest);
  }
    
  public static byte[] writeObjectToByteArray (Object obj)
    throws IOException
  {
    return writeObjectToByteArray (obj, null);
  }
    
  public static byte[] writeObjectToByteArray (Object obj, MessageDigest digest)
    throws IOException
  {
    String algorithm = null;
    int digestLength = 0;
    int flagByte = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = null;

    baos.write (magicBytes);
    baos.write (intBytes); // placeholder for final size
    
    if (digest != null)
    {
      algorithm = digest.getAlgorithm();
      digestLength = digest.getDigestLength();

      if (!algorithm.equals("MD5") || digestLength != 16)  // only MD5 - temp HACK
      {
        throw new RuntimeException ("Ony MD5 with 16 byte hash currently supported");  
      }

      flagByte = USING_MESSAGE_DIGEST_FLAG;
      baos.write (flagByte);
      baos.write (new byte[digestLength]);
      baos.flush();

      digest.reset();
      DigestOutputStream dos = new DigestOutputStream (baos, digest);
      oos = new ObjectOutputStream (dos);  // fyi stream header written
    }
    else 
    {
      flagByte = 0;
      baos.write (flagByte);
      baos.flush();

      oos = new ObjectOutputStream (baos); // fyi stream header written
    }

    oos.writeObject (obj);
    oos.flush();

    try { oos.close(); } catch (Exception e) {}
    byte byteArray[] = baos.toByteArray();

    byte[] size = convertIntToBytes (byteArray.length);
    for (int i=0; i<4; i++) byteArray[i+4] = size[i];

    if (digest != null)
    {
      byte messageDigest[] = digest.digest();  // digest is reset with this call
      for (int i=0; i<digestLength; i++) byteArray[i+9] = messageDigest[i];
    }

    return byteArray;
  }

  public static AttributedMessage readMessageFromByteArray (byte data[]) 
    throws MessageDeserializationException
  {
    return readMessageFromByteArray (data, null);
  }

  public static AttributedMessage readMessageFromByteArray (byte data[], MessageDigest digest) 
    throws MessageDeserializationException
  {
    try
    {
      return (AttributedMessage) readObjectFromByteArray (data, digest);
    }
    catch (Exception e)
    {
      throw new MessageDeserializationException (e);
    }
  }

  public static Object readObjectFromByteArray (byte[] data) 
    throws DataIntegrityException, ClassNotFoundException, IOException
  {
    return readObjectFromByteArray (data, null);
  }

  public static Object readObjectFromByteArray (byte[] data, MessageDigest digest) 
    throws DataIntegrityException, ClassNotFoundException, IOException
  {
    int digestLength = 16;
    byte embeddedDigest[] = null;
	ObjectInputStream ois = null;
	Object obj = null;

    //  Read the byte array header to verify the magic number and get the byte
    //  count of the encoded data.

    if (readByteArrayHeader (data) > data.length)
    {
      throw new DataIntegrityException ("Byte array too short for contained data");
    }

    ByteArrayInputStream bais = new ByteArrayInputStream (data);
    bais.skip (getByteArrayHeaderLength());
                     
    int flagByte = (bais.read() & 0xff);

    if ((flagByte & USING_MESSAGE_DIGEST_FLAG) != 0)
    {
      embeddedDigest = new byte[digestLength];
      bais.read (embeddedDigest, 0, embeddedDigest.length);        

      if (digest == null)
      {
        try { digest = MessageDigest.getInstance ("MD5"); }
        catch (Exception e) { throw new RuntimeException (e); }
      }

      digest.reset();
      DigestInputStream dis = new DigestInputStream (bais, digest);
      ois = new ObjectInputStream (dis);
    }
    else ois = new ObjectInputStream (bais);
	
	obj = ois.readObject();
	
	try { ois.close(); } catch (Exception e) {}

    if (digest != null)
    {
      byte calculatedDigest[] = digest.digest();  // digest is reset with this call

      if (!MessageDigest.isEqual (embeddedDigest, calculatedDigest))
      {
        throw new DataIntegrityException ("Embedded and calculated digests not equal");
      }
    }

	return obj;
  }

  public static int readByteArrayHeader (byte[] data) throws DataIntegrityException, IOException
  {
    if (convertBytesToInt (data) != MAGIC_NUMBER)
    {
      throw new DataIntegrityException ("Magic number wrong!");
    }

    return convertBytesToInt (data, 4);  // embedded size field
  }

  public static int getByteArrayHeaderLength ()
  {
    return 8;
  }

  private static byte[] convertIntToBytes (int value) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream (4);
    DataOutputStream dos = new DataOutputStream (baos);
    dos.writeInt (value);
    dos.flush();
    try { dos.close(); } catch (Exception e) {}
    return baos.toByteArray();
  }

  private static int convertBytesToInt (byte[] bytes) throws IOException
  {
    return convertBytesToInt (bytes, 0);
  }

  private static int convertBytesToInt (byte[] bytes, int offset) throws IOException
  {
    ByteArrayInputStream bais = new ByteArrayInputStream (bytes);
    DataInputStream dis = new DataInputStream (bais);
    if (offset > 0) bais.skip (offset);
    int value = dis.readInt();
    try { dis.close(); } catch (Exception e) {}
    return value;
  }

  public static void writeByteArray (OutputStream out, byte[] bytes) throws IOException
  {
    out.write (bytes, 0, bytes.length);
    out.flush();
  }

  public static byte[] readByteArray (InputStream in) 
    throws DataIntegrityException, IOException
  {
    return readByteArray (in, null);
  }

  public static byte[] readByteArray (InputStream in, byte[] buf) 
    throws DataIntegrityException, IOException
  {
    int hdrLen = getByteArrayHeaderLength();
    if (buf == null || buf.length < hdrLen) buf = new byte[hdrLen];

    int offset = 0, len = hdrLen, n = 0;
    while ((offset < len) && ((n = in.read (buf,offset,len-offset)) >= 0)) offset += n;
    if (n == -1) throw new EOFException ("Got a -1 on the read");

    int msgLen = readByteArrayHeader (buf);

    if (buf.length < msgLen) 
    {
      byte[] newbuf = new byte[msgLen];
      for (int i=0; i<hdrLen; i++) newbuf[i] = buf[i];
      buf = newbuf;
    }

    offset = hdrLen; len = msgLen; n = 0;
    while ((offset < len) && ((n = in.read (buf,offset,len-offset)) >= 0)) offset += n;
    if (n == -1) throw new EOFException ("Got a -1 on the read");

    return buf;
  }

  public static String byteBufferDigestToString (byte[] buf)
  {
    if (buf == null) return null;

    int flagByte = (int)(buf[0] & 0xff);

    if ((flagByte & USING_MESSAGE_DIGEST_FLAG) != 0)
    {
      int digestLength = 16;
      byte digest[] = new byte[digestLength];
      for (int i=0; i<digest.length; i++) digest[i] = buf[i+1];
      return digestToString (digest);
    }
    
    return "<no digest>";
  }

  public static String digestToString (byte[] digest)
  {
    StringBuffer buf = new StringBuffer (digest.length * 2);
        
    for (int i=0; i<digest.length; i++)
    {
      int n = digest[i] & 0xff;
      if (n < 0x10) buf.append ("0");
      buf.append (Integer.toHexString (n));
    }
        
    return buf.toString();
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
