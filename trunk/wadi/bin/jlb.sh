#!/bin/sh

## This script runs Jetty Balancer - a load balancer that comes as
## part of JettyExtra (jetty.mortbay.org). This is written in Java
## using NIO.

## This lb does per-connection stickiness.

## Fail-Over does not seem to work at the moment.

exec $JAVA_HOME/bin/java \
-cp \
$JETTY_HOME/extra/lib/org.mortbay.loadbalancer.jar:\
$JETTY_HOME/lib/org.mortbay.jetty.jar:\
$JETTY_HOME/ext/commons-logging.jar \
org.mortbay.loadbalancer.Balancer \
8081 \
- \
8090 \
8091 \
8092 \
8093
