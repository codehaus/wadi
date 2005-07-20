#!/bin/sh

if [ -z "$JETTY_HOME" ]
then
    JETTY_HOME=/home/jules/cvs/jetty
fi
export JETTY_HOME

if [ -z "$CATALINA_HOME" ]
then
    CATALINA_HOME=/usr/local/java/jakarta-tomcat-5.0.28
fi
export CATALINA_HOME


if [ -z "$JAVA_HOME" ]
then
    JAVA_HOME=/usr/local/java/sun-j2sdk1.4.2_08
fi
export JAVA_HOME

PATH=$JAVA_HOME/bin:$PATH
export PATH

echo JAVA_HOME=$JAVA_HOME

./node.sh xterm jetty red&
./node.sh xterm jetty green&
./node.sh xterm tomcat blue&
./node.sh xterm tomcat yellow&
