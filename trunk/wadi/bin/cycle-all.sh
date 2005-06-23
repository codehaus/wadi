#!/bin/sh

/bin/ps -C java -o pid,command | /bin/grep cycle.me | sed -e 's/ *\([0-9]*\) .*/\1/g' | xargs kill -2
