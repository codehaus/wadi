#!/bin/sh

if [ "$1" = xterm ]
then
    XTERM="xterm -geometry 165x12 -bg \$instance -e"
    shift
else
    XTERM=
fi

container=$1
shift
instance=$1
shift

XTERM=`eval "echo $XTERM"`

JAVA_OPTS="-Xmx512m -enablesystemassertions $JAVA_OPTS"

WADI_HOME=`pwd`/..
JAVA=$JAVA_HOME/bin/java

echo "JAVA_HOME=$JAVA_HOME"
echo "WADI_HOME=$WADI_HOME"
echo "JETTY_HOME=$JETTY_HOME"
echo "TOMCAT_HOME=$TOMCAT_HOME"
$JAVA -fullversion

properties=`sed -e '/#.*/d' -e 's/${wadi.home}/$WADI_HOME/g' -e 's/\(.*\)/-D\1/g' $WADI_HOME/conf/node.$instance.properties | tr '\n' ' '`
properties=`eval "echo $properties"`


properties="$properties \
-Dwadi.home=$WADI_HOME \
-Dactivemq.persistenceAdapter=org.codehaus.activemq.store.vm.VMPersistenceAdapter \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
-Dorg.apache.commons.logging.simplelog.log.org=info \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activecluster=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activemq=warn \
-Dorg.apache.commons.logging.simplelog.log.org.springframework=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.AsyncToSyncAdaptorXXX=trace \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi=info \
-Dorg.apache.commons.logging.simplelog.showShortLogname=true \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
"

if [ jettyold = "$container" ]
then
    cd $JETTY_HOME
    $XTERM $JAVA $properties -cp $WADI_HOME/WEB-INF/classes $JAVA_OPTS -jar start.jar $WADI_HOME/conf/jetty.xml
fi

if [ jetty = "$container" ]
then
    cd $JETTY_HOME
    classpath=`find lib ext $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`./start.jar:$JAVA_HOME/lib/tools.jar:$WADI_HOME/WEB-INF/classes
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty.xml
fi

if [ tomcat = "$container" ]
then
    cd $TOMCAT_HOME/bin
    properties="-Djava.endorsed.dirs=$TOMCAT_HOME/common/endorsed -Dcatalina.base=$TOMCAT_HOME -Dcatalina.home=$TOMCAT_HOME -Djava.io.tmpdir=$TOMCAT_HOME/temp $properties"
    classpath=`find $TOMCAT_HOME/. $WADI_HOME/lib $WADI_HOME/WEB-INF/lib $JETTY_HOME/lib/org.mortbay.jetty.jar $JAVA_HOME/lib/tools.jar -name "*.jar" | tr '\n' ':'`:$WADI_HOME/WEB-INF/classes
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat.xml start
fi
