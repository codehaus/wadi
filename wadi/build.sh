#!/bin/sh

if [ -z "$ANT_HOME" ]
then
    ANT_HOME=/usr/local/java/apache-ant-1.6.2
fi
export ANT_HOME

if [ -z "$JAVA_HOME" ]
then
    JAVA_HOME=/usr/local/java/sun-j2sdk1.4.2_05
fi
export JAVA_HOME

if [ -z "$ASPECTJ_HOME" ]
then
    ASPECTJ_HOME=/usr/local/java/aspectj1.2.1
fi
export ASPECTJ_HOME

if [ -z "$JETTY_HOME" ]
then
    JETTY_HOME=/usr/local/java/Jetty-5.1.1
    JETTY_HOME=$HOME/cvs/jetty
fi
export JETTY_HOME

if [ -z "$TOMCAT_HOME" ]
then
    TOMCAT_HOME=/usr/local/java/jakarta-tomcat-5.0.28
fi
export TOMCAT_HOME

PATH=$ASPECTJ_HOME/bin:$JAVA_HOME/bin:$ANT_HOME/bin:$PATH
export PATH

echo JAVA_HOME=$JAVA_HOME
echo ANT_HOME=$ANT_HOME
echo ASPECTJ_HOME=$ASPECTJ_HOME
echo TOMCAT_HOME=$TOMCAT_HOME
echo JETTY_HOME=$JETTY_HOME

sh $ANT_HOME/bin/ant -emacs $@
