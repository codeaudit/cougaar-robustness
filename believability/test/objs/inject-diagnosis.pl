#!/usr/bin/perl

die "Usage: $0 <asset-name> <port> <sensor-name> <sensor-value>\n"
    unless ( $#ARGV == 3 );

$use_asset = $ARGV[0];
$use_port = $ARGV[1];
$use_sensor = $ARGV[2];
$use_diagnosis = $ARGV[3];

#$use_sensor = "AgentCommunicationDiagnosis1";
#$use_diagnosis = "OK";
#$use_diagnosis = "DEGRADED";
#$use_diagnosis = "NOT_COMMUNICATING";

#$use_sensor = "AgentCommunicationDiagnosis2";
#$use_diagnosis = "OK";
#$use_diagnosis = "DEGRADED";
#$use_diagnosis = "NOT_COMMUNICATING";
#$use_diagnosis = "DISCONNECTED";

sub inList {
    my( $item, $list_ref ) = @_;

    foreach (@$list_ref) {
           return( 1 ) if ( $_ eq $item );
    }

    return( 0 );
} # sub inList

$diag_mon_servlet="http://localhost:$use_port/\\\$$use_asset/DiagnosisMonitorServlet";

%field
    = (
	  "CHANGEDIAGNOSISVALUE" => "$use_diagnosis",
	  "UID" => "-extracted-from-form-",
	  "REFRESH" => "10000",
	  "NAMEFORMAT" => "SHORTNAME",
	  "ASSETFILTER" => "ALL",
	  "Submit" => "Set\%20Value"
	  );

@sensor_name = ();
%sensor_values = ();
%sensor_uid = ();
%sensor_agent = ();

@diag_page = `wget -O - $diag_mon_servlet`;
foreach $line_no (0..$#diag_page)
{
    # A line with this content appears right before each row of the
    # table that allows you to set a diagnosis for an sensor.
    #
    next unless $diag_page[$line_no] =~ m/\<\/TR\>\<TR\>/;
    $line_no++;

    # First line after this is the agent name column
    #
    $diag_page[$line_no] =~ m/\<TD\>(.*)\<\/TD\>/;
    $agent_name = $1;
    $line_no++;
    
    # Next line is the sensor name column.
    #
    $diag_page[$line_no] =~ m/\<TD\>(.*)\<\/TD\>/;
    $sensor_name = $1;
    push( @sensor_name, $1 );
    $sensor_agent{$sensor_name} = $agent_name;
    
    print "\nSensor found: $sensor_name\n";
    print "\tAgent: $agent_name\n";
    
    # Now we want to process all the possible sensor values which are
    #in a SELECT input tag.
    #
    while( $line_no <= $#diag_page )
    {
	   $line_no++;

	   # Skip until we see the start of the select.
	   #
	   next unless  $diag_page[$line_no] =~ m/\<SELECT/;

	   @sensor_values = ();
	   while( $line_no <= $#diag_page )
	   {
		  # Loop over OPTION lines until we see the end of the
	   select.
		  #
		  last if $diag_page[$line_no] =~ m/\<\/SELECT\>/;
		  $line_no++;
		  
		  if ( $diag_page[$line_no] =~ m/\<OPTION value\=\"(.+)\"/ )
		  {
			 push( @sensor_values, $1 );
		  }
	   }

	   $sensor_values{$sensor_name} = \@sensor_values;

	   print "\tSensor values: @sensor_values\n";
	   last;
    }

    # Now scan down the remainder of this form until we find the
    # sensor UID field, which is a hidden field.
    #
    while ( $line_no <= $#diag_page )
    {
	   $line_no++;
	   next unless $diag_page[$line_no] =~ m/type=hidden name=\"UID\" value=\"(.+)\"/;

	   $sensor_uid = $1;
	   print "\tSensor UID: $sensor_uid\n";
	   $sensor_uid{$sensor_name} = $sensor_uid;

	   # Once we've seen this, start again at the next row of the
	   # table.
	   #
	   last;
    }

    last if ( $sensor_name eq $use_sensor );

} # foreach $line

die "**ERROR** Sensor name '$use_sensor' not found.\n"
    unless &inList( $use_sensor, \@sensor_name );

$values = $sensor_values{$use_sensor};

die "**ERROR** Diagnosis value '$use_diagnosis' not found.\n"
    unless &inList( $use_diagnosis, $values );

$field{"UID"} = $sensor_uid{$use_sensor};
$agent_name = $sensor_agent{$use_sensor};

$data_content = "";
$first = 1;
foreach $key (keys(%field))
{
    if ($first)
    {
	   $first = 0;
    }
    else
    {
	   $data_content .= "\&";
    }
    $data_content .= "$key=$field{$key}";
} # foreach $key


$cmd = "curl --get -d '$data_content' 'http://localhost:$use_port/\$$agent_name/DiagnosisMonitorServlet/UPDATEVALUE'";

print "CMD: $cmd\n";

system( $cmd )
