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
 site \


status=$?

firefox `pwd`/target/site/index.html

exit $status
