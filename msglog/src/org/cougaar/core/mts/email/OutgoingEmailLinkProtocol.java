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
 * 18 Jun 2002: Restored Node name to outboxes properties due to facilitate
                CSMART test configuration. (OBJS)
 * 11 Apr 2002: Removed Node name from outboxes property (2 reasons -
 *              no longer able to get Node name, and, more importantly,
 *              each Node has it's own properties, the differentiation
 *              was just done for testing convienience.)  Update from 
 *              Cougaar 9.0.0 to 9.1.x (OBJS)
 * 26 Mar 2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 19 Dec 2001: Commented out FQDN warning per Steve's request. (OBJS)
 * 15 Dec 2001: Changed missing msg nums to "-" in mail header. (OBJS)
 * 11 Dec 2001: Save backup of cached mail data from name server. (OBJS) 
 * 02 Dec 2001: Implement getRemoteReference (8.6.2.0) (OBJS)
 * 02 Dec 2001: Now set the msgNum in the mail message subject line here. (OBJS)
 * 20 Nov 2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 21 Oct 2001: Make call to sendMessage class synchronized. (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol, add debug & cost
 *              properties. (OBJS)
 * 25 Sep 2001: Updated from Cougaar 8.4 to 8.4.1 (OBJS)
 * 16 Sep 2001: Updated from Cougaar 8.3.1 to 8.4 (OBJS)
 * 24 Aug 2001: Revamped for new 8.3.1 component model. (OBJS)
 * 14 Jul 2001: Require FQDN hostnames in props file, since FQDN not avail
 *              from java.net.InetAddress under Windows & JDK 1.3. (OBJS)
 * 11 Jul 2001: Added initial name server support. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.util.*;
import java.io.IOException;
import java.net.InetAddress;
import javax.mail.URLName;

import org.cougaar.util.*;
import org.cougaar.core.mts.*;

/**
 * OutgoingEmailLinkProtocol is an OutgoingLinkProtocol which uses email to
 * send Cougaar messages from the current node to other nodes in the society.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.core.mts.email.OutgoingEmailLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. -Dorg.cougaar.message.transport.classes=org.cougaar.core.mts.email.OutgoingEmailLinkProtocol,
 * org.cougaar.core.mts.email.IncomingEmailLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.email.outboxes.<node-name></b> 
 * Specify the outbound SMTP servers for a node by setting this property 
 * to a string of the form <i>smtp,host,port,replyTo,cc,bcc</i>, where:
 * <pre>
 * smtp:  The literal string "smtp".
 * host:  The fully qualified domain name (FQDN) of the host running the SMTP mail server.
 * port:  The port the SMTP mail server is listening on (typically port 25).
 * replyTo:  Either an email address (e.g. someone@somewhere.com) or a dash ("-"). Use of the "-" 
 * means skip this feature.  If an email address is supplied, it is set as the Reply-To field on all 
 * email sent out via this SMTP server.  This feature is expected to only be used in rare testing 
 * and debugging situations.
 * cc:   Either an email address (e.g. someone@somewhere.com) or a dash ("-").  Use of the "-" means 
 * skip this feature.  If an email address is supplied, it is set as the CC (carbon copy)  field on 
 * all email sent out via this SMTP server.  This feature is expected to only be used in rare testing 
 * and debugging situations.
 * bcc:   Either an email address (e.g. someone@somewhere.com) or a dash ("-").  Use of the "-" means 
 * skip this feature.  If an email address is supplied, it is set as the BCC (blind carbon copy) field 
 * on all email sent out via this SMTP server.  This feature is expected to only be used in rare 
 * testing and debugging situations.
 * </pre>
 * (e.g. for a node named "X": 
 * -Dorg.cougaar.message.transport.email.outboxes.X=smtp,wally.objs.com,25,-,-,-)
 * <br><i>[Note:  Currently, only one outbox per node can be specified. That will change.]</i>
 * <p>
 * <b>org.cougaar.message.protocol.email.cost</b>
 * The cost function of the DestinationLink inner subclass defaults to 1500, so 
 * that, using the default MinCostLinkSelectionPolicy, it will be chosen after 
 * OutgoingSocketLinkProtocol and RMILinkProtocol, and before NNTPLinkProtocol. 
 * When using AdaptiveLinkSelectionPolicy, cost is
 * one of the factors that are used to select a protocol. To modify the default
 * cost, set the property to an integer 
 * <br>(e.g. org.cougaar.message.protocol.email.cost= 750).
 * <p>
 * <b>org.cougaar.message.protocol.email.debug</b> 
 * If true, prints debug information to System.out.
 * <p>
 * <b>mail.debug</b> 
 * If true, JavaMail prints debug information to System.out.
 * */

public class OutgoingEmailLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-email";
  public static final int DEFAULT_PROTOCOL_COST = 1500;

  public static final Date transportBirthday = new Date();

  private static final Object sendLock = new Object();

  private static boolean debug;
  private static boolean debugMail = false;
  private static boolean showTraffic;

  private int protocolCost;
  private HashMap links;
  private MailBox outboxes[];
  private EmailMessageOutputStream messageOut;


  public OutgoingEmailLinkProtocol ()
  {
    System.err.println ("Creating " + this);

    links = new HashMap();

    //  Read external properties

    debug = false;
    String s = "org.cougaar.message.protocol.email.debug";
    String debugProp = System.getProperty (s);
    if (debugProp != null && debugProp.equals("true")) debug = true;

    s = "org.cougaar.message.protocol.email.cost";
    String costProp = System.getProperty (s);
    try  { protocolCost = Integer.valueOf(costProp).intValue(); } 
    catch (Exception e) { protocolCost = DEFAULT_PROTOCOL_COST; }
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  public void initialize () 
  {
    super.initialize();
System.err.println ("OutgoingEmailLinkProtocol: initialize called");

    if (getRegistry() == null)
    {
      System.err.println ("OutgoingEmailLinkProtocol: Registry not available!");
    }
    else System.err.println ("OutgoingEmailLinkProtocol: "+getRegistry().getIdentifier());
  }

  public void load () 
  {
    super_load();
    String sta = "org.cougaar.core.mts.ShowTrafficAspect";
    showTraffic = (getAspectSupport().findAspect(sta) != null);
  }

  public void setNameSupport (NameSupport nameSupport) 
  {
    //  HACK! Registry not available in constructor above

    if (getRegistry() == null)
    {
      throw new RuntimeException ("OutgoingEmailLinkProtocol: Registry not available!");
    }

    String nodeID = getRegistry().getIdentifier();

    String s = "org.cougaar.message.protocol.email.outboxes." + nodeID;
    String outboxesProp = System.getProperty (s);

    if (outboxesProp == null || outboxesProp.equals(""))
    {
      throw new RuntimeException ("Bad or missing property: " +s);
    }

    //  Initialize the outgoing email link protocol

    if (startup (outboxesProp) == false)
    {
      throw new RuntimeException ("Failure starting up OutgoingEmailLinkProtocol!");
    }
  }

  public synchronized boolean startup (String outboxesProp)
  {
    shutdown();

    //  Only using the first outbox at this time

    outboxes = parseOutboxes (outboxesProp);
    if (outboxes == null || outboxes.length < 1) return false;
    MailBox outbox = outboxes[0];

    try
    {
      if (MailMan.checkMailServerAccess (outbox) == false)
      {
        System.err.println ("\nOutgoingEmailLinkProtocol ALERT: Is your mail server up?");
        System.err.println ("Unable to access mail server:\n" + outbox.toStringDiscreet());
        return false;
      }

      messageOut = createMessageOutStream (outbox);

      return true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return false;
    }
  }

  public synchronized void shutdown ()
  {
    outboxes = null;
    if (messageOut != null) try { messageOut.close(); } catch (Exception e) {}
    messageOut = null;
  }

  public MailBox[] parseOutboxes (String outboxesProp)
  {
    if (outboxesProp == null) return null;

    Vector out = new Vector();
 
    int MaxBoxes = 1;  // FIXED to 1 for now, perhaps forever

    //  Parse the outbox specs (separated by semicolons)

    for (int i=0; i<MaxBoxes; i++)
    {
      StringTokenizer specs = new StringTokenizer (outboxesProp, ";");

      if (specs.hasMoreTokens())
      {
        String spec = specs.nextToken();
        StringTokenizer st = new StringTokenizer (spec, ",");

        while (st.hasMoreTokens()) 
        {
          String protocol = st.nextToken();

          //  SMTP outboxes  
          //
          //  Format: smtp,host,port,replyTo,cc,bcc;...;... (general)
          //          smtp,host,port,-,-,-                  (typical)

          if (protocol.equals ("smtp"))
          {
            try
            {
              String host = getFQDN (nextParm (st));
              String port = nextParm (st);

              MailBox mbox = new MailBox (protocol, host, port);

              String replyTo = nextParm (st);
              String cc = nextParm (st);
              String bcc = nextParm (st);

              if (replyTo != null || cc != null || bcc != null)
              {
                MailMessageHeader hdr = new MailMessageHeader (null, replyTo, null, cc, bcc, null);
                mbox.setBoxHeader (hdr);
              }

              out.add (mbox);
            }
            catch (Exception e)
            {
              System.err.println ("Error: bad outbox spec in " +spec);
              System.err.println ("Bad outbox spec ignored");
            }
          }
          else break;
        }
      }
      else break;
    }

    //  Create the outboxes array and return it

    if (out.size() > 0)  
    {
      outboxes = (MailBox[]) out.toArray (new MailBox[out.size()]);
      return outboxes;
    }
    else
    {
      System.err.println ("Error: No outboxes defined in " +outboxesProp);
      return null;
    }
  }

  private String getFQDN (String host)
  {
    //  Not till Java 1.4 can we get the fully qualified domain name
    //  (FQDN) for hosts (via the new InetAddress getCanonicalHostname())
    //  when running on Windows.  So for now we will require that the 
    //  hostnames in email .props files are FQDN names.

    if (host.equals ("localhost"))
    {
      System.err.println ("ERROR: Only fully qualified domain names allowed as mailhost names: " +host);
      throw new RuntimeException ("Only fully qualified domain names allowed as mailhost names: " +host);
    }
    else if (host.indexOf ('.') == -1)
    {
//    System.err.println ("WARNING: Only fully qualified domain names allowed as mailhost names: " +host);
    }

    return host;
  }

  private String nextParm (StringTokenizer st)
  {
    //  Convert "-" strings into nulls

    String next = st.nextToken().trim();
    if (next.equals("-")) next = null;
    return next;
  }

  private EmailMessageOutputStream createMessageOutStream (MailBox mbox) throws IOException
  {
    EmailOutputStream emailOut = new EmailOutputStream (mbox);
    emailOut.setInfoDebug (debug);      // informative progress debug
    emailOut.setDebug (false);          // low-level debug
    emailOut.setDebugMail (debugMail);  // low-level mail debug
    return new EmailMessageOutputStream (emailOut);
  }

  private MailData lookupMailData (MessageAddress address)
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof MailData)
      {
        if (debug) 
        {
//        System.err.print ("\nOutgoingEmail: looked up email data in name "); 
//        System.err.println ("server:\n"+ (MailData)obj);
        }

        return (MailData) obj;
      }
      else
      {
        System.err.println ("\nOutgoingEmail: Invalid data in name server!");
      }
    }

    return null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (lookupMailData (address) != null);
    } 
    catch (Exception e) 
    {
      // System.err.println ("Failed in addressKnown: " +e);
      // e.printStackTrace();
    }

    return false;
  }

  public DestinationLink getDestinationLink (MessageAddress address) 
  {
    DestinationLink link = (DestinationLink) links.get (address);

    if (link == null) 
    {
      link = new Link (address);
      link = (DestinationLink) attachAspects (link, DestinationLink.class);
      links.put (address, link);
    }

    return link;
  }

  public Object getRemoteReference (MessageAddress address) 
  {
    MailBox outbox = outboxes[0];

    String host = outbox.getServerHost();
    String addr;
    try { addr = InetAddress.getByName(host).getHostAddress(); } 
    catch (Exception e) { addr = host; }
    int port = outbox.getServerPortAsInt();

    return new URLName
    (
      outbox.getProtocol(),
      addr,
      ((port >= 0) ? port : 25),
      outbox.getFolder(),
      outbox.getUsername(),
      "*"
    );
  }

  class Link implements DestinationLink 
  {
    private MessageAddress destination;
    private MailData mailData, savedMailData;

    Link (MessageAddress destination) 
    {
      this.destination = destination;
    }

    public MessageAddress getDestination () 
    {
      return destination;
    }

    public String toString ()
    {
      return OutgoingEmailLinkProtocol.this +"-destination:"+ destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingEmailLinkProtocol.class;
    }

    public int cost (AttributedMessage msg) 
    {
      // return protocolCost;  // pre 8.6.1

      //  Calling lookupMailData() is a hack to perform the canSendMessage()
      //  kind of method within the cost function rather than in the adaptive
      //  link selection policy code, where we believe it makes more sense.

      try 
      {
        lookupMailData (msg.getTarget());
        return protocolCost;
      } 
      catch (Exception e) 
      {
        return Integer.MAX_VALUE;
      }                          
    }

    public Object getRemoteReference ()
    {
      return null;
    }
    
    public void addMessageAttributes (MessageAttributes attrs)
    {
      // TBD
    }

    public boolean retryFailedMessage (AttributedMessage msg, int retryCount)
    {
      return true;
    }

    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws NameLookupException, UnregisteredNameException,
             CommFailureException, MisdeliveredMessageException
    {
      //  Get email info for destination address
    
      MailData mailData = lookupMailData (destination);

      //  Try mailing the message

      synchronized (sendLock)
      {
        boolean success = sendMessage (msg, mailData);

        if (success == false)
        {
          Exception e = new Exception ("OutgoingEmail: sendMessage unsuccessful");
          throw new CommFailureException (e);
        }

        MessageAttributes result = new SimpleMessageAttributes();
        String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
        result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
        return result;
      }
    }
   
    private final boolean sendMessage (AttributedMessage msg, MailData destAddr)
    {
      if (debug) 
      {
        System.err.println ("\nOutgoingEmail: send " +MessageUtils.toString(msg));
      }

      boolean success = false;

      try
      {
        //  Create email header
 
        String localnode = replaceSpaces (MessageUtils.getFromAgentNode (msg));
        String localhost = outboxes[0].getServerHost();

        String remotenode = replaceSpaces (destAddr.getNodeID());
        String remoteuser = destAddr.getInbox().getUsername();
        String remotehost = destAddr.getInbox().getServerHost();

        MailMessageHeader boxHeader = outboxes[0].getBoxHeader();

        String from = "EmailStream#"+ localnode +"@"+ localhost;
        String replyTo = null, cc = null, bcc = null;

        if (boxHeader != null)
        {
          replyTo = boxHeader.getReplyTo().getMaxAddress();
          cc =      boxHeader.getCc().getMaxAddress();
          bcc =     boxHeader.getBcc().getMaxAddress();
        }

        String to = remoteuser +"@"+ remotehost;
        String subject = "To: " +remotenode+ " Msg: " + MessageUtils.getMessageNumber (msg);

        MailMessageHeader header = new MailMessageHeader (from, replyTo, to, cc, bcc, subject);

        //  Try to send the message

        messageOut.writeMsg (msg);
        messageOut.sendMsg (header);
        success = true;
      }
      catch (Exception e)
      {
        if (debug) 
        {
          System.err.println ("\nOutgoingEmail: sendMessage exception: ");
          e.printStackTrace();
        }
        
        success = false;
      }

      return success;
    }

    private String replaceSpaces (String s)
    {
      return s.replace (' ', '_');  // replace spaces with underscores
    }
  }
}
