insert_after parameters[:location] do
 if( parameters[:start_delay] != nil && parameters[:start_delay] > 0)
    do_action "SleepFrom", parameters[:location], parameters[:start_delay]
  end
end
insert_after parameters[:location] do
  wait_for "NodesPersistedFindProviders", *parameters[:nodes]
end