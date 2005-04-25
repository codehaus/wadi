#!/bin/sh

if [ -z "$MAVEN_HOME" ]
then
    MAVEN_HOME=/usr/local/java/maven-1.0.2
fi
export MAVEN_HOME

if [ -z "$JAVA_HOME" ]
then
    JAVA_HOME=/usr/local/java/sun-j2sdk1.4.2_08
fi
export JAVA_HOME

PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
export PATH

echo JAVA_HOME=$JAVA_HOME
echo MAVEN_HOME=$MAVEN_HOME

exec $MAVEN_HOME/bin/maven --emacs --nobanner $@

#./maven.sh -o -Dmaven.test.skip=true clean war
