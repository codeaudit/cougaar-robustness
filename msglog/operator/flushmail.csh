#!/bin/tcsh 
/usr/java/j2sdk1.4.1/bin/java -classpath $1/lib/msglog.jar:$1/sys/mail.jar:$1/sys/activation.jar:$1/lib/util.jar org.cougaar.core.mts.email.FlushMail $2 $3

