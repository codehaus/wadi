#!/bin/sh

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
WEB-INF/classes:\
WEB-INF/classes.test:\
WEB-INF/lib/concurrent-1.3.4.jar:\
lib/commons-logging-1.0.4.jar:\
/usr/local/java/commons-httpclient-3.0-rc2/commons-httpclient-3.0-rc2.jar\
 \
org.codehaus.wadi.test.SoakTestClient 1000 10 100 1

#WEB-INF/lib/commons-httpclient-2.0.2.jar\
