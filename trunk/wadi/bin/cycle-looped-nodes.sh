#!/bin/bash

# bounce a node every $1 seconds
# nodes must each be run in loop (use looped-node.sh)
# nodes must mention http.port in their command line
# nodes will be bounced in random order

## return nth element in list, where n is random
randomth() { n=`expr $RANDOM \% $# + 1`; eval echo \$"$n"; }

interval=$1
while true
do
    nodes=`/bin/ps -C java -o pid,command | /bin/grep http.port | sed -e 's/ *\([0-9]*\) .*/\1/g'`
    node=`randomth $nodes`
    kill -2 $node
    sleep $interval
done
