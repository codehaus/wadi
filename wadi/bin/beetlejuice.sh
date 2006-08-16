#!/bin/sh

## an integration script for various ci systems.


if [ "$SHELL" = /sbin/nologin ]
then
    ## we are being run by Beetlejuice...
    echo "available maven versions:"
    ls -ld /usr/local/maven*
    export JAVA_HOME=/usr/local/java-1.4
    export M2_HOME=/usr/local/maven-2.0.4
    echo
    export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
    export PROPS="-Duser.home=."
    export HOME=/home/nobody
elif [ "$USER" = continuum ]
then
    ## we are being run by continuum
    ## most vars should be set up
    export PROPS="-Duser.home=."
else
    ## we are being run by hand...
    ## JAVA_HOME and PATH should already be correctly initialised...
    export PROPS=
fi

ls -ld .
if [ -w . ]
then
    echo "filesystem is writeable - continuing"
else
    echo "ERROR: filesystem is not writeable - quitting"
    exit
fi

echo "***** Starting BeetleJuice build *****"
echo
echo "system is `uname -a`"
echo "time is `date`"
echo "shell is $SHELL"
echo "user is `id`"
type java
java -fullversion 2>&1 | cat
type mvn
mvn -version
echo "cwd is `pwd`"
echo "disk space:"
df -h .
echo

## cleanup
rm -fr ./testresults

PROPS="-e $PROPS"

## execute build, recording status
mvn $PROPS -DdownloadSources=true -Dmaven.test.failure.ignore=true clean:clean install eclipse:eclipse site && \
mvn $PROPS -f pom.clover.xml clover:aggregate clover:clover
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
