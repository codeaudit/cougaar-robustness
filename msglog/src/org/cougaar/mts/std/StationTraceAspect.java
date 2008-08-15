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
 * 06 Sep 2002: Created, derived from TraceAspect. (OBJS)
 */

package org.cougaar.mts.std;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.ReceiveLinkDelegateImplBase;
import org.cougaar.mts.base.Router;
import org.cougaar.mts.base.RouterDelegateImplBase;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * Trace a message as it passes through the various stages of the message 
 * transport subsystem.  
 */

public class StationTraceAspect extends StandardAspect
{
  private LoggingService log;

  public StationTraceAspect () 
  {}

  public void load ()
  {
    super.load();
    log = loggingService;
  }

  public Object getDelegate (Object delegate,  Class type) 
  {
	if (type == SendQueue.class)        return new SendQueueDelegate ((SendQueue) delegate);
	if (type == Router.class)           return new RouterDelegate ((Router) delegate);
	if (type == DestinationQueue.class) return new DestinationQueueDelegate ((DestinationQueue) delegate);
	if (type == DestinationLink.class)  return new DestinationLinkDelegate ((DestinationLink) delegate);
	if (type == MessageDeliverer.class) return new MessageDelivererDelegate ((MessageDeliverer) delegate);
	if (type == ReceiveLink.class)      return new ReceiveLinkDelegate ((ReceiveLink) delegate);
	return null;
  }

  public class SendQueueDelegate extends SendQueueDelegateImplBase
  {
    public SendQueueDelegate (SendQueue queue) 
    {
      super (queue);
    }
  
    public void sendMessage (AttributedMessage msg) 
    {
      if (log.isInfoEnabled()) log.info ("SendQueue: msg= " +MessageUtils.toString(msg)+ " (" +this.size()+ ")");
      super.sendMessage (msg);
    }
  }

  public class RouterDelegate extends RouterDelegateImplBase
  {
    public RouterDelegate (Router router) 
    {
      super (router);
    }
  
    public void routeMessage (AttributedMessage msg) 
    {
      if (log.isInfoEnabled()) log.info ("Router: msg= " +MessageUtils.toString(msg));
      super.routeMessage (msg);
    }
  }

  public class DestinationQueueDelegate extends DestinationQueueDelegateImplBase
  {
    public DestinationQueueDelegate (DestinationQueue queue) 
    {
      super (queue);
    }
  
    public void holdMessage (AttributedMessage msg) 
    {
      if (log.isInfoEnabled()) log.info ("DestinationQueue hold: msg= " +MessageUtils.toString(msg));
      super.holdMessage(msg);
    }

    public void dispatchNextMessage (AttributedMessage msg) 
    {
      if (log.isInfoEnabled()) log.info ("DestinationQueue dispatch: msg= " +MessageUtils.toString(msg));
      super.dispatchNextMessage (msg);
    }
  }

  public class DestinationLinkDelegate extends DestinationLinkDelegateImplBase
  {
    public DestinationLinkDelegate (DestinationLink link)
    {
      super (link);
    }
  
    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, 
             NameLookupException, 
             CommFailureException,
             MisdeliveredMessageException
    {
      if (log.isInfoEnabled()) log.info ("DestinationLink: msg= " +MessageUtils.toString(msg));
      return super.forwardMessage(msg);
    }
  }

  public class MessageDelivererDelegate extends MessageDelivererDelegateImplBase
  {
    public MessageDelivererDelegate (MessageDeliverer deliverer) 
    {
      super (deliverer);
    }
  
    public MessageAttributes deliverMessage (AttributedMessage msg, MessageAddress dest) 
      throws MisdeliveredMessageException
    { 
      if (log.isInfoEnabled()) log.info ("MessageDeliverer: msg= " +MessageUtils.toString(msg));
      return super.deliverMessage (msg, dest);
    }
  }

  public class ReceiveLinkDelegate extends ReceiveLinkDelegateImplBase
  {
    public ReceiveLinkDelegate (ReceiveLink link) 
    {
      super (link);
    }
  
    public MessageAttributes deliverMessage (AttributedMessage msg) 
    {
      if (log.isInfoEnabled()) log.info ("ReceiveLink: msg= " +MessageUtils.toString(msg));
      return super.deliverMessage (msg);
    }
  }
}
