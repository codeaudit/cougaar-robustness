/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc.
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
 * 18 Aug 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.HashMap;
import org.cougaar.core.thread.Schedulable;

public class SendTimeoutAspect extends StandardAspect
{
  private static final String FAST_TIMEOUT = "org.cougaar.message.transport.aspects.SendTimeoutAspect.fastTimeout";
  private static final String SLOW_TIMEOUT = "org.cougaar.message.transport.aspects.SendTimeoutAspect.slowTimeout";
  private static final int fastTimeout;
  private static final int slowTimeout;

  static {
    fastTimeout = Integer.valueOf(System.getProperty(FAST_TIMEOUT,"1000")).intValue();
    slowTimeout = Integer.valueOf(System.getProperty(SLOW_TIMEOUT,"5000")).intValue();
  }

  public SendTimeoutAspect() {
  }

  public Object getDelegate(Object delegate, Class type) 
  {
    if (type ==  DestinationLink.class) {
      return new SendTimeoutDestinationLink((DestinationLink) delegate);
    } else {
	return null;
    }
  }

  private class SendTimeoutDestinationLink 
    extends DestinationLinkDelegateImplBase  
  {
    DestinationLink link;
    boolean timeoutp = true;
    boolean fastp = true;
	
    private SendTimeoutDestinationLink(DestinationLink link) {
	super(link);
      this.link = link;
      String linkProtocol = link.getProtocolClass().getName();
      if (linkProtocol.equals("org.cougaar.core.mts.LoopbackLinkProtocol")) {
        timeoutp = false;
      } else {
        if (linkProtocol.equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol")) {
          fastp = false;
        }
      }
    }
	
    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, NameLookupException, 
             CommFailureException, MisdeliveredMessageException
    {
      if (timeoutp == true) {

        // Forward the message in another thread so we can timeout on it here
        ForwardMessageThread t = new ForwardMessageThread(msg,link);
        Schedulable thread = threadService.getThread(this, t, "foo");
        thread.start();
 
        // check on its progress periodically, and close it if it exceeds timeout
        final int POLL_TIME = 100;
        int sleepTime = 0;
 
        int timeout;
        if (fastp == true) {
          timeout = fastTimeout;
        } else {
          timeout = slowTimeout;
        }
        while (true) {
          if (t.isDone()) return t.getAttributes();
          if (t.isException()) {
            Exception e = t.getException();
            if (e instanceof UnregisteredNameException) 
              throw (UnregisteredNameException)e;
            if (e instanceof NameLookupException) 
              throw (NameLookupException)e;
            if (e instanceof CommFailureException) 
              throw (CommFailureException)e;
            if (e instanceof MisdeliveredMessageException) 
              throw (MisdeliveredMessageException)e;
            throw new CommFailureException(e);
          }
          try { Thread.sleep (POLL_TIME); } catch (Exception e) {}
          sleepTime += POLL_TIME;
          if (sleepTime > timeout) {
            String s = "SendTimeoutAspect: Timeout sending message " +MessageUtils.toString(msg);
            throw new CommFailureException(new Exception(s));
          }
        }
      } else {
        return link.forwardMessage(msg);
      }
    }
  }

  private static class ForwardMessageThread implements Runnable
  {
    private AttributedMessage msg = null;
    private DestinationLink link = null;
    private MessageAttributes attrs = null;
    private Exception ex = null;

    public ForwardMessageThread (AttributedMessage msg, DestinationLink link) {
      this.msg = msg;
      this.link = link;
    }
    protected MessageAttributes getAttributes() {
      return attrs;
    }
    protected Exception getException() {
      return ex;
    }
    protected boolean isException () {
      return (ex != null);
    }
    protected boolean isDone () {
      return (attrs != null);
    }
    public void run () {
      try {
        attrs = link.forwardMessage(msg);
      } catch (Exception e) {
        ex = e;
      }
    }
  }
}
