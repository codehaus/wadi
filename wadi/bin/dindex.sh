#!/bin/sh

cd ..

exec time java\
 \
-Xmx64m \
-Dcycle.me=true \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
-Dorg.apache.commons.logging.simplelog.log.org=info \
-Dorg.apache.commons.logging.simplelog.showShortLogname=true \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
-classpath \
WEB-INF/classes:\
WEB-INF/classes.test:\
WEB-INF/lib/concurrent-1.3.4.jar:\
WEB-INF/lib/activecluster-1.1-SNAPSHOT.jar:\
WEB-INF/lib/activemq-3.1-SNAPSHOT.jar:\
WEB-INF/lib/geronimo-spec-jms-1.1-rc2.jar:\
WEB-INF/lib/geronimo-spec-j2ee-management-1.0-rc2.jar:\
lib/commons-logging-1.0.4.jar:\
 \
org.codehaus.wadi.sandbox.dindex.DIndexNode $1 $2
