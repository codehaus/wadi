#!/bin/sh

cd $WADI_HOME

$JAVA_HOME/bin/java \
-ea \
-cp \
WEB-INF/classes:\
WEB-INF/lib/activecluster-1.0-SNAPSHOT.jar:\
WEB-INF/lib/activemq-1.0-SNAPSHOT.jar:\
WEB-INF/lib/concurrent-1.3.2.jar:\
WEB-INF/lib/geronimo-spec-j2ee-management-1.0-M1.jar:\
WEB-INF/lib/geronimo-spec-jms-1.0-M1.jar:\
lib/commons-logging-1.0.3.jar:\
 \
-Dpid=$$ \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activecluster=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activemq=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi=debug \
-Dorg.apache.commons.logging.simplelog.showShortLogname=true \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
\
org.codehaus.wadi.test.ClusterDemo \
$@

