#!/bin/sh

PATH=$ASPECTJ_HOME/bin:$JAVA_HOME/bin:$ANT_HOME/bin:$PATH
export PATH

JETTY_HOME=/usr/java/Jetty-5.0.beta2
JETTY_HOME=$HOME/cvs/jetty/Jetty/
export JETTY_HOME

#TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.18
TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.19
export TOMCAT_HOME

echo JAVA_HOME=$JAVA_HOME

exec ant -emacs $@
