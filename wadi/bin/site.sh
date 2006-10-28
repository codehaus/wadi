#!/bin/sh

##rm -fr $HOME/.m2

mvn clean:clean && \
mvn -Dmaven.test.failure.ignore=true site clover::aggregate site:deploy &&
firefox file:///tmp/WADI/2.0M2-SNAPSHOT/index.html
status=$?

##mvn site:stage -DstagingDirectory=target/stage
##firefox file:///home/jules/scm/wadi/target/stage/index.html

exit $status
