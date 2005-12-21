#!/bin/sh +x

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

if [ "$1" = "jpda" ] ; then
  if [ -z "$JPDA_TRANSPORT" ]; then
    JPDA_TRANSPORT="dt_socket"
  fi
  if [ -z "$JPDA_ADDRESS" ]; then
    JPDA_ADDRESS="8000"
  fi
  if [ -z "$JPDA_OPTS" ]; then
    JPDA_OPTS="-Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=n"
  fi
  shift
fi

XTERM=`eval "echo $XTERM"`

JAVA_OPTS="-Xmx512m -ea $JAVA_OPTS"
#JPDA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"


WADI_HOME=`pwd`/..
WADI_VERSION=`cat $WADI_HOME/VERSION.txt`
JAVA=$JAVA_HOME/bin/java


echo "JAVA_HOME=$JAVA_HOME"
echo "WADI_HOME=$WADI_HOME"
echo "WADI_VERSION=$WADI_VERSION"
echo "JETTY5_HOME=$JETTY5_HOME"
echo "JETTY6_HOME=$JETTY6_HOME"
echo "TOMCAT50_HOME=$TOMCAT50_HOME"
echo "TOMCAT55_HOME=$TOMCAT55_HOME"
$JAVA -fullversion

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
-Dwadi.version=$WADI_VERSION \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
-Dorg.apache.commons.logging.simplelog.showdatetime=true \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi=debug \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.impl.AbsoluteEvicter=info \
-Dorg.apache.commons.logging.simplelog.log.org.codehaus.wadi.impl.NeverEvicter=info \
-Djava.io.tmpdir=$INSTANCE \
"

classpath=$WADI_HOME/target/classes:`find $WADI_HOME/lib -name "*.jar" | tr '\n' ':'`$JAVA_HOME/lib/tools.jar

case "$container" in

	jetty5)

	if [ -z "JETTY5_HOME" ]
	then
	    echo "Please set JETTY5_HOME"
	    exit 1
	fi

	JETTY_BASE=$INSTANCE
	cd $JETTY5_HOME

	properties="\
	$properties\
	-Djetty.home=$JETTY_BASE\
	"

	classpath=`find ./start.jar lib ext -name "*.jar" | tr '\n' ':'`$classpath
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS $JPDA_OPTS org.mortbay.jetty.Server $WADI_HOME/conf/jetty5.xml
	;;

	jetty6)

	if [ -z "JETTY6_HOME" ]
	then
	    echo "Please set JETTY6_HOME"
	    exit 1
	fi

	JETTY_BASE=$INSTANCE
	cd $JETTY6_HOME

	properties="\
	$properties\
	-Djetty.home=$JETTY_BASE\
	"

	classpath=`find ./start.jar lib ext -name "*.jar" | tr '\n' ':'`$classpath
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS $JPDA_OPTS org.mortbay.xml.XmlConfiguration $WADI_HOME/conf/jetty6.xml
	;;

	tomcat50)
	if [ -z "TOMCAT50_HOME" ]
	then
	    echo "Please set TOMCAT50_HOME"
	    exit 1
	fi

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
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS $JPDA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat50.xml start
	;;

	tomcat55)
	if [ -z "TOMCAT55_HOME" ]
	then
	    echo "Please set TOMCAT55_HOME"
	    exit 1
	fi
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
	$XTERM $JAVA $properties -cp $classpath $JAVA_OPTS $JPDA_OPTS org.apache.catalina.startup.Bootstrap -config $WADI_HOME/conf/tomcat55.xml start
	;;

	geronimo-jetty)
	if [ -z "GERONIMO_JETTY_HOME" ]
	then
	    echo "Please set GERONIMO_JETTY_HOME"
	    exit 1
	fi
	cd $GERONIMO_JETTY_HOME/bin
	export JAVA_OPTS=$properties
	$XTERM ./geronimo.sh run
	;;

	jboss4-tomcat)
	if [ -z "JBOSS4_TOMCAT_HOME" ]
	then
	    echo "Please set JBOSS4_TOMCAT_HOME"
	    exit 1
	fi

	pushd $WADI_HOME/lib
	# install log integration
	cp -f wadi-jboss4-*.jar $JBOSS4_TOMCAT_HOME/server/default/lib
	# deploy dep libs
	dir=/tmp/wadi-tmp-$$
	mkdir $dir
	cp *.jar $dir
	rm $dir/wadi-jboss4-*.jar
	mkdir $dir/META-INF
	echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <!DOCTYPE server> <server> </server>" > $dir/META-INF/jboss-service.xml
	cd $dir
	jar cvf $JBOSS4_TOMCAT_HOME/server/default/deploy/wadi.sar .
	popd

	# deploy webapp
	cp -f $WADI_HOME/webapps/wadi-webapp-*.war $JBOSS4_TOMCAT_HOME/server/default/deploy/wadi.war

	## we need to enable wadi logging somehow... - TODO

	cd $JBOSS4_TOMCAT_HOME/bin
	export JAVA_OPTS=$properties
	$XTERM ./run.sh
	;;

	*)
	echo "bad container name: $container"
	;;

esac

