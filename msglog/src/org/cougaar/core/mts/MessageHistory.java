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
 * 29 Nov  2001: Change getting message number to MessageAckingAspect way. (OBJS)
 * 24 Sept 2001: Updated from Cougaar 8.4 to 8.4.1 (OBJS)
 * 18 Sept 2001: Updated from Cougaar 8.3.1 to 8.4 (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;


/** 
 *  Store records of the activity of messages as they go thru the MessageTransport 
 *  system. 
**/

class MessageHistory
{
  public static final sendsClass sends = new sendsClass();
  public static final receivesClass receives = new receivesClass();

  public static class sendsClass
  {
    private static final Hashtable numTable = new Hashtable();
    private static final Hashtable idTable = new Hashtable();
    
    public static synchronized void put (MessageHistory.SendRecord rec)
    {
      Integer key = new Integer (rec.messageNum);
      Vector v = (Vector) numTable.get (key);
      if (v == null) v = new Vector();
      else if (v.contains (rec)) return;  // no duplicates
      v.add (rec);
      numTable.put (key, v);

      key = new Integer (rec.transportID);
      v = (Vector) idTable.get (key);
      if (v == null) v = new Vector();
      else if (v.contains (rec)) return;  // no duplicates
      v.add (rec);
      idTable.put (key, v);
    }

    public boolean hasHistory (AttributedMessage m)
    {
      int messageNum = getMessageNum (m);
      Vector v = (Vector) numTable.get (new Integer (messageNum));
      return (v != null && v.size() > 0);
    }

    public static MessageHistory.SendRecord get (int id, AttributedMessage m)
    {
      int messageNum = getMessageNum (m);
      return get (id, messageNum);
    }

    public static MessageHistory.SendRecord get (int id, int num)
    {
      MessageHistory.SendRecord[] recs = getByMessageNum (num);
      for (int i=0; i<recs.length; i++)
      {
        if (recs[i].transportID == id) return recs[i];
      }      

      return null;
    }

    public static MessageHistory.SendRecord[] getByMessageNum (int num)
    {
      Vector v = (Vector) numTable.get (new Integer(num));
      if (v == null) return new MessageHistory.SendRecord[0];
      return (MessageHistory.SendRecord[]) v.toArray (new MessageHistory.SendRecord[v.size()]); 
    }

    public static MessageHistory.SendRecord[] getByTransportID (int id)
    {
      Vector v = (Vector) idTable.get (new Integer(id));
      if (v == null) return new MessageHistory.SendRecord[0];
      return (MessageHistory.SendRecord[]) v.toArray (new MessageHistory.SendRecord[v.size()]); 
    }

    public static MessageHistory.SendRecord getLastRecByTransportID (int id)
    {
      Vector v = (Vector) idTable.get (new Integer(id));
      return (v != null ? (MessageHistory.SendRecord) v.lastElement() : null);
    }

    public static int countSuccessfulSends (AttributedMessage m)
    {
      int messageNum = getMessageNum (m);
      return countSuccessfulSendsByMessageNum (messageNum);
    }

    public static int countSuccessfulSendsByMessageNum (int num)
    {
      int count = 0;
      MessageHistory.SendRecord[] recs = getByMessageNum (num);

      for (int i=0; i<recs.length; i++) 
      {
        if (recs[i].sendTimestamp > 0 && recs[i].success == true) count++;
      }

      return count;
    }      

    public static int countSuccessfulSendsByTransportID (int id)
    {
      int count = 0;
      MessageHistory.SendRecord[] recs = getByTransportID (id);

      for (int i=0; i<recs.length; i++) 
      {
        if (recs[i].sendTimestamp > 0 && recs[i].success == true) count++;
      }

      return count;
    }      

    public static boolean allSendsUnsuccessful (AttributedMessage m)
    {
      //  If there were no sends then return false

      int messageNum = getMessageNum (m);
      return allSendsUnsuccessfulByMessageNum (messageNum);
    }

    public static boolean allSendsUnsuccessfulByMessageNum (int num)
    {
      int count = 0;
      MessageHistory.SendRecord[] recs = getByMessageNum (num);

      for (int i=0; i<recs.length; i++) 
      {
        if (recs[i].sendTimestamp > 0 && recs[i].success == false) count++;
      }

      //  If there were no sends then return false

      return (recs.length > 0 ? (count == recs.length) : false);
    }      

    public static int countUnsuccessfulSendsByTransportID (int id)
    {
      int count = 0;
      MessageHistory.SendRecord[] recs = getByTransportID (id);

      for (int i=0; i<recs.length; i++) 
      {
        if (recs[i].sendTimestamp > 0 && recs[i].success == false) count++;
      }

      return count;
    }

    public static boolean lastSendSuccessfulByTransportID (int id)
    {
      MessageHistory.SendRecord rec = getLastRecByTransportID (id);
      return (rec != null ? rec.success : false);
    }      
  }

  public static class receivesClass
  {
    private static final Hashtable numTable = new Hashtable();
    private static final Hashtable idTable = new Hashtable();
    
    public static synchronized void put (MessageHistory.ReceiveRecord rec)
    {
      Integer key = new Integer (rec.messageNum);
      Vector v = (Vector) numTable.get (key);
      if (v == null) v = new Vector();
      else if (v.contains (rec)) return;  // no duplicates
      v.add (rec);
      numTable.put (key, v);

      key = new Integer (rec.transportID);
      v = (Vector) idTable.get (key);
      if (v == null) v = new Vector();
      else if (v.contains (rec)) return;  // no duplicates
      v.add (rec);
      idTable.put (key, v);
    }

    public static MessageHistory.ReceiveRecord get (int id, int num)
    {
      MessageHistory.ReceiveRecord[] recs = getByMessageNum (num);

      for (int i=0; i<recs.length; i++)
      {
        if (recs[i].transportID == id) return recs[i];
      }      

      return null;
    }

    public static MessageHistory.ReceiveRecord[] getByMessageNum (int num)
    {
      Vector v = (Vector) numTable.get (new Integer(num));
      if (v == null) return new MessageHistory.ReceiveRecord[0];
      return (MessageHistory.ReceiveRecord[]) v.toArray (new MessageHistory.ReceiveRecord[v.size()]); 
    }

    public static MessageHistory.ReceiveRecord[] getByTransportID (int id)
    {
      Vector v = (Vector) idTable.get (new Integer(id));
      if (v == null) return new MessageHistory.ReceiveRecord[0];
      return (MessageHistory.ReceiveRecord[]) v.toArray (new MessageHistory.ReceiveRecord[v.size()]); 
    }
  }

  private static class BaseRecord
  {
    public int transportID;
    public int messageNum;
    public boolean success;
  }

  public static class SendRecord extends BaseRecord
  {
    public MessageAddress destination;
    public long routeTimestamp;
    public long sendTimestamp;
    public long abandonTimestamp;

    public SendRecord (int transportID, AttributedMessage m)
    {
      this.transportID = transportID;
      messageNum = getMessageNum (m);
      destination = m.getTarget();
    }

    public String toString ()
    {
      return "[" + "\n" +
             "      transportID= " + transportID + "\n" +
             "       messageNum= " + messageNum + "\n" +
             "      destination= " + destination + "\n" +
             "   routeTimestamp= " + toDate (routeTimestamp) + "\n" +
             "    sendTimestamp= " + toDate (sendTimestamp) + "\n" +
             " abandonTimestamp= " + toDate (abandonTimestamp) + "\n" +
             "          success= " + success + "\n" +
             "]";
    }
  }

  public static class ReceiveRecord extends BaseRecord
  {
    public MessageAddress origination; 
    public long receiveTimestamp;

    public String toString ()
    {
      return "[" + "\n" +
             "      transportID= " + transportID + "\n" +
             "       messageNum= " + messageNum + "\n" +
             "      origination= " + origination + "\n" +
             " receiveTimestamp= " + toDate (receiveTimestamp) + "\n" +
             "          success= " + success + "\n" +
             "]";
    }
  }

  private static int getMessageNum (AttributedMessage msg)
  {
    return MessageUtils.getMessageNumber (msg);
  }

  private static long timeNow ()
  {
    return System.currentTimeMillis();
  }

  private static String toDate (long time)
  {
    if (time == 0) return "0";

    //  Return date string with milliseconds

    String date = (new Date(time)).toString();
    String d1 = date.substring (0, 19);
    String d2 = date.substring (19);
    long ms = time % 1000;
    return d1 + "." + ms + d2;
  }
}
