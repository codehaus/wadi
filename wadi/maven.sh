#!/bin/sh

if [ -z "$MAVEN_HOME" ]
then
    MAVEN_HOME=/usr/java/maven-1.0
fi
export MAVEN_HOME

if [ -z "$JAVA_HOME" ]
then
    JAVA_HOME=/usr/java/j2sdk1.4.2_05
fi
export JAVA_HOME

PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
export PATH

echo JAVA_HOME=$JAVA_HOME
echo MAVEN_HOME=$MAVEN_HOME

exec $MAVEN_HOME/bin/maven --emacs --nobanner $@
