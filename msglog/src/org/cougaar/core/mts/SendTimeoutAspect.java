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

import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.mts.acking.*;

public class SendTimeoutAspect extends StandardAspect
{
  private static final String FAST_TIMEOUT = 
    "org.cougaar.message.transport.aspects.SendTimeoutAspect.fastTimeout";
  private static final String SLOW_TIMEOUT = 
    "org.cougaar.message.transport.aspects.SendTimeoutAspect.slowTimeout";
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

          Object sem = new Object();

          // shallow copy the AttributedMessage
          //AttributedMessage msgCopy = new AttributedMessage(msg); 
          AttributedMessage msgCopy; 

          if (msg instanceof PureAckAckMessage) msgCopy = new PureAckAckMessage((PureAckAckMessage)msg);
          else if (msg instanceof PureAckMessage) msgCopy = new PureAckMessage((PureAckMessage)msg);
          else msgCopy = new AttributedMessage(msg);

          // deep copy our MessageAttributes
          MessageUtils.setFromAgent(msgCopy,new AgentID(MessageUtils.getFromAgent(msg)));
          MessageUtils.setToAgent(msgCopy,new AgentID(MessageUtils.getToAgent(msg)));
          MessageUtils.setAck(msgCopy,Ack.clone(MessageUtils.getAck(msg),msgCopy));
          RTTAspect.setRTTDataAttribute(
            msgCopy,
            RTTAspect.cloneRTTData(RTTAspect.getRTTDataAttribute(msg))); 

          ForwardMessageThread t = new ForwardMessageThread(msgCopy,link,sem);
          //Schedulable thread = threadService.getThread(this, t, "SendTimeoutAspect");
          Thread thread = new Thread(t, "SendTimeoutAspect");
          //thread.setPriority(7);
          thread.start();
          synchronized (sem) {
            try {
              sem.wait(fastp ? fastTimeout : slowTimeout);
            } catch (InterruptedException e) {
	      e.printStackTrace();
              throw new CommFailureException(e);
            }
          }
 
          if (t.isDone()) return t.getAttributes();
          if (t.isException()) {
            Exception ex = t.getException();
//System.out.println("\nSendTimeoutAspect: got exception = " + ex);
            if (ex instanceof UnregisteredNameException) 
              throw (UnregisteredNameException)ex;
            if (ex instanceof NameLookupException) 
              throw (NameLookupException)ex;
            if (ex instanceof CommFailureException) 
              throw (CommFailureException)ex;
            if (ex instanceof MisdeliveredMessageException) 
              throw (MisdeliveredMessageException)ex;
            throw new CommFailureException(ex);
          }
          //Ack ack = MessageUtils.getAck(msgCopy);
          //String srcMsgNum = (ack.isSomePureAck()) ? MessageUtils.getSrcMsgNumber(msgCopy).t : "") + ") "

          String s = 
            "SendTimeoutAspect: Timeout sending msg " 
            + MessageUtils.getMessageNumber(msgCopy) + "."
            + (MessageUtils.getAck(msgCopy).getSendCount()+1) + " "
            + MessageUtils.getMessageTypeLetter(msgCopy) + "("
            + (MessageUtils.getAck(msgCopy).isSomePureAck() ? String.valueOf(MessageUtils.getSrcMsgNumber(msgCopy)) : "") + ") "
            + MessageUtils.toAltShortSequenceID(msgCopy) + " via "   
            + AdaptiveLinkSelectionPolicy.getLinkType(link.getProtocolClass().getName());
          throw new CommFailureException(new Exception(s));
      } else {
          return link.forwardMessage(msg);
      }
    }
  }

  private class ForwardMessageThread implements Runnable
  {
    private AttributedMessage msg = null;
    private DestinationLink link = null;
    private MessageAttributes attrs;
    private Exception ex;
    private Object sem;

    public ForwardMessageThread (AttributedMessage msg, DestinationLink link, Object sem) {
      this.msg = msg;
      this.link = link;
      attrs = null;
      ex = null;
      this.sem = sem;
    }
    public void run () {
      synchronized (sem) {
        try {
          attrs = link.forwardMessage(msg);
        } catch (Exception e) {
	    e.printStackTrace();
          ex = e;
        }
        sem.notify();
      }
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
  }
}
