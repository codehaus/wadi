#!/bin/sh

## this script runs Pen, a 'C' load-balancer available at :http://siag.nu/pen/

## I can't find a way to turn off stickiness, but fail-over works
## nicely...

exec pen -f 8080 localhost:8090 localhost:8091 localhost:8092 localhost:8093
