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
 * 23 Sep 2002: Created. (OBJS)
 */

package org.cougaar.mts.std.email;

import javax.net.SocketFactory;

import org.cougaar.core.component.ServiceBroker;


public class SmtpTimeoutSocketFactory extends TimeoutSocketFactory
{
  private static ServiceBroker serviceBroker;
  private static int socketTimeout;
  private static SmtpTimeoutSocketFactory instance;

  public SmtpTimeoutSocketFactory ()
  {
    super (serviceBroker, socketTimeout);
  }

  public static void setServiceBroker (ServiceBroker sb)
  {
    serviceBroker = sb;
  }

  public static ServiceBroker getServiceBroker ()
  {
    return serviceBroker;
  }

  public static void setSocketTimeout (int timeout)
  {
    socketTimeout = timeout;
  }

  public static int getSocketTimeout ()
  {
    return socketTimeout;
  }

  public static SocketFactory getDefault ()
  {
    if (instance == null) instance = new SmtpTimeoutSocketFactory();
    return instance;
  }
}
