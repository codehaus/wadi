#!/bin/sh

classpath=`find $JETTY_HOME -name "*.jar" | tr '\n' ':'`
exec $JAVA_HOME/bin/java org.mortbay.loadbalancer.Balancer 8081 8090 8091 8092 893