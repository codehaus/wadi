#!/bin/sh
./node.sh xterm jetty red&
./node.sh xterm jetty green&
./node.sh xterm tomcat blue&
./node.sh xterm tomcat yellow&
