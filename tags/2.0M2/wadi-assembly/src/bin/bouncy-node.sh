#!/bin/sh

## start a node, maintain it for a period of time, stop it

period=$1
shift

while true
do
    echo "starting node..."
    ./node.sh $@ & pid=$!
    echo "node started (" $pid ")"
    sleep $period
    echo "stopping node (" $pid ")"
    kill -1 $pid
    wait $pid
    echo "node stopped"
    sleep 2
done
