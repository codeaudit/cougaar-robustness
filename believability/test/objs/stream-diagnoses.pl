#!/usr/bin/perl

# Continually issues diagnoses in a random fashion.

$DEBUG=1;

$max_diagnosis = $ARGV[0];

print "\nSTART: Streaming random diagnoses script.\n\n";

%port_num = (
		   "TestAgent" => 8801,
		   "TestAgent2" => 8801,
		   "TestAgent3" => 8800,
		   "TestAgent4" => 8800
		   );

%sensor_values = (
			   "SampleDiagnosis" => "Compromised Secure Secure Isolated", 
			   "RestartDiagnosis" => "Live Live Live Live Dead Dead Dead"
			   );

$sleep_secs = 0;

@agent_name = keys( %port_num );
@sensor_name = keys( %sensor_values );

$diagnosis_count = 0;
$error_count = 0;
while( $diagnosis_count < $max_diagnosis )
{

    # Select an agent at random
    $agent_idx = int( rand() * ($#agent_name+1) );

    # Select a sensor at random
    $sensor_idx = int( rand() * ($#sensor_name+1) );
    
    # Select a sensor value (diagnosis) at random
    @sensor_value = split( '\s+', $sensor_values{$sensor_name[$sensor_idx]} );
    $diagnosis_idx = int( rand() * ($#sensor_value+1) );

    $agent_name = $agent_name[$agent_idx];
    $port = $port_num{$agent_name};
    $sensor_name = $sensor_name[$sensor_idx];
    $diagnosis_value = $sensor_value[$diagnosis_idx];

    # Send the diagnosis on its way.
    $cmd = "./inject-diagnosis.pl $agent_name $port $sensor_name $diagnosis_value";

    $diagnosis_count++;
    
    print "  EXEC[$diagnosis_count]: $cmd\n";

    system( "$cmd > /dev/null" );

    sleep( $sleep_secs );
    
}

print "\nEND: Streaming random diagnoses script.\n\n";

print "\tSent $diagnosis_count diagnoses.\n";
print "\tFound $error_count errors.\n";

