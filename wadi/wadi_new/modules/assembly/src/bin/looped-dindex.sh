#!/bin/sh

while true
do
    echo "starting dindex..."
    ./dindex.sh $@
    echo "...dindex stopped"
done
