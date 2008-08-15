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
 * 18 Apr  2002: Update from Cougaar 9.0.0 to 9.1.x (OBJS)
 * 21 Mar  2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 08 Jan  2002: Egregious temporary hack to handle last minute traffic
 *               masking messages.  (OBJS)
 * 13 Dec  2001: Re-org history to only record history after the message
 *               is forwarded, add wasLastSendSuccessful() method. (OBJS)
 * 02 Dec  2001: Removed aspect order dependency by getting message number
 *               by direct method call rather than envelope extraction.
 *               Removed rejectTransport() method - it had silently gone
 *               away.  Altered getDelegate() to keep out local msgs. (OBJS)
 * 29 Nov  2001: Change getting message number to MessageAckingAspect way. (OBJS)
 * 20 Nov  2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 24 Sept 2001: Updated from Cougaar 8.4 to 8.4.1 (OBJS)
 * 18 Sept 2001: Updated from Cougaar 8.3.1 to 8.4 (OBJS)
 * 08 Sept 2001: Created. (OBJS)
 */

package org.cougaar.mts.std;

import org.cougaar.core.mts.Attributes;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * An aspect which collects a history of message sends, used in
 * conjunction with the AdaptiveLinkSelectionPolicy.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.transport.aspects</b>
 * To cause this aspect to be loaded at init time, which is required if you are using
 * AdaptiveLinkSelectionPolicy, add <i>org.cougaar.core.mts.MessageSendHistoryAspect</i> 
 * to this property, adjacent to, and before, <i>org.cougaar.core.mts.MessageNumberAspect</i>.
 * <br>(e.g. -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.MessageSendHistoryAspect,
 * org.cougaar.core.mts.MessageNumberAspect)
 * <br><i>[Note: For more information, see the javadoc for AdaptiveLinkSelectionPolicy.]</i>
 * <p><b>org.cougaar.message.transport.aspects.sendHistory.debug</b> 
 * If true, prints debug information to System.out.
 * */

public class MessageSendHistoryAspect extends StandardAspect
{
//  HACK  - where to house history?
  //private static final MessageHistory messageHistory = AdaptiveLinkSelectionPolicy.messageHistory;
  public static final MessageHistory messageHistory = new MessageHistory();


  private static final Object recLock = new Object();

  public MessageSendHistoryAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == DestinationLink.class) 
    {
      DestinationLink link = (DestinationLink) delegate;
      String linkProtocol = link.getProtocolClass().getName();

      //  Avoid the loopback link - currently we are not keeping history on local messages
      
      if (!linkProtocol.equals("org.cougaar.mts.base.LoopbackLinkProtocol")) 
      {
        return new MessageSendHistoryDestinationLink (link);
      }
    }
 
    return null;
  }

  private class MessageSendHistoryDestinationLink extends DestinationLinkDelegateImplBase 
  {
    DestinationLink link;

    private MessageSendHistoryDestinationLink (DestinationLink link) 
    {
      super (link); 
      this.link = link;
    }

    public MessageAttributes forwardMessage (AttributedMessage message) 
      throws UnregisteredNameException, NameLookupException, 
  	         CommFailureException, MisdeliveredMessageException
    {
      int id = -1;
      MessageAttributes attrs = null;
      MessageHistory.SendRecord rec;
      Exception exception = null;

      //  Try sending the message

      long sendTime = 0;

      try
      {
        sendTime = now();
        attrs = link.forwardMessage (message);
      }
      catch (Exception e)
      {
        exception = e;

        if (loggingService.isDebugEnabled()) loggingService.debug (stackTraceToString(e));
        if (loggingService.isDebugEnabled()) {
	    loggingService.debug("msg="+MessageUtils.toString(message));
	    Attributes msgattrs = message.cloneAttributes();
	    if (msgattrs instanceof SimpleMessageAttributes)
		loggingService.debug(((SimpleMessageAttributes)msgattrs).getAttributesAsString());
	}        
      }

      //  NOTE:  Should we include traffic masking messages or heartbeats and the
      //  like in the message send history?
      
      //  If we got to this point, we assume the link has successfully
      //  sent the message or otherwise thrown an exception.

      //  NOTE:  Are we really guaranteed this situation though?  What if the 
      //  message is stuck in a queue, or some other delayed send situation?

      //  NOTE:  The acking message resender now resets the success flag if
      //  the message is resent - ie. the message send was sucessful in its
      //  first hop, but the lack of an ack back for the message indicates
      //  a potential send problem with that transport.
      
      id = AdaptiveLinkSelectionPolicy.getTransportID (link.getProtocolClass());
      
      synchronized (recLock)
      {
        rec = messageHistory.sends.get (id, message);
        if (rec == null) rec = new MessageHistory.SendRecord (id, message);
      
        rec.sendTimestamp = sendTime;
        rec.abandonTimestamp = (exception == null ? 0 : now());
        rec.success = (exception == null);
      
        messageHistory.sends.put (rec);
      }

      if (exception != null)
      {
        if (exception instanceof UnregisteredNameException)
        {
          throw (UnregisteredNameException) exception;
        }
        else if (exception instanceof NameLookupException)
        {
          throw (NameLookupException) exception;
        }
        else if (exception instanceof CommFailureException)
        {
          throw (CommFailureException) exception;
        }
        else if (exception instanceof MisdeliveredMessageException)
        {
          throw (MisdeliveredMessageException) exception;
        }
        else
        {
          throw new CommFailureException (exception);
        }
      }

      return attrs;
    }

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return link.retryFailedMessage (message, retryCount);
    }

    public Class getProtocolClass ()
    {
      return link.getProtocolClass();
    }

    public String toString ()
    {
      return link.toString();
    }

    public int cost (AttributedMessage message) 
    {
      return link.cost (message);
    }
  }

  public static float getPercentSuccessfulSendsByTransportID (int id)
  {
    return messageHistory.sends.getPercentSuccessfulSendsByTransportID (id);
  }

  public static boolean hasHistory (int id)
  {
    return messageHistory.sends.hasHistory (id);
  }

  public static boolean wasLastSendSuccessful (int id)
  {
    return messageHistory.sends.lastSendSuccessfulByTransportID (id);
  }

  public static void registerSendFailure (AttributedMessage msg)
  {
    String sendLink = MessageUtils.getSendProtocolLink (msg);
    int id = AdaptiveLinkSelectionPolicy.getTransportID (sendLink);
    MessageHistory.SendRecord rec = messageHistory.sends.get (id, msg);
    if (rec != null) rec.success = false;
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
