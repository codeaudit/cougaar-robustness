#!/bin/tcsh 

foreach i ( `cat $CIP/csmart/socC/msglog/stevesMachines` )
  echo ssh $i $*
  ssh $i $*
#xterm -sl 50000 -title "$i" -geometry 60x10 -exec ssh $i &
end

