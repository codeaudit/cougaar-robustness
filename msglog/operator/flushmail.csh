#!/bin/tcsh 
java -classpath $CIP/lib/msglog.jar:$CIP/sys/mail.jar:$CIP/sys/activation.jar:$CIP/lib/util.jar org.cougaar.core.mts.email.FlushMail $1 $2

