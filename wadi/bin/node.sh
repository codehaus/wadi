#!/bin/sh -x

export JETTY5_HOME=/usr/local/java/jetty-5.1.3
export JETTY6_HOME=/usr/local/java/jetty-6.0.alpha1
export TOMCAT50_HOME=/usr/local/java/jakarta-tomcat-5.0.28-mx4j301
export TOMCAT55_HOME=/usr/local/java/jakarta-tomcat-5.5.9

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

echo "JAVA_HOME=$JAVA_HOME"
echo "WADI_HOME=$WADI_HOME"
echo "JETTY5_HOME=$JETTY5_HOME"
echo "JETTY6_HOME=$JETTY6_HOME"
echo "TOMCAT50_HOME=$TOMCAT50_HOME"
echo "TOMCAT55_HOME=$TOMCAT55_HOME"
$JAVA -fullversion

cp $WADI_HOME/conf/simplelog.properties $WADI_HOME/WEB-INF/classes/

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

#classpath=`find $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`$JAVA_HOME/lib/tools.jar:$WADI_HOME/WEB-INF/classes
classpath=`find $WADI_HOME/WEB-INF/lib -name "*.jar" | tr '\n' ':'`$JAVA_HOME/lib/tools.jar

case "$container" in

	jetty5)
	JETTY_BASE=$INSTANCE
	cd $JETTY5_HOME

	properties="\
	$properties\
	-Djetty.home=$JETTY_BASE\
	"

	##classpath=$JAVA_HOME/lib/tools.jar
	classpath=`find ./start.jar lib ext -name "*.jar" | tr '\n' ':'`$classpath
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty5.xml
	;;

	jetty6)

	JETTY_BASE=$INSTANCE
	cd $JETTY6_HOME

	properties="\
	$properties\
	-Djetty.home=$JETTY_BASE\
	"

	##classpath=$JAVA_HOME/lib/tools.jar
	classpath=`find ./start.jar lib ext  $WADI_HOME/lib -name "*.jar" | tr '\n' ':'`$classpath
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.mortbay.xml.XmlConfiguration $WADI_HOME/conf/jetty6.xml
	;;

	tomcat50)
	CATALINA_HOME=$TOMCAT50_HOME
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
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat50.xml start
	;;

	tomcat55)
	CATALINA_HOME=$TOMCAT55_HOME
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
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat55.xml start
	;;


	*)
	echo "bad container name: $container"
	;;

esac

