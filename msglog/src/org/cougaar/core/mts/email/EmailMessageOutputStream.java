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
 * 26 Mar  2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 22 Oct  2001: Change derivation to NoHeaderOutputStream, and add
 *               stream reset call. (OBJS)
 * 24 Sept 2001: Hack to handle 8.4.1 MessageEnvelopes. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;

import org.cougaar.core.mts.Message;


public class EmailMessageOutputStream extends NoHeaderOutputStream
{
  EmailOutputStream out;

  public EmailMessageOutputStream (EmailOutputStream out) throws IOException
  {
    super (out);
    this.out = out;
  }

  public void writeMsg (Message msg) throws IOException
  {
    //  Important!!!  Reset the output stream so that subsequent
    //  writes do not refer to objects written previously.

    reset();
    writeObject (msg);
  }

  public void sendMsg (MailMessageHeader header) throws IOException
  {
    out.flush (header);  // sends email message
  }
}
