#!/bin/sh

asset_name=TestAgent
asset_port=8801

echo ""
echo "Injecting asset objects for $asset_name to port $asset_port."
echo ""

curl --get -d 'ASSETNAME='$asset_name'&Submit=Create' 'http://localhost:'$asset_port'/$TestAgent/PublishServlet/myForm' > /dev/null 2>&1

delay_secs=10

echo ""
echo -n "Sleeping for $delay_secs seconds..."
sleep $delay_secs
echo "sleeping done."

sensor=AgentCommunicationDiagnosis2
sensor_value_sequence="NOT_COMMUNICATING NOT_COMMUNICATING OK"
sensor_value_delay=5

first=1
for sensor_value in $sensor_value_sequence ; do

  if [ $first -eq 0 ] ; then
    echo ""
    echo -n "Sleeping for $sensor_value_delay seconds..."
    sleep $sensor_value_delay
    echo "sleeping done."
  fi

  echo ""
  echo "Injecting diagnosis $sensor_value for sensor $sensor."
  echo ""

  ./inject-diagnosis.pl \
      $asset_name $asset_port $sensor $sensor_value > /dev/null 2>&1

  first=0

done

echo ""
echo "Done."
echo ""
