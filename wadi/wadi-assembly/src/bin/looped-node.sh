#!/bin/sh

while true
do
    echo "starting node..."
    ./node.sh $@
    echo "...node stopped"
done
