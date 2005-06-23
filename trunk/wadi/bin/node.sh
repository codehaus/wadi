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

JAVA_OPTS="-Xmx512m -ea $JAVA_OPTS"

WADI_HOME=`pwd`/..
JAVA=$JAVA_HOME/bin/java

export TOMCAT55_HOME=/usr/local/java/jakarta-tomcat-5.5.9
export JETTY6_HOME=/usr/local/java/jetty-6.0.alpha1

echo "JAVA_HOME=$JAVA_HOME"
echo "WADI_HOME=$WADI_HOME"
echo "JETTY_HOME=$JETTY_HOME"
echo "TOMCAT_HOME=$TOMCAT_HOME"
$JAVA -fullversion

properties=`sed -e '/#.*/d' -e 's/${wadi.home}/$WADI_HOME/g' -e 's/\(.*\)/-D\1/g' $WADI_HOME/conf/node.$instance.properties | tr '\n' ' '`
properties=`eval "echo $properties"`

INSTANCE=$WADI_HOME/tmp/$instance
rm -fr $INSTANCE
mkdir -p $INSTANCE/logs
mkdir -p $INSTANCE/temp

properties="\
-Dcycle.me=true \
$properties \
-Dwadi.home=$WADI_HOME \
-Dactivemq.persistenceAdapter=org.codehaus.activemq.store.vm.VMPersistenceAdapter \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
-Dorg.apache.commons.logging.simplelog.log.org=info \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activecluster=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.activemq=warn \
-Dorg.apache.commons.logging.simplelog.log.org.springframework=warn \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi=debug \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.impl.AbsoluteEvicter=info \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.impl.NeverEvicter=info \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.dindex=debug \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.impl.HybridRelocater=debug \
-Dorg.apache.commons.logging.simplelog.showShortLogname=true \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
-Djava.io.tmpdir=$INSTANCE/temp\
"

if [ jetty = "$container" ]
then
    JETTY_BASE=$INSTANCE
    cd $JETTY_HOME

    properties="\
    $properties\
    -Djetty.home=$JETTY_BASE\
    -Djava.endorsed.dirs=$TOMCAT_HOME/common/endorsed\
    -Dcatalina.base=$TOMCAT_BASE\
    "

    classpath=`find lib ext $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`./start.jar:$JAVA_HOME/lib/tools.jar:$WADI_HOME/WEB-INF/classes
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty.xml
fi

if [ tomcat = "$container" ]
then
    TOMCAT_BASE=$INSTANCE

    mkdir -p $TOMCAT_BASE/webapps
    mkdir -p $TOMCAT_BASE/work
    cp -r $TOMCAT_HOME/conf $TOMCAT_BASE/
    rm -fr $TOMCAT_BASE/conf/Catalina/localhost
    mkdir -p $TOMCAT_BASE/conf/Catalina/localhost

    cd $TOMCAT_HOME/bin

    properties="\
    $properties\
    -Dcatalina.home=$TOMCAT_HOME\
    -Djava.endorsed.dirs=$TOMCAT_HOME/common/endorsed\
    -Dcatalina.base=$TOMCAT_BASE\
    "

    classpath=`find $TOMCAT_HOME/. $WADI_HOME/lib $WADI_HOME/WEB-INF/lib $JAVA_HOME/lib/tools.jar -name "*.jar" | tr '\n' ':'`:$WADI_HOME/WEB-INF/classes

    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat.xml start
fi

if [ jetty6 = "$container" ]
then
    cd $JETTY6_HOME
##    classpath=`find lib ext $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`./start.jar:$JAVA_HOME/lib/tools.jar:$WADI_HOME/WEB-INF/classes
##    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty.xml
    $XTERM $JAVA $properties -jar start.jar
fi

if [ tomcat55 = "$container" ]
then
    cd $TOMCAT55_HOME/bin
    properties="-Djava.endorsed.dirs=$TOMCAT55_HOME/common/endorsed -Dcatalina.base=$TOMCAT55_HOME -Dcatalina.home=$TOMCAT55_HOME -Djava.io.tmpdir=$TOMCAT55_HOME/temp $properties"
    classpath=`find $TOMCAT55_HOME/. $WADI_HOME/lib $WADI_HOME/WEB-INF/lib $JETTY_HOME/lib/org.mortbay.jetty.jar $JAVA_HOME/lib/tools.jar -name "*.jar" | tr '\n' ':'`:$WADI_HOME/WEB-INF/classes
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat55.xml start
fi
