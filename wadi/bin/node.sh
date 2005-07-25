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
echo "CATALINA_HOME=$CATALINA_HOME"
$JAVA -fullversion

cp $WADI_HOME/conf/simplelog.properties $WADI_HOME/WEB-INF/classes

properties=`sed -e '/#.*/d' -e 's/${wadi.home}/$WADI_HOME/g' -e 's/\(.*\)/-D\1/g' $WADI_HOME/conf/node.$instance.properties | tr '\n' ' '`
properties=`eval "echo $properties"`

INSTANCE=$WADI_HOME/tmp/$instance
rm -fr $INSTANCE
mkdir -p $INSTANCE/logs
mkdir -p $INSTANCE/sessions

properties="\
-Dcycle.me=true \
$properties \
-Dwadi.home=$WADI_HOME \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Djava.io.tmpdir=$INSTANCE \
"

classpath=`find $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`$WADI_HOME/WEB-INF/classes:$JAVA_HOME/lib/tools.jar

if [ jetty = "$container" ]
then
    JETTY_BASE=$INSTANCE
    cd $JETTY_HOME

    properties="\
    $properties\
    -Djetty.home=$JETTY_BASE\
    "

    classpath=`find ./start.jar lib ext -name "*.jar" | tr '\n' ':'`$classpath
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty.xml
fi

if [ tomcat = "$container" ]
then
    CATALINA_BASE=$INSTANCE
    cd $CATALINA_HOME/bin

    mkdir -p $CATALINA_BASE/webapps
    mkdir -p $CATALINA_BASE/work
    cp -r $CATALINA_HOME/conf $CATALINA_BASE/
    rm -fr $CATALINA_BASE/conf/Catalina/localhost
    mkdir -p $CATALINA_BASE/conf/Catalina/localhost


    properties="\
    $properties\
    -Dcatalina.home=$CATALINA_HOME\
    -Djava.endorsed.dirs=$CATALINA_HOME/common/endorsed\
    -Dcatalina.base=$CATALINA_BASE\
    "

    classpath=`find $CATALINA_HOME/. -name "*.jar" | tr '\n' ':'`$classpath
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
    classpath=`find $TOMCAT55_HOME/. $WADI_HOME/WEB-INF/lib $JETTY_HOME/lib/org.mortbay.jetty.jar $JAVA_HOME/lib/tools.jar -name "*.jar" | tr '\n' ':'`:$WADI_HOME/WEB-INF/classes
    $XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat55.xml start
fi
