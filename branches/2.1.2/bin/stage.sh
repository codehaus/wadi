#!/bin/sh

rm -fr ~/.m2/repository/org/codehaus/wadi

mvn $@ clean && \
mvn $@ \
 -fn \
 -Dmaven.test.failure.ignore=true \
 -DstagingDirectory=target/stage \
 clean \
 install \
 clover:instrument \
 clover:clover \
 site \
 clover:aggregate \
 site:stage

status=$?

firefox file://`pwd`/target/stage/https/odehaus.org/wadi/index.html

exit $status
