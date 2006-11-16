#!/bin/sh -x

numClients=1
concReqPerClient=1000
iters=100

cd ..

exec time java\
 \
-Xmx1024m \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
-Dorg.apache.commons.logging.simplelog.log.org=info \
-Dorg.apache.commons.logging.simplelog.showShortLogname=true \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
-classpath \
../../../core/target/test-classes:\
lib/wadi-core-2.0-SNAPSHOT.jar:\
lib/concurrent-1.3.4.jar:\
lib/commons-logging-api-1.0.4.jar:\
lib/commons-httpclient-2.0.2.jar:\
 \
org.codehaus.wadi.test.SoakTestClient $numClients $concReqPerClient $iters

##/usr/local/java/commons-httpclient-3.0-rc2/commons-httpclient-3.0-rc2.jar\

