#!/bin/bash -eu
if [ "$#" -eq 0 ]; then
  java -Dconfig.file=/etc/opt/arktrio/edge.conf -XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ZGenerational -jar /opt/terrafying/arktrio-edge.jar &
  PID=$!
  while [ ! -f /var/opt/arktrio/edge.shutdown ]; do
    sleep 1
  done
  kill -TERM $PID
  rm /var/opt/arktrio/edge.shutdown
else
  java -Dconfig.file=/etc/opt/arktrio/edge.conf -XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ZGenerational -jar /opt/terrafying/arktrio-edge.jar $@
fi
