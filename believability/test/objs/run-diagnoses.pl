#!/usr/bin/perl

# Reads a file of diagnoses and issues them to the running cougaar
# system via the inject-diagnosis.pl script.

$DEBUG=1;

print "\nSTART: Injecting diagnoses script.\n\n";

$line_num = 0;
$diagnosis_count = 0;
$error_count = 0;
while( <STDIN> )
{
    $line_num++;

    next if m/^\s*\#/;   # Skip comments
    next if m/^\s*$/;    # skip blank lines

    chomp;               # Strip newlines/carriage returns

    @field = split( '\s+' );

    if ( $DEBUG )
    {
	   print "DEBUG: Input fields:\n";
	   foreach $i (0..$#field)
	   {
		  print "\t$i) ".$field[$i]."\n";
	   }
    } # if DEBUG

    unless ( $#field == 4 )
    {
	   print "ERROR line $line_num: Wrong number of fields.\n";
	   $error_count++;
	   next;
    }

    unless ( $field[0] =~ m/^\s*\d+\s*$/ )
    {
	   print "ERROR line $line_num: Bad integer for delay time.\n";
	   $error_count++;
	   next;
    }

    unless ( $field[2] =~ m/^\s*\d+\s*$/ )
    {
	   print "ERROR line $line_num: Bad integer for port number.\n";
	   $error_count++;
	   next;
    }

    $cmd = "./inject-diagnosis.pl $field[1] $field[2] $field[3] $field[4]";

    $diagnosis_count++;
    print "Diagnosis Number: $diagnosis_count\n";
    
    print "\tSLEEP: $field[0] seconds\n";
    sleep( $field[0] );

    print "\tEXEC: $cmd\n";

    system( "$cmd > /dev/null" );
    
}

print "\nEND: Injecting diagnoses script.\n\n";

print "\tSent $diagnosis_count diagnoses.\n";
print "\tFound $error_count errors.\n";

