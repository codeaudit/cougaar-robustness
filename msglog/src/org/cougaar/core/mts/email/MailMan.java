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
 * 12 Jun 2002: Replaced System.getProperties call in openStore(), cited
 *              as potential security hole. (OBJS)
 * 27 Oct 2001: Added serverConnTimeout property, upped default server
 *              timeout from 4 to 8 seconds, and improved sendMessage
 *              exception catching and debug printing.  Also switched 
 *              from regular socket in checkMailServerAccess to a 
 *              timed socket, and added code to make sure to close the
 *              socket before leaving. (OBJS)
 * 21 Aug 2001: Added in new MailAddress type (previously String). (OBJS)
 * 13 Aug 2001: Added Cc & Bcc support. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.util.*;
import java.net.Socket;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.*;

import org.cougaar.util.log.Logging;

/**
 *  Main implementation class for mail sending and receiving.
**/

public class MailMan 
{
  public static final int DEFAULT_SERVER_CONN_TIMEOUT = 8*1000;  // in msecs

  private static final String TIMEOUT_PROPERTY = "org.cougaar.message.protocol.email.serverConnTimeout";

  private static int serverConnectionTimeout;
  private static final Message[] zeroMsgs = new Message[0];
  static boolean Debug;

  static 
  {
    String timeoutProp = System.getProperty (TIMEOUT_PROPERTY);
    try  { serverConnectionTimeout = Integer.valueOf(timeoutProp).intValue(); } 
    catch (Exception e) { serverConnectionTimeout = DEFAULT_SERVER_CONN_TIMEOUT; }
  }


  public static void setDebug (boolean b)
  {
    Debug = b;
  }

  private static Store openStore (MailBox mbox) throws Exception
  {
    // Get a Session object

    Properties props = new Properties();  // was System.getProperties();
    props.put ("mail.pop3.connectiontimeout", ""+serverConnectionTimeout);
    Session session = Session.getDefaultInstance (props, null);
    session.setDebug (Debug);

    // Get a Store object and do a connect

    Store store = null;

    String protocol = mbox.getProtocol();
    String host =     mbox.getServerHost();
    int    port =     mbox.getServerPortAsInt();
    String user =     mbox.getUsername();
    String password = mbox.getPassword();
    
    if (protocol != null) store = session.getStore (protocol);
    else                  store = session.getStore ();

    if (host != null || user != null || password != null)
    {
      store.connect (host, port, user, password);
    }
    else store.connect ();

    return store;
  }

  public static boolean checkMailBoxAccess (MailBox mbox)
  {
    //  Check that we can establish a connection to the mail server
    //  and account specified in the given mailbox.

    try
    {
      openStore (mbox);
      return true;
    }
    catch (Exception e)
    {
      return false;
    }      
  }

  public static boolean checkMailServerAccess (MailBox mbox)
  {
    //  Check that we can establish a connection to the mail server.
    //  Could go farther and establish that it is an SMTP mail server.

    Socket socket = null;

    try
    {
      String host = mbox.getServerHost();
      int port = mbox.getServerPortAsInt();

      socket = TimedSocket.getSocket (host, port, serverConnectionTimeout);

      return true;
    }
    catch (Exception e)
    {
      return false;
    }
    finally
    {
      if (socket != null)
      {
        try { socket.close(); } catch (Exception e) {}
      }
    }
  }

  public static MailMessageHeader[] readHeaders (MailBox mbox) throws Exception
  {
    return readHeaders (mbox, null, 0);
  }

  public static MailMessageHeader[] readHeaders (MailBox mbox, int pollTime) throws Exception
  {
    return readHeaders (mbox, null, pollTime);
  }

  public static MailMessageHeader[] readHeaders (MailBox mbox, MailFilters filters, int pollTime) 
    throws Exception
  {
    // Get and connect to a mail store

    Store store = openStore (mbox);

    //  Root folder

    Folder folder = null;

    if (mbox.getFolder() != null) folder = store.getFolder (mbox.getFolder());
    else                          folder = store.getDefaultFolder();

    if (Debug) Logging.getLogger(MailMan.class).debug ("Mailbox: " + folder.getURLName());

    if (!folder.isSubscribed())
    {
      throw new Exception ("Folder is not subscribed! (Is that important?)");
    }

    if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0)
    {
      throw new Exception ("Folder type does not hold messages!");
    }

    //  Build search terms from filters, if any

    SearchTerm searchTerms = null;

    if (filters != null)
    {
      FromStringTerm fromTerm = null;
      SubjectTerm subjectTerm = null;

      if (filters.fromFilter != null)    fromTerm = new FromStringTerm (filters.fromFilter);
      if (filters.subjectFilter != null) subjectTerm = new SubjectTerm (filters.subjectFilter);
    
      if (fromTerm != null && subjectTerm != null) 
      {
        searchTerms = new AndTerm (fromTerm, subjectTerm);
      }
      else if (fromTerm != null) 
      {
        searchTerms = fromTerm;
      }
      else searchTerms = subjectTerm;
    }

    //  Read headers

    Message msgs[] = zeroMsgs;
    MailMessageHeader mmh[] = null;

    try
    {
      while (true)
      {
        folder.open (Folder.READ_ONLY);

        if (searchTerms != null) msgs = folder.search (searchTerms);
        else                     msgs = folder.getMessages();

        if (msgs == null) msgs = zeroMsgs;

        if (Debug)  Logging.getLogger(MailMan.class).debug ("Read " + msgs.length + " message headers");

        //  Continue polling for headers in a loop if we did not get any
        //  and the poll time > 0

        if (msgs.length == 0 && pollTime > 0)
        {
          folder.close (false);
          try { Thread.sleep (pollTime); } catch (Exception e) {}
        }
        else break;
      }

      if (msgs.length > 0)
      {
        if (msgs.length > 1)  // define en masse threshold
        {
          //  Pre-fetch items en masse for the messages

          FetchProfile fp = new FetchProfile();
          fp.add (FetchProfile.Item.ENVELOPE);
          folder.fetch (msgs, fp);
        }

        //  Actually make use of the items, which will fetch
        //  them if they were not pre-fetched.

        mmh = new MailMessageHeader[msgs.length];
        for (int i=0; i<msgs.length; i++) mmh[i] = decodeHeader (msgs[i]);
      }  
      else mmh= new MailMessageHeader[0];
    }
    catch (Exception e)
    {
      throw e;
    }
    finally 
    {
      folder.close (false);
    }

    return mmh;
  }

  private static MailMessageHeader decodeHeader (Message msg)
  {    
    //  NOTE: Currently only decoding (at most) a single item for address fields
    
    //  From:

    Address f[] = null;
    try { f = msg.getFrom(); } catch (Exception e) {}
    MailAddress from = getMailAddresses(f)[0];

    //  ReplyTo:
    
    Address rt[] = null;
    try { rt = msg.getReplyTo(); } catch (Exception e) {}
    MailAddress replyTo = getMailAddresses(rt)[0];

    //  To:
    
    Address t[] = null;
    try { t = msg.getRecipients (Message.RecipientType.TO); } catch (Exception e) {}
    MailAddress to = getMailAddresses(t)[0];

    //  Cc:
    
    Address c[] = null;
    try { c = msg.getRecipients (Message.RecipientType.CC); } catch (Exception e) {}
    MailAddress cc = getMailAddresses(c)[0];

    //  Bcc:

    Address b[] = null;
    try { b = msg.getRecipients (Message.RecipientType.BCC); } catch (Exception e) {}
    MailAddress bcc = getMailAddresses(b)[0];

    //  SentDate:

    Date sent = null;
    try { sent = msg.getSentDate(); } catch (Exception e) {}
    
    //  RecvDate:

    Date recv = null;
    try { recv = msg.getReceivedDate(); } catch (Exception e) {}
    
    //  Subject:

    String subject = null;
    try { subject = msg.getSubject(); } catch (Exception e) {}

    //  Return

    return new MailMessageHeader (from, replyTo, to, cc, bcc, sent, recv, subject);
  }

  private static MailAddress[] getMailAddresses (Address addrs[])
  {
    //  NOTE: Only getting (at most) first address!

    Address a = (addrs != null && addrs.length > 0) ? addrs[0] : null;
    MailAddress addresses[] = new MailAddress[1];
    addresses[0] = getMailAddress (a);
    return addresses;
  }

  private static MailAddress getMailAddress (Address addr)
  {
    String personal = null, address = null;
    
    if (addr != null)
    {
      if (addr instanceof InternetAddress)
      {
        personal = ((InternetAddress)addr).getPersonal();
        address  = ((InternetAddress)addr).getAddress();
      }
      else
      { 
        personal = null;
        address = addr.toString();
      }
    }
    
    return new MailAddress (personal, address);
  }

  public static MailMessage[] readMessages (MailBox mbox) throws Exception
  {
    return readMessages (mbox, null, 0);
  }

  public static MailMessage[] readMessages (MailBox mbox, int pollTime) throws Exception
  {
    return readMessages (mbox, null, pollTime);
  }

  public static MailMessage[] readMessages (MailBox mbox, MailFilters filters,
       int pollTime) throws Exception
  {
    // Get and connect to a mail store

    Store store = openStore (mbox);

    //  Root folder

    Folder folder = null;

    if (mbox.getFolder() != null) folder = store.getFolder (mbox.getFolder());
    else                          folder = store.getDefaultFolder();
  
    if (Debug)  Logging.getLogger(MailMan.class).debug ("Mailbox: " + folder.getURLName());

    if (!folder.isSubscribed())
    {
      throw new Exception ("Folder is not subscribed! (Is that important?)");
    }

    if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0)
    {
      throw new Exception ("Folder type does not hold messages!");
    }

    //  Build search terms from filters, if any

    SearchTerm searchTerms = null;

    if (filters != null)
    {
      FromStringTerm fromTerm = null;
      SubjectTerm subjectTerm = null;

      if (filters.fromFilter != null)  fromTerm = new FromStringTerm (filters.fromFilter);
      if (filters.subjectFilter != null) subjectTerm = new SubjectTerm (filters.subjectFilter);
    
      if (fromTerm != null && subjectTerm != null) 
      {
        searchTerms = new AndTerm (fromTerm, subjectTerm);
      }
      else if (fromTerm != null) 
      {
        searchTerms = fromTerm;
      }
      else searchTerms = subjectTerm;
    }

    //  Read messages

    Message[] msgs = zeroMsgs;
    MailMessage mm[] = null;
    boolean success = false;

    try
    {
      while (true)
      {
        folder.open (Folder.READ_WRITE);

        if (searchTerms != null) msgs = folder.search (searchTerms);
        else                     msgs = folder.getMessages();

        if (msgs == null) msgs = zeroMsgs;

        if (Debug) Logging.getLogger(MailMan.class).debug ("Found " + msgs.length + " messages");

        //  Continue polling for messages in a loop if we did not get
        //  any messages and the poll time > 0

        if (msgs.length == 0 && pollTime > 0)
        {
          folder.close (false);
          try { Thread.sleep (pollTime); } catch (Exception e) {}
        }
        else break;
      }

      mm = new MailMessage[msgs.length];
      if (msgs.length == 0) return mm;

      //  Pre-fetch as needed

      if (msgs.length > 1)  // define en masse threshold
      {    
        FetchProfile fp = new FetchProfile();
        fp.add (FetchProfile.Item.ENVELOPE);
        folder.fetch (msgs, fp);
      }

      //  Get the header and content of the mail message.  You can't pre-fetch the
      //  content of the mail message.  In the likely event that getting the content
      //  also gets the header info, we'll get content first.

      for (int i=0; i<msgs.length; i++) 
      {
        //  Get content first
      
        Part part = (Part) msgs[i];
        String content = null;
        boolean isTextPlain = true;
      
        if (part.isMimeType ("text/plain")) 
        {
          content = (String) part.getContent();
        }  
        else isTextPlain = false;

        //  Then get (if not pre-fetched) and decode the headers

        mm[i] = new MailMessage (decodeHeader (msgs[i]));

        //  Then decode the content last - since we got the MailMessage objects
        //  from the header processing and we need them to deposit the contents.

        if (isTextPlain)
        {
          //  Base64 decode the content string back into bytes

          byte bytes[] = Base64.decodeString (content);  
          mm[i].setBodyBytes (bytes);
        }
        else
        {
          //  We'll save off the message content for now eventhough we may
          //  end up throwing away the message later.

          mm[i].setBodyContent (content);
          Logging.getLogger(MailMan.class).debug ("Got message that's not text/plain!");
        }

        if (Debug) Logging.getLogger(MailMan.class).debug ((i+1)+ ".  " + mm);
      }

      //  Delete read mail messages

      folder.setFlags (msgs, new Flags (Flags.Flag.DELETED), true);
      folder.close (true);  // true = expunge deleted messages

      success = true;
    }
    catch (Exception e)
    {
      throw e;
    }
    finally 
    {
      if (!success) folder.close (false);  // do not delete any messages
    }

    //  Return

    return mm;
  }

  public static boolean sendMessage (MailBox mbox, MailMessage msg) throws Exception
  {
    //  Get a Session object

    Properties props = new Properties();
    props.put ("mail.smtp.host", mbox.getServerHost());
    props.put ("mail.smtp.port", mbox.getServerPort());                    // Sun impl supports this
    props.put ("mail.smtp.connectiontimeout", ""+serverConnectionTimeout); // Sun impl supports this
    if (Debug) props.put ("mail.debug", "true");

    Session session = Session.getInstance (props);  // note not (potentially) shared session!
    session.setDebug (Debug);

    //  Create and send the message

    try
    {
      //  Construct mail message

      Message message = new MimeMessage (session);

      //  From:

      String from = msg.getFrom().getMaxAddress();

      if (from != null && !from.equals("")) 
      {
        message.setFrom (new InternetAddress (from));
      }

      //  Reply-To:

      String replyTo = msg.getReplyTo().getMaxAddress();

      if (replyTo != null && !replyTo.equals(""))
      {
        InternetAddress[] addrs = { new InternetAddress (replyTo) };
        message.setReplyTo (addrs);
      }

      //  To:

      String to = msg.getTo().getMaxAddress();

      if (to != null && !to.equals(""))
      {
        InternetAddress[] addrs = { new InternetAddress (to) };
        message.setRecipients (Message.RecipientType.TO, addrs);
      }

      //  Cc:

      String cc = msg.getCc().getMaxAddress();

      if (cc != null && !cc.equals(""))
      {
        InternetAddress[] addrs = { new InternetAddress (cc) };
        message.setRecipients (Message.RecipientType.CC, addrs);
      }

      //  Bcc:

      String bcc = msg.getBcc().getMaxAddress();

      if (bcc != null && !bcc.equals(""))
      {
        InternetAddress[] addrs = { new InternetAddress (bcc) };
        message.setRecipients (Message.RecipientType.BCC, addrs);
      }

      //  Subject:

      message.setSubject (msg.getSubject());

      //  Sent date

      message.setSentDate (new Date());

      //  Body

      if (msg.getBodyContent() != null)
      {
        message.setText (msg.getBodyContent());  // poss setText (text, charset)
      }
      else if (msg.getBodyBytes() != null)
      {
        String encodedBytes = Base64.encodeBytes (msg.getBodyBytes()); 
        message.setText (encodedBytes);

        //System.out.println ("encodedBytes len = " + encodedBytes.length());
        //System.out.println ("encodedBytes = " + encodedBytes);
      }

      //  Note: JavaMail allows you to only send one mail message at
      //  a time, reconnecting with the mail server each time.
      
      Transport.send (message);

      return true;  // msg successfuly sent
    } 
    catch (Exception e) 
    {
      if (Debug)
      {
        Logging.getLogger(MailMan.class).debug ("sendMessage exception: " +stackTraceToString(e));

        Exception ex = e;
        StringBuffer buf = new StringBuffer();

        do 
        {
          if (ex instanceof SendFailedException) 
          {
            SendFailedException sfex = (SendFailedException)ex;
            Address[] invalid = sfex.getInvalidAddresses();
            String sp = "         ";
            
            if (invalid != null) 
            {
              buf.append ("    ** Invalid Addresses\n");
              for (int i=0; i<invalid.length; i++) buf.append (sp +invalid[i]+ "\n");
            }

            Address[] validUnsent = sfex.getValidUnsentAddresses();
          
            if (validUnsent != null) 
            {
              buf.append ("    ** ValidUnsent Addresses\n");
              for (int i=0; i<validUnsent.length; i++) buf.append (sp +validUnsent[i] +"\n");
            }
          
            Address[] validSent = sfex.getValidSentAddresses();

            if (validSent != null) 
            {
              buf.append ("    ** ValidSent Addresses\n");
              for (int i=0; i<validSent.length; i++) buf.append (sp +validSent[i]+ "\n");
            }
          }
        
          Logging.getLogger(MailMan.class).debug (buf.toString());

          if (ex instanceof MessagingException) ex = ((MessagingException)ex).getNextException();
          else ex = null;
        }
        while (ex != null);
      }
    }

    return false;  // msg not successfuly sent
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
