#!/bin/sh


## I'm sure it is not meant to be done like this - but I am moving in the right direction

##rm -fr $HOME/.m2

## I can't collapse these targets onto the same line as modules sites
## are then removed before main site is built...

mvn clean:clean && mvn install site && \
mvn -f pom.clover.xml clover:aggregate clover:clover
status=$?
##mvn site:stage -DstagingDirectory=target/stage
##firefox file:///home/jules/scm/wadi/target/stage/index.html

exit $status
