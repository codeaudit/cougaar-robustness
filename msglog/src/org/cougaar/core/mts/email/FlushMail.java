/*
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc. (OBJS),
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
 * 30 Jun 2003: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import com.sun.mail.util.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *  A utility to delete messages from a mailbox, intended
 *  to be called from an ACME run script before starting a
 *  run to remove old messages leftover from prior runs from
 *  mailboxes to be used in this run.
 *
 *  Invoked by:
 *    java -classpath "$CIP/lib/msglog.jar;$CIP/sys/mail.jar;$CIP/sys/activation.jar" FlushMail pop3://user:pswd@host:port debug
 *    
 *    where:
 *      host is the pop3 server's hostname or address
 *      port is the pop3 server's port (normally 110)
 *      user is the user account that should be flushed
 *      pswd is the user's password
 *      debug (optional, false by default) prints out the message if true, else just prints the number of messages flushed.
 *
**/
public class FlushMail
{    
    public static void main (String args[]) {
        if (args.length != 1 && args.length != 2) {
	    System.out.println ("usage: FlushMail \"pop3://user:pswd@host:port\" debug(optional)");
	    System.exit (-1);
	}
        URI uri = null;
        try {
	    uri = new URI(args[0]);
	} catch (URISyntaxException e) {
	    System.err.println("Error parsing " + args[0]);	    
            e.printStackTrace();
	    System.exit(-1);
	}
        MailBox mailbox = new MailBox(uri);
        boolean debug = false;
        if (args.length == 2)
	    debug = Boolean.valueOf(args[1]).booleanValue();
	try {
	    //MailMan.setPop3Debug(debug);
            MailMan.setWithCougaar(false);
	    MailMessage msgs[] = MailMan.readMessages(mailbox);
	    if (msgs != null) {
		int n = msgs.length;
		System.out.println("Flushed " + n + " messages to " + uri);
                if (debug) {
		    for (int i=0; i<n; i++) System.out.println ("---\n" + msgs[i]);
		    System.out.println ("---");
		}
	    }
	} catch (Exception e) {
	    System.err.println("Error retrieving messages: " + e);
	    System.exit(-1);
	}
    }
}
