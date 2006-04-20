#!/bin/sh

## an integration script for the BeetleJuice ci system.

if [ "$SHELL" = /sbin/nologin ]
then
    ## we are being run by Beetlejuice...
    export JAVA_HOME=/usr/local/java-1.4
    export M2_HOME=/usr/local/maven-2.0
    export M2_HOME=/usr/local/maven-2.0.3-SNAPSHOT
    export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
    export PROPS="-Duser.home=."
    export HOME=/home/nobody
else
    ## we are being run by hand...
    ## JAVA_HOME and PATH should already be correctly initialised...
    PROPS=
fi

echo "***** Starting BeetleJuice build *****"
echo
echo "system is `uname -a`"
echo "shell is $SHELL"
echo "user is `id`"
type java
java -fullversion 2>&1 | cat
type mvn
mvn -version
echo "cwd is `pwd`"
echo

## cleanup
rm -fr ./testresults

## execute build, recording status
mvn $PROPS clean:clean && \
mvn $PROPS install && \
mvn $PROPS site && \
mvn -f pom.clover.xml clover:aggregate clover:clover
status=$?

## gather all test results together for BJ
mkdir ./testresults
for i in `find . -name "TEST*.xml"`
do
  mv $i ./testresults/
done

echo
echo "***** BeetleJuice build completed *****"

exit $status
