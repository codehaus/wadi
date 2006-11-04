#!/bin/sh

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

firefox file:///home/jules/scm/wadi/target/stage/

exit $status
