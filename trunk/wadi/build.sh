#!/bin/sh

PATH=$ASPECTJ_HOME/bin:$JAVA_HOME/bin:$ANT_HOME/bin:$PATH
export PATH

if [ -z "$JETTY_HOME" ]
then
    JETTY_HOME=/usr/java/Jetty-5.0.beta2
    JETTY_HOME=$HOME/cvs/jetty/Jetty
    #JETTY_HOME=$HOME/Jetty/
fi
export JETTY_HOME

if [ -z "$TOMCAT_HOME" ]
then
    #TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.18
    #TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.19
    TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.25
    TOMCAT_HOME=/usr/java/jakarta-tomcat-5.0.27
fi
export TOMCAT_HOME

echo JAVA_HOME=$JAVA_HOME
echo ANT_HOME=$ANT_HOME
echo ASPECTJ_HOME=$ASPECTJ_HOME
echo TOMCAT_HOME=$TOMCAT_HOME
echo JETTY_HOME=$JETTY_HOME

exec ant -emacs $@
