#!/bin/sh

rm -fr ~/.m2/repository/org/codehaus/wadi

mvn $@ clean && \
mvn $@ \
 -fn \
 -Dmaven.test.failure.ignore=true \
 clean \
 install \
 clover:instrument \
 clover:aggregate \
 site:site \
 site:deploy

status=$?

firefox http://wadi.codehaus.org

exit $status
