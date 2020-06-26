#!/bin/sh

#kill server
server_pid=`jps -l|grep "apollo.api"|awk '{print $1}'`

echo "spaces server pid is ${server_pid}"

if [[ -n ${server_pid} ]] ; then
  kill ${server_pid}
  echo "$server_pid is killed!"
fi